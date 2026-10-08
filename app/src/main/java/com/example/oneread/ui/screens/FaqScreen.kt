package com.example.oneread.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FaqItem(val question: String, val answer: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    val faqs = listOf(
        FaqItem(
            "What document formats are supported by HR Read?",
            "HR Read supports a wide range of formats including PDF documents (.pdf), Microsoft Word & Rich Text (.doc, .docx, .rtf, .odt), Excel spreadsheets (.xls, .xlsx, .csv), PowerPoint presentations (.ppt, .pptx), Plain Text (.txt, .md, .log), Source Code (.json, .xml, .java, .kt, .html), and Images (.png, .jpg, .webp)."
        ),
        FaqItem(
            "How do I convert photos to a PDF?",
            "Navigate to the 'Tools' tab and select 'Image to PDF Converter' (or tap the camera shortcut on the Home screen). Select one or multiple pictures from your gallery, adjust the document title and page orientation, then tap 'Convert'. Your new PDF will be saved and opened immediately."
        ),
        FaqItem(
            "How do I use Night Reading / Invert Color mode?",
            "When viewing any PDF, tap the brightness/moon icon in the top toolbar to invert colors for a dark background that is easy on your eyes in low-light environments."
        ),
        FaqItem(
            "How do I switch between Continuous Scroll and Page-by-Page?",
            "In the PDF viewer toolbar, tap the carousel icon to switch between continuous vertical scrolling and single-page horizontal swipe mode."
        ),
        FaqItem(
            "Can I print documents directly from the app?",
            "Yes! Tap the three-dot menu on any document or the Print icon inside the viewer. One Read connects directly to the Android System Print Manager, allowing you to print to any WiFi printer or export as a formatted PDF."
        ),
        FaqItem(
            "Where are my documents stored? Is my data private?",
            "Your privacy is 100% protected. All files, conversions, merges, and viewing happen entirely locally on your device storage. No documents are uploaded to external servers or third-party networks."
        ),
        FaqItem(
            "How do I recover a deleted document?",
            "Open the 'Tools' or 'Settings' tab, tap 'Recycle Bin', and find your file. Tap the Restore icon to put it back into your active document library."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FAQs & Help Guide",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("faq_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Frequently Asked Questions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(faqs.indices.toList()) { index ->
                val faq = faqs[index]
                val isExpanded = expandedStates[index] ?: false

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expandedStates[index] = !isExpanded },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = faq.question,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = faq.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
