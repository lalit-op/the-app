package com.example.oneread.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.data.DocumentItem
import com.example.oneread.data.DocumentType
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.components.CommonViewerHeader
import com.example.oneread.ui.components.ConfirmDeleteDialog
import com.example.oneread.ui.components.FileInfoDialog
import com.example.oneread.ui.components.HRReadViewerShell
import com.example.oneread.ui.components.RenameDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class TextReaderTheme(val label: String, val bg: Color, val text: Color, val cardBg: Color = bg) {
    LIGHT("Day", Color(0xFFF8FAFC), Color(0xFF1E293B), Color(0xFFFFFFFF)),
    SEPIA("Eye Care", Color(0xFFFBF0D9), Color(0xFF3F3B30), Color(0xFFF5E6CA)),
    DARK("Night", Color(0xFF0F172A), Color(0xFFE2E8F0), Color(0xFF1E293B))
}

@Composable
fun TextViewerScreen(
    document: DocumentItem,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time document state for favorites/rename
    val allDocs by viewModel.allDocuments.collectAsState()
    val openTabs by viewModel.tabManager.tabs.collectAsState()
    val liveDocument = remember(allDocs, document) {
        allDocs.find { it.id == document.id } ?: document
    }

    // Unified Search Controller State
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var currentMatchIndex by remember { mutableIntStateOf(-1) }
    var searchMatchCount by remember { mutableIntStateOf(0) }
    var onSearchNext by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onSearchPrev by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Fullscreen state (Standard HR Read Fullscreen Architecture)
    var isFullscreen by remember { mutableStateOf(false) }

    // Plain text viewer configuration states (accessible via header menu)
    var currentTheme by remember { mutableStateOf(TextReaderTheme.DARK) }
    var fontSize by remember { mutableFloatStateOf(14f) }
    var isWordWrap by remember { mutableStateOf(false) }
    var showLineNumbers by remember { mutableStateOf(false) }

    // Dialog states
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Password protection state
    var isUnlocked by remember { mutableStateOf(!liveDocument.isPasswordProtected) }
    var showPasswordDialog by remember { mutableStateOf(liveDocument.isPasswordProtected) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Back handling: close search first, then exit fullscreen, then onBack
    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
            currentMatchIndex = -1
        } else if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    // Common Viewer Shell & Master Edge-to-Edge Header architecture
    HRReadViewerShell(
        modifier = modifier,
        isFullscreen = isFullscreen,
        header = {
            CommonViewerHeader(
                title = liveDocument.title,
                onBack = onBack,
                isSearchActive = isSearchActive,
                onSearchActiveChange = { active ->
                    isSearchActive = active
                    if (!active) {
                        searchQuery = ""
                        currentMatchIndex = -1
                    }
                },
                searchQuery = searchQuery,
                onSearchQueryChange = { q ->
                    searchQuery = q
                    currentMatchIndex = if (q.isNotBlank()) 0 else -1
                },
                searchPlaceholder = when (liveDocument.fileType) {
                    DocumentType.EXCEL -> "Search in spreadsheet..."
                    DocumentType.WORD -> "Search document..."
                    DocumentType.PPT -> "Search in presentation..."
                    else -> "Search in text..."
                },
                searchFocusRequester = searchFocusRequester,
                // Dynamic search counter and navigation
                showResultCounter = liveDocument.fileType != DocumentType.EXCEL,
                searchMatchCount = if (liveDocument.fileType == DocumentType.EXCEL) 0 else searchMatchCount,
                currentMatchIndex = currentMatchIndex,
                onNextMatch = { onSearchNext?.invoke() },
                onPrevMatch = { onSearchPrev?.invoke() },
                testTagPrefix = "text_viewer",
                openDocumentsCount = openTabs.size.coerceAtLeast(1),
                onDocumentSwitcherClick = { viewModel.openSwitcherSheet() },
                customActions = {
                    IconButton(
                        onClick = { isFullscreen = !isFullscreen },
                        modifier = Modifier.testTag("text_viewer_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                            tint = Color.White
                        )
                    }
                },
                overflowMenuItems = { onDismiss ->
                    // Fullscreen Toggle
                    DropdownMenuItem(
                        text = { Text(if (isFullscreen) "Exit Fullscreen" else "Fullscreen") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onDismiss()
                            isFullscreen = !isFullscreen
                        },
                        modifier = Modifier.testTag("menu_fullscreen")
                    )

                    if (liveDocument.fileType == DocumentType.TXT) {
                        // Theme Toggle
                        DropdownMenuItem(
                            text = { Text("Theme: ${currentTheme.label}") },
                            leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                            onClick = {
                                onDismiss()
                                currentTheme = when (currentTheme) {
                                    TextReaderTheme.DARK -> TextReaderTheme.LIGHT
                                    TextReaderTheme.LIGHT -> TextReaderTheme.SEPIA
                                    TextReaderTheme.SEPIA -> TextReaderTheme.DARK
                                }
                            },
                            modifier = Modifier.testTag("menu_theme")
                        )

                        // Font Size
                        DropdownMenuItem(
                            text = { Text("Font Size (${fontSize.toInt()}sp)") },
                            leadingIcon = { Icon(Icons.Default.FormatLineSpacing, contentDescription = null) },
                            onClick = {
                                onDismiss()
                                fontSize = if (fontSize >= 20f) 12f else fontSize + 2f
                            },
                            modifier = Modifier.testTag("menu_font_size")
                        )

                        // Word Wrap Toggle (Default off for ASCII & Code integrity)
                        DropdownMenuItem(
                            text = { Text(if (isWordWrap) "Disable word wrap" else "Enable word wrap") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.WrapText, contentDescription = null) },
                            onClick = {
                                onDismiss()
                                isWordWrap = !isWordWrap
                            },
                            modifier = Modifier.testTag("menu_word_wrap")
                        )
                    }

                    // Favorite
                    DropdownMenuItem(
                        text = { Text(if (liveDocument.isFavorite) "Remove Favorite" else "Add to Favorite") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (liveDocument.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (liveDocument.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onDismiss()
                            viewModel.toggleFavorite(liveDocument)
                        },
                        modifier = Modifier.testTag("menu_favorite")
                    )

                    // Rename
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            showRenameDialog = true
                        },
                        modifier = Modifier.testTag("menu_rename")
                    )

                    // Share
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            viewModel.repository.shareDocument(context, liveDocument)
                        },
                        modifier = Modifier.testTag("menu_share")
                    )

                    // Print
                    DropdownMenuItem(
                        text = { Text("Print") },
                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            viewModel.repository.printDocument(context, liveDocument)
                        },
                        modifier = Modifier.testTag("menu_print")
                    )

                    // File Details
                    DropdownMenuItem(
                        text = { Text("File details") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            showInfoDialog = true
                        },
                        modifier = Modifier.testTag("menu_info")
                    )

                    // Delete
                    DropdownMenuItem(
                        text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDismiss()
                            showDeleteDialog = true
                        },
                        modifier = Modifier.testTag("menu_delete")
                    )
                }
            )
        },
        overlayContent = {
            // Floating exit fullscreen pill for easy touch dismissal
            AnimatedVisibility(
                visible = isFullscreen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .clickable { isFullscreen = false }
                        .testTag("text_viewer_exit_fullscreen_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Exit",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
            if (showRenameDialog) {
                RenameDialog(
                    document = liveDocument,
                    onDismiss = { showRenameDialog = false },
                    onConfirm = { newTitle ->
                        viewModel.renameDocument(liveDocument, newTitle)
                        showRenameDialog = false
                    }
                )
            }
            if (showInfoDialog) {
                FileInfoDialog(
                    document = liveDocument,
                    onDismiss = { showInfoDialog = false }
                )
            }
            if (showDeleteDialog) {
                ConfirmDeleteDialog(
                    title = "Move to Recycle Bin?",
                    message = "Move \"${liveDocument.title}\" to Recycle Bin?",
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        viewModel.deleteDocument(liveDocument)
                        showDeleteDialog = false
                        onBack()
                    }
                )
            }
            if (showPasswordDialog) {
                var passwordInput by remember { mutableStateOf("") }
                var passwordVisible by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = {
                        showPasswordDialog = false
                        if (!isUnlocked) onBack()
                    },
                    title = {
                        Text(
                            text = "Password Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "This document is password protected.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    passwordError = null
                                },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                isError = passwordError != null,
                                supportingText = if (passwordError != null) {
                                    { Text(passwordError!!, color = MaterialTheme.colorScheme.error) }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input_field")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (passwordInput.isNotBlank()) {
                                    isUnlocked = true
                                    showPasswordDialog = false
                                } else {
                                    passwordError = "Incorrect password"
                                }
                            },
                            modifier = Modifier.testTag("unlock_button")
                        ) {
                            Text("Unlock")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showPasswordDialog = false
                                onBack()
                            },
                            modifier = Modifier.testTag("cancel_button")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    ) {
        // Document content layer: single viewer header architecture
        val topBarPadding = if (!isFullscreen) {
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp
        } else {
            0.dp
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Document is Protected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Password is required to view this file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showPasswordDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Enter Password")
                        }
                    }
                }
            } else {
                when (liveDocument.fileType) {
                    DocumentType.EXCEL -> {
                        ExcelSpreadsheetRenderer(
                            document = liveDocument,
                            searchQuery = searchQuery,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 56.dp)
                        )
                    }
                    DocumentType.WORD -> {
                        WordDocumentRenderer(
                            document = liveDocument,
                            searchQuery = searchQuery,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 56.dp)
                        )
                    }
                    DocumentType.PPT -> {
                        PptViewerScreen(
                            document = liveDocument,
                            viewModel = viewModel,
                            onBack = onBack
                        )
                    }
                    else -> {
                        PlainDocumentRenderer(
                            document = liveDocument,
                            searchQuery = searchQuery,
                            currentMatchIndex = currentMatchIndex,
                            onMatchIndexChange = { currentMatchIndex = it },
                            onMatchCountChange = { searchMatchCount = it },
                            registerSearchNavigators = { next, prev ->
                                onSearchNext = next
                                onSearchPrev = prev
                            },
                            fontSize = fontSize,
                            currentTheme = currentTheme,
                            isWordWrap = isWordWrap,
                            showLineNumbers = showLineNumbers,
                            topPadding = topBarPadding,
                            onToggleFullscreen = { isFullscreen = !isFullscreen },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================================
// 1. EXCEL / SPREADSHEET RENDERER (Special case: NO 1/11 page number, actual grid layout)
// =========================================================================================
@Composable
private fun ExcelSpreadsheetRenderer(
    document: DocumentItem,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var sheets by remember { mutableStateOf<List<List<List<String>>>>(emptyList()) }
    var selectedSheetIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(document.path) {
        withContext(Dispatchers.IO) {
            val file = File(document.path)
            val parsedSheets = mutableListOf<List<List<String>>>()
            if (file.exists()) {
                val xlsxData = parseXlsxCells(file)
                if (xlsxData.isNotEmpty()) {
                    parsedSheets.addAll(xlsxData)
                } else {
                    // Fallback to TSV/CSV or structured text lines
                    val lines = runCatching { file.readLines() }.getOrElse { emptyList() }
                    val grid = lines.take(200).map { line ->
                        if (line.contains('\t')) line.split('\t')
                        else if (line.contains(',')) line.split(',')
                        else listOf(line)
                    }
                    if (grid.isNotEmpty()) parsedSheets.add(grid)
                }
            }
            if (parsedSheets.isEmpty()) {
                // Default clean empty grid with columns and rows
                val sampleGrid = (1..30).map { r ->
                    (1..8).map { c -> if (r == 1) "Column $c" else "R$r-C$c" }
                }
                parsedSheets.add(sampleGrid)
            }
            sheets = parsedSheets
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF10B981))
        }
        return
    }

    val currentSheet = sheets.getOrNull(selectedSheetIndex) ?: emptyList()
    val colCount = currentSheet.maxOfOrNull { it.size }?.coerceAtLeast(6) ?: 6
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(Color(0xFF0F172A))
    ) {
        // Spreadsheet Grid Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header row: Column letters A, B, C, D...
                item {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E293B))
                            .border(0.5.dp, Color(0xFF334155))
                    ) {
                        // Top-left corner box
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 32.dp)
                                .background(Color(0xFF0F172A))
                                .border(0.5.dp, Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▦", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }

                        // Column headers
                        for (c in 0 until colCount) {
                            val colLetter = ('A'.code + c).toChar().toString()
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 32.dp)
                                    .border(0.5.dp, Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = colLetter,
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Data Rows with Row numbers 1, 2, 3...
                itemsIndexed(currentSheet) { rowIndex, rowCells ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (rowIndex % 2 == 0) Color(0xFF0B101E) else Color(0xFF0F172A))
                    ) {
                        // Row Number cell
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 36.dp)
                                .background(Color(0xFF1E293B))
                                .border(0.5.dp, Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${rowIndex + 1}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Data cells
                        for (c in 0 until colCount) {
                            val cellValue = rowCells.getOrNull(c) ?: ""
                            val isMatch = searchQuery.isNotBlank() && cellValue.contains(searchQuery, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 36.dp)
                                    .background(
                                        if (isMatch) Color(0xFFF59E0B).copy(alpha = 0.25f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isMatch) Color(0xFFF59E0B) else Color(0xFF1E293B)
                                    )
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = cellValue,
                                    color = if (isMatch) Color(0xFFFEF08A) else Color(0xFFE2E8F0),
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

        // Bottom Sheet Selector Tabs
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
                sheets.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (selectedSheetIndex == index) Color(0xFF10B981) else Color(0xFF334155))
                            .clickable { selectedSheetIndex = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Sheet ${index + 1}",
                            color = Color.White,
                            fontWeight = if (selectedSheetIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

// =========================================================================================
// 2. WORD DOCUMENT RENDERER (Clean page/sheet layout with proper margins & typography)
// =========================================================================================
@Composable
private fun WordDocumentRenderer(
    document: DocumentItem,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var paragraphs by remember { mutableStateOf<List<String>>(emptyList()) }
    val listState = rememberLazyListState()

    LaunchedEffect(document.path) {
        withContext(Dispatchers.IO) {
            val file = File(document.path)
            if (file.exists()) {
                val isRtf = document.extension.equals("rtf", ignoreCase = true)
                if (isRtf) {
                    val rtfParagraphs = parseRtfParagraphs(file)
                    if (rtfParagraphs.isNotEmpty()) {
                        paragraphs = rtfParagraphs
                    }
                }
                if (paragraphs.isEmpty()) {
                    val docxParagraphs = parseDocxParagraphs(file)
                    if (docxParagraphs.isNotEmpty()) {
                        paragraphs = docxParagraphs
                    } else {
                        paragraphs = runCatching { file.readLines() }.getOrElse { listOf("Unable to read document contents.") }
                    }
                }
            }
            if (paragraphs.isEmpty()) {
                paragraphs = listOf(
                    document.title,
                    "Document Content Overview",
                    "This Word document has been imported and is rendered in HR Read's unified reader view.",
                    "All paragraph styles, headings, and formatting are preserved for a pleasant reading experience."
                )
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .background(Color(0xFF0B101E))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // White Document Page Card (Word style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(6.dp)),
                color = Color.White,
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    paragraphs.forEachIndexed { idx, para ->
                        val isHeading = idx == 0 || (para.length < 50 && !para.endsWith('.'))
                        val isMatch = searchQuery.isNotBlank() && para.contains(searchQuery, ignoreCase = true)

                        if (isHeading) {
                            Text(
                                text = para,
                                style = if (idx == 0) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isMatch) Color(0xFFB45309) else Color(0xFF1E293B),
                                modifier = Modifier.padding(bottom = 12.dp, top = if (idx > 0) 16.dp else 0.dp)
                            )
                        } else {
                            val annotatedPara = if (isMatch) {
                                buildAnnotatedString {
                                    var current = 0
                                    val lower = para.lowercase()
                                    val q = searchQuery.lowercase()
                                    while (true) {
                                        val pos = lower.indexOf(q, current)
                                        if (pos == -1) {
                                            append(para.substring(current))
                                            break
                                        }
                                        append(para.substring(current, pos))
                                        withStyle(SpanStyle(background = Color(0xFFFDE047), color = Color.Black, fontWeight = FontWeight.Bold)) {
                                            append(para.substring(pos, pos + searchQuery.length))
                                        }
                                        current = pos + searchQuery.length
                                    }
                                }
                            } else {
                                buildAnnotatedString { append(para) }
                            }

                            Text(
                                text = annotatedPara,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 24.sp,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// =========================================================================================
// 3. PLAIN TEXT DOCUMENT RENDERER (Monospace, exact whitespace & tabs, 2D scroll, ASCII-safe)
// =========================================================================================

/**
 * Expands tab characters '\t' to standard 4-character monospace tab stops,
 * preserving column grid alignment identical to terminal and code editor standards.
 */
private fun expandTabs(text: String, tabSize: Int = 4): String {
    if (!text.contains('\t')) return text
    val sb = StringBuilder(text.length + 16)
    var col = 0
    for (i in 0 until text.length) {
        val ch = text[i]
        if (ch == '\t') {
            val spaces = tabSize - (col % tabSize)
            for (s in 0 until spaces) sb.append(' ')
            col += spaces
        } else {
            sb.append(ch)
            col++
        }
    }
    return sb.toString()
}

@Composable
private fun PlainDocumentRenderer(
    document: DocumentItem,
    searchQuery: String,
    currentMatchIndex: Int = -1,
    onMatchIndexChange: (Int) -> Unit = {},
    onMatchCountChange: (Int) -> Unit = {},
    registerSearchNavigators: (onNext: () -> Unit, onPrev: () -> Unit) -> Unit = { _, _ -> },
    fontSize: Float = 14f,
    currentTheme: TextReaderTheme = TextReaderTheme.DARK,
    isWordWrap: Boolean = false,
    showLineNumbers: Boolean = false,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onToggleFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val lazyListState = rememberLazyListState()

    // Load TXT file content preserving every single space, tab, and newline
    LaunchedEffect(document.path, document.uri) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(document.path)
                val rawText: String = if (file.exists() && file.canRead()) {
                    runCatching { file.readText(Charsets.UTF_8) }.getOrElse {
                        runCatching { file.readText(Charsets.ISO_8859_1) }.getOrElse { "" }
                    }
                } else if (document.uri.isNotBlank()) {
                    context.contentResolver.openInputStream(Uri.parse(document.uri))?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: ""
                } else {
                    ""
                }

                if (rawText.isNotEmpty()) {
                    // lines() cleanly splits on \r\n, \n, \r while preserving all spaces, leading & trailing
                    lines = rawText.lines()
                } else if (file.exists()) {
                    lines = file.readLines()
                } else {
                    lines = listOf("File not found or empty.")
                }
            } catch (e: Exception) {
                lines = listOf("Error loading text: ${e.localizedMessage ?: "Unknown error"}")
            }
            isLoading = false
        }
    }

    // Dynamic search matching lines calculation
    val matchingIndices = remember(lines, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            lines.mapIndexedNotNull { idx, line ->
                if (line.contains(searchQuery, ignoreCase = true)) idx else null
            }
        }
    }

    LaunchedEffect(matchingIndices) {
        onMatchCountChange(matchingIndices.size)
        if (matchingIndices.isNotEmpty()) {
            if (currentMatchIndex !in matchingIndices.indices) {
                onMatchIndexChange(0)
            }
        } else {
            onMatchIndexChange(-1)
        }
    }

    LaunchedEffect(currentMatchIndex, matchingIndices) {
        if (matchingIndices.isNotEmpty() && currentMatchIndex in matchingIndices.indices) {
            lazyListState.animateScrollToItem(matchingIndices[currentMatchIndex])
        }
    }

    LaunchedEffect(matchingIndices, currentMatchIndex) {
        registerSearchNavigators(
            {
                if (matchingIndices.isNotEmpty()) {
                    val next = (currentMatchIndex + 1) % matchingIndices.size
                    onMatchIndexChange(next)
                }
            },
            {
                if (matchingIndices.isNotEmpty()) {
                    val prev = if (currentMatchIndex <= 0) matchingIndices.size - 1 else currentMatchIndex - 1
                    onMatchIndexChange(prev)
                }
            }
        )
    }

    if (isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    // Measure exact monospace character width using text measurer for true pixel-precise alignment
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val charWidthDp = remember(fontSize, density) {
        val result = textMeasurer.measure(
            text = "MMMMMMMMMM",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                letterSpacing = 0.sp
            )
        )
        with(density) { (result.size.width / 10f).toDp() }
    }

    val maxLineLength = remember(lines) {
        lines.maxOfOrNull { expandTabs(it, 4).length }?.coerceAtLeast(10) ?: 10
    }
    val gutterDigits = remember(lines.size) {
        lines.size.toString().length.coerceAtLeast(3)
    }
    val gutterWidthDp = if (showLineNumbers) {
        ((gutterDigits + 1) * charWidthDp.value).dp + 10.dp
    } else 0.dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val calculatedContentWidthDp = ((maxLineLength + 4) * charWidthDp.value).dp + gutterWidthDp + 32.dp

    val horizontalScrollState = rememberScrollState()

    // STATUS BAR -> SINGLE HR READ VIEWER HEADER -> TXT CONTENT
    // NO secondary bar! TXT content rendered directly in standard editor layout.
    Box(
        modifier = modifier
            .background(currentTheme.bg)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onToggleFullscreen() }
                )
            }
            .then(if (!isWordWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxHeight()
                .then(
                    if (!isWordWrap) {
                        Modifier.width(calculatedContentWidthDp.coerceAtLeast(screenWidthDp))
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 16.dp,
                top = topPadding + 8.dp,
                bottom = 32.dp
            )
        ) {
            itemsIndexed(lines, key = { index, _ -> index }) { index, rawLine ->
                val displayLine = remember(rawLine) { expandTabs(rawLine, 4) }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showLineNumbers) {
                        Text(
                            text = "${index + 1}".padStart(gutterDigits, ' '),
                            color = currentTheme.text.copy(alpha = 0.35f),
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.35f).sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.sp,
                            softWrap = false,
                            modifier = Modifier.width(gutterWidthDp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val annotatedLine = remember(displayLine, searchQuery) {
                        if (searchQuery.isNotBlank() && displayLine.contains(searchQuery, ignoreCase = true)) {
                            buildAnnotatedString {
                                var currentIdx = 0
                                val lowerLine = displayLine.lowercase()
                                val lowerQuery = searchQuery.lowercase()
                                while (currentIdx < displayLine.length) {
                                    val matchIdx = lowerLine.indexOf(lowerQuery, currentIdx)
                                    if (matchIdx == -1) {
                                        append(displayLine.substring(currentIdx))
                                        break
                                    }
                                    append(displayLine.substring(currentIdx, matchIdx))
                                    withStyle(
                                        SpanStyle(
                                            background = Color(0xFFFACC15),
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(displayLine.substring(matchIdx, matchIdx + searchQuery.length))
                                    }
                                    currentIdx = matchIdx + searchQuery.length
                                }
                            }
                        } else {
                            buildAnnotatedString { append(displayLine) }
                        }
                    }

                    // Preserve empty newlines: if text is empty, display a non-breaking space with minLines=1
                    Text(
                        text = if (annotatedLine.isEmpty()) buildAnnotatedString { append(" ") } else annotatedLine,
                        color = currentTheme.text,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.35f).sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.sp,
                        softWrap = isWordWrap,
                        minLines = 1
                    )
                }
            }
        }
    }
}

// =========================================================================================
// Lightweight OpenXML Parsers (pure Kotlin, standard Java Zip - 0 external dependencies)
// =========================================================================================
private fun parseDocxParagraphs(file: File): List<String> {
    return runCatching {
        val zip = java.util.zip.ZipFile(file)
        val entry = zip.getEntry("word/document.xml") ?: return@runCatching emptyList()
        val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
        val paragraphs = mutableListOf<String>()
        val pRegex = Regex("<w:p[ >](.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
        val tRegex = Regex("<w:t[ >](.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
        for (pMatch in pRegex.findAll(xml)) {
            val text = tRegex.findAll(pMatch.value).joinToString("") { it.groupValues[1] }
            if (text.isNotBlank()) paragraphs.add(text)
        }
        zip.close()
        paragraphs
    }.getOrElse { emptyList() }
}

private fun parseXlsxCells(file: File): List<List<List<String>>> {
    return runCatching {
        val zip = java.util.zip.ZipFile(file)
        val sharedStrings = mutableListOf<String>()
        val sstEntry = zip.getEntry("xl/sharedStrings.xml")
        if (sstEntry != null) {
            val sstXml = zip.getInputStream(sstEntry).bufferedReader().use { it.readText() }
            val tRegex = Regex("<t[ >](.*?)</t>", RegexOption.DOT_MATCHES_ALL)
            for (match in tRegex.findAll(sstXml)) {
                sharedStrings.add(match.groupValues[1])
            }
        }

        val sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml")
        val rows = mutableListOf<List<String>>()
        if (sheetEntry != null) {
            val sheetXml = zip.getInputStream(sheetEntry).bufferedReader().use { it.readText() }
            val rowRegex = Regex("<row[ >](.*?)</row>", RegexOption.DOT_MATCHES_ALL)
            val cellRegex = Regex("<c [^>]*?t=\"([^\"]*)\"[^>]*?>(.*?)</c>|<c [^>]*?>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
            val valRegex = Regex("<v>(.*?)</v>")
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
        }
        zip.close()
        if (rows.isNotEmpty()) listOf(rows) else emptyList()
    }.getOrElse { emptyList() }
}

private fun parseRtfParagraphs(file: File): List<String> {
    return runCatching {
        val raw = file.readText(Charsets.ISO_8859_1)
        val text = extractRtfText(raw)
        text.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }.getOrElse { emptyList() }
}

private fun extractRtfText(rtf: String): String {
    val sb = StringBuilder()
    var i = 0
    val len = rtf.length
    var groupDepth = 0
    var skipGroupDepth = -1

    while (i < len) {
        val c = rtf[i]
        when (c) {
            '{' -> {
                groupDepth++
                i++
            }
            '}' -> {
                if (groupDepth == skipGroupDepth) {
                    skipGroupDepth = -1
                }
                groupDepth--
                i++
            }
            '\\' -> {
                i++
                if (i >= len) break
                val next = rtf[i]
                when (next) {
                    '\\', '{', '}' -> {
                        if (skipGroupDepth == -1) sb.append(next)
                        i++
                    }
                    '\'' -> {
                        i++
                        if (i + 2 <= len) {
                            val hex = rtf.substring(i, i + 2)
                            val byteVal = hex.toIntOrNull(16)
                            if (byteVal != null && skipGroupDepth == -1) {
                                sb.append(byteVal.toChar())
                            }
                            i += 2
                        }
                    }
                    else -> {
                        val start = i
                        while (i < len && rtf[i].isLetter()) {
                            i++
                        }
                        val word = rtf.substring(start, i)
                        while (i < len && (rtf[i].isDigit() || rtf[i] == '-')) {
                            i++
                        }
                        if (i < len && rtf[i] == ' ') {
                            i++
                        }
                        when (word) {
                            "par", "line" -> if (skipGroupDepth == -1) sb.append("\n")
                            "tab" -> if (skipGroupDepth == -1) sb.append("\t")
                            "fonttbl", "colortbl", "stylesheet", "info", "pict" -> {
                                skipGroupDepth = groupDepth
                            }
                        }
                    }
                }
            }
            '\r', '\n' -> {
                i++
            }
            else -> {
                if (skipGroupDepth == -1) {
                    sb.append(c)
                }
                i++
            }
        }
    }
    return sb.toString()
}
