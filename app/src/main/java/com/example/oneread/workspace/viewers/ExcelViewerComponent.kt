package com.example.oneread.workspace.viewers

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.excel.loader.ExcelDocumentLoader
import com.example.oneread.workspace.model.DocumentTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

@Composable
fun ExcelViewerComponent(
    tab: DocumentTab,
    onUpdateTab: (DocumentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sheets by remember { mutableStateOf<List<List<List<String>>>>(emptyList()) }
    var sheetNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeSheetIndex by remember { mutableIntStateOf(tab.currentPage.coerceAtLeast(1) - 1) }

    // Selected cell for formula / content inspection
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(Pair(0, 0)) }

    val horizontalScrollState = rememberScrollState()

    var loadNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tab.filePath, tab.uriString, tab.originalFileName, tab.title) {
        isLoading = true
        errorMessage = null
        loadNotice = null
        try {
            val parsed = withContext(Dispatchers.IO) {
                val sourceName = tab.originalFileName.ifBlank { tab.title }
                val extension = sourceName.substringAfterLast('.', "").lowercase()
                val supportedExtensions = setOf("xls", "xlsx", "xlsm", "xlt", "xltx", "xltm", "csv", "tsv", "xml")
                if (extension !in supportedExtensions) {
                    throw IllegalArgumentException("Unsupported spreadsheet format: ${extension.ifBlank { "(unknown)" }}")
                }

                val existingFile = File(tab.filePath)
                val targetFile = if (existingFile.isFile && existingFile.canRead()) {
                    existingFile
                } else {
                    if (tab.uriString.isBlank()) {
                        throw IllegalArgumentException("The spreadsheet file is not accessible. Reopen it from the file picker.")
                    }
                    val uri = Uri.parse(tab.uriString)
                    val cacheFile = File(context.cacheDir, "excel_tab_${tab.id}.$extension")
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Android could not open the selected spreadsheet.")
                    input.use { source ->
                        cacheFile.outputStream().use { sink -> source.copyTo(sink) }
                    }
                    if (!cacheFile.isFile || cacheFile.length() == 0L) {
                        throw IllegalArgumentException("The selected spreadsheet is empty or could not be copied.")
                    }
                    cacheFile
                }

                val workbook = ExcelDocumentLoader.loadWorkbook(targetFile)
                if (workbook.sheets.isEmpty()) {
                    throw IllegalArgumentException("No worksheets could be read from this file.")
                }

                val maxRowsPerTab = 2000
                val maxColumnsPerTab = 128
                var clipped = false
                val matrixSheets = workbook.sheets.map { sheet ->
                    val actualRows = maxOf(sheet.maxRow, sheet.cells.values.maxOfOrNull { it.row } ?: 0)
                    val actualColumns = maxOf(sheet.maxCol, sheet.cells.values.maxOfOrNull { it.col } ?: 0)
                    val rowsToShow = actualRows.coerceAtLeast(20).coerceAtMost(maxRowsPerTab)
                    val columnsToShow = actualColumns.coerceAtLeast(6).coerceAtMost(maxColumnsPerTab)
                    if (actualRows > maxRowsPerTab || actualColumns > maxColumnsPerTab) clipped = true
                    (1..rowsToShow).map { row ->
                        (1..columnsToShow).map { col -> sheet.getCellValue(row, col) }
                    }
                }
                Triple(matrixSheets, workbook.sheets.map { it.name }, clipped)
            }

            sheets = parsed.first
            sheetNames = parsed.second
            loadNotice = if (parsed.third) {
                "Preview limited to the first 2,000 rows and 128 columns per sheet for memory safety."
            } else {
                null
            }
            activeSheetIndex = activeSheetIndex.coerceIn(0, sheets.lastIndex)
            onUpdateTab(tab.copy(totalPages = sheets.size))
        } catch (e: Exception) {
            sheets = emptyList()
            sheetNames = emptyList()
            errorMessage = e.message ?: "Could not open this spreadsheet."
        } finally {
            isLoading = false
        }
    }

    val currentSheet = sheets.getOrNull(activeSheetIndex) ?: emptyList()
    val colCount = currentSheet.maxOfOrNull { it.size }?.coerceAtLeast(6) ?: 6
    val rowCount = currentSheet.size.coerceAtLeast(20)

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        DocumentViewerToolbar(
            tab = tab,
            customLeadingContent = {
                Surface(
                    color = Color(0xFF16A34A).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${currentSheet.size} rows × $colCount cols",
                        color = Color(0xFF4ADE80),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        )

        // Formula / Cell Inspector Bar
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val r = selectedCell?.first ?: 0
                val c = selectedCell?.second ?: 0
                val colLetter = getColumnLetter(c)
                val cellAddress = "$colLetter${r + 1}"
                val cellValue = currentSheet.getOrNull(r)?.getOrNull(c) ?: ""

                // Address box
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = cellAddress,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "fx",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = cellValue.ifBlank { "(empty cell)" },
                        color = if (cellValue.isBlank()) Color(0xFF64748B) else Color(0xFFF1F5F9),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (loadNotice != null && !isLoading && errorMessage == null) {
            Text(
                text = loadNotice.orEmpty(),
                color = Color(0xFFFBBF24),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        if (isLoading) {
            DocumentLoadingState("Loading spreadsheet data…")
        } else if (errorMessage != null) {
            DocumentErrorState(errorMessage ?: "Error loading Excel")
        } else {
            // Interactive Spreadsheet Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Column Letters Row (A, B, C, D...)
                    item {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF1E293B))
                                .border(0.5.dp, Color(0xFF334155))
                        ) {
                            // Top-left corner
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 28.dp)
                                    .background(Color(0xFF0F172A))
                                    .border(0.5.dp, Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("▦", color = Color(0xFF64748B), fontSize = 11.sp)
                            }

                            for (col in 0 until colCount) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 28.dp)
                                        .background(Color(0xFF1E293B))
                                        .border(0.5.dp, Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getColumnLetter(col),
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Grid Data Rows
                    items(rowCount) { rowIdx ->
                        val rowData = currentSheet.getOrNull(rowIdx) ?: emptyList()
                        val isHeaderRow = (rowIdx == 0)

                        Row(
                            modifier = Modifier
                                .background(if (isHeaderRow) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFF0F172A))
                                .border(0.5.dp, Color(0xFF1E293B))
                        ) {
                            // Row Number Gutter (1, 2, 3...)
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 30.dp)
                                    .background(Color(0xFF1E293B))
                                    .border(0.5.dp, Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rowIdx + 1}",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Cells
                            for (colIdx in 0 until colCount) {
                                val cellText = rowData.getOrNull(colIdx) ?: ""
                                val isSelected = (selectedCell?.first == rowIdx && selectedCell?.second == colIdx)

                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 30.dp)
                                        .background(
                                            when {
                                                isSelected -> Color(0xFF0284C7).copy(alpha = 0.35f)
                                                isHeaderRow -> Color(0xFF1E293B)
                                                rowIdx % 2 == 0 -> Color(0xFF0F172A)
                                                else -> Color(0xFF141E33)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)
                                        )
                                        .clickable {
                                            selectedCell = Pair(rowIdx, colIdx)
                                        }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = cellText,
                                        color = when {
                                            isHeaderRow -> Color(0xFFE2E8F0)
                                            isSelected -> Color.White
                                            else -> Color(0xFFCBD5E1)
                                        },
                                        fontWeight = if (isHeaderRow) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sheet selector tabs at bottom
            if (sheetNames.size > 1) {
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = activeSheetIndex,
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF4ADE80),
                        edgePadding = 8.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeSheetIndex]),
                                color = Color(0xFF16A34A)
                            )
                        }
                    ) {
                        sheetNames.forEachIndexed { index, name ->
                            Tab(
                                selected = (activeSheetIndex == index),
                                onClick = {
                                    activeSheetIndex = index
                                    onUpdateTab(tab.copy(currentPage = index + 1))
                                },
                                text = {
                                    Text(
                                        text = name,
                                        color = if (activeSheetIndex == index) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                                        fontWeight = if (activeSheetIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getColumnLetter(index: Int): String {
    var num = index
    var letter = ""
    while (num >= 0) {
        letter = ('A' + (num % 26)).toString() + letter
        num = (num / 26) - 1
    }
    return letter
}
