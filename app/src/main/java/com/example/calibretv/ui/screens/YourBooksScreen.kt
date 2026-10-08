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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.calibretv.ui.components.UniversalSearchModal
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

enum class BookOriginFilter(val label: String) {
    ALL("Todos"),
    LOCAL("En Memoria TV"),
    OPDS("Servidor OPDS"),
    DRIVE("Google Drive")
}

data class YourBookEntry(
    val book: Book,
    val sizeBytes: Long,
    val origin: BookOriginFilter,
    val isDownloaded: Boolean
)

/**
 * Pantalla dedicada de «Tus Libros» (Sección 3.3 del Plan Maestro).
 * Funciona como Gestor Físico y Multifuente de Libros en el televisor:
 * - Libros descargados en la memoria física de la TV (con tamaño en MB y liberación de espacio).
 * - Libros en el Servidor OPDS (índice ligero con descarga/lectura bajo demanda lazyload).
 * - Libros en Google Drive (índice ligero con descarga/lectura bajo demanda lazyload).
 * - Paginación protegida para catálogos masivos (5.000+ libros).
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

    var selectedOriginFilter by remember { mutableStateOf(BookOriginFilter.ALL) }
    var allYourBookEntries by remember { mutableStateOf<List<YourBookEntry>>(emptyList()) }
    var totalUsedBytes by remember { mutableStateOf(0L) }
    var freeStorageBytes by remember { mutableStateOf(0L) }

    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var bookToManage by remember { mutableStateOf<YourBookEntry?>(null) }
    var isDownloadingBook by remember { mutableStateOf(false) }

    fun refreshStorageInfo() {
        val allBooks = repository.getCachedBooks()
        val entryList = mutableListOf<YourBookEntry>()
        var sumBytes = 0L

        allBooks.forEach { book ->
            val path = book.epubUrl
            var size = 0L
            var isDown = false
            if (!path.isNullOrBlank() && !path.startsWith("http://", ignoreCase = true) && !path.startsWith("https://", ignoreCase = true)) {
                val cleanPath = path.removePrefix("file://")
                val f = File(cleanPath)
                if (f.exists() && f.isFile && f.length() > 0) {
                    size = f.length()
                    sumBytes += size
                    isDown = true
                }
            }
            if (!isDown) {
                val fEpub = File(context.filesDir, "book_${book.id}.epub")
                if (fEpub.exists() && fEpub.length() > 0) {
                    size = fEpub.length()
                    sumBytes += size
                    isDown = true
                }
            }

            val origin = when {
                book.tags.any { it.contains("Drive", ignoreCase = true) } ||
                book.epubUrl?.contains("drive.google.com") == true -> BookOriginFilter.DRIVE
                isDown -> BookOriginFilter.LOCAL
                book.epubUrl?.startsWith("http", ignoreCase = true) == true -> BookOriginFilter.OPDS
                else -> BookOriginFilter.LOCAL
            }

            entryList.add(YourBookEntry(book, size, origin, isDown))
        }

        allYourBookEntries = entryList
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
    val config = remember { repository.getServerConfig() }
    val authHeader = remember(config) { com.example.calibretv.data.image.CoverLoader.buildBasicAuth(config.username, config.password) }
    var isDarkTheme by remember { mutableStateOf(prefs.isDarkTheme()) }
    val coroutineScope = rememberCoroutineScope()
    val sidebarFocusRequester = remember { FocusRequester() }

    val screenBg = if (isDarkTheme) BackgroundDark else CanvasBackgroundLight
    val cardBg = if (isDarkTheme) SurfaceCard else CardBackgroundLight
    val cardBorder = if (isDarkTheme) SurfaceRaised else Color(0xFFD6DDD6)
    val textPrimaryColor = if (isDarkTheme) TextPrimary else InkPrimary
    val textSecondaryColor = if (isDarkTheme) TextSecondary else InkSecondary
    val textMutedColor = if (isDarkTheme) TextMuted else InkMuted

    var isSearchOpen by remember { mutableStateOf(false) }
    val isAnyModalOpen = bookToManage != null || bookToDelete != null || showUserProfilesModal || isSearchOpen

    BackHandler {
        if (isSearchOpen) {
            isSearchOpen = false
        } else {
            onNavigateToHome()
        }
    }

    if (isSearchOpen) {
        UniversalSearchModal(
            repository = repository,
            isDarkTheme = isDarkTheme,
            onBookSelected = { book ->
                isSearchOpen = false
                onBookSelected(book)
            },
            onDismiss = { isSearchOpen = false }
        )
    } else {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 36.dp, top = 14.dp, bottom = 10.dp)
            ) {
                // Botón de Búsqueda Universal (D-Pad navegable)
                var isSearchBtnFocused by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSearchBtnFocused) AmberWarm
                            else if (isDarkTheme) Color.White.copy(alpha = 0.08f)
                            else Color.Black.copy(alpha = 0.05f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSearchBtnFocused) AmberWarm else (if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { isSearchOpen = true }
                        .onFocusChanged { isSearchBtnFocused = it.isFocused }
                        .focusable()
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = if (isSearchBtnFocused) Color(0xFF131316) else if (isDarkTheme) AmberWarm else InkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Buscar libros o clásicos...",
                        color = if (isSearchBtnFocused) Color(0xFF131316) else if (isDarkTheme) TextSecondary else InkSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "TUS LIBROS • MEMORIA TV",
                    color = textPrimaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            text = "${allYourBookEntries.size} libros en tu catálogo",
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

            // Pestañas Bento de Selección de Origen (Todos, Memoria TV, OPDS, Google Drive)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookOriginFilter.values().forEach { filter ->
                    val isSelected = selectedOriginFilter == filter
                    var isPillFocused by remember { mutableStateOf(false) }
                    val count = when (filter) {
                        BookOriginFilter.ALL -> allYourBookEntries.size
                        BookOriginFilter.LOCAL -> allYourBookEntries.count { it.isDownloaded }
                        BookOriginFilter.OPDS -> allYourBookEntries.count { it.origin == BookOriginFilter.OPDS }
                        BookOriginFilter.DRIVE -> allYourBookEntries.count { it.origin == BookOriginFilter.DRIVE }
                    }
                    val pillBg = when {
                        isPillFocused -> AmberWarm
                        isSelected -> if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)
                        else -> if (isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                    }
                    val pillTextColor = when {
                        isPillFocused -> Color(0xFF131316)
                        isSelected -> AmberWarm
                        else -> if (isDarkTheme) TextSecondary else InkSecondary
                    }
                    val pillBorder = when {
                        isPillFocused -> AmberWarm
                        isSelected -> AmberWarm.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(pillBg)
                            .border(1.dp, pillBorder, RoundedCornerShape(18.dp))
                            .clickable { selectedOriginFilter = filter }
                            .onFocusChanged { isPillFocused = it.isFocused }
                            .focusable()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            color = pillTextColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || isPillFocused) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            val filteredEntries = remember(allYourBookEntries, selectedOriginFilter) {
                when (selectedOriginFilter) {
                    BookOriginFilter.ALL -> allYourBookEntries
                    BookOriginFilter.LOCAL -> allYourBookEntries.filter { it.isDownloaded }
                    BookOriginFilter.OPDS -> allYourBookEntries.filter { it.origin == BookOriginFilter.OPDS }
                    BookOriginFilter.DRIVE -> allYourBookEntries.filter { it.origin == BookOriginFilter.DRIVE }
                }
            }
            var visibleLimit by remember { mutableIntStateOf(60) }
            val visibleEntries = filteredEntries.take(visibleLimit)

            // Grilla de libros o estado vacío
            if (filteredEntries.isEmpty()) {
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
                            text = when (selectedOriginFilter) {
                                BookOriginFilter.LOCAL -> "No tienes libros descargados en la memoria de la TV"
                                BookOriginFilter.OPDS -> "No hay libros del Servidor OPDS en tu catálogo"
                                BookOriginFilter.DRIVE -> "No hay libros vinculados desde Google Drive"
                                BookOriginFilter.ALL -> "No tienes libros en tu colección personal"
                            },
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
                    itemsIndexed(visibleEntries, key = { _, entry -> "${entry.book.id}_${entry.origin.name}" }) { index, entry ->
                        val isLeftEdge = index % COLUMNS_COUNT == 0
                        YourBookItemCard(
                            entry = entry,
                            authHeader = authHeader,
                            isDarkTheme = isDarkTheme,
                            isInteractive = !isAnyModalOpen,
                            isLeftEdge = isLeftEdge,
                            onLeftAtBoundary = { sidebarFocusRequester.requestFocus() },
                            onClick = { bookToManage = entry },
                            onDeleteClick = { bookToDelete = entry.book }
                        )
                    }

                    if (filteredEntries.size > visibleLimit) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                var isLoadMoreFocused by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isLoadMoreFocused) AmberWarm else if (isDarkTheme) SurfaceRaised else Color.White)
                                        .border(1.dp, if (isLoadMoreFocused) AmberWarm else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { visibleLimit += 60 }
                                        .onFocusChanged { isLoadMoreFocused = it.isFocused }
                                        .focusable()
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cargar más (+60 libros) • Mostrando ${visibleEntries.size} de ${filteredEntries.size}",
                                        color = if (isLoadMoreFocused) Color(0xFF131316) else AmberWarm,
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

        // Rail de Navegación Lateral Flotante (Overlay)
        if (!isSearchOpen) {
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
    }

    // Modal de Gestión / Lectura / Descarga Bajo Demanda
    bookToManage?.let { entry ->
        val book = entry.book
        AlertDialog(
            onDismissRequest = { if (!isDownloadingBook) bookToManage = null },
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
                Column {
                    val originDesc = when {
                        entry.isDownloaded -> "Guardado en memoria de la TV (${formatBytes(entry.sizeBytes)})"
                        entry.origin == BookOriginFilter.OPDS -> "Ubicado en Servidor OPDS personal (Descarga bajo demanda)"
                        entry.origin == BookOriginFilter.DRIVE -> "Ubicado en Google Drive (Descarga bajo demanda)"
                        else -> "En tu catálogo"
                    }
                    Text(
                        text = originDesc,
                        color = AmberWarm,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (entry.isDownloaded) {
                            "¿Qué deseas hacer con este libro en la televisión?"
                        } else {
                            "Este libro no está descargado localmente en la TV. Puedes descargarlo ahora para leerlo fluidamente en 3D."
                        },
                        color = if (isDarkTheme) TextSecondary else InkSecondary,
                        fontSize = 14.sp
                    )
                    if (isDownloadingBook) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = AmberWarm,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Descargando a memoria local...",
                                color = textPrimaryColor,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isDownloadingBook) {
                    if (entry.isDownloaded) {
                        TextButton(
                            onClick = {
                                val b = book
                                bookToManage = null
                                onBookSelected(b)
                            }
                        ) {
                            Text("📖 Leer Ahora", color = AmberWarm, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                isDownloadingBook = true
                                coroutineScope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        repository.resolveBookFile(book)
                                    }
                                    isDownloadingBook = false
                                    bookToManage = null
                                    refreshStorageInfo()
                                    if (file != null) {
                                        onBookSelected(book)
                                    }
                                }
                            }
                        ) {
                            Text("📥 Descargar y Leer", color = AmberWarm, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                if (!isDownloadingBook) {
                    if (entry.isDownloaded) {
                        TextButton(
                            onClick = {
                                val b = book
                                bookToManage = null
                                bookToDelete = b
                            }
                        ) {
                            Text("🗑️ Liberar Espacio", color = Color(0xFFEF5350))
                        }
                    } else {
                        TextButton(onClick = { bookToManage = null }) {
                            Text("Cancelar", color = if (isDarkTheme) TextSecondary else InkSecondary)
                        }
                    }
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
                    text = "¿Eliminar el archivo físico de «${book.title}» de la memoria del televisor? Seguirá en tu lista para volver a descargar cuando lo desees.",
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
                        val fEpub = File(context.filesDir, "book_${book.id}.epub")
                        if (fEpub.exists()) fEpub.delete()
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
}

@Composable
private fun YourBookItemCard(
    entry: YourBookEntry,
    authHeader: String? = null,
    isDarkTheme: Boolean = true,
    isInteractive: Boolean = true,
    isLeftEdge: Boolean = false,
    onLeftAtBoundary: () -> Unit = {},
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val book = entry.book
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(book.coverUrl, authHeader)

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

        val (badgeText, badgeColor, badgeBg) = when {
            entry.isDownloaded -> Triple("💾 ${formatBytes(entry.sizeBytes)}", Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.15f))
            entry.origin == BookOriginFilter.OPDS -> Triple("🌐 OPDS", CyanElectric, CyanElectric.copy(alpha = 0.15f))
            entry.origin == BookOriginFilter.DRIVE -> Triple("☁️ Drive", AmberWarm, AmberWarm.copy(alpha = 0.15f))
            else -> Triple("💾 Local", Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.15f))
        }

        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .background(badgeBg, RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = badgeText,
                color = badgeColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
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
