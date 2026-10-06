package com.example.calibretv.data.tts

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente HTTP de alto rendimiento para el microservicio local de síntesis Piper TTS.
 * Diseñado para streaming de ultra baja latencia en CalibroTV (Android TV).
 * Soporta endpoints directos GET con caché, POST para textos extensos,
 * precarga de modelos en RAM (/api/models/preload) y token de seguridad.
 */
object PiperTtsClient {

    private const val TAG = "PiperTtsClient"

    /**
     * Comprueba si el servidor local de Piper TTS está activo y saludable.
     */
    suspend fun isServerAvailable(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (serverUrl.endsWith("/")) "${serverUrl}health" else "$serverUrl/health"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = "GET"
            conn.connect()
            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                body.contains("\"status\":\"ok\"") || body.contains("\"status\": \"ok\"")
            } else {
                false
            }
        } catch (e: Exception) {
            Log.d(TAG, "Piper TTS server not reachable at $serverUrl: ${e.message}")
            false
        }
    }

    /**
     * Solicita la precarga del modelo de voz especificado en la memoria RAM del servidor Piper.
     * Esto asegura que la síntesis posterior sea instantánea.
     */
    suspend fun preloadVoice(
        voice: String,
        serverUrl: String = "http://192.168.1.89:5000",
        secret: String = "calibro_super_secret_token_change_me"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (serverUrl.endsWith("/")) "${serverUrl}api/models/preload" else "$serverUrl/api/models/preload"
            val tokenParam = if (secret.isNotBlank()) "&token=${URLEncoder.encode(secret, "UTF-8")}" else ""
            val fullUrl = "$endpoint?voice=${URLEncoder.encode(voice, "UTF-8")}$tokenParam"
            val conn = URL(fullUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 5000
            conn.requestMethod = "POST"
            if (secret.isNotBlank()) {
                conn.setRequestProperty("X-Calibro-Secret", secret)
            }
            conn.connect()
            val code = conn.responseCode
            code in 200..299
        } catch (e: Exception) {
            Log.d(TAG, "Preload voice $voice skipped: ${e.message}")
            false
        }
    }

    /**
     * Construye la URL de streaming directo GET soportada nativamente por reproductores
     * multimedia como MediaPlayer o ExoPlayer.
     */
    fun buildStreamingUrl(
        text: String,
        voice: String = "es_ES-davefx-medium",
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        serverUrl: String = "http://192.168.1.89:5000",
        secret: String = "calibro_super_secret_token_change_me"
    ): String {
        val baseUrl = if (serverUrl.endsWith("/")) "${serverUrl}api/tts" else "$serverUrl/api/tts"
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val tokenParam = if (secret.isNotBlank()) "&token=${URLEncoder.encode(secret, "UTF-8")}" else ""
        return "$baseUrl?voice=$voice&speed=$speed&pitch=$pitch$tokenParam&text=$encodedText"
    }

    /**
     * Sintetiza el texto solicitado y retorna el audio binario en formato WAV.
     * Utiliza GET para textos de tamaño estándar (beneficiándose del streaming y caché de Piper)
     * o POST con cuerpo JSON para párrafos más largos.
     */
    suspend fun synthesize(
        text: String,
        voice: String = "es_ES-davefx-medium",
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        serverUrl: String = "http://192.168.1.89:5000",
        secret: String = "calibro_super_secret_token_change_me"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val useGet = text.length <= 1800
            val conn = if (useGet) {
                val streamUrl = buildStreamingUrl(text, voice, speed, pitch, serverUrl, secret)
                (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 30000
                    if (secret.isNotBlank()) {
                        setRequestProperty("X-Calibro-Secret", secret)
                    }
                    setRequestProperty("Accept", "audio/wav, */*")
                }
            } else {
                val endpoint = if (serverUrl.endsWith("/")) "${serverUrl}api/tts" else "$serverUrl/api/tts"
                (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 40000
                    doInput = true
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "audio/wav, */*")
                    if (secret.isNotBlank()) {
                        setRequestProperty("X-Calibro-Secret", secret)
                    }
                    val payload = JSONObject().apply {
                        put("text", text)
                        put("voice", voice)
                        put("speed", speed)
                        put("pitch", pitch)
                    }
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(payload.toString())
                        writer.flush()
                    }
                }
            }

            conn.connect()
            val code = conn.responseCode
            if (code in 200..299) {
                val bytes = conn.inputStream.use { it.readBytes() }
                val cacheHeader = conn.getHeaderField("X-Cache-Status") ?: "MISS"
                Log.d(TAG, "Audio synthesized (${bytes.size} bytes, Cache: $cacheHeader, Voice: $voice)")
                Result.success(bytes)
            } else {
                val err = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) {
                    ""
                }
                Log.w(TAG, "Piper TTS HTTP error $code: $err")
                Result.failure(Exception("HTTP $code: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error synthesizing text via Piper TTS: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Resuelve el identificador de voz oficial de Piper a partir del código configurado.
     */
    fun resolveVoiceId(localeCode: String, fallbackVoice: String = "es_ES-davefx-medium"): String {
        return TtsVoiceCatalog.findVoice(localeCode).code
    }
}
