package com.example.oneread.word.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.data.DocumentItem
import com.example.oneread.word.model.DocxBlock
import com.example.oneread.word.model.DocxDocument
import com.example.oneread.word.model.DocxRun
import com.example.oneread.word.model.DocxTable
import com.example.oneread.word.model.DocxTableCell
import com.example.oneread.word.model.DocxTableRow
import com.example.oneread.word.parser.DocxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Rich Word Document Renderer for HR Read.
 * Renders DOCX with full fidelity:
 * - University header and logo images
 * - Real multi-column structured tables with Bloom's taxonomy, CO, and marks
 * - Headings, margins, alignments, fonts, bold and italic formatting
 * - Pinch-to-zoom and pan via visual graphics transform that never reflows or scrambles tables
 * - High-contrast search match highlighting
 */
@Composable
fun WordDocumentRenderer(
    document: DocumentItem,
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    currentMatchIndex: Int = -1,
    onMatchIndexChange: (Int) -> Unit = {},
    onMatchCountChange: (Int) -> Unit = {},
    registerSearchNavigators: (onNext: () -> Unit, onPrev: () -> Unit) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var parsedDoc by remember { mutableStateOf<DocxDocument?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()

    LaunchedEffect(document.path, document.uri) {
        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                val file = File(document.path)
                if (file.exists() && file.canRead()) {
                    val result = DocxParser.parse(file)
                    parsedDoc = result
                } else {
                    errorMessage = "Unable to read document file."
                }
            } catch (e: Exception) {
                errorMessage = "Error opening Word document: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Dynamic search matches count across document
    val matchCount = remember(parsedDoc, searchQuery) {
        if (searchQuery.isBlank() || parsedDoc == null) 0
        else {
            val q = searchQuery.lowercase()
            var count = 0
            fun countMatches(text: String) {
                var idx = 0
                val lower = text.lowercase()
                while (idx < lower.length) {
                    val found = lower.indexOf(q, idx)
                    if (found == -1) break
                    count++
                    idx = found + q.length
                }
            }
            parsedDoc?.allBlocks?.forEach { block ->
                when (block) {
                    is DocxBlock.Paragraph -> countMatches(block.fullText)
                    is DocxBlock.Table -> countMatches(block.allText)
                    else -> {}
                }
            }
            count
        }
    }

    LaunchedEffect(matchCount) {
        onMatchCountChange(matchCount)
        if (matchCount > 0 && currentMatchIndex !in 0 until matchCount) {
            onMatchIndexChange(0)
        } else if (matchCount == 0) {
            onMatchIndexChange(-1)
        }
    }

    LaunchedEffect(matchCount, currentMatchIndex) {
        registerSearchNavigators(
            {
                if (matchCount > 0) {
                    val next = (currentMatchIndex + 1) % matchCount
                    onMatchIndexChange(next)
                }
            },
            {
                if (matchCount > 0) {
                    val prev = if (currentMatchIndex <= 0) matchCount - 1 else currentMatchIndex - 1
                    onMatchIndexChange(prev)
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = modifier.background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF3B82F6), strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading Word document...",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    if (parsedDoc == null || errorMessage != null) {
        Box(
            modifier = modifier.background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage ?: "Could not render Word document.",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val doc = parsedDoc!!

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(0.75f, 3.5f)
        scale = newScale
        if (newScale <= 1.05f) {
            offsetX = 0f
            offsetY = 0f
        } else {
            offsetX += offsetChange.x
            offsetY += offsetChange.y
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF0F172A))
            .fillMaxSize()
    ) {
        // Main Document Area with Pinch to Zoom and Pan
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 1.6f
                            }
                        }
                    )
                }
                .transformable(state = transformableState)
        ) {
            val maxAvailableWidth = maxWidth

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // White Document Page Card (Word style)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .shadow(8.dp, RoundedCornerShape(4.dp)),
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 28.dp)
                        ) {
                            // 1. Header Elements (University logo, affiliation, assignment header)
                            if (doc.headerElements.isNotEmpty()) {
                                doc.headerElements.forEach { block ->
                                    RenderDocxBlock(block = block, searchQuery = searchQuery)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(
                                    color = Color(0xFFCBD5E1),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }

                            // 2. Body Elements (Title, paragraphs, question table, Bloom's level, signatures)
                            doc.bodyElements.forEach { block ->
                                RenderDocxBlock(block = block, searchQuery = searchQuery)
                            }

                            // 3. Footer Elements
                            if (doc.footerElements.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(
                                    color = Color(0xFFE2E8F0),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                doc.footerElements.forEach { block ->
                                    RenderDocxBlock(block = block, searchQuery = searchQuery)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Floating Quick Zoom Controller on bottom right
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shadowElevation = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = {
                        scale = (scale - 0.25f).coerceAtLeast(0.75f)
                        if (scale <= 1.05f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = {
                        scale = (scale + 0.25f).coerceAtMost(3.0f)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (scale != 1f) {
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Zoom",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders an individual DocxBlock: Paragraph, Table, Image, or Divider.
 */
@Composable
private fun RenderDocxBlock(
    block: DocxBlock,
    searchQuery: String,
    isHeader: Boolean = false
) {
    when (block) {
        is DocxBlock.Paragraph -> {
            RenderParagraph(block = block, searchQuery = searchQuery, isHeader = isHeader)
        }
        is DocxBlock.Table -> {
            RenderTable(table = block, searchQuery = searchQuery)
        }
        is DocxBlock.ImageBlock -> {
            RenderImageBlock(imageBlock = block)
        }
        is DocxBlock.HorizontalDivider -> {
            HorizontalDivider(
                color = block.color,
                thickness = block.thicknessDp.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

/**
 * Formats and renders a paragraph with runs, alignments, and headings.
 */
@Composable
private fun RenderParagraph(
    block: DocxBlock.Paragraph,
    searchQuery: String,
    isHeader: Boolean = false
) {
    val fullText = block.fullText
    if (fullText.isBlank() && block.runs.none { it.inlineImage != null }) {
        Spacer(modifier = Modifier.height(8.dp))
        return
    }

    val annotatedString = buildAnnotatedString {
        block.runs.forEach { run ->
            val runText = run.text
            if (runText.isNotEmpty()) {
                val hasMatch = searchQuery.isNotBlank() && runText.contains(searchQuery, ignoreCase = true)
                if (hasMatch) {
                    var currentIdx = 0
                    val lowerText = runText.lowercase()
                    val lowerQuery = searchQuery.lowercase()
                    while (currentIdx < runText.length) {
                        val matchIdx = lowerText.indexOf(lowerQuery, currentIdx)
                        if (matchIdx == -1) {
                            withStyle(
                                SpanStyle(
                                    fontWeight = if (run.isBold || block.isHeading || isHeader) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                                    textDecoration = if (run.isUnderline) TextDecoration.Underline else TextDecoration.None,
                                    fontSize = (if (block.isHeading) run.fontSizeSp + 3f else run.fontSizeSp).sp,
                                    color = run.color ?: if (block.isHeading) Color(0xFF0F172A) else Color(0xFF1E293B)
                                )
                            ) {
                                append(runText.substring(currentIdx))
                            }
                            break
                        }
                        if (matchIdx > currentIdx) {
                            withStyle(
                                SpanStyle(
                                    fontWeight = if (run.isBold || block.isHeading || isHeader) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                                    textDecoration = if (run.isUnderline) TextDecoration.Underline else TextDecoration.None,
                                    fontSize = (if (block.isHeading) run.fontSizeSp + 3f else run.fontSizeSp).sp,
                                    color = run.color ?: if (block.isHeading) Color(0xFF0F172A) else Color(0xFF1E293B)
                                )
                            ) {
                                append(runText.substring(currentIdx, matchIdx))
                            }
                        }
                        withStyle(
                            SpanStyle(
                                background = Color(0xFFFDE047),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = (if (block.isHeading) run.fontSizeSp + 3f else run.fontSizeSp).sp
                            )
                        ) {
                            append(runText.substring(matchIdx, matchIdx + searchQuery.length))
                        }
                        currentIdx = matchIdx + searchQuery.length
                    }
                } else {
                    withStyle(
                        SpanStyle(
                            fontWeight = if (run.isBold || block.isHeading || isHeader) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = if (run.isUnderline) TextDecoration.Underline else TextDecoration.None,
                            fontSize = (if (block.isHeading) run.fontSizeSp + 3f else run.fontSizeSp).sp,
                            color = run.color ?: if (block.isHeading) Color(0xFF0F172A) else Color(0xFF1E293B)
                        )
                    ) {
                        append(runText)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = block.spaceBeforeDp.dp,
                bottom = block.spaceAfterDp.dp,
                start = block.indentStartDp.dp
            )
    ) {
        // If run has inline image (e.g. logo or signature)
        block.runs.forEach { run ->
            if (run.inlineImage != null) {
                Image(
                    bitmap = run.inlineImage.asImageBitmap(),
                    contentDescription = "Document Image",
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }

        if (annotatedString.isNotEmpty()) {
            Text(
                text = annotatedString,
                textAlign = block.alignment,
                lineHeight = (if (block.isHeading) 26.sp else 22.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (block.hasBottomBorder) {
            HorizontalDivider(
                color = Color(0xFFCBD5E1),
                thickness = 1.dp,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }
    }
}

/**
 * Formats and renders a structured Word Table with borders, headers, and column ratios.
 */
@Composable
private fun RenderTable(
    table: DocxTable,
    searchQuery: String
) {
    if (table.rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .border(BorderStroke(1.dp, table.borderColor), RoundedCornerShape(2.dp))
    ) {
        table.rows.forEachIndexed { rowIndex, row ->
            val isHeaderRow = row.isHeaderRow || rowIndex == 0
            val rowBg = if (isHeaderRow) Color(0xFFF1F5F9) else Color.White

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg)
            ) {
                row.cells.forEachIndexed { colIndex, cell ->
                    val weight = table.colWidthWeights.getOrNull(colIndex) ?: 1f
                    val cellBg = cell.bgColor ?: rowBg

                    Box(
                        modifier = Modifier
                            .weight(weight.coerceAtLeast(0.1f))
                            .border(BorderStroke(0.5.dp, table.borderColor))
                            .background(cellBg)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            cell.blocks.forEach { block ->
                                RenderDocxBlock(
                                    block = block,
                                    searchQuery = searchQuery,
                                    isHeader = isHeaderRow
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders embedded media (university logo, diagrams, illustrations).
 */
@Composable
private fun RenderImageBlock(imageBlock: DocxBlock.ImageBlock) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = when (imageBlock.alignment) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Image(
            bitmap = imageBlock.bitmap.asImageBitmap(),
            contentDescription = imageBlock.description,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .heightIn(max = 160.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit
        )
    }
}
