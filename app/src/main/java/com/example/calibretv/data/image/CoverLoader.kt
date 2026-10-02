package com.example.calibretv.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object CoverLoader {
    private const val MAX_WIDTH_PX = 350

    // Cache limited to 20MB to fit within Fire TV Stick 250MB limit
    private val memoryCache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    suspend fun loadCover(
        urlStr: String,
        authHeader: String? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (urlStr.isBlank()) return@withContext null

        if (urlStr.startsWith("/") || urlStr.startsWith("file://")) {
            val localPath = if (urlStr.startsWith("file://")) urlStr.removePrefix("file://") else urlStr
            return@withContext loadLocalImage(java.io.File(localPath))
        }

        memoryCache.get(urlStr)?.let { return@withContext it }

        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            com.example.calibretv.data.opds.SslHelper.configureHttps(connection)
            connection.connectTimeout = 6000
            connection.readTimeout = 10000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "CalibreTV/1.0")
            if (!authHeader.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", authHeader)
            }
            connection.connect()

            if (connection.responseCode in 200..299) {
                val bytes = connection.inputStream.use { it.readBytes() }
                
                // 1. Decode bounds first to calculate inSampleSize
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                // 2. Downsample so width <= 350 px
                var sampleSize = 1
                if (options.outWidth > MAX_WIDTH_PX) {
                    sampleSize = Math.round(options.outWidth.toFloat() / MAX_WIDTH_PX.toFloat())
                }

                // 3. Decode actual downsampled bitmap in RGB_565 (2 bytes per pixel) to save 50% memory
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = Math.max(1, sampleSize)
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                if (bitmap != null) {
                    memoryCache.put(urlStr, bitmap)
                }
                return@withContext bitmap
            }
        } catch (_: Exception) {
            // Ignored, return null on error
        }
        return@withContext null
    }

    fun loadLocalImage(file: java.io.File, maxWidthPx: Int = 800): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null
        val path = file.absolutePath
        memoryCache.get(path)?.let { return it }

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)

            var sampleSize = 1
            if (options.outWidth > maxWidthPx) {
                sampleSize = Math.round(options.outWidth.toFloat() / maxWidthPx.toFloat())
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = Math.max(1, sampleSize)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(path, decodeOptions)
            if (bitmap != null) {
                memoryCache.put(path, bitmap)
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun buildBasicAuth(username: String, pass: String): String? {
        if (username.isBlank() && pass.isBlank()) return null
        val credentials = "$username:$pass"
        return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }
}

@Composable
fun rememberCoverImage(url: String?, authHeader: String? = null): ImageBitmap? {
    if (url.isNullOrBlank()) return null
    val bitmapState = remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url, authHeader) {
        val bmp = CoverLoader.loadCover(url, authHeader)
        if (bmp != null) {
            bitmapState.value = bmp.asImageBitmap()
        }
    }
    return bitmapState.value
}

@Composable
fun rememberLocalImage(file: java.io.File?): ImageBitmap? {
    if (file == null || !file.exists()) return null
    val bitmapState = remember(file.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file.absolutePath) {
        val bmp = withContext(Dispatchers.IO) { CoverLoader.loadLocalImage(file) }
        if (bmp != null) {
            bitmapState.value = bmp.asImageBitmap()
        }
    }
    return bitmapState.value
}

