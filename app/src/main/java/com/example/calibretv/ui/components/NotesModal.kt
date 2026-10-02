package com.example.calibretv.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.server.QrCodeGenerator
import com.example.calibretv.data.server.WifiImportServer
import com.example.calibretv.data.storage.BookNoteEntity
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.AntiqueIvory
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceContainerHighest
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog modal for reading notes and reviews in BookSpread v3.0.
 * Includes a real-time QR code linked to the local HTTP server so readers
 * can compose thoughts with their smartphone keyboard without tedious D-Pad typing.
 */
@Composable
fun NotesModal(
    book: Book,
    repository: BookRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeProfile = remember { repository.getActiveProfile() }
    val wifiServer = remember { WifiImportServer.getInstance(context, repository) }
    val closeFocusRequester = remember { FocusRequester() }

    var notesList by remember { mutableStateOf<List<BookNoteEntity>>(emptyList()) }
    var serverUrl by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val dateFormat = remember {
        SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
    }

    LaunchedEffect(Unit) {
        if (!wifiServer.isRunning) {
            wifiServer.startServer(8080)
        }
        val ip = wifiServer.getLocalIpAddress()
        serverUrl = "http://$ip:8080/note?bookId=${book.id}&profileId=${activeProfile.id}"
        qrBitmap = QrCodeGenerator.generateQrBitmap(serverUrl, 320, 320)

        notesList = repository.getNotes(book.id, activeProfile.id)
        closeFocusRequester.requestFocus()
    }

    // Escucha en tiempo real de nuevas notas enviadas desde el smartphone
    LaunchedEffect(Unit) {
        repository.noteAddedEvents.collect { newNote ->
            if (newNote.bookId == book.id) {
                notesList = repository.getNotes(book.id, activeProfile.id)
            }
        }
    }

    // Overlay oscurecido modal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable { onDismiss() }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                    onDismiss()
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.84f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(BackgroundDark)
                .border(1.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable(enabled = false) {} // Evitar dismiss al hacer clic dentro
                .padding(28.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header superior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Anotaciones & Reseñas • Companion Móvil",
                                color = AccentGold,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${book.title} — Lector: ${activeProfile.name}",
                            color = TextMuted,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Contador de notas
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, AccentGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${notesList.size} ${if (notesList.size == 1) "nota" else "notas"}",
                            color = AntiqueIvory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Contenido principal en dos columnas (QR móvil | Lista de notas en vivo)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Columna Izquierda: QR Companion
                    Box(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainer)
                            .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (qrBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(8.dp)
                                ) {
                                    Image(
                                        bitmap = qrBitmap!!.asImageBitmap(),
                                        contentDescription = "QR para escribir notas",
                                        modifier = Modifier.size(190.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCode,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "📱 Escribe desde tu móvil",
                                color = AccentGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Escanea el código con tu teléfono para escribir cómodamente con teclado táctil.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Columna Derecha: Lista de Anotaciones en tiempo real
                    Box(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainer)
                            .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        if (notesList.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = TextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aún no hay anotaciones para este libro",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Escanea el código QR de la izquierda para guardar tu primera cita o reflexión.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(notesList, key = { it.id }) { note ->
                                    var isFocused by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isFocused) SurfaceContainerHighest else SurfaceContainerHigh)
                                            .border(
                                                width = if (isFocused) 1.5.dp else 1.dp,
                                                color = if (isFocused) AccentGold else Color(0xFF33302E),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .focusable()
                                            .onFocusChanged { isFocused = it.isFocused }
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = dateFormat.format(Date(note.createdAt)),
                                                    color = AccentGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )

                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            repository.deleteNote(note.id)
                                                            notesList = repository.getNotes(book.id, activeProfile.id)
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = "Eliminar nota",
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = note.noteText,
                                                color = AntiqueIvory,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp,
                                                fontFamily = FontFamily.Serif
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Barra inferior de botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isCloseFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .focusRequester(closeFocusRequester)
                            .onFocusChanged { isCloseFocused = it.isFocused },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCloseFocused) AccentGold else SurfaceContainerHigh,
                            contentColor = if (isCloseFocused) BackgroundDark else AntiqueIvory
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Volver a la Lectura",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
