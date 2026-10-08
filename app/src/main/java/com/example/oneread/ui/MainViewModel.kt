package com.example.oneread.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneread.data.AppDatabase
import com.example.oneread.data.DocumentItem
import com.example.oneread.data.DocumentRepository
import com.example.oneread.data.DocumentType
import com.example.oneread.ui.navigation.AppThemeMode
import com.example.oneread.ui.navigation.BottomTab
import com.example.oneread.ui.navigation.Screen
import com.example.oneread.ui.navigation.SortOption
import com.example.oneread.util.KeepScreenOnManager
import com.example.oneread.workspace.model.DocumentFormat
import com.example.oneread.workspace.model.DocumentTab
import com.example.oneread.workspace.tab.TabManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DirectoryInfo(
    val name: String,
    val count: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = DocumentRepository(application, database.documentDao())
    val tabManager = TabManager(application)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Main)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(BottomTab.HOME)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DocumentType.ALL)
    val selectedCategory: StateFlow<DocumentType> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Centralized persistent KeepScreenOn preference
    val keepScreenOn: StateFlow<Boolean> = KeepScreenOnManager.keepScreenOn

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun setKeepScreenOn(enabled: Boolean) {
        KeepScreenOnManager.setKeepScreenOn(enabled)
    }

    fun setSelectedLanguage(language: String) {
        _selectedLanguage.value = language
    }

    // Resume Card Dismissal Persistence
    private val homePrefs: SharedPreferences =
        application.getSharedPreferences("hr_read_home_prefs", Context.MODE_PRIVATE)

    private val _dismissedResumeTabIds = MutableStateFlow<Set<String>>(
        homePrefs.getStringSet("dismissed_resume_tabs", emptySet()) ?: emptySet()
    )
    val dismissedResumeTabIds: StateFlow<Set<String>> = _dismissedResumeTabIds.asStateFlow()

    fun dismissResumeCard(tabId: String) {
        val updated = _dismissedResumeTabIds.value + tabId
        _dismissedResumeTabIds.value = updated
        homePrefs.edit().putStringSet("dismissed_resume_tabs", updated).apply()
    }

    fun restoreResumeCardEligibility(tabId: String) {
        if (_dismissedResumeTabIds.value.contains(tabId)) {
            val updated = _dismissedResumeTabIds.value - tabId
            _dismissedResumeTabIds.value = updated
            homePrefs.edit().putStringSet("dismissed_resume_tabs", updated).apply()
        }
    }

    // Scanning & Rescan State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow<String?>(null)
    val scanStatusMessage: StateFlow<String?> = _scanStatusMessage.asStateFlow()

    private val _scanErrorMessage = MutableStateFlow<String?>(null)
    val scanErrorMessage: StateFlow<String?> = _scanErrorMessage.asStateFlow()

    fun clearScanErrorMessage() {
        _scanErrorMessage.value = null
    }

    // Splash Screen State & Real Progress (0..100%)
    private val _splashProgress = MutableStateFlow(0)
    val splashProgress: StateFlow<Int> = _splashProgress.asStateFlow()

    private val _splashStatusText = MutableStateFlow("Loading...")
    val splashStatusText: StateFlow<String> = _splashStatusText.asStateFlow()

    private val _isSplashActive = MutableStateFlow(true)
    val isSplashActive: StateFlow<Boolean> = _isSplashActive.asStateFlow()

    // Real File Access & Permission State
    private val _hasFileAccess = MutableStateFlow(false)
    val hasFileAccess: StateFlow<Boolean> = _hasFileAccess.asStateFlow()

    private val _showPermissionRequired = MutableStateFlow(false)
    val showPermissionRequired: StateFlow<Boolean> = _showPermissionRequired.asStateFlow()

    fun dismissSplash() {
        _isSplashActive.value = false
    }

    fun dismissPermissionDialog() {
        _showPermissionRequired.value = false
    }

    fun showPermissionDialog() {
        _showPermissionRequired.value = true
    }

    fun checkFileAccess(isStartup: Boolean = false): Boolean {
        val app = getApplication<Application>()
        val hasAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        val wasGranted = _hasFileAccess.value
        _hasFileAccess.value = hasAccess

        if (hasAccess) {
            _showPermissionRequired.value = false
            if (!wasGranted && !isStartup) {
                // Newly granted when user returned from Android Settings!
                refreshDocuments(showStatus = true)
            }
        } else {
            _showPermissionRequired.value = true
        }

        return hasAccess
    }

    fun showSplash() {
        viewModelScope.launch {
            _splashProgress.value = 0
            _splashStatusText.value = "Loading..."
            _isSplashActive.value = true
            delay(120)
            _splashProgress.value = 35
            delay(220)
            _splashProgress.value = 75
            delay(250)
            _splashProgress.value = 100
            _splashStatusText.value = "Ready"
            delay(250)
            _isSplashActive.value = false
        }
    }

    // Reactive streams backed by Room Database
    val allDocuments: StateFlow<List<DocumentItem>> = repository.allActiveDocuments.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recentDocuments: StateFlow<List<DocumentItem>> = repository.recentDocuments.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val favorites: StateFlow<List<DocumentItem>> = repository.favorites.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recycleBin: StateFlow<List<DocumentItem>> = repository.recycleBin.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Dynamic directories computed from real documents in database
    val directories: StateFlow<List<DirectoryInfo>> = allDocuments.map { docs ->
        docs.groupBy { it.directoryName.ifBlank { "Documents" } }
            .map { (name, items) -> DirectoryInfo(name = name, count = items.size) }
            .sortedByDescending { it.count }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderCount: StateFlow<Int> = directories.map { it.size }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    // Filtered & sorted documents list for the Files screen
    val filteredDocuments: StateFlow<List<DocumentItem>> = combine(
        allDocuments,
        _selectedCategory,
        _searchQuery,
        _sortOption
    ) { docs, category, query, sort ->
        var list = docs
        if (category != DocumentType.ALL) {
            list = list.filter { it.fileType == category }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { doc ->
                doc.title.lowercase().contains(q) ||
                doc.displayTitle.lowercase().contains(q) ||
                doc.extension.lowercase().contains(q) ||
                doc.directoryName.lowercase().contains(q) ||
                doc.fileType.name.lowercase().contains(q) ||
                doc.fileType.displayName.lowercase().contains(q) ||
                doc.path.lowercase().contains(q)
            }
        }
        when (sort) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.lastModified }
            SortOption.DATE_ASC -> list.sortedBy { it.lastModified }
            SortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
            SortOption.SIZE_ASC -> list.sortedBy { it.sizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ContentObserver for detecting MediaStore changes automatically
    private val mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            refreshDocuments(showStatus = false)
        }
    }

    init {
        // Register observer for MediaStore changes
        try {
            val resolver = application.contentResolver
            resolver.registerContentObserver(
                MediaStore.Files.getContentUri("external"),
                true,
                mediaStoreObserver
            )
        } catch (_: Exception) {}

        // Check real file access on start
        val hasAccess = checkFileAccess(isStartup = true)
        if (hasAccess) {
            startStartupSequence()
        } else {
            _isSplashActive.value = false
            _showPermissionRequired.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        } catch (_: Exception) {}
    }

    private fun startStartupSequence() {
        // Phase 1: Immediate app launch with smooth, deterministic, branded splash.
        // The splash screen never blocks on heavy file scanning or directory traversal.
        viewModelScope.launch {
            _splashProgress.value = 0
            _splashStatusText.value = "Loading..."
            delay(120)
            _splashProgress.value = 35
            delay(220)
            _splashProgress.value = 75
            delay(250)
            _splashProgress.value = 100
            _splashStatusText.value = "Ready"
            delay(260)
            // Dismiss splash cleanly - enter Home screen immediately!
            _isSplashActive.value = false
        }

        // Phase 2: Start background file refresh asynchronously.
        // Home screen displays immediately with the cached Room document database.
        // File scanning runs in the background and updates the Room cache without blocking user interaction.
        viewModelScope.launch {
            delay(400)
            runBackgroundSync()
        }
    }

    private suspend fun runBackgroundSync() {
        if (!_hasFileAccess.value) return
        if (_isScanning.value) return
        try {
            _isScanning.value = true
            repository.scanDeviceDocuments(isInitial = false)
        } catch (e: Exception) {
            _scanErrorMessage.value = "Couldn't refresh files"
        } finally {
            _isScanning.value = false
        }
    }

    fun refreshDocuments(showStatus: Boolean = false) {
        if (!_hasFileAccess.value) return
        if (_isScanning.value) return
        viewModelScope.launch {
            if (!_isScanning.compareAndSet(expect = false, update = true)) {
                return@launch
            }
            try {
                repository.scanDeviceDocuments(isInitial = false)
            } catch (e: Exception) {
                _scanErrorMessage.value = "Couldn't refresh files"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun onAppForeground() {
        val wasGranted = _hasFileAccess.value
        val hasAccess = checkFileAccess(isStartup = false)
        if (hasAccess && wasGranted) {
            // Already had access: silently check for any external changes
            refreshDocuments(showStatus = false)
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun openCategory(category: DocumentType) {
        _selectedCategory.value = category
        _currentScreen.value = Screen.Files(category)
    }

    fun openSearch() {
        _currentScreen.value = Screen.Files(DocumentType.ALL)
    }

    fun navigateBack(): Boolean {
        return if (_currentScreen.value != Screen.Main) {
            _currentScreen.value = Screen.Main
            true
        } else {
            false
        }
    }

    fun setTab(tab: BottomTab) {
        _currentTab.value = tab
    }

    fun setCategory(category: DocumentType) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    // Document switcher bottom sheet visibility (WPS Office style over reader)
    private val _isSwitcherSheetVisible = MutableStateFlow(false)
    val isSwitcherSheetVisible: StateFlow<Boolean> = _isSwitcherSheetVisible.asStateFlow()

    fun openSwitcherSheet() {
        _isSwitcherSheetVisible.value = true
    }

    fun closeSwitcherSheet() {
        _isSwitcherSheetVisible.value = false
    }

    fun switchToTab(tab: DocumentTab) {
        restoreResumeCardEligibility(tab.id)
        closeSwitcherSheet()
        tabManager.activateDocument(tab.id)
        val doc = tab.toDocumentItem()
        when (tab.fileType) {
            DocumentFormat.PDF -> navigateTo(Screen.PdfViewer(doc))
            DocumentFormat.PPT -> navigateTo(Screen.PptViewer(doc))
            DocumentFormat.IMAGE -> navigateTo(Screen.ImageViewer(doc))
            DocumentFormat.EXCEL, DocumentFormat.CSV -> navigateTo(Screen.ExcelViewer(doc))
            else -> navigateTo(Screen.TextViewer(doc))
        }
    }

    fun closeTabFromSwitcher(tabId: String) {
        val wasActive = (tabManager.activeTabId.value == tabId)
        tabManager.closeDocument(tabId)
        if (wasActive) {
            val remainingActive = tabManager.activeTab
            if (remainingActive != null) {
                val doc = remainingActive.toDocumentItem()
                when (remainingActive.fileType) {
                    DocumentFormat.PDF -> navigateTo(Screen.PdfViewer(doc))
                    DocumentFormat.PPT -> navigateTo(Screen.PptViewer(doc))
                    DocumentFormat.IMAGE -> navigateTo(Screen.ImageViewer(doc))
                    DocumentFormat.EXCEL, DocumentFormat.CSV -> navigateTo(Screen.ExcelViewer(doc))
                    else -> navigateTo(Screen.TextViewer(doc))
                }
            } else {
                closeSwitcherSheet()
                navigateTo(Screen.Main)
            }
        } else {
            if (tabManager.tabs.value.isEmpty()) {
                closeSwitcherSheet()
                navigateTo(Screen.Main)
            }
        }
    }

    fun reopenClosedTabFromSwitcher(tab: DocumentTab) {
        val reopened = tabManager.reopenClosedTab() ?: tab
        switchToTab(reopened)
    }

    fun updateTabProgress(
        page: Int? = null,
        totalPages: Int? = null,
        sheet: Int? = null,
        slide: Int? = null,
        scroll: Int? = null,
        zoom: Float? = null
    ) {
        val activeId = tabManager.activeTabId.value ?: return
        tabManager.updateTabProgress(activeId, page, totalPages, sheet, slide, scroll, zoom)
    }

    fun openDocument(doc: DocumentItem) {
        viewModelScope.launch {
            repository.updateLastRead(doc.id, doc.lastReadPage)
        }
        val tab = tabManager.openDocument(doc, activate = true)
        restoreResumeCardEligibility(tab.id)
        val activeDoc = tab.toDocumentItem()
        when (tab.fileType) {
            DocumentFormat.PDF -> navigateTo(Screen.PdfViewer(activeDoc))
            DocumentFormat.PPT -> navigateTo(Screen.PptViewer(activeDoc))
            DocumentFormat.IMAGE -> navigateTo(Screen.ImageViewer(activeDoc))
            DocumentFormat.EXCEL, DocumentFormat.CSV -> navigateTo(Screen.ExcelViewer(activeDoc))
            else -> navigateTo(Screen.TextViewer(activeDoc))
        }
    }

    fun openMultiDocDemoSuite(): Int {
        val existingDocs = allDocuments.value
        var openedCount = 0

        val formatsToOpen = listOf(
            DocumentFormat.PDF to "Annual_Report.pdf",
            DocumentFormat.WORD to "Meeting_Notes.docx",
            DocumentFormat.EXCEL to "Q1_Financial_Data.xlsx",
            DocumentFormat.PPT to "Product_Roadmap.pptx"
        )

        var firstDoc: DocumentItem? = null
        formatsToOpen.forEachIndexed { index, (format, defaultTitle) ->
            val match = existingDocs.firstOrNull { doc ->
                DocumentFormat.fromDocumentType(doc.fileType, doc.extension) == format
            }

            val tab = if (match != null) {
                tabManager.openDocument(match, activate = (index == 0))
            } else {
                tabManager.openNewTab(format, defaultTitle)
            }
            if (index == 0) {
                firstDoc = tab.toDocumentItem()
            }
            openedCount++
        }

        firstDoc?.let { openDocument(it) }
        return openedCount
    }

    fun toggleFavorite(doc: DocumentItem) {
        viewModelScope.launch {
            repository.toggleFavorite(doc.id, !doc.isFavorite)
        }
    }

    fun renameDocument(doc: DocumentItem, newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(doc.id, newTitle)
        }
    }

    fun deleteDocument(doc: DocumentItem) {
        viewModelScope.launch {
            repository.moveToRecycleBin(doc.id)
        }
    }

    fun restoreDocument(doc: DocumentItem) {
        viewModelScope.launch {
            repository.restoreFromRecycleBin(doc.id)
        }
    }

    fun permanentlyDelete(doc: DocumentItem) {
        viewModelScope.launch {
            repository.permanentDelete(doc.id)
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            repository.emptyRecycleBin()
        }
    }

    fun importFile(uri: Uri, fileName: String? = null) {
        viewModelScope.launch {
            val doc = repository.importFileFromUri(uri, fileName)
            openDocument(doc)
        }
    }

    fun scanDirectoryTree(treeUri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanStatusMessage.value = "Scanning folder…"
            val added = repository.scanDirectoryTree(treeUri)
            _scanStatusMessage.value = if (added > 0) "$added documents added" else "No new documents found"
            _isScanning.value = false
            delay(3000)
            _scanStatusMessage.value = null
        }
    }

    fun createPdfFromImages(imageUris: List<Uri>, title: String, isLandscape: Boolean, onSuccess: (DocumentItem) -> Unit) {
        viewModelScope.launch {
            val doc = repository.createPdfFromImages(imageUris, title, isLandscape)
            onSuccess(doc)
        }
    }

    fun mergePdfs(pdfPaths: List<String>, title: String, onSuccess: (DocumentItem) -> Unit) {
        viewModelScope.launch {
            val doc = repository.mergePdfs(pdfPaths, title)
            onSuccess(doc)
        }
    }

    fun splitPdf(sourcePath: String, pageIndices: List<Int>, title: String, onSuccess: (DocumentItem) -> Unit) {
        viewModelScope.launch {
            val doc = repository.splitPdf(sourcePath, pageIndices, title)
            onSuccess(doc)
        }
    }

    fun updatePdfPage(docId: Long, page: Int) {
        viewModelScope.launch {
            repository.updateLastRead(docId, page)
        }
    }
}
