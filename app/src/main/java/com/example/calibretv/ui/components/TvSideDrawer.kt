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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
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
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary

enum class DrawerItem(val title: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Default.Home),
    BIBLIOTECA("Biblioteca (Catálogo)", Icons.Default.AutoStories),
    TUS_LIBROS("Tus Libros (Memoria TV)", Icons.Default.Storage),
    LECTOR_3D("Lector 3D", Icons.Default.MenuBook),
    IMPORTAR_WIFI("Importar por WiFi", Icons.Default.QrCodeScanner),
    USUARIOS("Usuarios", Icons.Default.Person),
    OPDS("Conexión Servidor", Icons.Default.CloudSync),
    AJUSTES("Ajustes", Icons.Default.Settings)
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
                .background(Color.Black.copy(alpha = 0.70f))
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
            // Glassmorphic Drawer Panel (Compact & Elegant proportions inspired by ReadEra)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F0F13),
                                Color(0xFF141419).copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF282832))
                        ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
                    .clickable(enabled = false) {}
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header: Logo + App Name (compact)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberWarm),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF131315),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "CALIBRO TV",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Lector 3D • Calibre-Web",
                                    color = AmberWarm.copy(alpha = 0.85f),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF23232C))
                        )
                    }

                    // Main Navigation Items (with smooth scrolling to guarantee all items fit on any screen)
                    val mainItems = listOf(
                        DrawerItem.HOME,
                        DrawerItem.BIBLIOTECA,
                        DrawerItem.TUS_LIBROS,
                        DrawerItem.LECTOR_3D,
                        DrawerItem.IMPORTAR_WIFI,
                        DrawerItem.USUARIOS,
                        DrawerItem.OPDS
                    )
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        mainItems.forEachIndexed { index, item ->
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

                    // Pinned Bottom Section (Ajustes + Close Hint, inspired by ReadEra)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF23232C))
                        )

                        DrawerMenuItem(
                            item = DrawerItem.AJUSTES,
                            isSelected = currentSelection == DrawerItem.AJUSTES,
                            onClick = {
                                onClose()
                                onItemSelected(DrawerItem.AJUSTES)
                            }
                        )

                        Text(
                            text = "‹ [Atrás] o [Derecha] para cerrar",
                            color = TextMuted.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 14.dp, top = 2.dp)
                        )
                    }
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.03f else 1.0f)
            .shadow(if (isFocused) 8.dp else 0.dp, RoundedCornerShape(8.dp), spotColor = AmberWarm)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> AmberWarm.copy(alpha = 0.18f)
                    isSelected -> SurfaceContainerHigh
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isFocused) 1.5.dp else if (isSelected) 1.dp else 0.dp,
                color = when {
                    isFocused -> AmberWarm
                    isSelected -> Color(0xFF33333E)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
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
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = when {
                isFocused -> AmberWarm
                isSelected -> AmberWarm
                else -> TextMuted
            },
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = item.title,
            color = when {
                isFocused -> TextPrimary
                isSelected -> TextPrimary
                else -> TextMuted
            },
            fontSize = 13.sp,
            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
