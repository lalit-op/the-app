package com.example.oneread.workspace.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.data.DocumentItem
import com.example.oneread.workspace.model.DocumentFormat

@Composable
fun NewTabScreen(
    recentDocuments: List<DocumentItem>,
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenFormat: (DocumentFormat) -> Unit,
    onPickFiles: () -> Unit,
    onOpenMultipleDemoTabs: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Header
            Text(
                text = "New Document Workspace",
                color = Color(0xFFF8FAFC),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Text(
                text = "Open, create, and manage multiple documents in independent tabs simultaneously",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Supported Format Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FormatActionCard(
                    title = "PDF",
                    subtitle = ".pdf",
                    icon = Icons.Default.PictureAsPdf,
                    badgeColor = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                ) { onOpenFormat(DocumentFormat.PDF) }

                FormatActionCard(
                    title = "Word",
                    subtitle = ".docx, .doc",
                    icon = Icons.Default.Description,
                    badgeColor = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                ) { onOpenFormat(DocumentFormat.WORD) }

                FormatActionCard(
                    title = "Excel",
                    subtitle = ".xlsx, .xls",
                    icon = Icons.Default.GridOn,
                    badgeColor = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                ) { onOpenFormat(DocumentFormat.EXCEL) }

                FormatActionCard(
                    title = "PowerPoint",
                    subtitle = ".pptx, .ppt",
                    icon = Icons.Default.Slideshow,
                    badgeColor = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f)
                ) { onOpenFormat(DocumentFormat.PPT) }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FormatActionCard(
                    title = "Text",
                    subtitle = ".txt, .log",
                    icon = Icons.Default.TextSnippet,
                    badgeColor = Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                ) { onOpenFormat(DocumentFormat.TEXT) }

                FormatActionCard(
                    title = "CSV",
                    subtitle = ".csv, .tsv",
                    icon = Icons.Default.TableChart,
                    badgeColor = Color(0xFF0D9488),
                    modifier = Modifier.weight(1f)
                ) { onOpenFormat(DocumentFormat.CSV) }

                FormatActionCard(
                    title = "Markdown",
                    subtitle = ".md",
                    icon = Icons.Default.Notes,
                    badgeColor = Color(0xFF9333EA),
                    modifier = Modifier.weight(1f)
                ) { onOpenFormat(DocumentFormat.MARKDOWN) }

                FormatActionCard(
                    title = "Image",
                    subtitle = ".png, .jpg",
                    icon = Icons.Default.Image,
                    badgeColor = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                ) { onOpenFormat(DocumentFormat.IMAGE) }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Drag & Drop / File Browser Drop Zone
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .clickable { onPickFiles() },
                color = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Drop files",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Drag & Drop Files Here or Tap to Browse",
                        color = Color(0xFFF1F5F9),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Supports PDF, DOCX, XLSX, PPTX, TXT, CSV, Markdown, and Images. Multiple files supported.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onPickFiles,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Browse Storage", fontSize = 13.sp)
                        }

                        Button(
                            onClick = onOpenMultipleDemoTabs,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Multi-Doc Suite (Demo)", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Files Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recent Documents",
                    color = Color(0xFFF8FAFC),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${recentDocuments.size} available",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(
                color = Color(0xFF1E293B),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        // Recent Documents List
        if (recentDocuments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent documents. Select a format above or browse files to open.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(recentDocuments.take(8)) { doc ->
                val format = DocumentFormat.fromDocumentType(doc.fileType, doc.extension)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOpenDocument(doc) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13192B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = format.badgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = format.getIcon(),
                                    contentDescription = null,
                                    tint = format.badgeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                color = Color(0xFFF1F5F9),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${doc.formattedSize} • ${doc.formattedDate}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Open in Tab",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF13192B),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                color = Color(0xFFF1F5F9),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}
