package com.example.calibretv.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.calibretv.ui.components.TvProfilePill
import com.example.calibretv.ui.components.UniversalSearchModal
import com.example.calibretv.ui.components.UserProfilesDialog
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.curator.CuratedBook
import com.example.calibretv.data.curator.CuratorRepository
import com.example.calibretv.data.image.CoverLoader
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.opds.OpdsClient
import com.example.calibretv.theme.AmberWarm
import androidx.compose.ui.platform.LocalContext
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CalibreTVTheme
import com.example.calibretv.theme.CanvasBackgroundLight
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.InkSecondary
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceContainerHighest
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.theme.TextSecondary
import com.example.calibretv.ui.components.Book3DView
import com.example.calibretv.ui.components.CuratedBookModal
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.components.TvSidebar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun CuratedBook.toBook(): Book = Book(
    id = this.id,
    title = this.title,
    author = this.author,
    coverUrl = this.coverUrl,
    summary = this.summary,
    category = this.category,
    shelves = listOf(this.category, "Nivel ${this.difficultyLevel}"),
    tags = listOf(this.category, "Nivel ${this.difficultyLevel}"),
    epubUrl = if (this.isPublicDomain) this.publicDownloadUrl else null
)

fun Book.toCuratedBook(): CuratedBook {
    val curated = CuratorRepository.findCuratedBook(this.id)
        ?: CuratorRepository.getAllCuratedBooks().find { it.title.equals(this.title, ignoreCase = true) }
    if (curated != null) return curated

    val isPublic = !this.epubUrl.isNullOrBlank()
    return CuratedBook(
        id = this.id,
        title = this.title,
        author = this.author,
        coverUrl = this.coverUrl ?: "",
        summary = this.summary,
        category = this.category.ifBlank { "Literatura" },
        isPublicDomain = isPublic,
        affiliateQrUrl = null,
        publicDownloadUrl = this.epubUrl,
        approximatePrice = if (isPublic) "Gratis" else "Consultar",
        rating = 4.8f,
        difficultyLevel = getBookDifficultyLevel(this)
    )
}

enum class CircleShelfType {
    ALL,
    LEVEL,
    CHARACTER,
    TAG
}

data class CircleShelfFilter(
    val id: String,
    val title: String,
    val type: CircleShelfType,
    val levelNumber: Int? = null,
    val coverUrl: String? = null,
    val bookCount: Int = 0
)

private val LEVEL_REGEX_1 = Regex("""(?i)(\d+)\s*(?:nivel|level|grado|grade|l)?""")
private val LEVEL_REGEX_2 = Regex("""(?i)(?:nivel|level|grado|grade|l)?\s*(\d+)""")
private val LEVEL_SHELF_REGEX = Regex("""^\d+\s*nivel.*""", RegexOption.IGNORE_CASE)
private val LEVEL_NUM_REGEX = Regex("""^[1-5]$""")

fun getBookDifficultyLevel(book: Book): Int {
    val allTokens = book.shelves + book.tags + listOf(book.category)
    for (token in allTokens) {
        val trimmed = token.trim()
        if (trimmed in listOf("1", "2", "3", "4", "5")) {
            return trimmed.toInt()
        }
        LEVEL_REGEX_1.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()?.let { if (it in 1..5) return it }
        LEVEL_REGEX_2.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()?.let { if (it in 1..5) return it }
    }
    return 99
}

fun isEnglishBook(b: Book): Boolean {
    val text = (b.shelves + b.tags + listOf(b.category, b.title, b.author)).joinToString(" ").lowercase()
    if (text.contains("ingl") || text.contains("english") || text.contains("ingles") || text.contains("en_us") || text.contains("en_gb")) {
        return true
    }
    val titleLower = b.title.lowercase().trim()
    val englishKeywords = listOf(
        "a girl from yamhill", "a separate peace", "dead men do tell tales", "babe",
        "the ", "a ", "of ", "in ", "and ", "girl", "peace", "tales", "world", "stories", "history", "book"
    )
    if (englishKeywords.any { titleLower.startsWith(it) || titleLower.contains(" $it ") || titleLower.equals(it) }) {
        if (!titleLower.contains("el ") && !titleLower.contains("la ") && !titleLower.contains("los ") && !titleLower.contains("las ") && !titleLower.contains("de ") && !titleLower.contains("del ") && !titleLower.contains("en el ") && !titleLower.contains("en la ")) {
            return true
        }
    }
    return false
}

