package com.example.oneread.excel.search

import com.example.oneread.excel.model.ExcelCellModel
import com.example.oneread.excel.model.ExcelWorkbookModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SearchMatch(
    val sheetIndex: Int,
    val sheetName: String,
    val row: Int,
    val col: Int,
    val cellRef: String,
    val value: String
)

data class SearchOptions(
    val query: String = "",
    val searchEntireWorkbook: Boolean = true,
    val matchCase: Boolean = false,
    val matchEntireCell: Boolean = false,
    val searchFormulas: Boolean = false
)

class ExcelSearchManager(private val workbook: ExcelWorkbookModel) {

    private val _matches = MutableStateFlow<List<SearchMatch>>(emptyList())
    val matches: StateFlow<List<SearchMatch>> = _matches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(-1)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    suspend fun search(options: SearchOptions) = withContext(Dispatchers.Default) {
        if (options.query.isBlank()) {
            _matches.value = emptyList()
            _currentMatchIndex.value = -1
            return@withContext
        }

        _isSearching.value = true
        val query = if (options.matchCase) options.query else options.query.lowercase()
        val found = mutableListOf<SearchMatch>()

        val sheetsToSearch = if (options.searchEntireWorkbook) {
            workbook.sheets
        } else {
            listOfNotNull(workbook.activeSheet)
        }

        for (sheet in sheetsToSearch) {
            for ((_, cell) in sheet.cells) {
                val targetText = if (options.searchFormulas && cell.formula != null) {
                    cell.formula
                } else {
                    cell.displayValue
                }

                val compText = if (options.matchCase) targetText else targetText.lowercase()

                val isMatch = if (options.matchEntireCell) {
                    compText == query
                } else {
                    compText.contains(query)
                }

                if (isMatch) {
                    found.add(
                        SearchMatch(
                            sheetIndex = sheet.index,
                            sheetName = sheet.name,
                            row = cell.row,
                            col = cell.col,
                            cellRef = cell.cellRef,
                            value = cell.displayValue
                        )
                    )
                }
            }
        }

        // Sort matches by sheet, row, col
        found.sortWith(compareBy({ it.sheetIndex }, { it.row }, { it.col }))

        _matches.value = found
        _currentMatchIndex.value = if (found.isNotEmpty()) 0 else -1
        _isSearching.value = false
    }

    fun nextMatch(): SearchMatch? {
        val list = _matches.value
        if (list.isEmpty()) return null
        val nextIdx = (_currentMatchIndex.value + 1) % list.size
        _currentMatchIndex.value = nextIdx
        return list[nextIdx]
    }

    fun prevMatch(): SearchMatch? {
        val list = _matches.value
        if (list.isEmpty()) return null
        val prevIdx = if (_currentMatchIndex.value <= 0) list.size - 1 else _currentMatchIndex.value - 1
        _currentMatchIndex.value = prevIdx
        return list[prevIdx]
    }

    fun selectMatch(index: Int): SearchMatch? {
        val list = _matches.value
        if (index in list.indices) {
            _currentMatchIndex.value = index
            return list[index]
        }
        return null
    }

    fun clearSearch() {
        _matches.value = emptyList()
        _currentMatchIndex.value = -1
    }

    /**
     * Replaces the current match with new text.
     */
    fun replaceCurrent(replacement: String): Boolean {
        val list = _matches.value
        val idx = _currentMatchIndex.value
        if (idx !in list.indices) return false

        val match = list[idx]
        val sheet = workbook.sheets.getOrNull(match.sheetIndex) ?: return false
        val cell = sheet.getCell(match.row, match.col) ?: return false

        val updatedValue = if (cell.displayValue.contains(match.value)) {
            cell.displayValue.replace(match.value, replacement)
        } else {
            replacement
        }

        sheet.setCellValue(match.row, match.col, updatedValue)
        workbook.isModified = true
        return true
    }

    /**
     * Replaces all occurrences across matching cells.
     * Returns count of cells updated.
     */
    fun replaceAll(findText: String, replacement: String, matchCase: Boolean = false): Int {
        val list = _matches.value
        if (list.isEmpty()) return 0
        var count = 0

        for (match in list) {
            val sheet = workbook.sheets.getOrNull(match.sheetIndex) ?: continue
            val cell = sheet.getCell(match.row, match.col) ?: continue
            val curVal = cell.rawValue
            val newVal = if (matchCase) {
                curVal.replace(findText, replacement)
            } else {
                curVal.replace(Regex(Regex.escape(findText), RegexOption.IGNORE_CASE), replacement)
            }
            if (newVal != curVal) {
                sheet.setCellValue(match.row, match.col, newVal)
                count++
            }
        }

        if (count > 0) {
            workbook.isModified = true
            clearSearch()
        }
        return count
    }
}
