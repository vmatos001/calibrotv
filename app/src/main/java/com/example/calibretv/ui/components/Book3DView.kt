package com.example.calibretv.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.TextPrimary

/**
 * Editorial 3D Book View based on physical closed-book mockup sketch:
 *
 * - Standby (!isFocused): The book stands at a 3D perspective angle (rotationY = -14f).
 *   The front cover preserves 100% of the downloaded artwork (no artificial spine crop).
 *   Along the right edge, the physical closed book's page block is visible (fore-edge paper
 *   pages between the front and back cover boards).
 * - Focused (isFocused): Smoothly animates into full forward-facing view (rotationY = 0f),
 *   page block folds behind, scales up (1.08f) with elevated floor shadow.
 * - Clean Selection: NO yellow selection outline around the book; the focused state is
 *   indicated naturally by front-facing orientation, scale, and shelf elevation.
 */
@Composable
fun Book3DView(
    coverBitmap: ImageBitmap?,
    title: String,
    modifier: Modifier = Modifier,
    width: Dp = 114.dp,
    height: Dp = 162.dp,
    isFocused: Boolean = false,
    badgeText: String? = null,
    badgeColor: Color = AmberWarm,
    enable3DStandby: Boolean = true
) {
    // Proportional closed-book page block width (stack of pages visible on the edge)
    val basePageBlockWidth = (width.value * 0.09f).dp.coerceIn(9.dp, 15.dp)

    val pageBlockWidth by animateDpAsState(
        targetValue = if (isFocused || !enable3DStandby) 0.dp else basePageBlockWidth,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "pageBlockWidth"
    )

    val rotationY by animateFloatAsState(
        targetValue = if (isFocused || !enable3DStandby) 0f else -14f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "bookRotationY"
    )

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "bookScale"
    )

    Box(
        modifier = modifier
            .width(width + (if (enable3DStandby && !isFocused) basePageBlockWidth else 0.dp))
            .height(height + 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Natural Shelf Floor Drop Shadow (Soft contact shadow under the book base)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.92f)
                .height(14.dp)
                .offset(y = (-2).dp)
                .graphicsLayer {
                    this.scaleX = scale
                    this.alpha = if (isFocused) 0.85f else 0.50f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Physical 3D Book Assembly (Front Cover + Closed Pages Fore-edge)
        Row(
            modifier = Modifier
                .height(height)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.rotationY = rotationY
                    this.cameraDistance = 18f * density
                    this.transformOrigin = TransformOrigin(0.08f, 0.5f)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A. Front Cover: 100% of cover art, completely uncropped
            Box(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF18181D))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = if (isFocused) 0.18f else 0.08f),
                        shape = RoundedCornerShape(3.dp)
                    )
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Editorial Fallback Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF2C241B), Color(0xFF17130F))
                                )
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = AmberWarm.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.Center)
                        )
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                }

                // Left spine-hinge shadow overlay (creates the appearance of a bound spine on the left edge)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f), // Left crease
                                    Color.White.copy(alpha = 0.12f), // Hinge highlight
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.15f)  // Soft right edge shade
                                ),
                                startX = 0f,
                                endX = 120f
                            )
                        )
                )

                // Optional Discount / Bestseller Badge
                if (!badgeText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.78f))
                            .border(0.5.dp, badgeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // B. Closed Pages Block (Visible in standby, tucks behind on focus)
            if (pageBlockWidth > 0.5.dp) {
                Box(
                    modifier = Modifier
                        .width(pageBlockWidth)
                        .fillMaxHeight()
                        .offset(x = (-1).dp)
                ) {
                    // Top hardcover board lip (ceja de la tapa)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0xFF26262B))
                    )

                    // Stack of closed paper pages (warm ivory paper block with page lines)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(vertical = 2.5.dp)
                            .clip(RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFBAB5A8), // Inner shadow near the front cover board
                                        Color(0xFFF7F4EB), // Warm paper sheen
                                        Color(0xFFDFD9CB), // Page block body
                                        Color(0xFF26262B)  // Back cover board rim
                                    )
                                )
                            )
                    ) {
                        // Subtle horizontal paper sheet striations
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.07f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.07f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.07f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.07f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.07f)
                                        )
                                    )
                                )
                        )
                    }

                    // Bottom hardcover board lip (ceja inferior de la tapa)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0xFF26262B))
                    )
                }
            }
        }
    }
}
