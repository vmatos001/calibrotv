package com.example.calibretv.data.tts

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente HTTP para el microservicio local de síntesis de voz Piper TTS (VITS Architecture).
 * Soporta llamadas con streaming/cache WAV, cabecera secreta X-Calibro-Secret y verificación de salud.
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
     * Sintetiza el texto solicitado y retorna el audio binario en formato WAV.
     */
    suspend fun synthesize(
        text: String,
        voice: String = "es_MX-claude-high",
        speed: Float = 1.0f,
        serverUrl: String = "http://192.168.1.89:5000",
        secret: String = "calibro_super_secret_token_change_me"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (serverUrl.endsWith("/")) "${serverUrl}api/tts" else "$serverUrl/api/tts"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 15000
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "audio/wav, */*")
            if (secret.isNotBlank()) {
                conn.setRequestProperty("X-Calibro-Secret", secret)
            }

            val payload = JSONObject().apply {
                put("text", text)
                put("voice", voice)
                put("speed", speed)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

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
     * Resuelve el identificador de voz oficial de Piper a partir del código de idioma o acento.
     */
    fun resolveVoiceId(localeCode: String, fallbackVoice: String = "es_MX-claude-high"): String {
        return when {
            // Español (4 voces)
            localeCode.equals("es-ES", ignoreCase = true) || localeCode.contains("España", ignoreCase = true) -> "es_ES-sharvard-medium"
            localeCode.equals("es-MX", ignoreCase = true) || localeCode.contains("México", ignoreCase = true) -> "es_MX-claude-high"
            localeCode.equals("es-US", ignoreCase = true) || localeCode.contains("Latino", ignoreCase = true) -> "es_MX-ald-medium"
            localeCode.equals("es-AR", ignoreCase = true) || localeCode.contains("Argentina", ignoreCase = true) -> "es_ES-carlfm-x_low"
            // Inglés (4 voces)
            localeCode.equals("en-GB", ignoreCase = true) || localeCode.contains("UK", ignoreCase = true) || localeCode.contains("Reino Unido", ignoreCase = true) -> "en_GB-alan-medium"
            localeCode.equals("en-AU", ignoreCase = true) || localeCode.contains("Australia", ignoreCase = true) -> "en_GB-alba-medium"
            localeCode.equals("en-CA", ignoreCase = true) || localeCode.contains("Canadá", ignoreCase = true) || localeCode.contains("Canada", ignoreCase = true) -> "en_US-amy-medium"
            localeCode.equals("en-US", ignoreCase = true) || localeCode.contains("EE.UU.", ignoreCase = true) || localeCode.startsWith("en", ignoreCase = true) -> "en_US-ryan-high"
            else -> fallbackVoice
        }
    }
}
