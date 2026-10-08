package com.example.oneread.workspace.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentFormat
import com.example.oneread.workspace.model.DocumentTab

/**
 * Compact bottom-sheet document switcher inspired by WPS Office.
 * Appears directly over the reader when tapping the document counter badge.
 * Does not navigate away from the current screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentSwitcherBottomSheet(
    openTabs: List<DocumentTab>,
    activeTabId: String?,
    closedHistory: List<DocumentTab>,
    onDismissRequest: () -> Unit,
    onSelectTab: (DocumentTab) -> Unit,
    onCloseTab: (String) -> Unit,
    onReopenClosedTab: (DocumentTab) -> Unit,
    onOpenNewDocument: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeight = screenHeight * 0.82f

    var showRecentlyClosed by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row: Title, Tab Count & Close Button [✕]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Open Documents",
                        color = Color(0xFFF8FAFC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${openTabs.size} ${if (openTabs.size == 1) "document" else "documents"} open",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("switcher_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close switcher",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Scrollable List of Open Documents (Lightweight items, NO heavy thumbnails)
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (openTabs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No open documents",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(openTabs, key = { it.id }) { tab ->
                        val isActive = (tab.id == activeTabId)
                        DocumentSwitcherRow(
                            tab = tab,
                            isActive = isActive,
                            onClick = { onSelectTab(tab) },
                            onClose = { onCloseTab(tab.id) }
                        )
                    }
                }

                // Action: "+ Open Document"
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onOpenNewDocument,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("switcher_open_document_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Document",
                            color = Color(0xFFF1F5F9),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Optional "Recently Closed" Section
                if (closedHistory.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showRecentlyClosed = !showRecentlyClosed }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recently Closed (${closedHistory.size})",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = if (showRecentlyClosed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showRecentlyClosed) {
                        items(closedHistory.take(5), key = { "closed_${it.id}" }) { tab ->
                            RecentlyClosedRow(
                                tab = tab,
                                onReopen = { onReopenClosedTab(tab) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Single document row in the switcher.
 * Shows file icon, truncated name, file type, current position, active indicator, and close button.
 */
@Composable
private fun DocumentSwitcherRow(
    tab: DocumentTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) Color(0xFF38BDF8) else Color(0xFF1E293B)
    val bgColor = if (isActive) Color(0xFF1E293B).copy(alpha = 0.9f) else Color(0xFF13192B)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("switcher_tab_${tab.id}"),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Type Badge Icon
            Surface(
                color = tab.fileType.badgeColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = tab.fileType.getIcon(),
                        contentDescription = tab.fileType.displayName,
                        tint = tab.fileType.badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Title & Position
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tab.title,
                        color = if (isActive) Color(0xFF38BDF8) else Color(0xFFF1F5F9),
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (tab.isModified) {
                        Text(
                            text = " ●",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Location info: e.g. "PDF • Page 1 of 7" or "XLSX • Sheet 2"
                Text(
                    text = "${tab.displayExtension} • ${tab.locationText}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Active Checkmark Indicator or Close Button
            if (isActive) {
                Surface(
                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(26.dp)
                        .padding(end = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active Document",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Close button [✕]
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("switcher_close_tab_${tab.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close ${tab.title}",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentlyClosedRow(
    tab: DocumentTab,
    onReopen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onReopen),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF13192B).copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = tab.fileType.getIcon(),
                    contentDescription = null,
                    tint = tab.fileType.badgeColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tab.title,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Reopen",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reopen",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
