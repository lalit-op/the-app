package com.example.oneread.ui.screens

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.style.TextAlign
import java.io.FileOutputStream
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.oneread.data.DocumentItem
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.components.CommonViewerHeader
import com.example.oneread.ui.components.ConfirmDeleteDialog
import com.example.oneread.ui.components.FileInfoDialog
import com.example.oneread.ui.components.RenameDialog
import com.example.oneread.util.PdfSearchEngine
import com.example.oneread.util.PdfSearchOccurrence
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot

private fun clampOffset(offset: Offset, scale: Float, containerWidth: Float, containerHeight: Float): Offset {
    if (scale <= 1.0f) return Offset.Zero
    val maxPanX = (containerWidth * (scale - 1f)).coerceAtLeast(0f) / 2f
    val maxPanY = (containerHeight * (scale - 1f)).coerceAtLeast(0f) / 2f
    return Offset(
        x = offset.x.coerceIn(-maxPanX, maxPanX),
        y = offset.y.coerceIn(-maxPanY, maxPanY)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    document: DocumentItem,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<PdfSearchOccurrence>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(-1) }
    var isSearching by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
            searchMatches = emptyList()
            currentMatchIndex = -1
        } else {
            onBack()
        }
    }

    // Real-time document state for favorites/rename
    val allDocs by viewModel.allDocuments.collectAsState()
    val openTabs by viewModel.tabManager.tabs.collectAsState()
    val liveDocument = remember(allDocs, document) {
        allDocs.find { it.id == document.id } ?: document
    }

    // Settings persistence (SharedPreferences)
    val prefs = remember(context) {
        context.getSharedPreferences("hr_read_pdf_prefs", Context.MODE_PRIVATE)
    }

    // Requirement 2: Default scroll mode must be Continuous, and persistent across sessions
    var isContinuousMode by remember {
        mutableStateOf(prefs.getBoolean("pref_pdf_continuous_mode", true))
    }

    // Requirement 4 & 5: Configurable page spacing, default ~8dp, persistent
    var pageSpacingDp by remember {
        mutableIntStateOf(prefs.getInt("pref_pdf_page_spacing_dp", 8))
    }
    var showPageSpacingDialog by remember { mutableStateOf(false) }

    // Requirement 1: Password-protected PDF support
    var isPasswordRequired by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isUnlocking by remember { mutableStateOf(false) }
    var unlockedTempFile by remember { mutableStateOf<File?>(null) }
    var cachedRenderFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    var isInvertColors by remember { mutableStateOf(false) }
    var totalPages by remember { mutableIntStateOf(liveDocument.pageCount.coerceAtLeast(1)) }
    var currentPage by remember { mutableIntStateOf(liveDocument.lastReadPage.coerceAtLeast(1)) }
    var rotationAngle by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentPage, totalPages) {
        viewModel.updateTabProgress(page = currentPage, totalPages = totalPages)
    }

    // Fullscreen reading mode
    var isFullscreen by remember { mutableStateOf(false) }

    // Dialog & Menu states
    var showMoreMenu by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Native zoom and pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration.orientation) {
        scale = 1f
        offset = Offset.Zero
    }

    // System immersive mode and status bar contrast handling
    DisposableEffect(isFullscreen) {
        val window = (view.context as? Activity)?.window
        val prevLightBars = window?.let { WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars } ?: false
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.isAppearanceLightStatusBars = false
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.isAppearanceLightStatusBars = prevLightBars
            }
        }
    }

    // Bitmap cache for rendered pages
    val renderedPages = remember { mutableStateMapOf<Int, Bitmap>() }
    val renderMutex = remember { Mutex() }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun resolvePdfFile(doc: DocumentItem): File? = withContext(Dispatchers.IO) {
        // 1. Direct path check (if it's not a content URI)
        if (doc.path.isNotBlank() && !doc.path.startsWith("content://")) {
            val f = File(doc.path)
            if (f.exists() && f.canRead() && f.length() > 0) {
                return@withContext f
            }
        }
        // 2. URI check (content:// or file://)
        val targetUriStr = if (doc.uri.isNotBlank()) doc.uri else if (doc.path.startsWith("content://")) doc.path else ""
        if (targetUriStr.isNotBlank()) {
            try {
                val uri = Uri.parse(targetUriStr)
                if (uri.scheme == "file") {
                    val f = File(uri.path ?: "")
                    if (f.exists() && f.canRead() && f.length() > 0) return@withContext f
                }
                val tempFile = File(context.cacheDir, "pdf_render_${doc.id.coerceAtLeast(0)}_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    return@withContext tempFile
                }
            } catch (e: Exception) {
                Log.e("PdfViewerScreen", "Failed to resolve PDF from URI: $targetUriStr", e)
            }
        }
        // 3. Fallback: If path is not blank, try reading stream via Uri.fromFile
        if (doc.path.isNotBlank() && !doc.path.startsWith("content://")) {
            try {
                val uri = Uri.fromFile(File(doc.path))
                val tempFile = File(context.cacheDir, "pdf_render_${doc.id.coerceAtLeast(0)}_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    return@withContext tempFile
                }
            } catch (_: Exception) {}
        }
        null
    }

    fun openRendererForFile(targetFile: File) {
        try {
            val descriptor = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            pfd?.close()
            pdfRenderer?.close()
            pfd = descriptor
            pdfRenderer = renderer
            totalPages = renderer.pageCount
            renderedPages.values.forEach { if (!it.isRecycled) it.recycle() }
            renderedPages.clear()
            isLoading = false
            errorMessage = null
            isPasswordRequired = false
            showPasswordDialog = false
            passwordError = null
        } catch (e: Exception) {
            e.printStackTrace()
            isLoading = false
            errorMessage = e.localizedMessage ?: "Failed to render PDF."
        }
    }

    suspend fun attemptUnlock(password: String) = withContext(Dispatchers.IO) {
        isUnlocking = true
        withContext(Dispatchers.Main) { passwordError = null }
        val file = cachedRenderFile ?: File(liveDocument.path)
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val pdDoc = PDDocument.load(file, password)
            pdDoc.isAllSecurityToBeRemoved = true
            val tempFile = File(context.cacheDir, "unlocked_${file.name.hashCode()}_${System.currentTimeMillis()}.pdf")
            pdDoc.save(tempFile)
            pdDoc.close()

            withContext(Dispatchers.Main) {
                unlockedTempFile?.delete()
                unlockedTempFile = tempFile
                openRendererForFile(tempFile)
                isUnlocking = false
            }
        } catch (e: InvalidPasswordException) {
            withContext(Dispatchers.Main) {
                isUnlocking = false
                passwordError = "Incorrect password"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                isUnlocking = false
                passwordError = "Incorrect password"
            }
        }
    }

    // Initialize renderer with password protection detection and ContentResolver fallback
    LaunchedEffect(liveDocument.id, liveDocument.path, liveDocument.uri, reloadTrigger) {
        withContext(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isLoading = true
                errorMessage = null
            }
            val resolvedFile = resolvePdfFile(liveDocument)
            if (resolvedFile != null && resolvedFile.exists()) {
                if (resolvedFile.absolutePath != liveDocument.path) {
                    cachedRenderFile?.delete()
                    cachedRenderFile = resolvedFile
                }
                var requiresPassword = false
                try {
                    val descriptor = ParcelFileDescriptor.open(resolvedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(descriptor)
                    withContext(Dispatchers.Main) {
                        pfd?.close()
                        pdfRenderer?.close()
                        pfd = descriptor
                        pdfRenderer = renderer
                        totalPages = renderer.pageCount
                        isLoading = false
                        errorMessage = null
                    }
                } catch (e: SecurityException) {
                    requiresPassword = true
                } catch (e: Exception) {
                    if (e.message?.contains("password", ignoreCase = true) == true) {
                        requiresPassword = true
                    } else {
                        try {
                            PDFBoxResourceLoader.init(context.applicationContext)
                            val testDoc = PDDocument.load(resolvedFile)
                            if (testDoc.isEncrypted) {
                                requiresPassword = true
                            }
                            testDoc.close()
                        } catch (pe: InvalidPasswordException) {
                            requiresPassword = true
                        } catch (pe: Exception) {
                            if (pe.message?.contains("password", ignoreCase = true) == true) {
                                requiresPassword = true
                            } else {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "Failed to open PDF document."
                                }
                            }
                        }
                    }
                }

                if (requiresPassword) {
                    withContext(Dispatchers.Main) {
                        isPasswordRequired = true
                        showPasswordDialog = true
                        isLoading = false
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "File does not exist or cannot be accessed on this device."
                }
            }
        }
    }

    DisposableEffect(liveDocument.id, liveDocument.path) {
        onDispose {
            renderedPages.values.forEach { if (!it.isRecycled) it.recycle() }
            renderedPages.clear()
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
            unlockedTempFile?.delete()
            cachedRenderFile?.delete()
        }
    }

    // Function to render a specific page
    suspend fun renderPage(pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex !in 0 until renderer.pageCount) return@withContext null

        renderedPages[pageIndex]?.let { if (!it.isRecycled) return@withContext it }

        renderMutex.withLock {
            // Re-check cache under lock
            renderedPages[pageIndex]?.let { if (!it.isRecycled) return@withLock it }

            val r = pdfRenderer ?: return@withLock null
            if (pageIndex !in 0 until r.pageCount) return@withLock null

            var page: PdfRenderer.Page? = null
            try {
                page = r.openPage(pageIndex)
                val screenWidth = context.resources.displayMetrics.widthPixels
                val pageW = page.width
                val pageH = page.height

                if (pageW <= 0 || pageH <= 0) return@withLock null

                // Calculate optimal scale for razor-sharp handwritten scans
                val renderScale = (screenWidth.toFloat() / pageW.toFloat()).coerceIn(1.2f, 2.8f)
                var destWidth = (pageW * renderScale).toInt().coerceAtLeast(1)
                var destHeight = (pageH * renderScale).toInt().coerceAtLeast(1)

                // Texture safety limit: cap max dimension to 2560px
                val maxDim = 2560
                if (maxOf(destWidth, destHeight) > maxDim) {
                    val downscale = maxDim.toFloat() / maxOf(destWidth, destHeight).toFloat()
                    destWidth = (destWidth * downscale).toInt().coerceAtLeast(1)
                    destHeight = (destHeight * downscale).toInt().coerceAtLeast(1)
                }

                val bitmap = try {
                    Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    Bitmap.createBitmap((destWidth / 2).coerceAtLeast(1), (destHeight / 2).coerceAtLeast(1), Bitmap.Config.RGB_565)
                }

                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                renderedPages[pageIndex] = bitmap
                bitmap
            } catch (t: Throwable) {
                Log.e("PdfViewerScreen", "Error rendering page $pageIndex", t)
                null
            } finally {
                try {
                    page?.close()
                } catch (_: Exception) {}
            }
        }
    }

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = (currentPage - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = (currentPage - 1).coerceAtLeast(0), pageCount = { totalPages })

    LaunchedEffect(pagerState.currentPage) {
        if (!isContinuousMode) {
            currentPage = pagerState.currentPage + 1
            viewModel.updatePdfPage(liveDocument.id, currentPage)
            scale = 1f
            offset = Offset.Zero
        }
    }

    LaunchedEffect(lazyListState.firstVisibleItemIndex) {
        if (isContinuousMode) {
            currentPage = lazyListState.firstVisibleItemIndex + 1
            viewModel.updatePdfPage(liveDocument.id, currentPage)
        }
    }

    val goToNextMatch: () -> Unit = {
        if (searchMatches.isNotEmpty()) {
            val nextIdx = (currentMatchIndex + 1) % searchMatches.size
            currentMatchIndex = nextIdx
            val match = searchMatches[nextIdx]
            val targetPage = match.pageIndex
            currentPage = targetPage + 1
            scale = 1f
            offset = Offset.Zero
            coroutineScope.launch {
                if (isContinuousMode) {
                    lazyListState.animateScrollToItem(targetPage)
                } else {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        }
    }

    val goToPrevMatch: () -> Unit = {
        if (searchMatches.isNotEmpty()) {
            val prevIdx = if (currentMatchIndex - 1 < 0) searchMatches.size - 1 else currentMatchIndex - 1
            currentMatchIndex = prevIdx
            val match = searchMatches[prevIdx]
            val targetPage = match.pageIndex
            currentPage = targetPage + 1
            scale = 1f
            offset = Offset.Zero
            coroutineScope.launch {
                if (isContinuousMode) {
                    lazyListState.animateScrollToItem(targetPage)
                } else {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        PdfSearchEngine.init(context)
    }

    LaunchedEffect(liveDocument.id, liveDocument.path, cachedRenderFile) {
        val f = cachedRenderFile ?: File(liveDocument.path)
        if (f.exists()) {
            withContext(Dispatchers.IO) {
                PdfSearchEngine.getPageData(f.absolutePath)
            }
        }
    }

    LaunchedEffect(searchQuery, isSearchActive) {
        if (!isSearchActive || searchQuery.isBlank()) {
            searchMatches = emptyList()
            currentMatchIndex = -1
            isSearching = false
        } else {
            isSearching = true
            delay(200)
            val searchPath = cachedRenderFile?.absolutePath ?: liveDocument.path
            val results = if (File(searchPath).exists()) {
                PdfSearchEngine.search(searchPath, searchQuery)
            } else emptyList()
            searchMatches = results
            isSearching = false
            if (results.isNotEmpty()) {
                currentMatchIndex = 0
                val targetPage = results[0].pageIndex
                currentPage = targetPage + 1
                scale = 1f
                offset = Offset.Zero
                coroutineScope.launch {
                    if (isContinuousMode) {
                        lazyListState.animateScrollToItem(targetPage)
                    } else {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            } else {
                currentMatchIndex = -1
            }
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(120)
            try {
                searchFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isInvertColors) Color(0xFF121212) else Color(0xFFE2E8F0))
    ) {
        // --- 1. FULLSCREEN PDF CONTENT LAYER ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
                                searchMatches = emptyList()
                                currentMatchIndex = -1
                            } else {
                                isFullscreen = !isFullscreen
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val targetScale = 2.5f
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val d = tapOffset - center
                                scale = targetScale
                                val targetOffset = Offset(
                                    -d.x * (targetScale - 1f),
                                    -d.y * (targetScale - 1f)
                                )
                                offset = clampOffset(targetOffset, targetScale, size.width.toFloat(), size.height.toFloat())
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var prevCentroid = Offset.Zero
                        var prevDist = 0f
                        var prevSinglePos = down.position
                        var isPinching = false
                        var isPanning = false

                        do {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            val count = activePointers.size

                            if (count >= 2) {
                                val p0 = activePointers[0]
                                val p1 = activePointers[1]
                                val currentCentroid = (p0.position + p1.position) / 2f
                                val dx = p0.position.x - p1.position.x
                                val dy = p0.position.y - p1.position.y
                                val currentDist = hypot(dx, dy)

                                if (isPinching && prevDist > 0f) {
                                    val zoomChange = currentDist / prevDist
                                    val panChange = currentCentroid - prevCentroid

                                    val oldScale = scale
                                    val newScale = (oldScale * zoomChange).coerceIn(0.8f, 4.0f)
                                    val center = Offset(size.width / 2f, size.height / 2f)

                                    val ratio = newScale / oldScale
                                    val d = currentCentroid - center
                                    val rawOffset = offset * ratio + d * (1f - ratio) + panChange

                                    scale = newScale
                                    offset = clampOffset(rawOffset, newScale, size.width.toFloat(), size.height.toFloat())

                                    event.changes.forEach { it.consume() }
                                }

                                prevCentroid = currentCentroid
                                prevDist = currentDist
                                isPinching = true
                                isPanning = false
                                prevSinglePos = currentCentroid
                            } else if (count == 1) {
                                val singlePointer = activePointers[0]
                                val currentPos = singlePointer.position

                                if (scale > 1.05f) {
                                    if (isPanning) {
                                        val panChange = currentPos - prevSinglePos
                                        val rawOffset = offset + panChange
                                        offset = clampOffset(rawOffset, scale, size.width.toFloat(), size.height.toFloat())
                                        singlePointer.consume()
                                    }
                                    prevSinglePos = currentPos
                                    isPanning = true
                                    isPinching = false
                                    prevDist = 0f
                                } else {
                                    isPanning = false
                                    isPinching = false
                                    prevDist = 0f
                                }
                            } else {
                                isPinching = false
                                isPanning = false
                                prevDist = 0f
                            }
                        } while (event.changes.any { it.pressed })

                        if (scale < 1.0f) {
                            scale = 1.0f
                            offset = Offset.Zero
                        }
                    }
                }
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null || totalPages == 0 || pdfRenderer == null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = liveDocument.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = errorMessage ?: "Unable to read PDF file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { reloadTrigger++ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry")
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val uri = if (liveDocument.uri.isNotBlank()) {
                                            Uri.parse(liveDocument.uri)
                                        } else {
                                            androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                File(liveDocument.path)
                                            )
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open with"))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No external app available to open PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Text("Open With")
                            }
                            TextButton(
                                onClick = onBack,
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            } else {
                val colorFilter = if (isInvertColors) {
                    val matrix = floatArrayOf(
                        -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                         0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                         0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                         0.0f,  0.0f,  0.0f, 1.0f,   0.0f
                    )
                    ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(matrix))
                } else null

                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val cutoutInsets = WindowInsets.displayCutout.asPaddingValues()
                val startCutout = cutoutInsets.calculateStartPadding(LayoutDirection.Ltr)
                val endCutout = cutoutInsets.calculateEndPadding(LayoutDirection.Ltr)
                val topBarPadding = if (!isFullscreen) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 58.dp else 0.dp
                val bottomBarPadding = if (!isFullscreen) 36.dp else 0.dp

                if (isContinuousMode) {
                    // Continuous Vertical Scroll: Natural Fit-to-Width with landscape optimization
                    LazyColumn(
                        state = lazyListState,
                        userScrollEnabled = scale <= 1.05f,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentPadding = PaddingValues(
                            top = topBarPadding,
                            bottom = 24.dp,
                            start = startCutout,
                            end = endCutout
                        ),
                        verticalArrangement = Arrangement.spacedBy(pageSpacingDp.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(totalPages) { index ->
                            val cached = renderedPages[index]
                            var pageBitmap by remember(index, cached) { mutableStateOf(cached) }

                            LaunchedEffect(index, pdfRenderer) {
                                if (pageBitmap == null) {
                                    pageBitmap = renderPage(index)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (isLandscape) 24.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (pageBitmap != null) {
                                    val bmp = pageBitmap!!
                                    val isRotatedSideways = (rotationAngle % 180 != 0)
                                    val baseAspect = bmp.width.toFloat() / bmp.height.toFloat()
                                    val pageAspect = if (isRotatedSideways) (1f / baseAspect) else baseAspect

                                    Box(
                                        modifier = Modifier
                                            .then(
                                                if (isLandscape && baseAspect < 1.0f) {
                                                    // In landscape orientation, portrait pages have a comfortable width constraint
                                                    Modifier.widthIn(max = 840.dp).fillMaxWidth()
                                                } else {
                                                    Modifier.fillMaxWidth()
                                                }
                                            )
                                            .aspectRatio(pageAspect)
                                            .background(if (isInvertColors) Color.Black else Color.White)
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Page ${index + 1}",
                                            colorFilter = colorFilter,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(rotationAngle.toFloat())
                                        )

                                        // Search match highlights on page
                                        if (isSearchActive && searchMatches.isNotEmpty()) {
                                            val pageMatches = searchMatches.filter { it.pageIndex == index }
                                            if (pageMatches.isNotEmpty()) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    for (match in pageMatches) {
                                                        val isCurrent = (match.matchIndex == currentMatchIndex)
                                                        val fillColor = if (isCurrent) Color(0xD9FF9800) else Color(0x66FFEB3B)
                                                        val strokeColor = if (isCurrent) Color(0xFFFF3D00) else Color(0x99FBC02D)

                                                        for (rect in match.rects) {
                                                            val rx = rect.normalizedLeft * size.width
                                                            val ry = rect.normalizedTop * size.height
                                                            val rw = rect.normalizedWidth * size.width
                                                            val rh = rect.normalizedHeight * size.height

                                                            drawRoundRect(
                                                                color = fillColor,
                                                                topLeft = Offset(rx, ry),
                                                                size = androidx.compose.ui.geometry.Size(rw, rh),
                                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                                                            )
                                                            drawRoundRect(
                                                                color = strokeColor,
                                                                topLeft = Offset(rx, ry),
                                                                size = androidx.compose.ui.geometry.Size(rw, rh),
                                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                                    width = if (isCurrent) 2.dp.toPx() else 1.dp.toPx()
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(320.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Page by page horizontal pager: Responsive Fit-to-Screen in both Portrait & Landscape
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = scale <= 1.05f,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    ) { pageIdx ->
                        val cached = renderedPages[pageIdx]
                        var pageBitmap by remember(pageIdx, cached) { mutableStateOf(cached) }

                        LaunchedEffect(pageIdx, pdfRenderer) {
                            if (pageBitmap == null) {
                                pageBitmap = renderPage(pageIdx)
                            }
                        }

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = topBarPadding,
                                    bottom = bottomBarPadding,
                                    start = startCutout,
                                    end = endCutout
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pageBitmap != null) {
                                val bmp = pageBitmap!!
                                val isRotatedSideways = (rotationAngle % 180 != 0)
                                val baseAspect = bmp.width.toFloat() / bmp.height.toFloat()
                                val pageAspect = if (isRotatedSideways) (1f / baseAspect) else baseAspect
                                val containerAspect = (maxWidth.value / maxHeight.value.coerceAtLeast(1f)).coerceAtLeast(0.01f)

                                // Fit page within container bounds in both dimensions (perfect for landscape)
                                val (pageW, pageH) = if (pageAspect > containerAspect) {
                                    maxWidth to (maxWidth / pageAspect)
                                } else {
                                    (maxHeight * pageAspect) to maxHeight
                                }

                                Box(
                                    modifier = Modifier
                                        .size(pageW, pageH)
                                        .background(if (isInvertColors) Color.Black else Color.White)
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Page ${pageIdx + 1}",
                                        colorFilter = colorFilter,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .rotate(rotationAngle.toFloat())
                                    )

                                    // Search match highlights on page
                                    if (isSearchActive && searchMatches.isNotEmpty()) {
                                        val pageMatches = searchMatches.filter { it.pageIndex == pageIdx }
                                        if (pageMatches.isNotEmpty()) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                for (match in pageMatches) {
                                                    val isCurrent = (match.matchIndex == currentMatchIndex)
                                                    val fillColor = if (isCurrent) Color(0xD9FF9800) else Color(0x66FFEB3B)
                                                    val strokeColor = if (isCurrent) Color(0xFFFF3D00) else Color(0x99FBC02D)

                                                    for (rect in match.rects) {
                                                        val rx = rect.normalizedLeft * size.width
                                                        val ry = rect.normalizedTop * size.height
                                                        val rw = rect.normalizedWidth * size.width
                                                        val rh = rect.normalizedHeight * size.height

                                                        drawRoundRect(
                                                            color = fillColor,
                                                            topLeft = Offset(rx, ry),
                                                            size = androidx.compose.ui.geometry.Size(rw, rh),
                                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                                                        )
                                                        drawRoundRect(
                                                            color = strokeColor,
                                                            topLeft = Offset(rx, ry),
                                                            size = androidx.compose.ui.geometry.Size(rw, rh),
                                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                                width = if (isCurrent) 2.dp.toPx() else 1.dp.toPx()
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- 2. FLOATING PAGE INDICATOR OVERLAY ---
        AnimatedVisibility(
            visible = !isFullscreen && totalPages > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.65f),
                contentColor = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .clickable { showJumpDialog = true }
                    .testTag("floating_page_pill")
            ) {
                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        // --- 3. TOP TOOLBAR OVERLAY (TRUE EDGE-TO-EDGE DARK HEADER) ---
        AnimatedVisibility(
            visible = !isFullscreen,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            CommonViewerHeader(
                title = liveDocument.title,
                onBack = onBack,
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = "Search or jump to page...",
                searchFocusRequester = searchFocusRequester,
                isSearching = isSearching,
                searchMatchCount = searchMatches.size,
                currentMatchIndex = currentMatchIndex,
                showResultCounter = true,
                showNavArrows = true,
                onPrevMatch = { goToPrevMatch() },
                onNextMatch = { goToNextMatch() },
                onSearchExecute = {
                    val num = searchQuery.trim().toIntOrNull()
                    if (searchMatches.isEmpty() && num != null && num in 1..totalPages) {
                        currentPage = num
                        coroutineScope.launch {
                            if (isContinuousMode) {
                                lazyListState.animateScrollToItem(num - 1)
                            } else {
                                pagerState.animateScrollToPage(num - 1)
                            }
                        }
                    } else if (searchMatches.isNotEmpty()) {
                        goToNextMatch()
                    }
                },
                onClearSearch = {
                    searchQuery = ""
                    searchMatches = emptyList()
                    currentMatchIndex = -1
                },
                testTagPrefix = "pdf",
                openDocumentsCount = openTabs.size.coerceAtLeast(1),
                onDocumentSwitcherClick = { viewModel.openSwitcherSheet() },
                customActions = {
                    IconButton(
                        onClick = { rotationAngle = (rotationAngle + 90) % 360 },
                        modifier = Modifier.testTag("pdf_rotate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate",
                            tint = Color.White
                        )
                    }
                },
                overflowMenuItems = { onDismiss ->
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

                    // Go to page
                    DropdownMenuItem(
                        text = { Text("Go to page") },
                        leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            showJumpDialog = true
                        },
                        modifier = Modifier.testTag("menu_go_to_page")
                    )

                    // Page by page / Continuous mode toggle
                    DropdownMenuItem(
                        text = { Text(if (isContinuousMode) "Page-by-page mode" else "Continuous mode") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isContinuousMode) Icons.Default.ViewCarousel else Icons.Default.ViewStream,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onDismiss()
                            val newMode = !isContinuousMode
                            isContinuousMode = newMode
                            prefs.edit().putBoolean("pref_pdf_continuous_mode", newMode).apply()
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier.testTag("menu_toggle_view_mode")
                    )

                    // Page Spacing
                    DropdownMenuItem(
                        text = { Text("Page Spacing (${pageSpacingDp} dp)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FormatLineSpacing,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onDismiss()
                            showPageSpacingDialog = true
                        },
                        modifier = Modifier.testTag("menu_page_spacing")
                    )

                    // Night / Invert Colors mode
                    DropdownMenuItem(
                        text = { Text(if (isInvertColors) "Light mode" else "Night mode") },
                        leadingIcon = { Icon(Icons.Default.Brightness4, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            isInvertColors = !isInvertColors
                        },
                        modifier = Modifier.testTag("menu_toggle_night_mode")
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
        }
    }

    // --- 4. ACTION DIALOGS ---
    // Jump to page Dialog
    if (showJumpDialog) {
        var inputPageText by remember { mutableStateOf("$currentPage") }
        var sliderValue by remember { mutableFloatStateOf(currentPage.toFloat()) }

        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select a page from 1 to $totalPages:")
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            inputPageText = it.toInt().toString()
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        steps = (totalPages - 2).coerceAtLeast(0)
                    )
                    OutlinedTextField(
                        value = inputPageText,
                        onValueChange = {
                            inputPageText = it
                            val num = it.toIntOrNull()
                            if (num != null && num in 1..totalPages) {
                                sliderValue = num.toFloat()
                            }
                        },
                        label = { Text("Page Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPage = inputPageText.toIntOrNull()?.coerceIn(1, totalPages) ?: 1
                        currentPage = targetPage
                        coroutineScope.launch {
                            if (isContinuousMode) {
                                lazyListState.scrollToItem(targetPage - 1)
                            } else {
                                pagerState.scrollToPage(targetPage - 1)
                            }
                        }
                        showJumpDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog) {
        RenameDialog(
            document = liveDocument,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.renameDocument(liveDocument, newName)
                showRenameDialog = false
            }
        )
    }

    // File Details Dialog
    if (showInfoDialog) {
        FileInfoDialog(
            document = liveDocument,
            onDismiss = { showInfoDialog = false }
        )
    }

    // Move to Trash Dialog
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Move to Recycle Bin?",
            message = "Move \"${liveDocument.title}\" to the Recycle Bin?",
            confirmText = "Move to Trash",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteDocument(liveDocument)
                showDeleteDialog = false
                onBack()
            }
        )
    }

    // Requirement 1: Password Required Dialog
    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                onBack()
            },
            title = {
                Text(
                    text = "Password Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "This PDF is password protected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
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
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (passwordInput.isNotEmpty() && !isUnlocking) {
                                    coroutineScope.launch {
                                        attemptUnlock(passwordInput)
                                    }
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        isError = passwordError != null,
                        supportingText = if (passwordError != null) {
                            { Text(text = passwordError!!, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput.isNotEmpty() && !isUnlocking) {
                            coroutineScope.launch {
                                attemptUnlock(passwordInput)
                            }
                        }
                    },
                    enabled = passwordInput.isNotEmpty() && !isUnlocking,
                    modifier = Modifier.testTag("pdf_unlock_button")
                ) {
                    if (isUnlocking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Unlock")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        onBack()
                    },
                    modifier = Modifier.testTag("pdf_cancel_unlock_button")
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Requirement 4 & 5 & 6: Page Spacing Dialog
    if (showPageSpacingDialog) {
        var tempSpacing by remember { mutableFloatStateOf(pageSpacingDp.toFloat()) }

        AlertDialog(
            onDismissRequest = { showPageSpacingDialog = false },
            title = {
                Text(
                    text = "Page Spacing",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Control vertical space between PDF pages:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${tempSpacing.toInt()} dp",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = tempSpacing,
                        onValueChange = { tempSpacing = it },
                        valueRange = 0f..20f,
                        steps = 4, // 0, 4, 8, 12, 16, 20
                        modifier = Modifier.testTag("pdf_page_spacing_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(0, 4, 8, 12, 16, 20).forEach { preset ->
                            FilterChip(
                                selected = tempSpacing.toInt() == preset,
                                onClick = { tempSpacing = preset.toFloat() },
                                label = { Text("${preset}dp", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalSpacing = tempSpacing.toInt()
                        pageSpacingDp = finalSpacing
                        prefs.edit().putInt("pref_pdf_page_spacing_dp", finalSpacing).apply()
                        showPageSpacingDialog = false
                    },
                    modifier = Modifier.testTag("pdf_page_spacing_apply")
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPageSpacingDialog = false }
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
