package com.example.calibretv.data.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

/**
 * Motor nativo de renderizado de archivos PDF optimizado para pantallas 16:9 y Android TV.
 *
 * Características clave de rendimiento y seguridad:
 * 1. Acceso thread-safe sincronizado mediante Mutex (PdfRenderer no tolera concurrencia).
 * 2. Cálculo inteligente de pliegos duales (Página 0 como portada solitaria, páginas subsiguientes en pares).
 * 3. Renderizado con preservación de relación de aspecto en memoria RGB_565 o ARGB_8888.
 * 4. Extracción rápida de portada para biblioteca y cartelera.
 */
object PdfParser {

    private const val TAG = "PdfParser"

    data class PdfSpread(
        val spreadIndex: Int,
        val leftPageIndex: Int?,     // 0-indexed; null si es portada solitaria o no hay página
        val rightPageIndex: Int?,    // 0-indexed; null si es la última página impar
        val isCoverSolo: Boolean = false
    )

    /**
     * Comprueba si el archivo dado es un documento PDF.
     */
    fun isPdfFile(file: File): Boolean {
        return file.extension.equals("pdf", ignoreCase = true)
    }

    /**
     * Abre un archivo PDF y retorna un controlador seguro de pliegos.
     */
    fun openDocument(file: File): PdfDocumentHandler? {
        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "El archivo PDF no existe o está vacío: ${file.absolutePath}")
            return null
        }
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            PdfDocumentHandler(file, pfd, renderer)
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo archivo PDF: ${file.name}", e)
            null
        }
    }

    /**
     * Extrae un bitmap miniatura de la primera página para usar como portada.
     */
    suspend fun extractCover(file: File, targetWidth: Int = 400, targetHeight: Int = 600): Bitmap? = withContext(Dispatchers.IO) {
        val handler = openDocument(file) ?: return@withContext null
        try {
            handler.renderPage(0, targetWidth, targetHeight)
        } finally {
            handler.close()
        }
    }
}

/**
 * Controlador de ciclo de vida para un documento PDF abierto.
 * Gestiona el mutex de renderizado y el cálculo de pliegos.
 */
class PdfDocumentHandler(
    val file: File,
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) {
    private val mutex = Mutex()
    private var isClosed = false

    val pageCount: Int
        get() = if (isClosed) 0 else renderer.pageCount

    /**
     * Lista precalculada de pliegos para el libro completo.
     * En formato libro editorial:
     * - Pliego 0: Portada solitaria (página 0 a la derecha o centrada).
     * - Pliegos subsiguientes: Pares (1 y 2, 3 y 4, etc.).
     */
    val spreads: List<PdfParser.PdfSpread> by lazy {
        val total = pageCount
        val list = mutableListOf<PdfParser.PdfSpread>()
        if (total <= 0) return@lazy list

        if (total == 1) {
            list.add(PdfParser.PdfSpread(spreadIndex = 0, leftPageIndex = null, rightPageIndex = 0, isCoverSolo = true))
            return@lazy list
        }

        // Pliego 0: Portada
        list.add(PdfParser.PdfSpread(spreadIndex = 0, leftPageIndex = null, rightPageIndex = 0, isCoverSolo = true))

        var page = 1
        var spreadIdx = 1
        while (page < total) {
            val left = page
            val right = if (page + 1 < total) page + 1 else null
            list.add(PdfParser.PdfSpread(spreadIndex = spreadIdx, leftPageIndex = left, rightPageIndex = right, isCoverSolo = false))
            page += 2
            spreadIdx++
        }
        list
    }

    val totalSpreads: Int
        get() = spreads.size

    /**
     * Renderiza una página individual a un Bitmap escalado que cabe exactamente
     * dentro de (targetWidth x targetHeight) manteniendo el aspect ratio original.
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isClosed || pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null

            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                val srcWidth = page.width
                val srcHeight = page.height

                if (srcWidth <= 0 || srcHeight <= 0) return@withLock null

                // Calcular escala para ajustar dentro de la mitad de pantalla (targetWidth x targetHeight)
                val scale = min(targetWidth.toFloat() / srcWidth, targetHeight.toFloat() / srcHeight)
                val destWidth = (srcWidth * scale).toInt().coerceAtLeast(1)
                val destHeight = (srcHeight * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
                // Fondo blanco inicial (los PDFs pueden tener transparencia por defecto)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                val destRect = Rect(0, 0, destWidth, destHeight)
                page.render(bitmap, destRect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                bitmap
            } catch (e: Exception) {
                Log.e("PdfDocumentHandler", "Error renderizando página $pageIndex de ${file.name}", e)
                null
            } finally {
                try {
                    page?.close()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Cierra el renderizador y el descriptor de archivo de manera segura.
     */
    fun close() {
        if (isClosed) return
        isClosed = true
        try {
            renderer.close()
        } catch (_: Exception) {}
        try {
            pfd.close()
        } catch (_: Exception) {}
    }
}
