package com.example.calibretv.data.provider

import android.content.Context
import com.example.calibretv.data.image.CoverLoader
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.model.ServerConfig
import com.example.calibretv.data.opds.OpdsClient
import com.example.calibretv.data.opds.SslHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 🌐 OpdsProvider — Proveedor para conexión opcional y avanzada a servidores Calibre y Calibre-Web.
 * Actúa como fuente secundaria sin bloquear el inicio local de la aplicación.
 */
class OpdsProvider(
    private val context: Context,
    private val configProvider: () -> ServerConfig
) : BookSourceProvider {

    override val sourceId: String = "opds_server"
    override val displayName: String = "Servidor Calibre-Web (OPDS)"

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val config = configProvider()
        config.serverUrl.isNotBlank() &&
                (config.serverUrl.startsWith("http://", ignoreCase = true) ||
                        config.serverUrl.startsWith("https://", ignoreCase = true))
    }

    override suspend fun fetchCatalog(): List<Book> = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext emptyList()
        val config = configProvider()
        val result = OpdsClient.fetchLibraryCatalog(
            serverUrl = config.serverUrl,
            username = config.username,
            password = config.password
        )
        if (result.isSuccess) {
            result.getOrNull()?.books ?: emptyList()
        } else {
            emptyList()
        }
    }

    override suspend fun resolveBookFile(book: Book): File? = withContext(Dispatchers.IO) {
        val epubUrl = book.epubUrl
        if (epubUrl.isNullOrBlank()) return@withContext null

        val isComic = epubUrl.endsWith(".cbz", ignoreCase = true) || epubUrl.endsWith(".cbr", ignoreCase = true)
        val isPdf = epubUrl.endsWith(".pdf", ignoreCase = true)
        val ext = when {
            isComic -> "cbz"
            isPdf -> "pdf"
            else -> "epub"
        }
        val cacheFile = File(context.cacheDir, "book_${book.id.hashCode()}.$ext")

        if (cacheFile.exists() && cacheFile.length() > 0) {
            return@withContext cacheFile
        }

        val downloaded = downloadRemoteFile(epubUrl, cacheFile)
        if (downloaded && cacheFile.exists() && cacheFile.length() > 0) {
            cacheFile
        } else {
            null
        }
    }

    private fun downloadRemoteFile(fileUrl: String, destFile: File): Boolean {
        return try {
            val config = configProvider()
            val url = URL(fileUrl)
            val conn = url.openConnection() as HttpURLConnection
            SslHelper.configureHttps(conn)
            conn.connectTimeout = 10000
            conn.readTimeout = 25000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "CalibroTV/3.0 (Android TV)")
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
