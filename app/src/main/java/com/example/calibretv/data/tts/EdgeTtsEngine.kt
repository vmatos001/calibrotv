package com.example.calibretv.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.*
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Motor de síntesis vocal neuronal de alta fidelidad basado en el protocolo abierto
 * de lectura en voz alta de Microsoft Edge (Edge TTS / Azure Neural Voices).
 * 100% gratuito, sin API Key ni registro.
 */
class EdgeTtsEngine(private val context: Context) {

    private val TAG = "EdgeTTS"
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var activeMediaPlayer: MediaPlayer? = null
    private var activeTempFile: File? = null
    private var activeWebSocket: WebSocket? = null

    companion object {
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val WIN_EPOCH = 11644473600L
        private const val CHROMIUM_VERSION = "143.0.3650.75"

        fun generateSecMsGec(): String {
            val nowUnix = System.currentTimeMillis() / 1000L
            var ticks = nowUnix + WIN_EPOCH
            ticks -= (ticks % 300L)
            ticks *= 10000000L // 10^7 (intervalos de 100 ns en Windows FileTime)

            val strToHash = "${ticks}$TRUSTED_CLIENT_TOKEN"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(strToHash.toByteArray(Charsets.US_ASCII))
            return digest.joinToString("") { "%02X".format(it) }
        }

        fun getVoiceForLanguage(lang: String): String {
            return if (lang.equals("en", ignoreCase = true)) {
                "en-US-AvaNeural"
            } else {
                "es-MX-DaliaNeural"
            }
        }
    }

    suspend fun generateAudio(
        text: String,
        speedRate: Float = 1.0f,
        pitch: Float = 1.0f,
        voice: String = "es-MX-DaliaNeural"
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null

        val secMsGec = generateSecMsGec()
        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val wssUrl = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                "&Sec-MS-GEC=$secMsGec" +
                "&Sec-MS-GEC-Version=1-$CHROMIUM_VERSION" +
                "&ConnectionId=$connectionId"

        val acceptLang = if (voice.startsWith("en", ignoreCase = true)) "en-US,en;q=0.9" else "es-MX,es;q=0.9,en;q=0.8"
        val request = Request.Builder()
            .url(wssUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
            .addHeader("Accept-Encoding", "gzip, deflate, br, zstd")
            .addHeader("Accept-Language", acceptLang)
            .addHeader("Pragma", "no-cache")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .build()

        val deferred = CompletableDeferred<ByteArray?>()
        val audioStream = ByteArrayOutputStream()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                try {
                    val dateHeader = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (z)", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())

                    // 1. Configuración de formato de audio (MP3 mono 24kHz 48kbps)
                    val configPayload = "X-Timestamp:$dateHeader\r\n" +
                            "Content-Type:application/json; charset=utf-8\r\n" +
                            "Path:speech.config\r\n\r\n" +
                            "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"
                    webSocket.send(configPayload)

                    // 2. SSML
                    val ratePercent = ((speedRate - 1.0f) * 100).toInt()
                    val rateStr = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
                    val pitchHz = ((pitch - 1.0f) * 50).toInt()
                    val pitchStr = if (pitchHz >= 0) "+${pitchHz}Hz" else "${pitchHz}Hz"

                    val escapedText = text
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&apos;")

                    val requestId = UUID.randomUUID().toString().replace("-", "")
                    val langCode = if (voice.startsWith("en", ignoreCase = true)) "en-US" else "es-MX"
                    val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$langCode'>" +
                            "<voice name='$voice'>" +
                            "<prosody pitch='$pitchStr' rate='$rateStr'>" +
                            escapedText +
                            "</prosody></voice></speak>"

                    val ssmlPayload = "X-RequestId:$requestId\r\n" +
                            "Content-Type:application/ssml+xml\r\n" +
                            "X-Timestamp:$dateHeader\r\n" +
                            "Path:ssml\r\n\r\n" +
                            ssml
                    webSocket.send(ssmlPayload)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending Edge TTS payloads", e)
                    deferred.complete(null)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (bytes.size > 2) {
                    val headerLength = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
                    if (bytes.size >= 2 + headerLength) {
                        val header = bytes.substring(2, 2 + headerLength).utf8()
                        if (header.contains("Path:audio")) {
                            val audioChunk = bytes.substring(2 + headerLength).toByteArray()
                            audioStream.write(audioChunk)
                        }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("Path:turn.end")) {
                    webSocket.close(1000, "Done")
                    deferred.complete(audioStream.toByteArray())
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Edge TTS WebSocket failure: ${t.message}, code: ${response?.code}")
                deferred.complete(null)
            }
        })

        activeWebSocket = ws

        withTimeoutOrNull(9000L) {
            deferred.await()
        }
    }

    suspend fun playAudio(mp3Bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (mp3Bytes.isEmpty()) return@withContext false
        stop()

        val tempFile = File.createTempFile("edge_audio_", ".mp3", context.cacheDir)
        activeTempFile = tempFile
        tempFile.writeBytes(mp3Bytes)

        suspendCancellableCoroutine { continuation ->
            val player = MediaPlayer()
            activeMediaPlayer = player
            try {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                player.setDataSource(tempFile.absolutePath)
                player.prepare()
                player.setOnCompletionListener {
                    cleanupPlayer()
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }
                player.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    cleanupPlayer()
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                    true
                }
                player.start()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing Edge TTS MP3", e)
                cleanupPlayer()
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }

            continuation.invokeOnCancellation {
                cleanupPlayer()
            }
        }
    }

    private fun cleanupPlayer() {
        try {
            activeMediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            activeMediaPlayer?.release()
        } catch (_: Exception) {}
        activeMediaPlayer = null

        try {
            activeTempFile?.delete()
        } catch (_: Exception) {}
        activeTempFile = null
    }

    fun stop() {
        try {
            activeWebSocket?.cancel()
            activeWebSocket = null
        } catch (_: Exception) {}
        cleanupPlayer()
    }

    fun release() {
        stop()
    }
}
