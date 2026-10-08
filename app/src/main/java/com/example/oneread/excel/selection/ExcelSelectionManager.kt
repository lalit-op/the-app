package com.example.oneread.excel.selection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.oneread.excel.model.CellRange
import com.example.oneread.excel.model.ExcelCellModel
import com.example.oneread.excel.model.ExcelSheetModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExcelSelectionManager {

    private val _activeCell = MutableStateFlow(Pair(1, 1)) // Row 1, Col 1 (A1)
    val activeCell: StateFlow<Pair<Int, Int>> = _activeCell.asStateFlow()

    private val _selectedRange = MutableStateFlow<CellRange?>(null)
    val selectedRange: StateFlow<CellRange?> = _selectedRange.asStateFlow()

    private var internalClipboardValue: String? = null
    private var internalClipboardFormula: String? = null

    val nameBoxText: String
        get() {
            val range = _selectedRange.value
            if (range != null && !range.isSingleCell) {
                return range.rangeRef
            }
            val cell = _activeCell.value
            return "${ExcelCellModel.colIndexToLetters(cell.second)}${cell.first}"
        }

    fun selectCell(row: Int, col: Int) {
        _activeCell.value = Pair(row, col)
        _selectedRange.value = CellRange(row, col, row, col)
    }

    fun selectRange(startRow: Int, startCol: Int, endRow: Int, endCol: Int) {
        _activeCell.value = Pair(startRow, startCol)
        _selectedRange.value = CellRange(
            minOf(startRow, endRow),
            minOf(startCol, endCol),
            maxOf(startRow, endRow),
            maxOf(startCol, endCol)
        )
    }

    fun selectRow(row: Int, maxCol: Int) {
        _activeCell.value = Pair(row, 1)
        _selectedRange.value = CellRange(row, 1, row, maxCol)
    }

    fun selectColumn(col: Int, maxRow: Int) {
        _activeCell.value = Pair(1, col)
        _selectedRange.value = CellRange(1, col, maxRow, col)
    }

    fun selectAll(maxRow: Int, maxCol: Int) {
        _activeCell.value = Pair(1, 1)
        _selectedRange.value = CellRange(1, 1, maxRow, maxCol)
    }

    fun copy(sheet: ExcelSheetModel, context: Context?) {
        val (row, col) = _activeCell.value
        val cell = sheet.getCell(row, col)
        val value = cell?.displayValue ?: ""
        internalClipboardValue = value
        internalClipboardFormula = cell?.formula

        if (context != null && value.isNotEmpty()) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("Excel Cell", value))
        }
    }

    fun cut(sheet: ExcelSheetModel, context: Context?) {
        copy(sheet, context)
        val (row, col) = _activeCell.value
        sheet.setCellValue(row, col, "")
    }

    fun paste(sheet: ExcelSheetModel, context: Context?) {
        val (row, col) = _activeCell.value
        var valueToPaste = internalClipboardValue
        val formulaToPaste = internalClipboardFormula

        if (context != null) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = cm?.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clipText.isNullOrEmpty()) {
                valueToPaste = clipText
            }
        }

        if (valueToPaste != null) {
            sheet.setCellValue(row, col, valueToPaste, formulaToPaste)
        }
    }
}
