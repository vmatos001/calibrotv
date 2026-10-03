package com.example.calibretv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.calibretv.data.model.UserProfile
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary

@Composable
fun TvProfilePill(
    profile: UserProfile,
    onOpenProfileSwitcher: () -> Unit = {},
    onOpenOpds: () -> Unit = {},
    onOpenWifiImport: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onQuickSync: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    hasNotification: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpandedMenuOpen by remember { mutableStateOf(false) }
    var isPillFocused by remember { mutableStateOf(false) }

    val avatarColor = try {
        Color(android.graphics.Color.parseColor(profile.avatarColorHex))
    } catch (_: Exception) {
        AmberWarm
    }

    Box(modifier = modifier) {
        // Encabezado Principal tipo Cápsula / Píldora
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .scale(if (isPillFocused) 1.05f else 1.0f)
                .shadow(if (isPillFocused) 14.dp else 4.dp, RoundedCornerShape(24.dp), spotColor = AmberWarm)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF131317))
                .border(
                    width = if (isPillFocused) 2.dp else 1.dp,
                    color = if (isPillFocused) AmberWarm else Color(0xFF2C2C34),
                    shape = RoundedCornerShape(24.dp)
                )
                .onFocusChanged { isPillFocused = it.isFocused }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter ||
                         event.key == Key.Enter ||
                         event.key == Key.NumPadEnter)
                    ) {
                        isExpandedMenuOpen = !isExpandedMenuOpen
                        true
                    } else if (event.type == KeyEventType.KeyDown && event.key == Key.Back && isExpandedMenuOpen) {
                        isExpandedMenuOpen = false
                        true
                    } else false
                }
                .clickable { isExpandedMenuOpen = !isExpandedMenuOpen }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            // Botón 1: Acción Rápida (Sincronización OPDS)
            CircleActionButton(
                icon = Icons.Default.CloudSync,
                contentDescription = "Sincronizar OPDS",
                onClick = onQuickSync
            )

            // Botón 2: Acción Rápida (Notificaciones / Actualizaciones)
            Box(contentAlignment = Alignment.Center) {
                CircleActionButton(
                    icon = Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    onClick = onNotificationsClick
                )
                if (hasNotification) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .background(Color(0xFFFF3D00), CircleShape)
                            .border(1.dp, Color(0xFF131317), CircleShape)
                    )
                }
            }

            // Avatar Circular de Usuario con marco
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(avatarColor, avatarColor.copy(alpha = 0.75f))
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profile.name.isNotBlank()) {
                    Text(
                        text = profile.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Saludo personalizado: "Hi, [Nombre]"
            Text(
                text = "Hi, ${profile.name}",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp, end = 8.dp)
            )
        }

        // Tarjeta Desplegable (Dropdown Card)
        if (isExpandedMenuOpen) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = androidx.compose.ui.unit.IntOffset(0, 140),
                onDismissRequest = { isExpandedMenuOpen = false },
                properties = PopupProperties(focusable = true)
            ) {
                ProfileDropdownMenuCard(
                    profile = profile,
                    onViewProfile = {
                        isExpandedMenuOpen = false
                        onOpenProfileSwitcher()
                    },
                    onOpenOpds = {
                        isExpandedMenuOpen = false
                        onOpenOpds()
                    },
                    onOpenWifiImport = {
                        isExpandedMenuOpen = false
                        onOpenWifiImport()
                    },
                    onOpenSettings = {
                        isExpandedMenuOpen = false
                        onOpenSettings()
                    },
                    onDismiss = { isExpandedMenuOpen = false }
                )
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(30.dp)
            .scale(if (isFocused) 1.15f else 1.0f)
            .clip(CircleShape)
            .background(if (isFocused) Color(0xFFFFB300) else AmberWarm)
            .border(
                width = if (isFocused) 1.5.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF131315),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun ProfileDropdownMenuCard(
    profile: UserProfile,
    onViewProfile: () -> Unit,
    onOpenOpds: () -> Unit,
    onOpenWifiImport: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1715)) // Fondo carbón cálido idéntico al mockup
            .border(1.dp, Color(0xFF332D28), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Profile Settings & View Profile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onViewProfile() }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Profile Settings",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "View Profile (${profile.name})",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = Color(0xFF2E2925),
                thickness = 1.dp
            )

            // Menú de opciones de perfil
            ProfileDropdownMenuItem(
                title = "Cambiar Perfil",
                icon = Icons.Default.PersonOutline,
                onClick = onViewProfile
            )

            ProfileDropdownMenuItem(
                title = "Conexión OPDS / Calibre",
                icon = Icons.Default.Dns,
                onClick = onOpenOpds
            )

            ProfileDropdownMenuItem(
                title = "Importar por Wi-Fi",
                icon = Icons.Default.Wifi,
                onClick = onOpenWifiImport
            )

            ProfileDropdownMenuItem(
                title = "Ajustes de Lectura",
                icon = Icons.Default.Settings,
                onClick = onOpenSettings
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = Color(0xFF2E2925),
                thickness = 1.dp
            )

            // Logout / Salir
            ProfileDropdownMenuItem(
                title = "Cambiar de Usuario",
                icon = Icons.AutoMirrored.Filled.Logout,
                isDestructive = true,
                onClick = onViewProfile
            )
        }
    }
}

@Composable
private fun ProfileDropdownMenuItem(
    title: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) AmberWarm.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = if (isFocused) AmberWarm else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                isDestructive -> Color(0xFFFF6B6B)
                isFocused -> AmberWarm
                else -> Color(0xFFD6D0C7)
            },
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = when {
                isDestructive -> Color(0xFFFF6B6B)
                isFocused -> AmberWarm
                else -> TextPrimary
            },
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
        )
    }
}
