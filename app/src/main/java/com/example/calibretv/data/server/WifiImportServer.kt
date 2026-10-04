package com.example.calibretv.data.server

import android.content.Context
import android.util.Log
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.comic.ComicParser
import com.example.calibretv.data.epub.EpubParser
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.pdf.PdfParser
import com.example.calibretv.data.quote.QuoteCardGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Collections

/**
 * Pure Android-compliant Socket HTTP Server for BookSpread v3.0.
 * Uses java.net.ServerSocket for 100% runtime compatibility on all Android TV devices.
 * Supports:
 * 1. WiFi Book Import (EPUB, PDF, CBZ/CBR)
 * 2. Mobile Companion: Note & Review Taking via QR Code
 * 3. Mobile Companion: Editorial Quote Card Sharing & Downloading
 */
class WifiImportServer(private val context: Context, private val repository: BookRepository) {

    private val TAG = "CalibroWifiServer"
    private var serverSocket: ServerSocket? = null
    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var activePort: Int = 8080
        private set

    @Volatile
    var connectedClientsCount: Int = 0
        private set

    @Volatile
    var uploadedFilesCount: Int = 0
        private set

    /** Token de acceso aleatorio por sesión de la app; se incluye en las URLs de los QR. */
    val accessToken: String = run {
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        val rnd = java.security.SecureRandom()
        (1..8).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
    }

    /** Construye la URL pública (con token) para mostrar en un código QR. */
    fun buildUrl(pathAndQuery: String = "/"): String {
        val p = if (pathAndQuery.startsWith("/")) pathAndQuery else "/$pathAndQuery"
        val sep = if (p.contains("?")) "&" else "?"
        return "http://${getLocalIpAddress()}:$activePort$p${sep}t=$accessToken"
    }

