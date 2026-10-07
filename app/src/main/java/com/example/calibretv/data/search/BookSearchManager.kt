package com.example.calibretv.data.search

import android.content.Context
import android.util.Log
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.curator.CuratedBook
import com.example.calibretv.data.curator.CuratorRepository
import com.example.calibretv.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 🔍 Categorías de resultados en el Buscador Universal de CalibroTV.
 */
enum class SearchCategory {
    LOCAL,           // "Tus Libros" / Memoria local del TV
    PUBLIC_DOMAIN,   // Descarga Gratuita Directa (Gutenberg, Standard Ebooks, Open Library)
    COMMERCIAL       // Bestsellers y Novedades (Comprar con QR multi-tienda)
}

/**
 * Modelo unificado de resultado de búsqueda para la TV.
 */
data class UnifiedBookResult(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val summary: String,
    val category: SearchCategory,
    val sourceName: String,
    val downloadUrl: String? = null,
    val localBook: Book? = null,
    val curatedBook: CuratedBook? = null,
    val isDownloaded: Boolean = false,
    val language: String = "es"
)

/**
 * Gestor del Buscador Universal Híbrido de CalibroTV / BookSpread.
 * Combina:
 * 1. Búsqueda Local en "Tus Libros" (SQLite / Memoria TV)
 * 2. Catálogo de Descarga Gratuita Directa (Project Gutenberg, Open Library, Standard Ebooks)
 * 3. Bestsellers y Novedades con ficha 3D y enlaces QR de afiliados
 * 4. Estrategia Híbrida de Enriquecimiento de Portadas HD
 */
object BookSearchManager {

    private const val TAG = "BookSearchManager"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Cache en memoria para portadas enriquecidas (evita llamadas redundantes a Open Library)
    private val enrichedCoverCache = ConcurrentHashMap<String, String>()

    // =========================================================================
    // 1. BÚSQUEDA LOCAL ("TUS LIBROS" / ROOM)
    // =========================================================================
    fun searchLocal(query: String, repository: BookRepository): List<UnifiedBookResult> {
        val q = query.trim().lowercase()
        val allCached = repository.getCachedBooks()
        val filtered = if (q.isBlank()) {
            allCached.take(20)
        } else {
            allCached.filter { b ->
                b.title.lowercase().contains(q) ||
                b.author.lowercase().contains(q) ||
                b.category.lowercase().contains(q) ||
                b.tags.any { it.lowercase().contains(q) } ||
                b.shelves.any { it.lowercase().contains(q) }
            }
        }

        return filtered.map { book ->
            val hasFile = !book.epubUrl.isNullOrBlank() && try {
                File(book.epubUrl.removePrefix("file://")).exists()
            } catch (_: Exception) { false }

            UnifiedBookResult(
                id = book.id,
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                summary = book.summary,
                category = SearchCategory.LOCAL,
                sourceName = if (hasFile) "En tu TV (Descargado)" else "En tu Biblioteca",
                downloadUrl = book.epubUrl,
                localBook = book,
                isDownloaded = hasFile
            )
        }
    }

    // =========================================================================
    // 2. BÚSQUEDA COMERCIAL (BESTSELLERS & NOVEDADES)
    // =========================================================================
    fun searchCommercial(query: String): List<UnifiedBookResult> {
        val q = query.trim().lowercase()
        val allCurated = CuratorRepository.getAllCuratedBooks().filter { c ->
            !c.isPublicDomain || c.difficultyLevel in 4..5 || !c.affiliateQrUrl.isNullOrBlank()
        }

        val filtered = if (q.isBlank()) {
            allCurated.take(20)
        } else {
            allCurated.filter { c ->
                c.title.lowercase().contains(q) ||
                c.author.lowercase().contains(q) ||
                c.category.lowercase().contains(q) ||
                c.summary.lowercase().contains(q)
            }
        }

        return filtered.map { c ->
            UnifiedBookResult(
                id = c.id,
                title = c.title,
                author = c.author,
                coverUrl = c.coverUrl,
                summary = c.summary,
                category = SearchCategory.COMMERCIAL,
                sourceName = "Bestseller BookSpread",
                curatedBook = c,
                isDownloaded = false
            )
        }
    }

    // =========================================================================
    // 3. BÚSQUEDA DE DOMINIO PÚBLICO (GUTENBERG & OPEN LIBRARY & CLÁSICOS)
    // =========================================================================

    /**
     * Busca en Project Gutenberg a través de la API rápida de Gutendex.
     */
    suspend fun searchGutenberg(query: String): List<UnifiedBookResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(q, "UTF-8")
            val url = "https://gutendex.com/books/?search=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "CalibroTV/3.45 (Android TV)")
                .build()

