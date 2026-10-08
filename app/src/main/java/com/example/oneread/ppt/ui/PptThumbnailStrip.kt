package com.example.oneread.ppt.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.ppt.model.PresentationModel
import com.example.oneread.ppt.render.SlideRasterizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun PptThumbnailStrip(
    presentation: PresentationModel,
    currentSlideIndex: Int,
    thumbnailCache: MutableMap<Int, Bitmap>,
    onSelectSlide: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Auto-scroll strip to keep current slide in view
    LaunchedEffect(currentSlideIndex) {
        val approxItemWidth = 100 // item width in px approximate
        scrollState.animateScrollTo((currentSlideIndex * approxItemWidth).coerceAtLeast(0))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presentation.slides.forEachIndexed { idx, slide ->
                val isSelected = (idx == currentSlideIndex)
                var thumbBitmap by remember(idx, slide) { mutableStateOf<Bitmap?>(thumbnailCache[idx]) }

                LaunchedEffect(idx, slide) {
                    if (thumbBitmap == null && idx in (currentSlideIndex - 4)..(currentSlideIndex + 4)) {
                        withContext(Dispatchers.IO) {
                            val bmp = SlideRasterizer.renderSlide(
                                slide = slide,
                                targetWidth = 240,
                                targetHeight = (240 / presentation.aspectRatio).roundToInt()
                            )
                            thumbnailCache[idx] = bmp
                            withContext(Dispatchers.Main) {
                                thumbBitmap = bmp
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectSlide(idx) }
                        .testTag("ppt_thumb_item_$idx")
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 84.dp, height = (84 / presentation.aspectRatio).dp.coerceIn(46.dp, 60.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFB923C) else Color(0xFF334155),
                                shape = RoundedCornerShape(6.dp)
                            ),
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFEA580C))
                        }

                        // Subtle index indicator on corner
                        Surface(
                            shape = RoundedCornerShape(bottomEnd = 4.dp),
                            color = if (isSelected) Color(0xFFEA580C) else Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = "${idx + 1}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "${idx + 1}",
                        color = if (isSelected) Color(0xFFFB923C) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
