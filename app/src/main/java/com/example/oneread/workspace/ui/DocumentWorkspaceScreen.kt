package com.example.oneread.workspace.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import com.example.oneread.data.DocumentItem
import com.example.oneread.ui.MainViewModel
import com.example.oneread.workspace.model.DocumentFormat
import com.example.oneread.workspace.model.DocumentTab
import com.example.oneread.workspace.tab.TabBar
import com.example.oneread.workspace.viewers.CsvViewerComponent
import com.example.oneread.workspace.viewers.ExcelViewerComponent
import com.example.oneread.workspace.viewers.ImageViewerComponent
import com.example.oneread.workspace.viewers.MarkdownViewerComponent
import com.example.oneread.workspace.viewers.PdfViewerComponent
import com.example.oneread.workspace.viewers.PptViewerComponent
import com.example.oneread.workspace.viewers.TextViewerComponent
import com.example.oneread.workspace.viewers.WordViewerComponent
import kotlinx.coroutines.launch

@Composable
fun DocumentWorkspaceScreen(
    viewModel: MainViewModel,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val tabManager = viewModel.tabManager

    val tabs by tabManager.tabs.collectAsState()
    val activeTabId by tabManager.activeTabId.collectAsState()
    val closedHistory by tabManager.closedTabHistory.collectAsState()
    val unsavedTabPrompt by tabManager.unsavedTabPrompt.collectAsState()
    val tabToRename by tabManager.tabToRename.collectAsState()
    val tabDetails by tabManager.tabDetails.collectAsState()

    val recentDocs by viewModel.recentDocuments.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    var showShortcutsDialog by remember { mutableStateOf(false) }
    var showFormatsDialog by remember { mutableStateOf(false) }
    var isNewTabHubOpen by remember { mutableStateOf(tabs.isEmpty()) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto-open new tab hub if all tabs closed
    LaunchedEffect(tabs.isEmpty()) {
        if (tabs.isEmpty()) {
            isNewTabHubOpen = true
        }
    }

    // File Picker for single or multiple files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEachIndexed { index, uri ->
                tabManager.openFile(uri, activate = (index == uris.lastIndex))
            }
            isNewTabHubOpen = false
            Toast.makeText(context, "Opened ${uris.size} document(s) in tabs", Toast.LENGTH_SHORT).show()
        }
    }

    // Hardware and software keyboard shortcut handler (Ctrl / Cmd)
    val handleKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { event ->
        val isCtrlOrCmd = event.isCtrlPressed || event.isMetaPressed
        if (isCtrlOrCmd) {
            when (event.key) {
                Key.T -> {
                    if (event.isShiftPressed) {
                        // Ctrl + Shift + T: Reopen closed tab
                        tabManager.reopenClosedTab()
                    } else {
                        // Ctrl + T: New tab
                        isNewTabHubOpen = true
                    }
                    true
                }
                Key.W -> {
                    // Ctrl + W: Close current tab
                    activeTabId?.let { tabManager.closeTab(it) }
                    true
                }
                Key.Tab -> {
                    // Ctrl + Tab / Ctrl + Shift + Tab
                    if (event.isShiftPressed) {
                        tabManager.previousTab()
                    } else {
                        tabManager.nextTab()
                    }
                    isNewTabHubOpen = false
                    true
                }
                Key.O -> {
                    filePickerLauncher.launch(arrayOf("*/*"))
                    true
                }
                Key.S -> {
                    activeTabId?.let { id ->
                        val active = tabs.firstOrNull { it.id == id }
                        if (active?.contentText != null) {
                            tabManager.saveTabContent(id, active.contentText)
                            Toast.makeText(context, "Saved ${active.title}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                Key.One -> { tabManager.selectTabByIndex(1); isNewTabHubOpen = false; true }
                Key.Two -> { tabManager.selectTabByIndex(2); isNewTabHubOpen = false; true }
                Key.Three -> { tabManager.selectTabByIndex(3); isNewTabHubOpen = false; true }
                Key.Four -> { tabManager.selectTabByIndex(4); isNewTabHubOpen = false; true }
                Key.Five -> { tabManager.selectTabByIndex(5); isNewTabHubOpen = false; true }
                Key.Six -> { tabManager.selectTabByIndex(6); isNewTabHubOpen = false; true }
                Key.Seven -> { tabManager.selectTabByIndex(7); isNewTabHubOpen = false; true }
                Key.Eight -> { tabManager.selectTabByIndex(8); isNewTabHubOpen = false; true }
                Key.Nine -> { tabManager.selectTabByIndex(9); isNewTabHubOpen = false; true }
                else -> false
            }
        } else {
            false
        }
    }

    // Intercept Back button
    BackHandler {
        if (isNewTabHubOpen && tabs.isNotEmpty()) {
            isNewTabHubOpen = false
        } else {
            onNavigateToLibrary()
        }
    }

    val activeTab = tabs.firstOrNull { it.id == activeTabId }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent(handleKeyEvent),
        containerColor = Color(0xFF070B14),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. TOP TAB BAR (WPS Office / Google Chrome Style)
            TabBar(
                tabs = tabs,
                activeTabId = if (isNewTabHubOpen) null else activeTabId,
                canReopenClosed = closedHistory.isNotEmpty(),
                onTabSelected = { id ->
                    isNewTabHubOpen = false
                    tabManager.switchTab(id)
                },
                onTabClose = { id -> tabManager.closeTab(id) },
                onTabCloseOthers = { id -> tabManager.closeOtherTabs(id) },
                onTabCloseToRight = { id -> tabManager.closeTabsToTheRight(id) },
                onTabCloseAll = { tabManager.closeAllTabs() },
                onTabDuplicate = { id -> tabManager.duplicateTab(id) },
                onTabTogglePin = { id ->
                    val tab = tabs.firstOrNull { it.id == id }
                    if (tab != null) tabManager.pinTab(id, !tab.isPinned)
                },
                onTabReopenClosed = {
                    isNewTabHubOpen = false
                    tabManager.reopenClosedTab()
                },
                onTabDoubleClicked = { id -> tabManager.promptRenameTab(id) },
                onTabReorder = { from, to -> tabManager.reorderTabs(from, to) },
                onTabInfo = { id -> tabManager.showTabDetails(id) },
                onNewTabClick = { isNewTabHubOpen = true },
                onMenuClick = onNavigateToLibrary
            )

            // 2. DESKTOP / WPS MENU BAR (File, Edit, View, Tools, Window, Help)
            WorkspaceMenuBar(
                activeTab = if (isNewTabHubOpen) null else activeTab,
                allTabs = tabs,
                canReopenClosed = closedHistory.isNotEmpty(),
                onNewTab = { isNewTabHubOpen = true },
                onOpenFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                onSave = {
                    activeTab?.let { tab ->
                        if (tab.contentText != null) {
                            tabManager.saveTabContent(tab.id, tab.contentText)
                            Toast.makeText(context, "Saved ${tab.title}", Toast.LENGTH_SHORT).show()
                        } else {
                            tabManager.markModified(tab.id, false)
                            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onCloseTab = {
                    activeTabId?.let { tabManager.closeTab(it) }
                },
                onCloseAllTabs = { tabManager.closeAllTabs() },
                onReopenClosedTab = {
                    isNewTabHubOpen = false
                    tabManager.reopenClosedTab()
                },
                onNextTab = {
                    isNewTabHubOpen = false
                    tabManager.nextTab()
                },
                onPrevTab = {
                    isNewTabHubOpen = false
                    tabManager.previousTab()
                },
                onSelectTab = { id ->
                    isNewTabHubOpen = false
                    tabManager.switchTab(id)
                },
                onRenameTab = {
                    activeTabId?.let { tabManager.promptRenameTab(it) }
                },
                onZoomIn = {
                    activeTab?.let { tab ->
                        tabManager.updateTabState(tab.id) { it.copy(zoomLevel = (it.zoomLevel + 0.2f).coerceAtMost(3.0f)) }
                    }
                },
                onZoomOut = {
                    activeTab?.let { tab ->
                        tabManager.updateTabState(tab.id) { it.copy(zoomLevel = (it.zoomLevel - 0.2f).coerceAtLeast(0.5f)) }
                    }
                },
                onResetZoom = {
                    activeTab?.let { tab ->
                        tabManager.updateTabState(tab.id) { it.copy(zoomLevel = 1.0f) }
                    }
                },
                onToggleNightMode = {
                    Toast.makeText(context, "Toggled viewer night mode", Toast.LENGTH_SHORT).show()
                },
                onShowDocInfo = {
                    activeTabId?.let { tabManager.showTabDetails(it) }
                },
                onShowShortcuts = { showShortcutsDialog = true },
                onShowSupportedFormats = { showFormatsDialog = true },
                onReturnToLibrary = onNavigateToLibrary,
                onShare = {
                    activeTab?.let { tab ->
                        Toast.makeText(context, "Sharing ${tab.title}…", Toast.LENGTH_SHORT).show()
                    }
                },
                onPrint = {
                    activeTab?.let { tab ->
                        Toast.makeText(context, "Printing / Exporting ${tab.title}…", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 3. MAIN WORKSPACE BODY
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isNewTabHubOpen || activeTab == null) {
                    NewTabScreen(
                        recentDocuments = recentDocs,
                        onOpenDocument = { doc ->
                            tabManager.openDocument(doc, activate = true)
                            isNewTabHubOpen = false
                        },
                        onOpenFormat = { format ->
                            tabManager.openNewTab(format)
                            isNewTabHubOpen = false
                        },
                        onPickFiles = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        onOpenMultipleDemoTabs = {
                            // Opens multiple diverse formats simultaneously into independent tabs
                            val opened = viewModel.openMultiDocDemoSuite()
                            isNewTabHubOpen = false
                            Toast.makeText(context, "Opened $opened demo documents across independent tabs!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // Active Document Renderer
                    when (activeTab.fileType) {
                        DocumentFormat.PDF -> {
                            PdfViewerComponent(
                                tab = activeTab,
                                onUpdateTab = { updated ->
                                    tabManager.updateTabState(activeTab.id) { updated }
                                }
                            )
                        }
                        DocumentFormat.WORD -> {
                            WordViewerComponent(
                                tab = activeTab,
                                onUpdateTab = { updated ->
                                    tabManager.updateTabState(activeTab.id) { updated }
                                }
                            )
                        }
                        DocumentFormat.EXCEL -> {
                            ExcelViewerComponent(
                                tab = activeTab,
                                onUpdateTab = { updated ->
                                    tabManager.updateTabState(activeTab.id) { updated }
                                }
                            )
                        }
                        DocumentFormat.PPT -> {
                            PptViewerComponent(
                                tab = activeTab,
                                onUpdateTab = { updated ->
                                    tabManager.updateTabState(activeTab.id) { updated }
                                }
                            )
                        }
                        DocumentFormat.TEXT -> {
                            TextViewerComponent(
                                tab = activeTab,
                                onUpdateTab = { updated ->
                                    tabManager.updateTabState(activeTab.id) { updated }
                                },
                                onSaveContent = { newText ->
                                    tabManager.saveTabContent(activeTab.id, newText)
                                    Toast.makeText(context, "Saved changes to ${activeTab.title}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        DocumentFormat.CSV -> {
                            CsvViewerComponent(
                                tab = activeTab,
                                onUpdateTab = { updated ->
                                    tabManager.updateTabState(activeTab.id) { updated }
                                },
                                onSaveContent = { newCsv ->
                                    tabManager.saveTabContent(activeTab.id, newCsv)
                                    Toast.makeText(context, "Saved changes to ${activeTab.title}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        DocumentFormat.MARKDOWN -> {
                            MarkdownViewerComponent(
                                tab = activeTab,
                                onUpdateTab = { updated ->
                                    tabManager.updateTabState(activeTab.id) { updated }
                                },
                                onSaveContent = { newMd ->
                                    tabManager.saveTabContent(activeTab.id, newMd)
                                    Toast.makeText(context, "Saved changes to ${activeTab.title}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        DocumentFormat.IMAGE -> {
                            ImageViewerComponent(
                                tab = activeTab,
                                onUpdateTab = { updated ->
                                    tabManager.updateTabState(activeTab.id) { updated }
                                }
                            )
                        }
                        DocumentFormat.UNKNOWN -> {
                            TextViewerComponent(
                                tab = activeTab,
                                onUpdateTab = { updated ->
                                    tabManager.updateTabState(activeTab.id) { updated }
                                },
                                onSaveContent = { newText ->
                                    tabManager.saveTabContent(activeTab.id, newText)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 4. DIALOGS
    // Unsaved Changes Protection Dialog
    unsavedTabPrompt?.let { tab ->
        UnsavedChangesDialog(
            tab = tab,
            onSave = {
                if (tab.contentText != null) {
                    tabManager.saveTabContent(tab.id, tab.contentText)
                }
                tabManager.forceCloseTab(tab.id)
            },
            onDiscard = {
                tabManager.forceCloseTab(tab.id)
            },
            onCancel = {
                tabManager.dismissUnsavedPrompt()
            }
        )
    }

    // Rename Tab Dialog
    tabToRename?.let { tab ->
        TabRenameDialog(
            tab = tab,
            onConfirm = { newName ->
                tabManager.confirmRenameTab(tab.id, newName)
            },
            onDismiss = {
                tabManager.dismissRenameDialog()
            }
        )
    }

    // Document Properties / Info Dialog
    tabDetails?.let { tab ->
        DocumentInfoDialog(
            tab = tab,
            onDismiss = {
                tabManager.dismissTabDetails()
            }
        )
    }

    // Shortcuts Dialog
    if (showShortcutsDialog) {
        ShortcutsDialog(onDismiss = { showShortcutsDialog = false })
    }

    // Supported Formats Dialog
    if (showFormatsDialog) {
        SupportedFormatsDialog(onDismiss = { showFormatsDialog = false })
    }
}
