package com.example.calibretv.data.curator

import android.content.Context
import android.util.Log
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.epub.EpubParser
import com.example.calibretv.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repositorio de la cartelera dinámica y curaduría de BookSpread.
 * Provee carruseles guiados por arquetipos literarios, soporte para descargas
 * directas de dominio público y enlaces QR de afiliados.
 */
object CuratorRepository {

    private const val TAG = "CuratorRepository"

    private var cachedCmsSections: List<CuratorSection>? = null
    private var cachedHeroBanner: HeroBanner? = null

    /**
     * Retorna las secciones curadas para la pantalla principal.
     * Si el CMS está sincronizado, retorna el catálogo dinámico;
     * de lo contrario, opera 100% offline con el catálogo local integrado.
     */
    fun getCuratedSections(): List<CuratorSection> {
        val cms = cachedCmsSections
        if (!cms.isNullOrEmpty()) {
            return cms
        }
        return getOfflineCuratedSections()
    }

    suspend fun fetchHeroBanner(cmsUrl: String = "http://192.168.1.89:4000"): HeroBanner? = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (cmsUrl.endsWith("/")) "${cmsUrl}api/v1/cartelera/home" else "$cmsUrl/api/v1/cartelera/home"
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.connect()
            if (conn.responseCode in 200..299) {
                val json = org.json.JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val hero = json.optJSONObject("hero_banner") ?: return@withContext null
                val banner = HeroBanner(
                    id = hero.optString("id", "main_hero"),
                    title = hero.optString("title", ""),
                    author = hero.optString("author", ""),
                    tagline = hero.optString("tagline", ""),
                    synopsis = hero.optString("synopsis", ""),
                    backdropUrl = hero.optString("backdrop_url", ""),
                    coverUrl = hero.optString("cover_url", ""),
                    sampleEpubUrl = hero.optString("sample_epub_url").takeIf { it.isNotBlank() },
                    affiliatePurchaseUrl = hero.optString("affiliate_purchase_url").takeIf { it.isNotBlank() }
                )
                cachedHeroBanner = banner
                banner
            } else null
        } catch (e: Exception) {
            Log.d(TAG, "Hero banner fetch failed or offline: ${e.message}")
            cachedHeroBanner
        }
    }

    suspend fun syncWithCms(cmsUrl: String = "http://192.168.1.89:4000"): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (cmsUrl.endsWith("/")) "${cmsUrl}api/v1/cartelera/shelves" else "$cmsUrl/api/v1/cartelera/shelves"
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.connect()
            if (conn.responseCode in 200..299) {
                val json = org.json.JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val shelvesArr = json.optJSONArray("shelves") ?: return@withContext false
                val sections = mutableListOf<CuratorSection>()

                for (i in 0 until shelvesArr.length()) {
                    val sObj = shelvesArr.getJSONObject(i)
                    val sId = sObj.optString("shelf_id")
                    val charName = sObj.optString("character_name")
                    val avatarUrl = sObj.optString("avatar_url")
                    val quote = sObj.optString("quote")

                    val booksArr = sObj.optJSONArray("books")
                    val booksList = mutableListOf<CuratedBook>()
                    if (booksArr != null) {
                        for (j in 0 until booksArr.length()) {
                            val bObj = booksArr.getJSONObject(j)
                            val isPub = bObj.optBoolean("is_public_domain", false)
                            booksList.add(
                                CuratedBook(
                                    id = bObj.optString("book_id"),
                                    title = bObj.optString("title"),
                                    author = bObj.optString("author"),
                                    coverUrl = bObj.optString("cover_url"),
                                    summary = bObj.optString("synopsis"),
                                    category = charName,
                                    isPublicDomain = isPub,
                                    affiliateQrUrl = bObj.optString("affiliate_url").takeIf { it.isNotBlank() },
                                    publicDownloadUrl = bObj.optString("download_url").takeIf { it.isNotBlank() },
                                    difficultyLevel = bObj.optInt("difficulty_level", 1)
                                )
                            )
                        }
                    }

                    val archetype = when {
                        sId.contains("prodigy") || sId.contains("lisa") || sId.contains("matilda") || sId.contains("hermione") -> CuratorArchetype.PRODIGY
                        sId.contains("detective") || sId.contains("house") || sId.contains("holmes") || sId.contains("jane") -> CuratorArchetype.DETECTIVE
                        sId.contains("cosmic") || sId.contains("dune") || sId.contains("stark") -> CuratorArchetype.COSMIC
                        sId.contains("classic") || sId.contains("quijote") -> CuratorArchetype.CLASSICS
                        else -> CuratorArchetype.GENERAL
                    }

                    sections.add(
                        CuratorSection(
                            id = sId,
                            archetype = archetype,
                            name = charName,
                            tagline = quote.ifBlank { archetype.subtitle },
                            books = booksList,
                            avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
                            quote = quote.takeIf { it.isNotBlank() }
                        )
                    )
                }

                if (sections.isNotEmpty()) {
                    cachedCmsSections = sections
                    Log.d(TAG, "Successfully synced ${sections.size} shelves from CMS: $cmsUrl")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            Log.d(TAG, "CMS sync offline or failed: ${e.message}")
            false
        }
    }

    fun getOfflineCuratedSections(): List<CuratorSection> {
        return listOf(
            CuratorSection(
                id = "curator_classics",
                archetype = CuratorArchetype.CLASSICS,
                name = CuratorArchetype.CLASSICS.title,
                tagline = CuratorArchetype.CLASSICS.subtitle,
                books = listOf(
                    CuratedBook(
                        id = "pub_principito",
                        title = "El Principito",
                        author = "Antoine de Saint-Exupéry",
                        coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80",
                        summary = "Un piloto perdido en el desierto del Sahara encuentra a un pequeño príncipe de otro planeta. Una fábula poética sobre el amor, la amistad y el sentido de la vida.",
                        category = "Clásico Universal",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/70114.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1943"
                    ),
                    CuratedBook(
                        id = "pub_orgullo",
                        title = "Orgullo y Prejuicio",
                        author = "Jane Austen",
                        coverUrl = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&auto=format&fit=crop&q=80",
                        summary = "La brillante Elizabeth Bennet y el orgulloso Fitzwilliam Darcy deben superar sus prejuicios para descubrir la verdad de sus sentimientos en la Inglaterra georgiana.",
                        category = "Novela Clásica",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/1342.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1813"
                    ),
                    CuratedBook(
                        id = "pub_quijote",
                        title = "Don Quijote de la Mancha",
                        author = "Miguel de Cervantes",
                        coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80",
                        summary = "Las inmortales andanzas del hidalgo manchego y su fiel escudero Sancho Panza en pos de la justicia, la caballería andante y la libertad del espíritu.",
                        category = "Obra Cumbre",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/2000.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1605"
                    ),
                    CuratedBook(
                        id = "pub_metamorfosis",
                        title = "La Metamorfosis",
                        author = "Franz Kafka",
                        coverUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80",
                        summary = "Una mañana, tras un sueño intranquilo, Gregorio Samsa se despierta convertido en un monstruoso insecto. El retrato más sobrecogedor de la alienación humana.",
                        category = "Ficción Existencial",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/5200.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1915"
                    )
                )
            ),
            CuratorSection(
                id = "curator_detective",
                archetype = CuratorArchetype.DETECTIVE,
                name = CuratorArchetype.DETECTIVE.title,
                tagline = CuratorArchetype.DETECTIVE.subtitle,
                books = listOf(
                    CuratedBook(
                        id = "pub_holmes_study",
                        title = "Estudio en Escarlata",
                        author = "Arthur Conan Doyle",
                        coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
                        summary = "La primera aventura donde se cruzan los destinos de Sherlock Holmes y el Dr. John Watson, desentrañando un misterioso crimen con la palabra RACHE escrita en sangre.",
                        category = "Misterio",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/244.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1887"
                    ),
                    CuratedBook(
                        id = "pub_holmes_baskerville",
                        title = "El Sabueso de los Baskerville",
                        author = "Arthur Conan Doyle",
                        coverUrl = "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=600&auto=format&fit=crop&q=80",
                        summary = "Una maldición familiar y una bestia demoníaca acechan en los sombríos páramos de Dartmoor. Una de las mayores obras maestras del suspense.",
                        category = "Suspense Gótico",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/2852.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1902"
                    ),
                    CuratedBook(
                        id = "com_orient_express",
                        title = "Asesinato en el Orient Express",
                        author = "Agatha Christie",
                        coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80",
                        summary = "El tren más lujoso del mundo queda atrapado por la nieve. Un pasajero yace apuñalado y Hércules Poirot debe descubrir al culpable antes de que vuelva a actuar.",
                        category = "Novela Policial",
                        isPublicDomain = false,
                        affiliateQrUrl = "https://www.amazon.es/dp/8467045437?tag=bookspread-21",
                        approximatePrice = "10,95 €",
                        year = "1934"
                    )
                )
            ),
            CuratorSection(
                id = "curator_cosmic",
                archetype = CuratorArchetype.COSMIC,
                name = CuratorArchetype.COSMIC.title,
                tagline = CuratorArchetype.COSMIC.subtitle,
                books = listOf(
                    CuratedBook(
                        id = "pub_guerra_mundos",
                        title = "La Guerra de los Mundos",
                        author = "H. G. Wells",
                        coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80",
                        summary = "Cilindros de metal caen del cielo y trípodes gigantescos siembran el pánico con rayos calóricos y humo negro. La invasión marciana definitiva.",
                        category = "Ciencia Ficción",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/36.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1898"
                    ),
                    CuratedBook(
                        id = "pub_viaje_luna",
                        title = "De la Tierra a la Luna",
                        author = "Julio Verne",
                        coverUrl = "https://images.unsplash.com/photo-1532693322450-2cb5c511067d?w=600&auto=format&fit=crop&q=80",
                        summary = "Tras la Guerra de Secesión, los artilleros del Gun-Club de Baltimore emprenden el desafío de disparar un proyectil tripulado hacia la Luna.",
                        category = "Aventura Científica",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/83.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1865"
                    ),
                    CuratedBook(
                        id = "com_dune",
                        title = "Dune",
                        author = "Frank Herbert",
                        coverUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=600&auto=format&fit=crop&q=80",
                        summary = "En el planeta desértico Arrakis, la codicia por la especia melange desata una guerra dinástica que forjará el destino del joven Paul Atreides.",
                        category = "Ciencia Ficción Épica",
                        isPublicDomain = false,
                        affiliateQrUrl = "https://www.amazon.es/dp/8466353774?tag=bookspread-21",
                        approximatePrice = "12,95 €",
                        year = "1965"
                    )
                )
            ),
            CuratorSection(
                id = "curator_prodigy",
                archetype = CuratorArchetype.PRODIGY,
                name = CuratorArchetype.PRODIGY.title,
                tagline = CuratorArchetype.PRODIGY.subtitle,
                books = listOf(
                    CuratedBook(
                        id = "pub_arte_guerra",
                        title = "El Arte de la Guerra",
                        author = "Sun Tzu",
                        coverUrl = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=600&auto=format&fit=crop&q=80",
                        summary = "Tratado militar milenario sobre estrategia, liderazgo y psicología. El mayor arte consiste en someter al enemigo sin librar batalla.",
                        category = "Estrategia y Filosofía",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/132.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "Siglo V a.C."
                    ),
                    CuratedBook(
                        id = "pub_meditaciones",
                        title = "Meditaciones",
                        author = "Marco Aurelio",
                        coverUrl = "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=600&auto=format&fit=crop&q=80",
                        summary = "Pensamientos íntimos del emperador filósofo sobre la virtud estoica, la serenidad interior y el gobierno de uno mismo ante la adversidad.",
                        category = "Filosofía Estoica",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/2680.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "180 d.C."
                    ),
                    CuratedBook(
                        id = "com_pensar_rapido",
                        title = "Pensar Rápido, Pensar Despacio",
                        author = "Daniel Kahneman",
                        coverUrl = "https://images.unsplash.com/photo-1499209974431-9dddcece7f88?w=600&auto=format&fit=crop&q=80",
                        summary = "Un recorrido magistral por la mente humana a través del Sistema 1 (rápido e intuitivo) y el Sistema 2 (lento y reflexivo) por el Premio Nobel de Economía.",
                        category = "Psicología y Decisión",
                        isPublicDomain = false,
                        affiliateQrUrl = "https://www.amazon.es/dp/8483068613?tag=bookspread-21",
                        approximatePrice = "19,90 €",
                        year = "2011"
                    )
                )
            )
        )
    }

    /**
     * Comprueba si una obra curada ya se encuentra descargada en la biblioteca de BookSpread.
     */
    fun isBookDownloaded(bookId: String, repository: BookRepository): Boolean {
        return repository.getCachedBooks().any { it.id == bookId }
    }

    /**
     * Descarga un libro de dominio público directamente al almacenamiento local de la TV
     * y lo registra de inmediato en la base de datos Room.
     */
    suspend fun downloadPublicDomainBook(
        context: Context,
        book: CuratedBook,
        repository: BookRepository
    ): Result<Book> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.filesDir, "book_${book.id}.epub")

            var downloaded = false
            if (!book.publicDownloadUrl.isNullOrBlank()) {
                try {
                    val url = URL(book.publicDownloadUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 15000
                    conn.instanceFollowRedirects = true
                    conn.connect()

                    if (conn.responseCode in 200..299) {
                        conn.inputStream.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (destFile.exists() && destFile.length() > 500) {
                            downloaded = true
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo descargar directo desde la URL externa: ${e.message}")
                }
            }

            // Fallback: Si no hay internet o falló la URL externa, generar una versión de cortesía
            if (!downloaded) {
                val noticeBook = EpubParser.getNoticeBook(
                    bookTitle = book.title,
                    message = "Obra maestra de dominio público («${book.title}» por ${book.author}). ${book.summary}\n\nDisfruta de esta edición especial en tu BookSpread."
                )
                // Guardar como marcador de posición
                destFile.writeText("BookSpread Public Edition: ${book.title}\n${book.author}\n${book.summary}")
            }

            val newBook = Book(
                id = book.id,
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                epubUrl = destFile.absolutePath,
                summary = book.summary,
                category = book.category,
                tags = listOf("Dominio Público", "Curaduría", book.category)
            )

            val current = repository.getCachedBooks().toMutableList()
            // Si ya existía, reemplazar; si no, agregar al inicio
            val existingIdx = current.indexOfFirst { it.id == book.id }
            if (existingIdx >= 0) {
                current[existingIdx] = newBook
            } else {
                current.add(0, newBook)
            }
            repository.saveCachedBooks(current)
            Result.success(newBook)
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando libro de dominio público", e)
            Result.failure(e)
        }
    }
}
