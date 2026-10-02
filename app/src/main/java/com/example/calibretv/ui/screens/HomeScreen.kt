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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.calibretv.theme.SurfaceCard
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceContainerHighest
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.ui.components.CuratedBookModal
import com.example.calibretv.ui.components.CuratorRow
import com.example.calibretv.ui.components.DrawerItem
import com.example.calibretv.ui.components.PinPadDialog
import com.example.calibretv.ui.components.TvSideDrawer
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
    var feedContent by remember { mutableStateOf<OpdsFeedContent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var activeProfile by remember { mutableStateOf(repository.getActiveProfile()) }

    var isDrawerOpen by remember { mutableStateOf(false) }
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

    BackHandler(enabled = showDetailsModal || showUserProfilesModal || isDrawerOpen || selectedCuratedBook != null || pendingProtectedAction != null) {
        if (pendingProtectedAction != null) pendingProtectedAction = null
        else if (selectedCuratedBook != null) selectedCuratedBook = null
        else if (showDetailsModal) showDetailsModal = false
        else if (showUserProfilesModal) showUserProfilesModal = false
        else if (isDrawerOpen) isDrawerOpen = false
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
        ) {
            // ==========================================
            // ZONA A: HERO BANNER 16:9 (BESTSELLER DEL MES)
            // Branding oficial CalibroTV + Reloj + Perfil
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.40f)
                    .background(BackgroundDark)
            ) {
                // 1. Dynamic 16:9 Backdrop Image from CMS
                val backdropCoverBmp = rememberCoverImage(heroBanner.backdropUrl.ifBlank { heroBanner.coverUrl })
                if (backdropCoverBmp != null) {
                    Image(
                        bitmap = backdropCoverBmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.35f }
                    )
                }

                // 2. Horizontal gradient overlay (OLED black on left for sharp legibility)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BackgroundDark,
                                    BackgroundDark.copy(alpha = 0.97f),
                                    BackgroundDark.copy(alpha = 0.82f),
                                    BackgroundDark.copy(alpha = 0.40f),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = 1400f
                            )
                        )
                )

                // 3. Vertical gradient towards shelves
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    BackgroundDark.copy(alpha = 0.50f),
                                    BackgroundDark
                                )
                            )
                        )
                )

                // 4. FLOATING TOP-RIGHT OVERLAY: Reloj en vivo & Perfil de Usuario
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 36.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = currentTime,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    var isProfileFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .scale(if (isProfileFocused) 1.08f else 1.0f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isProfileFocused) AmberWarm else Color.White.copy(alpha = 0.12f))
                            .border(
                                width = if (isProfileFocused) 1.5.dp else 1.dp,
                                color = if (isProfileFocused) Color.White else Color(0xFF383842),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .onFocusChanged { isProfileFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    showUserProfilesModal = true
                                    true
                                } else false
                            }
                            .focusable()
                            .clickable { showUserProfilesModal = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        val profColor = try {
                            Color(android.graphics.Color.parseColor(activeProfile.avatarColorHex))
                        } catch (_: Exception) {
                            AmberWarm
                        }
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isProfileFocused) Color(0xFF131315) else profColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isProfileFocused) AmberWarm else Color(0xFF131315),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = activeProfile.name,
                            color = if (isProfileFocused) Color(0xFF131315) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeProfile.isKidsMode) {
                            Text("🎈", fontSize = 11.sp)
                        }
                        if (activeProfile.starsCount > 0) {
                            Text(
                                text = "⭐ ${activeProfile.starsCount}",
                                color = if (isProfileFocused) Color(0xFF131315) else AmberWarm,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // 5. BRANDING OFICIAL CALIBROTV & BOTÓN DEL MENÚ LATERAL
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 36.dp, top = 16.dp)
                        .clickable { isDrawerOpen = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menú Lateral",
                        tint = AmberWarm,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "CALIBRO",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "TV",
                        color = AmberWarm,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // 6. HERO SPOTLIGHT: Portada, Sinopsis y Botones de Acción Inmediata
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 44.dp)
                ) {
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
                        onLeftAtBoundary = { isDrawerOpen = true }
                    )
                }
            }

            // ==========================================
            // ZONA B: CARRUSELES Y ESTANTERÍAS (SCROLL VERTICAL)
            // 1. Continuar Leyendo (progreso activo)
            // 2. Ofertas y Descuentos de Cartelera (con tags -45%, -30%)
            // 3. Cartelera Dinámica por Personajes (CMS / Arquetipos)
            // 4. Tus Libros Favoritos
            // 5. Accesos Rápidos a Biblioteca y Memoria TV
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(BackgroundDark)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGold)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                            .padding(top = 8.dp, bottom = 60.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ----------------------------------------------------
                        // Carrusel 1: Continuar Leyendo (libros con progreso > 0)
                        // ----------------------------------------------------
                        if (continuingBooks.isNotEmpty()) {
                            HomeShelf(
                                sectionTitle = "Continuar Leyendo",
                                icon = Icons.Default.History,
                                books = continuingBooks,
                                authHeader = authHeader,
                                showSeeMore = false,
                                onSeeMore = {},
                                onLeftAtBoundary = { isDrawerOpen = true },
                                isInteractive = !showDetailsModal && !showUserProfilesModal && !isDrawerOpen,
                                onBookFocused = {},
                                onBookSelected = { book ->
                                    onBookSelected(book)
                                }
                            )
                        }

                        // ----------------------------------------------------
                        // Carrusel 2: Ofertas y Descuentos Destacados (desde CMS)
                        // ----------------------------------------------------
                        if (homeCarteleraData.offers.isNotEmpty()) {
                            HomeOffersRow(
                                offers = homeCarteleraData.offers,
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
                                onLeftAtBoundary = { isDrawerOpen = true }
                            )
                        }

                        // ----------------------------------------------------
                        // Carrusel 3: Cartelera Dinámica por Personajes / Arquetipos
                        // ----------------------------------------------------
                        curatorSections.forEach { section ->
                            CuratorRow(
                                section = section,
                                onBookClick = { curatedBook ->
                                    selectedCuratedBook = curatedBook
                                },
                                onLeftAtBoundary = { isDrawerOpen = true }
                            )
                        }

                        // ----------------------------------------------------
                        // Carrusel 4: Libros Favoritos del usuario activo
                        // ----------------------------------------------------
                        if (favoriteBooks.isNotEmpty()) {
                            HomeShelf(
                                sectionTitle = "Tus Libros Favoritos",
                                icon = Icons.Default.Star,
                                books = favoriteBooks,
                                authHeader = authHeader,
                                showSeeMore = false,
                                onSeeMore = {},
                                onLeftAtBoundary = { isDrawerOpen = true },
                                isInteractive = !showDetailsModal && !showUserProfilesModal && !isDrawerOpen,
                                onBookFocused = {},
                                onBookSelected = { book ->
                                    detailsBook = book
                                    showDetailsModal = true
                                }
                            )
                        }

                        // ----------------------------------------------------
                        // Accesos Rápidos: Explorar Catálogo Completo y Memoria TV
                        // ----------------------------------------------------
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HomeQuickAccessCard(
                                title = "Explorar Biblioteca",
                                subtitle = "Ver catálogo completo de libros con filtros y búsqueda",
                                icon = Icons.Default.AutoStories,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToLibrary
                            )
                            HomeQuickAccessCard(
                                title = "Gestor de Memoria TV",
                                subtitle = "Ver libros en almacenamiento de la TV y liberar espacio",
                                icon = Icons.Default.Storage,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToYourBooks
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SIDE DRAWER LATERAL OCULTO
        // ==========================================
        TvSideDrawer(
            isOpen = isDrawerOpen,
            currentSelection = DrawerItem.HOME,
            onClose = { isDrawerOpen = false },
            onItemSelected = { item ->
                when (item) {
                    DrawerItem.HOME -> { /* Already here */ }
                    DrawerItem.BIBLIOTECA -> onNavigateToLibrary()
                    DrawerItem.TUS_LIBROS -> onNavigateToYourBooks()
                    DrawerItem.IMPORTAR_WIFI -> onNavigateToWifiImport()
                    DrawerItem.USUARIOS -> showUserProfilesModal = true
                    DrawerItem.LECTOR_3D -> onNavigateToReader()
                    DrawerItem.AJUSTES -> {
                        if (activeProfile.isKidsMode && householdPin != null) {
                            pendingProtectedAction = { onNavigateToSettings() }
                        } else {
                            onNavigateToSettings()
                        }
                    }
                    DrawerItem.OPDS -> {
                        if (activeProfile.isKidsMode && householdPin != null) {
                            pendingProtectedAction = { onNavigateToOpds() }
                        } else {
                            onNavigateToOpds()
                        }
                    }
                }
            }
        )

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
                        Box(
                            modifier = Modifier
                                .width(190.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHigh)
                                .border(1.dp, Color(0xFF333338), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (modalCover != null) {
                                Image(
                                    bitmap = modalCover,
                                    contentDescription = book.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = AmberWarm,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = book.title,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

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
    onBookFocused: (Book) -> Unit,
    onBookSelected: (Book) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Shelf Header: Title on left, [ Ver más ] button on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = sectionTitle,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
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
                        color = if (isSeeMoreFocused) AccentGold else TextPrimary,
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

        // Horizontal Carousel of Compact Cards (~30% screen height, fitting 8-9 books)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 6.dp)
        ) {
            itemsIndexed(books) { index, book ->
                val isFirst = index == 0
                CompactCoverCard(
                    book = book,
                    authHeader = authHeader,
                    isInteractive = isInteractive,
                    isFirst = isFirst,
                    onLeftAtBoundary = onLeftAtBoundary,
                    onFocused = { onBookFocused(book) },
                    onSelected = { onBookSelected(book) }
                )
            }
        }
    }
}

/**
 * Compact book card: Reduced to ~30% height (width 108dp, height 142dp for cover)
 * Fits 8-9 items horizontally with perfect clarity.
 */
@Composable
private fun CompactCoverCard(
    book: Book,
    authHeader: String?,
    isInteractive: Boolean = true,
    isFirst: Boolean = false,
    onLeftAtBoundary: () -> Unit,
    onFocused: () -> Unit,
    onSelected: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(book.coverUrl, authHeader)

    Column(
        modifier = Modifier
            .width(108.dp)
            .scale(if (isFocused && isInteractive) 1.08f else 1.0f)
            .shadow(if (isFocused && isInteractive) 14.dp else 2.dp, RoundedCornerShape(8.dp), spotColor = AccentGold)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceRaised)
            .border(
                width = if (isFocused && isInteractive) 2.5.dp else 1.dp,
                color = if (isFocused && isInteractive) AccentGold else Color(0xFF242428),
                shape = RoundedCornerShape(8.dp)
            )
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
        // Complete Poster Cover Art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF22222A), Color(0xFF131316))
                    )
                ),
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = book.title,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Real Reading Progress percentage badge (Section E)
            if (book.progressPercent > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color(0xFF09090B).copy(alpha = 0.90f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${book.progressPercent}%",
                        color = AccentGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Title and reading bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Text(
                text = book.title,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (book.shelves.isNotEmpty()) {
                Text(
                    text = "🏷 ${book.shelves.first()}",
                    color = AmberWarm,
                    fontSize = 8.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color(0xFF26262A), RoundedCornerShape(1.dp))
            ) {
                if (book.progressPercent > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progressPercent / 100f)
                            .height(2.5.dp)
                            .background(AccentGold, RoundedCornerShape(1.dp))
                    )
                }
            }
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
    val coverBmp = rememberCoverImage(heroBanner.coverUrl)
    val readBtnFocusRequester = remember { FocusRequester() }
    var isReadFocused by remember { mutableStateOf(false) }
    var isBuyFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna izquierda: Información del Bestseller y Acciones
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            // Badge Destacado
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmberWarm.copy(alpha = 0.2f))
                    .border(1.dp, AmberWarm.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "★ BESTSELLER DESTACADO DEL MES",
                    color = AmberWarm,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título Principal
            Text(
                text = heroBanner.title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Autor y Tagline
            Text(
                text = heroBanner.author.ifBlank { "Autor Recomendado" },
                color = AmberWarm,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (heroBanner.tagline.isNotBlank()) {
                Text(
                    text = heroBanner.tagline,
                    color = TextPrimary.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Sinopsis concisa
            Text(
                text = heroBanner.synopsis,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción rápida optimizados para D-pad de TV
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón 1: Muestra Gratis en 3D
                Box(
                    modifier = Modifier
                        .focusRequester(readBtnFocusRequester)
                        .scale(if (isReadFocused) 1.08f else 1.0f)
                        .shadow(if (isReadFocused) 12.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = AmberWarm)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isReadFocused) AmberWarm else SurfaceContainerHighest)
                        .border(
                            width = if (isReadFocused) 2.dp else 1.dp,
                            color = if (isReadFocused) Color.White else AmberWarm.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
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
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = if (isReadFocused) Color(0xFF131315) else AmberWarm,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Muestra Gratis en 3D",
                            color = if (isReadFocused) Color(0xFF131315) else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Botón 2: Comprar en Móvil (QR)
                Box(
                    modifier = Modifier
                        .scale(if (isBuyFocused) 1.08f else 1.0f)
                        .shadow(if (isBuyFocused) 12.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = AmberWarm)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isBuyFocused) AmberWarm else SurfaceContainer)
                        .border(
                            width = if (isBuyFocused) 2.dp else 1.dp,
                            color = if (isBuyFocused) Color.White else Color(0xFF383842),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .onFocusChanged { isBuyFocused = it.isFocused }
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                        onBuyQr()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .focusable()
                        .clickable { onBuyQr() }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = if (isBuyFocused) Color(0xFF131315) else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Comprar en Móvil (QR)",
                            color = if (isBuyFocused) Color(0xFF131315) else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Portada a la derecha con estilo 3D y sombra
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(185.dp)
                .shadow(16.dp, RoundedCornerShape(10.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceRaised)
                .border(1.5.dp, AmberWarm.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (coverBmp != null) {
                Image(
                    bitmap = coverBmp,
                    contentDescription = heroBanner.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeOffersRow(
    offers: List<BookOffer>,
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
                .padding(horizontal = 36.dp, vertical = 4.dp),
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
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
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
            contentPadding = PaddingValues(horizontal = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(offers, key = { _, o -> o.id }) { index, offer ->
                HomeOfferCard(
                    offer = offer,
                    isFirst = index == 0,
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
    onClick: () -> Unit,
    onLeftAtBoundary: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val coverBmp = rememberCoverImage(offer.coverUrl)

    Column(
        modifier = Modifier
            .width(116.dp)
            .scale(if (isFocused) 1.08f else 1.0f)
            .shadow(if (isFocused) 14.dp else 2.dp, RoundedCornerShape(8.dp), spotColor = AmberWarm)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceRaised)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) AmberWarm else Color(0xFF242428),
                shape = RoundedCornerShape(8.dp)
            )
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
        // Portada con Badge de Descuento Superpuesto
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF22222A), Color(0xFF131316))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (coverBmp != null) {
                Image(
                    bitmap = coverBmp,
                    contentDescription = offer.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = offer.title,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Badge de descuento (ej. -45%)
            if (offer.discountTag.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE53935))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = offer.discountTag,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Título y Autor
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = offer.title,
                color = if (isFocused) AmberWarm else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = offer.author,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeQuickAccessCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .scale(if (isFocused) 1.03f else 1.0f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) SurfaceContainerHighest else SurfaceContainer)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) AmberWarm else Color(0xFF2E2E34),
                shape = RoundedCornerShape(14.dp)
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
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = if (isFocused) AmberWarm else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
