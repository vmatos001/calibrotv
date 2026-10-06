package com.example.calibretv.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.calibretv.ui.utils.KeepScreenOnEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.comic.ComicParser
import com.example.calibretv.data.model.Book
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun rememberLocalImage(file: File?): ImageBitmap? {
    if (file == null || !file.exists()) return null
    return remember(file.absolutePath) {
        try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val reqWidth = 1920
            val reqHeight = 1080
            var sampleSize = 1
            if (boundsOptions.outHeight > reqHeight || boundsOptions.outWidth > reqWidth) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while (halfHeight / sampleSize >= reqHeight && halfWidth / sampleSize >= reqWidth) {
                    sampleSize *= 2
                }
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

data class ComicSpread(
    val leftPage: ComicParser.ComicPage?,
    val rightPage: ComicParser.ComicPage?
)

@Composable
fun ComicReaderScreen(
    book: Book,
    repository: BookRepository,
    onBack: () -> Unit
) {
    var comic by remember { mutableStateOf<ComicParser.ParsedComic?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(book.id) {
        isLoading = true
        comic = repository.loadComic(book)
        isLoading = false
    }

    if (isLoading || comic == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = CyanElectric)
                Text(
                    text = "Cargando páginas del cómic...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )
            }
        }
    } else {
        ComicReaderContent(
            bookId = book.id,
            comic = comic!!,
            isRightToLeft = comic!!.isRightToLeft,
            repository = repository,
            onBack = onBack
        )
    }
}

@Composable
fun ComicReaderScreen(
    comic: ComicParser.ParsedComic,
    isRightToLeft: Boolean,
    onBack: () -> Unit
) {
    ComicReaderContent(
        bookId = comic.title,
        comic = comic,
        isRightToLeft = isRightToLeft,
        repository = null,
        onBack = onBack
    )
}

@Composable
private fun ComicReaderContent(
    bookId: String,
    comic: ComicParser.ParsedComic,
    isRightToLeft: Boolean,
    repository: BookRepository?,
    onBack: () -> Unit
) {
    KeepScreenOnEffect()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val readerFocusRequester = remember { FocusRequester() }
    val hudFocusRequester = remember { FocusRequester() }

    val spreads = remember(comic.pages, isRightToLeft) {
        val list = mutableListOf<ComicSpread>()
        val pages = comic.pages
        var i = 0
        while (i < pages.size) {
            val p1 = pages.getOrNull(i)
            val p2 = pages.getOrNull(i + 1)
            if (isRightToLeft) {
                // Manga RTL: first page on right, next on left
                list.add(ComicSpread(leftPage = p2, rightPage = p1))
            } else {
                // Comic LTR: first page on left, second on right
                list.add(ComicSpread(leftPage = p1, rightPage = p2))
            }
            i += 2
        }
        list
    }

    val initialProgress = remember {
        repository?.getBookProgress(bookId)?.coerceIn(0, (spreads.size - 1).coerceAtLeast(0)) ?: 0
    }

    var currentSpreadIndex by remember { mutableIntStateOf(initialProgress) }
    var targetSpreadIndex by remember { mutableIntStateOf(initialProgress) }
    var isFlipping by remember { mutableStateOf(false) }
    var flipDirectionForward by remember { mutableStateOf(true) }
    val curlAnim = remember { Animatable(0f) }

    var showBottomHud by remember { mutableStateOf(false) }

    BackHandler {
        if (showBottomHud) {
            showBottomHud = false
            readerFocusRequester.requestFocus()
        } else {
            val exitPct = if (spreads.isNotEmpty()) (((currentSpreadIndex + 1) * 100) / spreads.size).coerceIn(1, 100) else 0
            repository?.saveBookProgress(bookId, currentSpreadIndex, exitPct)
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        readerFocusRequester.requestFocus()
    }

    // Exact Apple Books 3D curl animation logic matching ReaderScreen.kt
    fun turnPage(forward: Boolean) {
        if (isFlipping) return
        val nextIdx = if (forward) currentSpreadIndex + 1 else currentSpreadIndex - 1
        if (nextIdx !in spreads.indices) return

        isFlipping = true
        flipDirectionForward = forward
        targetSpreadIndex = nextIdx

        scope.launch {
            val animDuration = 450
            val paperEasing = CubicBezierEasing(0.35f, 0.05f, 0.25f, 1.0f)

            curlAnim.snapTo(0f)
            curlAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(animDuration, easing = paperEasing)
            )

            val pct = if (spreads.isNotEmpty()) (((nextIdx + 1) * 100) / spreads.size).coerceIn(1, 100) else 0
            currentSpreadIndex = nextIdx
            repository?.saveBookProgress(bookId, nextIdx, pct)
            curlAnim.snapTo(0f)
            isFlipping = false
        }
    }

    val currentSpread = spreads.getOrNull(currentSpreadIndex)
    val nextSpread = spreads.getOrNull(targetSpreadIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(readerFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    if (showBottomHud) {
                        when (keyEvent.key) {
                            Key.DirectionUp, Key.Back, Key.Escape -> {
                                showBottomHud = false
                                readerFocusRequester.requestFocus()
                                true
                            }
                            Key.DirectionLeft, Key.DirectionRight, Key.DirectionDown -> false
                            else -> false
                        }
                    } else {
                        when (keyEvent.key) {
                            // Manga (RTL): Left arrow advances, Right arrow goes backward
                            // Comic (LTR): Right arrow advances, Left arrow goes backward
                            Key.DirectionRight, Key.PageDown, Key.MediaFastForward -> {
                                if (isRightToLeft) {
                                    turnPage(forward = false)
                                } else {
                                    turnPage(forward = true)
                                }
                                true
                            }
                            Key.DirectionLeft, Key.PageUp, Key.MediaRewind -> {
                                if (isRightToLeft) {
                                    turnPage(forward = true)
                                } else {
                                    turnPage(forward = false)
                                }
                                true
                            }
                            Key.DirectionDown -> {
                                showBottomHud = true
                                scope.launch {
                                    kotlinx.coroutines.delay(80L)
                                    try { hudFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                true
                            }
                            Key.Back, Key.Escape -> {
                                val exitPct = if (spreads.isNotEmpty()) (((currentSpreadIndex + 1) * 100) / spreads.size).coerceIn(1, 100) else 0
                                repository?.saveBookProgress(bookId, currentSpreadIndex, exitPct)
                                onBack()
                                true
                            }
                            else -> false
                        }
                    }
                } else false
            }
    ) {
        if (currentSpread != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // 1. Base Layer
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val leftPage = if (isFlipping && !flipDirectionForward && nextSpread != null) {
                            nextSpread.leftPage
                        } else {
                            currentSpread.leftPage
                        }
                        ComicPageItem(page = leftPage)
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val rightPage = if (isFlipping && flipDirectionForward && nextSpread != null) {
                            nextSpread.rightPage
                        } else {
                            currentSpread.rightPage
                        }
                        ComicPageItem(page = rightPage)
                    }
                }

                // 2. 3D Turning Page Leaf
                if (isFlipping) {
                    val progress = curlAnim.value
                    val cylindricalFoldAlpha = (kotlin.math.sin(progress * Math.PI.toFloat()) * 0.58f).coerceIn(0f, 0.60f)

                    if (flipDirectionForward) {
                        val angle = -180f * progress
                        val isFront = angle > -90f

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterEnd)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = (1f - progress) * 0.60f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterEnd)
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    rotationY = angle
                                    cameraDistance = 7f * density
                                }
                                .background(Color.Black)
                        ) {
                            if (isFront) {
                                ComicPageItem(page = currentSpread.rightPage)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(80.dp)
                                        .align(Alignment.CenterEnd)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = cylindricalFoldAlpha)
                                                )
                                            )
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                ) {
                                    ComicPageItem(page = nextSpread?.leftPage)
                                }
                            }
                        }

                        if (progress > 0.40f) {
                            val shadowAlpha = (((progress - 0.40f) / 0.60f) * 0.55f).coerceIn(0f, 0.55f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .align(Alignment.CenterStart)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = shadowAlpha)
                                            )
                                        )
                                    )
                            )
                        }
                    } else {
                        val angle = -180f * (1f - progress)
                        val isFront = angle > -90f

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterStart)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = progress * 0.60f)
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterStart)
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(1f, 0.5f)
                                    rotationY = angle
                                    cameraDistance = 7f * density
                                }
                                .background(Color.Black)
                        ) {
                            if (isFront) {
                                ComicPageItem(page = nextSpread?.rightPage)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                ) {
                                    ComicPageItem(page = currentSpread.leftPage)
                                }
                            }
                        }
                    }
                }

                // 3. Central Spine Shadow
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp)
                        .align(Alignment.Center)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // HUD Inferior
        if (showBottomHud) {
            val totalSpreads = spreads.size.coerceAtLeast(1)
            val currentPercent = (((currentSpreadIndex + 1) * 100) / totalSpreads).coerceIn(0, 100)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xEE0D0D11), Color(0xFF0A0A0E))
                        )
                    )
                    .padding(horizontal = 48.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = comic.title,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .background(SurfaceContainerHigh, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isRightToLeft) "🗾 Manga (RTL)" else "📚 Cómic",
                                    color = AmberWarm,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Pliego ${currentSpreadIndex + 1} de $totalSpreads  •  $currentPercent%",
                            color = CyanElectric,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        var isBackFocused by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .focusRequester(hudFocusRequester)
                                .scale(if (isBackFocused) 1.06f else 1.0f)
                                .shadow(if (isBackFocused) 8.dp else 0.dp, RoundedCornerShape(18.dp), spotColor = AmberWarm)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isBackFocused) AmberWarm else SurfaceContainer)
                                .border(
                                    width = if (isBackFocused) 2.dp else 1.dp,
                                    color = if (isBackFocused) Color.White else Color(0xFF333338),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .onFocusChanged { isBackFocused = it.isFocused }
                                .onKeyEvent { e ->
                                    if (e.type == KeyEventType.KeyDown &&
                                        (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                                        val exitPct = if (spreads.isNotEmpty()) (((currentSpreadIndex + 1) * 100) / spreads.size).coerceIn(1, 100) else 0
                                        repository?.saveBookProgress(bookId, currentSpreadIndex, exitPct)
                                        onBack()
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable {
                                    val exitPct = if (spreads.isNotEmpty()) (((currentSpreadIndex + 1) * 100) / spreads.size).coerceIn(1, 100) else 0
                                    repository?.saveBookProgress(bookId, currentSpreadIndex, exitPct)
                                    onBack()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = if (isBackFocused) Color(0xFF131315) else TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Salir",
                                color = if (isBackFocused) Color(0xFF131315) else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicPageItem(
    page: ComicParser.ComicPage?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (page != null) {
            val bmp = rememberLocalImage(page.pageFile)
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = "Página ${page.pageNumber}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(
                    color = CyanElectric,
                    modifier = Modifier.size(32.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${page.pageNumber}",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
