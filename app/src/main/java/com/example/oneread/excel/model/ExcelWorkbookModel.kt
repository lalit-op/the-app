package com.example.oneread.excel.model

import java.io.File

/**
 * In-memory representation of an Excel workbook containing one or more sheets.
 */
data class ExcelWorkbookModel(
    val fileName: String,
    val filePath: String = "",
    val sheets: MutableList<ExcelSheetModel> = mutableListOf(),
    var activeSheetIndex: Int = 0,
    var isModified: Boolean = false
) {
    val activeSheet: ExcelSheetModel?
        get() = sheets.getOrNull(activeSheetIndex) ?: sheets.firstOrNull()

    val sheetCount: Int
        get() = sheets.size

    val sheetNames: List<String>
        get() = sheets.map { it.name }

    fun addSheet(name: String? = null): ExcelSheetModel {
        val newIndex = sheets.size
        val uniqueName = name ?: run {
            var counter = newIndex + 1
            while (sheets.any { it.name.equals("Sheet$counter", ignoreCase = true) }) {
                counter++
            }
            "Sheet$counter"
        }
        val newSheet = ExcelSheetModel(name = uniqueName, index = newIndex)
        sheets.add(newSheet)
        isModified = true
        return newSheet
    }

    fun duplicateSheet(index: Int): ExcelSheetModel? {
        val source = sheets.getOrNull(index) ?: return null
        var copyName = "${source.name} (Copy)"
        var counter = 2
        while (sheets.any { it.name.equals(copyName, ignoreCase = true) }) {
            copyName = "${source.name} (Copy $counter)"
            counter++
        }
        val copy = ExcelSheetModel(
            name = copyName,
            index = sheets.size,
            maxRow = source.maxRow,
            maxCol = source.maxCol,
            cells = source.cells.mapValues { it.value.copy() }.toMutableMap(),
            columnWidths = source.columnWidths.toMutableMap(),
            rowHeights = source.rowHeights.toMutableMap(),
            frozenRows = source.frozenRows,
            frozenCols = source.frozenCols,
            mergedRanges = source.mergedRanges.toMutableList()
        )
        sheets.add(index + 1, copy)
        isModified = true
        return copy
    }

    fun deleteSheet(index: Int): Boolean {
        if (sheets.size <= 1) return false // At least one sheet required
        if (index in sheets.indices) {
            sheets.removeAt(index)
            if (activeSheetIndex >= sheets.size) {
                activeSheetIndex = (sheets.size - 1).coerceAtLeast(0)
            }
            isModified = true
            return true
        }
        return false
    }

    fun renameSheet(index: Int, newName: String): Boolean {
        val clean = newName.trim()
        if (clean.isBlank()) return false
        if (sheets.anyIndexed { idx, s -> idx != index && s.name.equals(clean, ignoreCase = true) }) {
            return false // Name collision
        }
        val target = sheets.getOrNull(index) ?: return false
        sheets[index] = target.copy(name = clean)
        isModified = true
        return true
    }

    fun moveSheet(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in sheets.indices || toIndex !in sheets.indices || fromIndex == toIndex) {
            return false
        }
        val item = sheets.removeAt(fromIndex)
        sheets.add(toIndex, item)
        if (activeSheetIndex == fromIndex) {
            activeSheetIndex = toIndex
        }
        isModified = true
        return true
    }

    companion object {
        fun createEmpty(name: String = "Workbook.xlsx"): ExcelWorkbookModel {
            val wb = ExcelWorkbookModel(fileName = name)
            wb.sheets.add(ExcelSheetModel(name = "Sheet1", index = 0))
            return wb
        }
    }
}

private inline fun <T> List<T>.anyIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
    var index = 0
    for (element in this) {
        if (predicate(index++, element)) return true
    }
    return false
}
