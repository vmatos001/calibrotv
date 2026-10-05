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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.example.calibretv.data.storage.PreferencesManager
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.model.Book
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CanvasBackgroundLight
import com.example.calibretv.theme.CardBackgroundLight
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.InkMuted
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.InkSecondary
import com.example.calibretv.theme.SurfaceCard
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceFocused
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.theme.TextSecondary
import com.example.calibretv.ui.components.Book3DView
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.components.TvProfilePill
import com.example.calibretv.ui.components.TvSidebar
import com.example.calibretv.ui.components.UserProfilesDialog
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            var size = 0L
            if (!path.isNullOrBlank()) {
                val cleanPath = path.removePrefix("file://")
                val f = File(cleanPath)
                if (f.exists() && f.isFile) {
                    size = f.length()
                    sumBytes += size
                }
            }
            localList.add(Pair(book, size))
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
        withContext(Dispatchers.IO) {
            try {
                val feed = repository.getFeed()
                if (feed.books.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        refreshStorageInfo()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val prefs = remember { PreferencesManager(context) }
    var isDarkTheme by remember { mutableStateOf(prefs.isDarkTheme()) }
    val coroutineScope = rememberCoroutineScope()
    val sidebarFocusRequester = remember { FocusRequester() }

    val screenBg = if (isDarkTheme) BackgroundDark else CanvasBackgroundLight
    val cardBg = if (isDarkTheme) SurfaceCard else CardBackgroundLight
    val cardBorder = if (isDarkTheme) SurfaceRaised else Color(0xFFD6DDD6)
    val textPrimaryColor = if (isDarkTheme) TextPrimary else InkPrimary
    val textSecondaryColor = if (isDarkTheme) TextSecondary else InkSecondary
    val textMutedColor = if (isDarkTheme) TextMuted else InkMuted

    val isAnyModalOpen = bookToManage != null || bookToDelete != null || showUserProfilesModal

    BackHandler {
        onNavigateToHome()
    }

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
            ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 36.dp, top = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TUS LIBROS • MEMORIA TV",
                    color = textPrimaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                TvProfilePill(
                    profile = activeProfile,
                    isDarkTheme = isDarkTheme,
                    onOpenProfileSwitcher = { showUserProfilesModal = true },
                    onOpenOpds = {},
                    onOpenWifiImport = onNavigateToWifiImport,
                    onOpenSettings = onNavigateToSettings,
                    onQuickSync = { refreshStorageInfo() },
                    onNotificationsClick = onNavigateToSettings
                )
            }

            // Panel de información de memoria física en TV (Bento Style, limpio sin bordes duros)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
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
                    .padding(horizontal = 22.dp, vertical = 14.dp),
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
                            color = textPrimaryColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${localBooksWithFiles.size} libros en tu colección personal",
                            color = textSecondaryColor,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Espacio ocupado por libros",
                            color = textMutedColor,
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
                            color = textMutedColor,
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatBytes(freeStorageBytes),
                            color = textPrimaryColor,
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
                            tint = textMutedColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No tienes libros sincronizados ni descargados en este televisor",
                            color = textPrimaryColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Transfiere archivos EPUB/PDF desde tu teléfono o descarga clásicos desde la Biblioteca.",
                            color = textSecondaryColor,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            YourBooksActionButton(
                                title = "Transferir por Wi-Fi",
                                icon = Icons.Default.Wifi,
                                isDarkTheme = isDarkTheme,
                                onClick = onNavigateToWifiImport
                            )
                            YourBooksActionButton(
                                title = "Explorar Biblioteca",
                                icon = Icons.Default.MenuBook,
                                isDarkTheme = isDarkTheme,
                                onClick = onNavigateToLibrary
                            )
                        }
                    }
                }
            } else {
                val COLUMNS_COUNT = 4
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(localBooksWithFiles, key = { _, pair -> pair.first.id }) { index, (book, sizeBytes) ->
                        val isLeftEdge = index % COLUMNS_COUNT == 0
                        YourBookItemCard(
                            book = book,
                            sizeBytes = sizeBytes,
                            isDarkTheme = isDarkTheme,
                            isInteractive = !isAnyModalOpen,
                            isLeftEdge = isLeftEdge,
                            onLeftAtBoundary = { sidebarFocusRequester.requestFocus() },
                            onClick = { bookToManage = book },
                            onDeleteClick = { bookToDelete = book }
                        )
                    }
                }
            }
        }
    }

        // Rail de Navegación Lateral Flotante (Overlay)
        TvSidebar(
            modifier = Modifier.align(Alignment.CenterStart),
            currentTab = TvNavTab.TUS_LIBROS,
            onTabSelected = { tab ->
                when (tab) {
                    TvNavTab.HOME -> onNavigateToHome()
                    TvNavTab.BIBLIOTECA -> onNavigateToLibrary()
                    TvNavTab.TUS_LIBROS -> {}
                    TvNavTab.LECTOR_3D -> onNavigateToReader()
                    TvNavTab.AJUSTES -> onNavigateToSettings()
                }
            },
            isDarkTheme = isDarkTheme,
            onToggleTheme = {
                val newTheme = !isDarkTheme
                isDarkTheme = newTheme
                coroutineScope.launch { prefs.setDarkTheme(newTheme) }
            },
            focusRequester = sidebarFocusRequester
        )
    }

    // Modal de Gestión / Lectura
    bookToManage?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToManage = null },
            title = {
                Text(
                    text = book.title,
                    color = if (isDarkTheme) TextPrimary else InkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Text(
                    text = "¿Qué deseas hacer con este libro en la televisión?",
                    color = if (isDarkTheme) TextSecondary else InkSecondary,
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
            containerColor = if (isDarkTheme) SurfaceCard else Color(0xFFF7F5F0),
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
                    color = if (isDarkTheme) TextPrimary else InkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Eliminar el archivo físico de «${book.title}» de la memoria del televisor? Tu progreso y notas seguirán guardados.",
                    color = if (isDarkTheme) TextSecondary else InkSecondary,
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
                    Text("Cancelar", color = if (isDarkTheme) TextSecondary else InkSecondary)
                }
            },
            containerColor = if (isDarkTheme) SurfaceCard else Color(0xFFF7F5F0),
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
    isDarkTheme: Boolean = true,
    isInteractive: Boolean = true,
    isLeftEdge: Boolean = false,
    onLeftAtBoundary: () -> Unit = {},
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(book.coverUrl, null)

    Column(
        modifier = Modifier
            .width(122.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (isLeftEdge) {
                                onLeftAtBoundary()
                                true
                            } else false
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(enabled = isInteractive)
            .clickable(enabled = isInteractive) { onClick() },
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
            color = if (isFocused) AmberWarm else (if (isDarkTheme) TextPrimary else InkPrimary),
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
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
    isDarkTheme: Boolean = true,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val unfocusedBg = if (isDarkTheme) SurfaceCard else Color(0xFFF1F5F1)
    val unfocusedTextColor = if (isDarkTheme) TextPrimary else InkPrimary

    Row(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .background(if (isFocused) AmberWarm else unfocusedBg, RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) (if (isDarkTheme) Color.White else CyanElectric) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color(0xFF111317) else AmberWarm,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = if (isFocused) Color(0xFF111317) else unfocusedTextColor,
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