            val response = httpClient.newCall(req).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return@withContext emptyList()

            val list = mutableListOf<UnifiedBookResult>()
            val maxItems = minOf(results.length(), 6)

            for (i in 0 until maxItems) {
                val item = results.optJSONObject(i) ?: continue
                val id = item.optInt("id", 0)
                if (id == 0) continue

                val rawTitle = item.optString("title", "Obra sin título")
                val authorsArray = item.optJSONArray("authors")
                val author = if (authorsArray != null && authorsArray.length() > 0) {
                    val rawAuthor = authorsArray.getJSONObject(0).optString("name", "Autor Clásico")
                    cleanAuthorName(rawAuthor)
                } else "Autor Clásico"

                val formats = item.optJSONObject("formats")
                val epubUrl = formats?.optString("application/epub+zip")
                    ?: formats?.optString("application/x-mobipocket-ebook")
                    ?: "https://www.gutenberg.org/ebooks/$id.epub3.images"

                var coverUrl: String? = formats?.optString("image/jpeg")
                if (coverUrl.isNullOrBlank() || coverUrl.contains("small")) {
                    coverUrl = "https://www.gutenberg.org/cache/epub/$id/pg$id.cover.medium.jpg"
                }

                val languages = item.optJSONArray("languages")
                val lang = if (languages != null && languages.length() > 0) languages.getString(0) else "es"

                list.add(
                    UnifiedBookResult(
                        id = "gutenberg_$id",
                        title = cleanTitle(rawTitle),
                        author = author,
                        coverUrl = coverUrl,
                        summary = "Obra literaria clásica de dominio público preservada por Project Gutenberg.",
                        category = SearchCategory.PUBLIC_DOMAIN,
                        sourceName = "Project Gutenberg",
                        downloadUrl = epubUrl,
                        language = lang
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando Gutenberg: ${e.message}")
            emptyList()
        }
    }

    /**
     * Busca en Open Library (Internet Archive) para libros de dominio público y portadas de alta fidelidad.
     */
    suspend fun searchOpenLibrary(query: String): List<UnifiedBookResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(q, "UTF-8")
            val url = "https://openlibrary.org/search.json?q=$encoded&fields=key,title,author_name,cover_i,ia,ebook_access&limit=6"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "CalibroTV/3.45 (Android TV)")
                .build()

            val response = httpClient.newCall(req).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val docs = json.optJSONArray("docs") ?: return@withContext emptyList()

