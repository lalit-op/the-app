package com.example.oneread.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.data.DocumentItem
import com.example.oneread.data.DocumentType
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.components.ConfirmDeleteDialog
import com.example.oneread.ui.components.DocumentCard
import com.example.oneread.ui.components.FileInfoDialog
import com.example.oneread.ui.components.FilterChipMiniIcon
import com.example.oneread.ui.components.RenameDialog
import com.example.oneread.ui.navigation.SortOption

@Composable
fun FilesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val filteredDocs by viewModel.filteredDocuments.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var docToRename by remember { mutableStateOf<DocumentItem?>(null) }
    var docForInfo by remember { mutableStateOf<DocumentItem?>(null) }
    var docToDelete by remember { mutableStateOf<DocumentItem?>(null) }

    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }

    val handleBack: () -> Unit = {
        if (isSearchActive) {
            isSearchActive = false
            viewModel.setSearchQuery("")
            focusManager.clearFocus()
        } else {
            onBack?.invoke() ?: viewModel.navigateBack()
        }
    }

    BackHandler {
        handleBack()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFile(it) }
    }

    Scaffold(
        containerColor = Color(0xFF050811),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF050811))
                    .statusBarsPadding()
            ) {
                // Compact Top Header with animated transition
                AnimatedContent(
                    targetState = isSearchActive,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(180))
                    },
                    label = "search_header_anim"
                ) { searchActive ->
                    if (!searchActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Arrow on extreme left
                            IconButton(
                                onClick = handleBack,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFF8FAFC)
                                )
                            }

                            // Title
                            Text(
                                text = "Document Explorer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            // Search Icon on right
                            IconButton(
                                onClick = {
                                    isSearchActive = true
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFFF8FAFC)
                                )
                            }

                            // Three-dot Overflow Menu on extreme right
                            Box {
                                IconButton(
                                    onClick = { overflowMenuExpanded = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("header_overflow_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color(0xFFF8FAFC)
                                    )
                                }

                                DropdownMenu(
                                    expanded = overflowMenuExpanded,
                                    onDismissRequest = { overflowMenuExpanded = false },
                                    modifier = Modifier.background(Color(0xFF0F172A))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rescan Documents", color = Color(0xFFF1F5F9)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFF60A5FA)
                                            )
                                        },
                                        onClick = {
                                            overflowMenuExpanded = false
                                            viewModel.refreshDocuments(showStatus = true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Import from Storage", color = Color(0xFFF1F5F9)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                tint = Color(0xFF60A5FA)
                                            )
                                        },
                                        onClick = {
                                            overflowMenuExpanded = false
                                            filePickerLauncher.launch(arrayOf("*/*"))
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // In-Place Search Bar
                        LaunchedEffect(Unit) {
                            searchFocusRequester.requestFocus()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close search",
                                    tint = Color(0xFFF8FAFC)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color(0xFF1E3A5F))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search documents...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.setSearchQuery(it) },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = Color(0xFFF8FAFC),
                                                fontSize = 15.sp
                                            ),
                                            cursorBrush = SolidColor(Color(0xFF3B82F6)),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(searchFocusRequester)
                                                .testTag("search_text_input")
                                        )
                                    }

                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.setSearchQuery("") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }

                // File Type Filter Chips (Horizontally Scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentType.entries.forEach { type ->
                        val isSelected = selectedCategory == type
                        val chipBgColor by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF2563EB) else Color(0x800E1726),
                            animationSpec = tween(200),
                            label = "chip_bg"
                        )
                        val chipBorderColor by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E3A5F),
                            animationSpec = tween(200),
                            label = "chip_border"
                        )
                        val chipScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.04f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "chip_scale"
                        )
                        Surface(
                            modifier = Modifier
                                .scale(chipScale)
                                .clickable { viewModel.setCategory(type) }
                                .testTag("filter_chip_${type.name}"),
                            shape = RoundedCornerShape(16.dp),
                            color = chipBgColor,
                            border = BorderStroke(1.dp, chipBorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChipMiniIcon(fileType = type, isSelected = isSelected)
                                Text(
                                    text = type.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }

                // Document Count + Sort Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Document Count
                    val count = filteredDocs.size
                    val countText = "$count ${if (count == 1) "document" else "documents"} found"
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )

                    // Sort Control [ ↕ Date (Newest) ▼ ]
                    Box {
                        Surface(
                            modifier = Modifier
                                .clickable { sortMenuExpanded = true }
                                .testTag("sort_filter_button"),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF1E2D4A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = when (sortOption) {
                                        SortOption.DATE_DESC -> "Date (Newest)"
                                        SortOption.DATE_ASC -> "Date (Oldest)"
                                        SortOption.NAME_ASC -> "Name (A-Z)"
                                        SortOption.NAME_DESC -> "Name (Z-A)"
                                        SortOption.SIZE_DESC -> "Size (Largest)"
                                        SortOption.SIZE_ASC -> "Size (Smallest)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            color = if (sortOption == option) Color(0xFF60A5FA) else Color(0xFFF1F5F9),
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        if (sortOption == option) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val fabInteraction = remember { MutableInteractionSource() }
            val fabPressed by fabInteraction.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (fabPressed) 0.90f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "fab_scale"
            )
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                shape = CircleShape,
                interactionSource = fabInteraction,
                modifier = Modifier
                    .scale(fabScale)
                    .padding(bottom = 12.dp, end = 4.dp)
                    .testTag("fab_import_file")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Import file",
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        // Main Vertically Scrolling Document List
        if (filteredDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            "No matching documents"
                        } else if (selectedCategory == DocumentType.ALL) {
                            "No documents found"
                        } else {
                            "No ${selectedCategory.displayName} documents"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF8FAFC)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            "Try a different search query or clear the filter."
                        } else {
                            "Tap the + button to import documents from device storage."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDocs, key = { it.id }) { doc ->
                    DocumentCard(
                        document = doc,
                        modifier = Modifier.animateItem(),
                        onClick = { viewModel.openDocument(doc) },
                        onToggleFavorite = { viewModel.toggleFavorite(doc) },
                        onRename = { docToRename = doc },
                        onShare = { viewModel.repository.shareDocument(context, doc) },
                        onPrint = { viewModel.repository.printDocument(context, doc) },
                        onInfo = { docForInfo = doc },
                        onDelete = { docToDelete = doc }
                    )
                }
            }
        }
    }

    // Real Action Dialogs
    docToRename?.let { doc ->
        RenameDialog(
            document = doc,
            onDismiss = { docToRename = null },
            onConfirm = { newName ->
                viewModel.renameDocument(doc, newName)
                docToRename = null
            }
        )
    }

    docForInfo?.let { doc ->
        FileInfoDialog(
            document = doc,
            onDismiss = { docForInfo = null }
        )
    }

    docToDelete?.let { doc ->
        ConfirmDeleteDialog(
            title = "Move to Recycle Bin?",
            message = "Move '${doc.title}' to the recycle bin? You can restore it anytime.",
            confirmText = "Move to Trash",
            onDismiss = { docToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(doc)
                docToDelete = null
            }
        )
    }
}
