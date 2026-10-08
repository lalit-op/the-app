package com.example.oneread.excel.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.oneread.data.DocumentItem
import com.example.oneread.excel.formula.ExcelFormulaEvaluator
import com.example.oneread.excel.loader.ExcelDocumentLoader
import com.example.oneread.excel.model.ExcelCellModel
import com.example.oneread.excel.model.ExcelWorkbookModel
import com.example.oneread.excel.search.ExcelSearchManager
import com.example.oneread.excel.selection.ExcelSelectionManager
import com.example.oneread.excel.state.ExcelStateManager
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.components.CommonViewerHeader
import com.example.oneread.ui.components.HRReadViewerShell
import kotlinx.coroutines.launch
import java.io.File

/**
 * Master Professional Excel Workspace for HR Read.
 * Supports .xlsx, .xls, .xlsm, .xltx, .xltm, .csv, and spreadsheet .xml.
 * Features virtualized grid, frozen headers, multiple sheets, formula evaluator,
 * background search & replace, undo/redo, and tab synchronization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelViewerScreen(
    document: DocumentItem,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val openTabs by viewModel.tabManager.tabs.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()
    val liveDocument = remember(allDocs, document) {
        allDocs.find { it.id == document.id } ?: document
    }

    var isLoading by remember { mutableStateOf(true) }
    var loadingMessage by remember { mutableStateOf("Opening workbook...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var workbook by remember { mutableStateOf<ExcelWorkbookModel?>(null) }
    var activeSheetIndex by remember { mutableIntStateOf(0) }

    val selectionManager = remember { ExcelSelectionManager() }
    var stateManager by remember { mutableStateOf<ExcelStateManager?>(null) }
    var searchManager by remember { mutableStateOf<ExcelSearchManager?>(null) }

    val activeCellCoords by selectionManager.activeCell.collectAsState()
    val selectedRange by selectionManager.selectedRange.collectAsState()

    var showSearchSheet by remember { mutableStateOf(false) }
    var showCellEditDialog by remember { mutableStateOf(false) }
    var showRenameSheetDialog by remember { mutableStateOf(false) }
    var renameSheetIndex by remember { mutableIntStateOf(0) }
    var renameSheetNewName by remember { mutableStateOf("") }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var searchHighlightCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Professional Excel Zoom System (Default: 100%)
    val initialTabZoom = viewModel.tabManager.activeTab?.zoomLevel ?: 1.0f
    var zoomLevel by remember(liveDocument.id) {
        mutableFloatStateOf(if (initialTabZoom in 0.5f..3.0f) initialTabZoom else 1.0f)
    }
    var showZoomSheet by remember { mutableStateOf(false) }
    var isDarkSpreadsheetTheme by remember { mutableStateOf(false) } // DEFAULT: WHITE SPREADSHEET CANVAS
    var availableGridWidthPx by remember { mutableFloatStateOf(1080f) }

    fun setZoom(newZoom: Float) {
        val clamped = newZoom.coerceIn(0.5f, 3.0f)
        zoomLevel = clamped
        viewModel.updateTabProgress(zoom = clamped)
        stateManager?.saveScrollState(activeSheetIndex, 0, 0, clamped)
    }

    fun zoomIn() {
        val currentPct = (zoomLevel * 100).roundToInt()
        val nextPct = when {
            currentPct < 150 -> ((currentPct / 10) + 1) * 10
            else -> ((currentPct / 25) + 1) * 25
        }.coerceIn(50, 300)
        setZoom(nextPct / 100f)
    }

    fun zoomOut() {
        val currentPct = (zoomLevel * 100).roundToInt()
        val nextPct = when {
            currentPct <= 150 -> ((currentPct - 1) / 10) * 10
            else -> ((currentPct - 1) / 25) * 25
        }.coerceIn(50, 300)
        setZoom(nextPct / 100f)
    }

    fun resetZoom() {
        setZoom(1.0f)
    }

    fun fitToWidth() {
        val sheet = workbook?.sheets?.getOrNull(activeSheetIndex) ?: return
        var totalW = 46f
        val colsToFit = minOf(sheet.maxCol, 6)
        for (c in 1..colsToFit) {
            totalW += sheet.getColumnWidth(c)
        }
        if (totalW > 0) {
            val density = context.resources.displayMetrics.density
            val availDp = availableGridWidthPx / density
            val target = (availDp / totalW).coerceIn(0.5f, 2.5f)
            val rounded = (target * 10).roundToInt() / 10f
            setZoom(rounded)
        }
    }

    // Load workbook on background thread
    fun loadFile() {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null
                val file = File(liveDocument.path)
                val loaded = if (file.exists()) {
                    ExcelDocumentLoader.loadWorkbook(file) { status ->
                        loadingMessage = status
                    }
                } else {
                    ExcelWorkbookModel.createEmpty(liveDocument.title)
                }
                workbook = loaded
                stateManager = ExcelStateManager(loaded)
                searchManager = ExcelSearchManager(loaded)
                activeSheetIndex = loaded.activeSheetIndex

                // Update tab state
                viewModel.updateTabProgress(
                    sheet = activeSheetIndex + 1,
                    totalPages = loaded.sheetCount
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unable to open this workbook."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(liveDocument.path) {
        loadFile()
    }

    // Protect unsaved changes on back press
    BackHandler {
        if (showSearchSheet) {
            showSearchSheet = false
        } else if (workbook?.isModified == true) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    val wb = workbook
    val currentSheet = wb?.sheets?.getOrNull(activeSheetIndex)
    val activeCell = currentSheet?.getCell(activeCellCoords.first, activeCellCoords.second)
    val formulaBarValue = activeCell?.formula ?: activeCell?.rawValue ?: ""

    HRReadViewerShell(
        modifier = modifier,
        header = {
            val titleWithModified = buildString {
                append(liveDocument.title)
                if (wb?.isModified == true) append(" ●")
            }

            CommonViewerHeader(
                title = titleWithModified,
                onBack = {
                    if (wb?.isModified == true) {
                        showUnsavedDialog = true
                    } else {
                        onBack()
                    }
                },
                isSearchActive = false,
                onSearchActiveChange = { active ->
                    if (active) showSearchSheet = true
                },
                searchQuery = "",
                onSearchQueryChange = {},
                testTagPrefix = "excel_viewer",
                openDocumentsCount = openTabs.size.coerceAtLeast(1),
                onDocumentSwitcherClick = { viewModel.openSwitcherSheet() },
                customActions = {
                    val canUndo = stateManager?.canUndo?.collectAsState()?.value ?: false
                    val canRedo = stateManager?.canRedo?.collectAsState()?.value ?: false

                    // Undo Button
                    IconButton(
                        onClick = { stateManager?.undo() },
                        enabled = canUndo,
                        modifier = Modifier.size(36.dp).testTag("excel_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = { stateManager?.redo() },
                        enabled = canRedo,
                        modifier = Modifier.size(36.dp).testTag("excel_redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Real Spreadsheet Zoom Control: [ - ]  100%  [ + ]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                            .height(30.dp)
                            .testTag("excel_zoom_toolbar_controls")
                    ) {
                        IconButton(
                            onClick = { zoomOut() },
                            enabled = zoomLevel > 0.5f,
                            modifier = Modifier.size(28.dp).testTag("excel_zoom_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom Out",
                                tint = if (zoomLevel > 0.5f) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                if (zoomLevel != 1.0f) {
                                    resetZoom()
                                    Toast.makeText(context, "Zoom reset to 100%", Toast.LENGTH_SHORT).show()
                                } else {
                                    showZoomSheet = true
                                }
                            },
                            color = Color.Transparent,
                            modifier = Modifier
                                .padding(horizontal = 1.dp)
                                .testTag("excel_zoom_indicator")
                        ) {
                            Text(
                                text = "${(zoomLevel * 100).roundToInt()}%",
                                color = if (zoomLevel == 1.0f) Color(0xFF38BDF8) else Color(0xFF10B981),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp)
                            )
                        }

                        IconButton(
                            onClick = { zoomIn() },
                            enabled = zoomLevel < 3.0f,
                            modifier = Modifier.size(28.dp).testTag("excel_zoom_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom In",
                                tint = if (zoomLevel < 3.0f) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Edit Cell Button
                    IconButton(
                        onClick = { showCellEditDialog = true },
                        modifier = Modifier.size(36.dp).testTag("excel_edit_cell_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Cell",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                overflowMenuItems = { onDismiss ->
                    DropdownMenuItem(
                        text = { Text("Zoom: ${(zoomLevel * 100).roundToInt()}%") },
                        leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color(0xFF10B981)) },
                        onClick = {
                            onDismiss()
                            showZoomSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reset Zoom (100%)") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            resetZoom()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Fit to Width") },
                        leadingIcon = { Icon(Icons.Default.FitScreen, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            fitToWidth()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isDarkSpreadsheetTheme) "Switch to White Canvas (Default)" else "Switch to Dark Canvas") },
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            isDarkSpreadsheetTheme = !isDarkSpreadsheetTheme
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save Document") },
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            coroutineScope.launch {
                                wb?.let { ExcelDocumentLoader.saveWorkbook(it, File(liveDocument.path)) }
                                Toast.makeText(context, "Spreadsheet saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Find & Replace") },
                        leadingIcon = { Icon(Icons.Default.FindReplace, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            showSearchSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add New Sheet") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            wb?.let {
                                it.addSheet()
                                activeSheetIndex = it.sheets.size - 1
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reload File") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            loadFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Document") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            viewModel.repository.shareDocument(context, liveDocument)
                        }
                    )
                }
            )
        }
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(loadingMessage, color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = errorMessage ?: "Unable to open this workbook.",
                        color = Color(0xFFF87171),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(
                            onClick = { loadFile() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("Retry")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        } else if (wb != null && currentSheet != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp) // Offset for CommonViewerHeader
            ) {
                // Formula Bar & Name Box
                ExcelFormulaBar(
                    nameBoxText = selectionManager.nameBoxText,
                    currentValue = formulaBarValue,
                    onValueCommit = { newValue ->
                        val (r, c) = activeCellCoords
                        val isFormula = newValue.startsWith("=")
                        stateManager?.recordEdit(
                            sheetIndex = activeSheetIndex,
                            row = r,
                            col = c,
                            newValue = newValue,
                            newFormula = if (isFormula) newValue else null
                        )
                        // Trigger recalculation if formula
                        if (isFormula) {
                            val eval = ExcelFormulaEvaluator.evaluate(newValue, currentSheet, wb)
                            currentSheet.setCell(
                                ExcelCellModel(
                                    row = r,
                                    col = c,
                                    rawValue = newValue,
                                    formula = newValue,
                                    cachedResult = eval
                                )
                            )
                        }
                    },
                    onFormulaBarClick = { showCellEditDialog = true }
                )

                // Virtualized Spreadsheet Grid with BoxWithConstraints for responsive Fit to Width
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    availableGridWidthPx = constraints.maxWidth.toFloat()
                    ExcelGridRenderer(
                        sheet = currentSheet,
                        activeCell = activeCellCoords,
                        selectedRange = selectedRange,
                        zoomLevel = zoomLevel,
                        onPinchZoom = { setZoom(it) },
                        isDarkSpreadsheetTheme = isDarkSpreadsheetTheme,
                        onSelectCell = { r, c ->
                            selectionManager.selectCell(r, c)
                            searchHighlightCell = null
                        },
                        onSelectRow = { r -> selectionManager.selectRow(r, currentSheet.maxCol) },
                        onSelectColumn = { c -> selectionManager.selectColumn(c, currentSheet.maxRow) },
                        onSelectAll = { selectionManager.selectAll(currentSheet.maxRow, currentSheet.maxCol) },
                        onCellDoubleClick = { r, c ->
                            selectionManager.selectCell(r, c)
                            showCellEditDialog = true
                        },
                        onCellLongClick = { r, c ->
                            selectionManager.selectCell(r, c)
                            showCellEditDialog = true
                        },
                        searchHighlightCell = searchHighlightCell,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Sheet Tabs Bar at bottom: [Sheet1] [Sales] [Expenses] [+]
                ExcelSheetTabsBar(
                    sheetNames = wb.sheetNames,
                    activeSheetIndex = activeSheetIndex,
                    onSelectSheet = { idx ->
                        stateManager?.saveScrollState(activeSheetIndex, 0, 0, zoomLevel)
                        activeSheetIndex = idx
                        wb.activeSheetIndex = idx
                        val savedState = stateManager?.getScrollState(idx)
                        if (savedState != null && savedState.zoomLevel in 0.5f..3.0f) {
                            zoomLevel = savedState.zoomLevel
                        }
                        viewModel.updateTabProgress(sheet = idx + 1, totalPages = wb.sheetCount, zoom = zoomLevel)
                    },
                    onAddSheet = {
                        wb.addSheet()
                        activeSheetIndex = wb.sheets.size - 1
                        viewModel.updateTabProgress(sheet = activeSheetIndex + 1, totalPages = wb.sheetCount, zoom = zoomLevel)
                    },
                    onRenameSheet = { idx ->
                        renameSheetIndex = idx
                        renameSheetNewName = wb.sheets.getOrNull(idx)?.name ?: ""
                        showRenameSheetDialog = true
                    },
                    onDuplicateSheet = { idx ->
                        wb.duplicateSheet(idx)
                        activeSheetIndex = idx + 1
                    },
                    onDeleteSheet = { idx ->
                        wb.deleteSheet(idx)
                        activeSheetIndex = activeSheetIndex.coerceIn(0, maxOf(0, wb.sheets.size - 1))
                    },
                    onMoveSheetLeft = { idx ->
                        wb.moveSheet(idx, idx - 1)
                        activeSheetIndex = (activeSheetIndex - 1).coerceAtLeast(0)
                    },
                    onMoveSheetRight = { idx ->
                        wb.moveSheet(idx, idx + 1)
                        activeSheetIndex = (activeSheetIndex + 1).coerceAtMost(wb.sheets.size - 1)
                    }
                )
            }
        }

        // Spreadsheet Zoom Bottom Sheet
        if (showZoomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showZoomSheet = false },
                containerColor = Color(0xFF0F172A),
                scrimColor = Color.Black.copy(alpha = 0.55f)
            ) {
                ExcelZoomDialog(
                    currentZoom = zoomLevel,
                    onZoomChange = { setZoom(it) },
                    onFitToWidth = { fitToWidth() },
                    onDismiss = { showZoomSheet = false }
                )
            }
        }

        // Search and Replace Bottom Sheet
        if (showSearchSheet && searchManager != null) {
            ModalBottomSheet(
                onDismissRequest = { showSearchSheet = false },
                containerColor = Color(0xFF0F172A),
                scrimColor = Color.Black.copy(alpha = 0.55f)
            ) {
                ExcelSearchDialog(
                    searchManager = searchManager!!,
                    onNavigateToMatch = { match ->
                        activeSheetIndex = match.sheetIndex
                        wb?.activeSheetIndex = match.sheetIndex
                        selectionManager.selectCell(match.row, match.col)
                        searchHighlightCell = Pair(match.row, match.col)
                    },
                    onDismiss = { showSearchSheet = false }
                )
            }
        }

        // Cell Edit Dialog
        if (showCellEditDialog && currentSheet != null) {
            val (r, c) = activeCellCoords
            val cell = currentSheet.getCell(r, c)
            ModalBottomSheet(
                onDismissRequest = { showCellEditDialog = false },
                containerColor = Color(0xFF0F172A),
                scrimColor = Color.Black.copy(alpha = 0.55f)
            ) {
                ExcelCellEditDialog(
                    cellRef = selectionManager.nameBoxText,
                    initialValue = cell?.rawValue ?: "",
                    initialFormula = cell?.formula,
                    onCommit = { value, formula ->
                        stateManager?.recordEdit(activeSheetIndex, r, c, value, formula)
                        if (formula != null) {
                            val eval = ExcelFormulaEvaluator.evaluate(formula, currentSheet, wb)
                            currentSheet.setCell(
                                ExcelCellModel(
                                    row = r,
                                    col = c,
                                    rawValue = value,
                                    formula = formula,
                                    cachedResult = eval
                                )
                            )
                        }
                    },
                    onCopy = { selectionManager.copy(currentSheet, context) },
                    onCut = { selectionManager.cut(currentSheet, context) },
                    onPaste = { selectionManager.paste(currentSheet, context) },
                    onDismiss = { showCellEditDialog = false }
                )
            }
        }

        // Rename Sheet Dialog
        if (showRenameSheetDialog) {
            AlertDialog(
                onDismissRequest = { showRenameSheetDialog = false },
                title = { Text("Rename Sheet", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = renameSheetNewName,
                        onValueChange = { renameSheetNewName = it },
                        label = { Text("Sheet Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF10B981)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRenameSheetDialog = false
                            wb?.renameSheet(renameSheetIndex, renameSheetNewName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameSheetDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Unsaved Changes Protection Dialog
        if (showUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedDialog = false },
                title = { Text("Save changes?", color = Color.White) },
                text = { Text("Save changes to ${liveDocument.title}?", color = Color(0xFFCBD5E1)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showUnsavedDialog = false
                            coroutineScope.launch {
                                wb?.let { ExcelDocumentLoader.saveWorkbook(it, File(liveDocument.path)) }
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            showUnsavedDialog = false
                            wb?.isModified = false
                            onBack()
                        }) {
                            Text("Don't Save", color = Color(0xFFEF4444))
                        }
                        TextButton(onClick = { showUnsavedDialog = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }
    }
}
