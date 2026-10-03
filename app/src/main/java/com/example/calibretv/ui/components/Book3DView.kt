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
 * Editorial 3D Hardcover Book Mockup matching physical reference (e.g. Cixin Liu / Nova):
 *
 * - Standby (!isFocused): The book stands at a realistic 3D perspective angle (rotationY = -18f).
 *   The front cover displays 100% of the downloaded artwork without any artificial spine crop.
 *   On the right side, the physical closed book's paper page block (bright white fore-edge
 *   with subtle sheet striations) and the back cover board are visible with hardcover lip overhang.
 * - Focused (isFocused): Smoothly animates into full forward-facing view (rotationY = 0f),
 *   page block folds away behind, scales up (1.08f) with elevated floor shadow.
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
    // Proportional closed-book page block width (matches reference mockup thickness)
    val targetPageWidth = (width.value * 0.16f).dp.coerceIn(16.dp, 26.dp)

    val pageBlockWidth by animateDpAsState(
        targetValue = if (isFocused || !enable3DStandby) 0.dp else targetPageWidth,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "pageBlockWidth"
    )

    val rotationY by animateFloatAsState(
        targetValue = if (isFocused || !enable3DStandby) 0f else -18f,
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
            .width(width + (if (enable3DStandby && !isFocused) targetPageWidth else 0.dp))
            .height(height + 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Natural Shelf Floor Drop Shadow (Soft contact shadow cast to the right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.96f)
                .height(16.dp)
                .offset(x = if (isFocused || !enable3DStandby) 0.dp else 4.dp, y = 1.dp)
                .graphicsLayer {
                    this.scaleX = scale
                    this.alpha = if (isFocused) 0.85f else 0.55f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Physical 3D Hardcover Book Assembly (Front Cover + White Page Block + Back Board)
        Row(
            modifier = Modifier
                .height(height)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.rotationY = rotationY
                    this.cameraDistance = 16f * density
                    this.transformOrigin = TransformOrigin(0.06f, 0.5f)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A. Front Cover: 100% of cover art, completely uncropped
            Box(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 1.dp, bottomEnd = 1.dp))
                    .background(Color(0xFF18181D))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = if (isFocused) 0.18f else 0.08f),
                        shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 1.dp, bottomEnd = 1.dp)
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

                // Left spine-hinge volume lighting (simulates the rounded hardcover spine edge on the left)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f), // Left edge shadow
                                    Color.White.copy(alpha = 0.15f), // Cylindrical highlight
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.12f)  // Soft right edge vignette
                                ),
                                startX = 0f,
                                endX = 140f
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

            // B. 3D Closed Page Block & Back Cover Board (As seen in the Nova reference mockup)
            if (pageBlockWidth > 0.5.dp) {
                Row(
                    modifier = Modifier
                        .width(pageBlockWidth)
                        .fillMaxHeight()
                        .offset(x = (-1).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // White Paper Block with Hardcover Overhang (Ceja)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Top hardcover board lip (ceja superior)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(Color(0xFF222026))
                        )

                        // Clean White Paper Stack
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(vertical = 2.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFC8C4B8), // Inner shadow where pages meet front board
                                            Color(0xFFFAF8F2), // Crisp white paper sheen
                                            Color(0xFFFFFFFF), // Bright center
                                            Color(0xFFECE8DE)  // Shaded edge near back board
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
                                                Color.Black.copy(alpha = 0.06f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.06f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.06f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.06f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.06f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Bottom hardcover board lip (ceja inferior)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(Color(0xFF222026))
                        )
                    }

                    // Back Cover Hardcover Board (La contratapa trasera que encierra las hojas)
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF222026))
                            .border(0.3.dp, Color.White.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }
}
