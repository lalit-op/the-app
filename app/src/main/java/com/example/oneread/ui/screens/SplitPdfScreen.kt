package com.example.oneread.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oneread.data.DocumentItem
import com.example.oneread.data.DocumentType
import com.example.oneread.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val allDocs by viewModel.allDocuments.collectAsState()
    val pdfDocs = remember(allDocs) { allDocs.filter { it.fileType == DocumentType.PDF } }

    var selectedDoc by remember { mutableStateOf<DocumentItem?>(pdfDocs.firstOrNull()) }
    val selectedPages = remember { mutableStateListOf<Int>() }
    var outputTitle by remember { mutableStateOf("Split_Pages_${System.currentTimeMillis() % 10000}") }
    var isSplitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Split PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("split_pdf_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            val doc = selectedDoc
                            if (doc != null && selectedPages.isNotEmpty() && outputTitle.isNotBlank()) {
                                isSplitting = true
                                viewModel.splitPdf(
                                    sourcePath = doc.path,
                                    pageIndices = selectedPages.toList(),
                                    title = outputTitle.trim()
                                ) { newDoc ->
                                    isSplitting = false
                                    viewModel.openDocument(newDoc)
                                }
                            }
                        },
                        enabled = selectedDoc != null && selectedPages.isNotEmpty() && outputTitle.isNotBlank() && !isSplitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_split_button")
                    ) {
                        if (isSplitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Extracting Pages...")
                        } else {
                            Icon(Icons.Default.CallSplit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extract ${selectedPages.size} Page(s)")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = outputTitle,
                onValueChange = { outputTitle = it },
                label = { Text("Output PDF Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "1. Choose Source Document:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(pdfDocs, key = { it.id }) { doc ->
                    val isSelected = selectedDoc?.id == doc.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDoc = doc
                                selectedPages.clear()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = doc.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${doc.pageCount} pgs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "2. Select Pages to Extract (${selectedPages.size} selected):",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val doc = selectedDoc
            if (doc != null) {
                val total = doc.pageCount.coerceAtLeast(1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedPages.clear()
                            for (i in 0 until total) selectedPages.add(i)
                        }
                    ) {
                        Text("Select All")
                    }
                    Button(
                        onClick = { selectedPages.clear() }
                    ) {
                        Text("Clear")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.55f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(total) { index ->
                        val isChecked = selectedPages.contains(index)
                        FilterChip(
                            selected = isChecked,
                            onClick = {
                                if (isChecked) selectedPages.remove(index) else selectedPages.add(index)
                            },
                            label = { Text("Page ${index + 1}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(0.55f), contentAlignment = Alignment.Center) {
                    Text("Select a PDF document above to choose pages.")
                }
            }
        }
    }
}
