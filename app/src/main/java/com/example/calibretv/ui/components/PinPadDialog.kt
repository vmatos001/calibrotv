package com.example.calibretv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.delay

/**
 * On-screen 3x4 visual PIN pad for Android TV remote controls.
 * Allows entering a 4-digit parental control PIN using exclusively D-Pad navigation.
 */
@Composable
fun PinPadDialog(
    title: String = "Control Parental",
    subtitle: String = "Introduce el código PIN de 4 dígitos",
    targetPin: String? = null,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val initialFocusRequester = remember { FocusRequester() }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun handleDigit(digit: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            errorMessage = null

            if (newPin.length == 4) {
                if (targetPin != null) {
                    if (newPin == targetPin) {
                        onSuccess(newPin)
                    } else {
                        errorMessage = "PIN incorrecto. Inténtalo de nuevo."
                        enteredPin = ""
                    }
                } else {
                    onSuccess(newPin)
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable { onDismiss() }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Zero, Key.NumPad0 -> { handleDigit("0"); true }
                        Key.One, Key.NumPad1 -> { handleDigit("1"); true }
                        Key.Two, Key.NumPad2 -> { handleDigit("2"); true }
                        Key.Three, Key.NumPad3 -> { handleDigit("3"); true }
                        Key.Four, Key.NumPad4 -> { handleDigit("4"); true }
                        Key.Five, Key.NumPad5 -> { handleDigit("5"); true }
                        Key.Six, Key.NumPad6 -> { handleDigit("6"); true }
                        Key.Seven, Key.NumPad7 -> { handleDigit("7"); true }
                        Key.Eight, Key.NumPad8 -> { handleDigit("8"); true }
                        Key.Nine, Key.NumPad9 -> { handleDigit("9"); true }
                        Key.Backspace, Key.Delete -> { handleBackspace(); true }
                        Key.Escape, Key.Back -> { onDismiss(); true }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainer)
                .border(1.5.dp, AccentGold.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Icon & Title
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AccentGold.copy(alpha = 0.15f))
                        .border(1.dp, AccentGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }

                // 4 PIN Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) AccentGold else Color.Transparent)
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) AccentGold else Color(0xFF6B655B),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Error message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 3x4 Grid Buttons
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BACK", "0", "CANCEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rows.forEachIndexed { rowIndex, rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEach { item ->
                                val isFirstButton = rowIndex == 0 && item == "1"
                                PinKeyButton(
                                    label = item,
                                    isInitial = isFirstButton,
                                    focusRequester = if (isFirstButton) initialFocusRequester else null,
                                    onClick = {
                                        when (item) {
                                            "BACK" -> handleBackspace()
                                            "CANCEL" -> onDismiss()
                                            else -> handleDigit(item)
                                        }
                                    }
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
private fun PinKeyButton(
    label: String,
    isInitial: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val btnModifier = Modifier
        .size(68.dp, 54.dp)
        .scale(if (isFocused) 1.10f else 1.0f)
        .clip(RoundedCornerShape(12.dp))
        .background(
            when {
                isFocused -> AccentGold
                label == "CANCEL" -> SurfaceRaised
                label == "BACK" -> SurfaceRaised
                else -> SurfaceContainer
            }
        )
        .border(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color.White else Color(0xFF383842),
            shape = RoundedCornerShape(12.dp)
        )
        .onFocusChanged { isFocused = it.isFocused }
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown &&
                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
            ) {
                onClick()
                true
            } else false
        }
        .focusable()
        .clickable { onClick() }

    Box(
        modifier = btnModifier,
        contentAlignment = Alignment.Center
    ) {
        val textColor = if (isFocused) Color(0xFF0C0A09) else TextPrimary
        when (label) {
            "BACK" -> {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Borrar",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            "CANCEL" -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            else -> {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
