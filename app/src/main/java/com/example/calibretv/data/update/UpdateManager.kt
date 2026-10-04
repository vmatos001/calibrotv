package com.example.calibretv.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object UpdateManager {

    private const val TAG = "CalibroTV-Update"
    private const val GITHUB_REPO = "vmatos001/calibrotv"
    private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 CalibroTV"

    data class ReleaseInfo(
        val tagName: String,
        val title: String,
        val changelog: String,
        val apkUrl: String,
        val apkName: String,
        val apkSizeBytes: Long
    )

    sealed class CheckResult {
        data class UpdateAvailable(val release: ReleaseInfo, val currentVersion: String) : CheckResult()
        data class UpToDate(val currentVersion: String) : CheckResult()
        data class Error(val message: String) : CheckResult()
    }

    sealed class DownloadResult {
        data class Success(val apkFile: File) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getCurrentVersion(context: Context): Pair<String, Long> {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val vName = pInfo.versionName ?: "2.0"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            Pair(vName, vCode)
        } catch (_: Exception) {
            Pair("2.0", 4L)
        }
    }

    suspend fun checkForUpdate(context: Context): CheckResult = withContext(Dispatchers.IO) {
        try {
            val (currentVersion, _) = getCurrentVersion(context)
            val request = Request.Builder()
                .url(RELEASES_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", USER_AGENT)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext CheckResult.Error("No se pudo conectar con GitHub (HTTP ${response.code})")
            }

            val jsonStr = response.body?.string() ?: return@withContext CheckResult.Error("Respuesta vacía del servidor.")
            val root = JSONObject(jsonStr)

            val tagName = root.optString("tag_name", "")
            val title = root.optString("name", tagName)
            val changelog = root.optString("body", "Mejoras y correcciones generales.")

            val assets = root.optJSONArray("assets")
            var apkUrl = ""
            var apkName = "CalibroTV-$tagName.apk"
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isBlank()) {
                return@withContext CheckResult.Error("La última versión no contiene un archivo APK adjunto.")
            }

            if (isNewerVersion(currentVersion, tagName)) {
                CheckResult.UpdateAvailable(
                    ReleaseInfo(
                        tagName = tagName,
                        title = title,
                        changelog = changelog,
                        apkUrl = apkUrl,
                        apkName = apkName,
                        apkSizeBytes = apkSize
                    ),
                    currentVersion = currentVersion
                )
            } else {
                CheckResult.UpToDate(currentVersion = currentVersion)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking update: ${e.message}", e)
            CheckResult.Error(e.message ?: "Error al verificar actualizaciones.")
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.removePrefix("v").trim()
        val cleanLatest = latest.removePrefix("v").trim()

        val currParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currParts.size, lateParts.size)
        for (i in 0 until maxLen) {
            val c = currParts.getOrElse(i) { 0 }
            val l = lateParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun downloadApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val safeTag = release.tagName.replace('/', '_').replace('\\', '_')
        val destFile = File(cacheDir, "CalibroTV_${safeTag}.apk")
        val tmpFile = File(cacheDir, "CalibroTV_${safeTag}.tmp")

        try {
            if (tmpFile.exists()) tmpFile.delete()
            if (destFile.exists()) destFile.delete()

            // Intento 1: OkHttp con followRedirects nativo
            val okHttpSuccess = downloadWithOkHttp(release, tmpFile, onProgress)

            // Si OkHttp falló por cualquier motivo, fallback a HttpURLConnection con followRedirects nativo
            val finalSuccess = if (okHttpSuccess) {
                true
            } else {
                Log.w(TAG, "OkHttp download failed or incomplete, falling back to HttpURLConnection...")
                downloadWithHttpUrlConnection(release, tmpFile, onProgress)
            }

            if (!finalSuccess || !tmpFile.exists() || tmpFile.length() <= 1024L) {
                return@withContext DownloadResult.Error("No se pudo completar la descarga del archivo.")
            }

            // Renombrar archivo temporal al APK final
            if (destFile.exists()) destFile.delete()
            if (!tmpFile.renameTo(destFile)) {
                // Fallback copy si rename falla entre filesystems
                tmpFile.copyTo(destFile, overwrite = true)
                tmpFile.delete()
            }

            destFile.setReadable(true, false)

            // 🔐 Verificación de integridad: paquete, versión y certificado de firma
            val verifyError = verifyApk(context, destFile)
            if (verifyError != null) {
                Log.e(TAG, "APK rechazado: $verifyError")
                destFile.delete()
                return@withContext DownloadResult.Error(verifyError)
            }

            Log.i(TAG, "APK descargado y verificado: ${destFile.absolutePath} (${destFile.length()} bytes)")
            DownloadResult.Success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error in downloadApk: ${e.message}", e)
            DownloadResult.Error(e.message ?: "Error desconocido durante la descarga.")
        } finally {
            if (tmpFile.exists()) tmpFile.delete()
        }
    }

    private fun downloadWithOkHttp(
        release: ReleaseInfo,
        targetFile: File,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Boolean {
        return try {
            val request = Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                .header("Accept-Encoding", "identity")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "OkHttp download error HTTP ${response.code}")
                return false
            }

            val body = response.body ?: return false
            val contentLength = body.contentLength()
            val totalBytes = if (contentLength > 0) contentLength else if (release.apkSizeBytes > 0) release.apkSizeBytes else 12500000L

            saveStreamToFile(body.byteStream(), targetFile, totalBytes, onProgress)
        } catch (e: Exception) {
            Log.w(TAG, "OkHttp exception: ${e.message}")
            false
        }
    }

    private fun downloadWithHttpUrlConnection(
        release: ReleaseInfo,
        targetFile: File,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Boolean {
        return try {
            val url = URL(release.apkUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            conn.setRequestProperty("Accept-Encoding", "identity")
            conn.connect()

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                Log.w(TAG, "HttpURLConnection returned $responseCode")
                return false
            }

            val contentLength = conn.contentLengthLong
            val totalBytes = if (contentLength > 0) contentLength else if (release.apkSizeBytes > 0) release.apkSizeBytes else 12500000L

            saveStreamToFile(conn.inputStream, targetFile, totalBytes, onProgress)
        } catch (e: Exception) {
            Log.w(TAG, "HttpURLConnection exception: ${e.message}")
            false
        }
    }

    private fun saveStreamToFile(
        input: InputStream,
        targetFile: File,
        totalBytes: Long,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Boolean {
        var downloadedBytes = 0L
        var lastReportTime = 0L
        var lastReportedPercent = -1

        input.use { inStream ->
            FileOutputStream(targetFile).use { outStream ->
                val buffer = ByteArray(65536) // 64 KB para alta velocidad y bajo overhead
                var read: Int
                while (inStream.read(buffer).also { read = it } != -1) {
                    outStream.write(buffer, 0, read)
                    downloadedBytes += read

                    val percent = if (totalBytes > 0) {
                        ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }

                    val now = System.currentTimeMillis()
                    // Throttle: reportar cada 100ms o cuando sube el porcentaje
                    if (percent != lastReportedPercent || now - lastReportTime >= 100) {
                        lastReportedPercent = percent
                        lastReportTime = now
                        onProgress(percent, downloadedBytes, totalBytes)
                    }
                }
                outStream.flush()
            }
        }
        // Progreso final 100%
        onProgress(100, downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes)
        return downloadedBytes > 1024L
    }

    /**
     * Verifica que el APK descargado sea legítimo antes de instalarlo:
     * 1. Mismo applicationId que la app instalada.
     * 2. versionCode estrictamente mayor (evita downgrades).
     * 3. Certificado de firma idéntico (SHA-256) al de la app instalada.
     * @return null si es válido, o un mensaje de error legible.
     */
    @Suppress("DEPRECATION")
    fun verifyApk(context: Context, apkFile: File): String? {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
                ?: return "El archivo descargado no es un APK válido."
            archiveInfo.applicationInfo?.let {
                it.sourceDir = apkFile.absolutePath
                it.publicSourceDir = apkFile.absolutePath
            }

            if (archiveInfo.packageName != context.packageName) {
                return "El APK pertenece a otra aplicación (${archiveInfo.packageName})."
            }

            val (_, currentCode) = getCurrentVersion(context)
            val newCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                archiveInfo.longVersionCode else archiveInfo.versionCode.toLong()
            if (newCode <= currentCode) {
                return "La versión descargada ($newCode) no es más reciente que la instalada ($currentCode)."
            }

            val installedInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(context.packageName, flags)
            }

            val newCerts = signatureDigests(archiveInfo)
            val installedCerts = signatureDigests(installedInfo)
            if (newCerts.isEmpty() || newCerts != installedCerts) {
                return "La firma del APK no coincide con la app instalada. Actualización bloqueada por seguridad."
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "verifyApk error: ${e.message}", e)
            "No se pudo verificar la integridad del APK."
        }
    }

    @Suppress("DEPRECATION")
    private fun signatureDigests(info: android.content.pm.PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val si = info.signingInfo ?: return emptySet()
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        } else {
            info.signatures
        } ?: return emptySet()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return sigs.map { sig -> md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK: ${e.message}", e)
        }
    }
}
