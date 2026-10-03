package com.example.calibretv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
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
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary

@Composable
fun TvSidebar(
    currentTab: TvNavTab,
    onTabSelected: (TvNavTab) -> Unit,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSidebarFocused by remember { mutableStateOf(false) }
    var isForcedExpanded by remember { mutableStateOf(false) }

    val isExpanded = isSidebarFocused || isForcedExpanded
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 210.dp else 68.dp,
        animationSpec = spring(stiffness = 350f),
        label = "SidebarWidth"
    )

    Box(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .padding(vertical = 12.dp, horizontal = 6.dp)
            .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color.Black)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0D0D11)) // Fondo negro grafito idéntico a la imagen
            .border(1.dp, Color(0xFF22222A), RoundedCornerShape(22.dp))
            .onFocusChanged { isSidebarFocused = it.hasFocus }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 18.dp, horizontal = if (isExpanded) 12.dp else 8.dp),
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // TOP: Logo de Marca (Recuadro blanco con ícono)
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { isForcedExpanded = !isForcedExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "CalibroTV Logo",
                        tint = Color(0xFF101014),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isExpanded) {
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "CalibroTV",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // ==========================================
            // CENTER: Ítems de Navegación Vertical
            // ==========================================
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
            ) {
                SidebarNavItem(
                    title = "Dashboard",
                    icon = Icons.Default.Dashboard,
                    isSelected = currentTab == TvNavTab.HOME,
                    isExpanded = isExpanded,
                    onClick = { onTabSelected(TvNavTab.HOME) }
                )

                SidebarNavItem(
                    title = "Biblioteca",
                    icon = Icons.Default.MenuBook,
                    isSelected = currentTab == TvNavTab.BIBLIOTECA,
                    isExpanded = isExpanded,
                    onClick = { onTabSelected(TvNavTab.BIBLIOTECA) }
                )

                SidebarNavItem(
                    title = "Tus Libros",
                    icon = Icons.Default.Bookmark,
                    isSelected = currentTab == TvNavTab.TUS_LIBROS,
                    isExpanded = isExpanded,
                    onClick = { onTabSelected(TvNavTab.TUS_LIBROS) }
                )

                SidebarNavItem(
                    title = "Lector 3D",
                    icon = Icons.Default.ViewInAr,
                    isSelected = currentTab == TvNavTab.LECTOR_3D,
                    isExpanded = isExpanded,
                    onClick = { onTabSelected(TvNavTab.LECTOR_3D) }
                )

                SidebarNavItem(
                    title = "Ajustes",
                    icon = Icons.Default.Settings,
                    isSelected = currentTab == TvNavTab.AJUSTES,
                    isExpanded = isExpanded,
                    onClick = { onTabSelected(TvNavTab.AJUSTES) }
                )
            }

            // ==========================================
            // BOTTOM: Selector de Tema (Dark / Light)
            // ==========================================
            if (isExpanded) {
                ThemePillToggleExpanded(
                    isDarkTheme = isDarkTheme,
                    onToggle = onToggleTheme
                )
            } else {
                ThemePillToggleCollapsed(
                    isDarkTheme = isDarkTheme,
                    onToggle = onToggleTheme
                )
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    if (isExpanded) {
        // Modo Expandido: Icono + Texto
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .scale(if (isFocused) 1.04f else 1.0f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        isFocused -> AmberWarm
                        isSelected -> AmberWarm.copy(alpha = 0.22f)
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = if (isFocused) 1.5.dp else if (isSelected) 1.dp else 0.dp,
                    color = if (isFocused) CyanElectric else if (isSelected) AmberWarm else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter ||
                         event.key == Key.Enter ||
                         event.key == Key.NumPadEnter)
                    ) {
                        onClick()
                        true
                    } else false
                }
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isFocused) Color(0xFF121216) else AmberWarm,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                color = if (isFocused) Color(0xFF121216) else if (isSelected) AmberWarm else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    } else {
        // Modo Colapsado (Rail): Icono centrado con pastilla ámbar en el activo
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(if (isFocused) 1.12f else 1.0f)
                .clip(CircleShape)
                .background(
                    when {
                        isFocused -> AmberWarm
                        isSelected -> AmberWarm
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) CyanElectric else Color.Transparent,
                    shape = CircleShape
                )
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter ||
                         event.key == Key.Enter ||
                         event.key == Key.NumPadEnter)
                    ) {
                        onClick()
                        true
                    } else false
                }
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected || isFocused) Color(0xFF121216) else AmberWarm,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun ThemePillToggleExpanded(
    isDarkTheme: Boolean,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.03f else 1.0f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF17171E))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) CyanElectric else Color(0xFF2C2C38),
                shape = RoundedCornerShape(20.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onToggle()
                    true
                } else false
            }
            .clickable { onToggle() }
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lado Dark
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDarkTheme) Color(0xFF23232C) else Color.Transparent)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NightlightRound,
                contentDescription = null,
                tint = if (isDarkTheme) AmberWarm else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Dark",
                color = if (isDarkTheme) TextPrimary else TextMuted,
                fontSize = 11.sp,
                fontWeight = if (isDarkTheme) FontWeight.Bold else FontWeight.Normal
            )
        }

        // Lado Light
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (!isDarkTheme) AmberWarm else Color.Transparent)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = null,
                tint = if (!isDarkTheme) Color(0xFF121216) else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Light",
                color = if (!isDarkTheme) Color(0xFF121216) else TextMuted,
                fontSize = 11.sp,
                fontWeight = if (!isDarkTheme) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ThemePillToggleCollapsed(
    isDarkTheme: Boolean,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(36.dp)
            .scale(if (isFocused) 1.1f else 1.0f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF17171E))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) CyanElectric else Color(0xFF2C2C38),
                shape = RoundedCornerShape(18.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onToggle()
                    true
                } else false
            }
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isDarkTheme) AmberWarm else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NightlightRound,
                contentDescription = "Modo Oscuro",
                tint = if (isDarkTheme) Color(0xFF121216) else TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (!isDarkTheme) AmberWarm else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = "Modo Claro",
                tint = if (!isDarkTheme) Color(0xFF121216) else TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
