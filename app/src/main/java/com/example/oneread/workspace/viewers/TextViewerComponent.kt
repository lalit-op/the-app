package com.example.oneread.workspace.viewers

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun TextViewerComponent(
    tab: DocumentTab,
    onUpdateTab: (DocumentTab) -> Unit,
    onSaveContent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var textContent by remember { mutableStateOf(tab.contentText ?: "") }
    var showLineNumbers by remember { mutableStateOf(true) }
    var isWordWrap by remember { mutableStateOf(true) }
    var zoomLevel by remember { mutableFloatStateOf(tab.zoomLevel.coerceIn(0.75f, 2.0f)) }

    val verticalScrollState = rememberScrollState(initial = tab.scrollPosition)
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(tab.id) {
        if (tab.contentText != null) {
            textContent = tab.contentText
            isLoading = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                var targetFile = if (tab.filePath.isNotBlank()) File(tab.filePath) else null
                if (targetFile == null || !targetFile.exists()) {
                    if (tab.uriString.isNotBlank()) {
                        val uri = Uri.parse(tab.uriString)
                        val cacheFile = File(context.cacheDir, "text_tab_${tab.id}.txt")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        }
                        targetFile = cacheFile
                    }
                }

                if (targetFile != null && targetFile.exists()) {
                    textContent = targetFile.readText()
                } else if (textContent.isBlank()) {
                    textContent = "Welcome to One Read Document Workspace.\nType text here or open any document from the top tab bar."
                }
            } catch (e: Exception) {
                textContent = "Error reading file: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Persist scroll position
    LaunchedEffect(verticalScrollState.value) {
        if (verticalScrollState.value != tab.scrollPosition) {
            onUpdateTab(tab.copy(scrollPosition = verticalScrollState.value))
        }
    }

    val lines = textContent.split('\n')
    val lineCount = lines.size
    val charCount = textContent.length

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        DocumentViewerToolbar(
            tab = tab,
            onZoomIn = {
                zoomLevel = (zoomLevel + 0.15f).coerceAtMost(2.0f)
                onUpdateTab(tab.copy(zoomLevel = zoomLevel))
            },
            onZoomOut = {
                zoomLevel = (zoomLevel - 0.15f).coerceAtLeast(0.75f)
                onUpdateTab(tab.copy(zoomLevel = zoomLevel))
            },
            onResetZoom = {
                zoomLevel = 1.0f
                onUpdateTab(tab.copy(zoomLevel = zoomLevel))
            },
            onSave = {
                onSaveContent(textContent)
            },
            canSave = true,
            customLeadingContent = {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "$lineCount lines • $charCount chars",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            },
            customTrailingContent = {
                IconButton(
                    onClick = { showLineNumbers = !showLineNumbers },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = "Line Numbers",
                        tint = if (showLineNumbers) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { isWordWrap = !isWordWrap },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.WrapText,
                        contentDescription = "Word Wrap",
                        tint = if (isWordWrap) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        if (isLoading) {
            DocumentLoadingState("Loading text content…")
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0B101D))
            ) {
                // Line Number Gutter
                if (showLineNumbers) {
                    Surface(
                        color = Color(0xFF13192B),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(verticalScrollState)
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..maxOf(1, lineCount)) {
                                Text(
                                    text = "$i",
                                    color = Color(0xFF475569),
                                    fontSize = (13 * zoomLevel).sp,
                                    lineHeight = (20 * zoomLevel).sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Text Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .then(if (!isWordWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
                        .verticalScroll(verticalScrollState)
                ) {
                    BasicTextField(
                        value = textContent,
                        onValueChange = { newText ->
                            textContent = newText
                            onUpdateTab(
                                tab.copy(
                                    contentText = newText,
                                    isModified = true
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = Color(0xFFF1F5F9),
                            fontSize = (13 * zoomLevel).sp,
                            lineHeight = (20 * zoomLevel).sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(Color(0xFF38BDF8))
                    )
                }
            }
        }
    }
}