            val list = mutableListOf<UnifiedBookResult>()
            for (i in 0 until docs.length()) {
                val doc = docs.optJSONObject(i) ?: continue
                val rawTitle = doc.optString("title", "")
                if (rawTitle.isBlank()) continue

                val authorArray = doc.optJSONArray("author_name")
                val author = if (authorArray != null && authorArray.length() > 0) {
                    authorArray.getString(0)
                } else "Autor Desconocido"

                val coverId = doc.optInt("cover_i", 0)
                val coverUrl = if (coverId > 0) "https://covers.openlibrary.org/b/id/$coverId-L.jpg" else null

                val iaArray = doc.optJSONArray("ia")
                val isPublic = doc.optString("ebook_access", "") == "public"
                val iaId = if (iaArray != null && iaArray.length() > 0) iaArray.getString(0) else null

                val downloadUrl = if (isPublic && !iaId.isNullOrBlank()) {
                    "https://archive.org/download/$iaId/$iaId.epub"
                } else null

                list.add(
                    UnifiedBookResult(
                        id = "ol_${doc.optString("key", i.toString()).replace("/", "_")}",
                        title = cleanTitle(rawTitle),
                        author = author,
                        coverUrl = coverUrl,
                        summary = "Registro editorial de Open Library / Internet Archive.",
                        category = if (downloadUrl != null) SearchCategory.PUBLIC_DOMAIN else SearchCategory.COMMERCIAL,
                        sourceName = "Open Library",
                        downloadUrl = downloadUrl
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando Open Library: ${e.message}")
            emptyList()
        }
    }

    /**
     * Busca en los clásicos ya curados en CalibroTV (Standard Ebooks y ediciones de lujo integradas).
     */
    fun searchCuratedPublicDomain(query: String): List<UnifiedBookResult> {
        val q = query.trim().lowercase()
        val allCurated = CuratorRepository.getAllCuratedBooks().filter { it.isPublicDomain }
        val filtered = if (q.isBlank()) {
            allCurated
        } else {
            allCurated.filter {
                it.title.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }

        return filtered.map { c ->
            UnifiedBookResult(
                id = "curated_${c.id}",
                title = c.title,
                author = c.author,
                coverUrl = c.coverUrl,
                summary = c.summary,
                category = SearchCategory.PUBLIC_DOMAIN,
                sourceName = "Standard Ebooks • CalibroTV",
                downloadUrl = c.publicDownloadUrl,
                curatedBook = c,
                isDownloaded = false
            )
        }
    }

    // =========================================================================
    // 4. ESTRATEGIA HÍBRIDA DE ENRIQUECIMIENTO DE PORTADAS HD
    // =========================================================================

    /**
     * Si un libro de Gutenberg carece de portada o tiene una portada genérica/fea,
     * consulta la API de Open Library Covers para enriquecerla con una carátula HD real.
     */
    suspend fun enrichCoverIfNecessary(title: String, author: String, currentCoverUrl: String?): String? = withContext(Dispatchers.IO) {
        val cacheKey = "$title|$author".lowercase().trim()
        enrichedCoverCache[cacheKey]?.let { return@withContext it }

        // Si ya tiene una portada de alta calidad comprobada (Open Library o Standard Ebooks), mantenerla
        if (!currentCoverUrl.isNullOrBlank() && !currentCoverUrl.contains("gutenberg.org/cache")) {
            return@withContext currentCoverUrl
        }

        try {
            val q = URLEncoder.encode("$title $author", "UTF-8")
            val url = "https://openlibrary.org/search.json?q=$q&fields=cover_i&limit=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "CalibroTV/3.45 (Android TV)")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return@withContext currentCoverUrl
                val json = JSONObject(body)
                val docs = json.optJSONArray("docs")
                if (docs != null && docs.length() > 0) {
                    val coverId = docs.getJSONObject(0).optInt("cover_i", 0)
                    if (coverId > 0) {
                        val hdCover = "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
                        enrichedCoverCache[cacheKey] = hdCover
                        return@withContext hdCover
                    }
                }
            }
        } catch (_: Exception) {}

        currentCoverUrl
    }

    // =========================================================================
    // 5. BÚSQUEDA INTEGRADA TOTAL (UNIFIED SEARCH)
    // =========================================================================

    data class SearchResults(
        val localResults: List<UnifiedBookResult>,
        val publicDomainResults: List<UnifiedBookResult>,
        val commercialResults: List<UnifiedBookResult>
    ) {
        val totalCount: Int get() = localResults.size + publicDomainResults.size + commercialResults.size
        val isEmpty: Boolean get() = totalCount == 0
    }

    /**
     * Ejecuta la búsqueda concurrente a través de las 3 fuentes:
     * - Memoria local ("Tus Libros")
     * - Obras de Dominio Público con portadas enriquecidas (Gutenberg, Open Library, Standard Ebooks)
     * - Bestsellers Comerciales
     */
    suspend fun searchAll(
        query: String,
        repository: BookRepository
    ): SearchResults = coroutineScope {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@coroutineScope SearchResults(
                localResults = searchLocal("", repository),
                publicDomainResults = searchCuratedPublicDomain(""),
                commercialResults = searchCommercial("")
            )
        }

        // 1. Local y Curado (Instantáneos en memoria)
        val localDeferred = async(Dispatchers.Default) { searchLocal(cleanQuery, repository) }
        val curatedPublicDeferred = async(Dispatchers.Default) { searchCuratedPublicDomain(cleanQuery) }
        val commercialDeferred = async(Dispatchers.Default) { searchCommercial(cleanQuery) }

        // 2. Remotos (Gutenberg + Open Library en paralelo)
        val gutenbergDeferred = async(Dispatchers.IO) { searchGutenberg(cleanQuery) }
        val openLibraryDeferred = async(Dispatchers.IO) { searchOpenLibrary(cleanQuery) }

        val localList = localDeferred.await()
        val curatedPublicList = curatedPublicDeferred.await()
        val gutenbergList = gutenbergDeferred.await()
        val openLibraryList = openLibraryDeferred.await()
        val commercialList = commercialDeferred.await()

        // 3. Unificar Dominio Público deduplicando por título
        val combinedPublic = mutableListOf<UnifiedBookResult>()
        val seenTitles = mutableSetOf<String>()

        // Prioridad 1: Clásicos curados de CalibroTV / Standard Ebooks (máxima calidad de EPUB y portada)
        for (item in curatedPublicList) {
            val key = normalizeTitle(item.title)
            if (seenTitles.add(key)) combinedPublic.add(item)
        }

        // Prioridad 2: Gutenberg con enriquecimiento de portada en paralelo
        val enrichedGutenberg = gutenbergList.map { item ->
            async(Dispatchers.IO) {
                val betterCover = enrichCoverIfNecessary(item.title, item.author, item.coverUrl)
                item.copy(coverUrl = betterCover)
            }
        }.map { it.await() }

        for (item in enrichedGutenberg) {
            val key = normalizeTitle(item.title)
            if (seenTitles.add(key)) {
                combinedPublic.add(item)
            }
        }

        // Prioridad 3: Open Library (con descarga pública activa)
        for (item in openLibraryList.filter { it.category == SearchCategory.PUBLIC_DOMAIN }) {
            val key = normalizeTitle(item.title)
            if (seenTitles.add(key)) combinedPublic.add(item)
        }

        // 4. Deduplicar Comerciales (excluyendo los que ya están en local o dominio público)
        val finalCommercial = commercialList.filter { c ->
            val key = normalizeTitle(c.title)
            !seenTitles.contains(key) && localList.none { normalizeTitle(it.title) == key }
        }

        SearchResults(
            localResults = localList,
            publicDomainResults = combinedPublic,
            commercialResults = finalCommercial
        )
    }

