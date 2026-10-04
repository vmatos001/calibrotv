package com.example.calibretv.ui.screens

import android.util.Log
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.ui.components.Book3DView
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.curator.BookOffer
import com.example.calibretv.data.curator.CuratedBook
import com.example.calibretv.data.curator.CuratorRepository
import com.example.calibretv.data.curator.CuratorSection
import com.example.calibretv.data.curator.HeroBanner
import com.example.calibretv.data.curator.HomeCarteleraData
import com.example.calibretv.data.image.CoverLoader
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.opds.OpdsFeedContent
import com.example.calibretv.data.storage.PreferencesManager
import com.example.calibretv.theme.AccentGold
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceCard
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceContainerHighest
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.ui.components.CuratedBookModal
import com.example.calibretv.ui.components.CuratorRow
import com.example.calibretv.theme.CalibreTVTheme
import com.example.calibretv.theme.CanvasBackgroundLight
import com.example.calibretv.theme.InkPrimary
import com.example.calibretv.theme.InkSecondary
import com.example.calibretv.ui.components.PinPadDialog
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.components.TvProfilePill
import com.example.calibretv.ui.components.TvSidebar
import com.example.calibretv.ui.components.UserProfilesDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    repository: BookRepository,
    onBookSelected: (Book) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToYourBooks: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToOpds: () -> Unit,
    onNavigateToReader: () -> Unit,
    onNavigateToWifiImport: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { com.example.calibretv.data.storage.PreferencesManager(context) }
    var isDarkTheme by remember { mutableStateOf(prefsManager.isDarkTheme()) }

    var feedContent by remember { mutableStateOf<OpdsFeedContent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var activeProfile by remember { mutableStateOf(repository.getActiveProfile()) }

    val sidebarFocusRequester = remember { FocusRequester() }
    var showUserProfilesModal by remember { mutableStateOf(false) }
    var pendingProtectedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val householdPin = remember(activeProfile) {
        repository.getProfiles().firstOrNull { it.parentalPin != null }?.parentalPin
    }

    // Modal state for Book Details
    var showDetailsModal by remember { mutableStateOf(false) }
    var detailsBook by remember { mutableStateOf<Book?>(null) }
    var modalDescription by remember { mutableStateOf("") }
    val modalReadFocusRequester = remember { FocusRequester() }

    // Cartelera Dinámica: Hero Banner 16:9 + Carrusel de Ofertas (CMS /api/v1/cartelera/home)
    var homeCarteleraData by remember { mutableStateOf(CuratorRepository.getOfflineHomeData()) }

    // Estanterías por Personajes / Arquetipos (CMS /api/v1/cartelera/shelves)
    var allCuratorSections by remember { mutableStateOf(CuratorRepository.getCuratedSections()) }
    val curatorSections = remember(allCuratorSections, activeProfile) {
        if (!activeProfile.isKidsMode) {
            allCuratorSections
        } else {
            allCuratorSections.filter { sec ->
                sec.name.contains("Prodigio", ignoreCase = true) ||
                sec.name.contains("Universales", ignoreCase = true) ||
                sec.name.contains("Infantil", ignoreCase = true) ||
                sec.name.contains("Matilda", ignoreCase = true) ||
                sec.name.contains("Lisa", ignoreCase = true)
            }
        }
    }
    var selectedCuratedBook by remember { mutableStateOf<CuratedBook?>(null) }
    var isDownloadingCuratedBook by remember { mutableStateOf(false) }

    // Sincronización continua con CMS
    LaunchedEffect(Unit) {
        val prefs = PreferencesManager(context)
        val cmsUrl = prefs.getCmsServerUrl()
        coroutineScope.launch {
            try {
                val data = CuratorRepository.fetchHomeCartelera(cmsUrl)
                homeCarteleraData = data
            } catch (e: Exception) {
                Log.d("HomeScreen", "CMS Cartelera offline fallback: ${e.message}")
            }
        }
        coroutineScope.launch {
            try {
                val synced = CuratorRepository.syncWithCms(cmsUrl)
                if (synced) {
                    allCuratorSections = CuratorRepository.getCuratedSections()
                }
            } catch (e: Exception) {
                Log.d("HomeScreen", "CMS Shelves offline fallback: ${e.message}")
            }
        }
    }

    BackHandler(enabled = true) {
        if (pendingProtectedAction != null) pendingProtectedAction = null
        else if (selectedCuratedBook != null) selectedCuratedBook = null
        else if (showDetailsModal) showDetailsModal = false
        else if (showUserProfilesModal) showUserProfilesModal = false
        else sidebarFocusRequester.requestFocus()
    }

    LaunchedEffect(showDetailsModal) {
        if (showDetailsModal) {
            modalReadFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(detailsBook) {
        detailsBook?.let { b ->
            modalDescription = repository.getOrFetchBookDescription(b)
        }
    }

    val verticalScrollState = rememberScrollState()

    val config = remember { repository.getServerConfig() }
    val authHeader = remember(config) { CoverLoader.buildBasicAuth(config.username, config.password) }

    // Load local/OPDS library feed
    LaunchedEffect(activeProfile) {
        isLoading = true
        val result = repository.getFeed()
        feedContent = result
        isLoading = false
    }

    val rawBooks = feedContent?.books ?: emptyList()
    val allBooks = remember(rawBooks, activeProfile) {
        if (!activeProfile.isKidsMode) {
            rawBooks
        } else {
            if (activeProfile.whitelistBookIds.isNotEmpty()) {
                rawBooks.filter { b -> activeProfile.whitelistBookIds.contains(b.id) }
            } else {
                val kidsKeywords = listOf(
                    "infantil", "niño", "nino", "cuento", "fabula", "fábula", "aventura",
                    "principito", "alicia", "peter pan", "tesoro", "selva", "comic", "cómic", "dominio público"
                )
                rawBooks.filter { b ->
                    val titleNorm = b.title.lowercase()
                    val catNorm = b.category.lowercase()
                    val tagsNorm = b.tags.map { it.lowercase() }
                    kidsKeywords.any { k ->
                        titleNorm.contains(k) || catNorm.contains(k) || tagsNorm.any { t -> t.contains(k) }
                    }
                }
            }
        }
    }

    // Libros Favoritos del usuario activo
    var favoriteBooks by remember(activeProfile, allBooks) {
        mutableStateOf(repository.getFavoriteBooks())
    }

    fun toggleFavorite(book: Book) {
        repository.toggleFavorite(book.id)
        favoriteBooks = repository.getFavoriteBooks()
    }

    // Libros que el usuario está leyendo actualmente (con progreso > 0)
    val continuingBooks = remember(allBooks, activeProfile) {
        allBooks.filter { it.progressPercent > 0 }
    }

    // Reloj en vivo
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        currentTime = sdf.format(Date())
    }

    val heroBanner = homeCarteleraData.heroBanner ?: CuratorRepository.getOfflineHomeData().heroBanner!!

    val screenBg = if (isDarkTheme) BackgroundDark else CanvasBackgroundLight
    val heroBg = if (isDarkTheme) BackgroundDark else Color(0xFF141619)

    CalibreTVTheme(isDarkTheme = isDarkTheme) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
        ) {
            // Rail de Navegación Lateral (Letra A: 68dp, Letra B al desplegar: 215dp)
            TvSidebar(
                currentTab = TvNavTab.HOME,
                isDarkTheme = isDarkTheme,
                onToggleTheme = {
                    val newTheme = !isDarkTheme
                    isDarkTheme = newTheme
                    prefsManager.setDarkTheme(newTheme)
                },
                focusRequester = sidebarFocusRequester,
                onTabSelected = { tab ->
                    when (tab) {
                        TvNavTab.HOME -> {}
                        TvNavTab.BIBLIOTECA -> onNavigateToLibrary()
                        TvNavTab.TUS_LIBROS -> onNavigateToYourBooks()
                        TvNavTab.LECTOR_3D -> onNavigateToReader()
                        TvNavTab.AJUSTES -> {
                            if (activeProfile.isKidsMode && householdPin != null) {
                                pendingProtectedAction = { onNavigateToSettings() }
                            } else {
                                onNavigateToSettings()
                            }
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBg)
                ) {
                // ==========================================
                // CABECERA SUPERIOR: Reloj en vivo & Perfil de Usuario
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 28.dp, top = 14.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CalibroTV",
                        color = if (isDarkTheme) TextPrimary else InkPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = currentTime,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.75f) else Color(0xFF555B66),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        TvProfilePill(
                            profile = activeProfile,
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

                // ==========================================
                // CONTENIDO SCROLLABLE (Bento Grid)
                // ==========================================
                val shelfCardBg = if (isDarkTheme) Color(0xFF161920) else Color.White
                val shelfCardBorder = if (isDarkTheme) Color(0xFF262934) else Color(0xFFE2E7E2)

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGold)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                            .padding(bottom = 60.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ==========================================
                        // ZONA A: BENTO HERO BANNER (Estilo Imagen 2 Card A)
                        // ==========================================
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .padding(horizontal = 28.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color(0xFF12141A))
                                .border(1.dp, Color(0xFF242734), RoundedCornerShape(26.dp))
                                .shadow(
                                    elevation = if (isDarkTheme) 0.dp else 8.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    spotColor = Color.Black.copy(alpha = 0.15f)
                                )
                        ) {
                            val backdropCoverBmp = rememberCoverImage(heroBanner.backdropUrl.ifBlank { heroBanner.coverUrl })
                            if (backdropCoverBmp != null) {
                                Image(
                                    bitmap = backdropCoverBmp,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { alpha = 0.28f }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF12141A),
                                                Color(0xFF12141A).copy(alpha = 0.95f),
                                                Color(0xFF12141A).copy(alpha = 0.70f),
                                                Color(0xFF12141A).copy(alpha = 0.30f),
                                                Color.Transparent
                                            ),
                                            startX = 0f,
                                            endX = 1100f
                                        )
                                    )
                            )

                            HomeHeroBanner(
                                heroBanner = heroBanner,
                                onReadSample = {
                                    val sampleUrl = heroBanner.sampleEpubUrl
                                    if (!sampleUrl.isNullOrBlank()) {
                                        isDownloadingCuratedBook = true
                                        coroutineScope.launch {
                                            val heroCurated = CuratedBook(
                                                id = heroBanner.id,
                                                title = heroBanner.title,
                                                author = heroBanner.author,
                                                coverUrl = heroBanner.coverUrl,
                                                summary = heroBanner.synopsis,
                                                category = "Bestseller Destacado",
                                                isPublicDomain = true,
                                                publicDownloadUrl = sampleUrl
                                            )
                                            val res = CuratorRepository.downloadPublicDomainBook(context, heroCurated, repository)
                                            isDownloadingCuratedBook = false
                                            if (res.isSuccess && res.getOrNull() != null) {
                                                onBookSelected(res.getOrNull()!!)
                                            }
                                        }
                                    } else {
                                        selectedCuratedBook = CuratedBook(
                                            id = heroBanner.id,
                                            title = heroBanner.title,
                                            author = heroBanner.author,
                                            coverUrl = heroBanner.coverUrl,
                                            summary = heroBanner.synopsis,
                                            category = "Bestseller Destacado",
                                            isPublicDomain = false,
                                            affiliateQrUrl = heroBanner.affiliatePurchaseUrl
                                        )
                                    }
                                },
                                onBuyQr = {
                                    selectedCuratedBook = CuratedBook(
                                        id = heroBanner.id,
                                        title = heroBanner.title,
                                        author = heroBanner.author,
                                        coverUrl = heroBanner.coverUrl,
                                        summary = heroBanner.synopsis,
                                        category = "Bestseller Destacado",
                                        isPublicDomain = false,
                                        affiliateQrUrl = heroBanner.affiliatePurchaseUrl
                                    )
                                },
                                onLeftAtBoundary = { sidebarFocusRequester.requestFocus() }
                            )
                        }

                        // ==========================================
                        // ZONA B: TARJETAS BENTO PARA LIBROS (Estilo Imagen 2 Card B)
                        // ==========================================

                        // Carrusel 1: Continuar Leyendo (libros con progreso > 0)
                        if (continuingBooks.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(shelfCardBg)
                                    .border(1.dp, shelfCardBorder, RoundedCornerShape(24.dp))
                                    .shadow(
                                        elevation = if (isDarkTheme) 0.dp else 4.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        spotColor = Color.Black.copy(alpha = 0.05f)
                                    )
                                    .padding(vertical = 12.dp)
                            ) {
                                HomeShelf(
                                    sectionTitle = "Continuar Leyendo",
                                    icon = Icons.Default.History,
                                    books = continuingBooks,
                                    authHeader = authHeader,
                                    showSeeMore = false,
                                    onSeeMore = {},
                                    onLeftAtBoundary = { sidebarFocusRequester.requestFocus() },
                                    isInteractive = !showDetailsModal && !showUserProfilesModal,
                                    isDarkTheme = isDarkTheme,
                                    onBookFocused = {},
                                    onBookSelected = { book ->
                                        onBookSelected(book)
                                    }
                                )
                            }
                        }

                        // Carrusel 2: Ofertas y Descuentos Destacados (desde CMS)
                        if (homeCarteleraData.offers.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(shelfCardBg)
                                    .border(1.dp, shelfCardBorder, RoundedCornerShape(24.dp))
                                    .shadow(
                                        elevation = if (isDarkTheme) 0.dp else 4.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        spotColor = Color.Black.copy(alpha = 0.05f)
                                    )
                                    .padding(vertical = 12.dp)
                            ) {
                                HomeOffersRow(
                                    offers = homeCarteleraData.offers,
                                    isDarkTheme = isDarkTheme,
                                    onOfferSelected = { offer ->
                                        selectedCuratedBook = CuratedBook(
                                            id = offer.id,
                                            title = offer.title,
                                            author = offer.author,
                                            coverUrl = offer.coverUrl,
                                            summary = "Oferta especial de cartelera con descuento de ${offer.discountTag}.",
                                            category = "Oferta ${offer.discountTag}",
                                            isPublicDomain = false,
                                            affiliateQrUrl = offer.affiliateUrl
                                        )
                                    },
                                    onLeftAtBoundary = { sidebarFocusRequester.requestFocus() }
                                )
                            }
                        }

                        // Carrusel 3: Cartelera Dinámica por Personajes / Arquetipos
                        curatorSections.forEach { section ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(shelfCardBg)
                                    .border(1.dp, shelfCardBorder, RoundedCornerShape(24.dp))
                                    .shadow(
                                        elevation = if (isDarkTheme) 0.dp else 4.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        spotColor = Color.Black.copy(alpha = 0.05f)
                                    )
                                    .padding(vertical = 12.dp)
                            ) {
                                CuratorRow(
                                    section = section,
                                    isDarkTheme = isDarkTheme,
                                    onBookClick = { curatedBook ->
                                        selectedCuratedBook = curatedBook
                                    },
                                    onLeftAtBoundary = { sidebarFocusRequester.requestFocus() }
                                )
                            }
                        }

                        // Carrusel 4: Libros Favoritos del usuario activo
                        if (favoriteBooks.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(shelfCardBg)
                                    .border(1.dp, shelfCardBorder, RoundedCornerShape(24.dp))
                                    .shadow(
                                        elevation = if (isDarkTheme) 0.dp else 4.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        spotColor = Color.Black.copy(alpha = 0.05f)
                                    )
                                    .padding(vertical = 12.dp)
                            ) {
                                HomeShelf(
                                    sectionTitle = "Tus Libros Favoritos",
                                    icon = Icons.Default.Star,
                                    books = favoriteBooks,
                                    authHeader = authHeader,
                                    showSeeMore = false,
                                    onSeeMore = {},
                                    onLeftAtBoundary = { sidebarFocusRequester.requestFocus() },
                                    isInteractive = !showDetailsModal && !showUserProfilesModal,
                                    isDarkTheme = isDarkTheme,
                                    onBookFocused = {},
                                    onBookSelected = { book ->
                                        detailsBook = book
                                        showDetailsModal = true
                                    }
                                )
                            }
                        }

                        // ----------------------------------------------------
                        // Accesos Rápidos: Explorar Catálogo Completo y Memoria TV
                        // ----------------------------------------------------
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HomeQuickAccessCard(
                                title = "Explorar Biblioteca",
                                subtitle = "Ver catálogo completo de libros con filtros y búsqueda",
                                icon = Icons.Default.AutoStories,
                                modifier = Modifier.weight(1f),
                                isDarkTheme = isDarkTheme,
                                onClick = onNavigateToLibrary
                            )
                            HomeQuickAccessCard(
                                title = "Gestor de Memoria TV",
                                subtitle = "Ver libros en almacenamiento de la TV y liberar espacio",
                                icon = Icons.Default.Storage,
                                modifier = Modifier.weight(1f),
                                isDarkTheme = isDarkTheme,
                                onClick = onNavigateToYourBooks
                            )
                        }
                    }
                }
        }

        // ==========================================
        // PIN PAD DIALOG PARA ACCIONES PROTEGIDAS EN MODO KIDS
        // ==========================================
        val actionToRun = pendingProtectedAction
        if (actionToRun != null && householdPin != null) {
            PinPadDialog(
                title = "Control Parental",
                subtitle = "Introduce el PIN parental para continuar",
                targetPin = householdPin,
                onSuccess = {
                    pendingProtectedAction = null
                    actionToRun()
                },
                onDismiss = {
                    pendingProtectedAction = null
                }
            )
        }

        // ==========================================
        // MODAL DE GESTIÓN DE PERFILES DE USUARIO
        // ==========================================
        if (showUserProfilesModal) {
            UserProfilesDialog(
                repository = repository,
                activeProfile = activeProfile,
                onProfileChanged = { newProfile ->
                    activeProfile = newProfile
                    favoriteBooks = repository.getFavoriteBooks()
                    showUserProfilesModal = false
                },
                onDismiss = {
                    showUserProfilesModal = false
                    activeProfile = repository.getActiveProfile()
                    favoriteBooks = repository.getFavoriteBooks()
                }
            )
        }

        // ==========================================
        // MODAL DE DETALLES DEL LIBRO (Caja Stitch)
        // ==========================================
        if (showDetailsModal && detailsBook != null) {
            val book = detailsBook!!
            val modalCover = rememberCoverImage(book.coverUrl, authHeader)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable { showDetailsModal = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .fillMaxHeight(0.80f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceContainer)
                        .border(1.5.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
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
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = book.title,
                                    color = TextPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${book.author} • ${book.category}",
                                    color = AmberWarm,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (book.shelves.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        book.shelves.take(3).forEach { shelf ->
                                            Box(
                                                modifier = Modifier
                                                    .background(AmberWarm.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                    .border(1.dp, AmberWarm.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(text = "🏷 $shelf", color = AmberWarm, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }

                                if (book.tags.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        book.tags.take(4).forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(SurfaceContainerHigh, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(text = "#$tag", color = AccentGold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Sinopsis:",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = modalDescription.ifBlank { book.summary },
                                    color = TextPrimary.copy(alpha = 0.88f),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .verticalScroll(rememberScrollState())
                                )
                            }

                            // Modal Buttons with trapped remote focus
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                HomeActionCapsule(
                                    title = "Leer en 3D",
                                    icon = Icons.Default.MenuBook,
                                    isPrimary = true,
                                    modifier = Modifier.focusRequester(modalReadFocusRequester),
                                    onClick = {
                                        showDetailsModal = false
                                        onBookSelected(book)
                                    }
                                )
                                HomeActionCapsule(
                                    title = if (repository.isFavorite(book.id)) "En Favoritos" else "Añadir a Favoritos",
                                    icon = Icons.Default.Star,
                                    isPrimary = repository.isFavorite(book.id),
                                    onClick = {
                                        toggleFavorite(book)
                                    }
                                )
                                HomeActionCapsule(
                                    title = "Cerrar",
                                    icon = Icons.Default.Close,
                                    isPrimary = false,
                                    onClick = { showDetailsModal = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // MODAL DE LIBRO CURADO (Compra QR / Descarga)
        // ==========================================
        selectedCuratedBook?.let { curatedBook ->
            val isDownloaded = remember(curatedBook.id, allBooks) {
                CuratorRepository.isBookDownloaded(curatedBook.id, repository)
            }
            CuratedBookModal(
                book = curatedBook,
                isDownloaded = isDownloaded,
                isDownloading = isDownloadingCuratedBook,
                onDownload = {
                    isDownloadingCuratedBook = true
                    coroutineScope.launch {
                        val res = CuratorRepository.downloadPublicDomainBook(context, curatedBook, repository)
                        if (res.isSuccess) {
                            feedContent = repository.getFeed()
                        }
                        isDownloadingCuratedBook = false
                    }
                },
                onRead = {
                    val localBook = repository.getCachedBooks().find { it.id == curatedBook.id }
                    if (localBook != null) {
                        selectedCuratedBook = null
                        onBookSelected(localBook)
                    }
                },
                onDismiss = {
                    selectedCuratedBook = null
                }
            )
        }
    }
}
}
}

@Composable
private fun HomeHeroSection(
    book: Book,
    authHeader: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRead: () -> Unit,
    onDetails: () -> Unit
) {
    val coverBmp = rememberCoverImage(book.coverUrl, authHeader)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail poster on the left
        Box(
            modifier = Modifier
                .width(82.dp)
                .height(118.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerHigh)
                .border(1.dp, Color(0xFF333338), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (coverBmp != null) {
                Image(
                    bitmap = coverBmp,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // Details & Prominent Buttons (Section C)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Tagline
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (book.progressPercent > 0) "CONTINUAR LECTURA • ${book.progressPercent}% COMPLETADO" else "DESTACADO • LISTO PARA LEER",
                    color = AmberWarm,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Title
            Text(
                text = book.title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Author & Category
            Text(
                text = "${book.author} • ${book.category}",
                color = AccentGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Synopsis (compact)
            Text(
                text = book.summary,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons: Clearly visible under synopsis
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeActionCapsule(
                    title = "Leer en 3D",
                    icon = Icons.Default.MenuBook,
                    isPrimary = true,
                    onClick = onRead
                )
                HomeActionCapsule(
                    title = if (isFavorite) "En Favoritos" else "Favorito",
                    icon = Icons.Default.Star,
                    isPrimary = isFavorite,
                    onClick = onToggleFavorite
                )
                HomeActionCapsule(
                    title = "Detalles",
                    icon = Icons.Default.Info,
                    isPrimary = false,
                    onClick = onDetails
                )
            }
        }
    }
}

@Composable
private fun HomeShelf(
    sectionTitle: String,
    icon: ImageVector,
    books: List<Book>,
    authHeader: String?,
    showSeeMore: Boolean,
    onSeeMore: () -> Unit,
    onLeftAtBoundary: () -> Unit,
    isInteractive: Boolean = true,
    isDarkTheme: Boolean = true,
    onBookFocused: (Book) -> Unit,
    onBookSelected: (Book) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Shelf Header: Title on left, [ Ver más ] button on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = sectionTitle,
                    color = if (isDarkTheme) TextPrimary else InkPrimary,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }

            if (showSeeMore) {
                var isSeeMoreFocused by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .scale(if (isSeeMoreFocused) 1.08f else 1.0f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSeeMoreFocused) AccentGold.copy(alpha = 0.25f) else Color.Transparent)
                        .border(
                            width = if (isSeeMoreFocused) 1.5.dp else 1.dp,
                            color = if (isSeeMoreFocused) AccentGold else Color(0xFF33333E),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .onFocusChanged { isSeeMoreFocused = it.isFocused }
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                onSeeMore()
                                true
                            } else false
                        }
                        .focusable(enabled = isInteractive)
                        .clickable(enabled = isInteractive) { onSeeMore() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Ver más",
                        color = if (isSeeMoreFocused) AccentGold else (if (isDarkTheme) TextPrimary else InkPrimary),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = if (isSeeMoreFocused) AccentGold else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else {
                Text(
                    text = "${books.size} en progreso",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Horizontal Carousel of 3D Books (Tilted in standby, face-forward on focus)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            itemsIndexed(books) { index, book ->
                val isFirst = index == 0
                CompactCoverCard(
                    book = book,
                    authHeader = authHeader,
                    isInteractive = isInteractive,
                    isFirst = isFirst,
                    isDarkTheme = isDarkTheme,
                    onLeftAtBoundary = onLeftAtBoundary,
                    onFocused = { onBookFocused(book) },
                    onSelected = { onBookSelected(book) }
                )
            }
        }
    }
}

/**
 * 3D Book Cover: Physical book presentation without card background,
 * tilted in standby and rotating forward upon remote selection.
 */
@Composable
private fun CompactCoverCard(
    book: Book,
    authHeader: String?,
    isInteractive: Boolean = true,
    isFirst: Boolean = false,
    isDarkTheme: Boolean = true,
    onLeftAtBoundary: () -> Unit,
    onFocused: () -> Unit,
    onSelected: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(book.coverUrl, authHeader)

    Column(
        modifier = Modifier
            .width(122.dp)
            .onFocusChanged {
                if (isInteractive) {
                    isFocused = it.isFocused
                    if (it.isFocused) onFocused()
                }
            }
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
                            onSelected()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(enabled = isInteractive)
            .clickable(enabled = isInteractive) { onSelected() }
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
                .padding(horizontal = 2.dp)
        ) {
            Text(
                text = book.title,
                color = if (isFocused) AmberWarm else (if (isDarkTheme) TextPrimary else InkPrimary),
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeActionCapsule(
    title: String,
    icon: ImageVector,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .scale(if (isFocused) 1.06f else 1.0f)
            .shadow(if (isFocused) 10.dp else 2.dp, RoundedCornerShape(20.dp), spotColor = AmberWarm)
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isFocused -> AmberWarm
                    isPrimary -> AmberWarm
                    else -> SurfaceContainerHigh
                }
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    onClick()
                    true
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused || isPrimary) Color(0xFF131315) else TextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = if (isFocused || isPrimary) Color(0xFF131315) else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HomeCapsuleChip(
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
                    isSelected -> AccentGold.copy(alpha = 0.20f)
                    else -> SurfaceContainerHigh
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> AmberWarm
                    isSelected -> AccentGold
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
                isSelected -> AccentGold
                else -> TextMuted
            },
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = title,
            color = when {
                isFocused -> Color(0xFF131315)
                isSelected -> AccentGold
                else -> TextPrimary
            },
            fontSize = 11.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun FavoriteEmptyShelf(
    onLeftAtBoundary: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = AmberWarm,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Libros Favoritos",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .scale(if (isFocused) 1.01f else 1.0f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isFocused) SurfaceContainerHighest else SurfaceContainer)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) AmberWarm else Color(0xFF2E2E34),
                    shape = RoundedCornerShape(12.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                        onLeftAtBoundary()
                        true
                    } else false
                }
                .focusable()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AmberWarm.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Aún no tienes libros favoritos en este perfil",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Selecciona cualquier libro y presiona 'Favorito' para agregarlo a tu colección rápida.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryBanner(
    onNavigateToOpds: () -> Unit,
    onLeftAtBoundary: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isFocused) SurfaceContainerHighest else SurfaceContainer)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) AccentGold else Color(0xFF2E2E34),
                    shape = RoundedCornerShape(16.dp)
                )
                .scale(if (isFocused) 1.01f else 1.0f)
                .onFocusChanged { isFocused = it.isFocused }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                            onNavigateToOpds()
                            true
                        } else if (event.key == Key.DirectionLeft) {
                            onLeftAtBoundary()
                            true
                        } else false
                    } else false
                }
                .focusable()
                .clickable { onNavigateToOpds() }
                .padding(28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AccentGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tu biblioteca CalibroTV está lista",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Transfiere libros (.epub / .cbz) desde tu teléfono o PC por WiFi con QR, o conecta un servidor Calibre-Web.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentGold)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Configurar Servidor / Fuentes",
                        color = Color(0xFF131315),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeroBanner(
    heroBanner: HeroBanner,
    onReadSample: () -> Unit,
    onBuyQr: () -> Unit,
    onLeftAtBoundary: () -> Unit
) {
    val context = LocalContext.current
    val fallbackCoverBmp = remember {
        try {
            android.graphics.BitmapFactory.decodeResource(
                context.resources,
                com.example.calibretv.R.drawable.hero_tres_cuerpos
            )?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    val downloadedCover = rememberCoverImage(heroBanner.coverUrl)
    val coverBmp = downloadedCover ?: fallbackCoverBmp
    val readBtnFocusRequester = remember { FocusRequester() }
    var isReadFocused by remember { mutableStateOf(false) }

    val qrBitmap = remember(heroBanner.affiliatePurchaseUrl) {
        try {
            val url = heroBanner.affiliatePurchaseUrl?.ifBlank { "https://amazon.es" } ?: "https://amazon.es"
            com.example.calibretv.data.server.QrCodeGenerator.generateQrBitmap(url, 200, 200)
        } catch (_: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LADO IZQUIERDO: Portada 3D del libro destacado
        Book3DView(
            coverBitmap = coverBmp,
            title = heroBanner.title,
            width = 94.dp,
            height = 138.dp,
            isFocused = false,
            enable3DStandby = false
        )

        // CENTRO: Badge, Título, Autor, Cita, Sinopsis y Botón Blanco (Estilo Imagen 2 Card A)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            // Badge BESTSELLER DEL MES
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222632))
                    .border(1.dp, Color(0xFF333848), RoundedCornerShape(12.dp))
                    .padding(horizontal = 9.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "★ BESTSELLER DEL MES",
                    color = AmberWarm,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Título Principal
            Text(
                text = heroBanner.title,
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Autor
            Text(
                text = heroBanner.author.ifBlank { "Cixin Liu" },
                color = AmberWarm,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Cita editorial destacada
            Text(
                text = "“${heroBanner.tagline.ifBlank { heroBanner.synopsis }}”",
                color = Color(0xFFCBD5E1),
                fontSize = 10.5.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Sinopsis breve
            Text(
                text = heroBanner.synopsis.ifBlank { heroBanner.tagline },
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Botón Blanco de Acción (Estilo Imagen 2 Card A: [ Upgrade > ])
            Box(
                modifier = Modifier
                    .focusRequester(readBtnFocusRequester)
                    .scale(if (isReadFocused) 1.05f else 1.0f)
                    .shadow(
                        elevation = if (isReadFocused) 10.dp else 4.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = if (isReadFocused) AmberWarm else Color.Black.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .border(
                        width = if (isReadFocused) 2.dp else 0.dp,
                        color = if (isReadFocused) AmberWarm else Color.Transparent,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .onFocusChanged { isReadFocused = it.isFocused }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    onLeftAtBoundary()
                                    true
                                }
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    onReadSample()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .focusable()
                    .clickable { onReadSample() }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF111317),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Leer Muestra EPUB (Capítulo 1)",
                        color = Color(0xFF111317),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF111317),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // LADO DERECHO: Tarjeta con Código QR a la vista para comprar desde el móvil
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161922))
                .border(1.dp, Color(0xFF262A38), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Compra en Móvil",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Text(
                    text = "COMPRAR EN MÓVIL",
                    color = Color(0xFFD0D4DC),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun HomeOffersRow(
    offers: List<BookOffer>,
    isDarkTheme: Boolean = true,
    onOfferSelected: (BookOffer) -> Unit,
    onLeftAtBoundary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Cabecera de la fila de ofertas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sell,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = "Ofertas y Descuentos Destacados",
                    color = if (isDarkTheme) TextPrimary else InkPrimary,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = "Descuentos de cartelera válidos por tiempo limitado con entrega digital",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            itemsIndexed(offers, key = { _, o -> o.id }) { index, offer ->
                HomeOfferCard(
                    offer = offer,
                    isFirst = index == 0,
                    isDarkTheme = isDarkTheme,
                    onClick = { onOfferSelected(offer) },
                    onLeftAtBoundary = onLeftAtBoundary
                )
            }
        }
    }
}

@Composable
private fun HomeOfferCard(
    offer: BookOffer,
    isFirst: Boolean,
    isDarkTheme: Boolean = true,
    onClick: () -> Unit,
    onLeftAtBoundary: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(offer.coverUrl)

    Column(
        modifier = Modifier
            .width(122.dp)
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
            .focusable()
            .clickable { onClick() }
    ) {
        Book3DView(
            coverBitmap = coverBmp,
            title = offer.title,
            width = 96.dp,
            height = 142.dp,
            isFocused = isFocused
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = offer.title,
            color = if (isFocused) AmberWarm else (if (isDarkTheme) TextPrimary else InkPrimary),
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeQuickAccessCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardBg = if (isDarkTheme) {
        if (isFocused) Color(0xFF222632) else Color(0xFF161920)
    } else {
        if (isFocused) Color(0xFFF2F5F2) else Color.White
    }

    val cardBorder = if (isFocused) {
        AmberWarm
    } else {
        if (isDarkTheme) Color(0xFF262934) else Color(0xFFE2E7E2)
    }

    val titleColor = if (isDarkTheme) TextPrimary else InkPrimary
    val subtitleColor = if (isDarkTheme) TextMuted else InkSecondary

    Box(
        modifier = modifier
            .scale(if (isFocused) 1.03f else 1.0f)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(
                elevation = if (isDarkTheme) 0.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
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
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AmberWarm.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = if (isFocused) AmberWarm else (if (isDarkTheme) TextMuted else InkSecondary),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
