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
 * Editorial 3D Book View inspired by Adobe Stock 3D hardcover mockup & Apple Books.
 *
 * - Standby (!isFocused): Renders as a true 3D physical hardcover book:
 *   Visible rounded spine on the left edge with volume/lighting, and front cover angled
 *   in perspective (rotationY = -18f) with hinge reflection, page stack edge, and shelf drop shadow.
 * - Focused (isFocused): Smoothly animates into full forward-facing view (rotationY = 0f),
 *   spine folds away, front cover expands, scales gracefully (scale = 1.08f) with warm focus highlight.
 * - Safe for TV GPU: Avoids hardware elevation crashes on Fire TV OS by using clean Compose
 *   drawing primitives (gradients, borders, and scale) without RenderNode shadow bugs.
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
    // Proportional physical spine width in perspective (e.g. 14dp for 114dp book, 19dp for 158dp banner)
    val baseSpineWidth = (width.value * 0.125f).dp.coerceIn(13.dp, 20.dp)

    // Animations with smooth easing
    val spineWidth by animateDpAsState(
        targetValue = if (isFocused || !enable3DStandby) 0.dp else baseSpineWidth,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "bookSpineWidth"
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
            .width(width)
            .height(height + 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Shelf Floor Drop Shadow (Soft oval contact shadow on the shelf)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.92f)
                .height(14.dp)
                .offset(y = (-2).dp)
                .graphicsLayer {
                    this.scaleX = scale
                    this.alpha = if (isFocused) 0.90f else 0.55f
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

        // 2. Focused Ambient Warm Halo (Soft golden glow behind the book when selected)
        if (isFocused) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .graphicsLayer {
                        this.scaleX = scale * 1.06f
                        this.scaleY = scale * 1.06f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AmberWarm.copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
            )
        }

        // 3. Physical 3D Book Assembly (Spine + Front Cover)
        Row(
            modifier = Modifier
                .width(width)
                .height(height)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    // NO shadowElevation or clip=true here: prevents Fire OS 7 Mali GPU render crash
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A. Physical 3D Hardcover Spine (Visible in standby perspective, folds away on focus)
            if (spineWidth > 0.5.dp) {
                Box(
                    modifier = Modifier
                        .width(spineWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 0.dp, bottomEnd = 0.dp))
                        .background(Color(0xFF141416))
                ) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF221C16))
                        )
                    }

                    // Cylindrical lighting overlay: curved round highlight + hinge crease
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f), // Left edge curvature shadow
                                        Color.White.copy(alpha = 0.30f), // Cylindrical highlight streak
                                        Color.Black.copy(alpha = 0.22f), // Mid falloff
                                        Color.Black.copy(alpha = 0.75f)  // Deep hinge groove seam
                                    )
                                )
                            )
                    )

                    // Headband details (cabezada superior e inferior del libro)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0xFFE8E5DD).copy(alpha = 0.45f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0xFFE8E5DD).copy(alpha = 0.45f))
                    )
                }
            }

            // B. Front Cover (Rotates in 3D perspective, smoothly faces forward on focus)
            val coverShape = if (spineWidth > 1.dp) {
                RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 5.dp, bottomEnd = 5.dp)
            } else {
                RoundedCornerShape(5.dp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        this.rotationY = rotationY
                        this.cameraDistance = 16f * density
                        this.transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clip(coverShape)
                    .background(Color(0xFF18181D))
                    .border(
                        width = if (isFocused) 2.dp else 0.5.dp,
                        color = if (isFocused) AmberWarm else Color.White.copy(alpha = 0.16f),
                        shape = coverShape
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

                // Front Cover Hinge Lighting Reflection (Next to the spine crease)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),  // Hinge groove shade
                                    Color.White.copy(alpha = 0.24f),  // Hinge highlight reflection
                                    Color.Transparent,                // Smooth blend into cover art
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.20f)   // Right edge subtle vignette
                                ),
                                startX = 0f,
                                endX = 160f
                            )
                        )
                )

                // Page Stack Edge (Right paper block thickness)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFE8E5DD).copy(alpha = 0.25f),
                                    Color(0xFFC8C4B8).copy(alpha = 0.40f)
                                )
                            )
                        )
                )

                // Optional Badge (e.g. "GRATIS", "-45%", "BESTSELLER")
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
        }
    }
}
