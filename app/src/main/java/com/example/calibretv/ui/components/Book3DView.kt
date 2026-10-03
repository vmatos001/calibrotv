package com.example.calibretv.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
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
 * Editorial 3D Hardcover Book Mockup (Arquitectura Parte A y Parte B):
 *
 * - Parte A (Portada):
 *   - En reposo (!isFocused && enable3DStandby): Se aplica perspectiva 3D mediante código (rotationY = 18f,
 *     pivote en el lomo izquierdo). El lateral derecho fuga hacia el fondo de manera natural.
 *   - En selección (isFocused): Se endereza suavemente a 0° ("encuadrada sin perspectiva"),
 *     escalando a 1.10f con elevación.
 *
 * - Parte B (Hojas y Contratapa):
 *   - Representa el canto físico del libro cerrado (taco de páginas marfil con estrías y cejas de tapa dura).
 *   - Es idéntica y reutilizable para todos los libros ("se duplica en todos los libros").
 *   - En reposo: Se sitúa al lado derecho de la Parte A, encajando con su fuga de perspectiva.
 *   - En selección: Se oculta suavemente detrás de la Parte A para simular que el libro ha girado de frente.
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
    val targetPageWidth = (width.value * 0.17f).dp.coerceIn(14.dp, 20.dp)

    // Animación de la Parte B (se oculta detrás de la Parte A al seleccionar)
    val partBWidth by animateDpAsState(
        targetValue = if (isFocused || !enable3DStandby) 0.dp else targetPageWidth,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "partBWidth"
    )

    val partBAlpha by animateFloatAsState(
        targetValue = if (isFocused || !enable3DStandby) 0f else 1f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "partBAlpha"
    )

    // En perspectiva (rotationY = 18f con transformOrigin en 0f), el ancho proyectado en pantalla
    // de la Parte A es aproximadamente width * cos(18°) ≈ width * 0.94.
    // La Parte B arranca solapándose 2dp bajo la Parte A para evitar cualquier rendija.
    val projectedCoverWidth = if (enable3DStandby && !isFocused) width * 0.94f else width
    val partBStartX = projectedCoverWidth - 2.dp

    val totalContainerWidth = if (enable3DStandby && !isFocused) partBStartX + targetPageWidth else width

    Box(
        modifier = modifier
            .width(totalContainerWidth)
            .height(height + 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Sombra de contacto en el estante
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(width * 1.05f)
                .height(14.dp)
                .offset(x = if (isFocused || !enable3DStandby) 0.dp else 4.dp, y = (-2).dp)
                .graphicsLayer {
                    this.alpha = if (isFocused) 0.85f else 0.50f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.80f),
                            Color.Black.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Ensamblado del libro: La Parte B se dibuja primero (debajo de la Parte A)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(totalContainerWidth)
                .height(height)
        ) {
            // =========================================================================
            // PARTE B: Canto físico con hojas y contratapa (Reutilizable en todos los libros)
            // =========================================================================
            if (partBWidth > 0.5.dp && partBAlpha > 0.02f) {
                Book3DPartB(
                    width = partBWidth,
                    height = height,
                    modifier = Modifier
                        .offset(x = partBStartX)
                        .graphicsLayer {
                            this.alpha = partBAlpha
                        }
                )
            }

            // =========================================================================
            // PARTE A: Portada con perspectiva 3D por código (Gira de frente al seleccionar)
            // =========================================================================
            Book3DPartA(
                coverBitmap = coverBitmap,
                title = title,
                width = width,
                height = height,
                isFocused = isFocused,
                badgeText = badgeText,
                badgeColor = badgeColor,
                enable3DStandby = enable3DStandby,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

/**
 * PARTE A: Portada del libro con perspectiva 3D aplicada por código.
 * - En reposo: Gira con rotationY = 18f sobre el eje del lomo izquierdo.
 * - En foco: Vuelve a rotationY = 0f (perfectamente encuadrada y de frente) y escala a 1.10f.
 */
@Composable
private fun Book3DPartA(
    coverBitmap: ImageBitmap?,
    title: String,
    width: Dp,
    height: Dp,
    isFocused: Boolean,
    badgeText: String?,
    badgeColor: Color,
    enable3DStandby: Boolean,
    modifier: Modifier = Modifier
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isFocused || !enable3DStandby) 0f else 18f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "partARotationY"
    )

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.10f else 1.0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "partAScale"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.rotationY = rotationY
                this.cameraDistance = 16f
                this.transformOrigin = if (isFocused) TransformOrigin(0.5f, 0.5f) else TransformOrigin(0f, 0.5f)
            }
            .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 1.5.dp, bottomEnd = 1.5.dp))
            .background(Color(0xFF18181D))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = if (isFocused) 0.25f else 0.10f),
                shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 1.5.dp, bottomEnd = 1.5.dp)
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
            // Placeholder editorial elegante
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

        // Volumen cilíndrico del lomo en el lateral izquierdo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.12f)
                        ),
                        startX = 0f,
                        endX = 120f
                    )
                )
        )

        // Hendidura vertical del lomo encuadernado (a ~8dp)
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

        // Reflejo de brillo especular sobre el laminado (Efecto glare editorial)
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

        // Etiqueta de descuento / porcentaje perfectamente contenida dentro de la tapa
        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
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

