package com.example.oneread.workspace.viewers

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun CsvViewerComponent(
    tab: DocumentTab,
    onUpdateTab: (DocumentTab) -> Unit,
    onSaveContent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var tableData by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var sortedColumn by remember { mutableIntStateOf(-1) }
    var isSortAscending by remember { mutableStateOf(true) }

    // Cell editing dialog state
    var editingCell by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(tab.id) {
        if (tab.contentText != null) {
            tableData = parseCsvText(tab.contentText)
            isLoading = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                var targetFile = if (tab.filePath.isNotBlank()) File(tab.filePath) else null
                if (targetFile == null || !targetFile.exists()) {
                    if (tab.uriString.isNotBlank()) {
                        val uri = Uri.parse(tab.uriString)
                        val cacheFile = File(context.cacheDir, "csv_tab_${tab.id}.csv")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        }
                        targetFile = cacheFile
                    }
                }

                if (targetFile != null && targetFile.exists()) {
                    val rawText = targetFile.readText()
                    tableData = parseCsvText(rawText)
                } else {
                    tableData = listOf(
                        listOf("ID", "Customer", "Product", "Revenue", "Region", "Status"),
                        listOf("101", "Acme Corp", "Enterprise Suite", "$12,500", "US-East", "Active"),
                        listOf("102", "Globex Inc", "Professional", "$6,800", "EMEA", "Active"),
                        listOf("103", "Soylent Co", "Starter Pack", "$2,100", "APAC", "Pending"),
                        listOf("104", "Initech", "Consulting", "$18,000", "US-West", "Active")
                    )
                }
            } catch (e: Exception) {
                tableData = listOf(listOf("Error loading CSV: ${e.message}"))
            } finally {
                isLoading = false
            }
        }
    }

    fun exportToCsvString(data: List<List<String>>): String {
        return data.joinToString("\n") { row ->
            row.joinToString(",") { cell ->
                if (cell.contains(',') || cell.contains('"') || cell.contains('\n')) {
                    "\"${cell.replace("\"", "\"\"")}\""
                } else {
                    cell
                }
            }
        }
    }

    val colCount = tableData.maxOfOrNull { it.size } ?: 1
    val headerRow = tableData.firstOrNull() ?: emptyList()
    val bodyRows = if (tableData.size > 1) tableData.drop(1) else emptyList()

    // Sorted body rows
    val displayBodyRows = remember(bodyRows, sortedColumn, isSortAscending) {
        if (sortedColumn == -1) bodyRows
        else {
            if (isSortAscending) {
                bodyRows.sortedBy { it.getOrNull(sortedColumn) ?: "" }
            } else {
                bodyRows.sortedByDescending { it.getOrNull(sortedColumn) ?: "" }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        DocumentViewerToolbar(
            tab = tab,
            onSave = {
                val csvContent = exportToCsvString(tableData)
                onSaveContent(csvContent)
            },
            canSave = true,
            customLeadingContent = {
                Surface(
                    color = Color(0xFF0D9488).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${tableData.size} rows • $colCount cols",
                        color = Color(0xFF2DD4BF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        )

        if (isLoading) {
            DocumentLoadingState("Parsing CSV data…")
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Header Row
                    item {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF1E293B))
                                .border(0.5.dp, Color(0xFF334155))
                        ) {
                            // Row Number corner
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 34.dp)
                                    .background(Color(0xFF0F172A))
                                    .border(0.5.dp, Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("#", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            for (c in 0 until colCount) {
                                val colTitle = headerRow.getOrNull(c) ?: "Col ${c + 1}"
                                val isCurrentSorted = (sortedColumn == c)

                                Row(
                                    modifier = Modifier
                                        .size(width = 120.dp, height = 34.dp)
                                        .background(Color(0xFF1E293B))
                                        .border(0.5.dp, Color(0xFF334155))
                                        .clickable {
                                            if (sortedColumn == c) {
                                                isSortAscending = !isSortAscending
                                            } else {
                                                sortedColumn = c
                                                isSortAscending = true
                                            }
                                        }
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = colTitle,
                                        color = if (isCurrentSorted) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isCurrentSorted) {
                                        Icon(
                                            imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = "Sort direction",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Body Rows
                    itemsIndexed(displayBodyRows) { rowIdx, rowCells ->
                        val originalRowIndex = rowIdx + 1
                        Row(
                            modifier = Modifier
                                .background(if (rowIdx % 2 == 0) Color(0xFF0F172A) else Color(0xFF141E33))
                                .border(0.5.dp, Color(0xFF1E293B))
                        ) {
                            // Row number
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 32.dp)
                                    .background(Color(0xFF1E293B))
                                    .border(0.5.dp, Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$originalRowIndex",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Cells
                            for (c in 0 until colCount) {
                                val cellVal = rowCells.getOrNull(c) ?: ""
                                Box(
                                    modifier = Modifier
                                        .size(width = 120.dp, height = 32.dp)
                                        .border(0.5.dp, Color(0xFF1E293B))
                                        .clickable {
                                            // Open edit cell dialog
                                            editingCell = Triple(originalRowIndex, c, cellVal)
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = cellVal,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cell editing dialog
        editingCell?.let { (r, c, currentValue) ->
            var newValue by remember { mutableStateOf(currentValue) }
            val colHeader = headerRow.getOrNull(c) ?: "Column ${c + 1}"

            AlertDialog(
                onDismissRequest = { editingCell = null },
                title = {
                    Text("Edit Cell (Row $r, $colHeader)")
                },
                text = {
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Value") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val mutableList = tableData.map { it.toMutableList() }.toMutableList()
                            if (r in mutableList.indices && c in mutableList[r].indices) {
                                mutableList[r][c] = newValue
                                tableData = mutableList
                                val updatedCsv = exportToCsvString(mutableList)
                                onUpdateTab(
                                    tab.copy(
                                        contentText = updatedCsv,
                                        isModified = true
                                    )
                                )
                            }
                            editingCell = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                    ) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingCell = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun parseCsvText(text: String): List<List<String>> {
    val lines = text.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()

    // Delimiter detection
    val sample = lines.first()
    val delimiter = when {
        sample.count { it == '\t' } >= 1 -> '\t'
        sample.count { it == ';' } > sample.count { it == ',' } -> ';'
        sample.count { it == '|' } > sample.count { it == ',' } -> '|'
        else -> ','
    }

    return lines.map { line ->
        parseCsvLine(line, delimiter)
    }
}

private fun parseCsvLine(line: String, delimiter: Char): List<String> {
    val tokens = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < line.length) {
        val ch = line[i]
        if (ch == '\"') {
            if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                sb.append('\"')
                i++
            } else {
                inQuotes = !inQuotes
            }
        } else if (ch == delimiter && !inQuotes) {
            tokens.add(sb.toString().trim())
            sb.clear()
        } else {
            sb.append(ch)
        }
        i++
    }
    tokens.add(sb.toString().trim())
    return tokens
}
