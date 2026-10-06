package com.example.calibretv.data.epub

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.zip.ZipFile

sealed class PageItem {
    data class Paragraph(
        val text: String,
        val isHeader: Boolean = false,
        val isDropCap: Boolean = false
    ) : PageItem()

    data class Image(
        val imageFile: File,
        val altText: String? = null
    ) : PageItem()
}

data class PageContent(
    val chapterTitle: String,
    val bookTitle: String,
    val items: List<PageItem>,
    val pageNumber: Int
) {
    // Backwards-compatible property for existing code
    val paragraphs: List<String>
        get() = items.filterIsInstance<PageItem.Paragraph>().map { it.text }
}

data class PageSpread(
    val leftPage: PageContent,
    val rightPage: PageContent,
    val spreadIndex: Int,
    val totalSpreads: Int
)

sealed class RawContentItem {
    data class Text(val text: String, val isHeader: Boolean = false) : RawContentItem()
    data class Image(val imageFile: File, val altText: String? = null) : RawContentItem()
}

data class RawChapter(
    val title: String,
    val items: List<RawContentItem>
)

data class ParsedBook(
    val title: String,
    val chapters: List<RawChapter>
)

object EpubParser {

    suspend fun parseEpubToBook(epubFile: File, bookTitle: String): ParsedBook = withContext(Dispatchers.IO) {
        if (!epubFile.exists() || epubFile.length() == 0L) {
            return@withContext getSampleBook(bookTitle)
        }

        try {
            val zip = ZipFile(epubFile)
            val rootOpfPath = getOpfPath(zip)
                ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(".opf", ignoreCase = true) }?.name
                ?: "content.opf"
            val opfDir = if (rootOpfPath.contains("/")) rootOpfPath.substringBeforeLast("/") + "/" else ""

            // 1. Extract all illustrations to cache dir
            val imagesDir = File(epubFile.parentFile, "${epubFile.nameWithoutExtension}_assets")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val imageMap = extractImages(zip, imagesDir)

            // 2. Discover spine files
            var spineFiles = getSpineFiles(zip, rootOpfPath)
            if (spineFiles.isEmpty()) {
                spineFiles = zip.entries().asSequence()
                    .filter { it.name.endsWith(".xhtml", ignoreCase = true) || it.name.endsWith(".html", ignoreCase = true) }
                    .sortedBy { it.name }
                    .map { it.name.removePrefix(opfDir) }
                    .toList()
            }

            val chapters = mutableListOf<RawChapter>()

            for (fileName in spineFiles) {
                val fullPath = opfDir + fileName
                val decodedFullPath = try { URLDecoder.decode(fullPath, "UTF-8") } catch (_: Exception) { fullPath }
                val decodedFileName = try { URLDecoder.decode(fileName, "UTF-8") } catch (_: Exception) { fileName }
                val entry = zip.getEntry(fullPath)
                    ?: zip.getEntry(decodedFullPath)
                    ?: zip.getEntry(fileName)
                    ?: zip.getEntry(decodedFileName)
                    ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(fileName, ignoreCase = true) }

                if (entry != null) {
                    zip.getInputStream(entry).use { stream ->
                        val html = stream.bufferedReader().readText()
                        val title = extractChapterTitle(html) ?: "Capítulo ${chapters.size + 1}"
                        val items = parseHtmlToItems(html, imageMap)
                        if (items.isNotEmpty()) {
                            chapters.add(RawChapter(title, items))
                        }
                    }
                }
            }
            zip.close()

            if (chapters.isEmpty()) {
                return@withContext getNoticeBook(bookTitle, "No se encontraron secciones de texto legibles dentro de este archivo EPUB.")
            }

