package com.example.oneread.excel.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.excel.model.CellRange
import com.example.oneread.excel.model.ExcelCellModel
import com.example.oneread.excel.model.ExcelSheetModel
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Professional spreadsheet grid renderer with:
 * - Default crisp WHITE spreadsheet canvas
 * - Proportional zoom scaling (cells, headers, fonts, heights, widths)
 * - Pinch-to-zoom multi-touch gesture support
 * - Frozen column letter headers (A, B, C...)
 * - Frozen row number headers (1, 2, 3...)
 * - Active cell highlight with drag handle and clear readability
 * - Subtle row tracking highlight so users can follow rows horizontally
 * - Preserved Excel formatting (colors, bold, spans)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExcelGridRenderer(
    sheet: ExcelSheetModel,
    activeCell: Pair<Int, Int>, // row, col (1-based)
    selectedRange: CellRange?,
    zoomLevel: Float = 1.0f,
    onPinchZoom: (Float) -> Unit = {},
    isDarkSpreadsheetTheme: Boolean = false,
    onSelectCell: (Int, Int) -> Unit,
    onSelectRow: (Int) -> Unit,
    onSelectColumn: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onCellDoubleClick: (Int, Int) -> Unit,
    onCellLongClick: (Int, Int) -> Unit,
    searchHighlightCell: Pair<Int, Int>? = null,
    modifier: Modifier = Modifier
) {
    val totalRows = maxOf(sheet.maxRow, 30)
    val totalCols = maxOf(sheet.maxCol, 12)

    val effectiveZoom = zoomLevel.coerceIn(0.5f, 3.0f)
    val digits = totalRows.toString().length
    val baseWidth = maxOf(46f, 32f + digits * 8f)
    val rowHeaderWidth = (baseWidth * effectiveZoom).dp.coerceAtLeast(36.dp)
    val headerHeight = (28 * effectiveZoom).dp.coerceAtLeast(24.dp)
    val defaultRowHeight = (32 * effectiveZoom).dp.coerceAtLeast(24.dp)

    val currentZoomState = rememberUpdatedState(effectiveZoom)
    val onPinchZoomState = rememberUpdatedState(onPinchZoom)

    val horizontalScrollState = rememberScrollState()
    val lazyListState = rememberLazyListState()

    // Auto-scroll to search hit if active
    LaunchedEffect(searchHighlightCell) {
        if (searchHighlightCell != null) {
            val targetRow = searchHighlightCell.first
            lazyListState.animateScrollToItem((targetRow - 1).coerceAtLeast(0))
        }
    }

    // Grid Container with Pinch-to-Zoom
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkSpreadsheetTheme) Color(0xFF0F172A) else Color.White)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var isPinching = false
                    var prevDist = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val activePointers = event.changes.filter { it.pressed }
                        if (activePointers.size >= 2) {
                            // Intercept and consume touch events so that scrolling is never triggered while pinching
                            event.changes.forEach { it.consume() }
                            val p0 = activePointers[0]
                            val p1 = activePointers[1]
                            val dx = p0.position.x - p1.position.x
                            val dy = p0.position.y - p1.position.y
                            val currentDist = hypot(dx, dy)

                            if (isPinching && prevDist > 0f) {
                                val scaleFactor = currentDist / prevDist
                                if (abs(scaleFactor - 1f) > 0.003f) {
                                    val newZoom = (currentZoomState.value * scaleFactor).coerceIn(0.5f, 3.0f)
                                    onPinchZoomState.value(newZoom)
                                }
                            }
                            prevDist = currentDist
                            isPinching = true
                        } else {
                            isPinching = false
                            prevDist = 0f
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // =========================================================================
            // 1. FROZEN TOP ROW: [ Corner Box ] + [ Column Headers: A, B, C, D... ]
            // =========================================================================
            val colHeaderBg = if (isDarkSpreadsheetTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
            val headerBorderColor = if (isDarkSpreadsheetTheme) Color(0xFF334155) else Color(0xFFCBD5E1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(colHeaderBg)
            ) {
                // Top-Left Corner Header (Select All)
                Surface(
                    color = colHeaderBg,
                    modifier = Modifier
                        .width(rowHeaderWidth)
                        .height(headerHeight)
                        .clickable(onClick = onSelectAll)
                        .border(0.6.dp, headerBorderColor)
                        .testTag("excel_corner_select_all")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "◢",
                            color = if (isDarkSpreadsheetTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                            fontSize = (10f * effectiveZoom.coerceIn(0.8f, 1.4f)).sp
                        )
                    }
                }

                // Horizontally Scrolled Column Letter Headers (Pinned vertically)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    for (c in 1..totalCols) {
                        val colLetters = ExcelCellModel.colIndexToLetters(c)
                        val colWidth = (sheet.getColumnWidth(c) * effectiveZoom).dp.coerceAtLeast((40 * effectiveZoom).dp)
                        val isColActive = activeCell.second == c || (selectedRange != null && c in selectedRange.startCol..selectedRange.endCol)

                        val cellColBg = when {
                            isColActive -> if (isDarkSpreadsheetTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                            else -> colHeaderBg
                        }
                        val cellColTextColor = when {
                            isColActive -> Color(0xFF10B981)
                            isDarkSpreadsheetTheme -> Color(0xFF94A3B8)
                            else -> Color(0xFF475569)
                        }

                        Surface(
                            color = cellColBg,
                            modifier = Modifier
                                .width(colWidth)
                                .height(headerHeight)
                                .clickable { onSelectColumn(c) }
                                .border(0.6.dp, headerBorderColor)
                                .testTag("excel_col_header_$c")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = colLetters,
                                    color = cellColTextColor,
                                    fontWeight = if (isColActive) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = (11f * effectiveZoom.coerceIn(0.75f, 1.4f)).sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. MAIN VIRTUALIZED BODY: [ Frozen Row Numbers ] + [ Grid Cells ]
            // =========================================================================
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(
                    count = totalRows,
                    key = { rowIndex -> rowIndex + 1 }
                ) { rowIndex ->
                    val r = rowIndex + 1
                    val rowHeight = (sheet.getRowHeight(r) * effectiveZoom).dp.coerceAtLeast(defaultRowHeight)
                    val isRowActive = activeCell.first == r || (selectedRange != null && r in selectedRange.startRow..selectedRange.endRow)

                    val rowHeaderBg = when {
                        isRowActive -> if (isDarkSpreadsheetTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                        else -> colHeaderBg
                    }
                    val rowHeaderTextColor = when {
                        isRowActive -> Color(0xFF10B981)
                        isDarkSpreadsheetTheme -> Color(0xFF94A3B8)
                        else -> Color(0xFF475569)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                    ) {
                        // Frozen Left Row Number Header (1, 2, 3...)
                        Surface(
                            color = rowHeaderBg,
                            modifier = Modifier
                                .width(rowHeaderWidth)
                                .height(rowHeight)
                                .clickable { onSelectRow(r) }
                                .border(0.6.dp, headerBorderColor)
                                .testTag("excel_row_header_$r")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "$r",
                                    color = rowHeaderTextColor,
                                    fontWeight = if (isRowActive) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = (11f * effectiveZoom.coerceIn(0.75f, 1.4f)).sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }

                        // Horizontally Scrolled Cells in this Row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            for (c in 1..totalCols) {
                                val cell = sheet.getCell(r, c)
                                val colWidth = (sheet.getColumnWidth(c) * effectiveZoom).dp.coerceAtLeast((40 * effectiveZoom).dp)
                                val isSelected = activeCell.first == r && activeCell.second == c
                                val isInRange = selectedRange != null && selectedRange.contains(r, c)
                                val isSearchHit = searchHighlightCell?.first == r && searchHighlightCell.second == c

                                val isMergedHidden = sheet.isCellMergedHidden(r, c)
                                if (!isMergedHidden) {
                                    val span = sheet.getMergedSpan(r, c)
                                    val cellWidth = if (span != null) {
                                        var w = 0f
                                        for (sc in c until c + span.second) {
                                            w += sheet.getColumnWidth(sc) * effectiveZoom
                                        }
                                        w.dp
                                    } else {
                                        colWidth
                                    }

                                    ExcelGridCellView(
                                        cell = cell,
                                        row = r,
                                        col = c,
                                        width = cellWidth,
                                        height = rowHeight,
                                        isSelected = isSelected,
                                        isInRange = isInRange,
                                        isSearchHit = isSearchHit,
                                        isRowActive = isRowActive,
                                        effectiveZoom = effectiveZoom,
                                        isDarkSpreadsheetTheme = isDarkSpreadsheetTheme,
                                        onClick = { onSelectCell(r, c) },
                                        onDoubleClick = { onCellDoubleClick(r, c) },
                                        onLongClick = { onCellLongClick(r, c) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExcelGridCellView(
    cell: ExcelCellModel?,
    row: Int,
    col: Int,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    isInRange: Boolean,
    isSearchHit: Boolean,
    isRowActive: Boolean,
    effectiveZoom: Float,
    isDarkSpreadsheetTheme: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayVal = cell?.displayValue ?: ""
    val style = cell?.style

    // Cell Background
    val backgroundColor = when {
        isSearchHit -> Color(0xFFFEF08A) // Classic Excel yellow highlighter
        isSelected -> if (isDarkSpreadsheetTheme) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFFDBEAFE).copy(alpha = 0.65f)
        isInRange -> if (isDarkSpreadsheetTheme) Color(0xFF1E3A8A).copy(alpha = 0.25f) else Color(0xFFEFF6FF)
        style?.backgroundColor != null -> style.backgroundColor
        isRowActive -> if (isDarkSpreadsheetTheme) Color(0xFF151D2F) else Color(0xFFF8FAFC) // Row tracking subtle highlight
        else -> if (isDarkSpreadsheetTheme) Color(0xFF0F172A) else Color.White
    }

    // Cell Border
    val borderColor = when {
        isSearchHit -> Color(0xFFCA8A04)
        isSelected -> Color(0xFF10B981) // Emerald green Excel active selection
        isInRange -> Color(0xFF2563EB)
        else -> if (isDarkSpreadsheetTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    }

    val borderWidth = when {
        isSelected || isSearchHit -> (2.0f * effectiveZoom.coerceIn(0.9f, 1.3f)).dp
        isInRange -> 1.2.dp
        else -> 0.7.dp
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(backgroundColor)
            .border(borderWidth, borderColor)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = (6 * effectiveZoom.coerceIn(0.75f, 1.4f)).dp, vertical = (2 * effectiveZoom).dp)
            .testTag("excel_cell_${row}_${col}"),
        contentAlignment = when (style?.textAlign) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        if (displayVal.isNotEmpty()) {
            val textColor = when {
                isSearchHit -> Color(0xFF854D0E)
                isSelected -> if (isDarkSpreadsheetTheme) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                style?.textColor != null -> style.textColor
                row == 1 -> if (isDarkSpreadsheetTheme) Color(0xFF38BDF8) else Color(0xFF0F172A)
                else -> if (isDarkSpreadsheetTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            }

            Text(
                text = displayVal,
                color = textColor,
                fontSize = ((style?.fontSizeSp ?: 12f) * effectiveZoom).coerceIn(8f, 36f).sp,
                fontWeight = if (style?.isBold == true || row == 1) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Active Cell Corner Drag Handle Indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size((6 * effectiveZoom.coerceIn(0.8f, 1.3f)).dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF10B981), CircleShape)
            )
        }
    }
}
