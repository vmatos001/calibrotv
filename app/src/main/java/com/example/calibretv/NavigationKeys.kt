package com.example.calibretv

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SplashNavKey : NavKey

@Serializable
data object SetupWizardNavKey : NavKey

@Serializable
data object HomeNavKey : NavKey

@Serializable
data object ServerNavKey : NavKey

@Serializable
data class LibraryNavKey(
    val subfeedUrl: String? = null,
    val title: String? = null
) : NavKey

@Serializable
data object YourBooksNavKey : NavKey

@Serializable
data object SettingsNavKey : NavKey

@Serializable
data object WifiImportNavKey : NavKey

@Serializable
data class ReaderNavKey(
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val epubUrl: String? = null
) : NavKey

@Serializable
data class ComicReaderNavKey(
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val epubUrl: String? = null
) : NavKey

@Serializable
data class PdfReaderNavKey(
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val pdfUrl: String? = null
) : NavKey
