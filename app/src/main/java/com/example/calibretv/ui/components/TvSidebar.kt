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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
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
    var focusedItemCount by remember { mutableIntStateOf(0) }
    var isForcedExpanded by remember { mutableStateOf(false) }

    val isExpanded = isSidebarFocused || (focusedItemCount > 0) || isForcedExpanded
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 210.dp else 68.dp,
        animationSpec = spring(stiffness = 350f),
        label = "SidebarWidth"
    )

    // Colores del Sidebar:
    // - En Dark Mode (Invertido): Rail BLANCO puro (#FFFFFF) para alto contraste con fondo de cine
    // - En Light Mode (Bento): Rail CARBÓN (#141619) tal como en la referencia de diseño
    val sidebarBg = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF141619)
    val sidebarBorder = if (isDarkTheme) Color(0xFFE2E6E2) else Color(0xFF22222A)
    val logoBoxBg = if (isDarkTheme) Color(0xFF111317) else Color.White
    val logoIconTint = if (isDarkTheme) Color.White else Color(0xFF101014)
    val brandTitleColor = if (isDarkTheme) Color(0xFF111317) else Color.White

    Box(
        modifier = modifier
            .zIndex(100f)
            .width(sidebarWidth)
            .fillMaxHeight()
            .padding(vertical = 12.dp, horizontal = 6.dp)
            .shadow(
                elevation = if (isExpanded) 24.dp else 16.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (isDarkTheme) Color.Black.copy(alpha = 0.35f) else Color.Black
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
                    onFocusChanged = { if (it) focusedItemCount++ else focusedItemCount = (focusedItemCount - 1).coerceAtLeast(0) },
                    onClick = { onTabSelected(TvNavTab.HOME) }
                )

                SidebarNavItem(
                    title = "Biblioteca",
                    icon = Icons.Default.MenuBook,
                    isSelected = currentTab == TvNavTab.BIBLIOTECA,
                    isExpanded = isExpanded,
                    isDarkTheme = isDarkTheme,
                    focusRequester = if (currentTab == TvNavTab.BIBLIOTECA) focusRequester else null,
                    onFocusChanged = { if (it) focusedItemCount++ else focusedItemCount = (focusedItemCount - 1).coerceAtLeast(0) },
                    onClick = { onTabSelected(TvNavTab.BIBLIOTECA) }
                )

                SidebarNavItem(
                    title = "Tus Libros",
                    icon = Icons.Default.Bookmark,
                    isSelected = currentTab == TvNavTab.TUS_LIBROS,
                    isExpanded = isExpanded,
                    isDarkTheme = isDarkTheme,
                    focusRequester = if (currentTab == TvNavTab.TUS_LIBROS) focusRequester else null,
                    onFocusChanged = { if (it) focusedItemCount++ else focusedItemCount = (focusedItemCount - 1).coerceAtLeast(0) },
                    onClick = { onTabSelected(TvNavTab.TUS_LIBROS) }
                )

                SidebarNavItem(
                    title = "Lector 3D",
                    icon = Icons.Default.ViewInAr,
                    isSelected = currentTab == TvNavTab.LECTOR_3D,
                    isExpanded = isExpanded,
                    isDarkTheme = isDarkTheme,
                    focusRequester = if (currentTab == TvNavTab.LECTOR_3D) focusRequester else null,
                    onFocusChanged = { if (it) focusedItemCount++ else focusedItemCount = (focusedItemCount - 1).coerceAtLeast(0) },
                    onClick = { onTabSelected(TvNavTab.LECTOR_3D) }
                )

                SidebarNavItem(
                    title = "Ajustes",
                    icon = Icons.Default.Settings,
                    isSelected = currentTab == TvNavTab.AJUSTES,
                    isExpanded = isExpanded,
                    isDarkTheme = isDarkTheme,
                    focusRequester = if (currentTab == TvNavTab.AJUSTES) focusRequester else null,
                    onFocusChanged = { if (it) focusedItemCount++ else focusedItemCount = (focusedItemCount - 1).coerceAtLeast(0) },
                    onClick = { onTabSelected(TvNavTab.AJUSTES) }
                )
            }

            // ==========================================
            // BOTTOM: Selector de Tema (Dark / Light)
            // ==========================================
            ThemePillToggle(
                isExpanded = isExpanded,
                isDarkTheme = isDarkTheme,
                onToggle = onToggleTheme,
                onFocusChanged = { if (it) focusedItemCount++ else focusedItemCount = (focusedItemCount - 1).coerceAtLeast(0) }
            )
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
    onFocusChanged: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val normalIconTint = if (isDarkTheme) Color(0xFF4B5563) else Color(0xFF9DA1AA)
    val normalTextColor = if (isDarkTheme) Color(0xFF1F2937) else Color(0xFFD0D4DC)

    val focusedBg = AmberWarm
    val focusedContentColor = Color(0xFF111317)
    val focusedBorderColor = if (isDarkTheme) Color(0xFF111317) else CyanElectric

    val selectedBg = if (isDarkTheme) AmberWarm.copy(alpha = 0.18f) else Color(0xFF28241A)
    val selectedContentColor = if (isDarkTheme) Color(0xFFB45309) else AmberWarm
    val selectedBorderColor = AmberWarm

    val itemShape = if (isExpanded) RoundedCornerShape(12.dp) else CircleShape
    val itemBg = when {
        isFocused -> focusedBg
        isSelected -> if (isExpanded) selectedBg else AmberWarm
        else -> Color.Transparent
    }
    val itemBorderWidth = when {
        isFocused -> if (isExpanded) 1.5.dp else 2.dp
        isSelected -> if (isExpanded) 1.dp else 0.dp
        else -> 0.dp
    }
    val itemBorderColor = when {
        isFocused -> focusedBorderColor
        isSelected -> if (isExpanded) selectedBorderColor else Color.Transparent
        else -> Color.Transparent
    }
    val iconTint = when {
        !isExpanded && isSelected && !isFocused -> Color(0xFF111317)
        isFocused -> focusedContentColor
        isSelected -> selectedContentColor
        else -> normalIconTint
    }
    val textColor = when {
        isFocused -> focusedContentColor
        isSelected -> selectedContentColor
        else -> normalTextColor
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(if (isExpanded) Modifier.fillMaxWidth().height(44.dp) else Modifier.size(44.dp))
            .scale(if (isFocused) (if (isExpanded) 1.03f else 1.12f) else 1.0f)
            .clip(itemShape)
            .background(itemBg)
            .border(itemBorderWidth, itemBorderColor, itemShape)
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChanged(it.isFocused)
            }
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
            .padding(horizontal = if (isExpanded) 12.dp else 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        if (isExpanded) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ThemePillToggle(
    isExpanded: Boolean,
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    val containerBg = if (isDarkTheme) Color(0xFFF1F5F1) else Color(0xFF17171E)
    val containerBorder = if (isDarkTheme) Color(0xFFD1D5DB) else Color(0xFF2C2C38)
    val focusBorder = if (isDarkTheme) Color(0xFF111317) else CyanElectric

    Box(
        modifier = Modifier
            .then(
                if (isExpanded) {
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                } else {
                    Modifier
                        .width(36.dp)
                        .height(64.dp)
                }
            )
            .scale(if (isFocused) 1.05f else 1.0f)
            .clip(RoundedCornerShape(18.dp))
            .background(containerBg)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) focusBorder else containerBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onToggle()
                    true
                } else false
            }
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val darkSideBg = if (isDarkTheme) Color(0xFF111317) else Color.Transparent
                val darkSideTint = if (isDarkTheme) AmberWarm else (if (isDarkTheme) Color(0xFF4B5563) else Color(0xFF9DA1AA))
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

                val lightSideBg = if (!isDarkTheme) AmberWarm else Color.Transparent
                val lightSideTint = if (!isDarkTheme) Color(0xFF111317) else (if (isDarkTheme) Color(0xFF4B5563) else Color(0xFF9DA1AA))
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                        tint = if (isDarkTheme) AmberWarm else Color(0xFF9DA1AA),
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
                        tint = if (!isDarkTheme) Color(0xFF111317) else Color(0xFF9DA1AA),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
