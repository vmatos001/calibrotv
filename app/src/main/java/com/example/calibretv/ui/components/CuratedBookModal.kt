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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.curator.CommercialStoreType
import com.example.calibretv.data.curator.CuratedBook
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.server.QrCodeGenerator
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.AntiqueIvory
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.InkSecondary
import com.example.calibretv.theme.StarGold
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceContainerHighest
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.theme.TextSecondary

/**
 * Modal versátil para libros de cartelera y curaduría de BookSpread / CalibroTV.
 * - Si es comercial (!isPublicDomain || difficultyLevel >= 4): Presenta el embudo comercial multi-tienda
 *   en pantalla dividida con QR reactivo grande (260x260 dp) y lista navegable de tiendas.
 * - Si es de dominio público: Presenta la ficha de lectura / descarga directa en 3D.
 */
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
    val isCommercial = !book.isPublicDomain || book.difficultyLevel >= 4

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
        val modalBg = if (isDarkTheme) SurfaceContainer else Color(0xFFF7F5F0)

        Box(
            modifier = Modifier
                .fillMaxWidth(if (isCommercial) 0.90f else 0.86f)
                .fillMaxHeight(if (isCommercial) 0.88f else 0.82f)
                .shadow(
                    elevation = if (isDarkTheme) 0.dp else 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(modalBg)
                .clickable(enabled = false) {}
                .padding(if (isCommercial) 24.dp else 28.dp)
        ) {
            if (isCommercial) {
                CommercialMultiStoreModalContent(
                    book = book,
                    isDarkTheme = isDarkTheme,
                    onDismiss = onDismiss
                )
            } else {
                PublicDomainModalContent(
                    book = book,
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                    isDarkTheme = isDarkTheme,
                    onDownload = onDownload,
                    onRead = onRead,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * 🛒 Modal de Conversión Comercial (Crítico para Ventas)
 * Pantalla dividida:
 * - Izquierda: Portada HD, Título, Autor y lista navegable de tiendas con el control remoto:
 *   [ 🟠 Amazon (Físico / Kindle) ] (Seleccionado por defecto)
 *   [ 🟢 Casa del Libro (España / LATAM) ]
 *   [ 🔵 Google Play Books (Móvil / Tablet) ]
 *   [ Volver ]
 * - Derecha: Código QR grande reactivo (mínimo 250x250 dp). Al mover el cursor entre los botones
 *   de las tiendas, el código QR cambia instantáneamente para mostrar la URL correspondiente.
 * - Mensaje guía oficial: "Apunta la cámara de tu móvil para comprar con seguridad en la tienda oficial".
 */
@Composable
private fun CommercialMultiStoreModalContent(
    book: CuratedBook,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    var selectedStore by remember { mutableStateOf(CommercialStoreType.AMAZON) }
    val initialFocusRequester = remember { FocusRequester() }
    val coverBmp = rememberCoverImage(book.coverUrl)

    val activeUrl = remember(selectedStore, book) {
        book.getStoreUrl(selectedStore)
    }

    // QR Code grande reactivo (260x260 dp)
    val qrBitmap = remember(activeUrl) {
        QrCodeGenerator.generateQrBitmap(activeUrl, 320, 320)
    }

    LaunchedEffect(Unit) {
        initialFocusRequester.requestFocus()
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // ====================================================================
        // LADO IZQUIERDO: Portada HD, Título, Autor y Botones Navegables
        // ====================================================================
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Fila de encabezado: Portada + Detalles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Book3DView(
                        coverBitmap = coverBmp,
                        title = book.title,
                        width = 110.dp,
                        height = 162.dp,
                        isFocused = false,
                        enable3DStandby = false
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Badge Comercial / Nivel
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isDarkTheme) Color(0xFF2E2616) else Color(0xFFFFF3CD),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "EDICIÓN COMERCIAL",
                                    color = if (isDarkTheme) AccentGold else Color(0xFFB45309),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            if (book.year != null) {
                                Text(
                                    text = book.year,
                                    color = if (isDarkTheme) TextMuted else InkSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Título
                        Text(
                            text = book.title,
                            color = if (isDarkTheme) AntiqueIvory else InkPrimary,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Autor
                        Text(
                            text = book.author,
                            color = if (isDarkTheme) AccentGold else Color(0xFFB45309),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Calificación y precio orientativo
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
                            Text(text = "•", color = if (isDarkTheme) TextMuted else InkSecondary, fontSize = 12.sp)
                            Text(
                                text = book.approximatePrice ?: "Ver precio en tienda",
                                color = if (isDarkTheme) AntiqueIvory.copy(alpha = 0.9f) else InkPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Instrucción de selección de tienda
                Text(
                    text = "Selecciona una tienda para abrir en tu móvil:",
                    color = if (isDarkTheme) TextMuted else InkSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                // LISTA NAVEGABLE DE TIENDAS CON EL CONTROL REMOTO
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CommercialStoreType.entries.forEachIndexed { index, store ->
                        val isAmazonDefault = store == CommercialStoreType.AMAZON
                        val isCurrentSelected = selectedStore == store

                        StoreNavigationItem(
                            store = store,
                            isSelected = isCurrentSelected,
                            isDarkTheme = isDarkTheme,
                            modifier = if (isAmazonDefault) Modifier.focusRequester(initialFocusRequester) else Modifier,
                            onFocus = {
                                selectedStore = store
                            },
                            onClick = {
                                selectedStore = store
                            }
                        )
                    }
                }
            }

            // Botón de Volver al final
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                ModalActionButton(
                    title = "Volver",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    isPrimary = false,
                    isDarkTheme = isDarkTheme,
                    onClick = onDismiss
                )
            }
        }

        // ====================================================================
        // LADO DERECHO: Tarjeta con Código QR Grande Reactivo (>= 250x250 dp)
        // ====================================================================
        val qrContainerBg = if (isDarkTheme) SurfaceContainerHigh else Color.White

        Column(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
                .shadow(
                    elevation = if (isDarkTheme) 0.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(qrContainerBg)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Chip de tienda activa seleccionada
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Color(selectedStore.brandHex).copy(alpha = if (isDarkTheme) 0.22f else 0.12f)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${selectedStore.iconEmoji} ${selectedStore.storeName}",
                    color = if (isDarkTheme) Color.White else Color(selectedStore.brandHex),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Contenedor blanco limpio para el Código QR (260x260 dp)
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Código QR de ${selectedStore.storeName}",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Mensaje guía oficial estricto
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Apunta la cámara de tu móvil para comprar con seguridad en la tienda oficial",
                    color = if (isDarkTheme) AntiqueIvory.copy(alpha = 0.9f) else InkPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

/**
 * Botón interactivo para cada tienda comercial en la lista izquierda.
 */
@Composable
private fun StoreNavigationItem(
    store: CommercialStoreType,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val unfocusedBg = when {
        isSelected -> Color(store.brandHex).copy(alpha = if (isDarkTheme) 0.20f else 0.12f)
        isDarkTheme -> Color.White.copy(alpha = 0.05f)
        else -> Color.Black.copy(alpha = 0.04f)
    }

    val focusedBg = if (isDarkTheme) AccentGold else Color(0xFF111317)
    val focusedTextColor = if (isDarkTheme) BackgroundDark else Color.White

    val activeTextColor = when {
        isFocused -> focusedTextColor
        isSelected -> if (isDarkTheme) Color.White else Color(store.brandHex)
        isDarkTheme -> AntiqueIvory
        else -> InkPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.03f else 1.0f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) focusedBg else unfocusedBg)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) AccentGold else if (isSelected) Color(store.brandHex).copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = store.iconEmoji,
                fontSize = 15.sp
            )
            Text(
                text = store.storeName,
                color = activeTextColor,
                fontSize = 13.sp,
                fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Seleccionada",
                tint = if (isFocused) focusedTextColor else Color(store.brandHex),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Ficha de libro de dominio público (Lectura en 3D / Descarga gratuita).
 */
@Composable
private fun PublicDomainModalContent(
    book: CuratedBook,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isDarkTheme: Boolean,
    onDownload: () -> Unit,
    onRead: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionFocusRequester = remember { FocusRequester() }
    val coverBmp = rememberCoverImage(book.coverUrl)

    LaunchedEffect(Unit) {
        actionFocusRequester.requestFocus()
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Book3DView(
            coverBitmap = coverBmp,
            title = book.title,
            width = 180.dp,
            height = 265.dp,
            isFocused = false,
            enable3DStandby = false
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Text(
                    text = book.title,
                    color = if (isDarkTheme) AntiqueIvory else InkPrimary,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = book.author,
                    color = if (isDarkTheme) AccentGold else Color(0xFFB45309),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

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
                        text = "Gratis (Dominio Público)",
                        color = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF15803D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

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

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDownloaded) {
                    ModalActionButton(
                        title = "Leer en 3D",
                        icon = Icons.Default.MenuBook,
                        isPrimary = true,
                        isDarkTheme = isDarkTheme,
                        focusRequester = actionFocusRequester,
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
                        onClick = {
                            if (!isDownloading) onDownload()
                        }
                    )
                }

                ModalActionButton(
                    title = "Volver",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    isPrimary = false,
                    isDarkTheme = isDarkTheme,
                    onClick = onDismiss
                )
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
                        Key.DirectionUp, Key.DirectionDown -> true
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
