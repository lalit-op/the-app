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
import com.example.oneread.word.ui.RenderDocxBlock
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
    var parsedDocument by remember { mutableStateOf<com.example.oneread.word.model.DocxDocument?>(null) }
    var wordCount by remember { mutableStateOf(0) }
    var isNightMode by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var zoomLevel by remember { mutableFloatStateOf(tab.zoomLevel.coerceIn(0.75f, 2.0f)) }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = tab.scrollPosition)

    LaunchedEffect(tab.filePath, tab.uriString, tab.originalFileName, tab.displayExtension) {
        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                errorMessage = null
                parsedDocument = null

                val uriName = tab.uriString.takeIf { it.isNotBlank() }
                    ?.let { Uri.parse(it).lastPathSegment }
                    .orEmpty()
                val sourceName = tab.originalFileName.ifBlank { uriName.ifBlank { tab.title } }
                val extension = sourceName.substringAfterLast('.', "")
                    .ifBlank { tab.displayExtension }
                    .lowercase()
                    .removePrefix(".")

                var targetFile = tab.filePath.takeIf { it.isNotBlank() }
                    ?.let(::File)
                    ?.takeIf { it.isFile && it.canRead() }

                if (targetFile == null && tab.uriString.isNotBlank()) {
                    val ext = extension.ifBlank { "docx" }
                    val cacheFile = File(context.cacheDir, "word_tab_${tab.id}.$ext")
                    val copied = context.contentResolver.openInputStream(Uri.parse(tab.uriString))?.use { input ->
                        FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        true
                    } ?: false
                    if (copied && cacheFile.isFile && cacheFile.length() > 0) {
                        targetFile = cacheFile
                    }
                }

                val file = targetFile ?: throw IllegalArgumentException("Cannot access the selected Word document.")
                val ext = file.extension.lowercase()
                if (ext !in setOf("docx", "docm", "dotx", "dotm", "rtf")) {
                    throw IllegalArgumentException("The workspace Word renderer supports DOCX and RTF. Convert legacy DOC/ODT files to DOCX or PDF.")
                }

                if (ext != "rtf") {
                    val validPackage = runCatching {
                        ZipFile(file).use { it.getEntry("word/document.xml") != null }
                    }.getOrDefault(false)
                    if (!validPackage) {
                        throw IllegalArgumentException("The file is not a valid Word OpenXML document.")
                    }
                }

                val parsed = DocxParser.parse(file)
                if (parsed.isEmpty) {
                    throw IllegalArgumentException("The Word document contains no readable content.")
                }
                parsedDocument = parsed
                val allText = parsed.allBlocks.joinToString(" ") { block ->
                    when (block) {
                        is DocxBlock.Paragraph -> block.fullText
                        is DocxBlock.Table -> block.allText
                        is DocxBlock.ImageBlock, is DocxBlock.HorizontalDivider -> ""
                    }
                }
                wordCount = allText.split(Regex("\\s+")).count { it.isNotBlank() }
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
        } else if (parsedDocument == null) {
            DocumentErrorState("The Word document could not be rendered.")
        } else {
            val doc = parsedDocument!!
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(6.dp)),
                        // Structured Word blocks use the source text and formatting colors;
                        // keep a white page to retain legibility and document contrast.
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                        ) {
                            doc.headerElements.forEach { block ->
                                RenderDocxBlock(block = block, searchQuery = searchQuery, isHeader = true)
                            }
                            if (doc.headerElements.isNotEmpty() && doc.bodyElements.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            doc.bodyElements.forEach { block ->
                                RenderDocxBlock(block = block, searchQuery = searchQuery)
                            }
                            if (doc.footerElements.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                doc.footerElements.forEach { block ->
                                    RenderDocxBlock(block = block, searchQuery = searchQuery)
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
