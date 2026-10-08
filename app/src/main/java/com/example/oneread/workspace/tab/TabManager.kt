package com.example.oneread.workspace.tab

import android.content.Context
import android.net.Uri
import com.example.oneread.data.DocumentItem
import com.example.oneread.workspace.docmanager.FileDetector
import com.example.oneread.workspace.docmanager.WorkspacePersistence
import com.example.oneread.workspace.model.DocumentFormat
import com.example.oneread.workspace.model.DocumentTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

/**
 * Core manager for the multi-tab document workspace.
 * Responsible for opening, closing, pinning, duplicating, reordering,
 * switching tabs, maintaining closed tab history, and persisting workspace state.
 */
class TabManager(private val context: Context) {

    private val persistence = WorkspacePersistence(context)

    private val _tabs = MutableStateFlow<List<DocumentTab>>(emptyList())
    val tabs: StateFlow<List<DocumentTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _closedTabHistory = MutableStateFlow<List<DocumentTab>>(emptyList())
    val closedTabHistory: StateFlow<List<DocumentTab>> = _closedTabHistory.asStateFlow()

    // Tab pending close with unsaved changes
    private val _unsavedTabPrompt = MutableStateFlow<DocumentTab?>(null)
    val unsavedTabPrompt: StateFlow<DocumentTab?> = _unsavedTabPrompt.asStateFlow()

    // Tab pending rename
    private val _tabToRename = MutableStateFlow<DocumentTab?>(null)
    val tabToRename: StateFlow<DocumentTab?> = _tabToRename.asStateFlow()

    // Tab pending details/properties view
    private val _tabDetails = MutableStateFlow<DocumentTab?>(null)
    val tabDetails: StateFlow<DocumentTab?> = _tabDetails.asStateFlow()

    init {
        // Restore workspace from persistence if available
        val (restoredTabs, restoredActiveId) = persistence.restoreWorkspace()
        val restoredHistory = persistence.restoreClosedTabHistory()
        if (restoredTabs.isNotEmpty()) {
            _tabs.value = restoredTabs
            _activeTabId.value = restoredActiveId ?: restoredTabs.first().id
            _closedTabHistory.value = restoredHistory
        }
    }

    val activeTab: DocumentTab?
        get() = _tabs.value.firstOrNull { it.id == _activeTabId.value }

    fun openDocument(doc: DocumentItem, activate: Boolean = true): DocumentTab {
        val currentList = _tabs.value.toMutableList()

        // Check if already open by filePath or uri or id
        val existingIndex = currentList.indexOfFirst {
            (it.filePath.isNotBlank() && it.filePath == doc.path) ||
            (it.documentItemId != null && it.documentItemId == doc.id)
        }

        if (existingIndex != -1) {
            val existing = currentList[existingIndex]
            if (activate) {
                switchTab(existing.id)
            }
            return existing
        }

        val newTab = DocumentTab.fromDocumentItem(doc, activate = activate)
        currentList.add(newTab)
        _tabs.value = currentList

        if (activate) {
            switchTab(newTab.id)
        }
        persist()
        return newTab
    }

    fun openMultipleDocuments(docs: List<DocumentItem>) {
        if (docs.isEmpty()) return
        docs.forEachIndexed { index, doc ->
            openDocument(doc, activate = (index == docs.lastIndex))
        }
    }

    fun openFile(uri: Uri, fileName: String? = null, activate: Boolean = true): DocumentTab {
        val name = fileName ?: FileDetector.getFileNameFromUri(context, uri)
        val format = FileDetector.detectFormat(name)
        val currentList = _tabs.value.toMutableList()

        val existing = currentList.firstOrNull { it.uriString == uri.toString() || it.originalFileName == name }
        if (existing != null) {
            if (activate) switchTab(existing.id)
            return existing
        }

        val tab = DocumentTab(
            id = "file_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
            title = name,
            originalFileName = name,
            filePath = if (uri.scheme == "file") uri.path ?: "" else "",
            uriString = uri.toString(),
            fileType = format,
            isActive = activate,
            currentPage = 1,
            totalPages = 1
        )
        currentList.add(tab)
        _tabs.value = currentList
        if (activate) switchTab(tab.id)
        persist()
        return tab
    }

    fun openNewTab(format: DocumentFormat = DocumentFormat.TEXT, customName: String? = null): DocumentTab {
        val newTab = DocumentTab.createBlankDocument(format, customName)
        val currentList = _tabs.value.toMutableList()
        currentList.add(newTab)
        _tabs.value = currentList
        switchTab(newTab.id)
        persist()
        return newTab
    }

    fun switchTab(tabId: String) {
        val list = _tabs.value.map { tab ->
            tab.copy(isActive = (tab.id == tabId))
        }
        _tabs.value = list
        _activeTabId.value = tabId
        persist()
    }

