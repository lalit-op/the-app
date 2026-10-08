package com.example.oneread.workspace.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TabUnselected
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentTab

@Composable
fun TabContextMenu(
    tab: DocumentTab,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onClose: () -> Unit,
    onCloseOthers: () -> Unit,
    onCloseTabsToRight: () -> Unit,
    onCloseAll: () -> Unit,
    onDuplicate: () -> Unit,
    onTogglePin: () -> Unit,
    onReopenClosed: () -> Unit,
    canReopenClosed: Boolean,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(Color(0xFF13192B), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp)
    ) {
        // Tab Title Header
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            Text(
                text = tab.title,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

        // Close Tab
        DropdownMenuItem(
            text = { Text("Close", color = Color(0xFFF1F5F9), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            },
            trailingIcon = { Text("Ctrl+W", color = Color(0xFF64748B), fontSize = 11.sp) },
            onClick = {
                onDismissRequest()
                onClose()
            }
        )

        // Close Other Tabs
        DropdownMenuItem(
            text = { Text("Close Other Tabs", color = Color(0xFFF1F5F9), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.TabUnselected, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            },
            onClick = {
                onDismissRequest()
                onCloseOthers()
            }
        )

        // Close Tabs to the Right
        DropdownMenuItem(
            text = { Text("Close Tabs to the Right", color = Color(0xFFF1F5F9), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            },
            onClick = {
                onDismissRequest()
                onCloseTabsToRight()
            }
        )

        // Close All Tabs
        DropdownMenuItem(
            text = { Text("Close All Tabs", color = Color(0xFFF1F5F9), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            },
            onClick = {
                onDismissRequest()
                onCloseAll()
            }
        )

        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

        // Pin / Unpin Tab
        DropdownMenuItem(
            text = { Text(if (tab.isPinned) "Unpin Tab" else "Pin Tab", color = Color(0xFFF1F5F9), fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = if (tab.isPinned) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onTogglePin()
            }
        )

        // Duplicate Tab
        DropdownMenuItem(
            text = { Text("Duplicate Tab", color = Color(0xFFF1F5F9), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            },
            onClick = {
                onDismissRequest()
                onDuplicate()
            }
        )

        // Reopen Closed Tab
        if (canReopenClosed) {
            DropdownMenuItem(
                text = { Text("Reopen Closed Tab", color = Color(0xFFF1F5F9), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                },
                trailingIcon = { Text("Ctrl+Shift+T", color = Color(0xFF64748B), fontSize = 11.sp) },
                onClick = {
                    onDismissRequest()
                    onReopenClosed()
                }
            )
        }

        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

        // Open File Location / Info
        DropdownMenuItem(
            text = { Text("Document Info / Location", color = Color(0xFFF1F5F9), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
            },
            onClick = {
                onDismissRequest()
                onShowInfo()
            }
        )
    }
}
