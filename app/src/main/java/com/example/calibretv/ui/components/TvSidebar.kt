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
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary

@Composable
fun TvSidebar(
    currentTab: TvNavTab,
    onTabSelected: (TvNavTab) -> Unit,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    focusRequester: FocusRequester? = null,
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

    // Colores del Sidebar:
    // - En Dark Mode (Invertido): Rail BLANCO puro (#FFFFFF) para alto contraste con fondo de cine
    val sidebarBg = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF141619)
    val sidebarBorder = if (isDarkTheme) Color(0xFFE2E6E2) else Color(0xFF22222A)
    val logoBoxBg = if (isDarkTheme) Color(0xFFF1F3F1) else Color(0xFF20232B)
    val logoIconTint = if (isDarkTheme) Color(0xFF111317) else Color.White
    val brandTitleColor = if (isDarkTheme) Color(0xFF111317) else Color.White

    Box(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .padding(vertical = 12.dp, horizontal = 6.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (isDarkTheme) Color.Black.copy(alpha = 0.25f) else Color.Black
            )
            .clip(RoundedCornerShape(22.dp))
            .background(sidebarBg)
            .border(1.dp, sidebarBorder, RoundedCornerShape(22.dp))
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
            // TOP: Logo de Marca
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
                        .background(logoBoxBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "CalibroTV Logo",
                        tint = logoIconTint,
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
                            color = brandTitleColor,
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
                    title = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentTab == TvNavTab.HOME,
                    isExpanded = isExpanded,
                    isDarkTheme = isDarkTheme,
                    focusRequester = if (currentTab == TvNavTab.HOME) focusRequester else null,
                    onClick = { onTabSelected(TvNavTab.HOME) }
                )

                SidebarNavItem(
                    title = "Biblioteca",
                    icon = Icons.Default.MenuBook,
                    isSelected = currentTab == TvNavTab.BIBLIOTECA,
                    isExpanded = isExpanded,
                    isDarkTheme = isDarkTheme,
                    focusRequester = if (currentTab == TvNavTab.BIBLIOTECA) focusRequester else null,
                    onClick = { onTabSelected(TvNavTab.BIBLIOTECA) }
                )

                SidebarNavItem(
                    title = "Tus Libros",
                    icon = Icons.Default.Bookmark,
                    isSelected = currentTab == TvNavTab.TUS_LIBROS,
                    isExpanded = isExpanded,
                    isDarkTheme = isDarkTheme,
                    focusRequester = if (currentTab == TvNavTab.TUS_LIBROS) focusRequester else null,
                    onClick = { onTabSelected(TvNavTab.TUS_LIBROS) }
                )

                SidebarNavItem(
                    title = "Lector 3D",
                    icon = Icons.Default.ViewInAr,
                    isSelected = currentTab == TvNavTab.LECTOR_3D,
                    isExpanded = isExpanded,
                    isDarkTheme = isDarkTheme,
                    focusRequester = if (currentTab == TvNavTab.LECTOR_3D) focusRequester else null,
                    onClick = { onTabSelected(TvNavTab.LECTOR_3D) }
                )

                SidebarNavItem(
                    title = "Ajustes",
                    icon = Icons.Default.Settings,
                    isSelected = currentTab == TvNavTab.AJUSTES,
                    isExpanded = isExpanded,
                    isDarkTheme = isDarkTheme,
                    focusRequester = if (currentTab == TvNavTab.AJUSTES) focusRequester else null,
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
    isDarkTheme: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val normalIconTint = if (isDarkTheme) Color(0xFF6B7280) else Color(0xFF8E929E)
    val normalTextColor = if (isDarkTheme) Color(0xFF1F2937) else Color(0xFFD0D4DC)

    // Botones Invertidos:
    // - En Dark Mode (Rail Blanco): Botón seleccionado es OSCURO (#111317) con icono blanco
    // - En Light Mode (Rail Carbón): Botón seleccionado es BLANCO (#FFFFFF) con icono oscuro
    val selectedBg = if (isDarkTheme) Color(0xFF111317) else Color.White
    val selectedContentColor = if (isDarkTheme) Color.White else Color(0xFF111317)

    val focusedBg = if (isDarkTheme) Color(0xFF111317) else Color.White
    val focusedContentColor = if (isDarkTheme) Color.White else Color(0xFF111317)
    val focusedBorderColor = AmberWarm

    val baseModifier = Modifier
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)

    if (isExpanded) {
        // Modo Expandido: Icono + Texto
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = baseModifier
                .fillMaxWidth()
                .scale(if (isFocused) 1.04f else 1.0f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        isFocused -> focusedBg
                        isSelected -> selectedBg
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                    color = if (isFocused) focusedBorderColor else if (isSelected) selectedBg.copy(alpha = 0.5f) else Color.Transparent,
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
                tint = when {
                    isFocused -> focusedContentColor
                    isSelected -> selectedContentColor
                    else -> normalIconTint
                },
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                color = when {
                    isFocused -> focusedContentColor
                    isSelected -> selectedContentColor
                    else -> normalTextColor
                },
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    } else {
        // Modo Colapsado (Rail): Botón circular/squircle blanco u oscuro invertido
        val collapsedBg = when {
            isFocused -> focusedBg
            isSelected -> selectedBg
            else -> Color.Transparent
        }
        val collapsedTint = when {
            isFocused -> focusedContentColor
            isSelected -> selectedContentColor
            else -> normalIconTint
        }

        Box(
            modifier = baseModifier
                .size(44.dp)
                .scale(if (isFocused) 1.12f else 1.0f)
                .clip(CircleShape)
                .background(collapsedBg)
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) focusedBorderColor else Color.Transparent,
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
                tint = collapsedTint,
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

    val containerBg = if (isDarkTheme) Color(0xFFF1F5F1) else Color(0xFF17171E)
    val containerBorder = if (isDarkTheme) Color(0xFFD1D5DB) else Color(0xFF2C2C38)
    val focusBorder = AmberWarm

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.03f else 1.0f)
            .clip(RoundedCornerShape(20.dp))
            .background(containerBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) focusBorder else containerBorder,
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
        // Lado Dark: Botón Oscuro si activo
        val darkSideBg = if (isDarkTheme) Color(0xFF111317) else Color.Transparent
        val darkSideTint = if (isDarkTheme) Color.White else Color(0xFF8E929E)

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(darkSideBg)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NightlightRound,
                contentDescription = null,
                tint = darkSideTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Dark",
                color = darkSideTint,
                fontSize = 11.sp,
                fontWeight = if (isDarkTheme) FontWeight.Bold else FontWeight.Normal
            )
        }

        // Lado Light: Botón Blanco si activo
        val lightSideBg = if (!isDarkTheme) Color.White else Color.Transparent
        val lightSideTint = if (!isDarkTheme) Color(0xFF111317) else Color(0xFF6B7280)

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(lightSideBg)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = null,
                tint = lightSideTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Light",
                color = lightSideTint,
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

    val containerBg = if (isDarkTheme) Color(0xFFF1F5F1) else Color(0xFF17171E)
    val containerBorder = if (isDarkTheme) Color(0xFFD1D5DB) else Color(0xFF2C2C38)
    val focusBorder = AmberWarm

    Column(
        modifier = Modifier
            .width(36.dp)
            .scale(if (isFocused) 1.1f else 1.0f)
            .clip(RoundedCornerShape(18.dp))
            .background(containerBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) focusBorder else containerBorder,
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
        // Opción Oscura: Botón negro con icono blanco cuando activo
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isDarkTheme) Color(0xFF111317) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NightlightRound,
                contentDescription = "Modo Oscuro",
                tint = if (isDarkTheme) Color.White else Color(0xFF8E929E),
                modifier = Modifier.size(14.dp)
            )
        }

        // Opción Clara: Botón blanco con icono oscuro cuando activo
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (!isDarkTheme) Color.White else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = "Modo Claro",
                tint = if (!isDarkTheme) Color(0xFF111317) else Color(0xFF6B7280),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