    fun selectTabByIndex(index: Int) {
        val list = _tabs.value
        if (list.isEmpty()) return
        val targetIndex = (index - 1).coerceIn(0, list.lastIndex)
        switchTab(list[targetIndex].id)
    }

    fun nextTab() {
        val list = _tabs.value
        if (list.size <= 1) return
        val currentIdx = list.indexOfFirst { it.id == _activeTabId.value }
        val nextIdx = if (currentIdx == -1 || currentIdx == list.lastIndex) 0 else currentIdx + 1
        switchTab(list[nextIdx].id)
    }

    fun previousTab() {
        val list = _tabs.value
        if (list.size <= 1) return
        val currentIdx = list.indexOfFirst { it.id == _activeTabId.value }
        val prevIdx = if (currentIdx <= 0) list.lastIndex else currentIdx - 1
        switchTab(list[prevIdx].id)
    }

    fun closeTab(tabId: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        if (tab.isModified) {
            // Prompt user about unsaved changes
            _unsavedTabPrompt.value = tab
            return
        }
        forceCloseTab(tabId)
    }

    fun forceCloseTab(tabId: String) {
        val currentList = _tabs.value.toMutableList()
        val tabIndex = currentList.indexOfFirst { it.id == tabId }
        if (tabIndex == -1) return

        val closedTab = currentList.removeAt(tabIndex)

        // Save to closed tab history
        val history = _closedTabHistory.value.toMutableList()
        history.add(0, closedTab)
        if (history.size > 20) history.removeAt(history.lastIndex)
        _closedTabHistory.value = history

        // Determine new active tab if the closed tab was active
        if (_activeTabId.value == tabId) {
            if (currentList.isNotEmpty()) {
                val newActiveIndex = tabIndex.coerceAtMost(currentList.lastIndex)
                val newActiveTab = currentList[newActiveIndex]
                _activeTabId.value = newActiveTab.id
                _tabs.value = currentList.map { it.copy(isActive = (it.id == newActiveTab.id)) }
            } else {
                _activeTabId.value = null
                _tabs.value = emptyList()
            }
        } else {
            _tabs.value = currentList
        }

        _unsavedTabPrompt.value = null
        persist()
    }

    fun dismissUnsavedPrompt() {
        _unsavedTabPrompt.value = null
    }

    fun closeOtherTabs(tabId: String) {
        val currentList = _tabs.value
        val toKeep = currentList.filter { it.id == tabId || it.isPinned }
        val closed = currentList.filter { it.id != tabId && !it.isPinned }

        val history = _closedTabHistory.value.toMutableList()
        history.addAll(0, closed)
        _closedTabHistory.value = history.take(20)

        _tabs.value = toKeep.map { it.copy(isActive = (it.id == tabId)) }
        _activeTabId.value = tabId
        persist()
    }

    fun closeTabsToTheRight(tabId: String) {
        val currentList = _tabs.value
        val targetIndex = currentList.indexOfFirst { it.id == tabId }
        if (targetIndex == -1 || targetIndex == currentList.lastIndex) return

        val toKeep = mutableListOf<DocumentTab>()
        val closed = mutableListOf<DocumentTab>()

        currentList.forEachIndexed { index, tab ->
            if (index <= targetIndex || tab.isPinned) {
                toKeep.add(tab)
            } else {
                closed.add(tab)
            }
        }

        val history = _closedTabHistory.value.toMutableList()
        history.addAll(0, closed)
        _closedTabHistory.value = history.take(20)

        _tabs.value = toKeep
        if (closed.any { it.id == _activeTabId.value }) {
            switchTab(tabId)
        } else {
            persist()
        }
    }

    fun closeAllTabs() {
        val currentList = _tabs.value
        val pinned = currentList.filter { it.isPinned }
        val closed = currentList.filter { !it.isPinned }

        val history = _closedTabHistory.value.toMutableList()
        history.addAll(0, closed)
        _closedTabHistory.value = history.take(20)

        if (pinned.isNotEmpty()) {
            _tabs.value = pinned.mapIndexed { idx, tab -> tab.copy(isActive = (idx == 0)) }
            _activeTabId.value = pinned.first().id
        } else {
            _tabs.value = emptyList()
            _activeTabId.value = null
        }
        persist()
    }

