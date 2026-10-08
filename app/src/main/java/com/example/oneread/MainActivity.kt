package com.example.oneread

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import com.example.oneread.util.KeepScreenOnManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.outlined.Tab
import com.example.oneread.workspace.ui.DocumentWorkspaceScreen
import com.example.oneread.workspace.ui.DocumentSwitcherBottomSheet
import com.example.oneread.excel.ui.ExcelViewerScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.navigation.AppThemeMode
import com.example.oneread.ui.navigation.BottomTab
import com.example.oneread.ui.navigation.Screen
import com.example.oneread.ui.screens.FaqScreen
import com.example.oneread.ui.screens.FavoritesScreen
import com.example.oneread.ui.screens.FilesScreen
import com.example.oneread.ui.screens.HomeScreen
import com.example.oneread.ui.screens.ImageToPdfScreen
import com.example.oneread.ui.screens.ImageViewerScreen
import com.example.oneread.ui.screens.MergePdfScreen
import com.example.oneread.ui.screens.PdfViewerScreen
import com.example.oneread.ui.screens.PptViewerScreen
import com.example.oneread.ui.screens.RecentScreen
import com.example.oneread.ui.screens.RecycleBinScreen
import com.example.oneread.ui.screens.SettingsScreen
import com.example.oneread.ui.screens.SplashScreen
import com.example.oneread.ui.screens.SplitPdfScreen
import com.example.oneread.ui.screens.TextViewerScreen
import com.example.oneread.ui.screens.ToolsScreen
import com.example.oneread.ui.theme.OneReadTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        // Initialize centralized persistent KeepScreenOnManager
        KeepScreenOnManager.init(applicationContext)
        KeepScreenOnManager.attachWindow(window)

        // Handle opening documents from outside (VIEW or SEND intent)
        handleIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            // Storage permission launcher for pre-Android 11
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val anyGranted = permissions.values.any { it }
                if (anyGranted) {
                    viewModel.refreshDocuments(showStatus = true)
                }
            }

            // Storage permission launcher for Android 11+ (All Files Access)
            val manageStorageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.refreshDocuments(showStatus = true)
            }

            val isSplashActive by viewModel.isSplashActive.collectAsState()
            val splashProgress by viewModel.splashProgress.collectAsState()
            val splashStatusText by viewModel.splashStatusText.collectAsState()

            // Observe Activity lifecycle ON_RESUME to re-check real permission on return from Settings
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        viewModel.onAppForeground()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(isSplashActive) {
                if (!isSplashActive) {
                    viewModel.checkFileAccess()
                }
            }

            OneReadTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF030712)
                ) {
                    AnimatedContent(
                        targetState = isSplashActive,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(350))
                        },
                        label = "splash_exit_transition"
                    ) { splashActive ->
                        if (splashActive) {
                            SplashScreen(
                                progress = splashProgress,
                                statusText = splashStatusText,
                                onFinished = { viewModel.dismissSplash() }
                            )
                        } else {
                            OneReadApp(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        KeepScreenOnManager.reapply(this)
        viewModel.onAppForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        KeepScreenOnManager.detachWindow(window)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val dataUri: Uri? = intent.data ?: androidx.core.content.IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java
        )

        if ((action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND) && dataUri != null) {
            viewModel.importFile(dataUri)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OneReadApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val openTabs by viewModel.tabManager.tabs.collectAsState()
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()
    val closedHistory by viewModel.tabManager.closedTabHistory.collectAsState()
    val isSwitcherVisible by viewModel.isSwitcherSheetVisible.collectAsState()

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.closeSwitcherSheet()
            viewModel.importFile(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState is Screen.Main) {
                    // Popping back to Main screen
                    (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut())
                } else if (initialState is Screen.Main) {
                    // Navigating from Main to a sub-screen
                    (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn()) togetherWith
                    (slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } + fadeOut())
                } else {
                    // Navigating between sub-screens
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.95f)) togetherWith
                    (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 1.02f))
                }
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is Screen.Workspace -> {
                    DocumentWorkspaceScreen(
                        viewModel = viewModel,
                        onNavigateToLibrary = { viewModel.navigateTo(Screen.Main) }
                    )
                }
                is Screen.Main -> {
                    val showPermissionRequired by viewModel.showPermissionRequired.collectAsState()
                    val hasFileAccess by viewModel.hasFileAccess.collectAsState()
                    Scaffold(
                        containerColor = Color(0xFF050811),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (hasFileAccess || !showPermissionRequired) {
                                AppBottomNavigation(
                                    currentTab = currentTab,
                                    onTabSelected = { viewModel.setTab(it) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        val bottomPadding = innerPadding.calculateBottomPadding()
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                val forward = targetState.ordinal > initialState.ordinal
                                if (forward) {
                                    (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeIn()) togetherWith
                                    (slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 } + fadeOut())
                                } else {
                                    (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 } + fadeIn()) togetherWith
                                    (slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeOut())
                                }
                            },
                            label = "tab_transition"
                        ) { tab ->
                            when (tab) {
                                BottomTab.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(bottom = bottomPadding)
                                )
                                BottomTab.FAVORITES -> FavoritesScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(bottom = bottomPadding)
                                )
                                BottomTab.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(bottom = bottomPadding)
                                )
                            }
                        }
                    }
                }
                is Screen.Files -> {
                    FilesScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.PdfViewer -> {
                    PdfViewerScreen(
                        document = screen.document,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.PptViewer -> {
                    PptViewerScreen(
                        document = screen.document,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.ExcelViewer -> {
                    ExcelViewerScreen(
                        document = screen.document,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.TextViewer -> {
                    TextViewerScreen(
                        document = screen.document,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.ImageViewer -> {
                    ImageViewerScreen(
                        document = screen.document,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.ImageToPdf -> {
                    ImageToPdfScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.MergePdf -> {
                    MergePdfScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.SplitPdf -> {
                    SplitPdfScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.Recent -> {
                    RecentScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.RecycleBin -> {
                    RecycleBinScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.Faq -> {
                    FaqScreen(
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }
        }

        // WPS-style Compact Bottom-Sheet Document Switcher
        if (isSwitcherVisible) {
            DocumentSwitcherBottomSheet(
                openTabs = openTabs,
                activeTabId = activeTabId,
                closedHistory = closedHistory,
                onDismissRequest = { viewModel.closeSwitcherSheet() },
                onSelectTab = { tab -> viewModel.switchToTab(tab) },
                onCloseTab = { tabId -> viewModel.closeTabFromSwitcher(tabId) },
                onReopenClosedTab = { tab -> viewModel.reopenClosedTabFromSwitcher(tab) },
                onOpenNewDocument = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "text/plain",
                            "text/csv",
                            "text/markdown",
                            "image/*",
                            "*/*"
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun AppBottomNavigation(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF050811),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        ) {
            // Horizontal gradient divider: blue → purple → blue
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1D4ED8),
                                Color(0xFF3B82F6),
                                Color(0xFF8B5CF6),
                                Color(0xFF3B82F6),
                                Color(0xFF1D4ED8)
                            )
                        )
                    )
            )

            // Navigation row with 3 clean, standard items: Home | Favorites | Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HRReadNavButton(
                    selected = currentTab == BottomTab.HOME,
                    label = "Home",
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    testTag = "nav_home",
                    onClick = { onTabSelected(BottomTab.HOME) }
                )

                HRReadNavButton(
                    selected = currentTab == BottomTab.FAVORITES,
                    label = "Favorites",
                    selectedIcon = Icons.Filled.Star,
                    unselectedIcon = Icons.Outlined.StarBorder,
                    testTag = "nav_favorites",
                    onClick = { onTabSelected(BottomTab.FAVORITES) }
                )

                HRReadNavButton(
                    selected = currentTab == BottomTab.SETTINGS,
                    label = "Settings",
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    testTag = "nav_settings",
                    onClick = { onTabSelected(BottomTab.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun HRReadNavButton(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_btn_scale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_icon_scale"
    )

    val pillWidth by animateDpAsState(
        targetValue = if (selected) 64.dp else 46.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_pill_width"
    )

    val pillBgColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF14274E).copy(alpha = 0.85f) else Color.Transparent,
        animationSpec = tween(220),
        label = "nav_pill_bg"
    )

    val pillBorderColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF2979FF) else Color.Transparent,
        animationSpec = tween(220),
        label = "nav_pill_border"
    )

    val labelColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF2979FF) else Color(0xFF90A4AE),
        animationSpec = tween(200),
        label = "nav_label_color"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF2979FF) else Color(0xFF90A4AE),
        animationSpec = tween(200),
        label = "nav_icon_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(buttonScale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 32.dp),
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(32.dp)
                .drawBehind {
                    if (selected) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF2979FF).copy(alpha = 0.28f), Color.Transparent),
                                radius = size.maxDimension * 0.95f
                            )
                        )
                    }
                }
                .clip(RoundedCornerShape(20.dp))
                .background(pillBgColor)
                .border(
                    BorderStroke(1.2.dp, pillBorderColor),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier
                    .size(if (selected) 20.dp else 22.dp)
                    .scale(iconScale)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
