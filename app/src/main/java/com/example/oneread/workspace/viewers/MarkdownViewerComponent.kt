package com.example.oneread.workspace.viewers

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun MarkdownViewerComponent(
    tab: DocumentTab,
    onUpdateTab: (DocumentTab) -> Unit,
    onSaveContent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var markdownText by remember { mutableStateOf(tab.contentText ?: "") }
    var isPreviewMode by remember { mutableStateOf(true) }

    val rawScrollState = rememberScrollState()

    LaunchedEffect(tab.id) {
        if (tab.contentText != null) {
            markdownText = tab.contentText
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
                        val cacheFile = File(context.cacheDir, "md_tab_${tab.id}.md")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        }
                        targetFile = cacheFile
                    }
                }

                if (targetFile != null && targetFile.exists()) {
                    markdownText = targetFile.readText()
                } else {
                    markdownText = "# ${tab.title}\n\n" +
                            "A modern Markdown viewer and editor in the multi-tab workspace.\n\n" +
                            "## Features\n\n" +
                            "- Multi-level headings (# H1, ## H2, ### H3)\n" +
                            "- Bullet lists & numbered lists\n" +
                            "- **Bold text**, *italics*, and `inline code`\n" +
                            "- Blockquotes and code blocks\n" +
                            "- Seamless live editing and saving\n\n" +
                            "> \"Simplicity is the soul of efficiency.\"\n\n" +
                            "```kotlin\n" +
                            "fun main() {\n" +
                            "    println(\"Multi-tab workspace ready!\")\n" +
                            "}\n" +
                            "```\n"
                }
            } catch (e: Exception) {
                markdownText = "# Error loading document\n\n${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        DocumentViewerToolbar(
            tab = tab,
            onSave = {
                onSaveContent(markdownText)
            },
            canSave = true,
            customLeadingContent = {
                Surface(
                    color = Color(0xFF9333EA).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (isPreviewMode) "Preview Mode" else "Raw Editor",
                        color = Color(0xFFC084FC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            },
            customTrailingContent = {
                IconButton(
                    onClick = { isPreviewMode = !isPreviewMode },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPreviewMode) Icons.Default.Code else Icons.Default.Visibility,
                        contentDescription = if (isPreviewMode) "Edit Raw" else "Preview",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        if (isLoading) {
            DocumentLoadingState("Rendering Markdown…")
        } else if (isPreviewMode) {
            // Rendered Markdown Page
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(6.dp)),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp)
                        ) {
                            RenderMarkdownBlocks(markdownText)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else {
            // Raw Editor Mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0B101D))
                    .verticalScroll(rawScrollState)
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = markdownText,
                    onValueChange = { newText ->
                        markdownText = newText
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
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(Color(0xFF38BDF8))
                )
            }
        }
    }
}

@Composable
private fun RenderMarkdownBlocks(content: String) {
    val lines = content.lines()
    var inCodeBlock = false
    val codeBlockLines = mutableListOf<String>()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                // Render finished code block
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = codeBlockLines.joinToString("\n"),
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                codeBlockLines.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            codeBlockLines.add(line)
            continue
        }

        val trimmed = line.trim()
        when {
            trimmed.startsWith("# ") -> {
                Text(
                    text = trimmed.removePrefix("# ").trim(),
                    color = Color(0xFFF8FAFC),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
            }
            trimmed.startsWith("## ") -> {
                Text(
                    text = trimmed.removePrefix("## ").trim(),
                    color = Color(0xFFF1F5F9),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                )
                HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(bottom = 8.dp))
            }
            trimmed.startsWith("### ") -> {
                Text(
                    text = trimmed.removePrefix("### ").trim(),
                    color = Color(0xFFE2E8F0),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
            trimmed.startsWith("> ") -> {
                // Blockquote
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .background(Color(0xFF9333EA), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = trimmed.removePrefix("> ").trim(),
                        color = Color(0xFFCBD5E1),
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp
                    )
                }
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                    Text("•", color = Color(0xFF9333EA), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    Text(formatInlineMarkdown(trimmed.substring(2)), color = Color(0xFFCBD5E1), fontSize = 14.sp)
                }
            }
            trimmed.matches(Regex("^\\d+\\..*")) -> {
                val num = trimmed.substringBefore('.')
                val rest = trimmed.substringAfter('.').trim()
                Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                    Text("$num.", color = Color(0xFF9333EA), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    Text(formatInlineMarkdown(rest), color = Color(0xFFCBD5E1), fontSize = 14.sp)
                }
            }
            trimmed.startsWith("---") || trimmed.startsWith("***") -> {
                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
            }
            trimmed.isNotBlank() -> {
                Text(
                    text = formatInlineMarkdown(trimmed),
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            else -> {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

private fun formatInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            background = Color(0xFF0F172A),
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                    continue
                }
            }
            append(text[i])
            i++
        }
    }
}
