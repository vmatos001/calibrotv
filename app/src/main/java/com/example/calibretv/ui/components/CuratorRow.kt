package com.example.calibretv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.curator.CuratedBook
import com.example.calibretv.data.curator.CuratorArchetype
import com.example.calibretv.data.curator.CuratorSection
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.AntiqueIvory
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.InkSecondary
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextSecondary

@Composable
fun CuratorRow(
    section: CuratorSection,
    isDarkTheme: Boolean = true,
    isInteractive: Boolean = true,
    onBookClick: (CuratedBook) -> Unit,
    onLeftAtBoundary: () -> Unit
) {
    val archetypeIcon: ImageVector = when (section.archetype) {
        CuratorArchetype.PRODIGY -> Icons.Default.School
        CuratorArchetype.DETECTIVE -> Icons.Default.Search
        CuratorArchetype.COSMIC -> Icons.Default.Explore
        CuratorArchetype.CLASSICS -> Icons.Default.AutoStories
        else -> Icons.Default.AutoStories
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Cabecera estilizada del personaje curador con tipografía editorial estilo Apple Books
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar circular del arquetipo:
                // - Versión oscura: quitar aro naranja y fondo blanco (conservando icono naranja) -> fondo blanco, sin borde
                // - Versión light: quitar borde naranja, conservar fondo oscuro y el icono naranja
                val avatarBg = if (isDarkTheme) Color.White else Color(0xFF202020)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = archetypeIcon,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = section.name,
                            color = if (isDarkTheme) AntiqueIvory else InkPrimary,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(if (isDarkTheme) Color(0xFF2A2213) else Color(0xFFFFF3E0), RoundedCornerShape(4.dp))
                                .border(0.5.dp, if (isDarkTheme) AccentGold.copy(alpha = 0.6f) else AmberWarm.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "CARTELERA",
                                color = if (isDarkTheme) AccentGold else AmberWarm,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Text(
                        text = section.tagline,
                        color = if (isDarkTheme) TextMuted else InkSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            // Cita célebre del personaje
            Text(
                text = section.archetype.quote,
                color = if (isDarkTheme) AccentGold.copy(alpha = 0.85f) else AmberWarm,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fila horizontal de libros en 3D (sin tarjeta, con perspectiva y animación al foco)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            itemsIndexed(section.books, key = { _, b -> b.id }) { index, book ->
                CuratedBookCard(
                    book = book,
                    isFirst = index == 0,
                    isDarkTheme = isDarkTheme,
                    isInteractive = isInteractive,
                    onClick = { onBookClick(book) },
                    onLeftAtBoundary = onLeftAtBoundary
                )
            }
        }
    }
}

@Composable
private fun CuratedBookCard(
    book: CuratedBook,
    isFirst: Boolean,
    isDarkTheme: Boolean = true,
    isInteractive: Boolean = true,
    onClick: () -> Unit,
    onLeftAtBoundary: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(book.coverUrl)

    Column(
        modifier = Modifier
            .width(122.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (isFirst) {
                                onLeftAtBoundary()
                                true
                            } else false
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(enabled = isInteractive)
            .clickable(enabled = isInteractive) { onClick() }
    ) {
        Book3DView(
            coverBitmap = coverBmp,
            title = book.title,
            width = 96.dp,
            height = 142.dp,
            isFocused = isFocused
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = book.title,
            color = if (isFocused) AccentGold else (if (isDarkTheme) AntiqueIvory else InkPrimary),
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
