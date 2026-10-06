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
            val auth = CoverLoader.buildBasicAuth(config.username, config.password)
            var currentUrl = fileUrl
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
                android.util.Log.w("OpdsProvider", "HTTP $code downloading $currentUrl")
                return false
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("OpdsProvider", "Failed to download $fileUrl: ${e.message}", e)
            false
        }
    }
}
