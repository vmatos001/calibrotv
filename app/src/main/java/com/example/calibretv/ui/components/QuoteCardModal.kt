package com.example.calibretv.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * 📱 Panel Bento de Compartir Frase & Quote Card para CalibroTV.
 *
 * Se despliega lateralmente desde el borde derecho con el mismo estilo y ancho (276.dp)
 * que el panel de Notas:
 * - Contenedor Bento Superior: Código QR y enlace WiFi para descargar la tarjeta 9:16 en el móvil.
 * - Contenedor Bento Inferior: Vista previa de la frase seleccionada con autor y botón Cerrar enfocado para TV.
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
    val scope = rememberCoroutineScope()
    val wifiServer = remember { WifiImportServer.getInstance(context, repository) }
    val closeFocusRequester = remember { FocusRequester() }

    var isVisible by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cardBitmap by remember { mutableStateOf<Bitmap?>(null) }

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

    fun dismissAnimated() {
        scope.launch {
            isVisible = false
            delay(220L)
            onDismiss()
        }
    }

    // Intercepta el botón Atrás del control remoto
    BackHandler {
        dismissAnimated()
    }

    LaunchedEffect(cleanQuote, cardId) {
        if (!wifiServer.isRunning) {
            wifiServer.startServer(8080)
        }

        // 1. Generar la tarjeta editorial en Bitmap de alta resolución (9:16)
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
        val previewUrl = wifiServer.buildUrl("/quote-preview?cardId=$cardId")
        qrBitmap = QrCodeGenerator.generateQrBitmap(previewUrl, 300, 300)

        isVisible = true

        delay(150L)
        try {
            closeFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Capa base que cubre la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                    dismissAnimated()
                    true
                } else false
            }
    ) {
        // Scrim semitransparente izquierdo (hacer clic o tap cierra el panel)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { dismissAnimated() }
        )

        // Contenedor Bento anclado a la derecha con animación deslizante
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .width(276.dp)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp, end = 16.dp)
                        .clickable(enabled = false) {}, // Evita cerrar al hacer click dentro del bento
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ==========================================
                    // 1. CONTENEDOR BENTO SUPERIOR: QR COMPARTIR
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF16161A).copy(alpha = 0.98f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Título arriba del recuadro del QR
                            Text(
                                text = "Compartir Frase",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Recuadro del QR
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap!!.asImageBitmap(),
                                        contentDescription = "Código QR para descargar Quote Card",
                                        modifier = Modifier.size(136.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(136.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = AmberWarm,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Descripción abajo
                            Text(
                                text = "Escanea el código QR con tu móvil para descargar la tarjeta 9:16 en alta resolución.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Estado Wifi activo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(Color(0xFF4CAF50), CircleShape)
                                )
                                Text(
                                    text = "Wifi activo",
                                    color = Color(0xFF81C784),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 2. CONTENEDOR BENTO INFERIOR: CITA SELECCIONADA
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(16.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF16161A).copy(alpha = 0.98f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Cabecera Bento Inferior
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = null,
                                        tint = AmberWarm,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Cita",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Botón Cerrar enfocado para TV
                                var isCloseFocused by remember { mutableStateOf(false) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .focusRequester(closeFocusRequester)
                                        .onFocusChanged { isCloseFocused = it.isFocused }
                                        .focusable()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { dismissAnimated() }
                                        .onKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown &&
                                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                                dismissAnimated()
                                                true
                                            } else false
                                        }
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCloseFocused) AmberWarm else Color.White.copy(alpha = 0.08f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isCloseFocused) AmberWarm else Color.White.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Cerrar",
                                        tint = if (isCloseFocused) Color(0xFF131316) else TextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Cerrar",
                                        color = if (isCloseFocused) Color(0xFF131316) else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Tarjeta con la frase seleccionada
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1D1D22))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "“",
                                            color = AmberWarm,
                                            fontSize = 30.sp,
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = cleanQuote,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            fontStyle = FontStyle.Italic,
                                            fontFamily = FontFamily.Serif
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(Color.White.copy(alpha = 0.08f))
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = book.title,
                                            color = AmberWarm,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = book.author,
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
