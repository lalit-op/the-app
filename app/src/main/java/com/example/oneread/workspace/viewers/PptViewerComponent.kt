package com.example.oneread.workspace.viewers

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.ppt.model.PresentationModel
import com.example.oneread.ppt.parser.LegacyPptParser
import com.example.oneread.ppt.parser.PptxParser
import com.example.oneread.ppt.render.SlideRasterizer
import com.example.oneread.workspace.model.DocumentTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PptViewerComponent(
    tab: DocumentTab,
    onUpdateTab: (DocumentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var presentationModel by remember { mutableStateOf<PresentationModel?>(null) }
    var slideCount by remember { mutableIntStateOf(tab.totalPages.coerceAtLeast(1)) }
    var currentSlideIndex by remember { mutableIntStateOf(tab.currentPage.coerceAtLeast(1) - 1) }
    var showThumbnailStrip by remember { mutableStateOf(true) }

    // Rendered slide bitmaps cache
    val slideBitmaps = remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }

    LaunchedEffect(tab.filePath, tab.uriString) {
        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                var targetFile = if (tab.filePath.isNotBlank()) File(tab.filePath) else null
                if (targetFile == null || !targetFile.exists()) {
                    if (tab.uriString.isNotBlank()) {
                        val uri = Uri.parse(tab.uriString)
                        val cacheFile = File(context.cacheDir, "ppt_tab_${tab.id}.pptx")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        }
                        targetFile = cacheFile
                    }
                }

                if (targetFile != null && targetFile.exists()) {
                    val ext = targetFile.extension.lowercase()
                    val model = if (ext == "ppt") {
                        LegacyPptParser.parse(targetFile)
                    } else {
                        PptxParser.parse(targetFile)
                    }
                    presentationModel = model
                    val count = maxOf(1, model.slides.size)
                    slideCount = count
                    currentSlideIndex = currentSlideIndex.coerceIn(0, count - 1)
                    onUpdateTab(tab.copy(totalPages = count))

                    // Pre-render current slide
                    val targetH = (1280 / model.aspectRatio).toInt()
                    val bmp = SlideRasterizer.renderSlide(model.slides[currentSlideIndex], 1280, targetH)
                    slideBitmaps.value = mapOf(currentSlideIndex to bmp)
                } else {
                    errorMessage = "File not found"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Could not parse presentation."
            } finally {
                isLoading = false
            }
        }
    }

    // Function to render slide on demand
    fun renderSlide(index: Int) {
        val model = presentationModel ?: return
        if (slideBitmaps.value.containsKey(index)) return
        scope.launch(Dispatchers.IO) {
            try {
                if (index in model.slides.indices) {
                    val targetH = (1280 / model.aspectRatio).toInt()
                    val bmp = SlideRasterizer.renderSlide(model.slides[index], 1280, targetH)
                    slideBitmaps.value = slideBitmaps.value + (index to bmp)
                }
            } catch (_: Exception) { }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        DocumentViewerToolbar(
            tab = tab,
            customLeadingContent = {
                Surface(
                    color = Color(0xFFEA580C).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Slide ${currentSlideIndex + 1} / $slideCount",
                        color = Color(0xFFFB923C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            },
            customTrailingContent = {
                // Prev slide
                IconButton(
                    onClick = {
                        if (currentSlideIndex > 0) {
                            currentSlideIndex--
                            onUpdateTab(tab.copy(currentPage = currentSlideIndex + 1))
                            renderSlide(currentSlideIndex)
                        }
                    },
                    enabled = currentSlideIndex > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Prev Slide",
                        tint = if (currentSlideIndex > 0) Color(0xFFE2E8F0) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Next slide
                IconButton(
                    onClick = {
                        if (currentSlideIndex < slideCount - 1) {
                            currentSlideIndex++
                            onUpdateTab(tab.copy(currentPage = currentSlideIndex + 1))
                            renderSlide(currentSlideIndex)
                        }
                    },
                    enabled = currentSlideIndex < slideCount - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Slide",
                        tint = if (currentSlideIndex < slideCount - 1) Color(0xFFE2E8F0) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { showThumbnailStrip = !showThumbnailStrip },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewSidebar,
                        contentDescription = "Thumbnails",
                        tint = if (showThumbnailStrip) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        if (isLoading) {
            DocumentLoadingState("Rasterizing presentation slides…")
        } else if (errorMessage != null) {
            DocumentErrorState(errorMessage ?: "Error loading PowerPoint")
        } else {
            // Main Slide Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentBitmap = slideBitmaps.value[currentSlideIndex]
                if (currentBitmap == null) {
                    renderSlide(currentSlideIndex)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(presentationModel?.aspectRatio ?: (16f / 9f))
                        .shadow(12.dp, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White
                ) {
                    if (currentBitmap != null) {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "Slide ${currentSlideIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFEA580C))
                        }
                    }
                }
            }

            // Thumbnail Strip at Bottom
            if (showThumbnailStrip) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    color = Color(0xFF13192B)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until slideCount) {
                            val isSelected = (i == currentSlideIndex)
                            val thumb = slideBitmaps.value[i]
                            if (thumb == null && (i in (currentSlideIndex - 2)..(currentSlideIndex + 2))) {
                                renderSlide(i)
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .clickable {
                                        currentSlideIndex = i
                                        onUpdateTab(tab.copy(currentPage = i + 1))
                                        renderSlide(i)
                                    }
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 45.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFFB923C) else Color(0xFF334155),
                                            shape = RoundedCornerShape(4.dp)
                                        ),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White
                                ) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb.asImageBitmap(),
                                            contentDescription = "Slide ${i + 1}",
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${i + 1}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                }
                                Text(
                                    text = "${i + 1}",
                                    color = if (isSelected) Color(0xFFFB923C) else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