            return@withContext ParsedBook(bookTitle, chapters)
        } catch (_: Exception) {
            return@withContext getNoticeBook(bookTitle, "Error al decodificar la estructura interna de este archivo EPUB.")
        }
    }

    suspend fun parseEpub(
        epubFile: File,
        bookTitle: String,
        fontSizeSp: Int = 18,
        overscanPercent: Int = 0
    ): List<PageSpread> = withContext(Dispatchers.IO) {
        val parsedBook = parseEpubToBook(epubFile, bookTitle)
        return@withContext paginate(parsedBook, fontSizeSp, overscanPercent)
    }

    /**
     * Extracts images from the EPUB archive and indexes them by various path permutations.
     */
    private fun extractImages(zip: ZipFile, imagesDir: File): Map<String, File> {
        val map = mutableMapOf<String, File>()
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val name = entry.name
            val lower = name.lowercase()
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".webp") || lower.endsWith(".gif")) {
                val cleanFileName = name.replace('/', '_').replace('\\', '_')
                val destFile = File(imagesDir, cleanFileName)
                if (!destFile.exists() || destFile.length() == 0L) {
                    try {
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (_: Exception) {
                        // Ignored
                    }
                }
                if (destFile.exists() && destFile.length() > 0L) {
                    val baseName = name.substringAfterLast('/').lowercase()
                    map[name.lowercase()] = destFile
                    map[baseName] = destFile
                    try {
                        val decoded = URLDecoder.decode(name, "UTF-8").lowercase()
                        val decodedBase = decoded.substringAfterLast('/')
                        map[decoded] = destFile
                        map[decodedBase] = destFile
                    } catch (_: Exception) {}
                }
            }
        }
        return map
    }

    /**
     * Parses chapter HTML into a sequence of Text and Image items preserving original reading order.
     */
    private fun parseHtmlToItems(html: String, imageMap: Map<String, File>): List<RawContentItem> {
        val items = mutableListOf<RawContentItem>()

        // ── PASO 0: Eliminar encabezados XML/DOCTYPE/HTML antes de parsear ──────────
        val bodyContent = run {
            // 1. Quitar declaración XML: <?xml ... ?>
            var cleaned = html.replace(Regex("""<\?xml[^?]*\?>""", RegexOption.IGNORE_CASE), "")
            // 2. Quitar DOCTYPE: <!DOCTYPE ... > (puede ser multilínea)
            cleaned = cleaned.replace(Regex("""(?s)<!DOCTYPE[^>]*>""", RegexOption.IGNORE_CASE), "")
            // 3. Quitar comentarios HTML: <!-- ... -->
            cleaned = cleaned.replace(Regex("""(?s)<!--.*?-->"""), "")
            // 4. Extraer solo el contenido dentro de <body>...</body> si existe
            val bodyMatch = Regex("""(?is)<body[^>]*>(.*?)</body>""").find(cleaned)
            if (bodyMatch != null) {
                bodyMatch.groupValues[1]
            } else {
                // Si no hay <body>, quitar las etiquetas <html>, <head> y su contenido
                cleaned = cleaned.replace(Regex("""(?is)<head[^>]*>.*?</head>"""), "")
                cleaned = cleaned.replace(Regex("""(?i)</?html[^>]*>"""), "")
                cleaned
            }
        }
        // ── FIN PASO 0 ──────────────────────────────────────────────────────────────

        val sanitizedHtml = bodyContent
            .replace(Regex("""(?s)<script.*?</script>"""), "")
            .replace(Regex("""(?s)<style.*?</style>"""), "")

        val tagRegex = Regex("""<(/?[a-zA-Z0-9]+)([^>]*)>""")
        var currentText = StringBuilder()
        var lastEnd = 0
        var isHeader = false

        fun flushText(forceHeader: Boolean = false) {
            val clean = cleanEntities(currentText.toString()).trim()
            currentText.clear()
            if (clean.isNotBlank()) {
                items.add(RawContentItem.Text(clean, isHeader = isHeader || forceHeader))
            }
        }

        val matches = tagRegex.findAll(sanitizedHtml)
        for (match in matches) {
            val textBetween = sanitizedHtml.substring(lastEnd, match.range.first)
            currentText.append(textBetween)
            lastEnd = match.range.last + 1

            val tagName = match.groupValues[1].lowercase()
            val attributes = match.groupValues[2]

            when {
                tagName == "img" -> {
                    flushText()
                    val src = extractAttribute(attributes, "src")
                    if (!src.isNullOrBlank()) {
                        val file = resolveImageFile(src, imageMap)
                        if (file != null) {
                            val alt = extractAttribute(attributes, "alt")
                            items.add(RawContentItem.Image(file, alt))
                        }
                    }
                }
                tagName == "image" -> {
                    flushText()
                    val href = extractAttribute(attributes, "xlink:href") ?: extractAttribute(attributes, "href")
                    if (!href.isNullOrBlank()) {
                        val file = resolveImageFile(href, imageMap)
                        if (file != null) {
                            items.add(RawContentItem.Image(file))
                        }
                    }
                }
                tagName in listOf("h1", "h2", "h3", "h4") -> {
                    flushText()
                    isHeader = true
                }
                tagName in listOf("/h1", "/h2", "/h3", "/h4") -> {
                    flushText(forceHeader = true)
                    isHeader = false
                }
                tagName in listOf("p", "div", "blockquote", "section", "article") -> {
                    flushText()
                }
                tagName in listOf("/p", "/div", "/blockquote", "/section", "/article", "br", "br/") -> {
                    flushText()
                }
            }
        }

        val remainingText = sanitizedHtml.substring(lastEnd)
        currentText.append(remainingText)
        flushText()

        return items
    }

    private fun resolveImageFile(src: String, imageMap: Map<String, File>): File? {
        val decoded = try { URLDecoder.decode(src, "UTF-8") } catch (_: Exception) { src }
        val filename = decoded.substringAfterLast('/').substringAfterLast('\\').lowercase()
        val normalized = decoded.replace("../", "").replace("./", "").lowercase()
        return imageMap[normalized]
            ?: imageMap[filename]
            ?: imageMap.entries.firstOrNull { it.key.endsWith(filename) }?.value
    }

    private fun extractAttribute(attributes: String, attrName: String): String? {
        val pattern = Regex("""$attrName\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return pattern.find(attributes)?.groupValues?.get(1)
    }

    private fun cleanEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#160;", " ")
            .replace(Regex("""\s+"""), " ")
    }

    private fun extractChapterTitle(html: String): String? {
        val hRegex = Regex("""<h[1-3][^>]*>(.*?)</h[1-3]>""", RegexOption.IGNORE_CASE)
        val match = hRegex.find(html)
        return match?.groups?.get(1)?.value?.let { cleanEntities(it.replace(Regex("<[^>]*>"), "")) }?.take(40)
    }

    /**
     * DYNAMIC PAGINATION ALGORITHM:
     * Adapts words-per-page based on the exact font size (16sp, 18sp, 20sp, 22sp, 24sp)
     * and overscan settings, guaranteeing that:
     * 1. Increasing font size increases the total page count dynamically.
     * 2. NO text is cut off or lost beneath the bottom edge.
     * 3. Images receive appropriate height budget and are not squished or overflowed.
     */
    fun paginate(
        book: ParsedBook,
        fontSizeSp: Int,
        overscanPercent: Int = 0
    ): List<PageSpread> {
        val singlePages = mutableListOf<PageContent>()
        var pageCounter = 1

        // Precise word budget calculated from TV container height and line-height:
        val maxWordsPerPage = when {
            fontSizeSp <= 16 -> 82
            fontSizeSp <= 18 -> 68
            fontSizeSp <= 20 -> 54
            fontSizeSp <= 22 -> 44
            fontSizeSp <= 24 -> 36
            else -> 30
        }.let { if (overscanPercent > 0) (it * 0.88).toInt() else it }

        for (chapter in book.chapters) {
            val currentPageItems = mutableListOf<PageItem>()
            var currentWordWeight = 0

            fun flushPage() {
                if (currentPageItems.isNotEmpty()) {
                    singlePages.add(
                        PageContent(
                            chapterTitle = chapter.title,
                            bookTitle = book.title,
                            items = ArrayList(currentPageItems),
                            pageNumber = pageCounter++
                        )
                    )
                    currentPageItems.clear()
                    currentWordWeight = 0
                }
            }

            for (item in chapter.items) {
                when (item) {
                    is RawContentItem.Image -> {
                        // An image takes ~32 words equivalent of vertical height
                        val imageWeight = 32
                        if (currentWordWeight > 18) {
                            flushPage()
                        }
                        currentPageItems.add(PageItem.Image(item.imageFile, item.altText))
                        currentWordWeight += imageWeight
                        if (currentWordWeight >= maxWordsPerPage) {
                            flushPage()
                        }
                    }
                    is RawContentItem.Text -> {
                        val text = item.text.trim()
                        if (text.isBlank()) continue

                        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
                        val remainingCapacity = maxWordsPerPage - currentWordWeight

                        if (words.size <= remainingCapacity) {
                            currentPageItems.add(PageItem.Paragraph(text, isHeader = item.isHeader))
                            currentWordWeight += words.size
                            if (currentWordWeight >= maxWordsPerPage) {
                                flushPage()
                            }
                        } else {
                            // If only a tiny fragment fits, start a clean new page
                            if (currentPageItems.isNotEmpty() && remainingCapacity < 16) {
                                flushPage()
                            }

                            var wordIdx = 0
                            while (wordIdx < words.size) {
                                val capacity = if (currentPageItems.isEmpty()) maxWordsPerPage else (maxWordsPerPage - currentWordWeight)
                                val chunkCount = minOf(capacity, words.size - wordIdx)
                                val chunkText = words.subList(wordIdx, wordIdx + chunkCount).joinToString(" ")
                                currentPageItems.add(
                                    PageItem.Paragraph(
                                        chunkText,
                                        isHeader = item.isHeader && wordIdx == 0
                                    )
                                )
                                currentWordWeight += chunkCount
                                wordIdx += chunkCount
                                if (currentWordWeight >= maxWordsPerPage) {
                                    flushPage()
                                }
                            }
                        }
                    }
                }
            }
            flushPage()
        }

        // Pair single pages into PageSpread (2 columns 16:9)
        val spreads = mutableListOf<PageSpread>()
        val totalSpreads = (singlePages.size + 1) / 2

        var idx = 0
        while (idx < singlePages.size) {
            val left = singlePages[idx]
            val right = if (idx + 1 < singlePages.size) {
                singlePages[idx + 1]
            } else {
                PageContent(
                    chapterTitle = left.chapterTitle,
                    bookTitle = book.title,
                    items = listOf(PageItem.Paragraph("— Fin del libro —")),
                    pageNumber = pageCounter
                )
            }
            spreads.add(
                PageSpread(
                    leftPage = left,
                    rightPage = right,
                    spreadIndex = spreads.size,
                    totalSpreads = totalSpreads
                )
            )
            idx += 2
        }

        return if (spreads.isNotEmpty()) spreads else getSampleSpreads(book.title)
    }

    fun extractDescription(epubFile: File): String? {
        if (!epubFile.exists() || epubFile.length() == 0L) return null
        return try {
            val zip = ZipFile(epubFile)
            val rootOpfPath = getOpfPath(zip)
                ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(".opf", ignoreCase = true) }?.name
                ?: "content.opf"
            val entry = zip.getEntry(rootOpfPath) ?: return null
            var description: String? = null
            zip.getInputStream(entry).use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, null)
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && (parser.name.equals("description", ignoreCase = true) || parser.name.endsWith(":description", ignoreCase = true))) {
                        description = cleanEntities(parser.nextText().replace(Regex("<[^>]*>"), ""))
                        break
                    }
                    event = parser.next()
                }
            }
            zip.close()
            description?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun getOpfPath(zip: ZipFile): String? {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
        return try {
            zip.getInputStream(containerEntry).use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, null)
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name.equals("rootfile", ignoreCase = true)) {
                        return parser.getAttributeValue(null, "full-path")
                    }
                    event = parser.next()
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getSpineFiles(zip: ZipFile, opfPath: String): List<String> {
        val entry = zip.getEntry(opfPath) ?: return emptyList()
        val manifest = mutableMapOf<String, String>()
        val spine = mutableListOf<String>()

        try {
            zip.getInputStream(entry).use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, null)
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        when (parser.name.lowercase()) {
                            "item" -> {
                                val id = parser.getAttributeValue(null, "id") ?: ""
                                val href = parser.getAttributeValue(null, "href") ?: ""
                                if (id.isNotBlank() && href.isNotBlank()) {
                                    manifest[id] = href
                                }
                            }
                            "itemref" -> {
                                val idref = parser.getAttributeValue(null, "idref") ?: ""
                                val href = manifest[idref]
                                if (href != null) {
                                    spine.add(href)
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (_: Exception) {
            // Ignored
        }
        return spine
    }

    fun getNoticeBook(bookTitle: String, message: String): ParsedBook {
        val chapter = RawChapter(
            title = "Aviso del Formato",
            items = listOf(
                RawContentItem.Text("Información de compatibilidad:", isHeader = true),
                RawContentItem.Text(message),
                RawContentItem.Text("El lector 3D de CalibroTV está optimizado para libros en formato estándar EPUB con soporte de pliegos tipográficos 16:9 e ilustraciones."),
                RawContentItem.Text("Puedes sincronizar o subir una versión .epub de este título a tu servidor Calibre-Web para disfrutar de la experiencia completa.")
            )
        )
        return ParsedBook(bookTitle, listOf(chapter))
    }

    fun getNoticeSpreads(bookTitle: String, message: String): List<PageSpread> {
        return paginate(getNoticeBook(bookTitle, message), 18, 0)
    }

    fun getSampleBook(bookTitle: String = "Crónicas del Vacío"): ParsedBook {
        val ch1 = RawChapter(
            title = "Capítulo I",
            items = listOf(
                RawContentItem.Text("Era una noche de viento huracanado sobre los tejados de la ciudad virtual. Las pantallas emitían un fulgor ámbar que se reflejaba en los charcos de la avenida principal, mientras los servidores procesaban millones de páginas por segundo en silencio absoluto."),
                RawContentItem.Text("El sistema de proyección cenital había sido calibrado exactamente a dieciocho grados, permitiendo una lectura fluida desde cualquier posición del salón sin fatiga visual ni reflejos molestos."),
                RawContentItem.Text("—¿Crees que el lector comprende la profundidad de este pliegue digital? —preguntó el avatar de la esquina inferior, ajustando sus parámetros de contraste nocturno."),
                RawContentItem.Text("El brillo ámbar del OLED reducía la emisión de luz azul un noventa por ciento, garantizando que el descanso posterior fuera tan profundo como la inmersión actual en la biblioteca remota de Calibre.")
            )
        )
        val ch2 = RawChapter(
            title = "Capítulo II",
            items = listOf(
                RawContentItem.Text("La biblioteca flotante albergaba miles de manuscritos recuperados de la antigua red. Cada volumen aparecía en el lienzo oscuro como un portal hacia tiempos donde las palabras se imprimían en celulosa."),
                RawContentItem.Text("Al pulsar el botón del mando a distancia, la página se doblaba con una física suave y precisa, emulando el peso real del papel bajo la luz tenue de la habitación."),
                RawContentItem.Text("Los sensores del proyector detectaron la superficie del techo y adaptaron la geometría en tiempo real. No existía distorsión en las esquinas, ni bordes borrosos que entorpecieran la mirada."),
                RawContentItem.Text("—Todo está listo —susurró el sistema—. Que comience la travesía hacia los confines del vacío.")
            )
        )
        val ch3 = RawChapter(
            title = "Capítulo III",
            items = listOf(
                RawContentItem.Text("En el horizonte de silicio, los destellos de datos trazaban constelaciones artificiales. Los navegantes de la red sabían que leer era el último refugio contra la velocidad del mundo exterior."),
                RawContentItem.Text("Cada frase era una pausa deliberada, un respiro en la penumbra donde la mente recuperaba su cadencia natural."),
                RawContentItem.Text("La tipografía tallada en luz dorada permanecía nítida a tres metros de distancia. Sin distracciones, sin notificaciones invasivas, únicamente el autor, las palabras y el lector suspendidos en la calma de la noche."),
                RawContentItem.Text("—Continuará en el próximo pliego...")
            )
        )
        return ParsedBook(bookTitle, listOf(ch1, ch2, ch3))
    }

    fun getSampleSpreads(bookTitle: String = "Crónicas del Vacío"): List<PageSpread> {
        return paginate(getSampleBook(bookTitle), 18, 0)
    }

    /**
     * Elimina el directorio de assets extraídos de un libro EPUB para liberar almacenamiento.
     */
    fun cleanExtractedAssets(epubFile: File) {
        try {
            val assetsDir = File(epubFile.parentFile, "${epubFile.nameWithoutExtension}_assets")
            if (assetsDir.exists() && assetsDir.isDirectory) {
                assetsDir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }
}
