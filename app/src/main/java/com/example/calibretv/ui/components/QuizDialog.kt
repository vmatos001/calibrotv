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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.quiz.QuizRepository
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun QuizDialog(
    bookTitle: String,
    repository: BookRepository,
    onDismiss: () -> Unit
) {
    val quiz = remember(bookTitle) { QuizRepository.getQuizForBook(bookTitle) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var correctAnswersCount by remember { mutableIntStateOf(0) }
    var isQuizCompleted by remember { mutableStateOf(false) }
    var earnedStars by remember { mutableIntStateOf(0) }

    val activeProfile = remember { repository.getActiveProfile() }
    val initialFocusRequester = remember { FocusRequester() }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(currentQuestionIndex, isQuizCompleted, isAnswerSubmitted) {
        delay(120)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
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
                .width(680.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainer)
                .border(1.5.dp, AccentGold.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isQuizCompleted) {
                // Reward Screen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = 0.2f))
                            .border(2.dp, AccentGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Text(
                        text = if (correctAnswersCount >= 2) "¡Misión Cumplida!" else "¡Buen Intento!",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "Acertaste $correctAnswersCount de ${quiz.questions.size} preguntas sobre «$bookTitle».",
                        color = TextMuted,
                        fontSize = 15.sp
                    )

                    if (earnedStars > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentGold.copy(alpha = 0.15f))
                                .border(1.dp, AccentGold, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("⭐", fontSize = 20.sp)
                            Text(
                                text = "¡Has ganado $earnedStars Estrella Dorada!",
                                color = AccentGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    var isCloseFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .scale(if (isCloseFocused) 1.08f else 1.0f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCloseFocused) AccentGold else SurfaceRaised)
                            .border(
                                width = if (isCloseFocused) 2.dp else 1.dp,
                                color = if (isCloseFocused) Color.White else Color(0xFF383842),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .focusRequester(initialFocusRequester)
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
                            .padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Continuar Leyendo",
                            color = if (isCloseFocused) Color(0xFF0C0A09) else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Question Screen
                val currentQ = quiz.questions[currentQuestionIndex]

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(AccentGold.copy(alpha = 0.2f))
                                    .border(1.dp, AccentGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "QUIZ DE LECTURA",
                                color = AccentGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Progress Indicator
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            quiz.questions.forEachIndexed { idx, _ ->
                                val isCur = idx == currentQuestionIndex
                                val isDone = idx < currentQuestionIndex
                                Box(
                                    modifier = Modifier
                                        .size(if (isCur) 14.dp else 10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCur -> AccentGold
                                                isDone -> Color(0xFF4CAF50)
                                                else -> Color(0xFF42424E)
                                            }
                                        )
                                )
                            }
                        }
                    }

                    // Question text
                    Text(
                        text = currentQ.question,
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Options A, B, C
                    val labels = listOf("A", "B", "C")
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentQ.options.forEachIndexed { optIndex, optionText ->
                            val isSelected = selectedOptionIndex == optIndex
                            val isCorrect = optIndex == currentQ.correctIndex
                            var isFocused by remember { mutableStateOf(false) }

                            val optionBg = when {
                                isAnswerSubmitted && isCorrect -> Color(0xFF1B4D2E)
                                isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFF5A1E1E)
                                isFocused -> SurfaceRaised
                                else -> SurfaceContainerHigh
                            }

                            val optionBorder = when {
                                isAnswerSubmitted && isCorrect -> Color(0xFF4CAF50)
                                isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFEF5350)
                                isFocused -> AccentGold
                                else -> Color(0xFF2C2825)
                            }

                            val rowModifier = Modifier
                                .fillMaxWidth()
                                .scale(if (isFocused) 1.03f else 1.0f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(optionBg)
                                .border(
                                    width = if (isFocused || (isAnswerSubmitted && (isCorrect || isSelected))) 2.dp else 1.dp,
                                    color = optionBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .onFocusChanged { isFocused = it.isFocused }
                                .then(if (optIndex == 0 && !isAnswerSubmitted) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                .onKeyEvent { event ->
                                    if (!isAnswerSubmitted && event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        selectedOptionIndex = optIndex
                                        isAnswerSubmitted = true
                                        if (isCorrect) correctAnswersCount++
                                        true
                                    } else false
                                }
                                .focusable(!isAnswerSubmitted)
                                .clickable(enabled = !isAnswerSubmitted) {
                                    selectedOptionIndex = optIndex
                                    isAnswerSubmitted = true
                                    if (isCorrect) correctAnswersCount++
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)

                            Row(
                                modifier = rowModifier,
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isFocused || isSelected) AccentGold else Color(0xFF33302C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = labels.getOrElse(optIndex) { "?" },
                                        color = if (isFocused || isSelected) Color(0xFF0C0A09) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = optionText,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isAnswerSubmitted && isCorrect) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (isAnswerSubmitted && isSelected && !isCorrect) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Explanation and Next Button
                    if (isAnswerSubmitted) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF201D1A))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = currentQ.explanation,
                                color = TextMuted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            var isNextFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .scale(if (isNextFocused) 1.08f else 1.0f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isNextFocused) AccentGold else SurfaceRaised)
                                    .border(
                                        width = if (isNextFocused) 2.dp else 1.dp,
                                        color = if (isNextFocused) Color.White else Color(0xFF383842),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .onFocusChanged { isNextFocused = it.isFocused }
                                    .focusRequester(initialFocusRequester)
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            if (currentQuestionIndex + 1 < quiz.questions.size) {
                                                currentQuestionIndex++
                                                selectedOptionIndex = null
                                                isAnswerSubmitted = false
                                            } else {
                                                if (correctAnswersCount >= 2) {
                                                    earnedStars = 1
                                                    repository.awardStarToProfile(activeProfile.id, 1)
                                                }
                                                isQuizCompleted = true
                                            }
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable {
                                        if (currentQuestionIndex + 1 < quiz.questions.size) {
                                            currentQuestionIndex++
                                            selectedOptionIndex = null
                                            isAnswerSubmitted = false
                                        } else {
                                            if (correctAnswersCount >= 2) {
                                                earnedStars = 1
                                                repository.awardStarToProfile(activeProfile.id, 1)
                                            }
                                            isQuizCompleted = true
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                val isLast = currentQuestionIndex + 1 >= quiz.questions.size
                                Text(
                                    text = if (isLast) "Ver Resultado" else "Siguiente Pregunta ➔",
                                    color = if (isNextFocused) Color(0xFF0C0A09) else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
