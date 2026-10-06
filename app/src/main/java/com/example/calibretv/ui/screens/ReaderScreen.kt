package com.example.calibretv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableLongStateOf
import com.example.calibretv.data.tts.MediaKeyEventBus
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.sound.AmbientSoundManager
import com.example.calibretv.data.sound.SoundManager
import com.example.calibretv.data.storage.PreferencesManager
import com.example.calibretv.data.tts.PiperTtsClient
import com.example.calibretv.data.tts.TtsController
import com.example.calibretv.data.tts.TtsVoiceCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.example.calibretv.data.epub.EpubParser
import com.example.calibretv.data.epub.PageContent
import com.example.calibretv.data.epub.PageItem
import com.example.calibretv.data.epub.PageSpread
import com.example.calibretv.data.epub.ParsedBook
import com.example.calibretv.data.image.rememberLocalImage
import com.example.calibretv.data.model.AmbientSound
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.model.CurlSpeed
import com.example.calibretv.data.model.TtsEngineMode
import com.example.calibretv.data.model.ReadingFont
import com.example.calibretv.data.model.ReadingSettings
import com.example.calibretv.data.model.ReadingTheme
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.FontProvider
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.ui.components.NotesModal
import com.example.calibretv.ui.components.QuoteCardModal
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.components.TvTopBar
import kotlinx.coroutines.launch

enum class ReaderHudCategory(val title: String, val icon: ImageVector) {
    APARIENCIA("Apariencia", Icons.Default.Palette),
    VOZ_TTS("Voz TTS", Icons.Filled.RecordVoiceOver),
    AMBIENTE("Ambiente", Icons.Filled.MusicNote),
    HERRAMIENTAS("Herramientas", Icons.Default.MenuBook)
}

private fun detectBookLanguage(book: Book, spreads: List<PageSpread>): String {
    // 1. Tag or category check
    val tags = (book.tags + listOf(book.category)).map { it.lowercase() }
    if (tags.any { it in listOf("en", "eng", "english", "inglés", "ingles") }) return "en"
    if (tags.any { it in listOf("es", "spa", "spanish", "español", "espanol") }) return "es"

    // 2. Sample text from the first spreads
    val sb = java.lang.StringBuilder()
    for (i in 0 until minOf(5, spreads.size)) {
        val s = spreads[i]
        val left = s.leftPage.paragraphs.joinToString(" ")
        val right = s.rightPage.paragraphs.joinToString(" ")
        sb.append(" ").append(left).append(" ").append(right)
        if (sb.length > 600) break
    }
    val sample = sb.toString().lowercase()
    if (sample.isNotBlank()) {
        val esWords = listOf(" el ", " la ", " los ", " las ", " de ", " que ", " en ", " y ", " un ", " una ", " por ", " con ", " para ", " como ")
        val enWords = listOf(" the ", " and ", " of ", " to ", " in ", " that ", " is ", " was ", " he ", " with ", " for ", " as ", " on ", " at ")
        var esCount = 0
        for (w in esWords) {
            esCount += sample.split(w).size - 1
        }
        var enCount = 0
        for (w in enWords) {
            enCount += sample.split(w).size - 1
        }
        if (enCount > esCount && enCount >= 3) return "en"
        if (esCount > 0) return "es"
    }

    // 3. Fallback to book title / summary
    val meta = "${book.title} ${book.summary}".lowercase()
    if (meta.contains(" the ") || meta.contains(" and ") || meta.contains(" of ")) return "en"
    return "es"
}

