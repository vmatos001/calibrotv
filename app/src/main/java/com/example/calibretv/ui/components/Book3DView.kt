package com.example.calibretv.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * Editorial 3D Book View inspired by Apple Books & premium hardcover book mockups.
 *
 * - When [isFocused] is false: The book is displayed in a gentle 3D perspective angle
 *   (rotated in Y with spine depth and angled shadow), showing depth along the shelf.
 * - When [isFocused] is true: The book smoothly animates forward facing (rotationY = 0f),
 *   scales up with warm focus elevation and high-contrast sharpness.
 * - Spine highlight & crease: Realistic book hinge lighting on the left edge.
 */
@Composable
fun Book3DView(
    coverBitmap: ImageBitmap?,
    title: String,
    modifier: Modifier = Modifier,
    width: Dp = 112.dp,
    height: Dp = 158.dp,
    isFocused: Boolean = false,
    badgeText: String? = null,
    badgeColor: Color = AmberWarm,
    enable3DStandby: Boolean = true
) {
    // 3D Rotation & Scaling Physics
    val targetRotationY = if (!enable3DStandby || isFocused) 0f else -16f
    val rotationY by animateFloatAsState(
        targetValue = targetRotationY,
        animationSpec = tween(durationMillis = 280),
        label = "bookRotationY"
    )

    val targetScale = if (isFocused) 1.10f else 1.0f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 260),
        label = "bookScale"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isFocused) 18.dp else 4.dp,
        animationSpec = tween(durationMillis = 260),
        label = "bookShadow"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height + 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Shelf Floor Drop Shadow (Soft oval contact shadow under the physical book)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.92f)
                .height(14.dp)
                .offset(y = (-2).dp)
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

        // Physical 3D Hardcover Book
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .graphicsLayer {
                    this.rotationY = rotationY
                    this.scaleX = scale
                    this.scaleY = scale
                    this.cameraDistance = 18f * density
                    this.transformOrigin = TransformOrigin(0.05f, 0.5f)
                    this.shadowElevation = shadowElevation.toPx()
                    this.shape = RoundedCornerShape(
                        topStart = 2.dp,
                        bottomStart = 2.dp,
                        topEnd = 6.dp,
                        bottomEnd = 6.dp
                    )
                    this.clip = true
                }
                .border(
                    width = if (isFocused) 2.dp else 0.5.dp,
                    color = if (isFocused) AmberWarm else Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                )
                .background(Color(0xFF18181D))
        ) {
            if (coverBitmap != null) {
                // Book Cover Art
                Image(
                    bitmap = coverBitmap,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Elegant Editorial Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF2C241B), Color(0xFF17130F))
                            )
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = AmberWarm.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }
            }

            // Real Hardcover Spine & Hinge Lighting Overlay
            // (Creates the realistic curved spine on the left + reflection streak)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.40f),  // Spine edge crease
                                Color.White.copy(alpha = 0.28f),  // Hinge highlight reflection
                                Color.Transparent,                // Falloff
                                Color.Black.copy(alpha = 0.12f),  // Inner groove
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f)   // Right page edge shade
                            ),
                            startX = 0f,
                            endX = 140f
                        )
                    )
            )

            // Right-edge page stack sheen (simulates paper block)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(3.dp)
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFE8E5DD).copy(alpha = 0.22f))
                        )
                    )
            )

            // Optional Badge (e.g. "GRATIS", "-30%", etc.)
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
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
