package com.example.calibretv.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

object CoverLoader {
    private const val TAG = "CoverLoader"
    private const val MAX_WIDTH_PX = 350
    private var appContext: Context? = null

    // Cache limited to 20MB to fit within Fire TV Stick 250MB limit
    private val memoryCache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    private fun getDiskCacheFile(urlStr: String): File? {
        val ctx = appContext ?: return null
        val coversDir = File(ctx.cacheDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()
        val hash = try {
            val md = MessageDigest.getInstance("MD5")
            md.digest(urlStr.toByteArray()).joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            urlStr.hashCode().toString()
        }
        return File(coversDir, "cov_$hash.jpg")
    }

    fun resolveRedirectUrl(currentUrl: String, location: String): String {
        return try {
            val currentUri = URI(currentUrl)
            currentUri.resolve(location).toString()
        } catch (_: Exception) {
            if (location.startsWith("http://") || location.startsWith("https://")) location
            else {
                val url = URL(currentUrl)
                val portPart = if (url.port != -1 && url.port != 80 && url.port != 443) ":${url.port}" else ""
                "${url.protocol}://${url.host}$portPart${if (location.startsWith("/")) "" else "/"}$location"
            }
        }
    }

    suspend fun loadCover(
        urlStr: String,
        authHeader: String? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (urlStr.isBlank()) return@withContext null

        if (urlStr.startsWith("/") || urlStr.startsWith("file://")) {
            val localPath = if (urlStr.startsWith("file://")) urlStr.removePrefix("file://") else urlStr
            return@withContext loadLocalImage(File(localPath))
        }

        memoryCache.get(urlStr)?.let { return@withContext it }

        // 1. Check persistent disk cache first
        val diskFile = getDiskCacheFile(urlStr)
        if (diskFile != null && diskFile.exists() && diskFile.length() > 0L) {
            val diskBmp = loadLocalImage(diskFile)
            if (diskBmp != null) {
                memoryCache.put(urlStr, diskBmp)
                return@withContext diskBmp
            }
        }

        // 2. Resolve effective basic auth header if none was passed
        val effectiveAuth = if (!authHeader.isNullOrBlank()) {
            authHeader
        } else {
            appContext?.let { ctx ->
                try {
                    val prefs = com.example.calibretv.data.storage.PreferencesManager(ctx)
                    val cfg = prefs.getServerConfig()
                    buildBasicAuth(cfg.username, cfg.password)
                } catch (_: Exception) { null }
            }
        }

        // 3. Network fetch with manual redirect loop preserving Authorization & Cookies
        try {
            var currentUrl = urlStr
            var redirects = 0
            var cookies: String? = null

            while (redirects < 5) {
                val url = URL(currentUrl)
                val connection = url.openConnection() as HttpURLConnection
                com.example.calibretv.data.opds.SslHelper.configureHttps(connection)
                connection.connectTimeout = 12000
                connection.readTimeout = 20000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV) CalibroTV/1.0")
                if (!effectiveAuth.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", effectiveAuth)
                }
                if (!cookies.isNullOrBlank()) {
                    connection.setRequestProperty("Cookie", cookies)
                }
                connection.connect()

                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: break
                    connection.getHeaderField("Set-Cookie")?.let { sc ->
                        cookies = sc.split(";").firstOrNull()
                    }
                    currentUrl = resolveRedirectUrl(currentUrl, location)
                    redirects++
                    continue
                }

                if (code in 200..299) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    if (bytes.isNotEmpty()) {
                        // Save to disk cache
                        diskFile?.let { df ->
                            try {
                                df.writeBytes(bytes)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed writing cover to disk cache: ${e.message}")
                            }
                        }

                        // Decode downsampled bitmap
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                        var sampleSize = 1
                        if (options.outWidth > MAX_WIDTH_PX) {
                            sampleSize = Math.round(options.outWidth.toFloat() / MAX_WIDTH_PX.toFloat())
                        }

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
                } else {
                    Log.w(TAG, "HTTP $code fetching cover for $currentUrl")
                }
                break
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading cover: ${e.message}")
        }
        return@withContext null
    }

    fun loadLocalImage(file: File, maxWidthPx: Int = 800): Bitmap? {
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
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        CoverLoader.init(context)
    }
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
fun rememberLocalImage(file: File?): ImageBitmap? {
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

