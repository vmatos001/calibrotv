package com.example.calibretv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.curator.CuratedBook
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.server.QrCodeGenerator
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.AntiqueIvory
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.StarGold
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceContainerHighest
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.theme.TextSecondary

@Composable
fun CuratedBookModal(
    book: CuratedBook,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isDarkTheme: Boolean = true,
    onDownload: () -> Unit,
    onRead: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionFocusRequester = remember { FocusRequester() }
    val coverBmp = rememberCoverImage(book.coverUrl)

    val qrBitmap = remember(book.affiliateQrUrl, book.id) {
        val targetUrl = book.affiliateQrUrl ?: "https://amazon.es"
        QrCodeGenerator.generateQrBitmap(targetUrl, 260, 260)
    }

    LaunchedEffect(Unit) {
        actionFocusRequester.requestFocus()
    }

    // Fondo semi-transparente oscuro
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isDarkTheme) 0.88f else 0.65f))
            .clickable { onDismiss() }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                    onDismiss()
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        // Tarjeta principal de descripción: Fondo oscuro en Dark Mode, o color papel claro en Light Mode (sin bordes)
        val modalBg = if (isDarkTheme) SurfaceContainer else Color(0xFFF7F5F0) // Papel claro suave

        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(18.dp))
                .background(modalBg)
                .clickable(enabled = false) {}
                .padding(28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Lado izquierdo: Portada del libro
                Book3DView(
                    coverBitmap = coverBmp,
                    title = book.title,
                    width = 180.dp,
                    height = 265.dp,
                    isFocused = false,
                    enable3DStandby = false
                )

                // Centro: Metadatos, sinopsis y ficha editorial
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Badge de Categoría y Año
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categoryBg = if (isDarkTheme) SurfaceContainerHigh else Color(0xFFE8E3D8)
                            Box(
                                modifier = Modifier
                                    .background(categoryBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = book.category.uppercase(),
                                    color = if (isDarkTheme) AccentGold else Color(0xFFB45309),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (book.year != null) {
                                Text(
                                    text = book.year,
                                    color = if (isDarkTheme) TextMuted else Color(0xFF78716C),
                                    fontSize = 11.sp
                                )
                            }
                            if (book.isPublicDomain) {
                                Box(
                                    modifier = Modifier
                                        .background(if (isDarkTheme) Color(0xFF1E3A2F) else Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                    Text(
                                        text = "LIBRE ACCESO",
                                        color = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF15803D),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Título
                        Text(
                            text = book.title,
                            color = if (isDarkTheme) AntiqueIvory else InkPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Autor
                        Text(
                            text = book.author,
                            color = if (isDarkTheme) AccentGold else Color(0xFFB45309),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Calificación y precio
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StarGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${book.rating}",
                                color = if (isDarkTheme) TextPrimary else InkPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "•", color = if (isDarkTheme) TextMuted else Color(0xFF78716C), fontSize = 12.sp)
                            Text(
                                text = book.approximatePrice ?: "Consultar",
                                color = if (book.isPublicDomain) {
                                    if (isDarkTheme) Color(0xFF34D399) else Color(0xFF15803D)
                                } else {
                                    if (isDarkTheme) AccentGold else Color(0xFFB45309)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Sinopsis
                        Text(
                            text = "Sinopsis:",
                            color = if (isDarkTheme) TextMuted else Color(0xFF57534E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = book.summary,
                            color = if (isDarkTheme) TextSecondary else Color(0xFF44403C),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Botones de acción inferiores
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (book.isPublicDomain) {
                            if (isDownloaded) {
                                ModalActionButton(
                                    title = "Leer en 3D",
                                    icon = Icons.Default.MenuBook,
                                    isPrimary = true,
                                    isDarkTheme = isDarkTheme,
                                    focusRequester = actionFocusRequester,
                                    trapLeft = true,
                                    trapRight = false,
                                    onClick = onRead
                                )
                            } else {
                                ModalActionButton(
                                    title = if (isDownloading) "Descargando..." else "Descargar Libro",
                                    icon = Icons.Default.Download,
                                    isPrimary = true,
                                    isDarkTheme = isDarkTheme,
                                    isLoading = isDownloading,
                                    focusRequester = actionFocusRequester,
                                    trapLeft = true,
                                    trapRight = false,
                                    onClick = {
                                        if (!isDownloading) onDownload()
                                    }
                                )
                            }
                        }

                        ModalActionButton(
                            title = "Volver",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            isPrimary = !book.isPublicDomain,
                            isDarkTheme = isDarkTheme,
                            focusRequester = if (!book.isPublicDomain) actionFocusRequester else null,
                            trapLeft = !book.isPublicDomain,
                            trapRight = true,
                            onClick = onDismiss
                        )
                    }
                }

                // Lado derecho: Si es comercial, Código QR dinámico para comprar con el móvil
                // En modo light, esta tarjeta interna es blanca pura. En modo oscuro, mantiene SurfaceContainerHigh (sin bordes)
                if (!book.isPublicDomain) {
                    val qrCardBg = if (isDarkTheme) SurfaceContainerHigh else Color.White

                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(qrCardBg)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "COMPRA OFICIAL",
                            color = if (isDarkTheme) AccentGold else Color(0xFFB45309),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Contenedor blanco con el Código QR generado
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Código QR de compra",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "📱 Escanea con tu móvil para adquirir el libro en la librería oficial.",
                            color = if (isDarkTheme) TextMuted else Color(0xFF78716C),
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    isDarkTheme: Boolean = true,
    isLoading: Boolean = false,
    focusRequester: FocusRequester? = null,
    trapLeft: Boolean = false,
    trapRight: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val unfocusedBg = if (isDarkTheme) {
        if (isPrimary) Color(0xFF423419) else SurfaceContainerHighest
    } else {
        if (isPrimary) Color(0xFF111317) else Color(0xFFE5E0D8)
    }

    val unfocusedBorder = if (isDarkTheme) {
        Color(0xFF453F39)
    } else {
        if (isPrimary) Color(0xFF111317) else Color(0xFFD6D0C7)
    }

    val unfocusedTextColor = if (isDarkTheme) {
        if (isPrimary) AccentGold else TextPrimary
    } else {
        if (isPrimary) Color.White else InkPrimary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) AccentGold else unfocusedBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) AccentGold else unfocusedBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            // Prevenir que el foco salga del modal hacia los elementos de fondo de la pantalla Home
                            true
                        }
                        Key.DirectionLeft -> {
                            if (trapLeft) true else false
                        }
                        Key.DirectionRight -> {
                            if (trapRight) true else false
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = if (isFocused) BackgroundDark else unfocusedTextColor,
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) BackgroundDark else unfocusedTextColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = title,
            color = if (isFocused) BackgroundDark else unfocusedTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
