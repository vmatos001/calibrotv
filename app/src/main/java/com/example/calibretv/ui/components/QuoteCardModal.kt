package com.example.calibretv.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.quote.QuoteCardGenerator
import com.example.calibretv.data.server.QrCodeGenerator
import com.example.calibretv.data.server.WifiImportServer
import com.example.calibretv.data.storage.PreferencesManager
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.AntiqueIvory
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.InkSecondary
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Editorial Quote Card Modal for BookSpread v3.0.
 * Renders a high-resolution 9:16 vertical card with the "Noble Ink & Gold" aesthetic
 * and generates a dynamic QR code for instant smartphone download.
 */
@Composable
fun QuoteCardModal(
    book: Book,
    repository: BookRepository,
    selectedQuote: String? = null,
    isDarkTheme: Boolean = PreferencesManager(LocalContext.current).isDarkTheme(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val wifiServer = remember { WifiImportServer.getInstance(context, repository) }
    val closeFocusRequester = remember { FocusRequester() }

    val cleanQuote = remember(selectedQuote) {
        if (!selectedQuote.isNullOrBlank()) {
            selectedQuote.trim()
        } else {
            "«El libro es una extensión de la memoria y de la imaginación.»"
        }
    }

    val cardId = remember {
        "card_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
    }

    var cardBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(cleanQuote, cardId) {
        if (!wifiServer.isRunning) {
            wifiServer.startServer(8080)
        }

        // 1. Generar la tarjeta editorial en Bitmap de alta resolución
        val bmp = QuoteCardGenerator.generateQuoteCardBitmap(
            quoteText = cleanQuote,
            bookTitle = book.title,
            author = book.author
        )
        cardBitmap = bmp

        // 2. Almacenar bytes en memoria para servir por HTTP
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        QuoteCardGenerator.storeCard(cardId, stream.toByteArray())

        // 3. Generar código QR apuntando al preview de descarga móvil
        if (!wifiServer.isRunning) wifiServer.startServer()
        val previewUrl = wifiServer.buildUrl("/quote-preview?cardId=$cardId")
        qrBitmap = QrCodeGenerator.generateQrBitmap(previewUrl, 320, 320)

        closeFocusRequester.requestFocus()
    }

    val modalBg = if (isDarkTheme) SurfaceContainer else Color(0xFFF7F5F0)
    val cardBg = if (isDarkTheme) SurfaceContainerHigh else Color.White
    val textPrimary = if (isDarkTheme) TextPrimary else InkPrimary
    val textSecondary = if (isDarkTheme) TextMuted else InkSecondary
    val badgeBg = if (isDarkTheme) SurfaceContainerHigh else Color(0xFFE2E7E2)
    val badgeText = if (isDarkTheme) AntiqueIvory else InkPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isDarkTheme) 0.90f else 0.65f))
            .clickable { onDismiss() }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                    onDismiss()
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.86f)
                .shadow(28.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(modalBg)
                .clickable(enabled = false) {}
                .padding(28.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header superior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Quote Card Editorial • 9:16 Vertical",
                            color = AccentGold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "1080 × 1920 PX",
                            color = badgeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Contenido central (Vista previa de la tarjeta 9:16 | Panel de descarga QR)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Columna Izquierda: Vista previa de la tarjeta 9:16
                    Box(
                        modifier = Modifier
                            .weight(0.40f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cardBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.95f)
                                    .aspectRatio(9f / 16f)
                                    .shadow(16.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = cardBitmap!!.asImageBitmap(),
                                    contentDescription = "Vista previa de la tarjeta de cita",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.9f)
                                    .aspectRatio(9f / 16f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Generando tarjeta...",
                                    color = textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Columna Derecha: Panel de Descarga QR & Opciones
                    Column(
                        modifier = Modifier
                            .weight(0.60f)
                            .fillMaxHeight()
                            .shadow(if (isDarkTheme) 0.dp else 4.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Descárgala a tu Teléfono",
                            color = AccentGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Escanea el código QR para abrir y descargar la tarjeta en formato PNG de alta resolución sin cables.",
                            color = textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "QR para descargar Quote Card",
                                    modifier = Modifier.size(175.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(175.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDarkTheme) SurfaceContainerHigh else Color(0xFFE8ECE8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Optimizado para Instagram Stories, WhatsApp Status y X",
                                color = textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botón inferior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isCloseFocused by remember { mutableStateOf(false) }
                    val closeBtnBg = if (isCloseFocused) AccentGold else (if (isDarkTheme) SurfaceContainerHigh else Color(0xFFE2E7E2))
                    val closeBtnText = if (isCloseFocused) Color(0xFF111317) else (if (isDarkTheme) AntiqueIvory else InkPrimary)

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .focusRequester(closeFocusRequester)
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionUp || event.key == Key.DirectionDown)) {
                                    true
                                } else false
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = closeBtnBg,
                            contentColor = closeBtnText
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Volver a la Lectura",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
