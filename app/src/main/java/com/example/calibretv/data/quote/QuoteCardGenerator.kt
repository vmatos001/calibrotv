package com.example.calibretv.data.quote

import android.graphics.Bitmap
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

    fun generateQuoteCardBitmap(
        quoteText: String,
        bookTitle: String,
        author: String
    ): Bitmap {
        val width = 1080
        val height = 1920

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Deep Obsidian Ink Background (#0C0A09)
        canvas.drawColor(Color.parseColor("#0C0A09"))

        // 2. Ornamental Double Gold Border
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

        // Small corner gold accents
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            style = Paint.Style.FILL
        }
        val cornerSize = 12f
        canvas.drawRect(48f, 48f, 48f + cornerSize, 48f + cornerSize, cornerPaint)
        canvas.drawRect(width - 48f - cornerSize, 48f, width - 48f, 48f + cornerSize, cornerPaint)
        canvas.drawRect(48f, height - 48f - cornerSize, 48f + cornerSize, height - 48f, cornerPaint)
        canvas.drawRect(width - 48f - cornerSize, height - 48f - cornerSize, width - 48f, height - 48f, cornerPaint)

        // 3. Top Header: "BOOKSPREAD • SELECCIÓN EDITORIAL"
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.25f
        }
        canvas.drawText("B O O K S P R E A D   •   C I T A   L I T E R A R I A", width / 2f, 160f, headerPaint)

        // Symmetrical gold divider line
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            strokeWidth = 2f
        }
        canvas.drawLine(width / 2f - 180f, 195f, width / 2f + 180f, 195f, linePaint)

        // 4. Large Decorative Gold Quotation Mark (Opening “)
        val quoteMarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            textSize = 140f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            alpha = 180
        }
        canvas.drawText("“", width / 2f, 400f, quoteMarkPaint)

        // 5. Quote Text: Dynamic Serif Text (Centered, Antique Ivory #F7F4EE)
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
        val startY = (height / 2f) - (textHeight / 2f) - 60f

        canvas.save()
        canvas.translate(textMargin.toFloat(), startY)
        staticLayout.draw(canvas)
        canvas.restore()

        // 6. Bottom Attribution: Book Title, Author & Divider
        val attributionY = startY + textHeight + 120f
        canvas.drawLine(width / 2f - 140f, attributionY, width / 2f + 140f, attributionY, linePaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5A059")
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText(bookTitle.uppercase(), width / 2f, attributionY + 60f, titlePaint)

        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A8A29E")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("— $author —", width / 2f, attributionY + 115f, authorPaint)

        // 7. Footer Watermark: "Leído en BookSpread 3D • Android TV"
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B655B")
            textSize = 22f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
        }
        canvas.drawText("Leído en BookSpread 3D • Android TV", width / 2f, height - 100f, footerPaint)

        return bitmap
    }

    fun generateQuoteCardBytes(
        quoteText: String,
        bookTitle: String,
        author: String
    ): ByteArray {
        val bmp = generateQuoteCardBitmap(quoteText, bookTitle, author)
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        bmp.recycle()
        return bytes
    }
}