    private fun isAllowedBookFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in ALLOWED_EXTENSIONS
    }

    private fun sendStatus(output: OutputStream, code: Int, status: String, title: String, message: String) {
        val html = """
            <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0"><title>$title • CalibroTV</title>
            <style>body{background:#0A0A0A;color:#fff;font-family:sans-serif;text-align:center;padding:40px 20px}
            .box{background:#161616;border-radius:16px;padding:32px;max-width:440px;margin:auto}
            h2{color:#FFA000;margin-top:0}p{color:#A0A0A0;line-height:1.5}</style></head>
            <body><div class="box"><h2>${escapeHtml(title)}</h2><p>${escapeHtml(message)}</p></div></body></html>
        """.trimIndent()
        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code $status\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        try {
            output.write(header.toByteArray(Charsets.UTF_8))
            output.write(bytes)
            output.flush()
        } catch (_: Exception) {}
    }

    companion object {
        private const val COOKIE_NAME = "calibro_t"
        private const val MAX_FORM_BYTES = 64 * 1024
        private const val MAX_UPLOAD_BYTES = 300L * 1024 * 1024
        private val ALLOWED_EXTENSIONS = setOf("epub", "pdf", "cbz", "cbr")

        @Volatile
        private var instance: WifiImportServer? = null

        fun getInstance(context: Context, repository: BookRepository): WifiImportServer {
            return instance ?: synchronized(this) {
                instance ?: WifiImportServer(context.applicationContext, repository).also { instance = it }
            }
        }
    }

    fun getLocalIpAddress(): String {
        // 1. Prioridad: Consultar directamente WifiManager en Android
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                val ipStr = String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.%d",
                    (ipInt and 0xff),
                    (ipInt shr 8 and 0xff),
                    (ipInt shr 16 and 0xff),
                    (ipInt shr 24 and 0xff)
                )
                if (!ipStr.startsWith("0.") && !ipStr.startsWith("127.") && !ipStr.startsWith("192.168.49.")) {
                    return ipStr
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "WifiManager fallback: ${e.message}")
        }

        // 2. Escaneo inteligente de interfaces de red excluyendo subredes virtuales o p2p
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            val prioritized = interfaces.sortedByDescending {
                val name = it.name.lowercase()
                when {
                    name.startsWith("wlan") -> 3
                    name.startsWith("eth") -> 2
                    !name.contains("p2p") && !name.contains("dummy") && !name.contains("tun") -> 1
                    else -> 0
                }
            }

            for (intf in prioritized) {
                val name = intf.name.lowercase()
                if (name.contains("p2p") || name.contains("dummy") || name.contains("tun")) continue
                if (!intf.isUp || intf.isLoopback) continue

                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.168.49.")) continue // Omitir subred de Wi-Fi Direct
                        if (host.isNotBlank() && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining IP from NetworkInterfaces", e)
        }
        return "192.168.1.100"
    }

    fun startServer(preferredPort: Int = 8080): Boolean {
        if (isRunning && serverSocket != null && !serverSocket!!.isClosed) return true
        stopServer()

        val candidatePorts = linkedSetOf(preferredPort, 8080, 8088, 8888, 9090, 8081)
        for (port in candidatePorts) {
            try {
                val socket = ServerSocket()
                socket.reuseAddress = true
                socket.bind(java.net.InetSocketAddress(port))
                serverSocket = socket
                activePort = port
                isRunning = true
                Log.d(TAG, "Socket Server started successfully on port $activePort")
                break
            } catch (e: Exception) {
                Log.w(TAG, "Port $port unavailable: ${e.message}")
            }
        }

        if (!isRunning || serverSocket == null) {
            Log.e(TAG, "Failed to bind server on any candidate port")
            return false
        }

        CoroutineScope(Dispatchers.IO).launch {
            while (isRunning) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    connectedClientsCount++
                    launch {
                        handleClient(clientSocket)
                    }
                } catch (e: Exception) {
                    if (!isRunning) break
                }
            }
        }
        return true
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {}
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.use { s ->
                val input = s.getInputStream()
                val output = s.getOutputStream()

                // Lectura byte-a-byte de cabeceras HTTP para NO perder ningún byte del cuerpo binario
                val headerBytes = java.io.ByteArrayOutputStream()
                var match = 0
                while (true) {
                    val b = input.read()
                    if (b == -1) break
                    headerBytes.write(b)
                    if (b == '\r'.code && (match == 0 || match == 2)) {
                        match++
                    } else if (b == '\n'.code && (match == 1 || match == 3)) {
                        match++
                        if (match == 4) break
                    } else {
                        match = if (b == '\r'.code) 1 else 0
                    }
                    if (headerBytes.size() > 65536) break
                }

                val headerStr = headerBytes.toString("UTF-8")
                val lines = headerStr.split("\r\n")
                if (lines.isEmpty() || lines[0].isBlank()) return@withContext

                val tokens = lines[0].split(" ")
                if (tokens.size < 2) return@withContext

                val method = tokens[0].uppercase()
                val rawUri = tokens[1]
                val path = rawUri.substringBefore("?")
                val queryString = rawUri.substringAfter("?", "")
                val queryParams = parseQueryParams(queryString)

                val headers = mutableMapOf<String, String>()
                for (i in 1 until lines.size) {
                    val hLine = lines[i]
                    val idx = hLine.indexOf(":")
                    if (idx > 0) {
                        headers[hLine.substring(0, idx).trim().lowercase()] = hLine.substring(idx + 1).trim()
                    }
                }

                // 🔐 Autenticación: token del QR (?t=) → cookie de sesión
                val cookieToken = headers["cookie"]?.split(";")
                    ?.map { it.trim() }
                    ?.firstOrNull { it.startsWith("$COOKIE_NAME=") }
                    ?.substringAfter("=")
                val queryToken = queryParams["t"]

                if (queryToken == accessToken && method == "GET") {
                    // Fija la cookie y redirige a la URL limpia para que los enlaces relativos funcionen
                    val cleanQuery = queryString.split("&").filterNot { it.startsWith("t=") }.joinToString("&")
                    val location = if (cleanQuery.isBlank()) path else "$path?$cleanQuery"
                    val redirect = "HTTP/1.1 302 Found\r\n" +
                            "Location: $location\r\n" +
                            "Set-Cookie: $COOKIE_NAME=$accessToken; Path=/; HttpOnly; SameSite=Strict\r\n" +
                            "Content-Length: 0\r\nConnection: close\r\n\r\n"
                    output.write(redirect.toByteArray(Charsets.UTF_8))
                    output.flush()
                    return@withContext
                }
                if (cookieToken != accessToken && queryToken != accessToken) {
                    sendStatus(output, 403, "Forbidden", "Acceso no autorizado", "Escanea el código QR que aparece en la pantalla de tu TV para conectarte.")
                    return@withContext
                }

                when {
                    method == "GET" && path == "/note" -> {
                        val bookId = queryParams["bookId"] ?: ""
                        val profileId = queryParams["profileId"] ?: ""
                        serveNotePage(output, bookId, profileId)
                    }
                    (method == "POST") && (path == "/note" || path == "/api/note") -> {
                        val contentLength = headers["content-length"]?.toIntOrNull() ?: -1
                        if (contentLength <= 0 || contentLength > MAX_FORM_BYTES) {
                            sendStatus(output, 413, "Payload Too Large", "Nota demasiado larga", "La nota supera el tamaño máximo permitido.")
                            return@withContext
                        }
                        val bodyBytes = ByteArray(contentLength)
                        var readTotal = 0
                        while (readTotal < contentLength) {
                            val r = input.read(bodyBytes, readTotal, contentLength - readTotal)
                            if (r == -1) break
                            readTotal += r
                        }
                        val body = String(bodyBytes, 0, readTotal, Charsets.UTF_8)
                        val formParams = parseQueryParams(body)
                        val bookId = formParams["bookId"] ?: queryParams["bookId"] ?: ""
                        val profileId = formParams["profileId"] ?: queryParams["profileId"] ?: ""
                        val noteText = formParams["noteText"] ?: formParams["content"] ?: formParams["note"] ?: ""
                        val spreadIndex = (formParams["spreadIndex"] ?: "0").toIntOrNull() ?: 0
                        if (bookId.isNotBlank() && noteText.isNotBlank()) {
                            repository.addNote(bookId = bookId, text = noteText.take(5000), spreadIndex = spreadIndex, profileId = profileId)
                        }
                        serveNoteSuccessPage(output, bookId, profileId)
                    }
                    method == "GET" && path == "/quote" -> {
                        val cardId = queryParams["cardId"] ?: ""
                        serveQuoteImage(output, cardId)
                    }
                    method == "GET" && path == "/quote-preview" -> {
                        val cardId = queryParams["cardId"] ?: ""
                        serveQuotePreviewPage(output, cardId)
                    }
                    method == "POST" && path.startsWith("/upload") -> {
                        val contentLength = headers["content-length"]?.toLongOrNull() ?: -1L
                        val contentType = headers["content-type"] ?: ""
                        val filename = queryParams["filename"]
                        when {
                            contentLength <= 0L -> sendStatus(output, 411, "Length Required", "Archivo inválido", "No se recibió el tamaño del archivo.")
                            contentLength > MAX_UPLOAD_BYTES -> sendStatus(output, 413, "Payload Too Large", "Archivo demasiado grande", "El tamaño máximo es ${MAX_UPLOAD_BYTES / (1024 * 1024)} MB.")
                            filename != null && !isAllowedBookFile(filename) -> sendStatus(output, 415, "Unsupported Media Type", "Formato no soportado", "Solo se aceptan archivos EPUB, PDF, CBZ o CBR.")
                            else -> handleFileUpload(input, output, contentLength.toInt(), contentType, filename)
                        }
                    }
                    else -> {
                        serveMainPage(output)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client connection", e)
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx != -1) {
                val key = try { URLDecoder.decode(pair.substring(0, idx), "UTF-8") } catch (_: Exception) { pair.substring(0, idx) }
                val value = try { URLDecoder.decode(pair.substring(idx + 1), "UTF-8") } catch (_: Exception) { pair.substring(idx + 1) }
                map[key] = value
            }
        }
        return map
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun sendHtmlResponse(output: OutputStream, statusCode: Int, html: String) {
        val statusText = if (statusCode == 200) "OK" else if (statusCode == 404) "Not Found" else "Internal Error"
        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveNotePage(output: OutputStream, bookId: String, profileId: String) {
        val book = repository.getCachedBooks().find { it.id == bookId }
        val title = book?.title ?: "Libro en Lectura"
        val author = book?.author ?: ""
        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Anotación • BookSpread</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Georgia, serif;
                        background: #0C0A09;
                        color: #F7F4EE;
                        padding: 20px;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        min-height: 100vh;
                    }
                    .container {
                        width: 100%;
                        max-width: 460px;
                        background: #181513;
                        border: 1px solid #423419;
                        border-radius: 16px;
                        padding: 26px 20px;
                        box-shadow: 0 12px 36px rgba(0,0,0,0.8);
                        margin-top: 10px;
                    }
                    .badge {
                        display: inline-block;
                        color: #C5A059;
                        font-size: 11px;
                        font-weight: 700;
                        letter-spacing: 2px;
                        text-transform: uppercase;
                        margin-bottom: 8px;
                    }
                    h1 {
                        font-family: Georgia, serif;
                        color: #F7F4EE;
                        font-size: 20px;
                        margin: 0 0 4px 0;
                        line-height: 1.3;
                    }
                    .author {
                        color: #A8A29E;
                        font-size: 13px;
                        margin-bottom: 20px;
                        font-style: italic;
                    }
                    label {
                        display: block;
                        color: #C5A059;
                        font-size: 13px;
                        font-weight: 600;
                        margin-bottom: 8px;
                        letter-spacing: 0.5px;
                    }
                    textarea {
                        width: 100%;
                        min-height: 150px;
                        background: #0C0A09;
                        color: #F7F4EE;
                        border: 1px solid #423419;
                        border-radius: 12px;
                        padding: 14px;
                        font-size: 16px;
                        line-height: 1.5;
                        resize: vertical;
                        outline: none;
                        font-family: inherit;
                        transition: border-color 0.2s;
                    }
                    textarea:focus {
                        border-color: #C5A059;
                    }
                    .btn {
                        display: block;
                        width: 100%;
                        margin-top: 20px;
                        background: #C5A059;
                        color: #0C0A09;
                        border: none;
                        padding: 14px;
                        font-size: 16px;
                        font-weight: bold;
                        border-radius: 24px;
                        cursor: pointer;
                        letter-spacing: 0.5px;
                    }
                    .btn:hover {
                        background: #D4AF37;
                    }
                    .footer {
                        margin-top: 24px;
                        text-align: center;
                        color: #6B655B;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <span class="badge">BookSpread • Companion</span>
                    <h1>${escapeHtml(title)}</h1>
                    ${if (author.isNotBlank()) "<div class=\"author\">— " + escapeHtml(author) + " —</div>" else ""}
                    <form action="/note" method="POST">
                        <input type="hidden" name="bookId" value="${escapeHtml(bookId)}">
                        <input type="hidden" name="profileId" value="${escapeHtml(profileId)}">
                        <label for="noteText">Anotación / Reseña rápida:</label>
                        <textarea id="noteText" name="noteText" placeholder="Escribe aquí una cita destacada, pensamiento o reflexión. Se reflejará en la pantalla del televisor en tiempo real..." required autofocus></textarea>
                        <button type="submit" class="btn">✨ Enviar a BookSpread en TV</button>
                    </form>
                    <div class="footer">Sincronizado vía WiFi local con tu Android TV</div>
                </div>
            </body>
            </html>
        """.trimIndent()
        sendHtmlResponse(output, 200, html)
    }

    private fun serveNoteSuccessPage(output: OutputStream, bookId: String, profileId: String) {
        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>¡Nota Guardada! • BookSpread</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Georgia, serif;
                        background: #0C0A09;
                        color: #F7F4EE;
                        padding: 24px;
                        margin: 0;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        text-align: center;
                    }
                    .container {
                        width: 100%;
                        max-width: 440px;
                        background: #181513;
                        border: 1px solid #C5A059;
                        border-radius: 16px;
                        padding: 36px 24px;
                        box-shadow: 0 12px 36px rgba(0,0,0,0.8);
                    }
                    .icon { font-size: 52px; margin-bottom: 16px; }
                    h2 { font-family: Georgia, serif; color: #C5A059; margin: 0 0 12px 0; font-size: 24px; }
                    p { color: #A8A29E; font-size: 15px; line-height: 1.6; margin: 0 0 28px 0; }
                    .btn {
                        display: inline-block;
                        background: #C5A059;
                        color: #0C0A09;
                        text-decoration: none;
                        padding: 14px 28px;
                        font-size: 15px;
                        font-weight: bold;
                        border-radius: 24px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">✨</div>
                    <h2>¡Anotación Guardada!</h2>
                    <p>Tu nota ya está sincronizada y visible en la pantalla de BookSpread en tu televisor.</p>
                    <a href="/note?bookId=${escapeHtml(bookId)}&profileId=${escapeHtml(profileId)}" class="btn">+ Escribir otra nota</a>
                </div>
            </body>
            </html>
        """.trimIndent()
        sendHtmlResponse(output, 200, html)
    }

    private fun serveQuoteImage(output: OutputStream, cardId: String) {
        val cardBytes = QuoteCardGenerator.getCard(cardId)
        if (cardBytes == null) {
            val notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            output.write(notFound.toByteArray(Charsets.UTF_8))
            output.flush()
            return
        }

        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: image/png\r\n" +
                "Content-Length: ${cardBytes.size}\r\n" +
                "Content-Disposition: inline; filename=\"quote_$cardId.png\"\r\n" +
                "Cache-Control: public, max-age=86400\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(cardBytes)
        output.flush()
    }

    private fun serveQuotePreviewPage(output: OutputStream, cardId: String) {
        val cardBytes = QuoteCardGenerator.getCard(cardId)
        if (cardBytes == null) {
            val html = """
                <!DOCTYPE html>
                <html>
                <body style="background:#0C0A09;color:#C5A059;text-align:center;padding:40px;font-family:sans-serif;">
                    <h2>Tarjeta no encontrada o expirada</h2>
                    <p style="color:#A8A29E">Genera una nueva Quote Card desde BookSpread en tu televisor.</p>
                </body>
                </html>
            """.trimIndent()
            sendHtmlResponse(output, 404, html)
            return
        }

        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Quote Card • BookSpread</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Georgia, serif;
                        background: #0C0A09;
                        color: #F7F4EE;
                        padding: 20px;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        min-height: 100vh;
                        text-align: center;
                    }
                    .header { margin-bottom: 16px; margin-top: 8px; }
                    .badge {
                        color: #C5A059;
                        font-size: 11px;
                        font-weight: bold;
                        letter-spacing: 2px;
                        text-transform: uppercase;
                    }
                    .preview-card {
                        max-width: 320px;
                        width: 100%;
                        border-radius: 14px;
                        overflow: hidden;
                        box-shadow: 0 16px 48px rgba(0,0,0,0.9);
                        border: 1px solid #423419;
                        margin-bottom: 20px;
                    }
                    .preview-card img {
                        width: 100%;
                        height: auto;
                        display: block;
                    }
                    .btn {
                        display: inline-block;
                        background: #C5A059;
                        color: #0C0A09;
                        padding: 14px 28px;
                        font-size: 16px;
                        font-weight: bold;
                        border-radius: 24px;
                        text-decoration: none;
                        letter-spacing: 0.5px;
                        box-shadow: 0 4px 16px rgba(197,160,89,0.3);
                    }
                    .tip {
                        margin-top: 16px;
                        color: #A8A29E;
                        font-size: 13px;
                        max-width: 300px;
                        line-height: 1.5;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <span class="badge">BookSpread • Selección Editorial</span>
                </div>
                <div class="preview-card">
                    <img src="/quote?cardId=${escapeHtml(cardId)}" alt="BookSpread Quote Card">
                </div>
                <a href="/quote?cardId=${escapeHtml(cardId)}" download="BookSpread_Quote_${cardId.take(8)}.png" class="btn">
                    📥 Descargar Quote Card (PNG)
                </a>
                <p class="tip">
                    📱 Mantén pulsada la imagen para guardarla o compartirla directamente en WhatsApp, Instagram Stories o redes sociales.
                </p>
            </body>
            </html>
        """.trimIndent()
        sendHtmlResponse(output, 200, html)
    }

    private fun serveMainPage(output: OutputStream) {
        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>CalibroTV — Importador WiFi</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0A0A0A; color: #F0F0F0; text-align: center; padding: 24px 16px; margin: 0; }
                    .card { max-width: 480px; margin: 20px auto; background: #141416; border-radius: 16px; padding: 28px; box-shadow: 0 8px 30px rgba(0,0,0,0.8); border: 1.5px solid #FFA000; }
                    .logo { font-size: 30px; font-weight: 900; color: #FFA000; letter-spacing: 1px; margin-bottom: 6px; }
                    .tagline { color: #A0A0A0; font-size: 14px; margin-bottom: 24px; line-height: 1.4; }
                    .drop-zone { border: 2px dashed #FFA000; border-radius: 14px; padding: 32px 16px; margin: 20px 0; background: rgba(255, 160, 0, 0.05); cursor: pointer; transition: 0.2s; }
                    .drop-zone:hover { background: rgba(255, 160, 0, 0.12); }
                    input[type="file"] { display: none; }
                    .btn { background: #FFA000; color: #101010; border: none; padding: 14px 28px; font-size: 16px; font-weight: bold; border-radius: 24px; cursor: pointer; width: 100%; margin-top: 12px; }
                    .btn:hover { background: #FFB300; }
                    .progress-box { display: none; margin-top: 20px; }
                    .progress-bar-bg { width: 100%; height: 12px; background: #262626; border-radius: 6px; overflow: hidden; margin-top: 8px; }
                    .progress-bar { width: 0%; height: 100%; background: #FFA000; transition: width 0.15s; }
                    .status { margin-top: 16px; font-weight: bold; font-size: 14px; }
                    .footer { margin-top: 24px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">📖 CALIBROTV</div>
                    <div class="tagline">Transfiere libros (.epub, .pdf) o cómics (.cbz, .cbr) directamente a tu televisor por WiFi local.</div>
                    <div class="drop-zone" onclick="document.getElementById('fileInput').click()" id="dropZone">
                        <p id="dropText">📁 Pulsa aquí para elegir tu archivo o arrástralo a este cuadro</p>
                        <input type="file" id="fileInput" accept=".epub,.pdf,.cbz,.cbr">
                    </div>
                    <div class="progress-box" id="progressBox">
                        <div id="progressText" style="color: #FFA000; font-size: 13px;">Subiendo... 0%</div>
                        <div class="progress-bar-bg"><div class="progress-bar" id="progressBar"></div></div>
                    </div>
                    <button type="button" class="btn" id="uploadBtn" onclick="startUpload()">🚀 Enviar a CalibroTV</button>
                    <div class="status" id="statusMsg"></div>
                    <div class="footer">Sincronización directa vía red local WiFi</div>
                </div>
                <script>
                    const fileInput = document.getElementById('fileInput');
                    const dropText = document.getElementById('dropText');
                    const dropZone = document.getElementById('dropZone');
                    const progressBox = document.getElementById('progressBox');
                    const progressBar = document.getElementById('progressBar');
                    const progressText = document.getElementById('progressText');
                    const statusMsg = document.getElementById('statusMsg');
                    const uploadBtn = document.getElementById('uploadBtn');

                    fileInput.addEventListener('change', () => {
                        if (fileInput.files.length > 0) {
                            dropText.innerText = "📄 " + fileInput.files[0].name;
                        }
                    });

                    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.style.background = 'rgba(255,160,0,0.18)'; });
                    dropZone.addEventListener('dragleave', (e) => { e.preventDefault(); dropZone.style.background = 'rgba(255,160,0,0.05)'; });
                    dropZone.addEventListener('drop', (e) => {
                        e.preventDefault();
                        dropZone.style.background = 'rgba(255,160,0,0.05)';
                        if (e.dataTransfer.files.length > 0) {
                            fileInput.files = e.dataTransfer.files;
                            dropText.innerText = "📄 " + fileInput.files[0].name;
                        }
                    });

                    function startUpload() {
                        if (!fileInput.files || fileInput.files.length === 0) {
                            alert("Por favor selecciona primero un archivo EPUB, PDF o CBZ.");
                            return;
                        }
                        const file = fileInput.files[0];
                        uploadBtn.disabled = true;
                        uploadBtn.style.opacity = '0.5';
                        progressBox.style.display = 'block';
                        statusMsg.innerHTML = "Enviando " + file.name + "...";

                        const xhr = new XMLHttpRequest();
                        xhr.open('POST', '/upload?filename=' + encodeURIComponent(file.name), true);
                        xhr.setRequestHeader('Content-Type', 'application/octet-stream');

                        xhr.upload.onprogress = (e) => {
                            if (e.lengthComputable) {
                                const pct = Math.round((e.loaded / e.total) * 100);
                                progressBar.style.width = pct + '%';
                                progressText.innerText = "Subiendo... " + pct + "%";
                            }
                        };

                        xhr.onload = () => {
                            uploadBtn.disabled = false;
                            uploadBtn.style.opacity = '1';
                            if (xhr.status === 200) {
                                progressBar.style.width = '100%';
                                progressText.innerText = "¡Completado 100%!";
                                statusMsg.innerHTML = "<span style='color: #4CAF50;'>✅ ¡" + file.name + " transferido con éxito! Ya puedes abrirlo en tu TV.</span>";
                            } else {
                                statusMsg.innerHTML = "<span style='color: #FF5252;'>❌ Error del servidor al guardar el archivo.</span>";
                            }
                        };

                        xhr.onerror = () => {
                            uploadBtn.disabled = false;
                            uploadBtn.style.opacity = '1';
                            statusMsg.innerHTML = "<span style='color: #FF5252;'>❌ Error de conexión al televisor. Verifica que estés en la misma red Wi-Fi.</span>";
                        };

                        xhr.send(file);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(response.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun handleFileUpload(
        input: java.io.InputStream,
        output: OutputStream,
        contentLength: Int,
        contentType: String,
        urlFilename: String? = null
    ) {
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.tmp")
        try {
            var originalName = urlFilename ?: "libro_importado"

            if (contentType.contains("multipart/form-data")) {
                val boundaryMarker = contentType.substringAfter("boundary=", "").trim()
                val partHeaderBytes = java.io.ByteArrayOutputStream()
                var match = 0
                while (true) {
                    val b = input.read()
                    if (b == -1) break
                    partHeaderBytes.write(b)
                    if (b == '\r'.code && (match == 0 || match == 2)) {
                        match++
                    } else if (b == '\n'.code && (match == 1 || match == 3)) {
                        match++
                        if (match == 4) break
                    } else {
                        match = if (b == '\r'.code) 1 else 0
                    }
                    if (partHeaderBytes.size() > 16384) break
                }
                val partHeaderStr = partHeaderBytes.toString("UTF-8")
                val filenameRegex = Regex("""filename="([^"]+)"""")
                filenameRegex.find(partHeaderStr)?.groupValues?.get(1)?.let {
                    originalName = it
                }

                FileOutputStream(tempFile).use { out ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = partHeaderBytes.size()
                    while (totalRead < contentLength || contentLength == 0) {
                        val toRead = if (contentLength > 0) minOf(buffer.size, contentLength - totalRead) else buffer.size
                        if (toRead <= 0) break
                        bytesRead = input.read(buffer, 0, toRead)
                        if (bytesRead == -1) break
                        out.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                    }
                }

                if (boundaryMarker.isNotBlank() && tempFile.length() > boundaryMarker.length + 4) {
                    val length = tempFile.length()
                    val checkSize = minOf(length.toInt(), 512)
                    val tail = ByteArray(checkSize)
                    java.io.RandomAccessFile(tempFile, "rw").use { raf ->
                        raf.seek(length - checkSize)
                        raf.readFully(tail)
                        val tailStr = String(tail, Charsets.ISO_8859_1)
                        val bIndex = tailStr.lastIndexOf("--$boundaryMarker")
                        if (bIndex != -1) {
                            val newLength = (length - checkSize) + bIndex
                            val cutPos = if (newLength >= 2 && tailStr.getOrNull(bIndex - 2) == '\r' && tailStr.getOrNull(bIndex - 1) == '\n') {
                                newLength - 2
                            } else newLength
                            raf.setLength(cutPos.coerceAtLeast(0))
                        }
                    }
                }
            } else {
                FileOutputStream(tempFile).use { out ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0
                    while (totalRead < contentLength || contentLength == 0) {
                        val toRead = if (contentLength > 0) minOf(buffer.size, contentLength - totalRead) else buffer.size
                        if (toRead <= 0) break
                        bytesRead = input.read(buffer, 0, toRead)
                        if (bytesRead == -1) break
                        out.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                    }
                }
            }

            if (originalName.contains('.') && !isAllowedBookFile(originalName)) {
                tempFile.delete()
                sendStatus(output, 415, "Unsupported Media Type", "Formato no soportado", "Solo se aceptan archivos EPUB, PDF, CBZ o CBR.")
                return
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                uploadedFilesCount++
                processUploadedFile(tempFile, contentType, originalName)

                val successHtml = """
                    <!DOCTYPE html>
                    <html lang="es">
                    <head><meta charset="UTF-8"><title>¡Enviado! • CalibroTV</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { background: #0A0A0A; color: #fff; font-family: sans-serif; text-align: center; padding: 40px 20px; }
                        .box { background: #141416; border-radius: 16px; padding: 32px; max-width: 440px; margin: auto; border: 1.5px solid #FFA000; box-shadow: 0 10px 30px rgba(0,0,0,0.8); }
                        h2 { color: #FFA000; margin-top: 0; }
                        p { color: #A0A0A0; line-height: 1.5; }
                        .name { color: #fff; font-weight: bold; background: #222; padding: 8px 12px; border-radius: 8px; margin: 16px 0; word-break: break-all; }
                        a.btn { display: inline-block; background: #FFA000; color: #111; font-weight: bold; text-decoration: none; padding: 12px 24px; border-radius: 24px; margin-top: 12px; }
                    </style>
                    </head>
                    <body>
                        <div class="box">
                            <h2>✅ ¡Libro Recibido con Éxito!</h2>
                            <div class="name">📄 ${escapeHtml(originalName)}</div>
                            <p>El libro ya se ha transferido y está disponible en tu televisor CalibroTV.</p>
                            <a href="/" class="btn">+ Subir otro libro</a>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                val bytes = successHtml.toByteArray(Charsets.UTF_8)
                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(response.toByteArray(Charsets.UTF_8))
                output.write(bytes)
                output.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file", e)
            val errResponse = "HTTP/1.1 500 Internal Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            output.write(errResponse.toByteArray(Charsets.UTF_8))
            output.flush()
        }
    }

    private suspend fun processUploadedFile(tempFile: File, contentType: String, originalFilename: String = "") {
        withContext(Dispatchers.IO) {
            try {
                val isPdf = originalFilename.endsWith(".pdf", ignoreCase = true) ||
                        PdfParser.isPdfFile(tempFile) ||
                        contentType.contains("pdf")
                val isComic = !isPdf && (
                        originalFilename.endsWith(".cbz", ignoreCase = true) ||
                        originalFilename.endsWith(".cbr", ignoreCase = true) ||
                        ComicParser.isComicFile(tempFile) ||
                        contentType.contains("zip")
                )
                val bookId = "local_wifi_${System.currentTimeMillis()}"

                var title = if (originalFilename.isNotBlank()) originalFilename.substringBeforeLast(".").replace('_', ' ') else "Libro Importado WiFi"
                var author = "Importado por WiFi"
                var summary = "Libro importado directamente desde tu dispositivo mediante WiFi."
                var coverPath: String? = null

                when {
                    isPdf -> {
                        author = "Documento PDF"
                        summary = "Documento PDF importado directamente por WiFi."

                        val coverBmp = PdfParser.extractCover(tempFile, 400, 600)
                        if (coverBmp != null) {
                            val coverFile = File(context.filesDir, "cover_$bookId.png")
                            FileOutputStream(coverFile).use { out ->
                                coverBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                            }
                            coverPath = coverFile.absolutePath
                            coverBmp.recycle()
                        }
                    }
                    isComic -> {
                        author = "Cómic"
                        summary = "Cómic importado directamente por WiFi."
                    }
                    else -> {
                        val parsed = EpubParser.parseEpubToBook(tempFile, title)
                        if (parsed.title.isNotBlank()) title = parsed.title
                        val extractedDesc = EpubParser.extractDescription(tempFile)
                        if (!extractedDesc.isNullOrBlank()) summary = extractedDesc
                    }
                }

                val ext = when {
                    isPdf -> "pdf"
                    isComic -> "cbz"
                    else -> "epub"
                }

                val destFile = File(context.filesDir, "book_$bookId.$ext")
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()

                val category = when {
                    isPdf -> "PDF"
                    isComic -> "Cómic"
                    else -> "WiFi"
                }

                val newBook = Book(
                    id = bookId,
                    title = title,
                    author = author,
                    coverUrl = coverPath,
                    epubUrl = destFile.absolutePath,
                    summary = summary,
                    category = category,
                    tags = listOf(category, "Local")
                )

                val current = repository.getCachedBooks().toMutableList()
                current.add(0, newBook)
                repository.saveCachedBooks(current)
                Log.d(TAG, "Successfully processed uploaded book: $title ($category)")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing uploaded file", e)
            }
        }
    }
}
