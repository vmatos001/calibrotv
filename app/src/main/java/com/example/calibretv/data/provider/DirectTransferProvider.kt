package com.example.calibretv.data.provider

import android.content.Context
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.storage.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 📲 DirectTransferProvider — Proveedor para libros transferidos por WiFi (P2P LocalSend style con QR).
 * Gestiona archivos almacenados directamente en filesDir sin necesidad de internet o servidores externos.
 */
class DirectTransferProvider(private val context: Context) : BookSourceProvider {

    override val sourceId: String = "direct_transfer"
    override val displayName: String = "Transferencia Directa WiFi"

    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()

    override suspend fun isAvailable(): Boolean = true

    override suspend fun fetchCatalog(): List<Book> = withContext(Dispatchers.IO) {
        val allBooks = bookDao.getAllBooks()
        // Filtrar obras importadas localmente por WiFi
        allBooks
            .filter { it.id.startsWith("local_wifi_") || it.tags.contains("WiFi") }
            .map { entity ->
                val tagList = try {
                    val arr = org.json.JSONArray(entity.tags)
                    (0 until arr.length()).map { arr.getString(it) }
                } catch (_: Exception) {
                    emptyList()
                }
                Book(
                    id = entity.id,
                    title = entity.title,
                    author = entity.author,
                    coverUrl = entity.coverUrl,
                    epubUrl = entity.epubUrl,
                    summary = entity.summary,
                    category = entity.category,
                    tags = tagList,
                    progressPercent = entity.progressPercent
                )
            }
    }

    override suspend fun resolveBookFile(book: Book): File? = withContext(Dispatchers.IO) {
        val epubUrl = book.epubUrl
        if (!epubUrl.isNullOrBlank()) {
            val directFile = File(epubUrl)
            if (directFile.exists() && directFile.length() > 0) {
                return@withContext directFile
            }
        }

        val destFileEpub = File(context.filesDir, "book_${book.id}.epub")
        if (destFileEpub.exists() && destFileEpub.length() > 0) {
            return@withContext destFileEpub
        }

        val destFileCbz = File(context.filesDir, "book_${book.id}.cbz")
        if (destFileCbz.exists() && destFileCbz.length() > 0) {
            return@withContext destFileCbz
        }

        val destFilePdf = File(context.filesDir, "book_${book.id}.pdf")
        if (destFilePdf.exists() && destFilePdf.length() > 0) {
            return@withContext destFilePdf
        }

        null
    }
}
