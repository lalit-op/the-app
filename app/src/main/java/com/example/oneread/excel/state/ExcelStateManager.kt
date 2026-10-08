package com.example.oneread.excel.state

import com.example.oneread.excel.model.ExcelCellModel
import com.example.oneread.excel.model.ExcelWorkbookModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CellEditAction(
    val sheetIndex: Int,
    val row: Int,
    val col: Int,
    val previousValue: String,
    val previousFormula: String?,
    val newValue: String,
    val newFormula: String?
)

data class SheetScrollState(
    val scrollX: Int = 0,
    val scrollY: Int = 0,
    val zoomLevel: Float = 1.0f
)

class ExcelStateManager(val workbook: ExcelWorkbookModel) {

    private val undoStack = mutableListOf<CellEditAction>()
    private val redoStack = mutableListOf<CellEditAction>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val sheetScrollStates = mutableMapOf<Int, SheetScrollState>()

    fun recordEdit(sheetIndex: Int, row: Int, col: Int, newValue: String, newFormula: String?) {
        val sheet = workbook.sheets.getOrNull(sheetIndex) ?: return
        val currentCell = sheet.getCell(row, col)
        val prevVal = currentCell?.rawValue ?: ""
        val prevFormula = currentCell?.formula

        val action = CellEditAction(
            sheetIndex = sheetIndex,
            row = row,
            col = col,
            previousValue = prevVal,
            previousFormula = prevFormula,
            newValue = newValue,
            newFormula = newFormula
        )

        undoStack.add(action)
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
        workbook.isModified = true

        sheet.setCellValue(row, col, newValue, newFormula)
    }

    fun undo(): CellEditAction? {
        if (undoStack.isEmpty()) return null
        val action = undoStack.removeAt(undoStack.lastIndex)
        val sheet = workbook.sheets.getOrNull(action.sheetIndex)
        sheet?.setCellValue(action.row, action.col, action.previousValue, action.previousFormula)

        redoStack.add(action)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
        workbook.isModified = true
        return action
    }

    fun redo(): CellEditAction? {
        if (redoStack.isEmpty()) return null
        val action = redoStack.removeAt(redoStack.lastIndex)
        val sheet = workbook.sheets.getOrNull(action.sheetIndex)
        sheet?.setCellValue(action.row, action.col, action.newValue, action.newFormula)

        undoStack.add(action)
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()
        workbook.isModified = true
        return action
    }

    fun getScrollState(sheetIndex: Int): SheetScrollState {
        return sheetScrollStates[sheetIndex] ?: SheetScrollState()
    }

    fun saveScrollState(sheetIndex: Int, scrollX: Int, scrollY: Int, zoom: Float) {
        sheetScrollStates[sheetIndex] = SheetScrollState(scrollX, scrollY, zoom)
    }
}
