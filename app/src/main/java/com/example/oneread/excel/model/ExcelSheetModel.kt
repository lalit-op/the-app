package com.example.oneread.excel.model

/**
 * Represents a rectangular cell range (e.g. A1:C5).
 */
data class CellRange(
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int
) {
    val isSingleCell: Boolean
        get() = startRow == endRow && startCol == endCol

    fun contains(row: Int, col: Int): Boolean {
        return row in minOf(startRow, endRow)..maxOf(startRow, endRow) &&
                col in minOf(startCol, endCol)..maxOf(startCol, endCol)
    }

    val rangeRef: String
        get() {
            val startRef = "${ExcelCellModel.colIndexToLetters(minOf(startCol, endCol))}${minOf(startRow, endRow)}"
            val endRef = "${ExcelCellModel.colIndexToLetters(maxOf(startCol, endCol))}${maxOf(startRow, endRow)}"
            return if (isSingleCell) startRef else "$startRef:$endRef"
        }

    companion object {
        fun parse(rangeStr: String): CellRange? {
            val parts = rangeStr.trim().split(":")
            if (parts.size == 1) {
                val p = ExcelCellModel.parseCellRef(parts[0]) ?: return null
                return CellRange(p.first, p.second, p.first, p.second)
            } else if (parts.size == 2) {
                val p1 = ExcelCellModel.parseCellRef(parts[0]) ?: return null
                val p2 = ExcelCellModel.parseCellRef(parts[1]) ?: return null
                return CellRange(
                    minOf(p1.first, p2.first),
                    minOf(p1.second, p2.second),
                    maxOf(p1.first, p2.first),
                    maxOf(p1.second, p2.second)
                )
            }
            return null
        }
    }
}

/**
 * Placeholder for embedded charts or images in the spreadsheet.
 */
data class ExcelChartPlaceholder(
    val title: String,
    val range: CellRange,
    val chartType: String = "Chart"
)

/**
 * Represents a single worksheet in an Excel workbook.
 * Uses a memory-conscious sparse cell map so huge sparse sheets don't OOM.
 */
data class ExcelSheetModel(
    val name: String,
    val index: Int = 0,
    val maxRow: Int = 30,
    val maxCol: Int = 10,
    val cells: MutableMap<String, ExcelCellModel> = mutableMapOf(),
    val columnWidths: MutableMap<Int, Float> = mutableMapOf(), // Column index (1-based) to width dp
    val rowHeights: MutableMap<Int, Float> = mutableMapOf(), // Row index (1-based) to height dp
    var frozenRows: Int = 0, // Number of frozen header rows
    var frozenCols: Int = 0, // Number of frozen header columns
    val mergedRanges: MutableList<CellRange> = mutableListOf(),
    val charts: MutableList<ExcelChartPlaceholder> = mutableListOf()
) {
    fun getCell(row: Int, col: Int): ExcelCellModel? {
        return cells["$row:$col"]
    }

    fun getCellValue(row: Int, col: Int): String {
        return getCell(row, col)?.displayValue ?: ""
    }

    fun setCell(cell: ExcelCellModel) {
        cells["${cell.row}:${cell.col}"] = cell
    }

    fun setCellValue(row: Int, col: Int, value: String, formula: String? = null) {
        val existing = getCell(row, col)
        val updated = if (existing != null) {
            existing.copy(
                rawValue = value,
                formula = formula ?: if (value.startsWith("=")) value else null,
                cachedResult = if (formula != null || value.startsWith("=")) null else value
            )
        } else {
            ExcelCellModel(
                row = row,
                col = col,
                rawValue = value,
                formula = formula ?: if (value.startsWith("=")) value else null
            )
        }
        cells["$row:$col"] = updated
    }

    fun getColumnWidth(col: Int): Float {
        val explicit = columnWidths[col]
        // Dynamic auto-fit calculation from cells in this column (sample first 60 rows)
        var maxChars = 8
        for (r in 1..minOf(maxRow, 60)) {
            val cell = getCell(r, col)
            if (cell != null) {
                maxChars = maxOf(maxChars, cell.displayValue.length)
            }
        }
        val contentNeeded = (maxChars * 8.5f + 24f).coerceIn(88f, 500f)
        if (explicit != null) {
            // Preserve original Excel column width, but ensure long content is sufficiently readable
            return maxOf(explicit, minOf(contentNeeded, 240f))
        }
        return contentNeeded
    }

    fun getRowHeight(row: Int): Float {
        return rowHeights[row] ?: 32f // Default 32.dp
    }

    fun isCellMergedHidden(row: Int, col: Int): Boolean {
        for (range in mergedRanges) {
            if (range.contains(row, col)) {
                // If not top-left cell, it is hidden by the merge
                if (row != range.startRow || col != range.startCol) {
                    return true
                }
            }
        }
        return false
    }

    fun getMergedSpan(row: Int, col: Int): Pair<Int, Int>? {
        for (range in mergedRanges) {
            if (row == range.startRow && col == range.startCol) {
                val rowSpan = (range.endRow - range.startRow + 1)
                val colSpan = (range.endCol - range.startCol + 1)
                return Pair(rowSpan, colSpan)
            }
        }
        return null
    }
}