/**
 * PARTE B: Canto con hojas y contratapa trasera.
 * - Renderizado en Canvas con precisión geométrica y degradados suaves.
 * - Sigue la fuga de perspectiva de la Parte A sin depender de matrices 3D de GPU.
 * - Reutilizable en todos los libros.
 */
@Composable
private fun Book3DPartB(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        // Puntos de fuga que continúan exactamente la perspectiva de la Parte A
        // Borde izquierdo (x = 0, bajo la tapa delantera):
        val topStart = h * 0.028f    // ~4dp en 142dp
        val bottomStart = h * 0.972f // ~138dp en 142dp

        // Borde derecho (x = w, contratapa):
        val topEnd = h * 0.046f      // ~6.5dp en 142dp
        val bottomEnd = h * 0.954f   // ~135.5dp en 142dp

        val lipThickness = 2.5.dp.toPx()
        val backBoardThickness = 2.8.dp.toPx().coerceAtMost(w * 0.25f)
        val pagesRight = (w - backBoardThickness).coerceAtLeast(0f)

        // 1. Contratapa trasera (Tapa dura posterior que asoma a la derecha)
        val backBoardPath = Path().apply {
            moveTo(pagesRight, topEnd - 1.dp.toPx())
            lineTo(w, topEnd - 1.dp.toPx())
            lineTo(w, bottomEnd + 1.dp.toPx())
            lineTo(pagesRight, bottomEnd + 1.dp.toPx())
            close()
        }
        drawPath(
            path = backBoardPath,
            color = Color(0xFF1E1C22)
        )
        // Brillo sutil en el canto exterior de la contratapa
        drawLine(
            color = Color.White.copy(alpha = 0.20f),
            start = Offset(w - 0.5f, topEnd - 1.dp.toPx()),
            end = Offset(w - 0.5f, bottomEnd + 1.dp.toPx()),
            strokeWidth = 1f
        )

        // 2. Taco de hojas (Bloque de páginas blanco/marfil con micro-estrías)
        val pagesPath = Path().apply {
            moveTo(0f, topStart + lipThickness)
            lineTo(pagesRight, topEnd + lipThickness)
            lineTo(pagesRight, bottomEnd - lipThickness)
            lineTo(0f, bottomStart - lipThickness)
            close()
        }

        drawPath(
            path = pagesPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF888478), // Sombra de contacto profunda bajo la tapa
                    Color(0xFFFAF8F2), // Papel marfil limpio
                    Color(0xFFFFFFFF), // Brillo central
                    Color(0xFFEDE9DF)  // Sombra suave cerca de la contratapa
                ),
                startX = 0f,
                endX = pagesRight
            )
        )

        // Micro-estrías horizontales simulando el apilado de pliegos de papel
        val numStriations = 8
        val pagesHeightStart = (bottomStart - lipThickness) - (topStart + lipThickness)
        val pagesHeightEnd = (bottomEnd - lipThickness) - (topEnd + lipThickness)
        for (i in 1..numStriations) {
            val fraction = i.toFloat() / (numStriations + 1)
            val y1 = (topStart + lipThickness) + pagesHeightStart * fraction
            val y2 = (topEnd + lipThickness) + pagesHeightEnd * fraction
            drawLine(
                color = Color.Black.copy(alpha = 0.07f),
                start = Offset(0f, y1),
                end = Offset(pagesRight, y2),
                strokeWidth = 1f
            )
        }

        // 3. Ceja superior de tapa dura (reborde protector superior)
        val topLipPath = Path().apply {
            moveTo(0f, topStart)
            lineTo(pagesRight, topEnd)
            lineTo(pagesRight, topEnd + lipThickness)
            lineTo(0f, topStart + lipThickness)
            close()
        }
        drawPath(path = topLipPath, color = Color(0xFF1E1C22))

        // 4. Ceja inferior de tapa dura (reborde protector inferior)
        val bottomLipPath = Path().apply {
            moveTo(0f, bottomStart - lipThickness)
            lineTo(pagesRight, bottomEnd - lipThickness)
            lineTo(pagesRight, bottomEnd)
            lineTo(0f, bottomStart)
            close()
        }
        drawPath(path = bottomLipPath, color = Color(0xFF1E1C22))

        // 5. Sombra de contacto arrojada por la Parte A sobre las páginas
        val shadowWidth = 5.dp.toPx().coerceAtMost(w)
        val shadowPath = Path().apply {
            moveTo(0f, topStart + lipThickness)
            lineTo(shadowWidth, topStart + lipThickness + (topEnd - topStart) * (shadowWidth / w))
            lineTo(shadowWidth, bottomStart - lipThickness + (bottomEnd - bottomStart) * (shadowWidth / w))
            lineTo(0f, bottomStart - lipThickness)
            close()
        }
        drawPath(
            path = shadowPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.45f),
                    Color.Transparent
                ),
                startX = 0f,
                endX = shadowWidth
            )
        )
    }
}
