package com.example.calibretv.data.quote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Aesthetic Quote Card Generator for BookSpread v3.0.
 * Produces ultra high-resolution 1080x1920 (9:16 vertical) editorial cards
 * designed for Instagram Stories, WhatsApp Status, and social sharing.
 * Follows the "Noble Ink & Gold" editorial aesthetic.
 */
object QuoteCardGenerator {

    private val cardCache = ConcurrentHashMap<String, ByteArray>()

    fun storeCard(cardId: String, bytes: ByteArray) {
        cardCache[cardId] = bytes
    }

    fun getCard(cardId: String): ByteArray? {
        return cardCache[cardId]
    }

    /**
     * Selecciona inteligentemente el título del libro correspondiente al idioma de lectura ("es" o "en")
     * cuando el libro contiene múltiples títulos compilados o traducciones bilingües separados por '/' o '|'.
     */
    fun selectTitleForLanguage(rawTitle: String, bookLanguage: String = "es"): String {
        val trimmed = rawTitle.trim()
        if (trimmed.isBlank()) return "CALIBRO TV"

        // Separar por barras o delimitadores comunes: / , | , //
        val rawParts = trimmed.split(Regex("\\s*[/|]+\\s*")).map { it.trim() }.filter { it.isNotBlank() }
        if (rawParts.size <= 1) return trimmed

        val isEs = bookLanguage.startsWith("es", ignoreCase = true)

        fun scoreLanguage(text: String): Pair<Int, Int> {
            val lower = text.lowercase()
            var esScore = 0
            var enScore = 0

            // Caracteres distintivos del español
            if (lower.any { it in "áéíóúñü¿¡" }) esScore += 4

            val esWords = listOf("el", "la", "los", "las", "de", "del", "en", "y", "un", "una", "unos", "unas", "por", "con", "para", "como", "al", "o")
            val enWords = listOf("the", "and", "of", "to", "in", "a", "an", "is", "was", "for", "with", "on", "at", "by", "from", "or")

            val tokens = lower.split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }
            for (token in tokens) {
                if (token in esWords) esScore += 2
                if (token in enWords) enScore += 2
            }
            return Pair(esScore, enScore)
        }

        if (isEs) {
            // Buscar la parte que puntúe en español y no sea dominantemente inglesa
            val esCandidate = rawParts.firstOrNull { part ->
                val (es, en) = scoreLanguage(part)
                es > 0 && es >= en
            }
            if (esCandidate != null) return esCandidate

            val nonEnCandidate = rawParts.firstOrNull { part ->
                val (_, en) = scoreLanguage(part)
                en == 0
            }
            if (nonEnCandidate != null) return nonEnCandidate
        } else {
            // Idioma inglés solicitado
            val enCandidate = rawParts.firstOrNull { part ->
                val (es, en) = scoreLanguage(part)
                en > 0 && en >= es
            }
            if (enCandidate != null) return enCandidate

            val nonEsCandidate = rawParts.firstOrNull { part ->
                val (es, _) = scoreLanguage(part)
                es == 0
            }
            if (nonEsCandidate != null) return nonEsCandidate
        }

