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
import com.example.calibretv.theme.AntiqueIvory
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextSecondary

@Composable
fun CuratorRow(
    section: CuratorSection,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Cabecera estilizada del personaje curador con tipografía editorial estilo Apple Books
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar circular del arquetipo con aro dorado
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .border(1.5.dp, AccentGold, CircleShape),
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
                            color = AntiqueIvory,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2A2213), RoundedCornerShape(4.dp))
                                .border(0.5.dp, AccentGold.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "CARTELERA",
                                color = AccentGold,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Text(
                        text = section.tagline,
                        color = TextMuted,
                        fontSize = 11.5.sp
                    )
                }
            }

            // Cita célebre del personaje
            Text(
                text = section.archetype.quote,
                color = AccentGold.copy(alpha = 0.85f),
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
            contentPadding = PaddingValues(horizontal = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(section.books, key = { _, b -> b.id }) { index, book ->
                CuratedBookCard(
                    book = book,
                    isFirst = index == 0,
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
    onClick: () -> Unit,
    onLeftAtBoundary: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(book.coverUrl)

    Column(
        modifier = Modifier
            .width(116.dp)
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
            .focusable()
            .clickable { onClick() }
    ) {
        Book3DView(
            coverBitmap = coverBmp,
            title = book.title,
            width = 116.dp,
            height = 164.dp,
            isFocused = isFocused,
            badgeText = if (book.isPublicDomain) "GRATIS" else "QR COMPRA",
            badgeColor = if (book.isPublicDomain) Color(0xFF34D399) else AccentGold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = book.title,
            color = if (isFocused) AccentGold else AntiqueIvory,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = book.author,
            color = TextSecondary,
            fontSize = 9.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
