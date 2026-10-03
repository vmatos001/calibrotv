package com.example.calibretv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.model.UserProfile
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary

enum class TvNavTab {
    HOME,
    BIBLIOTECA,
    TUS_LIBROS,
    LECTOR_3D,
    AJUSTES
}

@Composable
fun TvTopBar(
    currentTab: TvNavTab,
    onTabSelected: (TvNavTab) -> Unit,
    activeProfile: UserProfile = UserProfile("user_1", "Principal", "#FFA000"),
    onProfileClick: () -> Unit = {},
    onOpenOpds: () -> Unit = {},
    onOpenWifiImport: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onQuickSync: () -> Unit = {},
    initialFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(BackgroundDark.copy(alpha = 0.96f))
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo estilizado idéntico al recuadro blanco de la referencia
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF101014),
                        modifier = Modifier.size(21.dp)
                    )
                }
                Text(
                    text = "CalibroTV",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )
            }

            // Navigation Tabs (Home, Biblioteca, Tus Libros, Lector 3D, Ajustes)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvNavTabItem(
                    title = "Inicio",
                    isSelected = currentTab == TvNavTab.HOME,
                    modifier = if (initialFocusRequester != null && currentTab == TvNavTab.HOME) {
                        Modifier.focusRequester(initialFocusRequester)
                    } else Modifier,
                    onClick = { onTabSelected(TvNavTab.HOME) }
                )
                TvNavTabItem(
                    title = "Biblioteca",
                    isSelected = currentTab == TvNavTab.BIBLIOTECA,
                    modifier = if (initialFocusRequester != null && currentTab == TvNavTab.BIBLIOTECA) {
                        Modifier.focusRequester(initialFocusRequester)
                    } else Modifier,
                    onClick = { onTabSelected(TvNavTab.BIBLIOTECA) }
                )
                TvNavTabItem(
                    title = "Tus Libros",
                    isSelected = currentTab == TvNavTab.TUS_LIBROS,
                    modifier = if (initialFocusRequester != null && currentTab == TvNavTab.TUS_LIBROS) {
                        Modifier.focusRequester(initialFocusRequester)
                    } else Modifier,
                    onClick = { onTabSelected(TvNavTab.TUS_LIBROS) }
                )
                TvNavTabItem(
                    title = "Lector 3D",
                    isSelected = currentTab == TvNavTab.LECTOR_3D,
                    modifier = if (initialFocusRequester != null && currentTab == TvNavTab.LECTOR_3D) {
                        Modifier.focusRequester(initialFocusRequester)
                    } else Modifier,
                    onClick = { onTabSelected(TvNavTab.LECTOR_3D) }
                )
                TvNavTabItem(
                    title = "Ajustes",
                    isSelected = currentTab == TvNavTab.AJUSTES,
                    modifier = if (initialFocusRequester != null && currentTab == TvNavTab.AJUSTES) {
                        Modifier.focusRequester(initialFocusRequester)
                    } else Modifier,
                    onClick = { onTabSelected(TvNavTab.AJUSTES) }
                )
            }

            // Status and User Profile Pill (Nuevo diseño cápsula con acciones y avatar)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val formattedTime = remember {
                    try {
                        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        sdf.format(java.util.Date())
                    } catch (_: Exception) {
                        "12:00"
                    }
                }

                Text(
                    text = formattedTime,
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                TvProfilePill(
                    profile = activeProfile,
                    onOpenProfileSwitcher = onProfileClick,
                    onOpenOpds = onOpenOpds,
                    onOpenWifiImport = onOpenWifiImport,
                    onOpenSettings = onOpenSettings,
                    onQuickSync = onQuickSync
                )
            }
        }
    }
}

@Composable
private fun TvNavTabItem(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .shadow(if (isFocused) 10.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isFocused -> AmberWarm
                    isSelected -> AmberWarm.copy(alpha = 0.85f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) CyanElectric else if (isSelected) AmberWarm else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
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
            .padding(horizontal = 13.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isFocused || isSelected) Color(0xFF131315) else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
