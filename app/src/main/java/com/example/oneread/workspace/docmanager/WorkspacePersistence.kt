package com.example.oneread.workspace.docmanager

import android.content.Context
import android.content.SharedPreferences
import com.example.oneread.workspace.model.DocumentFormat
import com.example.oneread.workspace.model.DocumentTab
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles workspace state persistence and restoration across application restarts.
 * Remembers open tabs, active tab, pinned status, scroll/page position, and closed tab history.
 */
class WorkspacePersistence(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("workspace_state_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_OPEN_TABS = "open_tabs_json"
        private const val KEY_ACTIVE_TAB_ID = "active_tab_id"
        private const val KEY_CLOSED_TABS = "closed_tabs_history_json"
        private const val KEY_LAST_SAVED_TIME = "last_saved_time"
    }

    fun saveWorkspace(tabs: List<DocumentTab>, activeTabId: String?, closedHistory: List<DocumentTab>) {
        try {
            val tabsArray = JSONArray()
            tabs.forEach { tab ->
                val obj = JSONObject().apply {
                    put("id", tab.id)
                    put("title", tab.title)
                    put("originalFileName", tab.originalFileName)
                    put("filePath", tab.filePath)
                    put("uriString", tab.uriString)
                    put("fileType", tab.fileType.name)
                    put("isPinned", tab.isPinned)
                    put("isModified", tab.isModified)
                    put("scrollPosition", tab.scrollPosition)
                    put("zoomLevel", tab.zoomLevel.toDouble())
                    put("currentPage", tab.currentPage)
                    put("totalPages", tab.totalPages)
                    put("documentItemId", tab.documentItemId ?: -1L)
                    tab.contentText?.let { put("contentText", it) }
                    put("isNewUnsavedDocument", tab.isNewUnsavedDocument)
                }
                tabsArray.put(obj)
            }

            val historyArray = JSONArray()
            closedHistory.take(15).forEach { tab ->
                val obj = JSONObject().apply {
                    put("id", tab.id)
                    put("title", tab.title)
                    put("originalFileName", tab.originalFileName)
                    put("filePath", tab.filePath)
                    put("uriString", tab.uriString)
                    put("fileType", tab.fileType.name)
                    put("isPinned", tab.isPinned)
                    put("currentPage", tab.currentPage)
                    put("totalPages", tab.totalPages)
                    put("documentItemId", tab.documentItemId ?: -1L)
                }
                historyArray.put(obj)
            }

            prefs.edit()
                .putString(KEY_OPEN_TABS, tabsArray.toString())
                .putString(KEY_ACTIVE_TAB_ID, activeTabId)
                .putString(KEY_CLOSED_TABS, historyArray.toString())
                .putLong(KEY_LAST_SAVED_TIME, System.currentTimeMillis())
                .apply()
        } catch (_: Exception) { }
    }

    fun restoreWorkspace(): Pair<List<DocumentTab>, String?> {
        val jsonString = prefs.getString(KEY_OPEN_TABS, null) ?: return Pair(emptyList(), null)
        val activeId = prefs.getString(KEY_ACTIVE_TAB_ID, null)

        val restoredTabs = mutableListOf<DocumentTab>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val format = runCatching { DocumentFormat.valueOf(obj.getString("fileType")) }.getOrDefault(DocumentFormat.UNKNOWN)
                val id = obj.getString("id")
                val docItemId = obj.optLong("documentItemId", -1L).takeIf { it != -1L }

                val tab = DocumentTab(
                    id = id,
                    title = obj.getString("title"),
                    originalFileName = obj.getString("originalFileName"),
                    filePath = obj.optString("filePath", ""),
                    uriString = obj.optString("uriString", ""),
                    fileType = format,
                    isActive = (id == activeId),
                    isModified = obj.optBoolean("isModified", false),
                    isPinned = obj.optBoolean("isPinned", false),
                    scrollPosition = obj.optInt("scrollPosition", 0),
                    zoomLevel = obj.optDouble("zoomLevel", 1.0).toFloat(),
                    currentPage = obj.optInt("currentPage", 1),
                    totalPages = obj.optInt("totalPages", 1),
                    contentText = if (obj.has("contentText")) obj.getString("contentText") else null,
                    isNewUnsavedDocument = obj.optBoolean("isNewUnsavedDocument", false),
                    documentItemId = docItemId
                )
                restoredTabs.add(tab)
            }
        } catch (_: Exception) {
            return Pair(emptyList(), null)
        }

        return Pair(restoredTabs, activeId)
    }

    fun restoreClosedTabHistory(): List<DocumentTab> {
        val jsonString = prefs.getString(KEY_CLOSED_TABS, null) ?: return emptyList()
        val list = mutableListOf<DocumentTab>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val format = runCatching { DocumentFormat.valueOf(obj.getString("fileType")) }.getOrDefault(DocumentFormat.UNKNOWN)
                val id = obj.getString("id")
                val docItemId = obj.optLong("documentItemId", -1L).takeIf { it != -1L }

                list.add(
                    DocumentTab(
                        id = id,
                        title = obj.getString("title"),
                        originalFileName = obj.getString("originalFileName"),
                        filePath = obj.optString("filePath", ""),
                        uriString = obj.optString("uriString", ""),
                        fileType = format,
                        isActive = false,
                        isPinned = obj.optBoolean("isPinned", false),
                        currentPage = obj.optInt("currentPage", 1),
                        totalPages = obj.optInt("totalPages", 1),
                        documentItemId = docItemId
                    )
                )
            }
        } catch (_: Exception) { }
        return list
    }

    fun clearWorkspace() {
        prefs.edit().clear().apply()
    }
}
