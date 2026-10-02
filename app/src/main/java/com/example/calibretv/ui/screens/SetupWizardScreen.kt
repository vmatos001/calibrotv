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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.calibretv.data.model.ReadingSettings
import com.example.calibretv.data.model.ReadingTheme
import com.example.calibretv.data.model.ServerConfig
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupWizardScreen(
    repository: BookRepository,
    onSetupFinished: () -> Unit
) {
    // 1: Servidor OPDS, 2: Orientación & Pantalla, 3: Estética & Perfil Familiar
    var step by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    // Step 1 State: Server Config
    val currentConfig = remember { repository.getServerConfig() }
    var serverUrl by remember { mutableStateOf(currentConfig.serverUrl.ifBlank { "http://" }) }
    var username by remember { mutableStateOf(currentConfig.username) }
    var password by remember { mutableStateOf(currentConfig.password) }
    var isConnecting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }

    // Step 2 State: Orientación de Pantalla (Estándar vs Techo)
    val currentReadingSettings = remember { repository.getReadingSettings() }
    var isCeilingMode by remember { mutableStateOf(currentReadingSettings.ceilingMode) }

    // Step 3 State: Estética & Perfil Familiar
    var selectedTheme by remember { mutableStateOf(currentReadingSettings.theme) }
    var profileName by remember { mutableStateOf("Mi Perfil") }
    val presetColors = listOf("#FFA000", "#38BDF8", "#4CAF50", "#AB47BC", "#FF5722", "#E91E63")
    var selectedColor by remember { mutableStateOf("#FFA000") }

    val initialFocus = remember { FocusRequester() }

    LaunchedEffect(step) {
        delay(200)
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
            // Header with Steps Progress
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
                                    1 -> "Paso 1 de 3: Servidor OPDS / Calibre-Web"
                                    2 -> "Paso 2 de 3: Orientación y Modo de Pantalla"
                                    else -> "Paso 3 de 3: Estética y Perfil de Lectura"
                                },
                                fontSize = 13.sp,
                                color = AmberWarm,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 3 Step Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 3).forEach { stepNum ->
                            val isActive = step == stepNum
                            val isCompleted = step > stepNum
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

            // Body depending on Step
            when (step) {
                1 -> {
                    // ==========================================
                    // STEP 1: CONEXIÓN SERVIDOR OPDS
                    // ==========================================
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Conecta CalibroTV a tu biblioteca de Calibre o Calibre-Web para sincronizar tus libros con soporte EPUB:",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(initialFocus),
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

                            // Right column: Feedback status card
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
                                        Text("Probando y sincronizando catálogo...", fontSize = 12.sp, color = TextPrimary)
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
                                        text = "Ingresa la dirección IP de tu servidor Calibre-Web y pulsa 'Probar y Sincronizar', o pulsa 'Configurar Más Tarde' si prefieres transferir libros vía WiFi.",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ==========================================
                    // STEP 2: ORIENTACIÓN Y MODO PANTALLA
                    // ==========================================
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "CalibroTV está optimizado para salas de estar con televisores tradicionales y para dormitorios con proyectores al techo:",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Card 1: Estándar TV
                            var isTvFocused by remember { mutableStateOf(false) }
                            val isTvSelected = !isCeilingMode

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isTvFocused) Color(0xFF1E242B) else SurfaceContainerHigh)
                                    .border(
                                        width = if (isTvFocused) 2.5.dp else if (isTvSelected) 1.5.dp else 1.dp,
                                        color = when {
                                            isTvFocused -> AmberWarm
                                            isTvSelected -> AmberWarm.copy(alpha = 0.8f)
                                            else -> Color(0xFF333340)
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .onFocusChanged { isTvFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            isCeilingMode = false
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable { isCeilingMode = false }
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(if (isTvSelected) AmberWarm.copy(alpha = 0.2f) else Color(0xFF282834)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = null,
                                                tint = if (isTvSelected) AmberWarm else TextMuted,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        if (isTvSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Seleccionado",
                                                tint = AmberWarm,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Televisor Estándar",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Orientación horizontal estándar a 10 pies. Dos páginas lado a lado con curvatura física 3D.",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        lineHeight = 17.sp
                                    )
                                }
                            }

                            // Card 2: Modo Techo / Proyector
                            var isCeilingFocused by remember { mutableStateOf(false) }
                            val isCeilingSelected = isCeilingMode

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isCeilingFocused) Color(0xFF1E242B) else SurfaceContainerHigh)
                                    .border(
                                        width = if (isCeilingFocused) 2.5.dp else if (isCeilingSelected) 1.5.dp else 1.dp,
                                        color = when {
                                            isCeilingFocused -> AmberWarm
                                            isCeilingSelected -> AmberWarm.copy(alpha = 0.8f)
                                            else -> Color(0xFF333340)
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .onFocusChanged { isCeilingFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            isCeilingMode = true
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable { isCeilingMode = true }
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(if (isCeilingSelected) AmberWarm.copy(alpha = 0.2f) else Color(0xFF282834)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Flip,
                                                contentDescription = null,
                                                tint = if (isCeilingSelected) AmberWarm else TextMuted,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        if (isCeilingSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Seleccionado",
                                                tint = AmberWarm,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Modo Techo / Proyector",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Invierte y espeja verticalmente la imagen para leer acostado en la cama con proyector hacia el techo.",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // ==========================================
                    // STEP 3: ESTÉTICA Y PERFIL FAMILIAR
                    // ==========================================
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section A: Theme Selection
                        Text(
                            text = "Contraste de Lectura Recomendado para Pantallas Grandes:",
                            fontSize = 13.sp,
                            color = TextMuted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val themes = listOf(
                                Triple(ReadingTheme.OLED_PURE, "OLED Puro", "Negro absoluto (#000000)"),
                                Triple(ReadingTheme.NIGHT_AMBER, "Ámbar Noche", "Cero luz azul (#FFA000)"),
                                Triple(ReadingTheme.SEPIA_CINE, "Sepia Cine", "Tono cálido cinematográfico")
                            )

                            themes.forEach { (themeOption, title, desc) ->
                                val isSelected = selectedTheme == themeOption
                                var isFocused by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isFocused) Color(0xFF1E242B) else SurfaceContainerHigh)
                                        .border(
                                            width = if (isFocused) 2.5.dp else if (isSelected) 1.5.dp else 1.dp,
                                            color = when {
                                                isFocused -> AmberWarm
                                                isSelected -> AmberWarm
                                                else -> Color(0xFF333340)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .onKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown &&
                                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                            ) {
                                                selectedTheme = themeOption
                                                true
                                            } else false
                                        }
                                        .focusable()
                                        .clickable { selectedTheme = themeOption }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (themeOption) {
                                                        ReadingTheme.OLED_PURE -> Color.Black
                                                        ReadingTheme.NIGHT_AMBER -> AmberWarm
                                                        else -> Color(0xFF26201A)
                                                    }
                                                )
                                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                        )
                                        Column {
                                            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(desc, fontSize = 10.sp, color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF282834))
                        )

                        // Section B: First Profile
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val parsedColor = try {
                                Color(android.graphics.Color.parseColor(selectedColor))
                            } catch (_: Exception) {
                                AmberWarm
                            }
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(2.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profileName.take(1).uppercase().ifBlank { "U" },
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF131315)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Nombre del Perfil de Lectura:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

                                var isNameFieldFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.75f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNameFieldFocused) Color(0xFF1E2A35) else SurfaceContainerHigh)
                                        .border(
                                            width = if (isNameFieldFocused) 2.dp else 1.dp,
                                            color = if (isNameFieldFocused) AmberWarm else Color(0xFF4A4A58),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .onFocusChanged { isNameFieldFocused = it.isFocused }
                                ) {
                                    OutlinedTextField(
                                        value = profileName,
                                        onValueChange = { profileName = it },
                                        placeholder = {
                                            Text(
                                                "Ej: Mi Perfil, Familia...",
                                                color = TextMuted,
                                                fontSize = 13.sp
                                            )
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            cursorColor = AmberWarm
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp)
                                    )
                                }

                                Text("Color de Avatar:", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    presetColors.forEach { hex ->
                                        val isColorSelected = selectedColor == hex
                                        var isColorFocused by remember { mutableStateOf(false) }
                                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { AmberWarm }

                                        Box(
                                            modifier = Modifier
                                                .size(if (isColorFocused) 36.dp else 30.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .border(
                                                    width = when {
                                                        isColorFocused -> 3.dp
                                                        isColorSelected -> 2.dp
                                                        else -> 0.dp
                                                    },
                                                    color = Color.White,
                                                    shape = CircleShape
                                                )
                                                .onFocusChanged { isColorFocused = it.isFocused }
                                                .onKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown &&
                                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                                    ) {
                                                        selectedColor = hex
                                                        true
                                                    } else false
                                                }
                                                .focusable()
                                                .clickable { selectedColor = hex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isColorSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF131315),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (step) {
                    1 -> {
                        // Step 1: Test & Sync, Skip, Next
                        var isTestFocused by remember { mutableStateOf(false) }
                        var isSkipFocused by remember { mutableStateOf(false) }
                        var isNextFocused by remember { mutableStateOf(false) }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Test Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isTestFocused) AmberWarm else CyanElectric)
                                    .onFocusChanged { isTestFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            scope.launch {
                                                isConnecting = true
                                                testResult = null
                                                val cfg = ServerConfig(serverUrl.trim(), username.trim(), password.trim())
                                                val scanResult = repository.scanServerLibrary(cfg)
                                                isConnecting = false
                                                if (scanResult.isSuccess) {
                                                    val feed = scanResult.getOrNull()!!
                                                    if (feed.books.isNotEmpty()) {
                                                        testSuccess = true
                                                        testResult = "¡Conexión exitosa! Sincronizados ${feed.books.size} libros EPUB."
                                                    } else {
                                                        testSuccess = false
                                                        testResult = "Se conectó al servidor pero no se encontraron libros EPUB."
                                                    }
                                                } else {
                                                    testSuccess = false
                                                    val err = scanResult.exceptionOrNull()?.message ?: "Error de conexión"
                                                    testResult = "Fallo al conectar: $err"
                                                }
                                            }
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable {
                                        scope.launch {
                                            isConnecting = true
                                            testResult = null
                                            val cfg = ServerConfig(serverUrl.trim(), username.trim(), password.trim())
                                            val scanResult = repository.scanServerLibrary(cfg)
                                            isConnecting = false
                                            if (scanResult.isSuccess) {
                                                val feed = scanResult.getOrNull()!!
                                                if (feed.books.isNotEmpty()) {
                                                    testSuccess = true
                                                    testResult = "¡Conexión exitosa! Sincronizados ${feed.books.size} libros EPUB."
                                                } else {
                                                    testSuccess = false
                                                    testResult = "Se conectó al servidor pero no se encontraron libros EPUB."
                                                }
                                            } else {
                                                testSuccess = false
                                                val err = scanResult.exceptionOrNull()?.message ?: "Error de conexión"
                                                testResult = "Fallo al conectar: $err"
                                            }
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Probar y Sincronizar",
                                    color = Color(0xFF131315),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Skip Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSkipFocused) AmberWarm else SurfaceRaised)
                                    .border(1.dp, if (isSkipFocused) Color.White else Color(0xFF383842), RoundedCornerShape(12.dp))
                                    .onFocusChanged { isSkipFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            step = 2
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable { step = 2 }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Configurar Más Tarde",
                                    color = if (isSkipFocused) Color(0xFF131315) else TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Next Step
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNextFocused) AmberWarm else SurfaceContainerHigh)
                                .border(1.dp, if (isNextFocused) Color.White else AmberWarm, RoundedCornerShape(12.dp))
                                .onFocusChanged { isNextFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        val cfg = ServerConfig(serverUrl.trim(), username.trim(), password.trim())
                                        repository.saveServerConfig(cfg)
                                        step = 2
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable {
                                    val cfg = ServerConfig(serverUrl.trim(), username.trim(), password.trim())
                                    repository.saveServerConfig(cfg)
                                    step = 2
                                }
                                .padding(horizontal = 22.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Siguiente >",
                                color = if (isNextFocused) Color(0xFF131315) else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    2 -> {
                        // Step 2: Back and Next
                        var isBackFocused by remember { mutableStateOf(false) }
                        var isNextFocused by remember { mutableStateOf(false) }

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
                                        step = 1
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable { step = 1 }
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "< Atrás",
                                color = if (isBackFocused) Color(0xFF131315) else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNextFocused) AmberWarm else SurfaceContainerHigh)
                                .border(1.dp, if (isNextFocused) Color.White else AmberWarm, RoundedCornerShape(12.dp))
                                .onFocusChanged { isNextFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        step = 3
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable { step = 3 }
                                .padding(horizontal = 22.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Siguiente >",
                                color = if (isNextFocused) Color(0xFF131315) else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    3 -> {
                        // Step 3: Back and Finish
                        var isBackFocused by remember { mutableStateOf(false) }
                        var isFinishFocused by remember { mutableStateOf(false) }

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
                                        step = 2
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable { step = 2 }
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "< Atrás",
                                color = if (isBackFocused) Color(0xFF131315) else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

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
                                        // Save settings, create profile, finish
                                        val updatedSettings = repository.getReadingSettings().copy(
                                            ceilingMode = isCeilingMode,
                                            verticalMirror = isCeilingMode,
                                            rotation180 = isCeilingMode,
                                            theme = selectedTheme
                                        )
                                        repository.saveReadingSettings(updatedSettings)
                                        repository.createProfile(profileName.trim().ifBlank { "Mi Perfil" }, selectedColor)
                                        repository.setSetupCompleted(true)
                                        onSetupFinished()
                                        true
                                    } else false
                                }
                                .focusable()
                                .clickable {
                                    val updatedSettings = repository.getReadingSettings().copy(
                                        ceilingMode = isCeilingMode,
                                        verticalMirror = isCeilingMode,
                                        rotation180 = isCeilingMode,
                                        theme = selectedTheme
                                    )
                                    repository.saveReadingSettings(updatedSettings)
                                    repository.createProfile(profileName.trim().ifBlank { "Mi Perfil" }, selectedColor)
                                    repository.setSetupCompleted(true)
                                    onSetupFinished()
                                }
                                .padding(horizontal = 28.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "Comenzar a Disfrutar CalibroTV",
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
