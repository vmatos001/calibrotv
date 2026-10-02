package com.example.calibretv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.model.Book
import com.example.calibretv.ui.components.TvNavTab
import com.example.calibretv.ui.screens.ComicReaderScreen
import com.example.calibretv.ui.screens.HomeScreen
import com.example.calibretv.ui.screens.LibraryGridScreen
import com.example.calibretv.ui.screens.PdfReaderScreen
import com.example.calibretv.ui.screens.ReaderScreen
import com.example.calibretv.ui.screens.ServerScreen
import com.example.calibretv.ui.screens.SettingsScreen
import com.example.calibretv.ui.screens.SplashScreen
import com.example.calibretv.ui.screens.SetupWizardScreen
import com.example.calibretv.ui.screens.YourBooksScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val repository = remember { BookRepository(context) }

    // Start with branded Splash screen
    val backStack = rememberNavBackStack(SplashNavKey)

    fun openBook(book: Book) {
        val isPdf = book.epubUrl?.endsWith(".pdf", ignoreCase = true) == true
        val isComic = book.epubUrl?.let { it.endsWith(".cbz", ignoreCase = true) || it.endsWith(".cbr", ignoreCase = true) } == true

        when {
            isPdf -> {
                backStack.add(
                    PdfReaderNavKey(
                        bookId = book.id,
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        pdfUrl = book.epubUrl
                    )
                )
            }
            isComic -> {
                backStack.add(
                    ComicReaderNavKey(
                        bookId = book.id,
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        epubUrl = book.epubUrl
                    )
                )
            }
            else -> {
                backStack.add(
                    ReaderNavKey(
                        bookId = book.id,
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        epubUrl = book.epubUrl
                    )
                )
            }
        }
    }

    fun navigateToReaderForLastBook() {
        val lastBook = repository.getLastOpenedBook() ?: repository.getCachedBooks().firstOrNull()
        if (lastBook != null &&
            backStack.lastOrNull() !is ReaderNavKey &&
            backStack.lastOrNull() !is ComicReaderNavKey &&
            backStack.lastOrNull() !is PdfReaderNavKey
        ) {
            openBook(lastBook)
        } else if (lastBook == null && backStack.lastOrNull() !is HomeNavKey) {
            backStack.add(HomeNavKey)
        }
    }

    fun navigateToTab(tab: TvNavTab) {
        when (tab) {
            TvNavTab.HOME -> {
                if (backStack.lastOrNull() !is HomeNavKey) {
                    backStack.add(HomeNavKey)
                }
            }
            TvNavTab.BIBLIOTECA -> {
                if (backStack.lastOrNull() !is LibraryNavKey) {
                    backStack.add(LibraryNavKey())
                }
            }
            TvNavTab.TUS_LIBROS -> {
                if (backStack.lastOrNull() !is YourBooksNavKey) {
                    backStack.add(YourBooksNavKey)
                }
            }
            TvNavTab.LECTOR_3D -> navigateToReaderForLastBook()
            TvNavTab.AJUSTES -> {
                if (backStack.lastOrNull() !is SettingsNavKey) {
                    backStack.add(SettingsNavKey)
                }
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<SplashNavKey> {
                SplashScreen(
                    repository = repository,
                    onNavigateNext = { isSetupCompleted ->
                        if (isSetupCompleted) {
                            backStack.add(HomeNavKey)
                        } else {
                            backStack.add(SetupWizardNavKey)
                        }
                    }
                )
            }
            entry<SetupWizardNavKey> {
                SetupWizardScreen(
                    repository = repository,
                    onSetupFinished = {
                        backStack.add(HomeNavKey)
                    }
                )
            }
            entry<HomeNavKey> {
                HomeScreen(
                    repository = repository,
                    onBookSelected = ::openBook,
                    onNavigateToLibrary = {
                        if (backStack.lastOrNull() !is LibraryNavKey) {
                            backStack.add(LibraryNavKey())
                        }
                    },
                    onNavigateToYourBooks = {
                        if (backStack.lastOrNull() !is YourBooksNavKey) {
                            backStack.add(YourBooksNavKey)
                        }
                    },
                    onNavigateToSettings = {
                        if (backStack.lastOrNull() !is SettingsNavKey) {
                            backStack.add(SettingsNavKey)
                        }
                    },
                    onNavigateToOpds = {
                        if (backStack.lastOrNull() !is ServerNavKey) {
                            backStack.add(ServerNavKey)
                        }
                    },
                    onNavigateToReader = ::navigateToReaderForLastBook,
                    onNavigateToWifiImport = {
                        if (backStack.lastOrNull() !is WifiImportNavKey) {
                            backStack.add(WifiImportNavKey)
                        }
                    }
                )
            }
            entry<LibraryNavKey> {
                LibraryGridScreen(
                    repository = repository,
                    onBookSelected = ::openBook,
                    onNavigateToHome = {
                        if (backStack.lastOrNull() !is HomeNavKey) {
                            backStack.add(HomeNavKey)
                        }
                    },
                    onNavigateToYourBooks = {
                        if (backStack.lastOrNull() !is YourBooksNavKey) {
                            backStack.add(YourBooksNavKey)
                        }
                    },
                    onNavigateToSettings = {
                        if (backStack.lastOrNull() !is SettingsNavKey) {
                            backStack.add(SettingsNavKey)
                        }
                    },
                    onNavigateToOpds = {
                        if (backStack.lastOrNull() !is ServerNavKey) {
                            backStack.add(ServerNavKey)
                        }
                    },
                    onNavigateToReader = ::navigateToReaderForLastBook,
                    onNavigateToWifiImport = {
                        if (backStack.lastOrNull() !is WifiImportNavKey) {
                            backStack.add(WifiImportNavKey)
                        }
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<YourBooksNavKey> {
                YourBooksScreen(
                    repository = repository,
                    onBookSelected = ::openBook,
                    onNavigateToHome = { navigateToTab(TvNavTab.HOME) },
                    onNavigateToLibrary = { navigateToTab(TvNavTab.BIBLIOTECA) },
                    onNavigateToReader = ::navigateToReaderForLastBook,
                    onNavigateToSettings = { navigateToTab(TvNavTab.AJUSTES) },
                    onNavigateToWifiImport = {
                        if (backStack.lastOrNull() !is WifiImportNavKey) {
                            backStack.add(WifiImportNavKey)
                        }
                    }
                )
            }
            entry<ServerNavKey> {
                ServerScreen(
                    repository = repository,
                    onConnected = {
                        backStack.add(HomeNavKey)
                    },
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                    onTabSelected = ::navigateToTab
                )
            }
            entry<SettingsNavKey> {
                SettingsScreen(
                    repository = repository,
                    onTabSelected = ::navigateToTab,
                    onOpenOpds = {
                        backStack.add(ServerNavKey)
                    },
                    onRunSetupWizard = {
                        backStack.add(SetupWizardNavKey)
                    },
                    onSaved = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            entry<WifiImportNavKey> {
                com.example.calibretv.ui.screens.WifiImportScreen(
                    repository = repository,
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            entry<ReaderNavKey> { key ->
                val book = remember(key) {
                    Book(
                        id = key.bookId,
                        title = key.bookTitle,
                        author = key.bookAuthor,
                        epubUrl = key.epubUrl
                    )
                }
                ReaderScreen(
                    book = book,
                    repository = repository,
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                    onTabSelected = ::navigateToTab
                )
            }
            entry<ComicReaderNavKey> { key ->
                val book = remember(key) {
                    Book(
                        id = key.bookId,
                        title = key.bookTitle,
                        author = key.bookAuthor,
                        epubUrl = key.epubUrl
                    )
                }
                ComicReaderScreen(
                    book = book,
                    repository = repository,
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            entry<PdfReaderNavKey> { key ->
                val book = remember(key) {
                    Book(
                        id = key.bookId,
                        title = key.bookTitle,
                        author = key.bookAuthor,
                        epubUrl = key.pdfUrl
                    )
                }
                PdfReaderScreen(
                    book = book,
                    repository = repository,
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}
