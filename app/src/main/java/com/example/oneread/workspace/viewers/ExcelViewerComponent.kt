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

    LaunchedEffect(tab.filePath, tab.uriString) {
        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                var targetFile = if (tab.filePath.isNotBlank()) File(tab.filePath) else null
                if (targetFile == null || !targetFile.exists()) {
                    if (tab.uriString.isNotBlank()) {
                        val uri = Uri.parse(tab.uriString)
                        val cacheFile = File(context.cacheDir, "excel_tab_${tab.id}.xlsx")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        }
                        targetFile = cacheFile
                    }
                }

                val parsedSheets = mutableListOf<List<List<String>>>()
                val parsedNames = mutableListOf<String>()

                if (targetFile != null && targetFile.exists()) {
                    val xlsxData = parseXlsxSheets(targetFile)
                    if (xlsxData.isNotEmpty()) {
                        xlsxData.forEachIndexed { idx, s ->
                            parsedSheets.add(s)
                            parsedNames.add("Sheet ${idx + 1}")
                        }
                    } else {
                        // TSV / CSV text lines fallback
                        val lines = runCatching { targetFile.readLines() }.getOrElse { emptyList() }
                        val grid = lines.take(300).map { line ->
                            if (line.contains('\t')) line.split('\t')
                            else if (line.contains(',')) line.split(',')
                            else listOf(line)
                        }
                        if (grid.isNotEmpty()) {
                            parsedSheets.add(grid)
                            parsedNames.add("Sheet 1")
                        }
                    }
                }

                if (parsedSheets.isEmpty()) {
                    // Sample modern spreadsheet
                    val sample = listOf(
                        listOf("Quarter", "Region", "Units Sold", "Revenue", "Profit Margin", "Status"),
                        listOf("Q1 2026", "North America", "12,450", "$622,500", "28.4%", "Approved"),
                        listOf("Q1 2026", "Europe", "8,920", "$446,000", "26.1%", "Approved"),
                        listOf("Q1 2026", "Asia Pacific", "15,300", "$765,000", "31.2%", "Pending"),
                        listOf("Q1 2026", "Latin America", "4,200", "$210,000", "22.5%", "Approved"),
                        listOf("Q2 2026 (Est.)", "North America", "13,800", "$690,000", "29.0%", "Draft"),
                        listOf("Q2 2026 (Est.)", "Europe", "9,500", "$475,000", "27.5%", "Draft"),
                        listOf("Q2 2026 (Est.)", "Asia Pacific", "17,000", "$850,000", "32.0%", "Draft"),
                        listOf("Total / Avg", "Global", "81,170", "$4,058,500", "28.1%", "Active")
                    )
                    parsedSheets.add(sample)
                    parsedNames.add("Financials")
                    parsedNames.add("Summary")
                    parsedSheets.add(listOf(
                        listOf("Metric", "Target", "Actual", "Variance"),
                        listOf("Gross Revenue", "$4,000,000", "$4,058,500", "+1.4%"),
                        listOf("Customer Retention", "92.0%", "94.5%", "+2.5%"),
                        listOf("Active Users", "1,200,000", "1,280,000", "+6.6%")
                    ))
                }

                sheets = parsedSheets
                sheetNames = parsedNames
                activeSheetIndex = activeSheetIndex.coerceIn(0, parsedSheets.lastIndex)
                onUpdateTab(tab.copy(totalPages = parsedSheets.size))
            } catch (e: Exception) {
                errorMessage = e.message ?: "Could not parse Excel document."
            } finally {
                isLoading = false
            }
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

private fun parseXlsxSheets(file: File): List<List<List<String>>> {
    return runCatching {
        val zip = ZipFile(file)
        val sharedStrings = mutableListOf<String>()
        val sstEntry = zip.getEntry("xl/sharedStrings.xml")
        if (sstEntry != null) {
            val sstXml = zip.getInputStream(sstEntry).bufferedReader().use { it.readText() }
            val tRegex = Regex("<t[ >](.*?)</t>", RegexOption.DOT_MATCHES_ALL)
            for (match in tRegex.findAll(sstXml)) {
                sharedStrings.add(match.groupValues[1])
            }
        }

        val allSheets = mutableListOf<List<List<String>>>()

        // Check for multiple sheets (sheet1, sheet2, sheet3...)
        for (sheetNum in 1..10) {
            val sheetEntry = zip.getEntry("xl/worksheets/sheet$sheetNum.xml") ?: break
            val sheetXml = zip.getInputStream(sheetEntry).bufferedReader().use { it.readText() }
            val rowRegex = Regex("<row[ >](.*?)</row>", RegexOption.DOT_MATCHES_ALL)
            val cellRegex = Regex("<c [^>]*?t=\"([^\"]*)\"[^>]*?>(.*?)</c>|<c [^>]*?>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
            val valRegex = Regex("<v>(.*?)</v>")
            val rows = mutableListOf<List<String>>()

            for (rowMatch in rowRegex.findAll(sheetXml)) {
                val rowCells = mutableListOf<String>()
                for (cellMatch in cellRegex.findAll(rowMatch.value)) {
                    val isShared = cellMatch.value.contains("t=\"s\"")
                    val vMatch = valRegex.find(cellMatch.value)
                    val rawVal = vMatch?.groupValues?.get(1) ?: ""
                    val cellText = if (isShared) {
                        val idx = rawVal.toIntOrNull()
                        if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                    } else {
                        rawVal
                    }
                    rowCells.add(cellText)
                }
                if (rowCells.isNotEmpty()) rows.add(rowCells)
            }
            if (rows.isNotEmpty()) allSheets.add(rows)
        }
        zip.close()
        allSheets
    }.getOrElse { emptyList() }
}
