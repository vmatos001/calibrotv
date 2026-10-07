package com.example.calibretv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.server.QrCodeGenerator
import com.example.calibretv.data.server.WifiImportServer
import com.example.calibretv.data.storage.BookNoteEntity
import com.example.calibretv.data.storage.PreferencesManager
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📱 Panel Bento de Anotaciones & Companion Móvil para CalibroTV.
 *
 * Se despliega lateralmente desde el borde derecho:
 * - Contenedor Bento Superior: Código QR y enlace WiFi en tiempo real para escribir desde el smartphone.
 * - Contenedor Bento Inferior: Lista de notas del libro con navegación vertical completa por D-Pad,
 *   scroll fluido y opciones de eliminación con confirmación.
 */
@Composable
fun NotesModal(
    book: Book,
    repository: BookRepository,
    isDarkTheme: Boolean = PreferencesManager(LocalContext.current).isDarkTheme(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeProfile = remember { repository.getActiveProfile() }
    val wifiServer = remember { WifiImportServer.getInstance(context, repository) }

    val closeFocusRequester = remember { FocusRequester() }
    val firstNoteFocusRequester = remember { FocusRequester() }

    var isVisible by remember { mutableStateOf(false) }
    var notesList by remember { mutableStateOf<List<BookNoteEntity>>(emptyList()) }
    var serverUrl by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var noteToDelete by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember {
        SimpleDateFormat("dd/MM/yy • HH:mm", Locale.getDefault())
    }

    fun dismissAnimated() {
        scope.launch {
            isVisible = false
            delay(220L)
            onDismiss()
        }
    }

    // Intercepta el botón Atrás del control remoto
    BackHandler {
        dismissAnimated()
    }

    LaunchedEffect(Unit) {
        if (!wifiServer.isRunning) {
            wifiServer.startServer(8080)
        }
        serverUrl = wifiServer.buildUrl("/note?bookId=${book.id}&profileId=${activeProfile.id}")
        qrBitmap = QrCodeGenerator.generateQrBitmap(serverUrl, 300, 300)
        notesList = repository.getNotes(book.id, activeProfile.id)
        isVisible = true

        delay(150L)
        try {
            if (notesList.isNotEmpty()) {
                firstNoteFocusRequester.requestFocus()
            } else {
                closeFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    // Sincronización en tiempo real de notas agregadas desde el móvil
    LaunchedEffect(Unit) {
        repository.noteAddedEvents.collect { newNote ->
            if (newNote.bookId == book.id) {
                notesList = repository.getNotes(book.id, activeProfile.id)
            }
        }
    }

    // Capa base que cubre la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                    dismissAnimated()
                    true
                } else false
            }
    ) {
        // Scrim semitransparente izquierdo (hacer clic o tap cierra el panel)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { dismissAnimated() }
        )

        // Contenedor Bento anclado a la derecha con animación deslizante
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .width(276.dp)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp, end = 16.dp)
                        .clickable(enabled = false) {}, // Evita cerrar al hacer click dentro del bento
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ==========================================
                    // 1. CONTENEDOR BENTO SUPERIOR: QR COMPANION
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF16161A).copy(alpha = 0.98f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Título arriba del recuadro del QR
                            Text(
                                text = "Escribe desde tu móvil",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Recuadro del QR
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap!!.asImageBitmap(),
                                        contentDescription = "Código QR para escribir notas desde smartphone",
                                        modifier = Modifier.size(136.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(136.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = AmberWarm,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Descripción abajo
                            Text(
                                text = "Escanea el código QR con tu móvil para agregar notas con teclado táctil.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Estado Wifi activo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(Color(0xFF4CAF50), CircleShape)
                                )
                                Text(
                                    text = "Wifi activo",
                                    color = Color(0xFF81C784),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 2. CONTENEDOR BENTO INFERIOR: TUS NOTAS
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(16.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF16161A).copy(alpha = 0.98f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Cabecera Bento Inferior
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = AmberWarm,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Notas",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${notesList.size}",
                                            color = AmberWarm,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Botón Cerrar enfocado para TV
                                var isCloseFocused by remember { mutableStateOf(false) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .focusRequester(closeFocusRequester)
                                        .onFocusChanged { isCloseFocused = it.isFocused }
                                        .focusable()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { dismissAnimated() }
                                        .onKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown &&
                                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                                dismissAnimated()
                                                true
                                            } else false
                                        }
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCloseFocused) AmberWarm else Color.White.copy(alpha = 0.08f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isCloseFocused) AmberWarm else Color.White.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Cerrar",
                                        tint = if (isCloseFocused) Color(0xFF131316) else TextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Cerrar",
                                        color = if (isCloseFocused) Color(0xFF131316) else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Lista scrolleable de notas
                            if (notesList.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = TextMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Aún no tienes notas",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Escanea el código QR superior para redactar tu primera nota.",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(notesList, key = { _, note -> note.id }) { index, note ->
                                        var isCardFocused by remember { mutableStateOf(false) }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (index == 0) Modifier.focusRequester(firstNoteFocusRequester)
                                                    else Modifier
                                                )
                                                .scale(if (isCardFocused) 1.02f else 1.0f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isCardFocused) Color(0xFF25252D) else Color(0xFF1D1D22))
                                                .border(
                                                    width = if (isCardFocused) 1.5.dp else 1.dp,
                                                    color = if (isCardFocused) AmberWarm else Color.White.copy(alpha = 0.08f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .onFocusChanged { isCardFocused = it.isFocused }
                                                .focusable()
                                                .onKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown) {
                                                        when (event.key) {
                                                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                                if (noteToDelete == note.id) {
                                                                    scope.launch {
                                                                        repository.deleteNote(note.id)
                                                                        notesList = repository.getNotes(book.id, activeProfile.id)
                                                                        noteToDelete = null
                                                                    }
                                                                } else {
                                                                    noteToDelete = note.id
                                                                }
                                                                true
                                                            }
                                                            Key.DirectionLeft -> {
                                                                dismissAnimated()
                                                                true
                                                            }
                                                            else -> false
                                                        }
                                                    } else false
                                                }
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (noteToDelete == note.id) {
                                                        scope.launch {
                                                            repository.deleteNote(note.id)
                                                            notesList = repository.getNotes(book.id, activeProfile.id)
                                                            noteToDelete = null
                                                        }
                                                    } else {
                                                        noteToDelete = note.id
                                                    }
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = dateFormat.format(Date(note.createdAt)),
                                                        color = AmberWarm,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )

                                                    if (noteToDelete == note.id) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "¿Borrar?",
                                                                color = Color(0xFFFF5252),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFFD32F2F))
                                                                    .clickable {
                                                                        scope.launch {
                                                                            repository.deleteNote(note.id)
                                                                            notesList = repository.getNotes(book.id, activeProfile.id)
                                                                            noteToDelete = null
                                                                        }
                                                                    }
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("Sí", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color.White.copy(alpha = 0.15f))
                                                                    .clickable { noteToDelete = null }
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("No", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Filled.Delete,
                                                            contentDescription = "Eliminar nota",
                                                            tint = if (isCardFocused) AmberWarm else TextMuted,
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clickable { noteToDelete = note.id }
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = note.noteText,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    lineHeight = 19.sp,
                                                    fontFamily = FontFamily.Serif
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
        }
    }
}
