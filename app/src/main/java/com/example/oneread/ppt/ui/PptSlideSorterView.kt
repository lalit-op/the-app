package com.example.oneread.ppt.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.ppt.model.PresentationModel
import com.example.oneread.ppt.model.SlideModel
import com.example.oneread.ppt.render.SlideRasterizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PptSlideSorterView(
    presentation: PresentationModel,
    activeSlideIndex: Int,
    thumbnailCache: MutableMap<Int, Bitmap>,
    onSelectSlide: (Int) -> Unit,
    onMoveSlide: (from: Int, to: Int) -> Unit,
    onDuplicateSlide: (Int) -> Unit,
    onDeleteSlide: (Int) -> Unit,
    onAddNewSlide: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
    ) {
        // Slide Sorter Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("ppt_sorter_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit Slide Sorter",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Slide Sorter",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${presentation.slides.size} slides • Tap to open",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }

                // Add Slide Button
                Button(
                    onClick = onAddNewSlide,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).testTag("ppt_sorter_add_slide")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Slide", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Grid of Slide Thumbnails
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().testTag("ppt_slide_sorter_grid")
        ) {
            itemsIndexed(
                items = presentation.slides,
                key = { idx, slide -> "slide_${slide.slideNumber}_$idx" }
            ) { idx, slide ->
                val isSelected = (idx == activeSlideIndex)
                var thumbBitmap by remember(idx, slide) { mutableStateOf<Bitmap?>(thumbnailCache[idx]) }

                LaunchedEffect(idx, slide) {
                    if (thumbBitmap == null) {
                        withContext(Dispatchers.IO) {
                            val bmp = SlideRasterizer.renderSlide(
                                slide = slide,
                                targetWidth = 320,
                                targetHeight = (320 / presentation.aspectRatio).roundToInt()
                            )
                            thumbnailCache[idx] = bmp
                            withContext(Dispatchers.Main) {
                                thumbBitmap = bmp
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFFB923C)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .clickable { onSelectSlide(idx) }
                        .testTag("ppt_sorter_card_$idx")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Slide Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(presentation.aspectRatio)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (thumbBitmap != null) {
                                Image(
                                    bitmap = thumbBitmap!!.asImageBitmap(),
                                    contentDescription = "Slide ${idx + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFFEA580C))
                            }

                            // Slide Number Badge on top-left of thumbnail
                            Surface(
                                shape = RoundedCornerShape(bottomEnd = 6.dp),
                                color = if (isSelected) Color(0xFFEA580C) else Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Slide Details & Quick Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = slide.title.ifBlank { "Slide ${idx + 1}" },
                                color = if (isSelected) Color(0xFFFB923C) else Color(0xFFE2E8F0),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            )

                            // Move Left (Up)
                            if (idx > 0) {
                                IconButton(
                                    onClick = { onMoveSlide(idx, idx - 1) },
                                    modifier = Modifier.size(24.dp).testTag("ppt_sorter_move_up_$idx")
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Move Left", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                }
                            }

                            // Move Right (Down)
                            if (idx < presentation.slides.size - 1) {
                                IconButton(
                                    onClick = { onMoveSlide(idx, idx + 1) },
                                    modifier = Modifier.size(24.dp).testTag("ppt_sorter_move_down_$idx")
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Move Right", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                }
                            }

                            // Duplicate
                            IconButton(
                                onClick = { onDuplicateSlide(idx) },
                                modifier = Modifier.size(24.dp).testTag("ppt_sorter_duplicate_$idx")
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            }

                            // Delete (if more than 1 slide)
                            if (presentation.slides.size > 1) {
                                IconButton(
                                    onClick = { onDeleteSlide(idx) },
                                    modifier = Modifier.size(24.dp).testTag("ppt_sorter_delete_$idx")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
