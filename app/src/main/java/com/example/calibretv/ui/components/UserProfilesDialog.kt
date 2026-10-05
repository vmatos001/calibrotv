package com.example.calibretv.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.UserProfile
import com.example.calibretv.data.storage.PreferencesManager
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.InkSecondary
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun UserProfilesDialog(
    repository: BookRepository,
    activeProfile: UserProfile,
    isDarkTheme: Boolean = PreferencesManager(LocalContext.current).isDarkTheme(),
    onProfileChanged: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var profiles by remember { mutableStateOf(repository.getProfiles()) }
    var currentActive by remember { mutableStateOf(activeProfile) }
    var showCreateField by remember { mutableStateOf(false) }

    // New profile form state
    var newUserName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#C5A059") }
    var isNewUserKidsMode by remember { mutableStateOf(false) }
    var newUserPin by remember { mutableStateOf<String?>(null) }
    var newUserLanguage by remember { mutableStateOf("es") }

    // PIN Pad states
    var showPinPadForTargetProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showPinPadForCreatingPin by remember { mutableStateOf(false) }

    val presetColors = listOf("#C5A059", "#38BDF8", "#4CAF50", "#AB47BC", "#FF5722", "#E91E63")
    val initialFocusRequester = remember { FocusRequester() }

    BackHandler {
        if (showPinPadForTargetProfile != null) {
            showPinPadForTargetProfile = null
        } else if (showPinPadForCreatingPin) {
            showPinPadForCreatingPin = false
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun applySwitchProfile(profile: UserProfile) {
        repository.saveActiveProfile(profile)
        currentActive = profile
        onProfileChanged(profile)
    }

    fun handleProfileSelection(target: UserProfile) {
        if (target.id == currentActive.id) return

        // If target profile has a PIN, verify it first
        if (target.parentalPin != null) {
            showPinPadForTargetProfile = target
        } else if (currentActive.isKidsMode) {
            // Leaving Kids Mode to an unprotected profile: check if ANY profile in the house has a parental PIN to protect exit
            val housePin = profiles.firstOrNull { it.parentalPin != null }?.parentalPin
            if (housePin != null) {
                showPinPadForTargetProfile = target.copy(parentalPin = housePin)
            } else {
                applySwitchProfile(target)
            }
        } else {
            applySwitchProfile(target)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .fillMaxHeight(0.85f)
                .shadow(
                    elevation = if (isDarkTheme) 0.dp else 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDarkTheme) SurfaceContainer else Color(0xFFF7F5F0))
                .clickable(enabled = false) {}
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentGold.copy(alpha = 0.2f))
                                    .border(1.dp, AccentGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "PERFILES FAMILIARES",
                                    color = if (isDarkTheme) TextPrimary else InkPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Progreso, favoritos, tamaño de letra y control parental independiente por lector",
                                    color = if (isDarkTheme) TextMuted else InkSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Current active badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentGold.copy(alpha = if (isDarkTheme) 0.15f else 0.22f))
                                .border(1.dp, AccentGold, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Activo: ", color = if (isDarkTheme) TextMuted else InkSecondary, fontSize = 12.sp)
                            Text(currentActive.name, color = AccentGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = currentActive.preferredLanguage.uppercase(),
                                color = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDarkTheme) Color(0xFF152642) else Color(0xFFE3F2FD))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                            if (currentActive.isKidsMode) {
                                Text("🎈", fontSize = 12.sp)
                            }
                            if (currentActive.starsCount > 0) {
                                Text("⭐ ${currentActive.starsCount}", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Middle: Profile Cards Row
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Selecciona un lector:",
                        color = if (isDarkTheme) TextPrimary else InkPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (profiles.isEmpty()) {
                        Text(
                            text = "No hay perfiles creados. Pulsa 'Crear Perfil' abajo para comenzar.",
                            color = if (isDarkTheme) TextMuted else InkSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(profiles) { profile ->
                                val isActive = profile.id == currentActive.id
                                val shouldFocus = if (profiles.any { it.id == currentActive.id }) isActive else profile == profiles.firstOrNull()
                                var isFocused by remember { mutableStateOf(false) }

                                val cardModifier = Modifier
                                    .width(160.dp)
                                    .scale(if (isFocused) 1.06f else 1.0f)
                                    .shadow(
                                        elevation = if (isDarkTheme) 0.dp else (if (isFocused) 8.dp else 2.dp),
                                        shape = RoundedCornerShape(18.dp),
                                        spotColor = Color.Black.copy(alpha = 0.08f)
                                    )
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isActive) (if (isDarkTheme) AccentGold.copy(alpha = 0.15f) else AmberWarm.copy(alpha = 0.2f))
                                        else (if (isDarkTheme) SurfaceRaised else Color.White)
                                    )
                                    .border(
                                        width = if (isFocused) 2.5.dp else if (isActive) 1.5.dp else if (isDarkTheme) 1.dp else 0.dp,
                                        color = if (isFocused) AccentGold else if (isActive) AccentGold.copy(alpha = 0.8f) else if (isDarkTheme) Color(0xFF2E2E36) else Color.Transparent,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .then(if (shouldFocus) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            handleProfileSelection(profile)
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable { handleProfileSelection(profile) }
                                    .padding(vertical = 18.dp, horizontal = 12.dp)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = cardModifier
                                ) {
                                    val profileColor = try {
                                        Color(android.graphics.Color.parseColor(profile.avatarColorHex))
                                    } catch (_: Exception) {
                                        AccentGold
                                    }

                                    // Avatar circle with initial or face
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                            .background(profileColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = profile.name.take(1).uppercase(),
                                            color = Color(0xFF0C0A09),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    // Profile Name
                                    Text(
                                        text = profile.name,
                                        color = if (isDarkTheme) TextPrimary else InkPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )

                                    // Badges row: Language, Kids, Stars, Pin Lock
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = profile.preferredLanguage.uppercase(),
                                            color = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isDarkTheme) Color(0xFF152642) else Color(0xFFE3F2FD))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                        if (profile.isKidsMode) {
                                            Text(
                                                text = "🎈 Kids",
                                                color = Color(0xFF81C784),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (profile.starsCount > 0) {
                                            Text(
                                                text = "⭐ ${profile.starsCount}",
                                                color = AccentGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (profile.parentalPin != null) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Protegido con PIN",
                                                tint = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF757575),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    if (isActive) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AccentGold)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF0C0A09),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text("ACTIVO", color = Color(0xFF0C0A09), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    } else {
                                        Text(
                                            text = if (profile.parentalPin != null) "🔒 Desbloquear" else "Seleccionar",
                                            color = if (isDarkTheme) TextMuted else InkSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Create New User Form / Toggle
                if (showCreateField) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(if (isDarkTheme) 0.dp else 4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDarkTheme) SurfaceContainerHigh else Color.White)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Nuevo Perfil de Lectura:",
                            color = if (isDarkTheme) TextPrimary else InkPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var isNameFieldFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isNameFieldFocused) (if (isDarkTheme) Color(0xFF262320) else Color(0xFFF0F4F0)) else (if (isDarkTheme) SurfaceContainerHigh else Color(0xFFF6F8F6)))
                                    .border(
                                        width = if (isNameFieldFocused) 2.dp else if (isDarkTheme) 1.dp else 0.dp,
                                        color = if (isNameFieldFocused) AccentGold else if (isDarkTheme) Color(0xFF4A4A58) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .onFocusChanged { isNameFieldFocused = it.isFocused }
                            ) {
                                OutlinedTextField(
                                    value = newUserName,
                                    onValueChange = { newUserName = it },
                                    placeholder = {
                                        Text(
                                            "Ej: Mateo, Papá, Niños...",
                                            color = if (isDarkTheme) TextMuted else InkSecondary,
                                            fontSize = 13.sp
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = if (isDarkTheme) TextPrimary else InkPrimary,
                                        unfocusedTextColor = if (isDarkTheme) TextPrimary else InkPrimary,
                                        cursorColor = AccentGold
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Kids Mode Toggle Button
                            var isKidsBtnFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isKidsBtnFocused -> AccentGold
                                            isNewUserKidsMode -> Color(0xFF1B4D2E)
                                            else -> SurfaceRaised
                                        }
                                    )
                                    .border(
                                        width = if (isKidsBtnFocused || isNewUserKidsMode) 1.5.dp else 1.dp,
                                        color = if (isKidsBtnFocused) Color.White else if (isNewUserKidsMode) Color(0xFF4CAF50) else Color(0xFF383842),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .onFocusChanged { isKidsBtnFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            isNewUserKidsMode = !isNewUserKidsMode
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable { isNewUserKidsMode = !isNewUserKidsMode }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isNewUserKidsMode) "🎈 Modo Infantil: SÍ" else "🎈 Modo Infantil: NO",
                                    color = if (isKidsBtnFocused) Color(0xFF0C0A09) else if (isNewUserKidsMode) Color(0xFFA5D6A7) else (if (isDarkTheme) TextMuted else InkSecondary),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Set PIN Toggle Button
                            var isPinBtnFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isPinBtnFocused -> AccentGold
                                            newUserPin != null -> Color(0xFF3E2723)
                                            else -> if (isDarkTheme) SurfaceRaised else Color(0xFFF3F5F3)
                                        }
                                    )
                                    .border(
                                        width = if (isPinBtnFocused || newUserPin != null) 1.5.dp else if (isDarkTheme) 1.dp else 0.dp,
                                        color = if (isPinBtnFocused) Color.White else if (newUserPin != null) AccentGold else if (isDarkTheme) Color(0xFF383842) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .onFocusChanged { isPinBtnFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            if (newUserPin != null) newUserPin = null
                                            else showPinPadForCreatingPin = true
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable {
                                        if (newUserPin != null) newUserPin = null
                                        else showPinPadForCreatingPin = true
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (newUserPin != null) "🔒 PIN Configurado" else "🔒 Añadir PIN",
                                    color = if (isPinBtnFocused) Color(0xFF0C0A09) else if (newUserPin != null) AccentGold else (if (isDarkTheme) TextMuted else InkSecondary),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Language Preference Toggle Button
                            var isLangBtnFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isLangBtnFocused -> AccentGold
                                            newUserLanguage == "en" -> Color(0xFF152642)
                                            else -> if (isDarkTheme) SurfaceRaised else Color(0xFFF3F5F3)
                                        }
                                    )
                                    .border(
                                        width = if (isLangBtnFocused || newUserLanguage == "en") 1.5.dp else if (isDarkTheme) 1.dp else 0.dp,
                                        color = if (isLangBtnFocused) Color.White else if (newUserLanguage == "en") Color(0xFF64B5F6) else if (isDarkTheme) Color(0xFF383842) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .onFocusChanged { isLangBtnFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            newUserLanguage = if (newUserLanguage == "en") "es" else "en"
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable { newUserLanguage = if (newUserLanguage == "en") "es" else "en" }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (newUserLanguage == "en") "Idioma: English (EN)" else "Idioma: Español (ES)",
                                    color = if (isLangBtnFocused) Color(0xFF0C0A09) else if (newUserLanguage == "en") Color(0xFF90CAF9) else (if (isDarkTheme) TextMuted else InkSecondary),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Save button
                            var isSaveFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSaveFocused) Color.White else AccentGold)
                                    .onFocusChanged { isSaveFocused = it.isFocused }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            if (newUserName.isNotBlank()) {
                                                val created = repository.createProfile(
                                                    name = newUserName.trim(),
                                                    colorHex = selectedColorHex,
                                                    isKidsMode = isNewUserKidsMode,
                                                    parentalPin = newUserPin,
                                                    preferredLanguage = newUserLanguage
                                                )
                                                profiles = repository.getProfiles()
                                                currentActive = created
                                                onProfileChanged(created)
                                                showCreateField = false
                                                newUserName = ""
                                                isNewUserKidsMode = false
                                                newUserPin = null
                                                newUserLanguage = "es"
                                            }
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable {
                                        if (newUserName.isNotBlank()) {
                                            val created = repository.createProfile(
                                                name = newUserName.trim(),
                                                colorHex = selectedColorHex,
                                                isKidsMode = isNewUserKidsMode,
                                                parentalPin = newUserPin,
                                                preferredLanguage = newUserLanguage
                                            )
                                            profiles = repository.getProfiles()
                                            currentActive = created
                                            onProfileChanged(created)
                                            showCreateField = false
                                            newUserName = ""
                                            isNewUserKidsMode = false
                                            newUserPin = null
                                            newUserLanguage = "es"
                                        }
                                    }
                                    .padding(horizontal = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Guardar", color = Color(0xFF0C0A09), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // Colors row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Color:", color = if (isDarkTheme) TextMuted else InkSecondary, fontSize = 11.sp)
                            presetColors.forEach { hex ->
                                val isColorSelected = selectedColorHex == hex
                                var isColorFocused by remember { mutableStateOf(false) }
                                val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { AccentGold }

                                Box(
                                    modifier = Modifier
                                        .size(if (isColorFocused) 36.dp else 28.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                        .border(
                                            width = if (isColorFocused || isColorSelected) 2.5.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .onFocusChanged { isColorFocused = it.isFocused }
                                        .onKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown &&
                                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                            ) {
                                                selectedColorHex = hex
                                                true
                                            } else false
                                        }
                                        .focusable()
                                        .clickable { selectedColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isColorSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isAddFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .shadow(if (isDarkTheme) 0.dp else 2.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.08f))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAddFocused) AccentGold else (if (isDarkTheme) SurfaceContainerHigh else Color.White))
                            .border(
                                width = if (isAddFocused) 2.dp else if (isDarkTheme) 1.dp else 0.dp,
                                color = if (isAddFocused) Color.White else if (isDarkTheme) Color(0xFF383842) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .onFocusChanged { isAddFocused = it.isFocused }
                            .then(if (profiles.isEmpty()) Modifier.focusRequester(initialFocusRequester) else Modifier)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    showCreateField = !showCreateField
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable { showCreateField = !showCreateField }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isAddFocused) Color(0xFF0C0A09) else AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (showCreateField) "Cancelar" else "Crear Nuevo Perfil",
                            color = if (isAddFocused) Color(0xFF0C0A09) else (if (isDarkTheme) TextPrimary else InkPrimary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Active Profile Language Switcher Button
                    var isLangToggleFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .shadow(if (isDarkTheme) 0.dp else 2.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.08f))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLangToggleFocused) AccentGold else (if (isDarkTheme) SurfaceContainerHigh else Color.White))
                            .border(
                                width = if (isLangToggleFocused) 2.dp else if (isDarkTheme) 1.dp else 0.dp,
                                color = if (isLangToggleFocused) Color.White else if (isDarkTheme) Color(0xFF383842) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .onFocusChanged { isLangToggleFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    val nextLang = if (currentActive.preferredLanguage == "en") "es" else "en"
                                    val updated = currentActive.copy(preferredLanguage = nextLang)
                                    repository.updateProfile(updated)
                                    currentActive = updated
                                    profiles = repository.getProfiles()
                                    onProfileChanged(updated)
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable {
                                val nextLang = if (currentActive.preferredLanguage == "en") "es" else "en"
                                val updated = currentActive.copy(preferredLanguage = nextLang)
                                repository.updateProfile(updated)
                                currentActive = updated
                                profiles = repository.getProfiles()
                                onProfileChanged(updated)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (currentActive.preferredLanguage == "en") "Idioma: English (EN)" else "Idioma: Español (ES)",
                            color = if (isLangToggleFocused) Color(0xFF0C0A09) else (if (isDarkTheme) TextPrimary else InkPrimary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    var isCloseFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .shadow(if (isDarkTheme) 0.dp else 2.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.08f))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCloseFocused) AccentGold else (if (isDarkTheme) SurfaceRaised else Color.White))
                            .border(
                                width = if (isCloseFocused) 2.dp else if (isDarkTheme) 1.dp else 0.dp,
                                color = if (isCloseFocused) Color.White else if (isDarkTheme) Color(0xFF383842) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    onDismiss()
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable { onDismiss() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isCloseFocused) Color(0xFF0C0A09) else (if (isDarkTheme) TextPrimary else InkPrimary),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Listo",
                            color = if (isCloseFocused) Color(0xFF0C0A09) else (if (isDarkTheme) TextPrimary else InkPrimary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // PIN Pad verification when switching to a protected profile
    val targetProfile = showPinPadForTargetProfile
    if (targetProfile != null) {
        PinPadDialog(
            title = "Acceso Protegido",
            subtitle = "Introduce el PIN de 4 dígitos para acceder a ${targetProfile.name}",
            targetPin = targetProfile.parentalPin,
            isDarkTheme = isDarkTheme,
            onSuccess = {
                showPinPadForTargetProfile = null
                applySwitchProfile(targetProfile)
            },
            onDismiss = {
                showPinPadForTargetProfile = null
            }
        )
    }

    // PIN Pad definition when creating a PIN for a new profile
    if (showPinPadForCreatingPin) {
        PinPadDialog(
            title = "Definir PIN Parental",
            subtitle = "Introduce 4 dígitos para proteger este perfil",
            targetPin = null,
            isDarkTheme = isDarkTheme,
            onSuccess = { pin ->
                newUserPin = pin
                showPinPadForCreatingPin = false
            },
            onDismiss = {
                showPinPadForCreatingPin = false
            }
        )
    }
}
