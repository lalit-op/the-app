package com.example.oneread.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oneread.data.DocumentItem
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.components.ConfirmDeleteDialog
import com.example.oneread.ui.components.DocumentCard
import com.example.oneread.ui.components.FileInfoDialog
import com.example.oneread.ui.components.RenameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(
    viewModel: MainViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    val context = LocalContext.current
    val recentDocs by viewModel.recentDocuments.collectAsState()

    var docToRename by remember { mutableStateOf<DocumentItem?>(null) }
    var docForInfo by remember { mutableStateOf<DocumentItem?>(null) }
    var docToDelete by remember { mutableStateOf<DocumentItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("recent_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openSearch() },
                        modifier = Modifier.testTag("recent_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (recentDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Recent Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Documents you open will appear here for fast access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "${recentDocs.size} recently opened",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(recentDocs, key = { "recent_${it.id}" }) { doc ->
                    DocumentCard(
                        document = doc,
                        modifier = Modifier.animateItem(),
                        onClick = { viewModel.openDocument(doc) },
                        onToggleFavorite = { viewModel.toggleFavorite(doc) },
                        onRename = { docToRename = doc },
                        onShare = { viewModel.repository.shareDocument(context, doc) },
                        onPrint = { viewModel.repository.printDocument(context, doc) },
                        onInfo = { docForInfo = doc },
                        onDelete = { docToDelete = doc }
                    )
                }
            }
        }
    }

    docToRename?.let { doc ->
        RenameDialog(
            document = doc,
            onDismiss = { docToRename = null },
            onConfirm = { newName ->
                viewModel.renameDocument(doc, newName)
                docToRename = null
            }
        )
    }

    docForInfo?.let { doc ->
        FileInfoDialog(
            document = doc,
            onDismiss = { docForInfo = null }
        )
    }

    docToDelete?.let { doc ->
        ConfirmDeleteDialog(
            title = "Move to Recycle Bin?",
            message = "Move '${doc.title}' to the recycle bin? You can restore it anytime.",
            confirmText = "Move to Trash",
            onDismiss = { docToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(doc)
                docToDelete = null
            }
        )
    }
}