        // Por defecto, retornar el primer título
        return rawParts.first()
    }

    fun generateQuoteCardBitmap(
        context: Context? = null,
        quoteText: String,
        bookTitle: String,
        author: String,
        bookLanguage: String = "es"
    ): Bitmap {
        val width = 1080
        val height = 1920

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Fondo Deep Obsidian Ink (#0C0A09)
        canvas.drawColor(Color.parseColor("#0C0A09"))

        // 2. Doble borde ornamental dorado
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#423419")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRect(RectF(48f, 48f, width - 48f, height - 48f), borderPaint)
        canvas.drawRect(RectF(64f, 64f, width - 64f, height - 64f), innerBorderPaint)

        // Acentos dorados en las 4 esquinas
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            style = Paint.Style.FILL
        }
        val cornerSize = 12f
        canvas.drawRect(48f, 48f, 48f + cornerSize, 48f + cornerSize, cornerPaint)
        canvas.drawRect(width - 48f - cornerSize, 48f, width - 48f, 48f + cornerSize, cornerPaint)
        canvas.drawRect(48f, height - 48f - cornerSize, 48f + cornerSize, height - 48f, cornerPaint)
        canvas.drawRect(width - 48f - cornerSize, height - 48f - cornerSize, width - 48f, height - 48f, cornerPaint)

        // 3. Encabezado Superior: "B O O K S P R E A D" centrado (Sección A centrada, B eliminada)
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            textSize = 25f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.28f
        }
        canvas.drawText("B O O K S P R E A D", width / 2f, 160f, headerPaint)

        // Línea divisoria simétrica centrada bajo BOOKSPREAD
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            strokeWidth = 2f
        }
        canvas.drawLine(width / 2f - 110f, 195f, width / 2f + 110f, 195f, linePaint)

        // 4. Texto de la Cita: Dynamic Serif (Centrado, Antique Ivory #F7F4EE)
        val cleanQuote = quoteText.trim().ifBlank {
            "«El libro es una extensión de la memoria y de la imaginación.»"
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F7F4EE")
            textSize = if (cleanQuote.length > 250) 42f else if (cleanQuote.length > 140) 48f else 56f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }

        val textMargin = 120
        val textWidth = width - (textMargin * 2)
        val staticLayout = StaticLayout.Builder.obtain(
            cleanQuote,
            0,
            cleanQuote.length,
            textPaint,
            textWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(12f, 1.3f)
            .build()

        val textHeight = staticLayout.height
        val startY = (height / 2f) - (textHeight / 2f) - 40f

        // Comillas decorativas doradas más cerca del texto (justo encima de la primera línea)
        val quoteMarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            textSize = 105f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            alpha = 195
        }
        canvas.drawText("“", width / 2f, startY - 25f, quoteMarkPaint)

        canvas.save()
        canvas.translate(textMargin.toFloat(), startY)
        staticLayout.draw(canvas)
        canvas.restore()

        // 5. Atribución Inferior: Título seleccionado según idioma, Autor y Divisor
        val attributionY = startY + textHeight + 100f
        canvas.drawLine(width / 2f - 140f, attributionY, width / 2f + 140f, attributionY, linePaint)

        val displayTitle = selectTitleForLanguage(bookTitle, bookLanguage).trim().uppercase()
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            textSize = if (displayTitle.length > 50) 26f else if (displayTitle.length > 30) 30f else 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val titleMargin = 100
        val titleWidth = width - (titleMargin * 2)
        val titleLayout = StaticLayout.Builder.obtain(
            displayTitle,
            0,
            displayTitle.length,
            titlePaint,
            titleWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(6f, 1.15f)
            .build()

        canvas.save()
        canvas.translate(titleMargin.toFloat(), attributionY + 40f)
        titleLayout.draw(canvas)
        canvas.restore()

        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A8A29E")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val authorY = attributionY + 40f + titleLayout.height + 30f
        canvas.drawText("— $author —", width / 2f, authorY, authorPaint)

        // 6. Pie de Página: Watermark centrado
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B655B")
            textSize = 20f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
        }
        canvas.drawText("Leído en BookSpread 3D • Android TV", width / 2f, height - 90f, footerPaint)

        // 7. Logo en la esquina inferior derecha (no muy llamativo / elegante marca de agua)
        if (context != null) {
            try {
                val logoBmp = BitmapFactory.decodeResource(
                    context.resources,
                    com.example.calibretv.R.drawable.ic_launcher
                )
                if (logoBmp != null) {
                    val logoSize = 84f
                    val logoX = width - 64f - 20f - logoSize
                    val logoY = height - 64f - 20f - logoSize
                    val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        isFilterBitmap = true
                        alpha = 160 // Sutil, no muy llamativo (~60% opacidad)
                    }
                    val dstRect = RectF(logoX, logoY, logoX + logoSize, logoY + logoSize)
                    canvas.drawBitmap(logoBmp, null, dstRect, logoPaint)
                }
            } catch (_: Exception) {}
        }

        return bitmap
    }

    fun generateQuoteCardBitmap(
        quoteText: String,
        bookTitle: String,
        author: String
    ): Bitmap = generateQuoteCardBitmap(
        context = null,
        quoteText = quoteText,
        bookTitle = bookTitle,
        author = author,
        bookLanguage = "es"
    )

    fun generateQuoteCardBytes(
        quoteText: String,
        bookTitle: String,
        author: String,
        context: Context? = null,
        bookLanguage: String = "es"
    ): ByteArray {
        val bmp = generateQuoteCardBitmap(
            context = context,
            quoteText = quoteText,
            bookTitle = bookTitle,
            author = author,
            bookLanguage = bookLanguage
        )
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        bmp.recycle()
        return bytes
    }
}
