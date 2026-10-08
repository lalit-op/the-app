package com.example.oneread.workspace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentFormat
import com.example.oneread.workspace.model.DocumentTab

/**
 * Dialog preventing accidental data loss when closing a modified tab.
 */
@Composable
fun UnsavedChangesDialog(
    tab: DocumentTab,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Unsaved Changes",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9)
            )
        },
        text = {
            Column {
                Text(
                    text = "Do you want to save the changes made to \"${tab.title}\" before closing?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your changes will be permanently lost if you don't save them.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDiscard) {
                    Text("Don't Save", color = Color(0xFFEF4444))
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        },
        containerColor = Color(0xFF13192B)
    )
}

/**
 * Dialog to rename the display label of a tab.
 */
@Composable
fun TabRenameDialog(
    tab: DocumentTab,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTitle by remember { mutableStateOf(tab.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rename Tab Label", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
        },
        text = {
            Column {
                Text(
                    text = "Enter a new display label for this document tab:",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newTitle) },
                enabled = newTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF13192B)
    )
}

/**
 * Detailed metadata dialog for a document tab.
 */
@Composable
fun DocumentInfoDialog(
    tab: DocumentTab,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Document Properties", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropertyRow("File Name", tab.originalFileName)
                PropertyRow("Format", "${tab.fileType.displayName} (${tab.displayExtension})")
                PropertyRow("Location", tab.fullPathOrUri)
                PropertyRow("Current Page / Slide", "${tab.currentPage} of ${tab.totalPages}")
                PropertyRow("Zoom Level", "${(tab.zoomLevel * 100).toInt()}%")
                PropertyRow("Status", if (tab.isModified) "Modified (Unsaved)" else "Saved")
                PropertyRow("Pinned", if (tab.isPinned) "Yes" else "No")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF13192B)
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = Color(0xFFE2E8F0), fontSize = 13.sp)
    }
}

/**
 * Keyboard shortcuts cheat sheet dialog.
 */
@Composable
fun ShortcutsDialog(onDismiss: () -> Unit) {
    val shortcuts = listOf(
        Pair("Ctrl + T", "Open New Tab"),
        Pair("Ctrl + W", "Close Current Tab"),
        Pair("Ctrl + Shift + T", "Reopen Last Closed Tab"),
        Pair("Ctrl + Tab", "Switch to Next Tab"),
        Pair("Ctrl + Shift + Tab", "Switch to Previous Tab"),
        Pair("Ctrl + 1..9", "Switch to Tab 1 to 9"),
        Pair("Ctrl + O", "Open File from Storage"),
        Pair("Ctrl + S", "Save Current Document"),
        Pair("Ctrl + F", "Find / Search in Document"),
        Pair("Double Click Tab", "Rename Tab Display Label")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Keyboard Shortcuts", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                shortcuts.forEach { (shortcut, action) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = shortcut,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(text = action, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        },
        containerColor = Color(0xFF13192B)
    )
}

/**
 * Supported formats overview dialog.
 */
@Composable
fun SupportedFormatsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Supported Formats Architecture", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "The One Read multi-tab workspace architecture natively supports modular renderers for:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                DocumentFormat.values().filter { it != DocumentFormat.UNKNOWN }.forEach { fmt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = fmt.badgeColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.width(64.dp)
                        ) {
                            Text(
                                text = fmt.displayName,
                                color = fmt.badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fmt.extensions.joinToString(", ") { ".$it" },
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF13192B)
    )
}
