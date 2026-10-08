package com.example.oneread.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import com.example.oneread.ui.components.PermissionRequiredDialog
import com.example.oneread.ui.components.openAllFilesPermissionSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.R
import com.example.oneread.data.DocumentType
import com.example.oneread.ui.DirectoryInfo
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.navigation.BottomTab
import com.example.oneread.ui.navigation.Screen

enum class HomeCategoryType {
    ALL_FILES,
    PDF,
    WORD,
    EXCEL,
    POWERPOINT,
    TEXT,
    DIRECTORIES,
    RECENTS,
    RECYCLE_BIN
}

private data class HeroCardModel(
    val id: String,
    val type: HomeCategoryType,
    val title: String,
    val countText: String,
    val bgColors: List<Color>,
    val borderColor: Color,
    val accentColor: Color,
    val badgeColors: List<Color>,
    val countColor: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allDocs by viewModel.allDocuments.collectAsState()
    val openTabs by viewModel.tabManager.tabs.collectAsState()
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val recycleBinDocs by viewModel.recycleBin.collectAsState()
    val directories by viewModel.directories.collectAsState()
    val folderCount by viewModel.folderCount.collectAsState()

    val hasFileAccess by viewModel.hasFileAccess.collectAsState()
    val showPermissionRequired by viewModel.showPermissionRequired.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val scanErrorMessage by viewModel.scanErrorMessage.collectAsState()
    val dismissedResumeTabIds by viewModel.dismissedResumeTabIds.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(scanErrorMessage) {
        scanErrorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearScanErrorMessage()
        }
    }

    var wasScanning by remember { mutableStateOf(false) }
    LaunchedEffect(isScanning) {
        if (wasScanning && !isScanning) {
            Toast.makeText(context, "Documents refreshed", Toast.LENGTH_SHORT).show()
        }
        wasScanning = isScanning
    }

    var isGridView by rememberSaveable { mutableStateOf(true) }
    var showDirectoryBrowser by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFile(it) }
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.scanDirectoryTree(it) }
    }

    // Dynamic counts from real database documents - strictly 0 by default when access is missing
    val displayAllDocs = if (hasFileAccess) allDocs else emptyList()
    val displayRecentDocs = if (hasFileAccess) recentDocs else emptyList()
    val displayRecycleBin = if (hasFileAccess) recycleBinDocs else emptyList()
    val displayFolderCount = if (hasFileAccess) folderCount else 0

    val pdfCount = remember(displayAllDocs) { displayAllDocs.count { it.fileType == DocumentType.PDF } }
    val wordCount = remember(displayAllDocs) { displayAllDocs.count { it.fileType == DocumentType.WORD } }
    val excelCount = remember(displayAllDocs) { displayAllDocs.count { it.fileType == DocumentType.EXCEL } }
    val pptCount = remember(displayAllDocs) { displayAllDocs.count { it.fileType == DocumentType.PPT } }
    val txtCount = remember(displayAllDocs) { displayAllDocs.count { it.fileType == DocumentType.TXT } }

    val allFilesCard = HeroCardModel(
        id = "cat_all",
        type = HomeCategoryType.ALL_FILES,
        title = "All Files",
        countText = if (displayAllDocs.size == 1) "1 file" else "${displayAllDocs.size} files",
        bgColors = listOf(Color(0xFF381566), Color(0xFF190633)),
        borderColor = Color(0xFF8B5CF6),
        accentColor = Color(0xFF7C3AED),
        badgeColors = listOf(Color(0xFFA855F7), Color(0xFF6366F1)),
        countColor = Color(0xFFDDD6FE),
        onClick = {
            if (hasFileAccess) {
                viewModel.openCategory(DocumentType.ALL)
            } else {
                viewModel.showPermissionDialog()
            }
        }
    )

    val pdfCard = HeroCardModel(
        id = "cat_pdf",
        type = HomeCategoryType.PDF,
        title = "PDF",
        countText = if (pdfCount == 1) "1 file" else "$pdfCount files",
        bgColors = listOf(Color(0xFF5D0A18), Color(0xFF1F0307)),
        borderColor = Color(0xFFE52538),
        accentColor = Color(0xFFE52538),
        badgeColors = listOf(Color(0xFFFF334B), Color(0xFFD5001C)),
        countColor = Color(0xFFFCA5A5),
        onClick = {
            if (hasFileAccess) {
                viewModel.openCategory(DocumentType.PDF)
            } else {
                viewModel.showPermissionDialog()
            }
        }
    )

    val wordCard = HeroCardModel(
        id = "cat_word",
        type = HomeCategoryType.WORD,
        title = "Word",
        countText = if (wordCount == 1) "1 file" else "$wordCount files",
        bgColors = listOf(Color(0xFF0B2B66), Color(0xFF071638)),
        borderColor = Color(0xFF2563EB),
        accentColor = Color(0xFF2979FF),
        badgeColors = listOf(Color(0xFF2979FF), Color(0xFF1565C0)),
        countColor = Color(0xFF93C5FD),
        onClick = {
            if (hasFileAccess) {
                viewModel.openCategory(DocumentType.WORD)
            } else {
                viewModel.showPermissionDialog()
            }
        }
    )

    val excelCard = HeroCardModel(
        id = "cat_excel",
        type = HomeCategoryType.EXCEL,
        title = "Excel",
        countText = if (excelCount == 1) "1 file" else "$excelCount files",
        bgColors = listOf(Color(0xFF074522), Color(0xFF031C0D)),
        borderColor = Color(0xFF00C853),
        accentColor = Color(0xFF00C853),
        badgeColors = listOf(Color(0xFF00E676), Color(0xFF00A344)),
        countColor = Color(0xFF86EFAC),
        onClick = {
            if (hasFileAccess) {
                viewModel.openCategory(DocumentType.EXCEL)
            } else {
                viewModel.showPermissionDialog()
            }
        }
    )

    val pptCard = HeroCardModel(
        id = "cat_ppt",
        type = HomeCategoryType.POWERPOINT,
        title = "PowerPoint",
        countText = if (pptCount == 1) "1 file" else "$pptCount files",
        bgColors = listOf(Color(0xFF5E2407), Color(0xFF240B02)),
        borderColor = Color(0xFFFF6D00),
        accentColor = Color(0xFFFF6D00),
        badgeColors = listOf(Color(0xFFFF9100), Color(0xFFFF5722)),
        countColor = Color(0xFFFDBA74),
        onClick = {
            if (hasFileAccess) {
                viewModel.openCategory(DocumentType.PPT)
            } else {
                viewModel.showPermissionDialog()
            }
        }
    )

    val textCard = HeroCardModel(
        id = "cat_txt",
        type = HomeCategoryType.TEXT,
        title = "Text",
        countText = if (txtCount == 1) "1 file" else "$txtCount files",
        bgColors = listOf(Color(0xFF1D2636), Color(0xFF0E131C)),
        borderColor = Color(0xFF64748B),
        accentColor = Color(0xFF64748B),
        badgeColors = listOf(Color(0xFF94A3B8), Color(0xFF546E7A)),
        countColor = Color(0xFFCBD5E1),
        onClick = {
            if (hasFileAccess) {
                viewModel.openCategory(DocumentType.TXT)
            } else {
                viewModel.showPermissionDialog()
            }
        }
    )

    val directoriesCard = HeroCardModel(
        id = "cat_directories",
        type = HomeCategoryType.DIRECTORIES,
        title = "Directories",
        countText = if (displayFolderCount == 1) "1 folder" else "$displayFolderCount folders",
        bgColors = listOf(Color(0xFF593905), Color(0xFF221601)),
        borderColor = Color(0xFFFFAB00),
        accentColor = Color(0xFFFFAB00),
        badgeColors = listOf(Color(0xFFFFD54F), Color(0xFFFF9800)),
        countColor = Color(0xFFFDE68A),
        onClick = {
            if (hasFileAccess) {
                showDirectoryBrowser = true
            } else {
                viewModel.showPermissionDialog()
            }
        }
    )

    val recentsCard = HeroCardModel(
        id = "cat_recents",
        type = HomeCategoryType.RECENTS,
        title = "Recents",
        countText = if (displayRecentDocs.size == 1) "1 file" else "${displayRecentDocs.size} files",
        bgColors = listOf(Color(0xFF064455), Color(0xFF021C24)),
        borderColor = Color(0xFF00B4D8),
        accentColor = Color(0xFF00B4D8),
        badgeColors = listOf(Color(0xFF00E5FF), Color(0xFF0091EA)),
        countColor = Color(0xFFA5F3FC),
        onClick = {
            if (hasFileAccess) {
                viewModel.navigateTo(Screen.Recent)
            } else {
                viewModel.showPermissionDialog()
            }
        }
    )

    val recycleBinCard = HeroCardModel(
        id = "cat_recycle_bin",
        type = HomeCategoryType.RECYCLE_BIN,
        title = "Recycle Bin",
        countText = if (displayRecycleBin.size == 1) "1 file" else "${displayRecycleBin.size} files",
        bgColors = listOf(Color(0xFF560B31), Color(0xFF210212)),
        borderColor = Color(0xFFF72585),
        accentColor = Color(0xFFF72585),
        badgeColors = listOf(Color(0xFFFF4081), Color(0xFFC51162)),
        countColor = Color(0xFFFBCFE8),
        onClick = {
            if (hasFileAccess) {
                viewModel.navigateTo(Screen.RecycleBin)
            } else {
                viewModel.showPermissionDialog()
            }
        }
    )

    val allCards = listOf(
        allFilesCard, pdfCard, wordCard, excelCard, pptCard, textCard,
        directoriesCard, recentsCard, recycleBinCard
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050811))
            .drawBehind {
                val w = size.width
                val h = size.height
                // Atmospheric ambient blue lighting (Static, zero CPU/recomposition overhead)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0E387A).copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(0f, h * 0.94f),
                        radius = w * 0.85f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF082250).copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(w, h * 0.90f),
                        radius = w * 0.90f
                    )
                )
                // Flowing luminous wave line
                val wavePath = Path().apply {
                    moveTo(0f, h * 0.83f)
                    cubicTo(
                        w * 0.35f, h * 0.79f,
                        w * 0.65f, h * 0.89f,
                        w, h * 0.81f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0A2B68).copy(alpha = 0.22f), Color.Transparent),
                        startY = h * 0.80f,
                        endY = h
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header: [APP LOGO] HR Read / ALL IN ONE READER           ↻  ⊞  ⋮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Existing HR Read App Logo
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "HR Read",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(9.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "HR Read",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            letterSpacing = 0.3.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "ALL IN ONE READER",
                            color = Color(0xFF8899AC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 1.4.sp,
                            lineHeight = 11.sp
                        )
                    }
                }

                // Header Action Buttons: Refresh and ⋮ (ONLY visible when hasFileAccess == true and permission dialog is not active)
                if (hasFileAccess && !showPermissionRequired) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RefreshHeaderButton(
                            isRefreshing = isScanning,
                            onClick = { viewModel.refreshDocuments() }
                        )
                        Box {
                            HeaderCircleButton(
                                icon = Icons.Default.MoreVert,
                                contentDescription = "More",
                                onClick = { showMoreMenu = true },
                                testTag = "home_more_menu_button"
                            )

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Refresh") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.refreshDocuments()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isGridView) "List view" else "Grid view") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        isGridView = !isGridView
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Open document") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        filePickerLauncher.launch(arrayOf("*/*"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Browse directories") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showDirectoryBrowser = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.setTab(BottomTab.SETTINGS)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick-Resume indicator for open documents (WPS Office style outside reader)
            val activeTab = openTabs.firstOrNull { it.id == activeTabId } ?: openTabs.firstOrNull()
            val isResumeDismissed = activeTab == null || dismissedResumeTabIds.contains(activeTab.id)

            AnimatedVisibility(
                visible = activeTab != null && !isResumeDismissed,
                enter = fadeIn(tween(220)) + slideInVertically(tween(220), initialOffsetY = { -it / 3 }),
                exit = fadeOut(tween(220)) + slideOutVertically(tween(220), targetOffsetY = { -it / 3 })
            ) {
                if (activeTab != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clickable { viewModel.switchToTab(activeTab) }
                            .testTag("home_quick_resume_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.8f))
                            ) {
                                Text(
                                    text = "${openTabs.size}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Resume: ${activeTab.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${activeTab.displayExtension} • ${activeTab.locationText} (${openTabs.size} open)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Button(
                                onClick = { viewModel.switchToTab(activeTab) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("resume_document_button")
                            ) {
                                Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Close/Dismiss Button [ × ]
                            IconButton(
                                onClick = { viewModel.dismissResumeCard(activeTab.id) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("dismiss_resume_card_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss resume suggestion",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Inline permission banner if user dismissed the modal overlay without granting permission
            if (!hasFileAccess) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0D1B36),
                    border = BorderStroke(1.dp, Color(0xFF1E356A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clickable { viewModel.showPermissionDialog() }
                        .testTag("home_permission_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF1D4ED8), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Tap to grant access and view your documents",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.showPermissionDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Cards Content with smooth morph transition between Grid and List views
            AnimatedContent(
                targetState = isGridView,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith
                    (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 1.02f))
                },
                label = "home_view_mode_transition"
            ) { gridMode ->
                if (gridMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        // Row 1: All Files (Blue) | PDF (Red)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroGridCard(card = allFilesCard, modifier = Modifier.weight(1f))
                            HeroGridCard(card = pdfCard, modifier = Modifier.weight(1f))
                        }

                        // Row 2: Word (Blue) | Excel (Green)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroGridCard(card = wordCard, modifier = Modifier.weight(1f))
                            HeroGridCard(card = excelCard, modifier = Modifier.weight(1f))
                        }

                        // Row 3: PowerPoint (Orange) | Text (Slate)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroGridCard(card = pptCard, modifier = Modifier.weight(1f))
                            HeroGridCard(card = textCard, modifier = Modifier.weight(1f))
                        }

                        // Row 4: Directories (Amber) | Recents (Cyan) | Recycle Bin (Magenta Pink)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BottomGridCard(card = directoriesCard, modifier = Modifier.weight(1f))
                            BottomGridCard(card = recentsCard, modifier = Modifier.weight(1f))
                            BottomGridCard(card = recycleBinCard, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    // List view alternative
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(allCards, key = { it.id }) { card ->
                            HeroListCard(card = card)
                        }
                    }
                }
            }
        }

        // Permission Required Modal Dialog Overlay matching reference design
        PermissionRequiredDialog(
            visible = showPermissionRequired && !hasFileAccess,
            onClose = { viewModel.dismissPermissionDialog() },
            onAllow = { openAllFilesPermissionSettings(context) }
        )
    }

    // Directory Browser Dialog
    if (showDirectoryBrowser) {
        DirectoryBrowserDialog(
            directories = directories,
            onDismiss = { showDirectoryBrowser = false },
            onSelectFolder = { dirName ->
                showDirectoryBrowser = false
                viewModel.setSearchQuery(dirName)
                viewModel.navigateTo(Screen.Files(DocumentType.ALL))
            },
            onBrowseDeviceTree = {
                showDirectoryBrowser = false
                directoryPickerLauncher.launch(null)
            }
        )
    }
}

@Composable
private fun RefreshHeaderButton(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by rememberInfiniteTransition(label = "refresh_rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_spin"
    )

    HeaderCircleButton(
        icon = Icons.Default.Refresh,
        contentDescription = if (isRefreshing) "Refreshing documents" else "Refresh documents",
        onClick = {
            if (!isRefreshing) {
                onClick()
            }
        },
        modifier = modifier,
        iconModifier = if (isRefreshing) Modifier.rotate(rotation) else Modifier,
        testTag = "home_refresh_button"
    )
}

@Composable
private fun HeaderCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "header_btn_scale"
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .scale(btnScale)
            .clip(CircleShape)
            .background(Color(0xFF131D33).copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, Color(0xFF233252)), shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 20.dp),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = iconModifier.size(18.dp)
        )
    }
}

/**
 * 2-Column Hero Grid Card (Row 1, 2, 3)
 */
@Composable
private fun HeroGridCard(
    card: HeroCardModel,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.955f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hero_grid_card_scale"
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (isPressed) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hero_badge_scale"
    )
    val arrowOffset by animateFloatAsState(
        targetValue = if (isPressed) 3.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hero_arrow_offset"
    )

    Box(
        modifier = modifier
            .height(134.dp)
            .scale(cardScale)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(card.bgColors))
            .border(
                BorderStroke(1.4.dp, card.borderColor.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = card.onClick
            )
            .testTag(card.id)
    ) {
        // Watermark decorative graphic on the right
        CardWatermark(
            type = card.type,
            accentColor = card.borderColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = 90.dp, height = 110.dp)
                .padding(end = 4.dp)
        )

        // Main foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Badge Icon with interactive pop
            CardBadge(
                type = card.type,
                badgeColors = card.badgeColors,
                size = 42.dp,
                modifier = Modifier.scale(badgeScale)
            )

            // Bottom Info: Title, Count, Action Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = card.countText,
                        color = card.countColor,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                // Circular action button with white right arrow and interactive nudge
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer { translationX = arrowOffset }
                        .clip(CircleShape)
                        .background(card.accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open ${card.title}",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3-Column Bottom Grid Card (Directories, Recents, Recycle Bin)
 */
@Composable
private fun BottomGridCard(
    card: HeroCardModel,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bottom_grid_card_scale"
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (isPressed) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bottom_badge_scale"
    )
    val arrowOffset by animateFloatAsState(
        targetValue = if (isPressed) 2.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bottom_arrow_offset"
    )

    Box(
        modifier = modifier
            .height(118.dp)
            .scale(cardScale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(card.bgColors))
            .border(
                BorderStroke(1.4.dp, card.borderColor.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = card.onClick
            )
            .testTag(card.id)
    ) {
        // Watermark decorative graphic
        CardWatermark(
            type = card.type,
            accentColor = card.borderColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = 65.dp, height = 85.dp)
        )

        // Main foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Badge Icon with interactive pop
            CardBadge(
                type = card.type,
                badgeColors = card.badgeColors,
                size = 34.dp,
                modifier = Modifier.scale(badgeScale)
            )

            // Bottom Info: Title, Count, Action Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = card.countText,
                        color = card.countColor,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.5.sp,
                        maxLines = 1
                    )
                }

                // Small circular action button with interactive nudge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { translationX = arrowOffset }
                        .clip(CircleShape)
                        .background(card.accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open ${card.title}",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * List Card View alternative
 */
@Composable
private fun HeroListCard(
    card: HeroCardModel,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hero_list_card_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(card.bgColors))
            .border(
                BorderStroke(1.2.dp, card.borderColor.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = card.onClick
            )
            .testTag(card.id)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardBadge(
                type = card.type,
                badgeColors = card.badgeColors,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = card.countText,
                    color = card.countColor,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(card.accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Vector Badge Icon at the top-left of each card
 */
@Composable
private fun CardBadge(
    type: HomeCategoryType,
    badgeColors: List<Color>,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape((size.value * 0.25f).dp))
            .background(Brush.linearGradient(badgeColors)),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            HomeCategoryType.ALL_FILES -> {
                // Folder outline with document slot
                Canvas(modifier = Modifier.size((size.value * 0.58f).dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    // Folder body
                    val folderPath = Path().apply {
                        moveTo(0f, h * 0.28f)
                        lineTo(w * 0.40f, h * 0.28f)
                        lineTo(w * 0.52f, h * 0.42f)
                        lineTo(w, h * 0.42f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = folderPath,
                        color = Color.White,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Inner document slot line
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.22f, h * 0.62f),
                        size = Size(w * 0.42f, h * 0.18f),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }

            HomeCategoryType.PDF -> {
                // PDF Document with ribbon curve
                Canvas(modifier = Modifier.size((size.value * 0.58f).dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    // Document outline with folded corner
                    val docPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w * 0.65f, 0f)
                        lineTo(w, h * 0.35f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = docPath,
                        color = Color.White.copy(alpha = 0.25f)
                    )
                    // Signature / Ribbon curve for PDF
                    val ribbon = Path().apply {
                        moveTo(w * 0.5f, h * 0.30f)
                        cubicTo(w * 0.3f, h * 0.45f, w * 0.2f, h * 0.65f, w * 0.35f, h * 0.78f)
                        cubicTo(w * 0.5f, h * 0.90f, w * 0.7f, h * 0.75f, w * 0.55f, h * 0.60f)
                        cubicTo(w * 0.4f, h * 0.45f, w * 0.65f, h * 0.35f, w * 0.5f, h * 0.30f)
                    }
                    drawPath(
                        path = ribbon,
                        color = Color.White,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            HomeCategoryType.WORD -> {
                Text(
                    text = "W",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.50f).sp
                )
            }

            HomeCategoryType.EXCEL -> {
                Text(
                    text = "X",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.50f).sp
                )
            }

            HomeCategoryType.POWERPOINT -> {
                Text(
                    text = "P",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.50f).sp
                )
            }

            HomeCategoryType.TEXT -> {
                Text(
                    text = "T",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.50f).sp
                )
            }

            HomeCategoryType.DIRECTORIES -> {
                // Folder icon
                Canvas(modifier = Modifier.size((size.value * 0.58f).dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    val folder = Path().apply {
                        moveTo(0f, h * 0.20f)
                        lineTo(w * 0.38f, h * 0.20f)
                        lineTo(w * 0.50f, h * 0.36f)
                        lineTo(w, h * 0.36f)
                        lineTo(w, h * 0.95f)
                        lineTo(0f, h * 0.95f)
                        close()
                    }
                    drawPath(path = folder, color = Color.White)
                    // Folder front flap highlight
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = Offset(0f, h * 0.40f),
                        size = Size(w, h * 0.55f),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }

            HomeCategoryType.RECENTS -> {
                // Clock icon
                Canvas(modifier = Modifier.size((size.value * 0.60f).dp)) {
                    val r = this.size.width / 2f
                    drawCircle(
                        color = Color.White,
                        radius = r * 0.9f,
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                    // Hour and minute hands
                    drawLine(
                        color = Color.White,
                        start = Offset(r, r),
                        end = Offset(r, r * 0.40f),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(r, r),
                        end = Offset(r * 1.45f, r),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            HomeCategoryType.RECYCLE_BIN -> {
                // Trash Can icon with vertical ribs
                Canvas(modifier = Modifier.size((size.value * 0.58f).dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    // Lid
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.15f, h * 0.10f),
                        size = Size(w * 0.70f, h * 0.14f),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                    // Body
                    val body = Path().apply {
                        moveTo(w * 0.22f, h * 0.28f)
                        lineTo(w * 0.78f, h * 0.28f)
                        lineTo(w * 0.72f, h * 0.95f)
                        lineTo(w * 0.28f, h * 0.95f)
                        close()
                    }
                    drawPath(path = body, color = Color.White)
                    // 2 Vertical Slots / Ribs
                    drawRoundRect(
                        color = badgeColors.first(),
                        topLeft = Offset(w * 0.40f, h * 0.42f),
                        size = Size(w * 0.08f, h * 0.38f),
                        cornerRadius = CornerRadius(1.5.dp.toPx())
                    )
                    drawRoundRect(
                        color = badgeColors.first(),
                        topLeft = Offset(w * 0.52f, h * 0.42f),
                        size = Size(w * 0.08f, h * 0.38f),
                        cornerRadius = CornerRadius(1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Translucent Background Watermark Illustration on the right of each card
 */
@Composable
private fun CardWatermark(
    type: HomeCategoryType,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (type) {
            HomeCategoryType.ALL_FILES -> {
                // Stack of tilted translucent folder outlines
                for (i in 0..1) {
                    val offsetX = i * w * 0.18f
                    val offsetY = i * h * 0.10f
                    val folder = Path().apply {
                        moveTo(offsetX + w * 0.15f, offsetY + h * 0.15f)
                        lineTo(offsetX + w * 0.45f, offsetY + h * 0.15f)
                        lineTo(offsetX + w * 0.58f, offsetY + h * 0.28f)
                        lineTo(offsetX + w * 0.95f, offsetY + h * 0.28f)
                        lineTo(offsetX + w * 0.95f, offsetY + h * 0.88f)
                        lineTo(offsetX + w * 0.15f, offsetY + h * 0.88f)
                        close()
                    }
                    drawPath(
                        path = folder,
                        color = accentColor.copy(alpha = 0.14f + (i * 0.05f))
                    )
                }
            }

            HomeCategoryType.PDF -> {
                // Stack of document sheets with ribbon curve
                for (i in 0..1) {
                    val offsetX = i * w * 0.18f
                    val offsetY = i * h * 0.08f
                    val doc = Path().apply {
                        moveTo(offsetX + w * 0.20f, offsetY + h * 0.10f)
                        lineTo(offsetX + w * 0.70f, offsetY + h * 0.10f)
                        lineTo(offsetX + w * 0.95f, offsetY + h * 0.32f)
                        lineTo(offsetX + w * 0.95f, offsetY + h * 0.90f)
                        lineTo(offsetX + w * 0.20f, offsetY + h * 0.90f)
                        close()
                    }
                    drawPath(
                        path = doc,
                        color = accentColor.copy(alpha = 0.13f + (i * 0.06f))
                    )
                }
                // Ribbon curve watermark
                val ribbon = Path().apply {
                    moveTo(w * 0.65f, h * 0.40f)
                    cubicTo(w * 0.45f, h * 0.52f, w * 0.40f, h * 0.70f, w * 0.55f, h * 0.80f)
                    cubicTo(w * 0.70f, h * 0.90f, w * 0.85f, h * 0.75f, w * 0.72f, h * 0.62f)
                    cubicTo(w * 0.58f, h * 0.50f, w * 0.80f, h * 0.42f, w * 0.65f, h * 0.40f)
                }
                drawPath(
                    path = ribbon,
                    color = accentColor.copy(alpha = 0.22f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            HomeCategoryType.WORD -> {
                // Stacked documents with large subtle 'W'
                val doc = Path().apply {
                    moveTo(w * 0.25f, h * 0.12f)
                    lineTo(w * 0.72f, h * 0.12f)
                    lineTo(w * 0.95f, h * 0.34f)
                    lineTo(w * 0.95f, h * 0.90f)
                    lineTo(w * 0.25f, h * 0.90f)
                    close()
                }
                drawPath(path = doc, color = accentColor.copy(alpha = 0.15f))
                // Large 'W' strokes
                val wPath = Path().apply {
                    moveTo(w * 0.42f, h * 0.38f)
                    lineTo(w * 0.50f, h * 0.76f)
                    lineTo(w * 0.60f, h * 0.48f)
                    lineTo(w * 0.70f, h * 0.76f)
                    lineTo(w * 0.78f, h * 0.38f)
                }
                drawPath(
                    path = wPath,
                    color = accentColor.copy(alpha = 0.18f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            HomeCategoryType.EXCEL -> {
                // Stacked document with table lines
                val doc = Path().apply {
                    moveTo(w * 0.25f, h * 0.12f)
                    lineTo(w * 0.72f, h * 0.12f)
                    lineTo(w * 0.95f, h * 0.34f)
                    lineTo(w * 0.95f, h * 0.90f)
                    lineTo(w * 0.25f, h * 0.90f)
                    close()
                }
                drawPath(path = doc, color = accentColor.copy(alpha = 0.15f))
                // Grid lines inside
                for (row in 1..3) {
                    val y = h * (0.35f + row * 0.13f)
                    drawLine(
                        color = accentColor.copy(alpha = 0.18f),
                        start = Offset(w * 0.38f, y),
                        end = Offset(w * 0.82f, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            HomeCategoryType.POWERPOINT -> {
                // Stacked slide with subtle 'P'
                val doc = Path().apply {
                    moveTo(w * 0.25f, h * 0.12f)
                    lineTo(w * 0.72f, h * 0.12f)
                    lineTo(w * 0.95f, h * 0.34f)
                    lineTo(w * 0.95f, h * 0.90f)
                    lineTo(w * 0.25f, h * 0.90f)
                    close()
                }
                drawPath(path = doc, color = accentColor.copy(alpha = 0.15f))
                // Large 'P' stroke
                val pPath = Path().apply {
                    moveTo(w * 0.48f, h * 0.76f)
                    lineTo(w * 0.48f, h * 0.38f)
                    lineTo(w * 0.66f, h * 0.38f)
                    cubicTo(w * 0.78f, h * 0.38f, w * 0.78f, h * 0.58f, w * 0.66f, h * 0.58f)
                    lineTo(w * 0.48f, h * 0.58f)
                }
                drawPath(
                    path = pPath,
                    color = accentColor.copy(alpha = 0.18f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            HomeCategoryType.TEXT -> {
                // Stacked sheet with subtle 'T'
                val doc = Path().apply {
                    moveTo(w * 0.25f, h * 0.12f)
                    lineTo(w * 0.72f, h * 0.12f)
                    lineTo(w * 0.95f, h * 0.34f)
                    lineTo(w * 0.95f, h * 0.90f)
                    lineTo(w * 0.25f, h * 0.90f)
                    close()
                }
                drawPath(path = doc, color = accentColor.copy(alpha = 0.15f))
                // Large 'T' strokes
                drawLine(
                    color = accentColor.copy(alpha = 0.18f),
                    start = Offset(w * 0.42f, h * 0.42f),
                    end = Offset(w * 0.78f, h * 0.42f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.18f),
                    start = Offset(w * 0.60f, h * 0.42f),
                    end = Offset(w * 0.60f, h * 0.76f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            HomeCategoryType.DIRECTORIES -> {
                val folder = Path().apply {
                    moveTo(w * 0.20f, h * 0.20f)
                    lineTo(w * 0.55f, h * 0.20f)
                    lineTo(w * 0.68f, h * 0.35f)
                    lineTo(w * 0.95f, h * 0.35f)
                    lineTo(w * 0.95f, h * 0.90f)
                    lineTo(w * 0.20f, h * 0.90f)
                    close()
                }
                drawPath(path = folder, color = accentColor.copy(alpha = 0.14f))
            }

            HomeCategoryType.RECENTS -> {
                drawCircle(
                    color = accentColor.copy(alpha = 0.14f),
                    radius = w * 0.35f,
                    center = Offset(w * 0.60f, h * 0.50f)
                )
            }

            HomeCategoryType.RECYCLE_BIN -> {
                val body = Path().apply {
                    moveTo(w * 0.35f, h * 0.30f)
                    lineTo(w * 0.85f, h * 0.30f)
                    lineTo(w * 0.78f, h * 0.88f)
                    lineTo(w * 0.42f, h * 0.88f)
                    close()
                }
                drawPath(path = body, color = accentColor.copy(alpha = 0.14f))
            }
        }
    }
}

/**
 * Real Directory Browser Dialog
 */
@Composable
private fun DirectoryBrowserDialog(
    directories: List<DirectoryInfo>,
    onDismiss: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onBrowseDeviceTree: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Device Directories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (directories.isEmpty()) {
                    Text(
                        text = "No document directories detected yet. Use the button below to pick and scan any folder on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Accessible folders with supported documents:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        items(directories, key = { it.name }) { dir ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectFolder(dir.name) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dir.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (dir.count == 1) "1 document" else "${dir.count} documents",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBrowseDeviceTree,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select & Scan Folder (SAF)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
