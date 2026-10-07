package com.example.calibretv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.curator.CuratedBook
import com.example.calibretv.data.image.rememberCoverImage
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.search.BookSearchManager
import com.example.calibretv.data.search.SearchCategory
import com.example.calibretv.data.search.UnifiedBookResult
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceCard
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.SurfaceFocused
import com.example.calibretv.theme.SurfaceRaised
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary
import com.example.calibretv.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🔍 Buscador Universal de CalibroTV (Pantalla Completa optimizada para Android TV / Fire TV).
 *
 * Características:
 * 1. Búsqueda Local en tiempo real ("Tus Libros" / Memoria TV).
 * 2. Catálogo de Descarga Gratuita Directa (Project Gutenberg, Open Library, Standard Ebooks).
 * 3. Bestsellers y Novedades con ficha 3D y enlaces QR de afiliados (Amazon, Kobo).
 * 4. Estrategia Híbrida de Portadas (Reemplazo automático con portadas HD de Open Library).
 * 5. Salvavidas Hardcover 3D con renderizado noble en tapa dura.
 * 6. Teclado en pantalla interactivo para control remoto D-Pad y chips de sugerencias instantáneas.
 */
@Composable
fun UniversalSearchModal(
    repository: BookRepository,
    isDarkTheme: Boolean = true,
    onBookSelected: (Book) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: Todos, 1: En tu TV, 2: Clásicos Gratis, 3: Bestsellers
    var isSearching by remember { mutableStateOf(false) }

    var localResults by remember { mutableStateOf<List<UnifiedBookResult>>(emptyList()) }
    var publicDomainResults by remember { mutableStateOf<List<UnifiedBookResult>>(emptyList()) }
    var commercialResults by remember { mutableStateOf<List<UnifiedBookResult>>(emptyList()) }

    var selectedBookResult by remember { mutableStateOf<UnifiedBookResult?>(null) }
    var curatedModalBook by remember { mutableStateOf<CuratedBook?>(null) }
    var showFullSummaryDialog by remember { mutableStateOf(false) }

    // Estado de descarga activa en la TV
    var downloadingBookId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadMessage by remember { mutableStateOf("") }
    val downloadedIds = remember { mutableSetOf<String>() }

    var searchJob by remember { mutableStateOf<Job?>(null) }
    val firstChipFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200L)
        try {
            firstChipFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Auto-enriquecimiento de sinopsis en español al seleccionar cualquier libro
    LaunchedEffect(selectedBookResult?.id) {
        val current = selectedBookResult ?: return@LaunchedEffect
        if (current.summary.isBlank() ||
            current.summary.contains("preservada por Project Gutenberg") ||
            current.summary.contains("Registro editorial de Open Library") ||
            current.summary.contains("Bestseller editorial registrado") ||
            current.summary.length < 50
        ) {
            val rich = BookSearchManager.fetchRichSummary(current.title, current.author)
            if (rich.isNotBlank() && selectedBookResult?.id == current.id) {
                selectedBookResult = current.copy(summary = rich)
            }
        }
    }

    // Chips de sugerencias rápidas para el control remoto
    val suggestionChips = remember {
        listOf(
            "Drácula",
            "El Principito",
            "Sherlock Holmes",
            "Don Quijote",
            "Frankenstein",
            "Alicia",
            "Edgar Allan Poe",
            "Julio Verne",
            "Stephen King"
        )
    }

    // Ejecutar búsqueda con debounce
    fun triggerSearch(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.isBlank()) {
            searchJob = scope.launch {
                isSearching = false
                val fullResults = BookSearchManager.searchAll("", repository)
                localResults = fullResults.localResults
                publicDomainResults = fullResults.publicDomainResults
                commercialResults = fullResults.commercialResults

                val allFound = localResults + publicDomainResults + commercialResults
                if (allFound.isNotEmpty() && (selectedBookResult == null || !allFound.any { it.id == selectedBookResult?.id })) {
                    selectedBookResult = allFound.first()
                }
            }
            return
        }

        searchJob = scope.launch {
            isSearching = true
            // Instantáneo local y comerciales y clásicos curados
            localResults = BookSearchManager.searchLocal(q, repository)
            commercialResults = BookSearchManager.searchCommercial(q)
            publicDomainResults = BookSearchManager.searchCuratedPublicDomain(q)

            val immediateFound = localResults + publicDomainResults + commercialResults
            if (immediateFound.isNotEmpty()) {
                selectedBookResult = immediateFound.first()
            }

            // Esperar un instante antes de llamadas remotas
            delay(200L)
            val fullResults = BookSearchManager.searchAll(q, repository)
            localResults = fullResults.localResults
            publicDomainResults = fullResults.publicDomainResults
            commercialResults = fullResults.commercialResults
            isSearching = false

            // Auto-seleccionar el primer resultado para previsualizar si no coincide
            val allFound = localResults + publicDomainResults + commercialResults
            if (allFound.isNotEmpty() && (selectedBookResult == null || !allFound.any { it.id == selectedBookResult?.id })) {
                selectedBookResult = allFound.first()
            }
        }
    }

    LaunchedEffect(searchQuery) {
        triggerSearch(searchQuery)
    }

    BackHandler {
        if (showFullSummaryDialog) {
            showFullSummaryDialog = false
        } else if (curatedModalBook != null) {
            curatedModalBook = null
        } else {
            onDismiss()
        }
    }

    val displayedResults = remember(selectedFilterIndex, localResults, publicDomainResults, commercialResults) {
        when (selectedFilterIndex) {
            1 -> localResults
            2 -> publicDomainResults
            3 -> commercialResults
            else -> localResults + publicDomainResults + commercialResults
        }
    }

    LaunchedEffect(selectedFilterIndex, displayedResults) {
        if (displayedResults.isNotEmpty() && (selectedBookResult == null || !displayedResults.any { it.id == selectedBookResult?.id })) {
            selectedBookResult = displayedResults.first()
        }
    }

    // Modal de compra / afiliado para libros comerciales
    if (curatedModalBook != null) {
        CuratedBookModal(
            book = curatedModalBook!!,
            isDownloaded = false,
            isDownloading = false,
            isDarkTheme = isDarkTheme,
            onDownload = {},
            onRead = {},
            onDismiss = { curatedModalBook = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // =========================================================================
            // COLUMNA IZQUIERDA: BUSCADOR, TECLADO RÁPIDO Y RESULTADOS (65% del ancho)
            // =========================================================================
            Column(
                modifier = Modifier
                    .weight(0.64f)
                    .fillMaxHeight()
                    .padding(start = 32.dp, top = 20.dp, end = 16.dp, bottom = 20.dp)
            ) {
                // Header Bar con botón Cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberWarm.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AmberWarm,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "BUSCADOR UNIVERSAL",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Tus Libros • Dominio Público (Gutenberg/Open Library) • Bestsellers",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Botón Salir / Volver
                    var isBackFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBackFocused) AmberWarm else SurfaceRaised)
                            .border(1.dp, if (isBackFocused) AmberWarm else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .focusable()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = if (isBackFocused) Color(0xFF131316) else TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Atrás",
                            color = if (isBackFocused) Color(0xFF131316) else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Visor de Entrada de Búsqueda (Sin activar teclado virtual de Fire OS)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(
                            width = 1.dp,
                            color = if (searchQuery.isNotEmpty()) AmberWarm.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (searchQuery.isNotEmpty()) AmberWarm else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Escribe abajo con el control o elige una sugerencia...",
                                    color = TextMuted.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            } else {
                                Text(
                                    text = searchQuery,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (searchQuery.isNotEmpty()) {
                            var isClearFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isClearFocused) AmberWarm else Color.White.copy(alpha = 0.1f))
                                    .clickable {
                                        searchQuery = ""
                                        selectedBookResult = null
                                    }
                                    .onFocusChanged { isClearFocused = it.isFocused }
                                    .focusable()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = if (isClearFocused) Color.Black else TextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Limpiar",
                                        color = if (isClearFocused) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (isSearching) {
                            CircularProgressIndicator(
                                color = AmberWarm,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fila de Sugerencias Rápidas (Chips accionables con 1 toque)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    suggestionChips.forEachIndexed { idx, chip ->
                        var isChipFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .then(if (idx == 0) Modifier.focusRequester(firstChipFocusRequester) else Modifier)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isChipFocused) AmberWarm else SurfaceRaised)
                                .border(
                                    width = 1.dp,
                                    color = if (isChipFocused) AmberWarm else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    searchQuery = chip
                                    triggerSearch(chip)
                                }
                                .onFocusChanged { isChipFocused = it.isFocused }
                                .focusable()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chip,
                                color = if (isChipFocused) Color(0xFF131316) else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Teclado Alfabético Rápido para TV (A-Z, Espacio, Borrar)
                TvQuickKeyboard(
                    onLetterClick = { letter -> searchQuery += letter },
                    onSpaceClick = { searchQuery += " " },
                    onBackspaceClick = { if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1) },
                    onClearClick = { searchQuery = "" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filtros de Categorías
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterTitles = listOf(
                        "Todos (${localResults.size + publicDomainResults.size + commercialResults.size})",
                        "En tu TV (${localResults.size})",
                        "Dominio Público (${publicDomainResults.size})",
                        "Bestsellers (${commercialResults.size})"
                    )

                    filterTitles.forEachIndexed { idx, title ->
                        var isTabFocused by remember { mutableStateOf(false) }
                        val isSelected = selectedFilterIndex == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isTabFocused) AmberWarm
                                    else if (isSelected) AmberWarm.copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isTabFocused || isSelected) AmberWarm else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFilterIndex = idx }
                                .onFocusChanged { isTabFocused = it.isFocused }
                                .focusable()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (isTabFocused) Color(0xFF131316) else if (isSelected) AmberWarm else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected || isTabFocused) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lista de Resultados de Búsqueda
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (displayedResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (searchQuery.isBlank()) Icons.Default.Search else Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = TextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = if (searchQuery.isBlank())
                                        "Selecciona una sugerencia o escribe para buscar en tu TV y en bibliotecas de internet"
                                    else
                                        "No se encontraron libros para «$searchQuery»",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(displayedResults) { _, item ->
                                var isCardFocused by remember { mutableStateOf(false) }
                                val isSelected = selectedBookResult?.id == item.id
                                val isAlreadyDownloaded = item.isDownloaded || downloadedIds.contains(item.id)
                                val coverBitmap = rememberCoverImage(item.coverUrl)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCardFocused) AmberWarm.copy(alpha = 0.12f) else Color.Transparent)
                                        .border(
                                            width = if (isCardFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                                            color = if (isCardFocused) AmberWarm else if (isSelected) AmberWarm.copy(alpha = 0.4f) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (selectedBookResult?.id == item.id) {
                                                when {
                                                    item.category == SearchCategory.LOCAL || isAlreadyDownloaded -> {
                                                        val target = item.localBook 
                                                            ?: repository.getCachedBooks().find { it.id == item.id || it.title.equals(item.title, ignoreCase = true) }
                                                            ?: Book(
                                                                id = item.id,
                                                                title = item.title,
                                                                author = item.author,
                                                                coverUrl = item.coverUrl,
                                                                epubUrl = item.downloadUrl,
                                                                summary = item.summary
                                                            )
                                                        onBookSelected(target)
                                                    }
                                                    item.category == SearchCategory.PUBLIC_DOMAIN -> {
                                                        scope.launch {
                                                            downloadingBookId = item.id
                                                            downloadMessage = "Descargando..."
                                                            downloadProgress = 0.05f
                                                            val downloadResult = BookSearchManager.downloadAndInstallBook(
                                                                context = context,
                                                                repository = repository,
                                                                result = item,
                                                                onProgress = { p ->
                                                                    downloadProgress = p
                                                                    downloadMessage = "Descargando ${(p * 100).toInt()}%..."
                                                                }
                                                            )
                                                            if (downloadResult.isSuccess) {
                                                                downloadedIds.add(item.id)
                                                                downloadMessage = "¡Completado!"
                                                                val downloadedBook = downloadResult.getOrNull()
                                                                if (downloadedBook != null) {
                                                                    selectedBookResult = item.copy(
                                                                        isDownloaded = true,
                                                                        localBook = downloadedBook
                                                                    )
                                                                }
                                                            } else {
                                                                downloadMessage = "Error al descargar"
                                                            }
                                                            delay(600L)
                                                            downloadingBookId = null
                                                        }
                                                    }
                                                    item.category == SearchCategory.COMMERCIAL -> {
                                                        curatedModalBook = item.curatedBook
                                                    }
                                                }
                                            } else {
                                                selectedBookResult = item
                                            }
                                        }
                                        .onFocusChanged {
                                            isCardFocused = it.isFocused
                                            if (it.isFocused) {
                                                selectedBookResult = item
                                            }
                                        }
                                        .focusable()
                                        .padding(6.dp)
                                ) {
                                    // Portada Hardcover 3D con indicador
                                    Book3DView(
                                        coverBitmap = coverBitmap,
                                        title = item.title,
                                        width = 96.dp,
                                        height = 142.dp,
                                        isFocused = isCardFocused,
                                        badgeText = when (item.category) {
                                            SearchCategory.LOCAL -> "TV"
                                            SearchCategory.PUBLIC_DOMAIN -> "GRATIS"
                                            SearchCategory.COMMERCIAL -> "TIENDA"
                                        },
                                        badgeColor = when (item.category) {
                                            SearchCategory.LOCAL -> Color(0xFF4CAF50)
                                            SearchCategory.PUBLIC_DOMAIN -> AmberWarm
                                            SearchCategory.COMMERCIAL -> Color(0xFF9C27B0)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = item.title,
                                        color = if (isCardFocused) AmberWarm else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = item.author,
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )

                                    if (isCardFocused) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = when {
                                                item.category == SearchCategory.LOCAL || isAlreadyDownloaded -> "▶ Leer [OK]"
                                                item.category == SearchCategory.PUBLIC_DOMAIN -> "☁ Bajar [OK]"
                                                else -> "🛒 Ver [OK]"
                                            },
                                            color = when {
                                                item.category == SearchCategory.LOCAL || isAlreadyDownloaded -> Color(0xFF81C784)
                                                item.category == SearchCategory.PUBLIC_DOMAIN -> AmberWarm
                                                else -> Color(0xFFCE93D8)
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // COLUMNA DERECHA: PANEL DETALLADO DE ACCIÓN Y FICHA EDITORIAL (36% del ancho)
            // =========================================================================
            Box(
                modifier = Modifier
                    .weight(0.36f)
                    .fillMaxHeight()
                    .background(Color(0xFF131317))
                    .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f))
                    .padding(24.dp)
            ) {
                if (selectedBookResult == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Selecciona un libro para ver detalles y opciones de lectura o descarga.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val book = selectedBookResult!!
                    val isDownloaded = book.isDownloaded || downloadedIds.contains(book.id)
                    val isCurrentlyDownloading = downloadingBookId == book.id

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Badge de Origen
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            when (book.category) {
                                                SearchCategory.LOCAL -> Color(0xFF4CAF50)
                                                SearchCategory.PUBLIC_DOMAIN -> AmberWarm
                                                SearchCategory.COMMERCIAL -> Color(0xFFAB47BC)
                                            },
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = book.sourceName.uppercase(),
                                    color = when (book.category) {
                                        SearchCategory.LOCAL -> Color(0xFF81C784)
                                        SearchCategory.PUBLIC_DOMAIN -> AmberWarm
                                        SearchCategory.COMMERCIAL -> Color(0xFFCE93D8)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = book.title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 22.sp,
                                fontFamily = FontFamily.Serif
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "por ${book.author}",
                                color = AmberWarm,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Caja de Sinopsis / Descripción
                            val isLongSummary = book.summary.length > 150 || book.summary.lines().size > 4
                            var isSummaryCardFocused by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSummaryCardFocused) SurfaceFocused else SurfaceCard)
                                    .border(
                                        width = if (isSummaryCardFocused) 1.5.dp else 1.dp,
                                        color = if (isSummaryCardFocused) AmberWarm else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .then(
                                        if (isLongSummary) {
                                            Modifier
                                                .clickable { showFullSummaryDialog = true }
                                                .onFocusChanged { isSummaryCardFocused = it.isFocused }
                                                .focusable()
                                        } else Modifier
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (book.summary.isNotBlank()) book.summary else "Una obra literaria indispensable disponible para disfrutar en pantalla grande con CalibroTV 3D.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (isLongSummary) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isSummaryCardFocused) "▶ [OK] Leer reseña completa..." else "📖 Leer más...",
                                                color = AmberWarm,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // BOTONES DE ACCIÓN SEGÚN CATEGORÍA
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when {
                                // 1. CASO LOCAL (En tu TV / Biblioteca local) o ya descargado
                                book.category == SearchCategory.LOCAL || isDownloaded -> {
                                    var isReadFocused by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isReadFocused) AmberWarm else Color(0xFF2E7D32))
                                            .clickable {
                                                val target = book.localBook 
                                                    ?: repository.getCachedBooks().find { it.id == book.id || it.title.equals(book.title, ignoreCase = true) }
                                                    ?: Book(
                                                        id = book.id,
                                                        title = book.title,
                                                        author = book.author,
                                                        coverUrl = book.coverUrl,
                                                        epubUrl = book.downloadUrl,
                                                        summary = book.summary
                                                    )
                                                onBookSelected(target)
                                            }
                                            .onFocusChanged { isReadFocused = it.isFocused }
                                            .focusable()
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = if (isReadFocused) Color(0xFF131316) else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Abrir y Leer Ahora",
                                                color = if (isReadFocused) Color(0xFF131316) else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // 2. CASO DOMINIO PÚBLICO (Descarga Directa Gratuita en TV)
                                book.category == SearchCategory.PUBLIC_DOMAIN -> {
                                    if (isCurrentlyDownloading) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SurfaceCard)
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = downloadMessage,
                                                    color = AmberWarm,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${(downloadProgress * 100).toInt()}%",
                                                    color = TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { downloadProgress },
                                                color = AmberWarm,
                                                trackColor = Color.White.copy(alpha = 0.1f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            )
                                        }
                                    } else {
                                        var isDownloadFocused by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isDownloadFocused) AmberWarm else Color(0xFF1976D2))
                                                .clickable {
                                                    scope.launch {
                                                        downloadingBookId = book.id
                                                        downloadMessage = "Descargando EPUB en TV..."
                                                        downloadProgress = 0.05f

                                                        val downloadResult = BookSearchManager.downloadAndInstallBook(
                                                            context = context,
                                                            repository = repository,
                                                            result = book,
                                                            onProgress = { p ->
                                                                downloadProgress = p
                                                                downloadMessage = "Descargando ${(p * 100).toInt()}%..."
                                                            }
                                                        )

                                                        if (downloadResult.isSuccess) {
                                                            downloadedIds.add(book.id)
                                                            downloadMessage = "¡Completado!"
                                                            val downloadedBook = downloadResult.getOrNull()
                                                            if (downloadedBook != null) {
                                                                selectedBookResult = book.copy(
                                                                    isDownloaded = true,
                                                                    localBook = downloadedBook
                                                                )
                                                            }
                                                        } else {
                                                            downloadMessage = "Error al descargar"
                                                        }
                                                        delay(600L)
                                                        downloadingBookId = null
                                                    }
                                                }
                                                .onFocusChanged { isDownloadFocused = it.isFocused }
                                                .focusable()
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDownload,
                                                    contentDescription = null,
                                                    tint = if (isDownloadFocused) Color(0xFF131316) else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Descargar en TV (Gratis)",
                                                    color = if (isDownloadFocused) Color(0xFF131316) else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. CASO COMERCIAL (Bestseller BookSpread con QR de compra móvil)
                                else -> {
                                    var isBuyFocused by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isBuyFocused) AmberWarm else Color(0xFF6A1B9A))
                                            .clickable {
                                                val cBook = book.curatedBook ?: CuratedBook(
                                                    id = book.id,
                                                    title = book.title,
                                                    author = book.author,
                                                    coverUrl = book.coverUrl ?: "",
                                                    summary = book.summary,
                                                    category = "Bestseller",
                                                    isPublicDomain = false,
                                                    affiliateQrUrl = "https://www.amazon.com/s?k=${book.title}+${book.author}",
                                                    approximatePrice = "Ver en tienda",
                                                    difficultyLevel = 4
                                                )
                                                curatedModalBook = cBook
                                            }
                                            .onFocusChanged { isBuyFocused = it.isFocused }
                                            .focusable()
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = if (isBuyFocused) Color(0xFF131316) else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Ver Ofertas y QR Móvil",
                                                color = if (isBuyFocused) Color(0xFF131316) else Color.White,
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
        }

        // Modal Flotante de Reseña y Sinopsis Completa con Navegación D-Pad
        if (showFullSummaryDialog && selectedBookResult != null) {
            val currentSummaryBook = selectedBookResult!!
            val closeBtnFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(120L)
                try { closeBtnFocusRequester.requestFocus() } catch (_: Exception) {}
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showFullSummaryDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight(0.82f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BackgroundDark)
                        .border(1.5.dp, AmberWarm.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .clickable(enabled = false) {}
                        .padding(28.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "RESEÑA Y SINOPSIS EDITORIAL",
                                color = AmberWarm,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentSummaryBook.title,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "por ${currentSummaryBook.author}",
                                color = AmberWarm,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentSummaryBook.summary,
                                color = TextPrimary.copy(alpha = 0.92f),
                                fontSize = 13.sp,
                                lineHeight = 22.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        var isCloseSummaryFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCloseSummaryFocused) AmberWarm else SurfaceRaised)
                                .border(1.dp, if (isCloseSummaryFocused) AmberWarm else Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .clickable { showFullSummaryDialog = false }
                                .onFocusChanged { isCloseSummaryFocused = it.isFocused }
                                .focusRequester(closeBtnFocusRequester)
                                .focusable()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Volver al libro (Cerrar reseña)",
                                color = if (isCloseSummaryFocused) Color(0xFF131316) else TextPrimary,
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

/**
 * Teclado en pantalla ágil para control remoto D-Pad (A-Z, Espacio, Borrar).
 */
@Composable
private fun TvQuickKeyboard(
    onLetterClick: (String) -> Unit,
    onSpaceClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val row1 = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M")
    val row2 = listOf("N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Fila 1 (A-M)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            row1.forEach { char ->
                TvKeyButton(char = char, modifier = Modifier.weight(1f)) { onLetterClick(char) }
            }
        }

        // Fila 2 (N-Z) + Teclas Especiales (Espacio, Borrar)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            row2.forEach { char ->
                TvKeyButton(char = char, modifier = Modifier.weight(1f)) { onLetterClick(char) }
            }

            // Tecla Espacio
            var isSpaceFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(2f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSpaceFocused) AmberWarm else Color.White.copy(alpha = 0.08f))
                    .border(1.dp, if (isSpaceFocused) AmberWarm else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable { onSpaceClick() }
                    .onFocusChanged { isSpaceFocused = it.isFocused }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ESPACIO",
                    color = if (isSpaceFocused) Color.Black else TextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tecla Borrar
            var isBackFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isBackFocused) AmberWarm else Color.White.copy(alpha = 0.08f))
                    .border(1.dp, if (isBackFocused) AmberWarm else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable { onBackspaceClick() }
                    .onFocusChanged { isBackFocused = it.isFocused }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Borrar",
                    tint = if (isBackFocused) Color.Black else TextPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun TvKeyButton(
    char: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) AmberWarm else Color.White.copy(alpha = 0.08f))
            .border(1.dp, if (isFocused) AmberWarm else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            color = if (isFocused) Color.Black else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
