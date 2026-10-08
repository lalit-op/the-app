package com.example.oneread.workspace.viewers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfViewerComponent(
    tab: DocumentTab,
    onUpdateTab: (DocumentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(tab.totalPages.coerceAtLeast(1)) }
    var currentPage by remember { mutableIntStateOf(tab.currentPage.coerceAtLeast(1)) }
    var zoomLevel by remember { mutableFloatStateOf(tab.zoomLevel.coerceIn(0.5f, 3.0f)) }
    var isNightMode by remember { mutableStateOf(false) }
    var showThumbnails by remember { mutableStateOf(false) }

    // File descriptor holder for cleanup
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // Page bitmaps cache (weak or simple memory cache)
    val pageBitmaps = remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (tab.currentPage - 1).coerceAtLeast(0))

    // Negative matrix for night mode
    val nightModeColorFilter = remember {
        val matrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        ColorFilter.colorMatrix(matrix)
    }

    LaunchedEffect(tab.filePath, tab.uriString) {
        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                errorMessage = null

                var targetFile = if (tab.filePath.isNotBlank()) File(tab.filePath) else null
                if (targetFile == null || !targetFile.exists()) {
                    if (tab.uriString.isNotBlank()) {
                        val uri = Uri.parse(tab.uriString)
                        val cacheFile = File(context.cacheDir, "pdf_tab_${tab.id}.pdf")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        }
                        targetFile = cacheFile
                    }
                }

                if (targetFile != null && targetFile.exists()) {
                    val pfd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    fileDescriptor = pfd
                    val pdfRenderer = PdfRenderer(pfd)
                    renderer = pdfRenderer
                    pageCount = pdfRenderer.pageCount
                    onUpdateTab(tab.copy(totalPages = pdfRenderer.pageCount))

                    // Pre-render first 3 pages
                    val initialMap = mutableMapOf<Int, Bitmap>()
                    val pagesToRender = minOf(3, pdfRenderer.pageCount)
                    for (i in 0 until pagesToRender) {
                        pdfRenderer.openPage(i).use { page ->
                            val width = (page.width * 1.5).toInt().coerceAtLeast(100)
                            val height = (page.height * 1.5).toInt().coerceAtLeast(100)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            initialMap[i] = bitmap
                        }
                    }
                    pageBitmaps.value = initialMap
                } else {
                    errorMessage = "File does not exist or cannot be accessed."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load PDF document."
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(tab.id) {
        onDispose {
            renderer?.close()
            fileDescriptor?.close()
        }
    }

    // Function to load a specific page bitmap on demand
    fun requestPageBitmap(pageIndex: Int) {
        val r = renderer ?: return
        if (pageBitmaps.value.containsKey(pageIndex)) return
        scope.launch(Dispatchers.IO) {
            try {
                if (pageIndex in 0 until r.pageCount) {
                    r.openPage(pageIndex).use { page ->
                        val width = (page.width * 1.5).toInt().coerceAtLeast(100)
                        val height = (page.height * 1.5).toInt().coerceAtLeast(100)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pageBitmaps.value = pageBitmaps.value + (pageIndex to bitmap)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        // Toolbar
        DocumentViewerToolbar(
            tab = tab,
            onZoomIn = {
                zoomLevel = (zoomLevel + 0.25f).coerceAtMost(3.0f)
                onUpdateTab(tab.copy(zoomLevel = zoomLevel))
            },
            onZoomOut = {
                zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.5f)
                onUpdateTab(tab.copy(zoomLevel = zoomLevel))
            },
            onResetZoom = {
                zoomLevel = 1.0f
                onUpdateTab(tab.copy(zoomLevel = zoomLevel))
            },
            onToggleNightMode = { isNightMode = !isNightMode },
            isNightMode = isNightMode,
            customLeadingContent = {
                // Page Indicator
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Page $currentPage / $pageCount",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            customTrailingContent = {
                IconButton(
                    onClick = { showThumbnails = !showThumbnails },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewSidebar,
                        contentDescription = "Thumbnails",
                        tint = if (showThumbnails) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        if (isLoading) {
            DocumentLoadingState("Rendering PDF pages…")
        } else if (errorMessage != null) {
            DocumentErrorState(errorMessage ?: "Error loading PDF")
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zoomLevel = (zoomLevel * zoom).coerceIn(0.5f, 3.0f)
                            onUpdateTab(tab.copy(zoomLevel = zoomLevel))
                        }
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(pageCount) { pageIdx ->
                        val pageNum = pageIdx + 1
                        val bitmap = pageBitmaps.value[pageIdx]
                        if (bitmap == null) {
                            requestPageBitmap(pageIdx)
                        }

                        // Update current visible page in tab state
                        LaunchedEffect(listState.firstVisibleItemIndex) {
                            val visiblePage = listState.firstVisibleItemIndex + 1
                            if (visiblePage != currentPage) {
                                currentPage = visiblePage
                                onUpdateTab(tab.copy(currentPage = visiblePage))
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .shadow(6.dp, RoundedCornerShape(4.dp))
                                .graphicsLayer(
                                    scaleX = zoomLevel,
                                    scaleY = zoomLevel
                                ),
                            shape = RoundedCornerShape(4.dp),
                            color = if (isNightMode) Color(0xFF1E293B) else Color.White
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "PDF Page $pageNum",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.FillWidth,
                                    colorFilter = if (isNightMode) nightModeColorFilter else null
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(380.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Thumbnail strip at bottom if toggled
                if (showThumbnails) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(90.dp),
                        color = Color(0xFF0B0F19).copy(alpha = 0.95f),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (p in 0 until pageCount) {
                                val pNum = p + 1
                                val thumb = pageBitmaps.value[p]
                                Surface(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(width = 50.dp, height = 70.dp)
                                        .border(
                                            width = if (pNum == currentPage) 2.dp else 1.dp,
                                            color = if (pNum == currentPage) Color(0xFF38BDF8) else Color(0xFF334155),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            scope.launch {
                                                listState.animateScrollToItem(p)
                                            }
                                        },
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White
                                ) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb.asImageBitmap(),
                                            contentDescription = "Thumb $pNum",
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("$pNum", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