@Composable
fun ReaderScreen(
    book: Book,
    repository: BookRepository,
    onBack: () -> Unit,
    onTabSelected: (TvNavTab) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(repository.getReadingSettings()) }
    var activeProfile by remember { mutableStateOf(repository.getActiveProfile()) }
    var parsedBook by remember { mutableStateOf<ParsedBook?>(null) }
    var spreads by remember { mutableStateOf<List<PageSpread>>(emptyList()) }
    var currentSpreadIndex by remember { mutableIntStateOf(0) }
    var targetSpreadIndex by remember { mutableIntStateOf(0) }
    var isFlipping by remember { mutableStateOf(false) }
    var flipDirectionForward by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }

    // Sleep Timer state
    var sleepTimerSecondsLeft by remember { mutableIntStateOf(0) }
    var isSleepTimerActive by remember { mutableStateOf(settings.sleepTimerMinutes > 0) }
    var screenAlpha by remember { mutableStateOf(1f) }

    // Navigation and HUD visibility
    var showBottomHud by remember { mutableStateOf(false) } // Triggered by DPAD_DOWN
    var showTopBar by remember { mutableStateOf(false) }    // Triggered by DPAD_UP
    var selectedHudCategory by remember { mutableStateOf<ReaderHudCategory?>(null) }
    var showNotesModal by remember { mutableStateOf(false) }
    var showQuoteCardModal by remember { mutableStateOf(false) }

    val curlAnim = remember { Animatable(0f) }
    val readerFocusRequester = remember { FocusRequester() }
    val hudInitialFocusRequester = remember { FocusRequester() }
    val topBarFocusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val ttsController = remember { TtsController(context) }
    val currentSentence by ttsController.currentSentenceIndex.collectAsState()
    val currentSentenceText by ttsController.currentSentenceText.collectAsState()
    val isTtsPlaying by ttsController.isPlaying.collectAsState()
    val isModelDownloading by ttsController.isModelDownloading.collectAsState()
    val downloadProgress by ttsController.downloadProgress.collectAsState()
    val downloadStatusText by ttsController.downloadStatusText.collectAsState()
    val soundManager = remember { SoundManager(context) }
    val ambientManager = remember { AmbientSoundManager(context) }
    var ttsAutoAdvanceJob by remember { mutableStateOf<Job?>(null) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                ttsAutoAdvanceJob?.cancel()
                ttsAutoAdvanceJob = null
                ambientManager.stop()
                ttsController.stop()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (settings.ambientSound != AmbientSound.NONE) {
                    ambientManager.play(settings.ambientSound, settings.ambientVolume)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ttsController.nextPageFirstChunkProvider = null
            ttsController.onPageFinishedListener = null
            ttsAutoAdvanceJob?.cancel()
            ttsAutoAdvanceJob = null
            ttsController.destroy()
            soundManager.release()
            ambientManager.release()
        }
    }

    LaunchedEffect(settings.ambientSound, settings.ambientVolume) {
        if (settings.ambientSound == AmbientSound.NONE) {
            ambientManager.stop()
        } else {
            ambientManager.play(settings.ambientSound, settings.ambientVolume)
        }
    }

    val bookLanguage = remember(book.id, spreads.size) { detectBookLanguage(book, spreads) }

    LaunchedEffect(bookLanguage) {
        val validVoice = TtsVoiceCatalog.ensureValidVoiceCode(settings.ttsVoiceLocale, bookLanguage)
        if (validVoice != settings.ttsVoiceLocale) {
            settings = settings.copy(ttsVoiceLocale = validVoice)
            repository.saveReadingSettings(settings)
        }
        // Pre-cargar modelo de voz en segundo plano (IO) para latencia instantánea al pulsar reproducir
        ttsController.warmUpEngine(bookLanguage)
    }

    LaunchedEffect(settings.ttsEngineMode) {
        ttsController.setEngineMode(settings.ttsEngineMode)
    }

    // Trigger Apple Books realistic 3D paper curl
    suspend fun turnPageSuspend(forward: Boolean, stopTts: Boolean = true): Boolean {
        if (isFlipping) return false
        val nextIdx = if (forward) currentSpreadIndex + 1 else currentSpreadIndex - 1
        if (nextIdx !in spreads.indices) return false
        val wasTtsRunning = isTtsPlaying || ttsController.isPlaying.value || !stopTts || (ttsAutoAdvanceJob?.isActive == true)
        if (stopTts) {
            ttsAutoAdvanceJob?.cancel()
            ttsAutoAdvanceJob = null
            ttsController.stop()
        }
        if (settings.pageSoundEnabled && !wasTtsRunning) {
            soundManager.playPageTurn()
        }

        isFlipping = true
        flipDirectionForward = forward
        targetSpreadIndex = nextIdx

        // Realistic organic duration: Apple Books 500ms vs Fluid 320ms
        val animDuration = if (settings.curlSpeed == CurlSpeed.APPLE_BOOKS_SMOOTH) 500 else 320
        // Organic paper physics easing (starts with natural peel resistance, accelerates through apex, decelerates as page lands)
        val paperEasing = CubicBezierEasing(0.35f, 0.05f, 0.25f, 1.0f)

        curlAnim.snapTo(0f)
        curlAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(animDuration, easing = paperEasing)
        )

        val pct = if (spreads.isNotEmpty()) (((nextIdx + 1) * 100) / spreads.size).coerceIn(1, 100) else 0
        currentSpreadIndex = nextIdx
        repository.saveBookProgress(book.id, nextIdx, pct)
        curlAnim.snapTo(0f)
        isFlipping = false
        return true
    }

    fun turnPage(forward: Boolean) {
        if (!isFlipping) {
            scope.launch {
                turnPageSuspend(forward, stopTts = true)
            }
        }
    }

    val isTtsActive = isTtsPlaying || (ttsAutoAdvanceJob?.isActive == true)

    fun advanceToNextReadableSpread(fromIndex: Int) {
        ttsAutoAdvanceJob?.cancel()
        ttsAutoAdvanceJob = scope.launch {
            var targetIdx = fromIndex
            var foundText = false

            while (targetIdx in spreads.indices && isActive) {
                val turned = turnPageSuspend(forward = true, stopTts = false)
                if (!turned) break

                val nextSpread = spreads.getOrNull(targetIdx)
                val nextL = nextSpread?.leftPage?.paragraphs?.joinToString("\n\n") ?: ""
                val nextR = nextSpread?.rightPage?.paragraphs?.joinToString("\n\n") ?: ""
                val nextText = listOf(nextL, nextR).filter { it.isNotBlank() }.joinToString("\n\n")

                if (nextText.isNotBlank()) {
                    foundText = true
                    val activeVoice = TtsVoiceCatalog.ensureValidVoiceCode(settings.ttsVoiceLocale, bookLanguage)
                    ttsController.readPage(nextText, settings.ttsSpeedRate, settings.ttsPitch, activeVoice)
                    break
                } else {
                    // El pliego actual contiene sólo ilustraciones o está en blanco.
                    // Pausa de 2.5s para que el usuario aprecie la imagen y continúa al siguiente pliego.
                    android.util.Log.i("ReaderScreen", "Pliego $targetIdx es una ilustración sin texto. Mostrando imagen y pasando al siguiente...")
                    kotlinx.coroutines.delay(2500L)
                    targetIdx++
                }
            }

            if (!foundText && targetIdx !in spreads.indices) {
                android.util.Log.i("ReaderScreen", "Fin del libro alcanzado durante la lectura TTS.")
                ttsController.stop()
            }
        }
    }

    var lastTtsToggleTime by remember { mutableLongStateOf(0L) }

    fun toggleTts() {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastTtsToggleTime < 500L) {
            android.util.Log.d("ReaderScreen", "toggleTts: ignorado por debounce (${now - lastTtsToggleTime}ms)")
            return
        }
        lastTtsToggleTime = now

        if (isTtsActive) {
            android.util.Log.i("ReaderScreen", "toggleTts: Pausando lectura TTS...")
            ttsAutoAdvanceJob?.cancel()
            ttsAutoAdvanceJob = null
            ttsController.stop()
        } else {
            val current = spreads.getOrNull(currentSpreadIndex)
            val leftText = current?.leftPage?.paragraphs?.joinToString("\n\n") ?: ""
            val rightText = current?.rightPage?.paragraphs?.joinToString("\n\n") ?: ""
            val pageText = listOf(leftText, rightText).filter { it.isNotBlank() }.joinToString("\n\n")
            if (pageText.isNotBlank()) {
                val activeVoice = TtsVoiceCatalog.ensureValidVoiceCode(settings.ttsVoiceLocale, bookLanguage)
                android.util.Log.i("ReaderScreen", "toggleTts: Iniciando lectura TTS con voz $activeVoice...")
                ttsController.readPage(pageText, settings.ttsSpeedRate, settings.ttsPitch, activeVoice)
            } else {
                android.util.Log.i("ReaderScreen", "toggleTts: Pliego sin texto, avanzando...")
                advanceToNextReadableSpread(currentSpreadIndex + 1)
            }
        }
    }

    LaunchedEffect(Unit) {
        MediaKeyEventBus.playPauseEvents.collect {
            android.util.Log.i("ReaderScreen", "MediaKeyEventBus: Recibida tecla Play/Pause del control remoto")
            toggleTts()
        }
    }

    LaunchedEffect(ttsController, bookLanguage, spreads.size) {
        ttsController.onPageFinishedListener = {
            advanceToNextReadableSpread(currentSpreadIndex + 1)
        }
        ttsController.nextPageFirstChunkProvider = {
            var targetIdx = currentSpreadIndex + 1
            var result: String? = null
            while (targetIdx in spreads.indices) {
                val nextSpread = spreads.getOrNull(targetIdx)
                val nextL = nextSpread?.leftPage?.paragraphs?.joinToString("\n\n") ?: ""
                val nextR = nextSpread?.rightPage?.paragraphs?.joinToString("\n\n") ?: ""
                val nextText = listOf(nextL, nextR).filter { it.isNotBlank() }.joinToString("\n\n")
                if (nextText.isNotBlank()) {
                    val chunks = TtsController.splitIntoSpeechChunks(nextText)
                    result = chunks.firstOrNull()
                    break
                }
                targetIdx++
            }
            result
        }
    }

    LaunchedEffect(isTtsActive) {
        ambientManager.duck(isTtsActive)
    }

    // Load parsed book once
    LaunchedEffect(book.id) {
        isLoading = true
        val raw = repository.loadRawBook(book)
        parsedBook = raw
        val initialSpreads = EpubParser.paginate(raw, settings.fontSizeSp, settings.overscanPercent)
        spreads = initialSpreads
        val savedSpread = repository.getBookProgress(book.id)
        if (savedSpread in initialSpreads.indices) {
            currentSpreadIndex = savedSpread
            targetSpreadIndex = savedSpread
        }
        isLoading = false
        readerFocusRequester.requestFocus()
    }

    // Sleep Timer con atenuación progresiva en los últimos 120 segundos
    LaunchedEffect(isSleepTimerActive, settings.sleepTimerMinutes) {
        if (!isSleepTimerActive || settings.sleepTimerMinutes == 0) {
            sleepTimerSecondsLeft = 0
            screenAlpha = 1f
            return@LaunchedEffect
        }
        sleepTimerSecondsLeft = settings.sleepTimerMinutes * 60
        while (sleepTimerSecondsLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            sleepTimerSecondsLeft--
            // Atenuación progresiva en los últimos 120 segundos
            screenAlpha = if (sleepTimerSecondsLeft <= 120) {
                (sleepTimerSecondsLeft / 120f).coerceIn(0.05f, 1f)
            } else {
                1f
            }
        }
        // Timer expirado: cerrar lector
        screenAlpha = 0f
        kotlinx.coroutines.delay(800L)
        onBack()
    }

    // Dynamic responsive re-pagination whenever font size or overscan changes!
    // Ensures text never cuts off and page count adapts organically to font size.
    LaunchedEffect(settings.fontSizeSp, settings.overscanPercent) {
        parsedBook?.let { raw ->
            val prevTotal = spreads.size.coerceAtLeast(1)
            val currentFraction = currentSpreadIndex.toFloat() / prevTotal.toFloat()
            val newSpreads = EpubParser.paginate(raw, settings.fontSizeSp, settings.overscanPercent)
            spreads = newSpreads
            if (newSpreads.isNotEmpty()) {
                val newIndex = (currentFraction * newSpreads.size).toInt().coerceIn(0, newSpreads.size - 1)
                currentSpreadIndex = newIndex
                targetSpreadIndex = newIndex
                val pct = (((newIndex + 1) * 100) / newSpreads.size).coerceIn(1, 100)
                repository.saveBookProgress(book.id, newIndex, pct)
            }
        }
    }

    val currentSpread = spreads.getOrNull(currentSpreadIndex)
    val nextSpread = spreads.getOrNull(targetSpreadIndex)

    // Palette Colors based on Stitch Reading Themes (Including Pergamino Clásico)
    val (pageBg, pageText, accentColor) = when (settings.theme) {
        ReadingTheme.PERGAMINO -> Triple(Color(0xFFF4F1EA), Color(0xFF2C2A29), Color(0xFFC29B38))
        ReadingTheme.OLED_PURE -> Triple(Color(0xFF000000), Color(0xFFE5E1E4), CyanElectric)
        ReadingTheme.SEPIA_CINE -> Triple(Color(0xFF26201A), Color(0xFFE6DBCC), AmberWarm)
        ReadingTheme.NIGHT_AMBER -> Triple(Color(0xFF0D0D0D), Color(0xFFFFC664), AmberWarm)
        ReadingTheme.PROYECTOR_BLANCO -> Triple(Color(0xFFFFFFFF), Color(0xFF1A1A1A), Color(0xFF0066CC))
        ReadingTheme.CINE_OSCURO -> Triple(Color(0xFF000000), Color(0xFF8B7355), Color(0xFF6B4F2A))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha * settings.readerBrightness }
            .background(BackgroundDark)
            .focusRequester(readerFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    if (showBottomHud) {
                        when (keyEvent.key) {
                            Key.Back, Key.Escape -> {
                                if (selectedHudCategory != null) {
                                    selectedHudCategory = null
                                } else {
                                    showBottomHud = false
                                    readerFocusRequester.requestFocus()
                                }
                                true
                            }
                            Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause, Key.One -> {
                                toggleTts()
                                true
                            }
                            // Allow D-Pad navigation between buttons in the HUD!
                            Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown -> false
                            else -> false
                        }
                    } else if (showTopBar) {
                        when (keyEvent.key) {
                            Key.DirectionDown, Key.Back, Key.Escape -> {
                                showTopBar = false
                                readerFocusRequester.requestFocus()
                                true
                            }
                            // Allow D-Pad navigation between tabs in the TopBar!
                            Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp -> false
                            else -> false
                        }
                    } else {
                        when (keyEvent.key) {
                            Key.DirectionRight, Key.PageDown, Key.MediaFastForward -> {
                                turnPage(forward = true)
                                true
                            }
                            Key.DirectionLeft, Key.PageUp, Key.MediaRewind -> {
                                turnPage(forward = false)
                                true
                            }
                            Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause, Key.One -> {
                                toggleTts()
                                true
                            }
                            // DOWN on remote reveals the Stitch HUD with direct button focus
                            Key.DirectionDown -> {
                                showBottomHud = true
                                showTopBar = false
                                selectedHudCategory = null
                                scope.launch {
                                    kotlinx.coroutines.delay(80L) // Espera una recomposición
                                    try { hudInitialFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                true
                            }
                            // UP on remote reveals the Top Navigation Bar
                            Key.DirectionUp -> {
                                showTopBar = true
                                showBottomHud = false
                                scope.launch {
                                    kotlinx.coroutines.delay(80L)
                                    try { topBarFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                true
                            }
                            Key.Back, Key.Escape -> {
                                ttsAutoAdvanceJob?.cancel()
                                ttsAutoAdvanceJob = null
                                ttsController.stop()
                                val exitPct = if (spreads.isNotEmpty()) (((currentSpreadIndex + 1) * 100) / spreads.size).coerceIn(1, 100) else 0
                                repository.saveBookProgress(book.id, currentSpreadIndex, exitPct)
                                onBack()
                                true
                            }
                            else -> false
                        }
                    }
                } else false
            }
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = CyanElectric)
                    Text("Cargando pliegos 16:9...", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
            }
        } else if (currentSpread != null) {
            // Full Screen 16:9 Two-Page Spread (Overscan margin applied strictly here, keeping HUD intact!)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = (settings.overscanPercent * 18).dp,
                        vertical = (settings.overscanPercent * 10).dp
                    )
                    .graphicsLayer {
                        // Projection hardware calibration (Ceiling / Mirror / Rotation)
                        if (settings.rotation180) {
                            rotationZ = 180f
                        }
                        if (settings.verticalMirror) {
                            scaleY = -1f
                        }
                    }
                    .background(pageBg)
            ) {
                // 1. Base Layer: Underneath spreads
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Page
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val leftContent = if (isFlipping && !flipDirectionForward && nextSpread != null) {
                            nextSpread.leftPage
                        } else {
                            currentSpread.leftPage
                        }
                        PageColumn(
                            content = leftContent,
                            pageNumber = currentSpreadIndex * 2 + 1,
                            fontSizeSp = settings.fontSizeSp,
                            textColor = pageText,
                            accentColor = accentColor,
                            isLeft = true,
                            readingFont = settings.readingFont,
                            activeSentenceText = currentSentenceText
                        )
                    }

                    // Right Page (shows next spread's right page when turning forward)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val rightContent = if (isFlipping && flipDirectionForward && nextSpread != null) {
                            nextSpread.rightPage
                        } else {
                            currentSpread.rightPage
                        }
                        PageColumn(
                            content = rightContent,
                            pageNumber = currentSpreadIndex * 2 + 2,
                            fontSizeSp = settings.fontSizeSp,
                            textColor = pageText,
                            accentColor = accentColor,
                            isLeft = false,
                            readingFont = settings.readingFont,
                            activeSentenceText = currentSentenceText
                        )
                    }
                }

                // 2. Physical 3D Apple-Books Style Turning Page Leaf (Page-Curl)
                if (isFlipping) {
                    val progress = curlAnim.value
                    val cylindricalFoldAlpha = (kotlin.math.sin(progress * Math.PI.toFloat()) * 0.58f).coerceIn(0f, 0.60f)

                    if (flipDirectionForward) {
                        // Turning Forward: Right page curls from right to left (0° to -180°)
                        val angle = -180f * progress
                        val isFront = angle > -90f

                        // Dynamic shadow underneath the curling fold
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

                        // The Organic Turning Leaf
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterEnd)
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0.5f) // Anchored at central spine
                                    rotationY = angle
                                    cameraDistance = 7f * density // 3D perspective distortion (StPageFlip standard)
                                }
                                .background(pageBg)
                        ) {
                            if (isFront) {
                                // Front of turning page: Current Right Page
                                PageColumn(
                                    content = currentSpread.rightPage,
                                    pageNumber = currentSpreadIndex * 2 + 2,
                                    fontSizeSp = settings.fontSizeSp,
                                    textColor = pageText,
                                    accentColor = accentColor,
                                    isLeft = false,
                                    readingFont = settings.readingFont
                                )

                                // Cylindrical curved paper fold shadow on outer edge
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
                                // Back of turning page: Next Left Page (flipped back for readability)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                 ) {
                                    nextSpread?.let { ns ->
                                        PageColumn(
                                            content = ns.leftPage,
                                            pageNumber = targetSpreadIndex * 2 + 1,
                                            fontSizeSp = settings.fontSizeSp,
                                            textColor = pageText,
                                            accentColor = accentColor,
                                            isLeft = true,
                                            readingFont = settings.readingFont
                                        )
                                    }
                                }
                            }
                        }

                        // Cast shadow over left page as the leaf lands
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
                        // Turning Backward: Next Left Page flips from left to right (-180° to 0°)
                        val angle = -180f * (1f - progress)
                        val isFront = angle > -90f

                        // Dynamic shadow under curling fold
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
                                .background(pageBg)
                        ) {
                            if (isFront) {
                                nextSpread?.let { ns ->
                                    PageColumn(
                                        content = ns.rightPage,
                                        pageNumber = targetSpreadIndex * 2 + 2,
                                        fontSizeSp = settings.fontSizeSp,
                                        textColor = pageText,
                                        accentColor = accentColor,
                                        isLeft = false,
                                        readingFont = settings.readingFont
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                ) {
                                    PageColumn(
                                        content = currentSpread.leftPage,
                                        pageNumber = currentSpreadIndex * 2 + 1,
                                        fontSizeSp = settings.fontSizeSp,
                                        textColor = pageText,
                                        accentColor = accentColor,
                                        isLeft = true,
                                        readingFont = settings.readingFont
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Central Spine Crease Shadow (Depth of real physical book based on spineDepth3D)
                val shadowIntensity = (settings.spineDepth3D * 0.7f).coerceIn(0.1f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width((30 + (settings.spineDepth3D * 28)).dp)
                        .align(Alignment.Center)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = shadowIntensity * 0.55f),
                                    Color.Black.copy(alpha = shadowIntensity * 0.20f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 4. Subtle Corner Curl Hint on Bottom-Right (Stitch Specification)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.16f),
                                    accentColor.copy(alpha = 0.14f)
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 36.dp)
                        )
                )
            }
        }

        // ==========================================
        // BARRA SUPERIOR (Requisito E: Aparece al pulsar DPAD_UP)
        // ==========================================
        AnimatedVisibility(
            visible = showTopBar,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LaunchedEffect(showTopBar) {
                if (showTopBar) {
                    topBarFocusRequester.requestFocus()
                }
            }

            TvTopBar(
                currentTab = TvNavTab.LECTOR_3D,
                initialFocusRequester = topBarFocusRequester,
                onTabSelected = { tab ->
                    showTopBar = false
                    repository.saveBookProgress(book.id, currentSpreadIndex)
                    onTabSelected(tab)
                },
                activeProfile = activeProfile,
                onProfileClick = {
                    val profiles = repository.getProfiles()
                    val curIdx = profiles.indexOfFirst { it.id == activeProfile.id }
                    activeProfile = profiles[(curIdx + 1) % profiles.size]
                    repository.saveActiveProfile(activeProfile)
                }
            )
        }

        // ==========================================
        // NUEVO HUD OFICIAL STITCH (Aparece al pulsar DPAD_DOWN)
        // Con foco automático y control directo por D-Pad
        // ==========================================
        AnimatedVisibility(
            visible = showBottomHud,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            val totalSpreads = spreads.size.coerceAtLeast(1)
            val currentProgressPct = ((currentSpreadIndex + 1) * 100) / totalSpreads
            val remainingMin = ((totalSpreads - currentSpreadIndex) * 1.5).toInt().coerceAtLeast(1)

            LaunchedEffect(showBottomHud) {
                if (showBottomHud) {
                    hudInitialFocusRequester.requestFocus()
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // PARTE B: Menú Desplegable Flotante (Por encima del HUD bar, NUNCA lo deforma)
                AnimatedVisibility(
                    visible = selectedHudCategory != null,
                    enter = fadeIn(tween(120)) + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut(tween(100)) + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Spacers alineados con Atrás, Salto Pág y Adelante
                        Spacer(Modifier.width(70.dp))
                        Spacer(Modifier.width(70.dp))
                        Spacer(Modifier.width(70.dp))

                        // Slot 4: Apariencia
                        Box(modifier = Modifier.width(105.dp), contentAlignment = Alignment.BottomCenter) {
                            if (selectedHudCategory == ReaderHudCategory.APARIENCIA) {
                                FloatingHudMenuCard(width = 175.dp) {
                                    val brightnessPercent = (settings.readerBrightness * 100).toInt()
                                    VerticalHudOptionButton(
                                        title = "Brillo: $brightnessPercent%",
                                        icon = Icons.Filled.Brightness4,
                                        isPrimary = settings.readerBrightness < 1.0f,
                                        onClick = {
                                            val nextBrightness = when {
                                                settings.readerBrightness > 0.85f -> 0.70f
                                                settings.readerBrightness > 0.60f -> 0.50f
                                                settings.readerBrightness > 0.40f -> 0.30f
                                                else -> 1.0f
                                            }
                                            settings = settings.copy(readerBrightness = nextBrightness)
                                            repository.saveReadingSettings(settings)
                                        }
                                    )
                                    val marginPct = settings.overscanPercent
                                    VerticalHudOptionButton(
                                        title = "Márgenes: $marginPct%",
                                        icon = Icons.Default.AspectRatio,
                                        isPrimary = settings.overscanPercent > 0,
                                        onClick = {
                                            val nextMargin = when (settings.overscanPercent) {
                                                0 -> 4
                                                4 -> 8
                                                else -> 0
                                            }
                                            settings = settings.copy(overscanPercent = nextMargin)
                                            repository.saveReadingSettings(settings)
                                        }
                                    )
                                    val themeName = when (settings.theme) {
                                        ReadingTheme.PERGAMINO -> "Pergamino"
                                        ReadingTheme.OLED_PURE -> "OLED Puro"
                                        ReadingTheme.SEPIA_CINE -> "Sepia"
                                        ReadingTheme.NIGHT_AMBER -> "Ámbar Noche"
                                        ReadingTheme.PROYECTOR_BLANCO -> "Proyector"
                                        ReadingTheme.CINE_OSCURO -> "Cine Oscuro"
                                    }
                                    VerticalHudOptionButton(
                                        title = "Tema: $themeName",
                                        icon = Icons.Default.Palette,
                                        onClick = {
                                            val nextTheme = when (settings.theme) {
                                                ReadingTheme.PERGAMINO -> ReadingTheme.OLED_PURE
                                                ReadingTheme.OLED_PURE -> ReadingTheme.SEPIA_CINE
                                                ReadingTheme.SEPIA_CINE -> ReadingTheme.NIGHT_AMBER
                                                ReadingTheme.NIGHT_AMBER -> ReadingTheme.PROYECTOR_BLANCO
                                                ReadingTheme.PROYECTOR_BLANCO -> ReadingTheme.CINE_OSCURO
                                                ReadingTheme.CINE_OSCURO -> ReadingTheme.PERGAMINO
                                            }
                                            settings = settings.copy(theme = nextTheme)
                                            repository.saveReadingSettings(settings)
                                        }
                                    )
                                    VerticalHudOptionButton(
                                        title = "Fuente: ${settings.fontSizeSp}sp",
                                        icon = Icons.Default.FormatSize,
                                        onClick = {
                                            val nextSize = when (settings.fontSizeSp) {
                                                16 -> 18
                                                18 -> 20
                                                20 -> 22
                                                22 -> 24
                                                else -> 16
                                            }
                                            settings = settings.copy(fontSizeSp = nextSize)
                                            repository.saveReadingSettings(settings)
                                        }
                                    )
                                }
                            }
                        }

                        // Slot 5: Voz TTS
                        Box(modifier = Modifier.width(105.dp), contentAlignment = Alignment.BottomCenter) {
                            if (selectedHudCategory == ReaderHudCategory.VOZ_TTS) {
                                FloatingHudMenuCard(width = 210.dp) {
                                    val pitchName = when (settings.ttsPitch) {
                                        0.8f -> "Grave"
                                        1.2f -> "Agudo"
                                        else -> "Normal"
                                    }
                                    VerticalHudOptionButton(
                                        title = "Tono: $pitchName",
                                        icon = Icons.Filled.MusicNote,
                                        onClick = {
                                            val nextPitch = when (settings.ttsPitch) {
                                                1.0f -> 1.2f
                                                1.2f -> 0.8f
                                                else -> 1.0f
                                            }
                                            settings = settings.copy(ttsPitch = nextPitch)
                                            repository.saveReadingSettings(settings)
                                        }
                                    )
                                    VerticalHudOptionButton(
                                        title = "Velocidad: ${settings.ttsSpeedRate}x",
                                        icon = Icons.Default.Timer,
                                        isPrimary = settings.ttsSpeedRate != 1.0f,
                                        onClick = {
                                            val nextSpeed = when (settings.ttsSpeedRate) {
                                                1.0f -> 1.25f
                                                1.25f -> 1.5f
                                                1.5f -> 0.75f
                                                else -> 1.0f
                                            }
                                            settings = settings.copy(ttsSpeedRate = nextSpeed)
                                            repository.saveReadingSettings(settings)
                                        }
                                    )
                                    val currentVoiceName = TtsVoiceCatalog.getVoiceDisplayName(settings.ttsVoiceLocale, bookLanguage)
                                    VerticalHudOptionButton(
                                        title = "Voz: $currentVoiceName",
                                        icon = Icons.Filled.RecordVoiceOver,
                                        isPrimary = true,
                                        onClick = {
                                            val nextVoice = TtsVoiceCatalog.getNextVoice(settings.ttsVoiceLocale, bookLanguage)
                                            settings = settings.copy(ttsVoiceLocale = nextVoice.code)
                                            repository.saveReadingSettings(settings)

                                            android.widget.Toast.makeText(context, "Voz: ${nextVoice.displayName}", android.widget.Toast.LENGTH_SHORT).show()

                                            if (isTtsActive) {
                                                ttsController.stop()
                                                val current = spreads.getOrNull(currentSpreadIndex)
                                                val leftText = current?.leftPage?.paragraphs?.joinToString("\n\n") ?: ""
                                                val rightText = current?.rightPage?.paragraphs?.joinToString("\n\n") ?: ""
                                                val pageText = listOf(leftText, rightText).filter { it.isNotBlank() }.joinToString("\n\n")
                                                if (pageText.isNotBlank()) {
                                                    ttsController.readPage(pageText, settings.ttsSpeedRate, settings.ttsPitch, nextVoice.code)
                                                }
                                            }
                                        }
                                    )
                                    VerticalHudOptionButton(
                                        title = if (isTtsActive) "Pausar" else "Leer en Voz",
                                        icon = if (isTtsActive) Icons.Filled.Pause else Icons.Filled.RecordVoiceOver,
                                        isPrimary = isTtsActive,
                                        onClick = {
                                            toggleTts()
                                        }
                                    )
                                }
                            }
                        }

                        // Slot 6: Ambiente
                        Box(modifier = Modifier.width(105.dp), contentAlignment = Alignment.BottomCenter) {
                            if (selectedHudCategory == ReaderHudCategory.AMBIENTE) {
                                FloatingHudMenuCard(width = 175.dp) {
                                    VerticalHudOptionButton(
                                        title = "Paso Pág.: ${if (settings.pageSoundEnabled) "Activado" else "Silenciado"}",
                                        icon = Icons.Default.Flip,
                                        isPrimary = settings.pageSoundEnabled,
                                        onClick = {
                                            settings = settings.copy(pageSoundEnabled = !settings.pageSoundEnabled)
                                            repository.saveReadingSettings(settings)
                                        }
                                    )
                                    VerticalHudOptionButton(
                                        title = "Volumen: ${(settings.ambientVolume * 100).toInt()}%",
                                        icon = Icons.Filled.MusicNote,
                                        onClick = {
                                            val nextVol = when {
                                                settings.ambientVolume < 0.35f -> 0.50f
                                                settings.ambientVolume < 0.65f -> 0.80f
                                                settings.ambientVolume < 0.95f -> 1.0f
                                                else -> 0.20f
                                            }
                                            settings = settings.copy(ambientVolume = nextVol)
                                            repository.saveReadingSettings(settings)
                                        }
                                    )
                                    val ambientLabel = when (settings.ambientSound) {
                                        AmbientSound.NONE -> "Silencio"
                                        AmbientSound.RAIN -> "Lluvia"
                                        AmbientSound.FIREPLACE -> "Chimenea"
                                        AmbientSound.OCEAN -> "Mar"
                                        AmbientSound.CAFE -> "Café"
                                        AmbientSound.FOREST -> "Bosque"
                                        AmbientSound.LOFI -> "Lo-Fi"
                                    }
                                    VerticalHudOptionButton(
                                        title = "Sonido: $ambientLabel",
                                        icon = Icons.Filled.MusicNote,
                                        isPrimary = settings.ambientSound != AmbientSound.NONE,
                                        onClick = {
                                            val nextSound = when (settings.ambientSound) {
                                                AmbientSound.NONE -> AmbientSound.RAIN
                                                AmbientSound.RAIN -> AmbientSound.FIREPLACE
                                                AmbientSound.FIREPLACE -> AmbientSound.OCEAN
                                                AmbientSound.OCEAN -> AmbientSound.CAFE
                                                AmbientSound.CAFE -> AmbientSound.FOREST
                                                AmbientSound.FOREST -> AmbientSound.LOFI
                                                AmbientSound.LOFI -> AmbientSound.NONE
                                            }
                                            settings = settings.copy(ambientSound = nextSound)
                                            repository.saveReadingSettings(settings)
                                        }
                                    )
                                }
                            }
                        }

                        // Slot 7: Herramientas
                        Box(modifier = Modifier.width(105.dp), contentAlignment = Alignment.BottomCenter) {
                            if (selectedHudCategory == ReaderHudCategory.HERRAMIENTAS) {
                                FloatingHudMenuCard(width = 185.dp) {
                                    VerticalHudOptionButton(
                                        title = "Notas",
                                        icon = Icons.Default.Edit,
                                        onClick = {
                                            selectedHudCategory = null
                                            showBottomHud = false
                                            showNotesModal = true
                                        }
                                    )
                                    VerticalHudOptionButton(
                                        title = "Compartir Frase",
                                        icon = Icons.Default.Share,
                                        onClick = {
                                            selectedHudCategory = null
                                            showBottomHud = false
                                            showQuoteCardModal = true
                                        }
                                    )
                                    val timerLabel = when {
                                        !isSleepTimerActive -> "Off"
                                        sleepTimerSecondsLeft > 60 -> "${sleepTimerSecondsLeft / 60}m"
                                        else -> "${sleepTimerSecondsLeft}s"
                                    }
                                    VerticalHudOptionButton(
                                        title = "Sleep: $timerLabel",
                                        icon = Icons.Filled.Timer,
                                        isPrimary = isSleepTimerActive,
                                        onClick = {
                                            val nextMinutes = when (settings.sleepTimerMinutes) {
                                                0 -> 15
                                                15 -> 30
                                                30 -> 45
                                                45 -> 60
                                                else -> 0
                                            }
                                            settings = settings.copy(sleepTimerMinutes = nextMinutes)
                                            repository.saveReadingSettings(settings)
                                            isSleepTimerActive = nextMinutes > 0
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // PARTE A: Barra Inferior Fija (HUD Compacto, NUNCA se deforma)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF131315).copy(alpha = 0.96f))
                        .border(1.5.dp, CyanElectric.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Top Row: Chapter Info + Reading Telemetry
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = AmberWarm,
                                    modifier = Modifier.size(16.dp)
                                )
                                val leftPageNum = currentSpreadIndex * 2 + 1
                                val rightPageNum = currentSpreadIndex * 2 + 2
                                val chapterName = currentSpread?.leftPage?.chapterTitle ?: book.title
                                Text(
                                    text = "$chapterName • Págs. $leftPageNum-$rightPageNum",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "$currentProgressPct% • $remainingMin min restantes",
                                color = CyanElectric,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color(0xFF26262A), RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(currentProgressPct / 100f)
                                    .height(4.dp)
                                    .background(CyanElectric, RoundedCornerShape(2.dp))
                            )
                        }

                        // Row A: 7 opciones (Atrás, Salto Pág, Adelante, Apariencia, Voz TTS, Ambiente, Herramientas)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Atrás (1 solo click: pasa página atrás)
                            HudActionButton(
                                title = "‹ Atrás",
                                icon = Icons.Default.ChevronLeft,
                                isPrimary = false,
                                onClick = {
                                    selectedHudCategory = null
                                    turnPage(forward = false)
                                },
                                onFocus = { selectedHudCategory = null }
                            )

                            // 2. Salto Pág (1 solo click: salta 5 páginas)
                            HudActionButton(
                                title = "Salto Pág",
                                icon = Icons.Default.FastForward,
                                isPrimary = false,
                                onClick = {
                                    selectedHudCategory = null
                                    val jumpIdx = (currentSpreadIndex + 5).coerceAtMost(spreads.size - 1)
                                    currentSpreadIndex = jumpIdx
                                    repository.saveBookProgress(book.id, jumpIdx)
                                },
                                onFocus = { selectedHudCategory = null }
                            )

                            // 3. Adelante (1 solo click: pasa página adelante)
                            HudActionButton(
                                title = "Adelante ›",
                                icon = Icons.Default.ChevronRight,
                                isPrimary = false,
                                onClick = {
                                    selectedHudCategory = null
                                    turnPage(forward = true)
                                },
                                onFocus = { selectedHudCategory = null }
                            )

                            // 4. Grupo Apariencia
                            HudTabButton(
                                title = ReaderHudCategory.APARIENCIA.title,
                                icon = ReaderHudCategory.APARIENCIA.icon,
                                isSelected = selectedHudCategory == ReaderHudCategory.APARIENCIA,
                                modifier = Modifier.focusRequester(hudInitialFocusRequester),
                                onFocus = {
                                    if (selectedHudCategory != null) {
                                        selectedHudCategory = ReaderHudCategory.APARIENCIA
                                    }
                                },
                                onClick = {
                                    selectedHudCategory = if (selectedHudCategory == ReaderHudCategory.APARIENCIA) null else ReaderHudCategory.APARIENCIA
                                }
                            )

                            // 5. Grupo Voz TTS
                            HudTabButton(
                                title = if (isTtsActive) "Pausar Voz" else ReaderHudCategory.VOZ_TTS.title,
                                icon = if (isTtsActive) Icons.Filled.Pause else ReaderHudCategory.VOZ_TTS.icon,
                                isSelected = selectedHudCategory == ReaderHudCategory.VOZ_TTS,
                                isPrimary = isTtsActive,
                                onFocus = {
                                    if (selectedHudCategory != null) {
                                        selectedHudCategory = ReaderHudCategory.VOZ_TTS
                                    }
                                },
                                onClick = {
                                    if (isTtsActive) {
                                        toggleTts()
                                    } else {
                                        selectedHudCategory = ReaderHudCategory.VOZ_TTS
                                        toggleTts()
                                    }
                                }
                            )

                            // 6. Grupo Ambiente
                            HudTabButton(
                                title = ReaderHudCategory.AMBIENTE.title,
                                icon = ReaderHudCategory.AMBIENTE.icon,
                                isSelected = selectedHudCategory == ReaderHudCategory.AMBIENTE,
                                onFocus = {
                                    if (selectedHudCategory != null) {
                                        selectedHudCategory = ReaderHudCategory.AMBIENTE
                                    }
                                },
                                onClick = {
                                    selectedHudCategory = if (selectedHudCategory == ReaderHudCategory.AMBIENTE) null else ReaderHudCategory.AMBIENTE
                                }
                            )

                            // 7. Grupo Herramientas
                            HudTabButton(
                                title = ReaderHudCategory.HERRAMIENTAS.title,
                                icon = ReaderHudCategory.HERRAMIENTAS.icon,
                                isSelected = selectedHudCategory == ReaderHudCategory.HERRAMIENTAS,
                                onFocus = {
                                    if (selectedHudCategory != null) {
                                        selectedHudCategory = ReaderHudCategory.HERRAMIENTAS
                                    }
                                },
                                onClick = {
                                    selectedHudCategory = if (selectedHudCategory == ReaderHudCategory.HERRAMIENTAS) null else ReaderHudCategory.HERRAMIENTAS
                                }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // MODALES INTEGRADOS EN EL LECTOR 3D
        // ==========================================
        if (showNotesModal) {
            NotesModal(
                book = book,
                repository = repository,
                isDarkTheme = true,
                onDismiss = {
                    showNotesModal = false
                    try { readerFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            )
        }

        if (showQuoteCardModal) {
            val sampleQuote = currentSpread?.leftPage?.paragraphs?.firstOrNull { it.length > 30 }
                ?: currentSpread?.rightPage?.paragraphs?.firstOrNull { it.length > 30 }
                ?: book.summary?.takeIf { it.isNotBlank() }
                ?: book.title
            QuoteCardModal(
                book = book,
                repository = repository,
                selectedQuote = sampleQuote,
                isDarkTheme = true,
                onDismiss = {
                    showQuoteCardModal = false
                    try { readerFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            )
        }

        if (isModelDownloading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 50.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161619).copy(alpha = 0.95f)),
                    border = BorderStroke(1.2.dp, AmberWarm.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .width(420.dp)
                        .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = AmberWarm)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.size(24.dp),
                                color = AmberWarm,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Descargando voz neuronal ($downloadProgress%)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AmberWarm,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                        Text(
                            text = downloadStatusText.ifBlank { "Descargando modelo de voz..." },
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "La lectura comenzará de forma automática al completarse.",
                            color = AmberWarm.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun buildHighlightedParagraph(
    fullText: String,
    activeText: String,
    textColor: Color,
    accentColor: Color
): AnnotatedString {
    val cleanActive = activeText.trim()
    if (cleanActive.isBlank() || !fullText.contains(cleanActive, ignoreCase = true)) {
        return AnnotatedString(fullText)
    }
    val startIdx = fullText.indexOf(cleanActive, ignoreCase = true)
    val endIdx = startIdx + cleanActive.length
    return buildAnnotatedString {
        append(fullText.substring(0, startIdx))
        withStyle(
            SpanStyle(
                background = accentColor.copy(alpha = 0.28f),
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append(fullText.substring(startIdx, endIdx))
        }
        append(fullText.substring(endIdx))
    }
}

@Composable
private fun PageColumn(
    content: PageContent,
    pageNumber: Int,
    fontSizeSp: Int,
    textColor: Color,
    accentColor: Color,
    isLeft: Boolean,
    readingFont: ReadingFont = ReadingFont.SERIF_SYSTEM,
    activeSentenceText: String = ""
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 42.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Running Head (Header)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = content.chapterTitle.uppercase(),
                color = textColor.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = content.bookTitle,
                color = textColor.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Body Content (Formatted with safe layout to prevent overflow clipping)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            content.items.forEachIndexed { index, item ->
                when (item) {
                    is PageItem.Paragraph -> {
                        val isChapterStart = index == 0 && content.pageNumber % 2 == 1 && item.text.length > 20 && !item.isHeader
                        if (isChapterStart) {
                            val dropLetter = item.text.take(1)
                            val remainingPara = item.text.drop(1)
                            val annotatedRemaining = buildHighlightedParagraph(remainingPara, activeSentenceText, textColor, accentColor)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = dropLetter,
                                    fontSize = (fontSizeSp * 2.3).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor,
                                    fontFamily = FontProvider.getFontFamily(readingFont),
                                    lineHeight = (fontSizeSp * 2.3).sp,
                                    modifier = Modifier.padding(end = 10.dp, top = 2.dp)
                                )
                                Text(
                                    text = annotatedRemaining,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp * 1.55).sp,
                                    color = textColor,
                                    fontFamily = FontProvider.getFontFamily(readingFont),
                                    textAlign = TextAlign.Justify,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else if (item.isHeader) {
                            val annotatedHeader = buildHighlightedParagraph(item.text, activeSentenceText, accentColor, accentColor)
                            Text(
                                text = annotatedHeader,
                                fontSize = (fontSizeSp * 1.25).sp,
                                lineHeight = (fontSizeSp * 1.6).sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = FontProvider.getFontFamily(readingFont),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            val annotatedPara = buildHighlightedParagraph(item.text, activeSentenceText, textColor, accentColor)
                            Text(
                                text = annotatedPara,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.55).sp,
                                color = textColor,
                                fontFamily = FontProvider.getFontFamily(readingFont),
                                textAlign = TextAlign.Justify
                            )
                        }
                        if (index < content.items.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    is PageItem.Image -> {
                        val imageBitmap = rememberLocalImage(item.imageFile)
                        if (imageBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = item.altText ?: "Ilustración",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .heightIn(min = 60.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudActionButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
    onFocus: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val debouncedClick = {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastClickTime > 350L) {
            lastClickTime = now
            onClick()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .scale(if (isFocused) 1.06f else 1.0f)
            .shadow(if (isFocused) 8.dp else 0.dp, RoundedCornerShape(8.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> AmberWarm
                    isPrimary -> AmberWarm.copy(alpha = 0.25f)
                    else -> Color.White.copy(alpha = 0.08f)
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isPrimary) 1.dp else 0.dp,
                color = if (isFocused) Color.White else if (isPrimary) AmberWarm else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    debouncedClick()
                    true
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { debouncedClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color(0xFF131315) else if (isPrimary) AmberWarm else TextPrimary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = title,
            color = if (isFocused) Color(0xFF131315) else if (isPrimary) AmberWarm else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun HudTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val debouncedClick = {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastClickTime > 350L) {
            lastClickTime = now
            onClick()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .scale(if (isFocused) 1.06f else 1.0f)
            .shadow(if (isFocused) 8.dp else 0.dp, RoundedCornerShape(8.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> AmberWarm
                    isPrimary -> AmberWarm.copy(alpha = 0.45f)
                    isSelected -> AmberWarm.copy(alpha = 0.35f)
                    else -> Color.White.copy(alpha = 0.08f)
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isPrimary || isSelected) 1.5.dp else 0.dp,
                color = if (isFocused) Color.White else if (isPrimary) CyanElectric else if (isSelected) AmberWarm else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                        debouncedClick()
                        true
                    } else if (event.key == Key.DirectionUp && !isSelected) {
                        debouncedClick()
                        true
                    } else false
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { debouncedClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color(0xFF131315) else if (isPrimary) CyanElectric else if (isSelected) AmberWarm else TextMuted,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = title,
            color = if (isFocused) Color(0xFF131315) else if (isPrimary) CyanElectric else if (isSelected) Color.White else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (isFocused || isPrimary || isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun FloatingHudMenuCard(
    width: Dp = 180.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .wrapContentWidth(unbounded = true, align = Alignment.CenterHorizontally)
            .width(width)
            .shadow(16.dp, RoundedCornerShape(14.dp), spotColor = CyanElectric.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161619).copy(alpha = 0.98f))
            .border(1.2.dp, CyanElectric.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun VerticalHudOptionButton(
    title: String,
    icon: ImageVector,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val debouncedClick = {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastClickTime > 350L) {
            lastClickTime = now
            onClick()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.04f else 1.0f)
            .shadow(if (isFocused) 6.dp else 0.dp, RoundedCornerShape(8.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> AmberWarm
                    isPrimary -> AmberWarm.copy(alpha = 0.20f)
                    else -> Color.White.copy(alpha = 0.08f)
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isPrimary) 1.dp else 0.dp,
                color = if (isFocused) Color.White else if (isPrimary) AmberWarm else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    debouncedClick()
                    true
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { debouncedClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color(0xFF131315) else if (isPrimary) AmberWarm else TextPrimary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = title,
            color = if (isFocused) Color(0xFF131315) else if (isPrimary) AmberWarm else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (isFocused || isPrimary) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
