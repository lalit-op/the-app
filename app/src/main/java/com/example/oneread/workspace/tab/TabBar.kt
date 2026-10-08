package com.example.oneread.workspace.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentTab
import kotlin.math.roundToInt

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TabBar(
    tabs: List<DocumentTab>,
    activeTabId: String?,
    canReopenClosed: Boolean,
    onTabSelected: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabCloseOthers: (String) -> Unit,
    onTabCloseToRight: (String) -> Unit,
    onTabCloseAll: () -> Unit,
    onTabDuplicate: (String) -> Unit,
    onTabTogglePin: (String) -> Unit,
    onTabReopenClosed: () -> Unit,
    onTabDoubleClicked: (String) -> Unit,
    onTabReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onTabInfo: (String) -> Unit,
    onNewTabClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var contextMenuTab by remember { mutableStateOf<DocumentTab?>(null) }
    var isOverflowOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        color = Color(0xFF090D18),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Far Left: Menu button ☰
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Workspace Menu / Library",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Scrollable Tab List + New Tab Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.Bottom
            ) {
                tabs.forEachIndexed { index, tab ->
                    SingleTabItem(
                        tab = tab,
                        isActive = (tab.id == activeTabId),
                        onSelect = { onTabSelected(tab.id) },
                        onClose = { onTabClose(tab.id) },
                        onDoubleClick = { onTabDoubleClicked(tab.id) },
                        onLongClick = { contextMenuTab = tab },
                        onDragEnd = { deltaX ->
                            // Reorder tab if dragged across threshold
                            val approxTabWidth = 150f
                            val offsetSteps = (deltaX / approxTabWidth).roundToInt()
                            if (offsetSteps != 0) {
                                val targetIndex = (index + offsetSteps).coerceIn(0, tabs.lastIndex)
                                if (targetIndex != index) {
                                    onTabReorder(index, targetIndex)
                                }
                            }
                        }
                    )
                }

                // "+" New Tab Button (Chrome / WPS style)
                IconButton(
                    onClick = onNewTabClick,
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 4.dp)
                        .size(32.dp)
                        .align(Alignment.CenterVertically)
                        .clip(CircleShape)
                        .background(Color(0xFF13192B))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Tab",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Tab Overflow dropdown button if tabs > 4
            if (tabs.size >= 4) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 4.dp)
                ) {
                    Surface(
                        color = Color(0xFF13192B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .combinedClickable(onClick = { isOverflowOpen = true })
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "${tabs.size} tabs",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Overflow Tabs",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isOverflowOpen,
                        onDismissRequest = { isOverflowOpen = false },
                        modifier = Modifier.background(Color(0xFF13192B), RoundedCornerShape(8.dp))
                    ) {
                        tabs.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = t.fileType.getIcon(),
                                            contentDescription = null,
                                            tint = t.fileType.badgeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = t.title,
                                            color = if (t.id == activeTabId) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                            fontWeight = if (t.id == activeTabId) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (t.isModified) {
                                            Text(" ●", color = Color(0xFFF59E0B), fontSize = 10.sp)
                                        }
                                    }
                                },
                                trailingIcon = {
                                    if (t.id == activeTabId) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    isOverflowOpen = false
                                    onTabSelected(t.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Context Menu for Tab
    contextMenuTab?.let { tab ->
        TabContextMenu(
            tab = tab,
            expanded = true,
            onDismissRequest = { contextMenuTab = null },
            onClose = { onTabClose(tab.id) },
            onCloseOthers = { onTabCloseOthers(tab.id) },
            onCloseTabsToRight = { onTabCloseToRight(tab.id) },
            onCloseAll = onTabCloseAll,
            onDuplicate = { onTabDuplicate(tab.id) },
            onTogglePin = { onTabTogglePin(tab.id) },
            onReopenClosed = onTabReopenClosed,
            canReopenClosed = canReopenClosed,
            onShowInfo = { onTabInfo(tab.id) }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SingleTabItem(
    tab: DocumentTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragEnd: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val tabBg = when {
        isActive -> Color(0xFF13192B)
        else -> Color(0xFF0C101D)
    }

    val textColor = when {
        isActive -> Color(0xFFF1F5F9)
        else -> Color(0xFF94A3B8)
    }

    val borderColor = when {
        isActive -> Color(0xFF38BDF8).copy(alpha = 0.5f)
        else -> Color(0xFF1E293B)
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
            .widthIn(min = if (tab.isPinned) 54.dp else 120.dp, max = if (tab.isPinned) 70.dp else 220.dp)
            .height(if (isActive) 38.dp else 34.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
            .combinedClickable(
                onClick = onSelect,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick
            )
            .pointerInput(tab.id) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount.x
                    },
                    onDragEnd = {
                        onDragEnd(dragOffsetX)
                        dragOffsetX = 0f
                    },
                    onDragCancel = {
                        dragOffsetX = 0f
                    }
                )
            },
        color = tabBg,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // File Type Icon
                Icon(
                    imageVector = tab.fileType.getIcon(),
                    contentDescription = tab.fileType.displayName,
                    tint = tab.fileType.badgeColor,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Title (or pinned icon)
                if (tab.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Text(
                        text = tab.title,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Unsaved indicator "●"
                if (tab.isModified) {
                    Text(
                        text = " ●",
                        color = Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Close "×" button (unless pinned)
            if (!tab.isPinned) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        tint = if (isActive) Color(0xFFCBD5E1) else Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
