package com.example.calibretv.ui.screens

import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.model.Book
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.SurfaceCard
import com.example.calibretv.theme.SurfaceFocused
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.theme.TextSecondary
import com.example.calibretv.ui.components.Book3DView
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.components.TvTopBar
import com.example.calibretv.ui.components.UserProfilesDialog
import java.io.File
import java.util.Locale

/**
 * Pantalla dedicada de «Tus Libros» (Sección 3.3 del Plan Maestro).
 * Funciona como Gestor Físico de Memoria en el televisor:
 * - Muestra los libros descargados físicamente en la TV con su tamaño en MB.
 * - Indicador de almacenamiento libre disponible en el dispositivo.
 * - Acción rápida con control remoto para [Liberar Espacio] eliminando el archivo local sin perder el historial.
 */
@Composable
fun YourBooksScreen(
    repository: BookRepository,
    onBookSelected: (Book) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToReader: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWifiImport: () -> Unit
) {
    val context = LocalContext.current
    var activeProfile by remember { mutableStateOf(repository.getActiveProfile()) }
    var showUserProfilesModal by remember { mutableStateOf(false) }

    // Lista de libros con archivos físicos existentes en el almacenamiento de la TV
    var localBooksWithFiles by remember { mutableStateOf<List<Pair<Book, Long>>>(emptyList()) }
    var totalUsedBytes by remember { mutableStateOf(0L) }
    var freeStorageBytes by remember { mutableStateOf(0L) }

    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var bookToManage by remember { mutableStateOf<Book?>(null) }

    fun refreshStorageInfo() {
        val allBooks = repository.getCachedBooks()
        val localList = mutableListOf<Pair<Book, Long>>()
        var sumBytes = 0L

        allBooks.forEach { book ->
            val path = book.epubUrl
            if (!path.isNullOrBlank()) {
                val cleanPath = path.removePrefix("file://")
                val f = File(cleanPath)
                if (f.exists() && f.isFile) {
                    val size = f.length()
                    localList.add(Pair(book, size))
                    sumBytes += size
                }
            }
        }

        localBooksWithFiles = localList
        totalUsedBytes = sumBytes

        try {
            val stat = StatFs(context.filesDir.absolutePath)
            freeStorageBytes = stat.availableBytes
        } catch (_: Exception) {
            freeStorageBytes = 0L
        }
    }

    LaunchedEffect(Unit) {
        refreshStorageInfo()
    }

    BackHandler {
        onNavigateToHome()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // TopBar cinemática de 5 pestañas
        TvTopBar(
            currentTab = TvNavTab.TUS_LIBROS,
            activeProfile = activeProfile,
            onTabSelected = { tab ->
                when (tab) {
                    TvNavTab.HOME -> onNavigateToHome()
                    TvNavTab.BIBLIOTECA -> onNavigateToLibrary()
                    TvNavTab.TUS_LIBROS -> {}
                    TvNavTab.LECTOR_3D -> onNavigateToReader()
                    TvNavTab.AJUSTES -> onNavigateToSettings()
                }
            },
            onProfileClick = { showUserProfilesModal = true }
        )

        // Panel de información de memoria física en TV (10-Foot UI)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 12.dp)
                .background(SurfaceCard, RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceRaised, RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AmberWarm.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Almacenamiento",
                        tint = AmberWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Gestor Físico de Memoria TV",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${localBooksWithFiles.size} libros descargados en la memoria interna",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Espacio ocupado por libros",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatBytes(totalUsedBytes),
                        color = AmberWarm,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Espacio libre en TV",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatBytes(freeStorageBytes),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Grilla de libros locales o estado vacío
        if (localBooksWithFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No tienes libros descargados en este televisor",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Transfiere archivos EPUB/PDF desde tu teléfono o descarga clásicos desde la Biblioteca.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        YourBooksActionButton(
                            title = "Transferir por Wi-Fi",
                            icon = Icons.Default.Wifi,
                            onClick = onNavigateToWifiImport
                        )
                        YourBooksActionButton(
                            title = "Explorar Biblioteca",
                            icon = Icons.Default.MenuBook,
                            onClick = onNavigateToLibrary
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(localBooksWithFiles, key = { it.first.id }) { (book, sizeBytes) ->
                    YourBookItemCard(
                        book = book,
                        sizeBytes = sizeBytes,
                        onClick = { bookToManage = book },
                        onDeleteClick = { bookToDelete = book }
                    )
                }
            }
        }
    }

    // Modal de Gestión / Lectura
    bookToManage?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToManage = null },
            title = {
                Text(
                    text = book.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Text(
                    text = "¿Qué deseas hacer con este libro en la televisión?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val b = book
                        bookToManage = null
                        onBookSelected(b)
                    }
                ) {
                    Text("📖 Leer Ahora", color = AmberWarm, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val b = book
                        bookToManage = null
                        bookToDelete = b
                    }
                ) {
                    Text("🗑️ Liberar Espacio", color = Color(0xFFEF5350))
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal de Confirmación de Borrado de archivo físico
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = {
                Text(
                    text = "Liberar espacio en la TV",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Eliminar el archivo físico de «${book.title}» de la memoria del televisor? Tu progreso y notas seguirán guardados.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val path = book.epubUrl?.removePrefix("file://")
                        if (!path.isNullOrBlank()) {
                            val f = File(path)
                            if (f.exists()) f.delete()
                        }
                        bookToDelete = null
                        refreshStorageInfo()
                    }
                ) {
                    Text("Eliminar Archivo", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showUserProfilesModal) {
        UserProfilesDialog(
            repository = repository,
            activeProfile = activeProfile,
            onProfileChanged = { newProfile ->
                activeProfile = newProfile
                showUserProfilesModal = false
            },
            onDismiss = { showUserProfilesModal = false }
        )
    }
}

@Composable
private fun YourBookItemCard(
    book: Book,
    sizeBytes: Long,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(book.coverUrl, null)

    Column(
        modifier = Modifier
            .width(122.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Book3DView(
            coverBitmap = coverBmp,
            title = book.title,
            width = 96.dp,
            height = 142.dp,
            isFocused = isFocused
        )

        Spacer(Modifier.height(4.dp))
        Text(
            text = book.title,
            color = if (isFocused) AmberWarm else TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = book.author.ifBlank { "Memoria TV" },
            color = TextSecondary,
            fontSize = 9.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun YourBooksActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .background(if (isFocused) AmberWarm else SurfaceCard, RoundedCornerShape(8.dp))
            .border(1.dp, if (isFocused) AmberWarm else SurfaceRaised, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.Black else AmberWarm,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = if (isFocused) Color.Black else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
