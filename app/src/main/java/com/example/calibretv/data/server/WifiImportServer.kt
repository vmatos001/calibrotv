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
    var connectedClientsCount: Int = 0
        private set

    @Volatile
    var uploadedFilesCount: Int = 0
        private set

    companion object {
        @Volatile
        private var instance: WifiImportServer? = null

        fun getInstance(context: Context, repository: BookRepository): WifiImportServer {
            return instance ?: synchronized(this) {
                instance ?: WifiImportServer(context.applicationContext, repository).also { instance = it }
            }
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining IP", e)
        }
        return "192.168.1.100"
    }

    fun startServer(port: Int = 8080): Boolean {
        if (isRunning) return true
        return try {
            serverSocket = ServerSocket(port)
            isRunning = true
            Log.d(TAG, "Socket Server started on port $port")

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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ServerSocket", e)
            isRunning = false
            false
        }
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
                val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))

                val requestLine = reader.readLine() ?: return@withContext
                val tokens = requestLine.split(" ")
                if (tokens.size < 2) return@withContext

                val method = tokens[0].uppercase()
                val rawUri = tokens[1]
                val path = rawUri.substringBefore("?")
                val queryString = rawUri.substringAfter("?", "")
                val queryParams = parseQueryParams(queryString)

                val headers = mutableMapOf<String, String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val headerLine = line ?: break
                    if (headerLine.isBlank()) break
                    val parts = headerLine.split(":", limit = 2)
                    if (parts.size == 2) {
                        headers[parts[0].trim().lowercase()] = parts[1].trim()
                    }
                }

                when {
                    method == "GET" && path == "/note" -> {
                        val bookId = queryParams["bookId"] ?: ""
                        val profileId = queryParams["profileId"] ?: ""
                        serveNotePage(output, bookId, profileId)
                    }
                    (method == "POST") && (path == "/note" || path == "/api/note") -> {
                        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                        val bodyChars = CharArray(contentLength)
                        var readTotal = 0
                        while (readTotal < contentLength) {
                            val r = reader.read(bodyChars, readTotal, contentLength - readTotal)
                            if (r == -1) break
                            readTotal += r
                        }
                        val body = String(bodyChars, 0, readTotal)
                        val formParams = parseQueryParams(body)
                        val bookId = formParams["bookId"] ?: queryParams["bookId"] ?: ""
                        val profileId = formParams["profileId"] ?: queryParams["profileId"] ?: ""
                        val noteText = formParams["noteText"] ?: formParams["content"] ?: formParams["note"] ?: ""
                        val spreadIndex = (formParams["spreadIndex"] ?: "0").toIntOrNull() ?: 0
                        if (bookId.isNotBlank() && noteText.isNotBlank()) {
                            repository.addNote(bookId = bookId, text = noteText, spreadIndex = spreadIndex, profileId = profileId)
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
                        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                        val contentType = headers["content-type"] ?: ""
                        handleFileUpload(input, output, contentLength, contentType)
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
                <title>BookSpread — Importar Libro por WiFi</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0C0A09; color: #F7F4EE; text-align: center; padding: 24px; margin: 0; }
                    .card { max-width: 480px; margin: 20px auto; background: #181513; border-radius: 16px; padding: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.6); border: 1px solid #423419; }
                    h1 { color: #C5A059; font-size: 24px; margin-bottom: 8px; letter-spacing: 1px; }
                    p { color: #A8A29E; font-size: 14px; line-height: 1.5; }
                    .drop-zone { border: 2px dashed #C5A059; border-radius: 12px; padding: 32px 16px; margin: 20px 0; background: rgba(197, 160, 89, 0.06); cursor: pointer; }
                    input[type="file"] { display: none; }
                    .btn { background: #C5A059; color: #0C0A09; border: none; padding: 14px 28px; font-size: 16px; font-weight: bold; border-radius: 24px; cursor: pointer; width: 100%; margin-top: 12px; }
                    .btn:hover { background: #D4AF37; }
                    .status { margin-top: 16px; font-weight: bold; color: #C5A059; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>📖 BookSpread</h1>
                    <p>Sube libros (.epub, .pdf) o cómics (.cbz / .cbr) directamente a tu televisor.</p>
                    <form action="/upload" method="post" enctype="multipart/form-data" id="uploadForm">
                        <div class="drop-zone" onclick="document.getElementById('fileInput').click()">
                            <p id="dropText">📁 Haz clic aquí para seleccionar tu archivo EPUB / PDF / CBZ</p>
                            <input type="file" name="file" id="fileInput" accept=".epub,.pdf,.cbz,.cbr" onchange="fileSelected()">
                        </div>
                        <button type="submit" class="btn">🚀 Enviar a BookSpread</button>
                    </form>
                    <div class="status" id="statusMsg"></div>
                </div>
                <script>
                    function fileSelected() {
                        const fi = document.getElementById('fileInput');
                        if (fi.files.length > 0) {
                            document.getElementById('dropText').innerText = "📄 " + fi.files[0].name;
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(response.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun handleFileUpload(
        input: java.io.InputStream,
        output: OutputStream,
        contentLength: Int,
        contentType: String
    ) {
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.tmp")
        try {
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

            if (tempFile.exists() && tempFile.length() > 0) {
                uploadedFilesCount++
                processUploadedFile(tempFile, contentType)

                val successHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><meta charset="UTF-8"><title>¡Enviado!</title>
                    <style>
                        body { background: #121216; color: #fff; font-family: sans-serif; text-align: center; padding: 40px; }
                        .box { background: #181513; border-radius: 16px; padding: 32px; max-width: 400px; margin: auto; border: 1px solid #C5A059; }
                        h2 { color: #C5A059; }
                        p { color: #A8A29E; }
                        a { color: #C5A059; font-weight: bold; text-decoration: none; }
                    </style>
                    </head>
                    <body>
                        <div class="box">
                            <h2>✅ ¡Libro Enviado con Éxito!</h2>
                            <p>El archivo ya está disponible en tu biblioteca de BookSpread.</p>
                            <br>
                            <a href="/">+ Subir otro libro</a>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                val bytes = successHtml.toByteArray(Charsets.UTF_8)
                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
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

    private suspend fun processUploadedFile(tempFile: File, contentType: String) {
        withContext(Dispatchers.IO) {
            try {
                val isPdf = PdfParser.isPdfFile(tempFile) || contentType.contains("pdf")
                val isComic = !isPdf && (ComicParser.isComicFile(tempFile) || contentType.contains("zip") || tempFile.name.endsWith(".cbz"))
                val bookId = "local_wifi_${System.currentTimeMillis()}"

                var title = "Libro Importado WiFi"
                var author = "Importado por WiFi"
                var summary = "Libro importado directamente desde tu dispositivo mediante WiFi."
                var coverPath: String? = null

                when {
                    isPdf -> {
                        title = tempFile.nameWithoutExtension.replace('_', ' ')
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
                        title = tempFile.nameWithoutExtension
                        author = "Cómic"
                        summary = "Cómic importado directamente por WiFi."
                    }
                    else -> {
                        val parsed = EpubParser.parseEpubToBook(tempFile, tempFile.nameWithoutExtension)
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
