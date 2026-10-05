package com.example.calibretv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.storage.PreferencesManager
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CanvasBackgroundLight
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.InkSecondary
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.components.TvSidebar
import kotlinx.coroutines.launch

@Composable
fun ServerScreen(
    repository: BookRepository,
    onConnected: () -> Unit,
    onBack: () -> Unit = {},
    onTabSelected: (TvNavTab) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    var isDarkTheme by remember { mutableStateOf(prefs.isDarkTheme()) }
    val sidebarFocusRequester = remember { FocusRequester() }

    var config by remember { mutableStateOf(repository.getServerConfig()) }
    var statusText by remember { mutableStateOf("Listo para conectar") }
    var isChecking by remember { mutableStateOf(false) }

    // Focus requesters for TV on-screen keyboard chaining
    val urlFocusRequester = remember { FocusRequester() }
    val userFocusRequester = remember { FocusRequester() }
    val passFocusRequester = remember { FocusRequester() }
    val connectFocusRequester = remember { FocusRequester() }

    var isUrlFocused by remember { mutableStateOf(false) }
    var isUserFocused by remember { mutableStateOf(false) }
    var isPassFocused by remember { mutableStateOf(false) }
    var isConnectFocused by remember { mutableStateOf(false) }
    var isDemoFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        connectFocusRequester.requestFocus()
    }

    val screenBg = if (isDarkTheme) BackgroundDark else CanvasBackgroundLight
    val contentPrimary = if (isDarkTheme) TextPrimary else InkPrimary
    val contentSecondary = if (isDarkTheme) TextMuted else InkSecondary

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .shadow(
                            elevation = if (isDarkTheme) 0.dp else 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color.Black.copy(alpha = 0.05f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDarkTheme) SurfaceContainer else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isDarkTheme) Color(0xFF26262A) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "CALIBRE TV",
                                color = AmberWarm,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "• Conexión OPDS",
                                color = CyanElectric,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Ingresa la dirección de tu servidor local Calibre-Web o pulsa 'Sincronizar Biblioteca' para conectar de inmediato.",
                            color = contentSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 1. URL Input Field with ImeAction.Next
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "URL DEL SERVIDOR OPDS",
                                color = contentSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(if (isUrlFocused) 1.02f else 1.0f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDarkTheme) SurfaceContainerHigh else Color(0xFFF3F5F3))
                                    .border(
                                        width = if (isUrlFocused) 2.dp else if (isDarkTheme) 1.dp else 0.dp,
                                        color = if (isUrlFocused) CyanElectric else if (isDarkTheme) Color(0xFF333338) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = config.serverUrl,
                                    onValueChange = { config = config.copy(serverUrl = it) },
                                    textStyle = TextStyle(color = contentPrimary, fontSize = 15.sp),
                                    cursorBrush = SolidColor(CyanElectric),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { userFocusRequester.requestFocus() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(urlFocusRequester)
                                        .onFocusChanged { isUrlFocused = it.isFocused }
                                )
                            }
                        }

                        // 2. Optional User & Password with ImeAction.Next and ImeAction.Done
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "USUARIO (Opcional)", color = contentSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(if (isUserFocused) 1.02f else 1.0f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDarkTheme) SurfaceContainerHigh else Color(0xFFF3F5F3))
                                        .border(
                                            width = if (isUserFocused) 2.dp else if (isDarkTheme) 1.dp else 0.dp,
                                            color = if (isUserFocused) CyanElectric else if (isDarkTheme) Color(0xFF333338) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    BasicTextField(
                                        value = config.username,
                                        onValueChange = { config = config.copy(username = it) },
                                        textStyle = TextStyle(color = contentPrimary, fontSize = 15.sp),
                                        cursorBrush = SolidColor(CyanElectric),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(
                                            onNext = { passFocusRequester.requestFocus() }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(userFocusRequester)
                                            .onFocusChanged { isUserFocused = it.isFocused }
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "CONTRASEÑA (Opcional)", color = contentSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(if (isPassFocused) 1.02f else 1.0f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDarkTheme) SurfaceContainerHigh else Color(0xFFF3F5F3))
                                        .border(
                                            width = if (isPassFocused) 2.dp else if (isDarkTheme) 1.dp else 0.dp,
                                            color = if (isPassFocused) CyanElectric else if (isDarkTheme) Color(0xFF333338) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    BasicTextField(
                                        value = config.password,
                                        onValueChange = { config = config.copy(password = it) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        textStyle = TextStyle(color = contentPrimary, fontSize = 15.sp),
                                        cursorBrush = SolidColor(CyanElectric),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = { connectFocusRequester.requestFocus() }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(passFocusRequester)
                                            .onFocusChanged { isPassFocused = it.isFocused }
                                    )
                                }
                            }
                        }

                        // Status message
                        Text(
                            text = statusText,
                            color = when {
                                isChecking -> AmberWarm
                                statusText.startsWith("✓") -> CyanElectric
                                statusText.startsWith("⚠") -> Color(0xFFFF5252)
                                else -> contentSecondary
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons with 1-click activation
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .scale(if (isConnectFocused) 1.06f else 1.0f)
                                    .shadow(if (isDarkTheme) 0.dp else 2.dp, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberWarm)
                                    .border(
                                        width = if (isConnectFocused) 2.dp else 0.dp,
                                        color = if (isConnectFocused) CyanElectric else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .focusRequester(connectFocusRequester)
                                    .onFocusChanged { isConnectFocused = it.isFocused }
                                    .clickable {
                                        if (isChecking) return@clickable
                                        isChecking = true
                                        statusText = "Verificando conexión con ${config.serverUrl}..."
                                        scope.launch {
                                            val result = repository.scanServerLibrary(config)
                                            isChecking = false
                                            if (result.isSuccess) {
                                                val feed = result.getOrNull()!!
                                                statusText = "✓ Conectado exitosamente (${feed.books.size} libros encontrados)"
                                                kotlinx.coroutines.delay(700)
                                                onConnected()
                                            } else {
                                                val errMsg = result.exceptionOrNull()?.message ?: "Error al conectar con el servidor"
                                                statusText = "⚠ $errMsg"
                                            }
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isChecking) "Verificando..." else "Conectar y Guardar",
                                    color = Color(0xFF131315),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .scale(if (isDemoFocused) 1.06f else 1.0f)
                                    .shadow(if (isDarkTheme) 0.dp else 2.dp, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDarkTheme) SurfaceContainerHigh else Color(0xFFE8ECE8))
                                    .border(
                                        width = if (isDemoFocused) 2.dp else 0.dp,
                                        color = if (isDemoFocused) CyanElectric else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .onFocusChanged { isDemoFocused = it.isFocused }
                                    .clickable {
                                        if (isChecking) return@clickable
                                        isChecking = true
                                        statusText = "Sincronizando libros y portadas del servidor..."
                                        scope.launch {
                                            val result = repository.scanServerLibrary(config)
                                            isChecking = false
                                            if (result.isSuccess) {
                                                val feed = result.getOrNull()!!
                                                statusText = "✓ Sincronización exitosa (${feed.books.size} libros)"
                                                kotlinx.coroutines.delay(600)
                                                onConnected()
                                            } else {
                                                val errMsg = result.exceptionOrNull()?.message ?: "Error al sincronizar catálogo"
                                                statusText = "⚠ $errMsg"
                                            }
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sincronizar Biblioteca",
                                    color = contentPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Bottom Remote Helper Note
                Text(
                    text = "D-Pad Activo • Presiona OK para interactuar",
                    color = contentSecondary.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }

        // Floating rail navigation overlay
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
}