    fun duplicateTab(tabId: String) {
        val original = _tabs.value.firstOrNull { it.id == tabId } ?: return
        val duplicate = original.copy(
            id = "dup_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
            title = "${original.title} (Copy)",
            isActive = true,
            isPinned = false
        )
        val currentList = _tabs.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == tabId }
        if (idx != -1) {
            currentList.add(idx + 1, duplicate)
        } else {
            currentList.add(duplicate)
        }
        _tabs.value = currentList
        switchTab(duplicate.id)
        persist()
    }

    fun pinTab(tabId: String, pinned: Boolean) {
        val currentList = _tabs.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == tabId }
        if (idx == -1) return

        val updated = currentList[idx].copy(isPinned = pinned)
        currentList.removeAt(idx)

        // Pinned tabs move to the front; unpinned tabs stay after pinned tabs
        if (pinned) {
            val lastPinnedIndex = currentList.indexOfLast { it.isPinned }
            val insertAt = if (lastPinnedIndex == -1) 0 else lastPinnedIndex + 1
            currentList.add(insertAt, updated)
        } else {
            val firstUnpinned = currentList.indexOfFirst { !it.isPinned }
            val insertAt = if (firstUnpinned == -1) currentList.size else firstUnpinned
            currentList.add(insertAt, updated)
        }

        _tabs.value = currentList
        persist()
    }

    fun reopenClosedTab(): DocumentTab? {
        val history = _closedTabHistory.value.toMutableList()
        if (history.isEmpty()) return null

        val toReopen = history.removeAt(0)
        _closedTabHistory.value = history

        val currentList = _tabs.value.toMutableList()
        // Ensure fresh ID
        val reopened = toReopen.copy(
            id = "reopened_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
            isActive = true
        )
        currentList.add(reopened)
        _tabs.value = currentList
        switchTab(reopened.id)
        persist()
        return reopened
    }

    fun reorderTabs(fromIndex: Int, toIndex: Int) {
        val list = _tabs.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return

        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _tabs.value = list
        persist()
    }

    fun promptRenameTab(tabId: String) {
        _tabToRename.value = _tabs.value.firstOrNull { it.id == tabId }
    }

    fun dismissRenameDialog() {
        _tabToRename.value = null
    }

    fun confirmRenameTab(tabId: String, newName: String) {
        if (newName.isBlank()) return
        val currentList = _tabs.value.map { tab ->
            if (tab.id == tabId) tab.copy(title = newName.trim(), isModified = true) else tab
        }
        _tabs.value = currentList
        _tabToRename.value = null
        persist()
    }

    fun showTabDetails(tabId: String) {
        _tabDetails.value = _tabs.value.firstOrNull { it.id == tabId }
    }

    fun dismissTabDetails() {
        _tabDetails.value = null
    }

    fun updateTabState(tabId: String, updater: (DocumentTab) -> DocumentTab) {
        val currentList = _tabs.value.map { tab ->
            if (tab.id == tabId) updater(tab) else tab
        }
        _tabs.value = currentList
    }

    fun markModified(tabId: String, isModified: Boolean) {
        updateTabState(tabId) { it.copy(isModified = isModified) }
    }

    fun saveTabContent(tabId: String, newContent: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        if (tab.filePath.isNotBlank()) {
            runCatching {
                File(tab.filePath).writeText(newContent)
            }
        }
        updateTabState(tabId) {
            it.copy(
                contentText = newContent,
                isModified = false,
                isNewUnsavedDocument = false
            )
        }
        persist()
    }

    fun persist() {
        persistence.saveWorkspace(_tabs.value, _activeTabId.value, _closedTabHistory.value)
    }

    fun updateActiveDocumentState(updater: (DocumentTab) -> DocumentTab) {
        val activeId = _activeTabId.value ?: return
        updateTabState(activeId, updater)
    }

    fun updateTabProgress(
        tabId: String,
        page: Int? = null,
        totalPages: Int? = null,
        sheet: Int? = null,
        slide: Int? = null,
        scroll: Int? = null,
        zoom: Float? = null
    ) {
        updateTabState(tabId) { tab ->
            tab.copy(
                currentPage = page ?: tab.currentPage,
                totalPages = totalPages ?: tab.totalPages,
                currentSheet = sheet ?: tab.currentSheet,
                currentSlide = slide ?: tab.currentSlide,
                scrollPosition = scroll ?: tab.scrollPosition,
                zoomLevel = zoom ?: tab.zoomLevel
            )
        }
    }

    // Explicit DocumentTabManager API aliases requested by architecture specification
    fun activateDocument(tabId: String) = switchTab(tabId)
    fun closeDocument(tabId: String) = closeTab(tabId)
    fun closeOtherDocuments(tabId: String) = closeOtherTabs(tabId)
    fun closeDocumentsToRight(tabId: String) = closeTabsToTheRight(tabId)
    fun closeAllDocuments() = closeAllTabs()
    fun reopenClosedDocument(): DocumentTab? = reopenClosedTab()
    fun reorderDocuments(fromIndex: Int, toIndex: Int) = reorderTabs(fromIndex, toIndex)
    fun duplicateDocument(tabId: String) = duplicateTab(tabId)
    fun pinDocument(tabId: String, pinned: Boolean) = pinTab(tabId, pinned)
    fun getOpenDocumentCount(): Int = _tabs.value.size
}
