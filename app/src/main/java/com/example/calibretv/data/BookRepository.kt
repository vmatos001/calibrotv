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
import com.example.calibretv.data.provider.BookSourceProvider
import com.example.calibretv.data.provider.DirectTransferProvider
import com.example.calibretv.data.provider.LocalRoomProvider
import com.example.calibretv.data.provider.OpdsProvider
import com.example.calibretv.data.storage.AppDatabase
import com.example.calibretv.data.storage.BookEntity
import com.example.calibretv.data.storage.BookNoteEntity
import com.example.calibretv.data.storage.FavoriteEntity
import com.example.calibretv.data.storage.ReadingProgressEntity
import com.example.calibretv.data.storage.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class BookRepository(val context: Context) {
    private val prefs = PreferencesManager(context)
    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()
    private val progressDao = db.progressDao()
    private val favoriteDao = db.favoriteDao()
    private val bookNoteDao = db.bookNoteDao()

    private val _noteAddedEvents = MutableSharedFlow<BookNoteEntity>(extraBufferCapacity = 20)
    val noteAddedEvents = _noteAddedEvents.asSharedFlow()

    // Proveedores desacoplados de fuentes de libros (Multi-Source Pattern)
    val localProvider = LocalRoomProvider(context)
    val directTransferProvider = DirectTransferProvider(context)
    val opdsProvider = OpdsProvider(context) { getServerConfig() }

    val providers: List<BookSourceProvider> = listOf(
        localProvider,
        directTransferProvider,
        opdsProvider
    )

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

    fun isDarkTheme(): Boolean = prefs.isDarkTheme()
    fun setDarkTheme(isDark: Boolean) = prefs.setDarkTheme(isDark)

    fun getAppLanguage(): String = prefs.getAppLanguage()
    fun setAppLanguage(lang: String) = prefs.setAppLanguage(lang)

    fun getActiveProfile(): UserProfile = prefs.getActiveProfile()
    fun saveActiveProfile(profile: UserProfile) = prefs.saveActiveProfile(profile)
    fun getProfiles(): List<UserProfile> = prefs.getProfiles()
    fun saveProfiles(profiles: List<UserProfile>) = prefs.saveProfiles(profiles)
    fun createProfile(
        name: String,
        colorHex: String = "#FFA000",
        isKidsMode: Boolean = false,
        parentalPin: String? = null,
        preferredLanguage: String = "es"
    ): UserProfile = prefs.createProfile(name, colorHex, isKidsMode, parentalPin, preferredLanguage)
    fun updateProfile(updated: UserProfile) = prefs.updateProfile(updated)
    fun deleteProfile(profileId: String): Boolean = prefs.deleteProfile(profileId)
    fun awardStarToProfile(profileId: String, count: Int = 1): Int = prefs.awardStarToProfile(profileId, count)
    fun updateProfileWhitelist(profileId: String, whitelistBookIds: List<String>) = prefs.updateProfileWhitelist(profileId, whitelistBookIds)

    // Notas y Reseñas (Mobile Companion)
    fun addNote(
        bookId: String,
        text: String,
        spreadIndex: Int = 0,
        profileId: String = prefs.getActiveProfile().id
    ): BookNoteEntity = runBlocking(Dispatchers.IO) {
        val note = BookNoteEntity(
            id = "note_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}",
            bookId = bookId,
            profileId = profileId,
            noteText = text.trim(),
            spreadIndex = spreadIndex,
            createdAt = System.currentTimeMillis()
        )
        bookNoteDao.insertNote(note)
        _noteAddedEvents.tryEmit(note)
        note
    }

    fun getNotes(
        bookId: String,
        profileId: String = prefs.getActiveProfile().id
    ): List<BookNoteEntity> = runBlocking(Dispatchers.IO) {
        bookNoteDao.getNotes(bookId, profileId)
    }

    fun deleteNote(noteId: String) = runBlocking(Dispatchers.IO) {
        bookNoteDao.deleteNote(noteId)
    }

    @Volatile
    private var inMemoryBooks: List<Book>? = null

    private val favoriteIdsCache = java.util.concurrent.ConcurrentHashMap<String, MutableSet<String>>()

    fun isFavorite(bookId: String): Boolean {
        val profileId = prefs.getActiveProfile().id
        val cachedSet = favoriteIdsCache[profileId]
        if (cachedSet != null) {
            return cachedSet.contains(bookId)
        }
        return runBlocking(Dispatchers.IO) {
            val isFav = favoriteDao.isFavorite(profileId, bookId)
            val set = favoriteIdsCache.getOrPut(profileId) { java.util.concurrent.ConcurrentHashMap.newKeySet() }
            if (isFav) set.add(bookId) else set.remove(bookId)
            isFav
        }
    }

    fun toggleFavorite(bookId: String): Boolean {
        val profileId = prefs.getActiveProfile().id
        val set = favoriteIdsCache.getOrPut(profileId) { java.util.concurrent.ConcurrentHashMap.newKeySet() }
        val currentlyFav = if (set.contains(bookId)) true else {
            runBlocking(Dispatchers.IO) { favoriteDao.isFavorite(profileId, bookId) }
        }
        val newFav = !currentlyFav
        if (newFav) set.add(bookId) else set.remove(bookId)

        CoroutineScope(Dispatchers.IO).launch {
            if (newFav) {
                favoriteDao.add(FavoriteEntity(profileId, bookId))
            } else {
                favoriteDao.remove(profileId, bookId)
            }
        }
        return newFav
    }

    fun getFavoriteBookIds(): Set<String> {
        val profileId = prefs.getActiveProfile().id
        favoriteIdsCache[profileId]?.let { return it.toSet() }
        return runBlocking(Dispatchers.IO) {
            val favIds = favoriteDao.getFavoriteIds(profileId).toSet()
            val set = favoriteIdsCache.getOrPut(profileId) { java.util.concurrent.ConcurrentHashMap.newKeySet() }
            set.addAll(favIds)
            favIds
        }
    }

    fun getFavoriteBooks(): List<Book> {
        val profileId = prefs.getActiveProfile().id
        val favIds = getFavoriteBookIds()
        if (favIds.isEmpty()) return emptyList()
        val cached = inMemoryBooks
        if (cached != null) {
            return cached.filter { favIds.contains(it.id) }
        }
        return runBlocking(Dispatchers.IO) {
            bookDao.getBooksByIds(favIds.toList()).map { it.toBook() }
        }
    }

    fun getCachedBooks(): List<Book> {
        inMemoryBooks?.let { return it }
        return runBlocking(Dispatchers.IO) {
            val config = getServerConfig()
            val entities = bookDao.getAllBooks()

            // Si no hay servidor configurado, purgar cualquier portada/libro fantasma de prueba no descargado físicamente
            val validEntities = if (config.serverUrl.isBlank()) {
                val ghosts = entities.filter {
                    it.epubUrl?.contains("duckdns.org") == true && !File(context.filesDir, "book_${it.id}.epub").exists()
                }
                if (ghosts.isNotEmpty()) {
                    ghosts.forEach { bookDao.deleteBook(it.id) }
                    entities.filterNot { it.epubUrl?.contains("duckdns.org") == true && !File(context.filesDir, "book_${it.id}.epub").exists() }
                } else entities
            } else {
                entities
            }

            val books = if (validEntities.isEmpty()) {
                val legacy = prefs.getCachedBooks()
                if (legacy.isNotEmpty() && config.serverUrl.isNotBlank()) {
                    bookDao.upsertBooks(legacy.map { it.toEntity() })
                    legacy
                } else emptyList()
            } else {
                validEntities.map { it.toBook() }
            }
            inMemoryBooks = books
            books
        }
    }

    suspend fun saveCachedBooks(books: List<Book>) = withContext(Dispatchers.IO) {
        val unique = books.distinctBy { it.id.ifBlank { it.title } }
            .distinctBy { "${it.title.lowercase().trim()}_${it.author.lowercase().trim()}" }
        bookDao.clearAll()
        bookDao.upsertBooks(unique.map { it.toEntity() })
        inMemoryBooks = unique
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
                        val combined = (book.shelves + matchingShelves).distinct().filter { !it.equals("null", ignoreCase = true) }
                        book.copy(shelves = combined)
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
        if (config.serverUrl.isBlank()) {
            return@withContext Result.failure(Exception("No hay servidor configurado"))
        }

        // Si el usuario configuró explícitamente un endpoint REST API de personajes
        if (config.serverUrl.contains("/personajes") || config.serverUrl.contains("/api")) {
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

        // 3. Solo escanear el servidor si el usuario ha configurado una URL válida
        if (config.serverUrl.isNotBlank()) {
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
        }

        // 4. Si no hay libros ni servidor configurado, devolver catálogo vacío
        OpdsFeedContent(
            title = "Biblioteca Calibre",
            categories = listOf(OpdsCategory("cat_all", "Todos", "")),
            books = emptyList()
        )
    }

    /**
     * Resuelve el archivo físico del libro a través de la cadena de proveedores:
     * 1. LocalRoomProvider
     * 2. DirectTransferProvider (WiFi Import)
     * 3. OpdsProvider (Descarga remota en cacheDir)
     */
    suspend fun resolveBookFile(book: Book): File? = withContext(Dispatchers.IO) {
        for (provider in providers) {
            try {
                val file = provider.resolveBookFile(book)
                if (file != null && file.exists() && file.length() > 0) {
                    return@withContext file
                }
            } catch (_: Exception) {}
        }
        null
    }

    fun isSetupCompleted(): Boolean = prefs.isSetupCompleted()
    fun setSetupCompleted(completed: Boolean) = prefs.setSetupCompleted(completed)

    /**
     * Lazy EPUB Loader: Resuelve el archivo físico mediante la cadena de proveedores (Local, WiFi, OPDS)
     * y extrae capítulos, texto e ilustraciones para el Lector 3D.
     */
    suspend fun loadRawBook(book: Book): ParsedBook = withContext(Dispatchers.IO) {
        saveLastOpenedBook(book)

        val file = resolveBookFile(book)
        if (file == null || !file.exists() || file.length() == 0L) {
            return@withContext EpubParser.getNoticeBook(
                book.title,
                "No se pudo cargar el archivo del libro. Si fue transferido por WiFi o servidor, comprueba la conexión o el almacenamiento."
            )
        }

        return@withContext EpubParser.parseEpubToBook(file, book.title)
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
        val file = resolveBookFile(book)
        if (file == null || !file.exists() || file.length() == 0L) {
            return@withContext ComicParser.ParsedComic(book.title, emptyList())
        }
        ComicParser.parseCbz(file, context.cacheDir)
    }

    private fun downloadEpub(epubUrl: String, destFile: File): Boolean {
        return try {
            val config = getServerConfig()
            val auth = CoverLoader.buildBasicAuth(config.username, config.password)
            var currentUrl = epubUrl
            var redirects = 0
            var cookies: String? = null

            while (redirects < 6) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                SslHelper.configureHttps(conn)
                conn.connectTimeout = 15000
                conn.readTimeout = 45000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", "CalibroTV/3.0 (Android TV)")
                if (!auth.isNullOrBlank()) {
                    conn.setRequestProperty("Authorization", auth)
                }
                if (!cookies.isNullOrBlank()) {
                    conn.setRequestProperty("Cookie", cookies)
                }
                conn.connect()

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location") ?: return false
                    conn.getHeaderField("Set-Cookie")?.let { sc ->
                        cookies = sc.split(";").firstOrNull()
                    }
                    currentUrl = CoverLoader.resolveRedirectUrl(currentUrl, location)
                    redirects++
                    continue
                }

                if (code in 200..299) {
                    val tempFile = File(destFile.parentFile, "${destFile.name}.tmp")
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0L) {
                        if (destFile.exists()) destFile.delete()
                        tempFile.renameTo(destFile)
                        return true
                    }
                    return false
                }
                android.util.Log.w("BookRepository", "HTTP $code downloading $currentUrl")
                return false
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("BookRepository", "Failed to download $epubUrl: ${e.message}", e)
            false
        }
    }
}
