package com.example.calibretv.data

import android.content.Context
import com.example.calibretv.data.comic.ComicParser
import com.example.calibretv.data.epub.EpubParser
import com.example.calibretv.data.epub.PageSpread
import com.example.calibretv.data.epub.ParsedBook
import com.example.calibretv.data.image.CoverLoader
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.model.CalibreShelf
import com.example.calibretv.data.model.OpdsCategory
import com.example.calibretv.data.model.ReadingSettings
import com.example.calibretv.data.model.ServerConfig
import com.example.calibretv.data.model.UserProfile
import com.example.calibretv.data.opds.OpdsClient
import com.example.calibretv.data.opds.OpdsFeedContent
import com.example.calibretv.data.opds.SslHelper
import com.example.calibretv.data.storage.AppDatabase
import com.example.calibretv.data.storage.BookEntity
import com.example.calibretv.data.storage.FavoriteEntity
import com.example.calibretv.data.storage.ReadingProgressEntity
import com.example.calibretv.data.storage.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class BookRepository(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()
    private val progressDao = db.progressDao()
    private val favoriteDao = db.favoriteDao()

    private fun BookEntity.toBook(): Book {
        val tagList = try {
            val arr = org.json.JSONArray(tags)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
        val shelfList = try {
            val arr = org.json.JSONArray(shelves)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
        return Book(
            id = id,
            title = title,
            author = author,
            coverUrl = coverUrl,
            epubUrl = epubUrl,
            summary = summary,
            category = category,
            tags = tagList,
            shelves = shelfList,
            progressPercent = progressPercent
        )
    }

    private fun Book.toEntity(lastReadSpread: Int = 0): BookEntity {
        val tagsJson = org.json.JSONArray(tags).toString()
        val shelvesJson = org.json.JSONArray(shelves).toString()
        return BookEntity(
            id = id,
            title = title,
            author = author,
            coverUrl = coverUrl,
            epubUrl = epubUrl,
            summary = summary,
            category = category,
            tags = tagsJson,
            shelves = shelvesJson,
            progressPercent = progressPercent,
            lastReadSpread = lastReadSpread
        )
    }

    fun getServerConfig(): ServerConfig = prefs.getServerConfig()
    fun saveServerConfig(config: ServerConfig) = prefs.saveServerConfig(config)

    fun getReadingSettings(): ReadingSettings = prefs.getReadingSettings()
    fun saveReadingSettings(settings: ReadingSettings) = prefs.saveReadingSettings(settings)

    fun getActiveProfile(): UserProfile = prefs.getActiveProfile()
    fun saveActiveProfile(profile: UserProfile) = prefs.saveActiveProfile(profile)
    fun getProfiles(): List<UserProfile> = prefs.getProfiles()
    fun saveProfiles(profiles: List<UserProfile>) = prefs.saveProfiles(profiles)
    fun createProfile(name: String, colorHex: String = "#FFA000"): UserProfile = prefs.createProfile(name, colorHex)

    fun isFavorite(bookId: String): Boolean = runBlocking(Dispatchers.IO) {
        favoriteDao.isFavorite(prefs.getActiveProfile().id, bookId)
    }

    fun toggleFavorite(bookId: String): Boolean = runBlocking(Dispatchers.IO) {
        val profileId = prefs.getActiveProfile().id
        val isFav = favoriteDao.isFavorite(profileId, bookId)
        if (isFav) {
            favoriteDao.remove(profileId, bookId)
            false
        } else {
            favoriteDao.add(FavoriteEntity(profileId, bookId))
            true
        }
    }

    fun getFavoriteBookIds(): Set<String> = runBlocking(Dispatchers.IO) {
        favoriteDao.getFavoriteIds(prefs.getActiveProfile().id).toSet()
    }

    fun getFavoriteBooks(): List<Book> = runBlocking(Dispatchers.IO) {
        val favIds = favoriteDao.getFavoriteIds(prefs.getActiveProfile().id)
        if (favIds.isEmpty()) emptyList() else bookDao.getBooksByIds(favIds).map { it.toBook() }
    }

    fun getCachedBooks(): List<Book> = runBlocking(Dispatchers.IO) {
        val entities = bookDao.getAllBooks()
        if (entities.isEmpty()) {
            val legacy = prefs.getCachedBooks()
            if (legacy.isNotEmpty()) {
                bookDao.upsertBooks(legacy.map { it.toEntity() })
                return@runBlocking legacy
            }
        }
        entities.map { it.toBook() }
    }

    suspend fun saveCachedBooks(books: List<Book>) = withContext(Dispatchers.IO) {
        bookDao.upsertBooks(books.map { it.toEntity() })
    }

    fun getBookProgress(bookId: String): Int = runBlocking(Dispatchers.IO) {
        progressDao.getSpreadIndex(prefs.getActiveProfile().id, bookId) ?: prefs.getBookProgress(bookId)
    }

    fun getBookProgressPercent(bookId: String): Int = runBlocking(Dispatchers.IO) {
        progressDao.getPercent(prefs.getActiveProfile().id, bookId) ?: prefs.getBookProgressPercent(bookId)
    }

    fun saveBookProgress(bookId: String, spreadIndex: Int, percent: Int = 0) {
        val profileId = prefs.getActiveProfile().id
        prefs.saveBookProgress(bookId, spreadIndex, percent)
        CoroutineScope(Dispatchers.IO).launch {
            progressDao.upsert(
                ReadingProgressEntity(
                    profileId = profileId,
                    bookId = bookId,
                    spreadIndex = spreadIndex,
                    percent = percent,
                    lastReadAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun getLastOpenedBook(): Book? = prefs.getLastOpenedBook()
    fun saveLastOpenedBook(book: Book) = prefs.saveLastOpenedBook(book)

    private var cachedShelves: List<CalibreShelf> = emptyList()

    fun getShelves(): List<CalibreShelf> = cachedShelves

    suspend fun loadAndApplyShelves(config: ServerConfig): List<CalibreShelf> = withContext(Dispatchers.IO) {
        val apiResult = com.example.calibretv.data.api.ApiClient.fetchShelves()
        if (apiResult.isSuccess && apiResult.getOrNull()!!.isNotEmpty()) {
            val shelves = apiResult.getOrNull()!!
            cachedShelves = shelves
            return@withContext shelves
        }
        val result = OpdsClient.fetchShelves(config.serverUrl, config.username, config.password)
        if (result.isSuccess) {
            val shelves = result.getOrNull() ?: emptyList()
            cachedShelves = shelves

            if (shelves.isNotEmpty()) {
                val currentBooks = getCachedBooks()
                val updatedBooks = currentBooks.map { book ->
                    val matchingShelves = shelves.filter { it.bookIds.contains(book.id) }.map { it.name }
                    if (matchingShelves.isNotEmpty()) {
                        book.copy(shelves = matchingShelves)
                    } else {
                        book
                    }
                }
                saveCachedBooks(updatedBooks)
            }
            shelves
        } else {
            emptyList()
        }
    }

    suspend fun getOrFetchBookDescription(book: Book): String = withContext(Dispatchers.IO) {
        // 1. Si la descripción ya es texto real de Calibre-Web, la retornamos inmediatamente
        if (book.summary.isNotBlank() && book.summary.length > 25 &&
            !book.summary.startsWith("Obra de", ignoreCase = true) &&
            !book.summary.contains("Sin descripción", ignoreCase = true)
        ) {
            return@withContext book.summary
        }

        // 2. Consulta al endpoint individual de detalles de Calibre-Web (/opds/book/{id})
        val config = getServerConfig()
        if (config.serverUrl.isNotBlank()) {
            val fetchedSynopsis = OpdsClient.fetchBookDetailSynopsis(config.serverUrl, book.id, config.username, config.password)
            if (!fetchedSynopsis.isNullOrBlank() && fetchedSynopsis.length > 15 && !fetchedSynopsis.startsWith("Obra de", ignoreCase = true)) {
                val updatedBook = book.copy(summary = fetchedSynopsis)
                saveCachedBooks(listOf(updatedBook))
                return@withContext fetchedSynopsis
            }
        }

        // 3. Extraer descripción del archivo EPUB si ya se encuentra descargado
        val cacheFile = File(context.cacheDir, "book_${book.id.hashCode()}.epub")
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            val internalDesc = EpubParser.extractDescription(cacheFile)
            if (!internalDesc.isNullOrBlank() && internalDesc.length > 15) {
                val updatedBook = book.copy(summary = internalDesc)
                saveCachedBooks(listOf(updatedBook))
                return@withContext internalDesc
            }
        }

        // 4. Fallback generado si no hay sinopsis disponible en el servidor
        val tagsToUse = book.shelves.ifEmpty { book.tags }
        OpdsClient.buildSmartDescription(book.title, book.author, book.category, tagsToUse)
    }

    /**
     * Connects to the OPDS server, verifies credentials, scans for all books and saves metadata.
     * Does NOT download EPUBs (protects TV memory).
     * Performs incremental comparison preserving local reading progress.
     */
    suspend fun scanServerLibrary(config: ServerConfig): Result<OpdsFeedContent> = withContext(Dispatchers.IO) {
        // Try REST API first
        val apiBooksRes = com.example.calibretv.data.api.ApiClient.fetchBooks(500)
        if (apiBooksRes.isSuccess && apiBooksRes.getOrNull()!!.isNotEmpty()) {
            val books = apiBooksRes.getOrNull()!!
            saveServerConfig(config)
            val mergedBooks = books.map { newBook ->
                val savedPct = getBookProgressPercent(newBook.id)
                if (savedPct > 0) newBook.copy(progressPercent = savedPct) else newBook
            }
            saveCachedBooks(mergedBooks)
            try {
                loadAndApplyShelves(config)
            } catch (_: Exception) {}
            return@withContext Result.success(OpdsFeedContent(title = "Biblioteca", categories = emptyList(), books = mergedBooks))
        }

        val scanResult = OpdsClient.fetchLibraryCatalog(
            serverUrl = config.serverUrl,
            username = config.username,
            password = config.password
        )

        if (scanResult.isSuccess) {
            val feed = scanResult.getOrNull()!!
            saveServerConfig(config)
            if (feed.books.isNotEmpty()) {
                // Incremental sync: Preserve user reading progress for existing books
                val mergedBooks = feed.books.map { newBook ->
                    val savedPct = getBookProgressPercent(newBook.id)
                    if (savedPct > 0) newBook.copy(progressPercent = savedPct) else newBook
                }
                saveCachedBooks(mergedBooks)

                // Cargar estanterías (shelves) en segundo plano
                try {
                    loadAndApplyShelves(config)
                } catch (_: Exception) {}

                Result.success(feed.copy(books = mergedBooks))
            } else {
                Result.success(feed)
            }
        } else {
            scanResult
        }
    }

    suspend fun getFeed(targetUrl: String? = null): OpdsFeedContent = withContext(Dispatchers.IO) {
        val config = getServerConfig()

        // 1. If explicit subfeed target requested
        if (!targetUrl.isNullOrBlank() && (targetUrl.startsWith("http://") || targetUrl.startsWith("https://"))) {
            val result = OpdsClient.fetchFeed(targetUrl, config.username, config.password)
            if (result.isSuccess) {
                val feed = result.getOrNull()!!
                val annotated = feed.books.map { b ->
                    val realPct = getBookProgressPercent(b.id)
                    b.copy(progressPercent = if (realPct > 0) realPct else b.progressPercent)
                }
                return@withContext feed.copy(books = annotated)
            }
        }

        // 2. Load from cached library if available (Lightweight metadata - covers & synopses)
        val cached = getCachedBooks()
        if (cached.isNotEmpty()) {
            val allTags = cached.flatMap { it.tags.ifEmpty { listOf(it.category) } }
                .distinct()
                .filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) }

            val categories = mutableListOf(OpdsCategory("cat_all", "Todos", ""))
            allTags.forEachIndexed { idx, tag ->
                categories.add(OpdsCategory("cat_$idx", tag, ""))
            }

            val annotated = cached.map { b ->
                val realPct = getBookProgressPercent(b.id)
                b.copy(progressPercent = if (realPct > 0) realPct else b.progressPercent)
            }
            return@withContext OpdsFeedContent(
                title = "Biblioteca Calibre",
                categories = categories,
                books = annotated
            )
        }

        // 3. If no cached books, scan via ApiClient / Server
        val scanResult = scanServerLibrary(config)
        if (scanResult.isSuccess) {
            val feed = scanResult.getOrNull()!!
            if (feed.books.isNotEmpty()) {
                val annotated = feed.books.map { b ->
                    val realPct = getBookProgressPercent(b.id)
                    b.copy(progressPercent = if (realPct > 0) realPct else b.progressPercent)
                }
                return@withContext feed.copy(books = annotated)
            }
        }

        // 4. No fake mockup data: Return empty feed if not configured or empty
        OpdsFeedContent(
            title = "Biblioteca Calibre",
            categories = listOf(OpdsCategory("cat_all", "Todos", "")),
            books = emptyList()
        )
    }

    fun isSetupCompleted(): Boolean = prefs.isSetupCompleted()
    fun setSetupCompleted(completed: Boolean) = prefs.setSetupCompleted(completed)

    /**
     * Lazy EPUB Loader: Downloads the EPUB file to TV cache ONLY when the user clicks 'Leer en 3D'.
     * Returns structured ParsedBook containing chapters, text blocks and extracted images.
     */
    suspend fun loadRawBook(book: Book): ParsedBook = withContext(Dispatchers.IO) {
        saveLastOpenedBook(book)
        val epubUrl = book.epubUrl
        if (epubUrl.isNullOrBlank()) {
            return@withContext EpubParser.getNoticeBook(
                book.title,
                "Este título no cuenta con archivo EPUB descargable en el servidor."
            )
        }

        val cacheFile = File(context.cacheDir, "book_${book.id.hashCode()}.epub")
        if (!cacheFile.exists() || cacheFile.length() == 0L) {
            val downloaded = downloadEpub(epubUrl, cacheFile)
            if (!downloaded) {
                return@withContext EpubParser.getNoticeBook(
                    book.title,
                    "Error al descargar el libro desde el servidor Calibre-Web. Verifica tu conexión de red o permisos."
                )
            }
        }

        return@withContext EpubParser.parseEpubToBook(cacheFile, book.title)
    }

    suspend fun loadBookSpreads(
        book: Book,
        fontSizeSp: Int = 18,
        overscanPercent: Int = 0
    ): List<PageSpread> = withContext(Dispatchers.IO) {
        val raw = loadRawBook(book)
        return@withContext EpubParser.paginate(raw, fontSizeSp, overscanPercent)
    }

    suspend fun loadComic(book: Book): ComicParser.ParsedComic = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "book_${book.id.hashCode()}.cbz")
        if (!cacheFile.exists() || cacheFile.length() == 0L) {
            val url = book.epubUrl ?: return@withContext ComicParser.ParsedComic(book.title, emptyList())
            val downloaded = downloadEpub(url, cacheFile)
            if (!downloaded) {
                return@withContext ComicParser.ParsedComic(book.title, emptyList())
            }
        }
        ComicParser.parseCbz(cacheFile, context.cacheDir)
    }

    private fun downloadEpub(epubUrl: String, destFile: File): Boolean {
        return try {
            val config = getServerConfig()
            val url = URL(epubUrl)
            val conn = url.openConnection() as HttpURLConnection
            SslHelper.configureHttps(conn)
            conn.connectTimeout = 10000
            conn.readTimeout = 25000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "CalibreTV/1.0 (Android TV)")
            val auth = CoverLoader.buildBasicAuth(config.username, config.password)
            if (auth != null) conn.setRequestProperty("Authorization", auth)
            conn.connect()

            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