    // =========================================================================
    // 6. DESCARGA E INSTALACIÓN DIRECTA EN LA TV (SIN FRICCIÓN)
    // =========================================================================

    /**
     * Descarga el libro en la memoria interna de la TV, verifica el archivo EPUB,
     * guarda la portada en caché y lo registra en la biblioteca Room / lista local.
     */
    suspend fun downloadAndInstallBook(
        context: Context,
        repository: BookRepository,
        result: UnifiedBookResult,
        onProgress: (Float) -> Unit = {}
    ): Result<Book> = withContext(Dispatchers.IO) {
        val downloadUrl = result.downloadUrl
        if (downloadUrl.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Este título no dispone de enlace de descarga directa."))
        }

        try {
            val booksDir = File(context.filesDir, "library")
            if (!booksDir.exists()) booksDir.mkdirs()

            val cleanId = result.id.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val destFile = File(booksDir, "book_${cleanId}.epub")
            val tempFile = File(booksDir, "temp_download_${System.currentTimeMillis()}.tmp")

            onProgress(0.1f)

            val req = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "CalibroTV/3.45 (Android TV)")
                .build()

            val response = httpClient.newCall(req).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error en el servidor de descarga (HTTP ${response.code})"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Respuesta vacía del servidor."))
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = 0.1f + (totalRead.toFloat() / contentLength.toFloat()) * 0.8f
                            onProgress(progress.coerceIn(0.1f, 0.9f))
                        }
                    }
                    output.flush()
                }
            }

            // Validar firma mágica de archivo EPUB/ZIP (primeros 2 bytes deben ser 'P' 'K')
            if (!tempFile.exists() || tempFile.length() < 1024) {
                if (tempFile.exists()) tempFile.delete()
                return@withContext Result.failure(Exception("El archivo descargado está incompleto o dañado."))
            }

            val header = ByteArray(2)
            java.io.FileInputStream(tempFile).use { it.read(header) }
            if (header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
                tempFile.delete()
                return@withContext Result.failure(Exception("El archivo descargado no es un EPUB válido."))
            }

            if (destFile.exists()) destFile.delete()
            if (!tempFile.renameTo(destFile)) {
                return@withContext Result.failure(Exception("No se pudo guardar el libro en el almacenamiento del televisor."))
            }

            onProgress(0.95f)

            // Crear y persistir el nuevo Book en la base de datos
            val newBook = Book(
                id = result.id,
                title = result.title,
                author = result.author,
                coverUrl = result.coverUrl,
                epubUrl = destFile.absolutePath,
                summary = result.summary,
                category = "Dominio Público",
                tags = listOf("Dominio Público", result.sourceName)
            )

            val current = repository.getCachedBooks().toMutableList()
            val existingIdx = current.indexOfFirst { it.id == result.id || it.title.equals(result.title, ignoreCase = true) }
            if (existingIdx >= 0) {
                current[existingIdx] = newBook
            } else {
                current.add(0, newBook)
            }
            repository.saveCachedBooks(current)

            onProgress(1.0f)
            Result.success(newBook)
        } catch (e: Exception) {
            Log.e(TAG, "Fallo durante la descarga del libro", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // UTILIDADES DE FORMATO Y LIMPIEZA
    // =========================================================================

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\s*\\(esp(?:a[ñn]ol)?\\)"), "")
            .replace(Regex("(?i)\\s*;\\s*or,.*"), "")
            .trim()
    }

    private fun cleanAuthorName(author: String): String {
        // En Gutenberg los autores vienen como "Cervantes Saavedra, Miguel de"
        val parts = author.split(",")
        return if (parts.size >= 2) {
            "${parts[1].trim()} ${parts[0].trim()}"
        } else author.trim()
    }

    private fun normalizeTitle(t: String): String {
        return t.lowercase()
            .replace(Regex("[^a-z0-9áéíóúñü]"), "")
            .trim()
    }
}
