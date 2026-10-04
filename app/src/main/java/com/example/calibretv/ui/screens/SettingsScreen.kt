package com.example.calibretv.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.calibretv.data.update.UpdateManager
import com.example.calibretv.ui.components.UserProfilesDialog
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.AmbientSound
import com.example.calibretv.data.model.CurlSpeed
import com.example.calibretv.data.model.ReadingFont
import com.example.calibretv.data.model.ReadingSettings
import com.example.calibretv.data.model.ReadingTheme
import com.example.calibretv.data.storage.PreferencesManager
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CanvasBackgroundLight
import com.example.calibretv.theme.CardBackgroundLight
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.InkMuted
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.InkSecondary
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.components.TvSidebar
import androidx.compose.ui.focus.FocusRequester

@Composable
fun SettingsScreen(
    repository: BookRepository,
    onTabSelected: (TvNavTab) -> Unit,
    onOpenOpds: () -> Unit = {},
    onRunSetupWizard: () -> Unit = {},
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (currentVersionName, currentVersionCode) = remember { UpdateManager.getCurrentVersion(context) }
    var updateCheckStatus by remember { mutableStateOf("") }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableRelease by remember { mutableStateOf<UpdateManager.ReleaseInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadDetails by remember { mutableStateOf("") }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler {
        if (showUpdateDialog && !isDownloadingUpdate) {
            showUpdateDialog = false
        } else {
            onSaved()
        }
    }

    var settings by remember { mutableStateOf(repository.getReadingSettings()) }
    var activeProfile by remember { mutableStateOf(repository.getActiveProfile()) }
    var showUserProfilesModal by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    fun checkUpdate() {
        scope.launch {
            isCheckingUpdate = true
            updateCheckStatus = "Buscando en GitHub..."
            when (val res = UpdateManager.checkForUpdate(context)) {
                is UpdateManager.CheckResult.UpdateAvailable -> {
                    availableRelease = res.release
                    updateCheckStatus = "¡Nueva versión ${res.release.tagName} disponible!"
                    showUpdateDialog = true
                }
                is UpdateManager.CheckResult.UpToDate -> {
                    availableRelease = null
                    updateCheckStatus = "✓ CalibroTV está actualizado (${res.currentVersion})"
                }
                is UpdateManager.CheckResult.Error -> {
                    availableRelease = null
                    updateCheckStatus = "⚠️ ${res.message}"
                }
            }
            isCheckingUpdate = false
        }
    }

    fun startDownloadAndInstall(release: UpdateManager.ReleaseInfo) {
        scope.launch {
            isDownloadingUpdate = true
            downloadProgress = 0
            downloadDetails = "Conectando con el servidor..."
            downloadError = null
            val result = UpdateManager.downloadApk(context, release) { pct, cur, tot ->
                downloadProgress = pct
                val curMb = String.format(java.util.Locale.US, "%.1f", cur / 1048576.0)
                val totMb = String.format(java.util.Locale.US, "%.1f", tot / 1048576.0)
                downloadDetails = "$pct% ($curMb MB / $totMb MB)"
            }
            isDownloadingUpdate = false
            when (result) {
                is UpdateManager.DownloadResult.Success -> {
                    showUpdateDialog = false
                    downloadError = null
                    UpdateManager.installApk(context, result.apkFile)
                }
                is UpdateManager.DownloadResult.Error -> {
                    downloadError = result.message
                    updateCheckStatus = "⚠️ ${result.message}"
                }
            }
        }
    }

    val prefs = remember { PreferencesManager(context) }
    var isDarkTheme by remember { mutableStateOf(prefs.isDarkTheme()) }
    val sidebarFocusRequester = remember { FocusRequester() }
    val screenBg = if (isDarkTheme) BackgroundDark else CanvasBackgroundLight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 68.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 36.dp, vertical = 16.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyanElectric,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CONTROL REMOTO TV • AJUSTES DIRECTOS",
                            color = CyanElectric,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ajustes",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Profile and TV Info
                var isProfilePillFocused by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .scale(if (isProfilePillFocused) 1.05f else 1.0f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isProfilePillFocused) SurfaceContainerHigh else SurfaceContainer)
                        .border(
                            width = if (isProfilePillFocused) 2.dp else 1.dp,
                            color = if (isProfilePillFocused) CyanElectric else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .onFocusChanged { isProfilePillFocused = it.isFocused }
                        .focusable()
                        .clickable { showUserProfilesModal = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Perfil Activo: ${activeProfile.name}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "HDMI • 1080p @ 60Hz • Presiona para cambiar",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bento Grid: Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Modo de Proyección Card (ETIQUETAS ELIMINADAS - Requisito A)
                CleanBentoCard(
                    modifier = Modifier.weight(0.38f),
                    title = "Modo de Proyección",
                    icon = Icons.Default.Flip
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingToggleButton(
                            modifier = Modifier.weight(1f),
                            title = "Espejo Vertical",
                            icon = Icons.Default.VerticalSplit,
                            isActive = settings.verticalMirror,
                            onToggle = {
                                settings = settings.copy(
                                    verticalMirror = !settings.verticalMirror,
                                    ceilingMode = !settings.verticalMirror || settings.rotation180
                                )
                            }
                        )
                        SettingToggleButton(
                            modifier = Modifier.weight(1f),
                            title = "Rotación 180°",
                            icon = Icons.Default.Sync,
                            isActive = settings.rotation180,
                            onToggle = {
                                settings = settings.copy(
                                    rotation180 = !settings.rotation180,
                                    ceilingMode = settings.verticalMirror || !settings.rotation180
                                )
                            }
                        )
                    }
                }

                // Temas de Color Ópticos (INCLUYE PERGAMINO CLÁSICO APPLE - Requisito B)
                CleanBentoCard(
                    modifier = Modifier.weight(0.62f),
                    title = "Temas de Color Ópticos",
                    icon = Icons.Default.Palette
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionItem(
                            modifier = Modifier.weight(1f),
                            title = "Pergamino",
                            subtitle = "Papel Clásico",
                            bgColor = Color(0xFFF4F1EA),
                            textColor = Color(0xFF2C2A29),
                            isSelected = settings.theme == ReadingTheme.PERGAMINO,
                            onClick = { settings = settings.copy(theme = ReadingTheme.PERGAMINO) }
                        )
                        ThemeOptionItem(
                            modifier = Modifier.weight(1f),
                            title = "OLED Puro",
                            subtitle = "Negro Total",
                            bgColor = Color(0xFF000000),
                            textColor = Color(0xFFE5E1E4),
                            isSelected = settings.theme == ReadingTheme.OLED_PURE,
                            onClick = { settings = settings.copy(theme = ReadingTheme.OLED_PURE) }
                        )
                        ThemeOptionItem(
                            modifier = Modifier.weight(1f),
                            title = "Sepia Cine",
                            subtitle = "Cálido",
                            bgColor = Color(0xFF26201A),
                            textColor = Color(0xFFE6DBCC),
                            isSelected = settings.theme == ReadingTheme.SEPIA_CINE,
                            onClick = { settings = settings.copy(theme = ReadingTheme.SEPIA_CINE) }
                        )
                        ThemeOptionItem(
                            modifier = Modifier.weight(1f),
                            title = "Ámbar Noche",
                            subtitle = "Cero Azul",
                            bgColor = Color(0xFF1C140C),
                            textColor = Color(0xFFFFC664),
                            isSelected = settings.theme == ReadingTheme.NIGHT_AMBER,
                            onClick = { settings = settings.copy(theme = ReadingTheme.NIGHT_AMBER) }
                        )
                        ThemeOptionItem(
                            modifier = Modifier.weight(1f),
                            title = "Proyector",
                            subtitle = "Blanco",
                            bgColor = Color(0xFFFFFFFF),
                            textColor = Color(0xFF1A1A1A),
                            isSelected = settings.theme == ReadingTheme.PROYECTOR_BLANCO,
                            onClick = { settings = settings.copy(theme = ReadingTheme.PROYECTOR_BLANCO) }
                        )
                        ThemeOptionItem(
                            modifier = Modifier.weight(1f),
                            title = "Cine Oscuro",
                            subtitle = "Ámbar Tenue",
                            bgColor = Color(0xFF000000),
                            textColor = Color(0xFF8B7355),
                            isSelected = settings.theme == ReadingTheme.CINE_OSCURO,
                            onClick = { settings = settings.copy(theme = ReadingTheme.CINE_OSCURO) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bento Grid: Row 2 (ETIQUETAS ELIMINADAS - Requisito A)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Márgenes de Pantalla
                CleanBentoCard(
                    modifier = Modifier.weight(0.33f),
                    title = "Márgenes de Pantalla",
                    icon = Icons.Default.DisplaySettings
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "Estándar (0%)",
                            isSelected = settings.overscanPercent == 0,
                            onClick = { settings = settings.copy(overscanPercent = 0) }
                        )
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "Medio (4%)",
                            isSelected = settings.overscanPercent == 4,
                            onClick = { settings = settings.copy(overscanPercent = 4) }
                        )
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "Amplio (8%)",
                            isSelected = settings.overscanPercent == 8,
                            onClick = { settings = settings.copy(overscanPercent = 8) }
                        )
                    }
                }

                // Tamaño de Fuente
                CleanBentoCard(
                    modifier = Modifier.weight(0.33f),
                    title = "Tamaño de Fuente",
                    icon = Icons.Default.FormatSize
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "18px",
                            isSelected = settings.fontSizeSp == 18,
                            onClick = { settings = settings.copy(fontSizeSp = 18) }
                        )
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "20px",
                            isSelected = settings.fontSizeSp == 20,
                            onClick = { settings = settings.copy(fontSizeSp = 20) }
                        )
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "24px",
                            isSelected = settings.fontSizeSp == 24,
                            onClick = { settings = settings.copy(fontSizeSp = 24) }
                        )
                    }
                }

                // Animación 3D (APPLE BOOKS 500ms vs FLUIDO - Requisito C)
                CleanBentoCard(
                    modifier = Modifier.weight(0.34f),
                    title = "Animación 3D de Hoja",
                    icon = Icons.Default.ViewCarousel
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "Apple Books (Suave)",
                            isSelected = settings.curlSpeed == CurlSpeed.APPLE_BOOKS_SMOOTH,
                            onClick = { settings = settings.copy(curlSpeed = CurlSpeed.APPLE_BOOKS_SMOOTH) }
                        )
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "Fluido (320ms)",
                            isSelected = settings.curlSpeed == CurlSpeed.FLUID,
                            onClick = { settings = settings.copy(curlSpeed = CurlSpeed.FLUID) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tipografía y Profundidad 3D Card (Fase 6)
            CleanBentoCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Tipografía y Profundidad 3D",
                icon = Icons.Default.FormatSize
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Fuente
                    Column(modifier = Modifier.weight(0.6f)) {
                        Text("Catálogo de Fuentes", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = "Serif Sistema",
                                isSelected = settings.readingFont == ReadingFont.SERIF_SYSTEM,
                                onClick = { settings = settings.copy(readingFont = ReadingFont.SERIF_SYSTEM) }
                            )
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = "OpenDyslexic",
                                isSelected = settings.readingFont == ReadingFont.OPEN_DYSLEXIC,
                                onClick = { settings = settings.copy(readingFont = ReadingFont.OPEN_DYSLEXIC) }
                            )
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = "Monospace",
                                isSelected = settings.readingFont == ReadingFont.MONOSPACE,
                                onClick = { settings = settings.copy(readingFont = ReadingFont.MONOSPACE) }
                            )
                        }
                    }

                    // Profundidad 3D del Lomo
                    Column(modifier = Modifier.weight(0.4f)) {
                        val depthPct = (settings.spineDepth3D * 100).toInt()
                        Text("Profundidad 3D Lomo: $depthPct%", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = "0% Plano",
                                isSelected = settings.spineDepth3D < 0.2f,
                                onClick = { settings = settings.copy(spineDepth3D = 0.0f) }
                            )
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = "50% Estándar",
                                isSelected = settings.spineDepth3D in 0.2f..0.7f,
                                onClick = { settings = settings.copy(spineDepth3D = 0.5f) }
                            )
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = "100% Grueso",
                                isSelected = settings.spineDepth3D > 0.7f,
                                onClick = { settings = settings.copy(spineDepth3D = 1.0f) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sonido de Paso de Página (Foley)
            CleanBentoCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Efectos de Sonido (Paso de Página)",
                icon = Icons.Filled.VolumeUp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sonido Foley de papel al pasar la hoja",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Reproduce un crujido orgánico aleatorizado que simula el roce físico del papel.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Row(
                        modifier = Modifier.width(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "🔊 Activado",
                            isSelected = settings.pageSoundEnabled,
                            onClick = {
                                settings = settings.copy(pageSoundEnabled = true)
                                repository.saveReadingSettings(settings)
                            }
                        )
                        SegmentedOption(
                            modifier = Modifier.weight(1f),
                            title = "🔇 Desactivado",
                            isSelected = !settings.pageSoundEnabled,
                            onClick = {
                                settings = settings.copy(pageSoundEnabled = false)
                                repository.saveReadingSettings(settings)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sleep Timer
            CleanBentoCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Temporizador de Apagado (Sleep Timer)",
                icon = Icons.Filled.Timer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apaga automáticamente el lector tras el tiempo seleccionado",
                            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "La pantalla se atenúa progresivamente en los últimos 2 minutos antes de cerrar.",
                            color = TextMuted, fontSize = 11.sp
                        )
                    }
                    Row(
                        modifier = Modifier.width(320.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Apagado", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1 hora").forEach { (min, label) ->
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = label,
                                isSelected = settings.sleepTimerMinutes == min,
                                onClick = {
                                    settings = settings.copy(sleepTimerMinutes = min)
                                    repository.saveReadingSettings(settings)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lectura en Voz Alta (TTS)
            CleanBentoCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Lectura en Voz Alta (TTS)",
                icon = Icons.Filled.RecordVoiceOver
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lee el libro en voz alta con resaltado de oración activa",
                            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Activa desde el HUD del lector (botón ▶ Leer). Ajusta aquí la velocidad.",
                            color = TextMuted, fontSize = 11.sp
                        )
                    }
                    Row(
                        modifier = Modifier.width(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.75f to "Lento", 1.0f to "Normal", 1.25f to "Rápido", 1.5f to "Veloz").forEach { (speed, label) ->
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = label,
                                isSelected = settings.ttsSpeedRate == speed,
                                onClick = {
                                    settings = settings.copy(ttsSpeedRate = speed)
                                    repository.saveReadingSettings(settings)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brillo del Lector y Modo Cine
            CleanBentoCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Brillo del Lector y Modo Cine",
                icon = Icons.Filled.Brightness4
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Control de brillo interno independiente del brillo del TV",
                            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "También ajustable desde el HUD del lector. Combinar con tema 'Cine Oscuro' para sala oscura.",
                            color = TextMuted, fontSize = 11.sp
                        )
                    }
                    Row(
                        modifier = Modifier.width(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1.0f to "100%", 0.70f to "70%", 0.50f to "50%", 0.30f to "30%").forEach { (brightness, label) ->
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = label,
                                isSelected = settings.readerBrightness == brightness,
                                onClick = {
                                    settings = settings.copy(readerBrightness = brightness)
                                    repository.saveReadingSettings(settings)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sonido Ambiental
            CleanBentoCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Sonido Ambiental de Lectura",
                icon = Icons.Filled.MusicNote
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reproduce ambiente sonoro continuo durante la lectura",
                                color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Diseñado para TV en sala. Cicla entre sonidos desde el HUD del lector.",
                                color = TextMuted, fontSize = 11.sp
                            )
                        }
                    }
                    // Selector de tipo de sonido
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            AmbientSound.NONE to "🔕 Ninguno",
                            AmbientSound.RAIN to "🌧 Lluvia",
                            AmbientSound.FIREPLACE to "🔥 Chimenea",
                            AmbientSound.OCEAN to "🌊 Mar",
                            AmbientSound.CAFE to "☕ Café",
                            AmbientSound.FOREST to "🌲 Bosque"
                        ).forEach { (sound, label) ->
                            SegmentedOption(
                                modifier = Modifier.weight(1f),
                                title = label,
                                isSelected = settings.ambientSound == sound,
                                onClick = {
                                    settings = settings.copy(ambientSound = sound)
                                    repository.saveReadingSettings(settings)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actualizaciones de Software (OTA)
            CleanBentoCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Actualizaciones de Software (OTA)",
                icon = Icons.Filled.SystemUpdate
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Versión instalada: v$currentVersionName (Build $currentVersionCode)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (updateCheckStatus.isNotBlank()) updateCheckStatus else "Verifica directamente desde GitHub si hay nuevas versiones de CalibroTV disponibles.",
                            color = if (availableRelease != null) CyanElectric else TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (availableRelease != null) {
                            TvActionButton(
                                title = "Instalar v${availableRelease?.tagName?.removePrefix("v") ?: ""}",
                                icon = Icons.Filled.CloudDownload,
                                isPrimary = true,
                                onClick = { showUpdateDialog = true }
                            )
                        } else {
                            TvActionButton(
                                title = if (isCheckingUpdate) "Buscando..." else "Buscar Actualización",
                                icon = Icons.Filled.Sync,
                                isPrimary = false,
                                onClick = { checkUpdate() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (saveFeedback.isNotBlank()) saveFeedback else "ⓘ Aplicación instantánea con 1 solo click del control remoto.",
                    color = if (saveFeedback.isNotBlank()) CyanElectric else TextMuted,
                    fontSize = 12.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvActionButton(
                        title = "Asistente Inicial",
                        icon = Icons.Default.Tune,
                        isPrimary = false,
                        onClick = onRunSetupWizard
                    )
                    TvActionButton(
                        title = "Conexión OPDS",
                        icon = Icons.Default.Sync,
                        isPrimary = false,
                        onClick = onOpenOpds
                    )
                    TvActionButton(
                        title = "Restaurar",
                        icon = Icons.Default.RestartAlt,
                        isPrimary = false,
                        onClick = {
                            settings = ReadingSettings()
                            repository.saveReadingSettings(settings)
                            saveFeedback = "Ajustes restaurados a valores de fábrica"
                        }
                    )
                    TvActionButton(
                        title = "Guardar Perfil TV",
                        icon = Icons.Default.Save,
                        isPrimary = true,
                        onClick = {
                            repository.saveReadingSettings(settings)
                            saveFeedback = "✓ Perfil de TV guardado correctamente"
                        }
                    )
                }
            }
        }
    }

        // Rail de Navegación Lateral Flotante (Overlay)
        TvSidebar(
            modifier = Modifier.align(Alignment.CenterStart),
            currentTab = TvNavTab.AJUSTES,
            onTabSelected = onTabSelected,
            isDarkTheme = isDarkTheme,
            onToggleTheme = {
                val newTheme = !isDarkTheme
                isDarkTheme = newTheme
                scope.launch { prefs.setDarkTheme(newTheme) }
            },
            focusRequester = sidebarFocusRequester
        )
    }

    if (showUserProfilesModal) {
        UserProfilesDialog(
            repository = repository,
            activeProfile = activeProfile,
            onProfileChanged = {
                activeProfile = it
                showUserProfilesModal = false
            },
            onDismiss = {
                showUserProfilesModal = false
                activeProfile = repository.getActiveProfile()
            }
        )
    }

    if (showUpdateDialog && availableRelease != null) {
        val rel = availableRelease!!
        Dialog(
            onDismissRequest = {
                if (!isDownloadingUpdate) {
                    showUpdateDialog = false
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !isDownloadingUpdate,
                dismissOnClickOutside = !isDownloadingUpdate
            )
        ) {
            Box(
                modifier = Modifier
                    .width(520.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceContainer)
                    .border(1.5.dp, CyanElectric, RoundedCornerShape(18.dp))
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberWarm.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = AmberWarm,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Nueva versión: ${rel.tagName}",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "CalibroTV OTA Update",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (rel.changelog.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BackgroundDark)
                                .border(1.dp, Color(0xFF26262A), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = rel.changelog,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (downloadError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF331414))
                                .border(1.dp, Color(0xFFE53935), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "⚠️ $downloadError",
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    if (isDownloadingUpdate) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Descargando actualización...",
                                    color = CyanElectric,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$downloadProgress%",
                                    color = AmberWarm,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CyanElectric,
                                trackColor = SurfaceContainerHigh
                            )
                            if (downloadDetails.isNotBlank()) {
                                Text(
                                    text = downloadDetails,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvActionButton(
                                title = "Cancelar",
                                icon = Icons.Filled.Close,
                                isPrimary = false,
                                onClick = { showUpdateDialog = false }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TvActionButton(
                                title = if (downloadError != null) "Reintentar Descarga" else "Descargar e Instalar",
                                icon = Icons.Filled.CloudDownload,
                                isPrimary = true,
                                onClick = { startDownloadAndInstall(rel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanBentoCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .border(1.dp, Color(0xFF26262A), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingToggleButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .shadow(if (isFocused) 10.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) AmberWarm.copy(alpha = 0.25f) else SurfaceContainerHigh)
            .border(
                width = if (isFocused) 2.dp else if (isActive) 1.5.dp else 0.dp,
                color = if (isFocused) CyanElectric else if (isActive) AmberWarm else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) AmberWarm else TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                color = if (isActive) AmberWarm else TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ThemeOptionItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .shadow(if (isFocused) 10.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 2.dp else 1.dp,
                color = if (isFocused) CyanElectric else if (isSelected) CyanElectric else Color(0xFF333333),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(if (isSelected) CyanElectric else Color.Transparent, CircleShape)
                    .border(1.dp, if (isSelected) CyanElectric else Color(0xFF666666), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF131315),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = title,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = textColor.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SegmentedOption(
    modifier: Modifier = Modifier,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .shadow(if (isFocused) 8.dp else 0.dp, RoundedCornerShape(8.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AmberWarm else SurfaceContainerHigh)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) CyanElectric else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color(0xFF131315) else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun TvActionButton(
    title: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .shadow(if (isFocused) 10.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPrimary || isFocused) AmberWarm else SurfaceContainerHigh)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) CyanElectric else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPrimary || isFocused) Color(0xFF131315) else TextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = if (isPrimary || isFocused) Color(0xFF131315) else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
