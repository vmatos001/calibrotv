package com.example.calibretv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary

import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storage

enum class DrawerItem(val title: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Default.Home),
    BIBLIOTECA("Biblioteca (Catálogo)", Icons.Default.AutoStories),
    TUS_LIBROS("Tus Libros (Memoria TV)", Icons.Default.Storage),
    IMPORTAR_WIFI("Importar por WiFi", Icons.Default.QrCodeScanner),
    USUARIOS("Usuarios", Icons.Default.Person),
    LECTOR_3D("Lector 3D", Icons.Default.MenuBook),
    AJUSTES("Ajustes", Icons.Default.Settings),
    OPDS("Conexión Servidor", Icons.Default.CloudSync)
}

@Composable
fun TvSideDrawer(
    isOpen: Boolean,
    currentSelection: DrawerItem = DrawerItem.HOME,
    onClose: () -> Unit,
    onItemSelected: (DrawerItem) -> Unit
) {
    BackHandler(enabled = isOpen) {
        onClose()
    }

    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            firstItemFocusRequester.requestFocus()
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    ) {
        // Overlay barrier
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onClose() }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.key == Key.DirectionRight || event.key == Key.Back || event.key == Key.Escape) {
                            onClose()
                            true
                        } else false
                    } else false
                }
        ) {
            // Glassmorphic Drawer Panel
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF101014),
                                Color(0xFF16161C).copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF2E2E36))
                        ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
                    .clickable(enabled = false) {}
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Header: Logo + App Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberWarm),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF131315),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "CALIBRO TV",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Lector 3D • Calibre-Web",
                                    color = CyanElectric,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Navigation Items List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DrawerItem.values().forEachIndexed { index, item ->
                                val isFirst = index == 0
                                DrawerMenuItem(
                                    item = item,
                                    isSelected = currentSelection == item,
                                    modifier = if (isFirst) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                                    onClick = {
                                        onClose()
                                        onItemSelected(item)
                                    }
                                )
                            }
                        }
                    }

                    // Bottom helper text
                    Text(
                        text = "› Derecha o [Atrás] para cerrar",
                        color = TextMuted.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    item: DrawerItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.04f else 1.0f)
            .shadow(if (isFocused) 8.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isFocused -> CyanElectric.copy(alpha = 0.22f)
                    isSelected -> SurfaceContainerHigh
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = when {
                    isFocused -> CyanElectric
                    isSelected -> Color(0xFF33333E)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = when {
                isFocused -> AmberWarm
                isSelected -> CyanElectric
                else -> TextMuted
            },
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = item.title,
            color = when {
                isFocused -> TextPrimary
                isSelected -> TextPrimary
                else -> TextMuted
            },
            fontSize = 14.sp,
            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
