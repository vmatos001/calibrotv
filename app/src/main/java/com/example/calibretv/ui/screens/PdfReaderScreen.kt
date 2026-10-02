package com.example.calibretv.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import com.example.calibretv.data.model.AmbientSound
import com.example.calibretv.data.sound.AmbientSoundManager
import com.example.calibretv.ui.components.NotesModal
import com.example.calibretv.ui.components.QuizDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.pdf.PdfDocumentHandler
import com.example.calibretv.data.pdf.PdfParser
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.AntiqueIvory
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Par de bitmaps que componen un pliego dual (página izquierda, página derecha).
 */
private data class SpreadBitmaps(
    val left: Bitmap?,
    val right: Bitmap?
) {
    fun recycle() {
        try {
            if (left != null && !left.isRecycled) left.recycle()
        } catch (_: Exception) {}
        try {
            if (right != null && !right.isRecycled) right.recycle()
        } catch (_: Exception) {}
    }
}

@Composable
fun PdfReaderScreen(
    book: Book,
    repository: BookRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var pdfHandler by remember { mutableStateOf<PdfDocumentHandler?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentSpreadIndex by remember { mutableIntStateOf(0) }
    var showHud by remember { mutableStateOf(false) }
    var showQuizModal by remember { mutableStateOf(false) }
    var showNotesModal by remember { mutableStateOf(false) }

    // Memoria estricta: Mantener en caché activa solo una ventana de 3 pliegos (anterior, actual, siguiente)
    val spreadCache = remember { mutableStateMapOf<Int, SpreadBitmaps>() }

    // Paisajes sonoros inmersivos
    val ambientManager = remember { AmbientSoundManager(context) }
    var currentAmbientSound by remember { mutableStateOf(AmbientSound.NONE) }

    // 1. Cargar el archivo PDF y restaurar progreso
    LaunchedEffect(book.id) {
        withContext(Dispatchers.IO) {
            val resolvedFile = repository.resolveBookFile(book)
            if (resolvedFile == null || !resolvedFile.exists()) {
                errorMessage = "No se pudo encontrar el archivo PDF del libro."
                isLoading = false
                return@withContext
            }

            val handler = PdfParser.openDocument(resolvedFile)
            if (handler == null || handler.pageCount == 0) {
                errorMessage = "No se pudo leer el contenido del documento PDF."
                isLoading = false
                return@withContext
            }

            pdfHandler = handler
            val savedSpread = repository.getBookProgress(book.id)
            currentSpreadIndex = savedSpread.coerceIn(0, (handler.totalSpreads - 1).coerceAtLeast(0))
            isLoading = false
            repository.saveLastOpenedBook(book)
        }
    }

    // 2. Liberación segura de recursos nativos y reciclaje de bitmaps al desmontar la pantalla
    DisposableEffect(Unit) {
        onDispose {
            pdfHandler?.close()
            spreadCache.values.forEach { it.recycle() }
            spreadCache.clear()
            ambientManager.release()
        }
    }

    // Función para guardar el progreso actual de lectura en Room y SharedPreferences
    fun saveCurrentProgress(spreadIdx: Int) {
        val handler = pdfHandler ?: return
        val total = handler.totalSpreads
        val percent = if (total > 0) ((spreadIdx.toFloat() / (total - 1).coerceAtLeast(1)) * 100).toInt() else 0
        repository.saveBookProgress(book.id, spreadIdx, percent)
    }

    // Función que descarga/recicla pliegos fuera de la ventana [current - 1, current + 1]
    fun pruneAndLoadSpreads(targetSpread: Int, widthPx: Int, heightPx: Int) {
        val handler = pdfHandler ?: return
        val total = handler.totalSpreads
        if (total == 0) return

        val minKeep = (targetSpread - 1).coerceAtLeast(0)
        val maxKeep = (targetSpread + 1).coerceAtMost(total - 1)
        val window = minKeep..maxKeep

        // Reciclar bitmaps viejos que salieron de la ventana de memoria
        val toEvict = spreadCache.keys.filter { it !in window }
        toEvict.forEach { idx ->
            spreadCache.remove(idx)?.recycle()
        }

        // Cargar los pliegos de la ventana si no están ya en caché
        coroutineScope.launch(Dispatchers.IO) {
            for (idx in window) {
                if (!spreadCache.containsKey(idx)) {
                    val spreadDef = handler.spreads.getOrNull(idx) ?: continue
                    val targetPageW = (widthPx / 2).coerceAtLeast(200)
                    val targetPageH = heightPx.coerceAtLeast(200)

                    val leftBmp = spreadDef.leftPageIndex?.let { handler.renderPage(it, targetPageW, targetPageH) }
                    val rightBmp = spreadDef.rightPageIndex?.let { handler.renderPage(it, targetPageW, targetPageH) }

                    spreadCache[idx] = SpreadBitmaps(leftBmp, rightBmp)
                }
            }
        }
    }

    // Manejar retroceso físico
    BackHandler {
        if (showHud) {
            showHud = false
        } else {
            saveCurrentProgress(currentSpreadIndex)
            onBack()
        }
    }

    // Auto-ocultar HUD tras 5 segundos de inactividad
    LaunchedEffect(showHud) {
        if (showHud) {
            delay(5000)
            showHud = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val handler = pdfHandler
                    val total = handler?.totalSpreads ?: 0

                    when (event.key) {
                        Key.DirectionRight, Key.PageDown, Key.MediaFastForward -> {
                            if (currentSpreadIndex < total - 1) {
                                currentSpreadIndex++
                                saveCurrentProgress(currentSpreadIndex)
                            }
                            true
                        }
                        Key.DirectionLeft, Key.PageUp, Key.MediaRewind -> {
                            if (currentSpreadIndex > 0) {
                                currentSpreadIndex--
                                saveCurrentProgress(currentSpreadIndex)
                            }
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown, Key.DirectionCenter, Key.Enter, Key.Menu -> {
                            showHud = !showHud
                            true
                        }
                        Key.Back, Key.Escape -> {
                            if (showQuizModal || showNotesModal) {
                                showQuizModal = false
                                showNotesModal = false
                                focusRequester.requestFocus()
                            } else if (showHud) {
                                showHud = false
                            } else {
                                saveCurrentProgress(currentSpreadIndex)
                                onBack()
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable {
                showHud = !showHud
            }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }

        // Actualizar la ventana de renderizado al cambiar el pliego o el tamaño de la pantalla
        LaunchedEffect(currentSpreadIndex, pdfHandler, widthPx, heightPx) {
            if (pdfHandler != null && widthPx > 0 && heightPx > 0) {
                pruneAndLoadSpreads(currentSpreadIndex, widthPx, heightPx)
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = AccentGold, modifier = Modifier.size(48.dp))
                        Text(
                            text = "Abriendo documento PDF...",
                            color = AntiqueIvory,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Error al abrir el PDF",
                            color = Color(0xFFEF4444),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentGold)
                                .clickable { onBack() }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Volver a la Biblioteca",
                                color = BackgroundDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            pdfHandler != null -> {
                val handler = pdfHandler!!
                val currentSpreadDef = handler.spreads.getOrNull(currentSpreadIndex)
                val bitmaps = spreadCache[currentSpreadIndex]

                // Visualización del Pliego 16:9
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmaps == null) {
                        // Indicador de carga de pliego rápido
                        CircularProgressIndicator(
                            color = AccentGold.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                    } else if (currentSpreadDef?.isCoverSolo == true) {
                        // Portada única (Página 0 sola y centrada con marco editorial)
                        val coverBmp = bitmaps.right ?: bitmaps.left
                        if (coverBmp != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.92f)
                                    .shadow(16.dp, RoundedCornerShape(4.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = coverBmp.asImageBitmap(),
                                    contentDescription = "Portada",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxHeight()
                                )
                            }
                        }
                    } else {
                        // Pliego Dual Completo (Izquierda | Derecha)
                        Row(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Página Izquierda
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.94f)
                                    .padding(end = 2.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                val leftBmp = bitmaps.left
                                if (leftBmp != null) {
                                    Box(
                                        modifier = Modifier
                                            .shadow(12.dp, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp), spotColor = Color.Black)
                                            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                                            .background(Color.White)
                                    ) {
                                        Image(
                                            bitmap = leftBmp.asImageBitmap(),
                                            contentDescription = "Página Izquierda",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxHeight()
                                        )
                                    }
                                }
                            }

                            // Ranura central / Sombra del lomo encuadernado (Gutter spine)
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight(0.94f)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.45f),
                                                Color.Black.copy(alpha = 0.70f),
                                                Color.Black.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )

                            // Página Derecha
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.94f)
                                    .padding(start = 2.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val rightBmp = bitmaps.right
                                if (rightBmp != null) {
                                    Box(
                                        modifier = Modifier
                                            .shadow(12.dp, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp), spotColor = Color.Black)
                                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                            .background(Color.White)
                                    ) {
                                        Image(
                                            bitmap = rightBmp.asImageBitmap(),
                                            contentDescription = "Página Derecha",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxHeight()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. HUD (Controles superpuestos con información y barra de progreso)
                AnimatedVisibility(
                    visible = showHud,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    PdfReaderTopHud(
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        currentAmbient = currentAmbientSound,
                        onCycleAmbient = {
                            val nextSound = when (currentAmbientSound) {
                                AmbientSound.NONE -> AmbientSound.RAIN
                                AmbientSound.RAIN -> AmbientSound.FIREPLACE
                                AmbientSound.FIREPLACE -> AmbientSound.OCEAN
                                AmbientSound.OCEAN -> AmbientSound.CAFE
                                AmbientSound.CAFE -> AmbientSound.FOREST
                                AmbientSound.FOREST -> AmbientSound.NONE
                            }
                            currentAmbientSound = nextSound
                            if (nextSound == AmbientSound.NONE) {
                                ambientManager.stop()
                            } else {
                                ambientManager.play(nextSound, 0.4f)
                            }
                        },
                        onOpenQuiz = {
                            showHud = false
                            showQuizModal = true
                        },
                        onOpenNotes = {
                            showHud = false
                            showNotesModal = true
                        },
                        onBack = {
                            saveCurrentProgress(currentSpreadIndex)
                            onBack()
                        }
                    )
                }

                AnimatedVisibility(
                    visible = showHud,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    PdfReaderBottomHud(
                        currentSpread = currentSpreadIndex,
                        totalSpreads = handler.totalSpreads,
                        spreadDef = currentSpreadDef,
                        totalPages = handler.pageCount,
                        onPrevSpread = {
                            if (currentSpreadIndex > 0) {
                                currentSpreadIndex--
                                saveCurrentProgress(currentSpreadIndex)
                            }
                        },
                        onNextSpread = {
                            if (currentSpreadIndex < handler.totalSpreads - 1) {
                                currentSpreadIndex++
                                saveCurrentProgress(currentSpreadIndex)
                            }
                        }
                    )
                }
            }
        }

        // ==========================================
        // MINI-QUIZ DE COMPRENSIÓN LECTORA
        // ==========================================
        if (showQuizModal) {
            QuizDialog(
                bookTitle = book.title,
                repository = repository,
                onDismiss = {
                    showQuizModal = false
                    focusRequester.requestFocus()
                }
            )
        }

        // ==========================================
        // ANOTACIONES Y RESEÑAS MÓVILES (FASE 7)
        // ==========================================
        if (showNotesModal) {
            NotesModal(
                book = book,
                repository = repository,
                onDismiss = {
                    showNotesModal = false
                    focusRequester.requestFocus()
                }
            )
        }
    }
}

@Composable
private fun PdfReaderTopHud(
    bookTitle: String,
    bookAuthor: String,
    currentAmbient: AmbientSound,
    onCycleAmbient: () -> Unit,
    onOpenQuiz: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onBack: () -> Unit
) {
    var isBackFocused by remember { mutableStateOf(false) }
    var isAmbientFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark.copy(alpha = 0.95f),
                        BackgroundDark.copy(alpha = 0.70f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 36.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botón Volver estilizado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isBackFocused) AccentGold else SurfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = if (isBackFocused) AccentGold else Color(0xFF2A2826),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onFocusChanged { isBackFocused = it.isFocused }
                    .focusable()
                    .clickable { onBack() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = if (isBackFocused) BackgroundDark else AntiqueIvory,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Biblioteca",
                    color = if (isBackFocused) BackgroundDark else AntiqueIvory,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón Paisaje Sonoro inmersivo
            val ambientLabel = when (currentAmbient) {
                AmbientSound.NONE -> "🔕 Ambiente"
                AmbientSound.RAIN -> "🌧 Lluvia"
                AmbientSound.FIREPLACE -> "🔥 Chimenea"
                AmbientSound.OCEAN -> "🌊 Mar"
                AmbientSound.CAFE -> "☕ Café"
                AmbientSound.FOREST -> "🌲 Bosque"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAmbientFocused) AccentGold else if (currentAmbient != AmbientSound.NONE) Color(0xFF423419) else SurfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = if (isAmbientFocused) AccentGold else Color(0xFF2A2826),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onFocusChanged { isAmbientFocused = it.isFocused }
                    .focusable()
                    .clickable { onCycleAmbient() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isAmbientFocused) BackgroundDark else if (currentAmbient != AmbientSound.NONE) AccentGold else AntiqueIvory,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = ambientLabel,
                    color = if (isAmbientFocused) BackgroundDark else if (currentAmbient != AmbientSound.NONE) AccentGold else AntiqueIvory,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón Mini-Quiz de Comprensión
            var isQuizFocused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isQuizFocused) AccentGold else SurfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = if (isQuizFocused) AccentGold else Color(0xFF2A2826),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onFocusChanged { isQuizFocused = it.isFocused }
                    .focusable()
                    .clickable { onOpenQuiz() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (isQuizFocused) BackgroundDark else AccentGold,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Quiz",
                    color = if (isQuizFocused) BackgroundDark else AntiqueIvory,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón Anotaciones Móviles
            var isNotesFocused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isNotesFocused) AccentGold else SurfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = if (isNotesFocused) AccentGold else Color(0xFF2A2826),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onFocusChanged { isNotesFocused = it.isFocused }
                    .focusable()
                    .clickable { onOpenNotes() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = if (isNotesFocused) BackgroundDark else AccentGold,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Notas",
                    color = if (isNotesFocused) BackgroundDark else AntiqueIvory,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Título del libro y autor
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth(0.70f)
        ) {
            Text(
                text = bookTitle,
                color = AntiqueIvory,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = bookAuthor,
                color = AccentGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PdfReaderBottomHud(
    currentSpread: Int,
    totalSpreads: Int,
    spreadDef: PdfParser.PdfSpread?,
    totalPages: Int,
    onPrevSpread: () -> Unit,
    onNextSpread: () -> Unit
) {
    val progressPercent = if (totalSpreads > 0) {
        ((currentSpread.toFloat() / (totalSpreads - 1).coerceAtLeast(1)) * 100).toInt()
    } else 0

    val pageLabel = remember(currentSpread, spreadDef, totalPages) {
        when {
            spreadDef == null -> ""
            spreadDef.isCoverSolo -> "Portada • Pág. 1 de $totalPages"
            spreadDef.leftPageIndex != null && spreadDef.rightPageIndex != null ->
                "Páginas ${spreadDef.leftPageIndex + 1}-${spreadDef.rightPageIndex + 1} de $totalPages"
            spreadDef.leftPageIndex != null ->
                "Página ${spreadDef.leftPageIndex + 1} de $totalPages"
            spreadDef.rightPageIndex != null ->
                "Página ${spreadDef.rightPageIndex + 1} de $totalPages"
            else -> "Pliego ${currentSpread + 1} de $totalSpreads"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        BackgroundDark.copy(alpha = 0.70f),
                        BackgroundDark.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(horizontal = 36.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Barra de progreso elegante
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(4.dp)
                .background(SurfaceContainerHigh, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressPercent / 100f)
                    .height(4.dp)
                    .background(AccentGold, RoundedCornerShape(2.dp))
            )
        }

        // Fila informativa: Anterior | Páginas & Porcentaje | Siguiente
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "◄ D-Pad Izq",
                color = if (currentSpread > 0) TextMuted else Color(0xFF33302C),
                fontSize = 11.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = pageLabel,
                    color = AntiqueIvory,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "($progressPercent%)",
                    color = AccentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "D-Pad Der ►",
                color = if (currentSpread < totalSpreads - 1) TextMuted else Color(0xFF33302C),
                fontSize = 11.sp
            )
        }
    }
}
