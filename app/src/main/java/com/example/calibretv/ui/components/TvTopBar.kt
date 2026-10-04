package com.example.calibretv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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

import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector

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
    // Menú horizontal contraído centrado en pantalla para el Lector 3D
    // Mismo estilo que el menú contraído vertical: iconos circulares limpios, sin botón de cambiar tema
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF141619))
                .border(1.dp, Color(0xFF242734), RoundedCornerShape(26.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvHorizontalNavIconItem(
                    title = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentTab == TvNavTab.HOME,
                    focusRequester = if (currentTab == TvNavTab.HOME) initialFocusRequester else null,
                    onClick = { onTabSelected(TvNavTab.HOME) }
                )
                TvHorizontalNavIconItem(
                    title = "Biblioteca",
                    icon = Icons.Default.MenuBook,
                    isSelected = currentTab == TvNavTab.BIBLIOTECA,
                    focusRequester = if (currentTab == TvNavTab.BIBLIOTECA) initialFocusRequester else null,
                    onClick = { onTabSelected(TvNavTab.BIBLIOTECA) }
                )
                TvHorizontalNavIconItem(
                    title = "Tus Libros",
                    icon = Icons.Default.Bookmark,
                    isSelected = currentTab == TvNavTab.TUS_LIBROS,
                    focusRequester = if (currentTab == TvNavTab.TUS_LIBROS) initialFocusRequester else null,
                    onClick = { onTabSelected(TvNavTab.TUS_LIBROS) }
                )
                TvHorizontalNavIconItem(
                    title = "Lector 3D",
                    icon = Icons.Default.ViewInAr,
                    isSelected = currentTab == TvNavTab.LECTOR_3D,
                    focusRequester = if (currentTab == TvNavTab.LECTOR_3D) initialFocusRequester else null,
                    onClick = { onTabSelected(TvNavTab.LECTOR_3D) }
                )
                TvHorizontalNavIconItem(
                    title = "Ajustes",
                    icon = Icons.Default.Settings,
                    isSelected = currentTab == TvNavTab.AJUSTES,
                    focusRequester = if (currentTab == TvNavTab.AJUSTES) initialFocusRequester else null,
                    onClick = { onTabSelected(TvNavTab.AJUSTES) }
                )
            }
        }
    }
}

@Composable
private fun TvHorizontalNavIconItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val normalIconTint = Color(0xFF9DA1AA)
    val focusedBg = AmberWarm
    val focusedContentColor = Color(0xFF111317)
    val focusedBorderColor = CyanElectric

    val collapsedBg = when {
        isFocused -> focusedBg
        isSelected -> AmberWarm
        else -> Color.Transparent
    }
    val collapsedTint = when {
        isFocused -> focusedContentColor
        isSelected -> Color(0xFF111317)
        else -> normalIconTint
    }

    Box(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .size(44.dp)
            .scale(if (isFocused) 1.15f else 1.0f)
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
