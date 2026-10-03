package com.example.calibretv.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
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
 * Editorial 3D Hardcover Book Mockup matching physical reference (Nova / Cixin Liu):
 *
 * - Standby (!isFocused && enable3DStandby): The book stands in 3D perspective (rotationY = -18f).
 *   The front cover preserves 100% of the downloaded artwork without any artificial spine crop.
 *   On the right side, the physical closed book's paper page block (bright white fore-edge
 *   with sheet striations) and the back cover board are visible with hardcover lip overhang (ceja).
 *   Camera perspective (cameraDistance = 7f) provides authentic optical foreshortening.
 * - Focused (isFocused): Smoothly animates into full forward-facing view (rotationY = 0f),
 *   page block folds away behind, scales up (1.10f) with elevated floor shadow.
 * - Clean Selection: NO yellow selection outline around the book; the focused state is
 *   indicated naturally by front-facing orientation, scale, and shelf elevation.
 * - Banner Mode (enable3DStandby = false): Stays flat and prestigious without side page block.
 */
@Composable
fun Book3DView(
    coverBitmap: ImageBitmap?,
    title: String,
    modifier: Modifier = Modifier,
    width: Dp = 96.dp,
    height: Dp = 142.dp,
    isFocused: Boolean = false,
    badgeText: String? = null,
    badgeColor: Color = AmberWarm,
    enable3DStandby: Boolean = true
) {
    // Proportional closed-book page block width (matches reference mockup thickness)
    val targetPageWidth = (width.value * 0.17f).dp.coerceIn(14.dp, 22.dp)

    val pageBlockWidth by animateDpAsState(
        targetValue = if (isFocused || !enable3DStandby) 0.dp else targetPageWidth,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "pageBlockWidth"
    )

    val rotationY by animateFloatAsState(
        targetValue = if (isFocused || !enable3DStandby) 0f else -18f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "bookRotationY"
    )

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.10f else 1.0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "bookScale"
    )

    val totalWidth = if (enable3DStandby && !isFocused) width + targetPageWidth + 4.dp else width

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(height + 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Natural Shelf Floor Drop Shadow (Soft contact shadow under the book base)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(width * 1.1f)
                .height(14.dp)
                .offset(x = if (isFocused || !enable3DStandby) 0.dp else 6.dp, y = (-2).dp)
                .graphicsLayer {
                    this.scaleX = scale
                    this.alpha = if (isFocused) 0.85f else 0.55f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.80f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Physical 3D Hardcover Book Assembly (Rotated in true 3D perspective)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(width + pageBlockWidth)
                .height(height)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.rotationY = rotationY
                    this.cameraDistance = 7f // Genuine 3D perspective (Do NOT multiply by density!)
                    this.transformOrigin = if (isFocused) TransformOrigin(0.5f, 0.5f) else TransformOrigin(0.15f, 0.5f)
                }
        ) {
            // LAYER 1: Back Cover Board (Contratapa) - Visible behind pages on the right
            if (pageBlockWidth > 1.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = width + pageBlockWidth - 3.dp, y = 0.dp)
                        .width(3.dp)
                        .height(height)
                        .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                        .background(Color(0xFF1E1C22))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                )
            }

            // LAYER 2: Paper Page Block (El Taco de Páginas / Fore-edge) - Recessed behind front cover
            if (pageBlockWidth > 1.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = width - 2.dp, y = 3.dp) // Recessed by 3dp top and bottom (ceja)
                        .width(pageBlockWidth)
                        .height(height - 6.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF9E9A8E), // Deep shadow where pages emerge from behind front cover
                                    Color(0xFFFAF8F2), // Luminous ivory white paper
                                    Color(0xFFFFFFFF), // Bright center
                                    Color(0xFFE8E4DA)  // Shaded fore-edge near back cover
                                )
                            )
                        )
                ) {
                    // Subtle horizontal micro-striations simulating individual sheet layers
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.06f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.06f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.08f)
                                    )
                                )
                            )
                    )
                }

                // Top Hardcover Board Overhang (Ceja superior sobre las hojas)
                Box(
                    modifier = Modifier
                        .offset(x = width - 2.dp, y = 0.dp)
                        .width(pageBlockWidth)
                        .height(3.dp)
                        .background(Color(0xFF1E1C22))
                )

                // Bottom Hardcover Board Overhang (Ceja inferior bajo las hojas)
                Box(
                    modifier = Modifier
                        .offset(x = width - 2.dp, y = height - 3.dp)
                        .width(pageBlockWidth)
                        .height(3.dp)
                        .background(Color(0xFF1E1C22))
                )

                // Shadow cast by front cover onto the recessed pages
                Box(
                    modifier = Modifier
                        .offset(x = width - 2.dp, y = 3.dp)
                        .width(4.dp)
                        .height(height - 6.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // LAYER 3: Front Cover Board (Tapa Delantera) - Sits in front of the pages
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 1.dp, bottomEnd = 1.dp))
                    .background(Color(0xFF18181D))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = if (isFocused) 0.25f else 0.10f),
                        shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 1.dp, bottomEnd = 1.dp)
                    )
            ) {
                // Book Artwork
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Elegant Editorial Fallback
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
                                .size(32.dp)
                                .align(Alignment.Center)
                        )
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 10.sp,
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

                // Left Hardcover Spine Ridge & Groove (Hendidura y lomo cilíndrico)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.38f), // Spine curvature shadow
                                    Color.White.copy(alpha = 0.18f), // Spine cylinder highlight
                                    Color.Transparent,              // Open artwork surface
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.12f)  // Fore-edge shadow
                                ),
                                startX = 0f,
                                endX = 120f
                            )
                        )
                )

                // Spine Hinge Indent (La hendidura vertical del lomo a ~8dp)
                Box(
                    modifier = Modifier
                        .offset(x = 8.dp)
                        .width(1.5.dp)
                        .height(height)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.15f)
                                )
                            )
                        )
                )

                // Specular Glare Sheen (VanillaTilt glare effect: glossy laminate reflection)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isFocused) 0.16f else 0.10f),
                                    Color.Transparent,
                                    Color.Transparent
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(300f, 300f)
                            )
                        )
                )

                // Optional Discount / Bestseller Badge (Neatly tucked inside cover bounds)
                if (!badgeText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.82f))
                            .border(0.5.dp, badgeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
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
        }
    }
}
