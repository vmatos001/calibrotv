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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.image.CoverLoader
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.model.OpdsCategory
import com.example.calibretv.data.opds.OpdsFeedContent
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceContainerHigh
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.ui.components.Book3DView
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.components.TvTopBar

@Composable
fun LibraryScreen(
    repository: BookRepository,
    onBookSelected: (Book) -> Unit,
    onTabSelected: (TvNavTab) -> Unit,
    initialFeedUrl: String? = null
) {
    var feedContent by remember { mutableStateOf<OpdsFeedContent?>(null) }
    var currentUrl by remember { mutableStateOf(initialFeedUrl) }
    var historyUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var focusedBook by remember { mutableStateOf<Book?>(null) }
    var activeProfile by remember { mutableStateOf(repository.getActiveProfile()) }

    // Modal state for Book Details
    var showDetailsModal by remember { mutableStateOf(false) }
    var detailsBook by remember { mutableStateOf<Book?>(null) }
    var modalDescription by remember { mutableStateOf("") }
    val modalReadFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = showDetailsModal) {
        showDetailsModal = false
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

    // Fetch feed content when currentUrl or activeProfile changes
    LaunchedEffect(currentUrl, activeProfile) {
        isLoading = true
        val result = repository.getFeed(currentUrl)
        feedContent = result
        if (result.books.isNotEmpty()) {
            focusedBook = result.books.first()
        }
        isLoading = false
    }

    val allBooks = feedContent?.books ?: emptyList()

    // Filtros de Shelves de Calibre-Web dinámicos:
    // Personajes (filtros primarios) + otras clasificaciones (secundarias)
    val filterItems = remember(allBooks) {
        val shelvesFromBooks = allBooks.flatMap { it.shelves }.distinct()
        val charShelves = shelvesFromBooks.filter { com.example.calibretv.data.opds.OpdsClient.isCharacterShelfName(it) }.sorted()
        val otherShelves = shelvesFromBooks.filter { !com.example.calibretv.data.opds.OpdsClient.isCharacterShelfName(it) }.sorted()
        val remainingTags = allBooks.flatMap { it.tags.ifEmpty { listOf(it.category) } }
            .distinct()
            .filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) && !shelvesFromBooks.contains(it) }

        listOf("Todos") + charShelves + otherShelves + remainingTags
    }

    val filteredBooks = remember(allBooks, selectedCategory) {
        if (selectedCategory == "Todos" || selectedCategory.isBlank()) allBooks
        else allBooks.filter { b ->
            b.shelves.any { it.equals(selectedCategory, ignoreCase = true) } ||
            b.category.equals(selectedCategory, ignoreCase = true) ||
            b.tags.any { it.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val continuingBooks = remember(allBooks) {
        allBooks.filter { it.progressPercent > 0 }
    }

    // Dynamic background cover based on focused book
    val backdropCoverBmp = rememberCoverImage(focusedBook?.coverUrl, authHeader)

    fun switchProfile() {
        val profiles = repository.getProfiles()
        val curIdx = profiles.indexOfFirst { it.id == activeProfile.id }
        val nextProfile = profiles[(curIdx + 1) % profiles.size]
        activeProfile = nextProfile
        repository.saveActiveProfile(nextProfile)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
        ) {
            // ==========================================
            // ZONA A: SUPERIOR ESTÁTICA CON BACKDROP Y DEGRADADO NEGRO
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f)
                    .background(BackgroundDark)
            ) {
                // 1. Dynamic cover artwork backdrop
                if (backdropCoverBmp != null) {
                    Image(
                        bitmap = backdropCoverBmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.32f }
                    )
                }

                // 2. Black/Dark horizontal gradient overlay so text on the left stays sharply legible
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BackgroundDark,
                                    BackgroundDark.copy(alpha = 0.96f),
                                    BackgroundDark.copy(alpha = 0.75f),
                                    BackgroundDark.copy(alpha = 0.30f),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = 1350f
                            )
                        )
                )

                // 3. Black/Dark vertical gradient at the bottom towards Zone B
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    BackgroundDark.copy(alpha = 0.45f),
                                    BackgroundDark
                                )
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    // TopBar
                    TvTopBar(
                        currentTab = TvNavTab.BIBLIOTECA,
                        onTabSelected = onTabSelected,
                        activeProfile = activeProfile,
                        onProfileClick = ::switchProfile
                    )

                    // Hero Spotlight Compacto
                    focusedBook?.let { book ->
                        CompactHeroSection(
                            book = book,
                            authHeader = authHeader,
                            onRead = { onBookSelected(book) },
                            onDetails = {
                                detailsBook = book
                                showDetailsModal = true
                            }
                        )
                    }
                }
            }

            // ==========================================
            // ZONA B: CÁPSULAS / CHIPS COMPACTAS
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(BackgroundDark)
                    .padding(horizontal = 36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (historyUrls.isNotEmpty()) {
                        item {
                            TvCapsuleChip(
                                title = "‹ Volver",
                                icon = Icons.Default.Folder,
                                isSelected = false,
                                onClick = {
                                    val previous = historyUrls.lastOrNull()
                                    historyUrls = historyUrls.dropLast(1)
                                    currentUrl = previous
                                }
                            )
                        }
                    }

                    items(filterItems) { tag ->
                        val isChar = com.example.calibretv.data.opds.OpdsClient.isCharacterShelfName(tag)
                        TvCapsuleChip(
                            title = tag,
                            icon = when {
                                tag == "Todos" -> Icons.Default.Folder
                                isChar -> Icons.Default.Person
                                else -> Icons.Default.Sell
                            },
                            isSelected = selectedCategory == tag,
                            onClick = { selectedCategory = tag }
                        )
                    }
                }
            }

            // ==========================================
            // ZONA C: ÁREA PRINCIPAL CON SCROLL Y PORTADAS COMPLETAS
            // Bottom padding de 90dp para que nunca se corten las portadas inferiores
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(BackgroundDark)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanElectric)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                            .padding(top = 4.dp, bottom = 90.dp)
                    ) {
                        // Carrusel 1: Continuar Leyendo (Movimiento Horizontal)
                        if (continuingBooks.isNotEmpty()) {
                            ShelfCarousel(
                                sectionTitle = "Continuar Leyendo",
                                icon = Icons.Default.History,
                                books = continuingBooks,
                                authHeader = authHeader,
                                isInteractive = !showDetailsModal,
                                onBookFocused = { focusedBook = it },
                                onBookSelected = { book ->
                                    detailsBook = book
                                    showDetailsModal = true
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Carrusel 2: Libros de tu biblioteca (Movimiento Horizontal)
                        ShelfCarousel(
                            sectionTitle = if (selectedCategory == "Todos") "Libros de tu biblioteca" else selectedCategory,
                            icon = Icons.Default.MenuBook,
                            books = filteredBooks,
                            authHeader = authHeader,
                            isInteractive = !showDetailsModal,
                            onBookFocused = { focusedBook = it },
                            onBookSelected = { book ->
                                detailsBook = book
                                showDetailsModal = true
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // MODAL DE DETALLES DEL LIBRO (Caja Stitch)
        // Presenta detalles y botón de descarga/lectura bajo demanda
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
                        .border(1.5.dp, CyanElectric.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .clickable(enabled = false) {} // Prevent background click from dismissing
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
                                                Text(text = "#$tag", color = CyanElectric, fontSize = 11.sp)
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
                                HeroCapsuleButton(
                                    title = "Leer en 3D",
                                    icon = Icons.Default.MenuBook,
                                    isPrimary = true,
                                    modifier = Modifier.focusRequester(modalReadFocusRequester),
                                    onClick = {
                                        showDetailsModal = false
                                        onBookSelected(book)
                                    }
                                )
                                HeroCapsuleButton(
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
    }
}

@Composable
private fun CompactHeroSection(
    book: Book,
    authHeader: String?,
    onRead: () -> Unit,
    onDetails: () -> Unit
) {
    val coverBmp = rememberCoverImage(book.coverUrl, authHeader)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail poster on the left of Zone A
        Book3DView(
            coverBitmap = coverBmp,
            title = book.title,
            width = 85.dp,
            height = 125.dp,
            isFocused = false,
            enable3DStandby = false
        )

        // Book details & prominent action buttons
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    modifier = Modifier.size(13.dp)
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
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Author & Category
            Text(
                text = "${book.author} • ${book.category}",
                color = CyanElectric,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Synopsis (compact 1-2 lines)
            Text(
                text = book.summary,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons (Inverted: now large capsule pills with generous sizing)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroCapsuleButton(
                    title = "Leer en 3D",
                    icon = Icons.Default.MenuBook,
                    isPrimary = true,
                    onClick = onRead
                )
                HeroCapsuleButton(
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
private fun ShelfCarousel(
    sectionTitle: String,
    icon: ImageVector,
    books: List<Book>,
    authHeader: String?,
    isInteractive: Boolean = true,
    onBookFocused: (Book) -> Unit,
    onBookSelected: (Book) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 4.dp),
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
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = sectionTitle,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${books.size} títulos",
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 6.dp)
        ) {
            items(books) { book ->
                FullCoverCard(
                    book = book,
                    authHeader = authHeader,
                    isInteractive = isInteractive,
                    onFocused = { onBookFocused(book) },
                    onSelected = { onBookSelected(book) }
                )
            }
        }
    }
}

@Composable
private fun FullCoverCard(
    book: Book,
    authHeader: String?,
    isInteractive: Boolean = true,
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

        // Title and reading bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        ) {
            Text(
                text = book.title,
                color = if (isFocused) AmberWarm else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = book.author.ifBlank { "Biblioteca" },
                color = TextMuted,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (book.progressPercent > 0) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(Color(0xFF26262A), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progressPercent / 100f)
                            .height(2.5.dp)
                            .background(AmberWarm, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun TvCapsuleChip(
    title: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .height(30.dp)
            .scale(if (isFocused) 1.05f else 1.0f)
            .shadow(if (isFocused) 6.dp else 0.dp, RoundedCornerShape(15.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(15.dp))
            .background(
                when {
                    isFocused -> CyanElectric
                    isSelected -> AmberWarm
                    else -> SurfaceContainerHigh
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) TextPrimary else if (isSelected) AmberWarm else Color.Transparent,
                shape = RoundedCornerShape(15.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 3.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected || isFocused) Color(0xFF131315) else CyanElectric,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = title,
            color = if (isSelected || isFocused) Color(0xFF131315) else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun HeroCapsuleButton(
    title: String,
    icon: ImageVector,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .height(44.dp)
            .wrapContentWidth()
            .scale(if (isFocused) 1.06f else 1.0f)
            .shadow(if (isFocused) 12.dp else 0.dp, RoundedCornerShape(24.dp), spotColor = CyanElectric)
            .clip(RoundedCornerShape(24.dp))
            .background(
                when {
                    isFocused -> CyanElectric
                    isPrimary -> AmberWarm
                    else -> SurfaceContainerHigh
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) TextPrimary else Color(0xFF3E3E44),
                shape = RoundedCornerShape(24.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPrimary || isFocused) Color(0xFF131315) else TextPrimary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = if (isPrimary || isFocused) Color(0xFF131315) else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}
