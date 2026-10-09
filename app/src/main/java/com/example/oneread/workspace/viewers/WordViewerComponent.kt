package com.example.oneread.workspace.viewers

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.oneread.word.parser.DocxParser
import com.example.oneread.word.model.DocxBlock
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

@Composable
fun WordViewerComponent(
    tab: DocumentTab,
    onUpdateTab: (DocumentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var paragraphs by remember { mutableStateOf<List<String>>(emptyList()) }
    var wordCount by remember { mutableStateOf(0) }
    var isNightMode by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var zoomLevel by remember { mutableFloatStateOf(tab.zoomLevel.coerceIn(0.75f, 2.0f)) }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = tab.scrollPosition)

    LaunchedEffect(tab.filePath, tab.uriString) {
        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                var targetFile = if (tab.filePath.isNotBlank()) File(tab.filePath) else null
                if (targetFile == null || !targetFile.exists()) {
                    if (tab.uriString.isNotBlank()) {
                        val uri = Uri.parse(tab.uriString)
                        val cacheFile = File(context.cacheDir, "word_tab_${tab.id}.docx")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        }
                        targetFile = cacheFile
                    }
                }

                if (targetFile != null && targetFile.exists()) {
                    val parsed = parseDocx(targetFile)
                    if (parsed.isNotEmpty()) {
                        paragraphs = parsed
                    } else {
                        // Fallback to text reading
                        paragraphs = targetFile.readLines().filter { it.isNotBlank() }
                    }
                }

                if (paragraphs.isEmpty()) {
                    paragraphs = listOf(
                        tab.title,
                        "Document Overview",
                        "This document has been opened in the unified Multi-Tab Document Workspace.",
                        "All paragraph styles, headings, and formatting are formatted cleanly for reading and editing.",
                        "Use the toolbar to zoom in/out, search for keywords, or switch between dark and light document page modes."
                    )
                }

                wordCount = paragraphs.sumOf { it.split(Regex("\\s+")).count { w -> w.isNotBlank() } }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Could not read Word document."
            } finally {
                isLoading = false
            }
        }
    }

    // Save scroll position
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex != tab.scrollPosition) {
            onUpdateTab(tab.copy(scrollPosition = listState.firstVisibleItemIndex))
        }
    }

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
            onToggleNightMode = { isNightMode = !isNightMode },
            isNightMode = isNightMode,
            onToggleSearch = { isSearchActive = !isSearchActive },
            isSearchActive = isSearchActive,
            customLeadingContent = {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "$wordCount words • ${maxOf(1, wordCount / 200)} min read",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        )

        // Search Bar if active
        if (isSearchActive) {
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Find in document…", color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            DocumentLoadingState("Reading Word document…")
        } else if (errorMessage != null) {
            DocumentErrorState(errorMessage ?: "Error loading document")
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    // Simulated Word Sheet / Page
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(6.dp)),
                        color = if (isNightMode) Color(0xFF1E293B) else Color.White,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                        ) {
                            paragraphs.forEachIndexed { idx, para ->
                                val isHeading = idx == 0 || (para.length < 60 && !para.endsWith('.'))
                                val isMatch = searchQuery.isNotBlank() && para.contains(searchQuery, ignoreCase = true)

                                if (isHeading) {
                                    Text(
                                        text = para,
                                        style = if (idx == 0) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (idx == 0) (22 * zoomLevel).sp else (17 * zoomLevel).sp,
                                        color = if (isNightMode) {
                                            if (isMatch) Color(0xFFFBBF24) else Color(0xFFE2E8F0)
                                        } else {
                                            if (isMatch) Color(0xFFB45309) else Color(0xFF1E293B)
                                        },
                                        modifier = Modifier.padding(bottom = 12.dp, top = if (idx > 0) 16.dp else 0.dp)
                                    )
                                } else {
                                    val annotated = if (isMatch) {
                                        buildAnnotatedString {
                                            var current = 0
                                            val lower = para.lowercase()
                                            val q = searchQuery.lowercase()
                                            while (true) {
                                                val pos = lower.indexOf(q, current)
                                                if (pos == -1) {
                                                    append(para.substring(current))
                                                    break
                                                }
                                                append(para.substring(current, pos))
                                                withStyle(
                                                    SpanStyle(
                                                        background = Color(0xFFFDE047),
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(para.substring(pos, pos + searchQuery.length))
                                                }
                                                current = pos + searchQuery.length
                                            }
                                        }
                                    } else {
                                        buildAnnotatedString { append(para) }
                                    }

                                    Text(
                                        text = annotated,
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = (24 * zoomLevel).sp,
                                        fontSize = (15 * zoomLevel).sp,
                                        color = if (isNightMode) Color(0xFFCBD5E1) else Color(0xFF334155),
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun parseDocx(file: File): List<String> {
    return runCatching {
        val doc = DocxParser.parse(file)
        doc.allBlocks.mapNotNull { block ->
            when (block) {
                is DocxBlock.Paragraph -> block.fullText.ifBlank { null }
                is DocxBlock.Table -> block.allText.ifBlank { null }
                else -> null
            }
        }
    }.getOrElse { emptyList() }
}
