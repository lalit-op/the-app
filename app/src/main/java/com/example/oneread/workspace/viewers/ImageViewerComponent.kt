package com.example.oneread.workspace.viewers

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImageViewerComponent(
    tab: DocumentTab,
    onUpdateTab: (DocumentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var imageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var imageDimensions by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var zoomScale by remember { mutableFloatStateOf(tab.zoomLevel.coerceIn(0.5f, 5.0f)) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(tab.filePath, tab.uriString) {
        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                var targetFile = if (tab.filePath.isNotBlank()) File(tab.filePath) else null
                if (targetFile == null || !targetFile.exists()) {
                    if (tab.uriString.isNotBlank()) {
                        val uri = Uri.parse(tab.uriString)
                        val cacheFile = File(context.cacheDir, "img_tab_${tab.id}.png")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        }
                        targetFile = cacheFile
                    }
                }

                if (targetFile != null && targetFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(targetFile.absolutePath)
                    if (bmp != null) {
                        imageBitmap = bmp
                        imageDimensions = Pair(bmp.width, bmp.height)
                    } else {
                        errorMessage = "Could not decode image file."
                    }
                } else {
                    errorMessage = "Image file not found."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load image."
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF070B14))) {
        DocumentViewerToolbar(
            tab = tab,
            onZoomIn = {
                zoomScale = (zoomScale + 0.3f).coerceAtMost(5.0f)
                onUpdateTab(tab.copy(zoomLevel = zoomScale))
            },
            onZoomOut = {
                zoomScale = (zoomScale - 0.3f).coerceAtLeast(0.5f)
                onUpdateTab(tab.copy(zoomLevel = zoomScale))
            },
            onResetZoom = {
                zoomScale = 1.0f
                panOffset = Offset.Zero
                rotationDegrees = 0f
                onUpdateTab(tab.copy(zoomLevel = zoomScale))
            },
            customLeadingContent = {
                imageDimensions?.let { (w, h) ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${w} × ${h} px • ${(zoomScale * 100).toInt()}%",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            customTrailingContent = {
                IconButton(
                    onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        if (isLoading) {
            DocumentLoadingState("Loading high-res image…")
        } else if (errorMessage != null) {
            DocumentErrorState(errorMessage ?: "Error loading image")
        } else if (imageBitmap != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.5f, 5.0f)
                            panOffset += pan
                            onUpdateTab(tab.copy(zoomLevel = zoomScale))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = imageBitmap!!.asImageBitmap(),
                    contentDescription = tab.title,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = panOffset.x,
                            translationY = panOffset.y,
                            rotationZ = rotationDegrees
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
