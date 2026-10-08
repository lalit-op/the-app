package com.example.oneread.excel.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp

/**
 * Modern spreadsheet bottom tab bar for multiple sheets.
 * [Sheet1] [Sales] [Expenses] [Summary] [+]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExcelSheetTabsBar(
    sheetNames: List<String>,
    activeSheetIndex: Int,
    onSelectSheet: (Int) -> Unit,
    onAddSheet: () -> Unit,
    onRenameSheet: (Int) -> Unit,
    onDuplicateSheet: (Int) -> Unit,
    onDeleteSheet: (Int) -> Unit,
    onMoveSheetLeft: (Int) -> Unit,
    onMoveSheetRight: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuSheetIndex by remember { mutableIntStateOf(-1) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScrollableTabRow(
                selectedTabIndex = activeSheetIndex.coerceIn(0, maxOf(0, sheetNames.size - 1)),
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    if (activeSheetIndex in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeSheetIndex]),
                            color = Color(0xFF10B981),
                            height = 3.dp
                        )
                    }
                },
                divider = {},
                modifier = Modifier.weight(1f)
            ) {
                sheetNames.forEachIndexed { index, name ->
                    val isSelected = index == activeSheetIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                            .combinedClickable(
                                onClick = { onSelectSheet(index) },
                                onLongClick = { menuSheetIndex = index }
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("excel_sheet_tab_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF94A3B8),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1
                        )

                        // Context menu on long press
                        if (menuSheetIndex == index) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { menuSheetIndex = -1 }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename Sheet") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuSheetIndex = -1
                                        onRenameSheet(index)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate Sheet") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        menuSheetIndex = -1
                                        onDuplicateSheet(index)
                                    }
                                )
                                if (index > 0) {
                                    DropdownMenuItem(
                                        text = { Text("Move Left") },
                                        leadingIcon = { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null) },
                                        onClick = {
                                            menuSheetIndex = -1
                                            onMoveSheetLeft(index)
                                        }
                                    )
                                }
                                if (index < sheetNames.size - 1) {
                                    DropdownMenuItem(
                                        text = { Text("Move Right") },
                                        leadingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                        onClick = {
                                            menuSheetIndex = -1
                                            onMoveSheetRight(index)
                                        }
                                    )
                                }
                                if (sheetNames.size > 1) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Sheet", color = Color(0xFFEF4444)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                        onClick = {
                                            menuSheetIndex = -1
                                            onDeleteSheet(index)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // [+] Add New Sheet Button
            IconButton(
                onClick = onAddSheet,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(36.dp)
                    .testTag("excel_add_sheet_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Sheet",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
