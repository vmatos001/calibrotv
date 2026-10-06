package com.example.calibretv.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.R
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.ServerConfig
import com.example.calibretv.data.opds.OpdsClient
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceContainerHighest
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SetupWizardStep {
    WELCOME_CHOICE,    // Paso 1: ¿Qué desea configurar? (4 opciones en tarjetas navegables)
    OPDS_FORM,         // Formulario OPDS (solo si el usuario lo seleccionó)
    WIFI_INFO,         // Info de Transferencia WiFi
    GDRIVE_INFO,       // Info de Google Drive
    LANGUAGE,          // Paso 2: ¿Qué idioma prefieres?
    THEME,             // Paso 3: Tema de la app
    PROFILE            // Paso 4: ¿Quién leerá los libros?
}

@Composable
fun SetupWizardScreen(
    repository: BookRepository,
    onSetupFinished: () -> Unit
) {
    var step by remember { mutableStateOf(SetupWizardStep.WELCOME_CHOICE) }
    val scope = rememberCoroutineScope()

    // Configuración de Servidor OPDS
    val currentConfig = remember { repository.getServerConfig() }
    var serverUrl by remember { mutableStateOf(currentConfig.serverUrl.ifBlank { "http://" }) }
    var username by remember { mutableStateOf(currentConfig.username) }
    var password by remember { mutableStateOf(currentConfig.password) }
    var isConnecting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }

    // Idioma & Tema
    var selectedLanguage by remember { mutableStateOf(repository.getAppLanguage()) }
    var isDarkTheme by remember { mutableStateOf(repository.isDarkTheme()) }

    // Perfil
    var profileName by remember { mutableStateOf(if (selectedLanguage == "en") "My Profile" else "Mi Perfil") }
    val presetColors = listOf("#FFA000", "#38BDF8", "#4CAF50", "#AB47BC", "#FF5722", "#E91E63")
    var selectedColor by remember { mutableStateOf("#FFA000") }

    // Foco inicial controlado
    val initialFocus = remember { FocusRequester() }

    LaunchedEffect(step) {
        delay(150)
        try {
            initialFocus.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF13151D),
                        BackgroundDark,
                        Color(0xFF090A0D)
                    )
                )
            )
            .padding(horizontal = 48.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.96f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainer)
                .border(1.5.dp, AmberWarm.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header con progreso de pasos
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = "CalibroTV",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                text = "CALIBROTV — CONFIGURACIÓN INICIAL",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = when (step) {
                                    SetupWizardStep.WELCOME_CHOICE -> "¿Qué desea configurar?"
                                    SetupWizardStep.OPDS_FORM -> "Configuración de Servidor OPDS"
                                    SetupWizardStep.WIFI_INFO -> "Transferencia de Libros vía WiFi"
                                    SetupWizardStep.GDRIVE_INFO -> "Sincronización con Google Drive"
                                    SetupWizardStep.LANGUAGE -> "Idioma de la Aplicación"
                                    SetupWizardStep.THEME -> "Apariencia y Tema Visual"
                                    SetupWizardStep.PROFILE -> "¿Quién leerá los libros?"
                                },
                                fontSize = 13.sp,
                                color = AmberWarm,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Indicadores de progreso (4 etapas)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentStage = when (step) {
                            SetupWizardStep.WELCOME_CHOICE, SetupWizardStep.OPDS_FORM, SetupWizardStep.WIFI_INFO, SetupWizardStep.GDRIVE_INFO -> 1
                            SetupWizardStep.LANGUAGE -> 2
                            SetupWizardStep.THEME -> 3
                            SetupWizardStep.PROFILE -> 4
                        }
                        (1..4).forEach { stage ->
                            val isActive = currentStage == stage
                            val isCompleted = currentStage > stage
                            Box(
                                modifier = Modifier
                                    .size(if (isActive) 14.dp else 10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive -> AmberWarm
                                            isCompleted -> CyanElectric
                                            else -> Color(0xFF383842)
                                        }
                                    )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF282834))
                )
            }

            // Contenido dinámico según el paso
            when (step) {
                // =========================================================================
                // PASO 1: BIENVENIDA Y ELECCIÓN DE FUENTE (SOLO BOTONES GRANDES D-PAD)
                // =========================================================================
                SetupWizardStep.WELCOME_CHOICE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "¿Cómo deseas añadir libros a tu televisor?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Selecciona una opción con el mando a distancia. Podrás cambiarla o añadir más fuentes en cualquier momento desde los Ajustes.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. Servidor OPDS
                            SetupOptionCard(
                                title = "Servidor OPDS",
                                description = "Conectar con Calibre-Web o servidor OPDS",
                                icon = Icons.Default.Dns,
                                iconColor = AmberWarm,
                                focusRequester = initialFocus,
                                modifier = Modifier.weight(1f),
                                onSelect = { step = SetupWizardStep.OPDS_FORM }
                            )

                            // 2. Enviar por WiFi
                            SetupOptionCard(
                                title = "Enviar por WiFi",
                                description = "Subir libros directamente desde el móvil o PC",
                                icon = Icons.Default.Wifi,
                                iconColor = CyanElectric,
                                modifier = Modifier.weight(1f),
                                onSelect = { step = SetupWizardStep.WIFI_INFO }
                            )

                            // 3. Conectar GDrive
                            SetupOptionCard(
                                title = "Conectar GDrive",
                                description = "Sincronizar biblioteca desde Google Drive",
                                icon = Icons.Default.Cloud,
                                iconColor = Color(0xFF4285F4),
                                modifier = Modifier.weight(1f),
                                onSelect = { step = SetupWizardStep.GDRIVE_INFO }
                            )

                            // 4. Omitir
                            SetupOptionCard(
                                title = "Omitir por Ahora",
                                description = "Continuar sin configurar fuente ahora",
                                icon = Icons.Default.SkipNext,
                                iconColor = Color(0xFFAAAAAA),
                                modifier = Modifier.weight(1f),
                                onSelect = { step = SetupWizardStep.LANGUAGE }
                            )
                        }
                    }
                }

                // =========================================================================
                // SUB-PASO: FORMULARIO SERVIDOR OPDS
                // =========================================================================
                SetupWizardStep.OPDS_FORM -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Ingresa los datos de conexión de tu servidor Calibre-Web:",
                            fontSize = 13.sp,
                            color = TextMuted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1.3f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("URL del Servidor OPDS:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = serverUrl,
                                    onValueChange = { serverUrl = it },
                                    placeholder = { Text("http://192.168.1.X:8083/opds", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberWarm,
                                        unfocusedBorderColor = Color(0xFF383842),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Usuario (opcional):", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        OutlinedTextField(
                                            value = username,
                                            onValueChange = { username = it },
                                            placeholder = { Text("admin", color = TextMuted) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AmberWarm,
                                                unfocusedBorderColor = Color(0xFF383842),
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            )
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Contraseña (opcional):", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        OutlinedTextField(
                                            value = password,
                                            onValueChange = { password = it },
                                            placeholder = { Text("••••", color = TextMuted) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AmberWarm,
                                                unfocusedBorderColor = Color(0xFF383842),
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            )
                                        )
                                    }
                                }
                            }

                            // Feedback status card
                            Column(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerHigh)
                                    .border(1.dp, Color(0xFF333340), RoundedCornerShape(14.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("ESTADO DE LA CONEXIÓN", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)

                                if (isConnecting) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AmberWarm, strokeWidth = 2.dp)
                                        Text("Probando conexión...", fontSize = 12.sp, color = TextPrimary)
                                    }
                                } else if (testResult != null) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (testSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (testSuccess) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = testResult!!,
                                            fontSize = 12.sp,
                                            color = if (testSuccess) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                            lineHeight = 16.sp
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Introduce la URL de tu servidor y pulsa 'Probar Conexión' antes de continuar, o avanza si prefieres verificarlo luego.",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // SUB-PASO: INFO WIFI IMPORT
                // =========================================================================
                SetupWizardStep.WIFI_INFO -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CyanElectric.copy(alpha = 0.15f))
                                .border(1.5.dp, CyanElectric, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = CyanElectric, modifier = Modifier.size(36.dp))
                        }
                        Text(
                            text = "Transferencia Directa de Libros por WiFi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "¡Excelente elección! CalibroTV incluye un servidor web integrado. Cuando entres a la aplicación, solo pulsa 'Importar por WiFi' en el menú lateral y escanea el código QR desde tu teléfono o escribe la IP en el navegador de tu ordenador para enviar libros EPUB, PDF o cómics de forma instantánea.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth(0.85f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // =========================================================================
                // SUB-PASO: INFO GDRIVE
                // =========================================================================
                SetupWizardStep.GDRIVE_INFO -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4285F4).copy(alpha = 0.15f))
                                .border(1.5.dp, Color(0xFF4285F4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(36.dp))
                        }
                        Text(
                            text = "Sincronización con Google Drive",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Podrás conectar y autorizar tu cuenta de Google Drive para sincronizar carpetas de libros en la nube. Esta función estará disponible en la sección de Ajustes > Conexiones Cloud de tu televisor.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth(0.85f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // =========================================================================
                // PASO 2: IDIOMA DE LA APLICACIÓN
                // =========================================================================
                SetupWizardStep.LANGUAGE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "¿Qué idioma prefieres para la aplicación?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Selecciona el idioma principal de la interfaz y la síntesis de voz.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Opción Español
                            SetupOptionCard(
                                title = "Español 🇪🇸",
                                description = "Interfaz y voces neuronales en español",
                                icon = Icons.Default.Language,
                                iconColor = AmberWarm,
                                isSelected = selectedLanguage == "es",
                                focusRequester = initialFocus,
                                modifier = Modifier.weight(1f),
                                onSelect = { selectedLanguage = "es" }
                            )

                            // Opción Inglés
                            SetupOptionCard(
                                title = "English 🇺🇸",
                                description = "Interface and neural text-to-speech in English",
                                icon = Icons.Default.Language,
                                iconColor = CyanElectric,
                                isSelected = selectedLanguage == "en",
                                modifier = Modifier.weight(1f),
                                onSelect = { selectedLanguage = "en" }
                            )
                        }
                    }
                }

                // =========================================================================
                // PASO 3: TEMA DE LA APLICACIÓN (CLARO / OSCURO)
                // =========================================================================
                SetupWizardStep.THEME -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Tema Visual de la Aplicación",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Elige la apariencia que mejor se adapte a tu televisor o proyector.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Modo Oscuro
                            SetupOptionCard(
                                title = "Tema Oscuro 🌙",
                                description = "Fondos negros cinemáticos optimizados para TV, OLED y lectura nocturna",
                                icon = Icons.Default.DarkMode,
                                iconColor = CyanElectric,
                                isSelected = isDarkTheme,
                                focusRequester = initialFocus,
                                modifier = Modifier.weight(1f),
                                onSelect = { isDarkTheme = true }
                            )

                            // Modo Claro
                            SetupOptionCard(
                                title = "Tema Claro ☀️",
                                description = "Mayor contraste diurno tipo papel pergamino claro",
                                icon = Icons.Default.LightMode,
                                iconColor = AmberWarm,
                                isSelected = !isDarkTheme,
                                modifier = Modifier.weight(1f),
                                onSelect = { isDarkTheme = false }
                            )
                        }
                    }
                }

                // =========================================================================
                // PASO 4: PERFIL INICIAL (¿QUIÉN LEERÁ LOS LIBROS?)
                // =========================================================================
                SetupWizardStep.PROFILE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "¿Quién leerá los libros?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Crea tu perfil inicial de lectura para guardar el progreso de cada página y tus libros favoritos.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Preview Card
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceContainerHigh)
                                    .padding(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(selectedColor))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profileName.take(1).uppercase(),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                }
                                Text(profileName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            // Inputs y colores
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("Nombre del Perfil:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = profileName,
                                    onValueChange = { profileName = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberWarm,
                                        unfocusedBorderColor = Color(0xFF383842),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                Text("Color Favorito:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    presetColors.forEach { colorHex ->
                                        val isColorSelected = selectedColor == colorHex
                                        var isFocused by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .scale(if (isFocused) 1.2f else 1f)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                                .border(
                                                    width = if (isColorSelected || isFocused) 2.5.dp else 1.dp,
                                                    color = if (isFocused) Color.White else if (isColorSelected) TextPrimary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .onFocusChanged { isFocused = it.isFocused }
                                                .onKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown &&
                                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                                    ) {
                                                        selectedColor = colorHex
                                                        true
                                                    } else false
                                                }
                                                .focusable()
                                                .clickable { selectedColor = colorHex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isColorSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Barra inferior de botones de navegación (Atrás / Siguiente / Finalizar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Atrás (disponible en todos los pasos excepto el inicial)
                if (step != SetupWizardStep.WELCOME_CHOICE) {
                    var isBackFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isBackFocused) AmberWarm else SurfaceRaised)
                            .border(1.dp, if (isBackFocused) Color.White else Color(0xFF383842), RoundedCornerShape(12.dp))
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    step = when (step) {
                                        SetupWizardStep.OPDS_FORM, SetupWizardStep.WIFI_INFO, SetupWizardStep.GDRIVE_INFO -> SetupWizardStep.WELCOME_CHOICE
                                        SetupWizardStep.LANGUAGE -> SetupWizardStep.WELCOME_CHOICE
                                        SetupWizardStep.THEME -> SetupWizardStep.LANGUAGE
                                        SetupWizardStep.PROFILE -> SetupWizardStep.THEME
                                        else -> SetupWizardStep.WELCOME_CHOICE
                                    }
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable {
                                step = when (step) {
                                    SetupWizardStep.OPDS_FORM, SetupWizardStep.WIFI_INFO, SetupWizardStep.GDRIVE_INFO -> SetupWizardStep.WELCOME_CHOICE
                                    SetupWizardStep.LANGUAGE -> SetupWizardStep.WELCOME_CHOICE
                                    SetupWizardStep.THEME -> SetupWizardStep.LANGUAGE
                                    SetupWizardStep.PROFILE -> SetupWizardStep.THEME
                                    else -> SetupWizardStep.WELCOME_CHOICE
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "< Atrás",
                            color = if (isBackFocused) Color(0xFF131315) else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Botón de acción derecha
                when (step) {
                    SetupWizardStep.WELCOME_CHOICE -> {
                        // En la bienvenida las 4 tarjetas navegan directamente con un clic
                        Text(
                            text = "Navega con las flechas del mando y presiona OK para seleccionar",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    SetupWizardStep.OPDS_FORM -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Botón Probar
                            var isTestFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isTestFocused) AmberWarm else SurfaceRaised)
                                    .border(1.dp, if (isTestFocused) Color.White else Color(0xFF383842), RoundedCornerShape(12.dp))
                                    .onFocusChanged { isTestFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            isConnecting = true
                                            scope.launch {
                                                val res = OpdsClient.fetchFeed(serverUrl.trim(), username.trim(), password.trim())
                                                isConnecting = false
                                                if (res.isSuccess) {
                                                    testSuccess = true
                                                    testResult = "¡Conexión exitosa! Catálogo detectado."
                                                } else {
                                                    testSuccess = false
                                                    testResult = "Error al conectar: ${res.exceptionOrNull()?.message}"
                                                }
                                            }
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable {
                                        isConnecting = true
                                        scope.launch {
                                            val res = OpdsClient.fetchFeed(serverUrl.trim(), username.trim(), password.trim())
                                            isConnecting = false
                                            if (res.isSuccess) {
                                                testSuccess = true
                                                testResult = "¡Conexión exitosa! Catálogo detectado."
                                            } else {
                                                testSuccess = false
                                                testResult = "Error al conectar: ${res.exceptionOrNull()?.message}"
                                            }
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Probar Conexión",
                                    color = if (isTestFocused) Color(0xFF131315) else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Botón Siguiente
                            var isNextFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isNextFocused) CyanElectric else AmberWarm)
                                    .onFocusChanged { isNextFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            if (serverUrl.isNotBlank() && serverUrl != "http://") {
                                                repository.saveServerConfig(ServerConfig(serverUrl.trim(), username.trim(), password.trim()))
                                            }
                                            step = SetupWizardStep.LANGUAGE
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable {
                                        if (serverUrl.isNotBlank() && serverUrl != "http://") {
                                            repository.saveServerConfig(ServerConfig(serverUrl.trim(), username.trim(), password.trim()))
                                        }
                                        step = SetupWizardStep.LANGUAGE
                                    }
                                    .padding(horizontal = 22.dp, vertical = 12.dp)
                            ) {
                                Text("Siguiente >", color = Color(0xFF131315), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    SetupWizardStep.WIFI_INFO, SetupWizardStep.GDRIVE_INFO -> {
                        var isNextFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNextFocused) CyanElectric else AmberWarm)
                                .onFocusChanged { isNextFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        step = SetupWizardStep.LANGUAGE
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable { step = SetupWizardStep.LANGUAGE }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Continuar >", color = Color(0xFF131315), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    SetupWizardStep.LANGUAGE -> {
                        var isNextFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNextFocused) CyanElectric else AmberWarm)
                                .onFocusChanged { isNextFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        repository.setAppLanguage(selectedLanguage)
                                        step = SetupWizardStep.THEME
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable {
                                    repository.setAppLanguage(selectedLanguage)
                                    step = SetupWizardStep.THEME
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Siguiente >", color = Color(0xFF131315), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    SetupWizardStep.THEME -> {
                        var isNextFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNextFocused) CyanElectric else AmberWarm)
                                .onFocusChanged { isNextFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        repository.setDarkTheme(isDarkTheme)
                                        step = SetupWizardStep.PROFILE
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable {
                                    repository.setDarkTheme(isDarkTheme)
                                    step = SetupWizardStep.PROFILE
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Siguiente >", color = Color(0xFF131315), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    SetupWizardStep.PROFILE -> {
                        var isFinishFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isFinishFocused) CyanElectric else AmberWarm)
                                .shadow(if (isFinishFocused) 16.dp else 4.dp, shape = RoundedCornerShape(14.dp), spotColor = AmberWarm)
                                .onFocusChanged { isFinishFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        repository.setAppLanguage(selectedLanguage)
                                        repository.setDarkTheme(isDarkTheme)
                                        repository.createProfile(
                                            name = profileName.trim().ifBlank { if (selectedLanguage == "en") "My Profile" else "Mi Perfil" },
                                            colorHex = selectedColor,
                                            preferredLanguage = selectedLanguage
                                        )
                                        repository.setSetupCompleted(true)
                                        onSetupFinished()
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable {
                                    repository.setAppLanguage(selectedLanguage)
                                    repository.setDarkTheme(isDarkTheme)
                                    repository.createProfile(
                                        name = profileName.trim().ifBlank { if (selectedLanguage == "en") "My Profile" else "Mi Perfil" },
                                        colorHex = selectedColor,
                                        preferredLanguage = selectedLanguage
                                    )
                                    repository.setSetupCompleted(true)
                                    onSetupFinished()
                                }
                                .padding(horizontal = 28.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "¡Comenzar a Leer!",
                                color = Color(0xFF131315),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta navegable con D-Pad para selecciones de configuración
 */
@Composable
private fun SetupOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    focusRequester: FocusRequester? = null,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val baseModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }

    Box(
        modifier = baseModifier
            .scale(if (isFocused) 1.04f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) SurfaceRaised else if (isSelected) SurfaceContainerHighest else SurfaceContainerHigh)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                color = if (isFocused) AmberWarm else if (isSelected) CyanElectric else Color(0xFF333340),
                shape = RoundedCornerShape(16.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onSelect()
                    true
                } else false
            }
            .focusable()
            .clickable { onSelect() }
            .padding(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanElectric, modifier = Modifier.size(22.dp))
                }
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) AmberWarm else TextPrimary
            )

            Text(
                text = description,
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp
            )
        }
    }
}
