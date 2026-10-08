package com.example.oneread.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.oneread.data.DocumentItem
import com.example.oneread.ppt.model.PresentationModel
import com.example.oneread.ppt.model.SlideModel
import com.example.oneread.ppt.parser.LegacyPptParser
import com.example.oneread.ppt.parser.PptxParser
import com.example.oneread.ppt.render.SlideRasterizer
import com.example.oneread.ppt.ui.*
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class PptViewMode {
    NORMAL,
    SLIDE_SORTER,
    PRESENTATION
}

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
fun PptViewerScreen(
    document: DocumentItem,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time document state for tabs, favorites, rename
    val allDocs by viewModel.allDocuments.collectAsState()
    val openTabs by viewModel.tabManager.tabs.collectAsState()
    val liveDocument = remember(allDocs, document) {
        allDocs.find { it.id == document.id } ?: document
    }

    // Presentation model & loading state
    var presentation by remember { mutableStateOf<PresentationModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // View mode: Normal, Slide Sorter, Presentation
    var viewMode by remember { mutableStateOf(PptViewMode.NORMAL) }
    var showThumbnailStrip by remember { mutableStateOf(true) }

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var searchMatches by remember { mutableStateOf<List<Int>>(emptyList()) } // Slide indices with matches
    var currentMatchIndex by remember { mutableIntStateOf(-1) }

    // Fullscreen state
    var isFullscreen by remember { mutableStateOf(false) }

    // Zoom state (Default: 100% / 1.0f)
    val initialTabZoom = viewModel.tabManager.activeTab?.zoomLevel ?: 1.0f
    var zoomLevel by remember(liveDocument.id) {
        mutableFloatStateOf(if (initialTabZoom in 0.5f..3.0f) initialTabZoom else 1.0f)
    }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Dialog states
    var showJumpDialog by remember { mutableStateOf(false) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showNewSlideDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Bitmap cache for rendered slides
    val slideBitmapCache = remember { mutableStateMapOf<Int, Bitmap>() }
    val thumbnailBitmapCache = remember { mutableStateMapOf<Int, Bitmap>() }

    fun setZoom(newZoom: Float) {
        val clamped = newZoom.coerceIn(0.5f, 3.0f)
        zoomLevel = clamped
        viewModel.updateTabProgress(zoom = clamped)
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
        offset = Offset.Zero
    }

    // Back handling: search -> dialogs -> sorter/presentation -> unsaved -> exit
    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
            searchMatches = emptyList()
            currentMatchIndex = -1
        } else if (viewMode != PptViewMode.NORMAL) {
            viewMode = PptViewMode.NORMAL
        } else if (isFullscreen) {
            isFullscreen = false
        } else if (presentation?.isModified == true) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    // Immersive system bars handling
    val isImmersive = isFullscreen || viewMode == PptViewMode.PRESENTATION
    DisposableEffect(isImmersive) {
        val window = (view.context as? Activity)?.window
        val prevLightBars = window?.let { WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars } ?: false
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.isAppearanceLightStatusBars = false
            if (isImmersive) {
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

    // Load presentation
    fun loadPresentation() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val file = File(liveDocument.path)
                val ext = file.name.substringAfterLast('.', "").lowercase()
                val isLegacy = ext in setOf("ppt", "pps", "pot")

                val model = if (file.exists() && file.canRead()) {
                    if (isLegacy) LegacyPptParser.parse(file)
                    else PptxParser.parse(file)
                } else if (liveDocument.uri.isNotBlank()) {
                    val temp = File(context.cacheDir, "temp_ppt_${System.currentTimeMillis()}.${if (isLegacy) "ppt" else "pptx"}")
                    context.contentResolver.openInputStream(Uri.parse(liveDocument.uri))?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (isLegacy) LegacyPptParser.parse(temp)
                    else PptxParser.parse(temp)
                } else {
                    throw IllegalArgumentException("File not found or unreadable: ${liveDocument.path}")
                }

                withContext(Dispatchers.Main) {
                    presentation = model
                    slideBitmapCache.clear()
                    thumbnailBitmapCache.clear()
                    isLoading = false
                    viewModel.updateTabProgress(
                        sheet = liveDocument.lastReadPage.coerceAtLeast(1),
                        totalPages = model.slides.size.coerceAtLeast(1),
                        zoom = zoomLevel
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    errorMessage = e.message ?: "Unable to open presentation"
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(liveDocument.path, liveDocument.uri) {
        loadPresentation()
    }

    // Clean up bitmaps on dispose
    DisposableEffect(Unit) {
        onDispose {
            slideBitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
            slideBitmapCache.clear()
            thumbnailBitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
            thumbnailBitmapCache.clear()
        }
    }

    val totalSlides = presentation?.slides?.size ?: 0
    val pagerState = rememberPagerState(
        initialPage = (liveDocument.lastReadPage - 1).coerceIn(0, (totalSlides - 1).coerceAtLeast(0)),
        pageCount = { totalSlides.coerceAtLeast(1) }
    )

    // Save reading progress to global tab manager
    LaunchedEffect(pagerState.currentPage, totalSlides) {
        if (totalSlides > 0) {
            viewModel.updateTabProgress(
                sheet = pagerState.currentPage + 1,
                totalPages = totalSlides,
                zoom = zoomLevel
            )
            viewModel.updatePdfPage(liveDocument.id, pagerState.currentPage + 1)
        }
    }

    // Search matching logic
    LaunchedEffect(searchQuery, presentation) {
        if (searchQuery.isBlank() || presentation == null) {
            searchMatches = emptyList()
            currentMatchIndex = -1
        } else {
            val matches = mutableListOf<Int>()
            presentation!!.slides.forEachIndexed { idx, slide ->
                if (slide.fullSearchableText.contains(searchQuery, ignoreCase = true)) {
                    matches.add(idx)
                }
            }
            searchMatches = matches
            currentMatchIndex = if (matches.isNotEmpty()) 0 else -1
            if (matches.isNotEmpty()) {
                pagerState.animateScrollToPage(matches[0])
            }
        }
    }

    val onNextMatch: () -> Unit = {
        if (searchMatches.isNotEmpty()) {
            val next = (currentMatchIndex + 1) % searchMatches.size
            currentMatchIndex = next
            coroutineScope.launch {
                pagerState.animateScrollToPage(searchMatches[next])
            }
        }
    }

    val onPrevMatch: () -> Unit = {
        if (searchMatches.isNotEmpty()) {
            val prev = if (currentMatchIndex <= 0) searchMatches.size - 1 else currentMatchIndex - 1
            currentMatchIndex = prev
            coroutineScope.launch {
                pagerState.animateScrollToPage(searchMatches[prev])
            }
        }
    }

    // Render slide on demand with memory caching
    fun getOrRenderSlideBitmap(slideIndex: Int, width: Int, height: Int): Bitmap? {
        val cached = slideBitmapCache[slideIndex]
        if (cached != null && !cached.isRecycled) return cached

        val slide = presentation?.slides?.getOrNull(slideIndex) ?: return null
        val bmp = SlideRasterizer.renderSlide(
            slide = slide,
            targetWidth = width,
            targetHeight = height,
            searchQuery = searchQuery
        )
        slideBitmapCache[slideIndex] = bmp

        // Evict distant slides if cache exceeds 8 full-resolution slides
        if (slideBitmapCache.size > 8) {
            val toRemove = slideBitmapCache.keys.filter { kotlin.math.abs(it - pagerState.currentPage) > 2 }
            toRemove.forEach { k ->
                slideBitmapCache.remove(k)?.recycle()
            }
        }

        return bmp
    }

    // Main Viewer Scaffold
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFEA580C), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Opening presentation...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (errorMessage != null || presentation == null || totalSlides == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "Unable to open presentation",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row {
                        Button(
                            onClick = { loadPresentation() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Text("Retry")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = onBack) {
                            Text("Back", color = Color.White)
                        }
                    }
                }
            }
        } else {
            val pres = presentation!!

            when (viewMode) {
                // =========================================================================
                // 1. SLIDE SORTER VIEW
                // =========================================================================
                PptViewMode.SLIDE_SORTER -> {
                    PptSlideSorterView(
                        presentation = pres,
                        activeSlideIndex = pagerState.currentPage,
                        thumbnailCache = thumbnailBitmapCache,
                        onSelectSlide = { idx ->
                            coroutineScope.launch {
                                pagerState.scrollToPage(idx)
                                viewMode = PptViewMode.NORMAL
                            }
                        },
                        onMoveSlide = { from, to ->
                            presentation = pres.moveSlide(from, to)
                            thumbnailBitmapCache.clear()
                            slideBitmapCache.clear()
                        },
                        onDuplicateSlide = { idx ->
                            presentation = pres.duplicateSlide(idx)
                            thumbnailBitmapCache.clear()
                            slideBitmapCache.clear()
                        },
                        onDeleteSlide = { idx ->
                            presentation = pres.deleteSlide(idx)
                            thumbnailBitmapCache.clear()
                            slideBitmapCache.clear()
                        },
                        onAddNewSlide = { showNewSlideDialog = true },
                        onClose = { viewMode = PptViewMode.NORMAL }
                    )
                }

                // =========================================================================
                // 2. PRESENTATION MODE (Full-screen distraction-free)
                // =========================================================================
                PptViewMode.PRESENTATION -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { tapOffset ->
                                        val width = size.width
                                        if (tapOffset.x > width * 0.6f) {
                                            // Tap right side -> Next slide
                                            if (pagerState.currentPage < totalSlides - 1) {
                                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                            }
                                        } else if (tapOffset.x < width * 0.4f) {
                                            // Tap left side -> Previous slide
                                            if (pagerState.currentPage > 0) {
                                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        // Slide Deck
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIndex ->
                            val slide = pres.slides.getOrNull(pageIndex)
                            if (slide != null) {
                                var slideBitmap by remember(pageIndex, pres) { mutableStateOf<Bitmap?>(null) }
                                LaunchedEffect(pageIndex, pres) {
                                    withContext(Dispatchers.IO) {
                                        val bmp = getOrRenderSlideBitmap(pageIndex, 1920, (1920 / pres.aspectRatio).roundToInt())
                                        withContext(Dispatchers.Main) { slideBitmap = bmp }
                                    }
                                }

                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (slideBitmap != null) {
                                        Image(
                                            bitmap = slideBitmap!!.asImageBitmap(),
                                            contentDescription = "Slide ${pageIndex + 1}",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .aspectRatio(pres.aspectRatio)
                                                .fillMaxSize()
                                        )
                                    } else {
                                        CircularProgressIndicator(color = Color(0xFFEA580C))
                                    }
                                }
                            }
                        }

                        // Bottom Floating Presenter Bar
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Black.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp)
                                .testTag("ppt_presenter_bar")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (pagerState.currentPage > 0) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                        }
                                    },
                                    enabled = pagerState.currentPage > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous", tint = if (pagerState.currentPage > 0) Color.White else Color(0xFF475569))
                                }

                                Text(
                                    text = "${pagerState.currentPage + 1} / $totalSlides",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )

                                IconButton(
                                    onClick = {
                                        if (pagerState.currentPage < totalSlides - 1) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                        }
                                    },
                                    enabled = pagerState.currentPage < totalSlides - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next", tint = if (pagerState.currentPage < totalSlides - 1) Color.White else Color(0xFF475569))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    onClick = { viewMode = PptViewMode.NORMAL },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFDC2626).copy(alpha = 0.85f),
                                    modifier = Modifier.testTag("ppt_exit_presentation")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Exit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 3. NORMAL VIEW (Current Slide + Controls + Bottom Thumbnails)
                // =========================================================================
                PptViewMode.NORMAL -> {
                    val topBarPadding = if (!isFullscreen) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp else 0.dp

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topBarPadding)
                    ) {
                        // Slide Presentation Stage
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clipToBounds()
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
                                        onDoubleTap = {
                                            if (zoomLevel > 1.05f) {
                                                resetZoom()
                                            } else {
                                                setZoom(2.0f)
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
                                                    val oldScale = zoomLevel
                                                    val newScale = (oldScale * zoomChange).coerceIn(0.5f, 3.0f)
                                                    setZoom(newScale)

                                                    val center = Offset(size.width / 2f, size.height / 2f)
                                                    val ratio = newScale / oldScale
                                                    val d = currentCentroid - center
                                                    val rawOffset = offset * ratio + d * (1f - ratio) + panChange
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

                                                if (zoomLevel > 1.05f) {
                                                    if (isPanning) {
                                                        val panChange = currentPos - prevSinglePos
                                                        val rawOffset = offset + panChange
                                                        offset = clampOffset(rawOffset, zoomLevel, size.width.toFloat(), size.height.toFloat())
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
                                            }
                                        } while (event.changes.any { it.pressed })

                                        if (zoomLevel < 1.0f) {
                                            setZoom(1.0f)
                                            offset = Offset.Zero
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = zoomLevel <= 1.05f,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomLevel,
                                        scaleY = zoomLevel,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            ) { pageIndex ->
                                val slide = pres.slides.getOrNull(pageIndex)
                                if (slide != null) {
                                    var slideBitmap by remember(pageIndex, pres) { mutableStateOf<Bitmap?>(null) }

                                    LaunchedEffect(pageIndex, pres, searchQuery) {
                                        withContext(Dispatchers.IO) {
                                            val bmp = getOrRenderSlideBitmap(pageIndex, 1920, (1920 / pres.aspectRatio).roundToInt())
                                            withContext(Dispatchers.Main) { slideBitmap = bmp }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(
                                                horizontal = if (isFullscreen) 0.dp else 12.dp,
                                                vertical = if (isFullscreen) 0.dp else 14.dp
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (slideBitmap != null) {
                                            Image(
                                                bitmap = slideBitmap!!.asImageBitmap(),
                                                contentDescription = "Slide ${pageIndex + 1}: ${slide.title}",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .aspectRatio(pres.aspectRatio)
                                                    .fillMaxWidth()
                                                    .shadow(if (isFullscreen) 0.dp else 10.dp, RoundedCornerShape(4.dp))
                                            )
                                        } else {
                                            CircularProgressIndicator(color = Color(0xFFEA580C), modifier = Modifier.size(36.dp))
                                        }
                                    }
                                }
                            }

                            // Slide Nav Chevron (Left)
                            if (pagerState.currentPage > 0 && !isFullscreen) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 6.dp)
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .testTag("ppt_prev_slide_chevron")
                                ) {
                                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Slide", tint = Color.White)
                                }
                            }

                            // Slide Nav Chevron (Right)
                            if (pagerState.currentPage < totalSlides - 1 && !isFullscreen) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 6.dp)
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .testTag("ppt_next_slide_chevron")
                                ) {
                                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Slide", tint = Color.White)
                                }
                            }

                            // Floating Slide Pill Indicator (e.g. "Slide 7 of 42")
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isFullscreen && totalSlides > 0 && !showThumbnailStrip,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.Black.copy(alpha = 0.75f),
                                    contentColor = Color.White,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .clickable { showJumpDialog = true }
                                        .testTag("floating_slide_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Slideshow,
                                            contentDescription = null,
                                            tint = Color(0xFFFB923C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Slide ${pagerState.currentPage + 1} of $totalSlides",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Slide Thumbnail Navigation Strip
                        if (showThumbnailStrip && !isFullscreen) {
                            PptThumbnailStrip(
                                presentation = pres,
                                currentSlideIndex = pagerState.currentPage,
                                thumbnailCache = thumbnailBitmapCache,
                                onSelectSlide = { idx ->
                                    coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                                },
                                modifier = Modifier.testTag("ppt_bottom_thumbnail_strip")
                            )
                        }
                    }

                    // Pinned Header
                    AnimatedVisibility(
                        visible = !isFullscreen,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        val titleWithModified = buildString {
                            append(liveDocument.title)
                            if (pres.isModified) append(" ●")
                        }

                        CommonViewerHeader(
                            title = titleWithModified,
                            onBack = {
                                if (pres.isModified) {
                                    showUnsavedDialog = true
                                } else {
                                    onBack()
                                }
                            },
                            isSearchActive = isSearchActive,
                            onSearchActiveChange = { active ->
                                isSearchActive = active
                                if (!active) {
                                    searchQuery = ""
                                    searchMatches = emptyList()
                                    currentMatchIndex = -1
                                }
                            },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { q -> searchQuery = q },
                            searchPlaceholder = "Search presentation...",
                            searchFocusRequester = searchFocusRequester,
                            searchMatchCount = searchMatches.size,
                            currentMatchIndex = currentMatchIndex,
                            showResultCounter = true,
                            showNavArrows = true,
                            onNextMatch = onNextMatch,
                            onPrevMatch = onPrevMatch,
                            testTagPrefix = "ppt_viewer",
                            openDocumentsCount = openTabs.size.coerceAtLeast(1),
                            onDocumentSwitcherClick = { viewModel.openSwitcherSheet() },
                            customActions = {
                                // Zoom Stepper: [ - ] 100% [ + ]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E293B))
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                        .height(30.dp)
                                        .testTag("ppt_zoom_toolbar_controls")
                                ) {
                                    IconButton(
                                        onClick = { zoomOut() },
                                        enabled = zoomLevel > 0.5f,
                                        modifier = Modifier.size(28.dp).testTag("ppt_zoom_out_button")
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
                                                showZoomDialog = true
                                            }
                                        },
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .padding(horizontal = 1.dp)
                                            .testTag("ppt_zoom_indicator")
                                    ) {
                                        Text(
                                            text = "${(zoomLevel * 100).roundToInt()}%",
                                            color = if (zoomLevel == 1.0f) Color(0xFFFB923C) else Color(0xFF38BDF8),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { zoomIn() },
                                        enabled = zoomLevel < 3.0f,
                                        modifier = Modifier.size(28.dp).testTag("ppt_zoom_in_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Zoom In",
                                            tint = if (zoomLevel < 3.0f) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                // Present Button
                                IconButton(
                                    onClick = { viewMode = PptViewMode.PRESENTATION },
                                    modifier = Modifier.size(36.dp).testTag("ppt_present_button")
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Present", tint = Color(0xFF22C55E), modifier = Modifier.size(22.dp))
                                }

                                // Slide Sorter Button
                                IconButton(
                                    onClick = { viewMode = PptViewMode.SLIDE_SORTER },
                                    modifier = Modifier.size(36.dp).testTag("ppt_sorter_button")
                                ) {
                                    Icon(imageVector = Icons.Default.GridView, contentDescription = "Slide Sorter", tint = Color.White, modifier = Modifier.size(19.dp))
                                }
                            },
                            overflowMenuItems = { onDismiss ->
                                DropdownMenuItem(
                                    text = { Text("Present Slideshow") },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF22C55E)) },
                                    onClick = {
                                        onDismiss()
                                        viewMode = PptViewMode.PRESENTATION
                                    },
                                    modifier = Modifier.testTag("menu_present")
                                )
                                DropdownMenuItem(
                                    text = { Text("Slide Sorter") },
                                    leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, tint = Color(0xFF38BDF8)) },
                                    onClick = {
                                        onDismiss()
                                        viewMode = PptViewMode.SLIDE_SORTER
                                    },
                                    modifier = Modifier.testTag("menu_slide_sorter")
                                )
                                DropdownMenuItem(
                                    text = { Text("Speaker Notes") },
                                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = Color(0xFFFB923C)) },
                                    onClick = {
                                        onDismiss()
                                        showNotesDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_speaker_notes")
                                )
                                DropdownMenuItem(
                                    text = { Text("New Slide") },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF10B981)) },
                                    onClick = {
                                        onDismiss()
                                        showNewSlideDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_new_slide")
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate Slide") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        onDismiss()
                                        presentation = pres.duplicateSlide(pagerState.currentPage)
                                        thumbnailBitmapCache.clear()
                                        slideBitmapCache.clear()
                                    },
                                    modifier = Modifier.testTag("menu_duplicate_slide")
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Slide") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        onDismiss()
                                        presentation = pres.deleteSlide(pagerState.currentPage)
                                        thumbnailBitmapCache.clear()
                                        slideBitmapCache.clear()
                                    },
                                    enabled = pres.slides.size > 1,
                                    modifier = Modifier.testTag("menu_delete_slide")
                                )
                                DropdownMenuItem(
                                    text = { Text("Go to slide") },
                                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                                    onClick = {
                                        onDismiss()
                                        showJumpDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_go_to_slide")
                                )
                                DropdownMenuItem(
                                    text = { Text("Zoom (${(zoomLevel * 100).roundToInt()}%)") },
                                    leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null) },
                                    onClick = {
                                        onDismiss()
                                        showZoomDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_zoom")
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset Zoom (100%)") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        onDismiss()
                                        resetZoom()
                                    },
                                    modifier = Modifier.testTag("menu_reset_zoom")
                                )
                                DropdownMenuItem(
                                    text = { Text(if (showThumbnailStrip) "Hide Thumbnail Strip" else "Show Thumbnail Strip") },
                                    leadingIcon = { Icon(Icons.Default.ViewSidebar, contentDescription = null) },
                                    onClick = {
                                        onDismiss()
                                        showThumbnailStrip = !showThumbnailStrip
                                    },
                                    modifier = Modifier.testTag("menu_toggle_thumbs")
                                )
                                DropdownMenuItem(
                                    text = { Text("Save Presentation") },
                                    leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                                    onClick = {
                                        onDismiss()
                                        Toast.makeText(context, "Presentation saved", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("menu_save")
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        onDismiss()
                                        viewModel.repository.shareDocument(context, liveDocument)
                                    },
                                    modifier = Modifier.testTag("menu_share")
                                )
                                DropdownMenuItem(
                                    text = { Text("Print") },
                                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                    onClick = {
                                        onDismiss()
                                        viewModel.repository.printDocument(context, liveDocument)
                                    },
                                    modifier = Modifier.testTag("menu_print")
                                )
                                DropdownMenuItem(
                                    text = { Text("File details") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    onClick = {
                                        onDismiss()
                                        showInfoDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_info")
                                )
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

                    // Floating Exit Fullscreen Pill
                    AnimatedVisibility(
                        visible = isFullscreen,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 16.dp, end = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            contentColor = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .clickable { isFullscreen = false }
                                .testTag("ppt_viewer_exit_fullscreen_pill")
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
                                Text(text = "Exit", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- OVERLAY DIALOGS ---

    // 1. Jump to Slide Dialog
    if (showJumpDialog && totalSlides > 0) {
        var pageInput by remember { mutableStateOf((pagerState.currentPage + 1).toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Go to slide", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Enter slide number (1 to $totalSlides):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().testTag("jump_slide_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFB923C),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = pageInput.toIntOrNull()
                        if (num != null && num in 1..totalSlides) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(num - 1)
                            }
                        }
                        showJumpDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    modifier = Modifier.testTag("jump_slide_confirm")
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("Cancel", color = Color(0xFF94A3B8)) }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // 2. Presentation Zoom Bottom Sheet
    if (showZoomDialog) {
        ModalBottomSheet(
            onDismissRequest = { showZoomDialog = false },
            containerColor = Color(0xFF0F172A),
            scrimColor = Color.Black.copy(alpha = 0.55f)
        ) {
            PptZoomDialog(
                currentZoom = zoomLevel,
                onZoomChange = { setZoom(it) },
                onFitSlide = { resetZoom() },
                onFitWidth = { setZoom(1.35f) },
                onDismiss = { showZoomDialog = false }
            )
        }
    }

    // 3. Speaker Notes Dialog
    if (showNotesDialog && presentation != null) {
        val currentSlide = presentation!!.slides.getOrNull(pagerState.currentPage)
        if (currentSlide != null) {
            ModalBottomSheet(
                onDismissRequest = { showNotesDialog = false },
                containerColor = Color(0xFF0F172A),
                scrimColor = Color.Black.copy(alpha = 0.55f)
            ) {
                PptNotesDialog(
                    slideNumber = pagerState.currentPage + 1,
                    slideTitle = currentSlide.title,
                    initialNotes = currentSlide.notes,
                    onSaveNotes = { newNotes ->
                        presentation = presentation?.updateNotes(pagerState.currentPage, newNotes)
                    },
                    onDismiss = { showNotesDialog = false }
                )
            }
        }
    }

    // 4. New Slide Dialog
    if (showNewSlideDialog) {
        PptNewSlideDialog(
            onConfirm = { title, content, _ ->
                presentation = presentation?.addSlide(title, content, pagerState.currentPage + 1)
                thumbnailBitmapCache.clear()
                slideBitmapCache.clear()
                showNewSlideDialog = false
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            },
            onDismiss = { showNewSlideDialog = false }
        )
    }

    // 5. Unsaved Changes Warning Dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Save changes?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("Save changes to \"${liveDocument.title}\" before closing?", color = Color(0xFFE2E8F0)) },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        Toast.makeText(context, "Presentation saved", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showUnsavedDialog = false
                        onBack()
                    }) {
                        Text("Don't Save", color = Color(0xFFEF4444))
                    }
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // 6. Rename Dialog
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

    // 7. File Info Dialog
    if (showInfoDialog) {
        FileInfoDialog(
            document = liveDocument,
            onDismiss = { showInfoDialog = false }
        )
    }

    // 8. Delete Dialog
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
}
