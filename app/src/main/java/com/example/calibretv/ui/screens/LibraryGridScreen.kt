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
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.calibretv.ui.components.TvProfilePill
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
import com.example.calibretv.data.image.CoverLoader
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.opds.OpdsClient
import com.example.calibretv.data.opds.OpdsFeedContent
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
import com.example.calibretv.ui.components.Book3DView
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.components.TvSidebar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

fun getBookDifficultyLevel(book: Book): Int {
    val levelRegex = Regex("""(?i)(\d+)\s*(?:nivel|level|grado|grade|l)?""")
    val levelRegex2 = Regex("""(?i)(?:nivel|level|grado|grade|l)?\s*(\d+)""")

    val allTokens = book.shelves + book.tags + listOf(book.category)
    for (token in allTokens) {
        val trimmed = token.trim()
        if (trimmed in listOf("1", "2", "3", "4", "5")) {
            return trimmed.toInt()
        }
        levelRegex.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()?.let { if (it in 1..5) return it }
        levelRegex2.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()?.let { if (it in 1..5) return it }
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

    var feedContent by remember { mutableStateOf<OpdsFeedContent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilterId by remember { mutableStateOf("all") }
    var activeProfile by remember { mutableStateOf(repository.getActiveProfile()) }

    val sidebarFocusRequester = remember { FocusRequester() }
    var showUserProfilesModal by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Modal state for Book Details
    var showDetailsModal by remember { mutableStateOf(false) }
    var detailsBook by remember { mutableStateOf<Book?>(null) }
    var modalDescription by remember { mutableStateOf("") }
    val modalReadFocusRequester = remember { FocusRequester() }
    val bookFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    BackHandler {
        if (showDetailsModal) {
            showDetailsModal = false
            detailsBook = null
        }
        else if (showUserProfilesModal) showUserProfilesModal = false
        else onBack()
    }

    LaunchedEffect(showDetailsModal) {
        if (showDetailsModal) {
            modalReadFocusRequester.requestFocus()
        } else if (detailsBook != null) {
            val lastId = detailsBook?.id
            if (lastId != null) {
                kotlinx.coroutines.delay(80L)
                try {
                    bookFocusRequesters[lastId]?.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(detailsBook) {
        detailsBook?.let { b ->
            modalDescription = "Cargando sinopsis..."
            modalDescription = repository.getOrFetchBookDescription(b)
        }
    }

    val config = remember { repository.getServerConfig() }
    val authHeader = remember(config) { CoverLoader.buildBasicAuth(config.username, config.password) }

    // Load full catalog and enrich shelves
    LaunchedEffect(activeProfile) {
        isLoading = true
        val result = repository.getFeed()
        feedContent = result
        if (config.serverUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    repository.loadAndApplyShelves(config)
                    val updated = repository.getCachedBooks()
                    if (updated.isNotEmpty()) {
                        feedContent = feedContent?.copy(books = updated)
                    }
                } catch (_: Exception) {}
            }
        }
        isLoading = false
    }

    val allBooks = feedContent?.books ?: emptyList()

    // Circular Shelf Filters (Section A: Netflix Kids style)
    val circleFilters = remember(allBooks) {
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

        // 2. Shelves de Personajes de TV (desde Calibre-Web, ubicados entre TODOS y NIVEL 1)
        val cachedShelves = repository.getShelves()
        val shelvesFromBooks = allBooks.flatMap { it.shelves }.distinct()
            .filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.matches(Regex("""^\d+\s*nivel.*""", RegexOption.IGNORE_CASE)) && !it.contains("ingl", ignoreCase = true) && !it.matches(Regex("""^[1-5]$""")) }

        val charShelves = shelvesFromBooks.filter { OpdsClient.isCharacterShelfName(it) }.sorted()

        charShelves.forEach { shelfName ->
            val matchingBooks = allBooks.filter { b ->
                b.shelves.any { it.equals(shelfName, ignoreCase = true) }
            }
            val shelfObj = cachedShelves.firstOrNull { it.name.equals(shelfName, ignoreCase = true) }
            val coverUrl = shelfObj?.imageUrl ?: matchingBooks.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl
            list.add(
                CircleShelfFilter(
                    id = shelfName,
                    title = shelfName,
                    type = CircleShelfType.CHARACTER,
                    coverUrl = coverUrl,
                    bookCount = matchingBooks.size
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
        list.add(
            CircleShelfFilter(
                id = "ingles",
                title = "Inglés",
                type = CircleShelfType.TAG,
                coverUrl = englishBooks.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl,
                bookCount = englishBooks.size
            )
        )

        list
    }

    val filteredBooks = remember(allBooks, selectedFilterId) {
        val selected = circleFilters.firstOrNull { it.id == selectedFilterId } ?: circleFilters.first()
        when (selected.type) {
            CircleShelfType.ALL -> allBooks
            CircleShelfType.LEVEL -> allBooks.filter { getBookDifficultyLevel(it) == selected.levelNumber }
            CircleShelfType.CHARACTER, CircleShelfType.TAG -> {
                val matches = if (selected.id == "ingles") {
                    allBooks.filter { isEnglishBook(it) }
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
    val isAnyModalOpen = showDetailsModal || showUserProfilesModal

    CalibreTVTheme(isDarkTheme = isDarkTheme) {
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
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 36.dp, top = 14.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ESTANTERÍA DE LIBROS",
                                    color = if (isDarkTheme) TextPrimary else InkPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "• ${filteredBooks.size} libros sincronizados",
                                    color = if (isDarkTheme) CyanElectric else Color(0xFF0284C7),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                    // Clock and profile
                    Row(
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
                                    feedContent = repository.getFeed()
                                    isLoading = false
                                }
                            },
                            onNotificationsClick = onNavigateToSettings
                        )
                    }
                }

                // Section A: Netflix Kids Circular Shelves & Difficulty Badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .padding(horizontal = 32.dp, vertical = 4.dp),
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
                                        detailsBook = book
                                        showDetailsModal = true
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

        // Book Details Dialog
        // Book Details Dialog (Sin bordes, superficie adaptativa al tema)
        if (showDetailsModal && detailsBook != null) {
            val book = detailsBook!!
            val modalCover = rememberCoverImage(book.coverUrl, authHeader)
            val modalBg = if (isDarkTheme) SurfaceContainer else Color(0xFFF7F5F0)
            val synopsisBg = if (isDarkTheme) SurfaceContainerHigh.copy(alpha = 0.5f) else Color.White

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isDarkTheme) 0.88f else 0.65f))
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Back || keyEvent.key == Key.Escape)) {
                            showDetailsModal = false
                            detailsBook = null
                            true
                        } else false
                    }
                    .clickable {
                        showDetailsModal = false
                        detailsBook = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .fillMaxHeight(0.80f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(modalBg)
                        .clickable(enabled = false) {}
                        .padding(28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Cover on the left
                        Book3DView(
                            coverBitmap = modalCover,
                            title = book.title,
                            width = 180.dp,
                            height = 265.dp,
                            isFocused = false,
                            enable3DStandby = false
                        )

                        // Details & Actions on the right
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = book.title,
                                    color = if (isDarkTheme) TextPrimary else InkPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${book.author} • ${book.category}",
                                    color = AmberWarm,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val modalLevel = remember(book) { getBookDifficultyLevel(book) }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (modalLevel in 1..5) {
                                        Box(
                                            modifier = Modifier
                                                .background(CyanElectric.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                                                .border(1.dp, CyanElectric, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "⭐ Dificultad: Nivel $modalLevel",
                                                color = CyanElectric,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    book.shelves.take(3).forEach { shelf ->
                                        Box(
                                            modifier = Modifier
                                                .background(AmberWarm.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                                                .border(1.dp, AmberWarm, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "🏷 $shelf",
                                                color = AmberWarm,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Sinopsis:",
                                    color = if (isDarkTheme) TextMuted else InkSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(synopsisBg)
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = if (modalDescription.isNotBlank()) modalDescription else book.summary.ifBlank { "Sin descripción disponible." },
                                        color = if (isDarkTheme) TextPrimary.copy(alpha = 0.9f) else Color(0xFF44403C),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            var isFav by remember(book.id) { mutableStateOf(repository.isFavorite(book.id)) }

                            // Modal Buttons strictly FIXED at the bottom
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                GridActionCapsule(
                                    title = "Leer en 3D",
                                    icon = Icons.Default.MenuBook,
                                    isPrimary = true,
                                    isDarkTheme = isDarkTheme,
                                    modifier = Modifier.focusRequester(modalReadFocusRequester),
                                    onClick = {
                                        showDetailsModal = false
                                        detailsBook = null
                                        onBookSelected(book)
                                    }
                                )
                                GridActionCapsule(
                                    title = if (isFav) "En Favoritos" else "Añadir a Favoritos",
                                    icon = Icons.Default.Star,
                                    isPrimary = isFav,
                                    isDarkTheme = isDarkTheme,
                                    onClick = {
                                        repository.toggleFavorite(book.id)
                                        isFav = repository.isFavorite(book.id)
                                    }
                                )
                                GridActionCapsule(
                                    title = "Cerrar",
                                    icon = Icons.Default.Close,
                                    isPrimary = false,
                                    isDarkTheme = isDarkTheme,
                                    onClick = {
                                        showDetailsModal = false
                                        detailsBook = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
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

