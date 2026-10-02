package com.example.calibretv.data.provider

import android.content.Context
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.storage.AppDatabase
import com.example.calibretv.data.storage.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 🏠 LocalRoomProvider — Proveedor de libros y progreso persistidos en base de datos local (Room).
 * Permite que CalibroTV funcione 100% offline sin conexión de red.
 */
class LocalRoomProvider(private val context: Context) : BookSourceProvider {

    override val sourceId: String = "local_room"
    override val displayName: String = "Almacenamiento Local"

    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()

    override suspend fun isAvailable(): Boolean = true

    override suspend fun fetchCatalog(): List<Book> = withContext(Dispatchers.IO) {
        val entities = bookDao.getAllBooks()
        entities.map { it.toBook() }
    }

    override suspend fun resolveBookFile(book: Book): File? = withContext(Dispatchers.IO) {
        // 1. Si epubUrl es una ruta de archivo local absoluta existente
        val url = book.epubUrl
        if (!url.isNullOrBlank() && !url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            val file = File(url)
            if (file.exists() && file.length() > 0) {
                return@withContext file
            }
        }

        // 2. Comprobar en archivos internos de la app (filesDir)
        val filesDirFile = File(context.filesDir, "book_${book.id}.epub")
        if (filesDirFile.exists() && filesDirFile.length() > 0) {
            return@withContext filesDirFile
        }

        val filesDirCbz = File(context.filesDir, "book_${book.id}.cbz")
        if (filesDirCbz.exists() && filesDirCbz.length() > 0) {
            return@withContext filesDirCbz
        }

        val filesDirPdf = File(context.filesDir, "book_${book.id}.pdf")
        if (filesDirPdf.exists() && filesDirPdf.length() > 0) {
            return@withContext filesDirPdf
        }

        // 3. Comprobar en el caché de la app (cacheDir)
        val cacheEpub = File(context.cacheDir, "book_${book.id.hashCode()}.epub")
        if (cacheEpub.exists() && cacheEpub.length() > 0) {
            return@withContext cacheEpub
        }

        val cacheCbz = File(context.cacheDir, "book_${book.id.hashCode()}.cbz")
        if (cacheCbz.exists() && cacheCbz.length() > 0) {
            return@withContext cacheCbz
        }

        val cachePdf = File(context.cacheDir, "book_${book.id.hashCode()}.pdf")
        if (cachePdf.exists() && cachePdf.length() > 0) {
            return@withContext cachePdf
        }

        null
    }

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
}
