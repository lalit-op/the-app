package com.example.oneread.ui.navigation

import com.example.oneread.data.DocumentItem
import com.example.oneread.data.DocumentType

sealed class Screen {
    object Main : Screen()
    object Workspace : Screen()
    data class Files(val category: DocumentType = DocumentType.ALL) : Screen()
    data class PdfViewer(val document: DocumentItem) : Screen()
    data class PptViewer(val document: DocumentItem) : Screen()
    data class ExcelViewer(val document: DocumentItem) : Screen()
    data class TextViewer(val document: DocumentItem) : Screen()
    data class ImageViewer(val document: DocumentItem) : Screen()
    object ImageToPdf : Screen()
    object MergePdf : Screen()
    object SplitPdf : Screen()
    object Recent : Screen()
    object RecycleBin : Screen()
    object Faq : Screen()
}

enum class BottomTab(val title: String) {
    HOME("Home"),
    FAVORITES("Favorites"),
    SETTINGS("Settings")
}

enum class SortOption(val label: String) {
    DATE_DESC("Date (Newest first)"),
    DATE_ASC("Date (Oldest first)"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    SIZE_DESC("Size (Largest first)"),
    SIZE_ASC("Size (Smallest first)")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("Follow System"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}