@Composable
fun LibraryGridScreen(
    repository: BookRepository,
    onBookSelected: (Book) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOpds: () -> Unit,
    onNavigateToReader: () -> Unit,
    onNavigateToWifiImport: () -> Unit = {},
    onNavigateToYourBooks: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { com.example.calibretv.data.storage.PreferencesManager(context) }
    var isDarkTheme by remember { mutableStateOf(prefsManager.isDarkTheme()) }

    var curatedSections by remember { mutableStateOf(CuratorRepository.getCuratedSections()) }
    var isLoading by remember { mutableStateOf(curatedSections.isEmpty()) }
    var selectedFilterId by remember { mutableStateOf("all") }
    var activeProfile by remember { mutableStateOf(repository.getActiveProfile()) }

    val sidebarFocusRequester = remember { FocusRequester() }
    var showUserProfilesModal by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Modal state for Curated Book Details (Cloud inspiration showcase)
    var selectedCuratedBook by remember { mutableStateOf<CuratedBook?>(null) }
    var isDownloadingCuratedBook by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    val bookFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    BackHandler {
        if (isSearchOpen) {
            isSearchOpen = false
        } else if (selectedCuratedBook != null) {
            selectedCuratedBook = null
        } else if (showUserProfilesModal) {
            showUserProfilesModal = false
        } else {
            onBack()
        }
    }

    val config = remember { repository.getServerConfig() }
    val authHeader = remember(config) { CoverLoader.buildBasicAuth(config.username, config.password) }

    // Sincronizar catálogo curado con Cloud Firestore en segundo plano sin bloquear la UI
    LaunchedEffect(activeProfile) {
        if (curatedSections.isEmpty()) {
            isLoading = true
        }
        try {
            withContext(Dispatchers.IO) {
                val synced = CuratorRepository.syncWithCms()
                if (synced) {
                    curatedSections = CuratorRepository.getCuratedSections()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LibraryGridScreen", "Error sincronizando con Firebase/CMS: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Escaparate de Inspiración: Obras curadas de la nube (Cloud Firestore) y catálogo local integrado
    val allBooks = remember(curatedSections) {
        val fromSections = curatedSections.flatMap { it.books }
        val allCurated = CuratorRepository.getAllCuratedBooks()
        (fromSections + allCurated).distinctBy { it.id }.map { it.toBook() }
    }

    // Circular Shelf Filters (Sección A: Estanterías de Personajes y Dificultad)
    val circleFilters = remember(allBooks, curatedSections) {
        val list = mutableListOf<CircleShelfFilter>()
        // 1. Todos los libros
        list.add(
            CircleShelfFilter(
                id = "all",
                title = "Todos",
                type = CircleShelfType.ALL,
                bookCount = allBooks.size
            )
        )

        // 2. Shelves de Personajes desde Cloud Firestore (cartelera_shelves) y curaduría
        curatedSections.forEach { section ->
            val matchingCount = allBooks.count { b ->
                b.shelves.any { it.equals(section.name, ignoreCase = true) } ||
                b.category.equals(section.name, ignoreCase = true) ||
                section.books.any { it.id == b.id }
            }
            list.add(
                CircleShelfFilter(
                    id = section.name,
                    title = section.name,
                    type = CircleShelfType.CHARACTER,
                    coverUrl = section.avatarUrl ?: section.books.firstOrNull()?.coverUrl,
                    bookCount = matchingCount
                )
            )
        }

        // 3. Niveles del 1 al 5
        for (lvl in 1..5) {
            val count = allBooks.count { getBookDifficultyLevel(it) == lvl }
            list.add(
                CircleShelfFilter(
                    id = "level_$lvl",
                    title = "Nivel $lvl",
                    type = CircleShelfType.LEVEL,
                    levelNumber = lvl,
                    bookCount = count
                )
            )
        }

        // 4. Estantería Inglés
        val englishBooks = allBooks.filter { isEnglishBook(it) }
        if (englishBooks.isNotEmpty()) {
            list.add(
                CircleShelfFilter(
                    id = "ingles",
                    title = "Inglés",
                    type = CircleShelfType.TAG,
                    coverUrl = englishBooks.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl,
                    bookCount = englishBooks.size
                )
            )
        }

        list
    }

    val filteredBooks = remember(allBooks, selectedFilterId, curatedSections) {
        val selected = circleFilters.firstOrNull { it.id == selectedFilterId } ?: circleFilters.first()
        when (selected.type) {
            CircleShelfType.ALL -> allBooks
            CircleShelfType.LEVEL -> allBooks.filter { getBookDifficultyLevel(it) == selected.levelNumber }
            CircleShelfType.CHARACTER, CircleShelfType.TAG -> {
                val sec = curatedSections.firstOrNull { it.name.equals(selected.id, ignoreCase = true) }
                val matches = if (selected.id == "ingles") {
                    allBooks.filter { isEnglishBook(it) }
                } else if (sec != null) {
                    allBooks.filter { b ->
                        sec.books.any { it.id == b.id } ||
                        b.shelves.any { it.equals(sec.name, ignoreCase = true) } ||
                        b.category.equals(sec.name, ignoreCase = true)
                    }
                } else {
                    allBooks.filter { b ->
                        b.shelves.any { it.equals(selected.id, ignoreCase = true) } ||
                        b.tags.any { it.equals(selected.id, ignoreCase = true) } ||
                        b.category.equals(selected.id, ignoreCase = true)
                    }
                }
                // Sort books from level 1 to 5, then alphabetically
                matches.sortedWith(compareBy({ getBookDifficultyLevel(it) }, { it.title }))
            }
        }
    }

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentTime = sdf.format(Date())
    }

    val COLUMNS = 7

    val screenBg = if (isDarkTheme) BackgroundDark else CanvasBackgroundLight
    val isAnyModalOpen = selectedCuratedBook != null || showUserProfilesModal || isSearchOpen

    CalibreTVTheme(isDarkTheme = isDarkTheme) {
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
                Column(modifier = Modifier.fillMaxSize().background(screenBg)) {
                    // Header Bar (Buscador a la izquierda, título centrado, reloj y perfil a la derecha)
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
                            text = "ESTANTERÍA DE LIBROS",
                            color = if (isDarkTheme) TextPrimary else InkPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Clock and profile
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = currentTime,
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else InkPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            TvProfilePill(
                                profile = activeProfile,
                                isDarkTheme = isDarkTheme,
                                onOpenProfileSwitcher = { showUserProfilesModal = true },
                                onOpenOpds = onNavigateToOpds,
                                onOpenWifiImport = onNavigateToWifiImport,
                                onOpenSettings = onNavigateToSettings,
                                onQuickSync = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        try {
                                            withContext(Dispatchers.IO) {
                                                val synced = CuratorRepository.syncWithCms()
                                                if (synced) {
                                                    curatedSections = CuratorRepository.getCuratedSections()
                                                }
                                            }
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                onNotificationsClick = onNavigateToSettings
                            )
                        }
                    }

                    // Section A: Netflix Kids Circular Shelves & Difficulty Badges (Contenedor Bento con estilo del proyecto)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 6.dp)
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
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(circleFilters) { index, filter ->
                                val isFirst = index == 0
                                NetflixShelfCircleItem(
                                    filter = filter,
                                    isSelected = selectedFilterId == filter.id,
                                    authHeader = authHeader,
                                    isFirst = isFirst,
                                    isDarkTheme = isDarkTheme,
                                    isInteractive = !isAnyModalOpen,
                                    onLeftAtBoundary = { sidebarFocusRequester.requestFocus() },
                                    onClick = { selectedFilterId = filter.id }
                                )
                            }
                        }
                    }

                // Books Grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                ) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = if (isDarkTheme) CyanElectric else AmberWarm)
                        }
                    } else if (filteredBooks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text("No hay libros en esta categoría", color = if (isDarkTheme) TextMuted else InkSecondary, fontSize = 14.sp)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(COLUMNS),
                            contentPadding = PaddingValues(bottom = 60.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(filteredBooks) { index, book ->
                                val isLeftEdge = index % COLUMNS == 0
                                GridCoverCard(
                                    book = book,
                                    authHeader = authHeader,
                                    isInteractive = !isAnyModalOpen,
                                    isLeftEdge = isLeftEdge,
                                    isDarkTheme = isDarkTheme,
                                    modifier = Modifier.focusRequester(bookFocusRequesters.getOrPut(book.id) { FocusRequester() }),
                                    onLeftAtBoundary = { sidebarFocusRequester.requestFocus() },
                                    onSelected = {
                                        selectedCuratedBook = book.toCuratedBook()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal de Gestión de Perfiles
        if (showUserProfilesModal) {
            UserProfilesDialog(
                repository = repository,
                activeProfile = activeProfile,
                onProfileChanged = { newProfile ->
                    activeProfile = newProfile
                },
                onDismiss = { showUserProfilesModal = false }
            )
        }

        // Modal de Ficha Curada (Descarga, Compra QR o Lectura directa en 3D)
        selectedCuratedBook?.let { curatedBook ->
            val cachedBooks = repository.getCachedBooks()
            val matchedLocalBook = remember(curatedBook.id) {
                CuratorRepository.findMatchingLocalBook(curatedBook, cachedBooks)
            }
            var isCuratedDownloaded by remember(curatedBook.id) {
                mutableStateOf(
                    matchedLocalBook != null ||
                    CuratorRepository.isBookDownloadedOrAvailable(curatedBook, repository)
                )
            }

            CuratedBookModal(
                book = curatedBook,
                isDownloaded = isCuratedDownloaded,
                isDownloading = isDownloadingCuratedBook,
                isDarkTheme = isDarkTheme,
                onDownload = {
                    if (matchedLocalBook != null) {
                        selectedCuratedBook = null
                        onBookSelected(matchedLocalBook)
                    } else {
                        isDownloadingCuratedBook = true
                        coroutineScope.launch {
                            val res = CuratorRepository.downloadPublicDomainBook(context, curatedBook, repository)
                            if (res.isSuccess) {
                                isCuratedDownloaded = true
                                android.widget.Toast.makeText(context, "¡Descarga completada! Ya puedes abrir el libro.", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Error al descargar: ${res.exceptionOrNull()?.message ?: "Comprueba tu conexión"}", android.widget.Toast.LENGTH_LONG).show()
                            }
                            isDownloadingCuratedBook = false
                        }
                    }
                },
                onRead = {
                    val localBook = matchedLocalBook ?: repository.getCachedBooks().find { 
                        it.id == curatedBook.id || it.title.equals(curatedBook.title, ignoreCase = true)
                    }
                    selectedCuratedBook = null
                    if (localBook != null) {
                        onBookSelected(localBook)
                    } else {
                        onBookSelected(curatedBook.toBook())
                    }
                },
                onDismiss = {
                    selectedCuratedBook = null
                }
            )
        }

        // Rail de Navegación Lateral Flotante (Overlay)
        TvSidebar(
            modifier = Modifier.align(Alignment.CenterStart),
            currentTab = TvNavTab.BIBLIOTECA,
            isDarkTheme = isDarkTheme,
            onToggleTheme = {
                val newTheme = !isDarkTheme
                isDarkTheme = newTheme
                prefsManager.setDarkTheme(newTheme)
            },
            focusRequester = sidebarFocusRequester,
            onTabSelected = { tab ->
                when (tab) {
                    TvNavTab.HOME -> onNavigateToHome()
                    TvNavTab.BIBLIOTECA -> {}
                    TvNavTab.TUS_LIBROS -> onNavigateToYourBooks()
                    TvNavTab.LECTOR_3D -> onNavigateToReader()
                    TvNavTab.AJUSTES -> onNavigateToSettings()
                }
            }
        )
    }
}
}
}

@Composable
private fun GridCoverCard(
    book: Book,
    authHeader: String?,
    isInteractive: Boolean = true,
    isLeftEdge: Boolean = false,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier,
    onLeftAtBoundary: () -> Unit,
    onSelected: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(book.coverUrl, authHeader)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (isInteractive) {
                    isFocused = it.isFocused
                }
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (isLeftEdge) {
                                onLeftAtBoundary()
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onSelected()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(enabled = isInteractive)
            .clickable(enabled = isInteractive) { onSelected() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Book3DView(
            coverBitmap = coverBmp,
            title = book.title,
            width = 96.dp,
            height = 142.dp,
            isFocused = isFocused
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Title strictly legible in both themes, without author or progress bar underneath
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = book.title,
                color = if (isFocused) AmberWarm else (if (isDarkTheme) TextPrimary else InkPrimary),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GridCapsuleChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isFirst: Boolean = false,
    onLeftAtBoundary: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isFocused -> AmberWarm
                    isSelected -> CyanElectric.copy(alpha = 0.20f)
                    else -> SurfaceContainerHigh
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> AmberWarm
                    isSelected -> CyanElectric
                    else -> Color(0xFF2E2E34)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (isFirst) {
                                onLeftAtBoundary()
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                isFocused -> Color(0xFF131315)
                isSelected -> CyanElectric
                else -> TextMuted
            },
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = title,
            color = when {
                isFocused -> Color(0xFF131315)
                isSelected -> CyanElectric
                else -> TextPrimary
            },
            fontSize = 11.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun GridActionCapsule(
    title: String,
    icon: ImageVector,
    isPrimary: Boolean,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val unfocusedBg = if (isDarkTheme) {
        if (isPrimary) Color(0xFF423419) else SurfaceContainerHighest
    } else {
        if (isPrimary) Color(0xFF111317) else Color(0xFFE5E0D8)
    }

    val unfocusedTextColor = if (isDarkTheme) {
        if (isPrimary) AmberWarm else TextPrimary
    } else {
        if (isPrimary) Color.White else InkPrimary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .scale(if (isFocused) 1.06f else 1.0f)
            .shadow(if (isFocused) 10.dp else 2.dp, RoundedCornerShape(20.dp), spotColor = AmberWarm)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isFocused) AmberWarm else unfocusedBg)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onClick()
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            // Prevenir fuga de foco fuera de los botones del modal hacia el grid de fondo
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color(0xFF131315) else unfocusedTextColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = if (isFocused) Color(0xFF131315) else unfocusedTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NetflixShelfCircleItem(
    filter: CircleShelfFilter,
    isSelected: Boolean,
    authHeader: String?,
    isFirst: Boolean = false,
    isDarkTheme: Boolean = true,
    isInteractive: Boolean = true,
    onLeftAtBoundary: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = if (filter.type == CircleShelfType.CHARACTER && !filter.coverUrl.isNullOrBlank()) {
        rememberCoverImage(filter.coverUrl, authHeader)
    } else null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(82.dp)
            .scale(if (isFocused) 1.14f else if (isSelected) 1.05f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (isFirst) {
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
            .clickable(enabled = isInteractive) { onClick() }
    ) {
        // Circle Avatar
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = if (isFocused) 12.dp else if (isSelected) 6.dp else 2.dp,
                    shape = CircleShape,
                    spotColor = if (isFocused) CyanElectric else AmberWarm
                )
                .clip(CircleShape)
                .background(
                    when (filter.type) {
                        CircleShelfType.ALL -> Brush.radialGradient(listOf(Color(0xFF2E2E38), Color(0xFF18181C)))
                        CircleShelfType.LEVEL -> when (filter.levelNumber) {
                            1 -> Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF1B5E20)))
                            2 -> Brush.linearGradient(listOf(Color(0xFF00B0FF), Color(0xFF01579B)))
                            3 -> Brush.linearGradient(listOf(Color(0xFFFFAB00), Color(0xFFFF6D00)))
                            4 -> Brush.linearGradient(listOf(Color(0xFFFF3D00), Color(0xFFBF360C)))
                            5 -> Brush.linearGradient(listOf(Color(0xFFE040FB), Color(0xFF4A148C)))
                            else -> Brush.radialGradient(listOf(Color(0xFF2E2E38), Color(0xFF18181C)))
                        }
                        CircleShelfType.CHARACTER -> Brush.radialGradient(listOf(Color(0xFF32323E), Color(0xFF1A1A22)))
                        CircleShelfType.TAG -> Brush.radialGradient(listOf(Color(0xFF2A2A34), Color(0xFF16161A)))
                    }
                )
                .border(
                    width = if (isFocused) 3.dp else if (isSelected) 2.5.dp else 1.5.dp,
                    color = when {
                        isFocused -> Color.White
                        isSelected -> CyanElectric
                        else -> Color(0xFF383842)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (filter.type) {
                CircleShelfType.ALL -> {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Todos",
                        tint = if (isFocused) Color.White else AmberWarm,
                        modifier = Modifier.size(28.dp)
                    )
                }
                CircleShelfType.LEVEL -> {
                    Text(
                        text = "${filter.levelNumber}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                CircleShelfType.CHARACTER -> {
                    if (coverBmp != null) {
                        Image(
                            bitmap = coverBmp,
                            contentDescription = filter.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = filter.title.take(2).uppercase(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                CircleShelfType.TAG -> {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = filter.title,
                        tint = if (isFocused) Color.White else CyanElectric,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val labelColor = when {
            isFocused -> if (isDarkTheme) Color.White else InkPrimary
            isSelected -> CyanElectric
            else -> if (isDarkTheme) TextPrimary.copy(alpha = 0.85f) else InkSecondary
        }

        Text(
            text = if (filter.type == CircleShelfType.LEVEL) "Nivel ${filter.levelNumber}" else filter.title,
            color = labelColor,
            fontSize = 10.sp,
            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

