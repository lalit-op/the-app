package com.example.oneread.ui.components

import com.example.oneread.data.DocumentType

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oneread.data.DocumentItem

@Composable
fun RenameDialog(
    document: DocumentItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialBaseName = remember(document) {
        document.title.substringBeforeLast('.', document.title)
    }
    var text by remember { mutableStateOf(initialBaseName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(
                    "Enter a new name for the document:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input_field"),
                    label = { Text("Document Name") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("confirm_rename_button")
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_rename_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FileInfoDialog(
    document: DocumentItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Details", fontWeight = FontWeight.SemiBold) },
        text = {
            val formatDesc = when (document.fileType) {
                DocumentType.PPT -> "Microsoft PowerPoint Presentation (.${document.extension.ifBlank { "pptx" }.lowercase()})"
                DocumentType.EXCEL -> "Microsoft Excel Spreadsheet (.${document.extension.ifBlank { "xlsx" }.lowercase()})"
                DocumentType.WORD -> "Microsoft Word Document (.${document.extension.ifBlank { "docx" }.lowercase()})"
                DocumentType.PDF -> "Portable Document Format (.pdf)"
                else -> "${document.fileType.displayName} (.${document.extension.lowercase()})"
            }
            val countLabel = if (document.fileType == DocumentType.PPT) "Slides" else "Pages"

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow("File Name", document.displayTitle)
                InfoRow("Format", formatDesc)
                InfoRow("Size", document.formattedSize)
                InfoRow(countLabel, if (document.pageCount > 0) "${document.pageCount}" else "1")
                InfoRow("Last Modified", document.formattedDate)
                InfoRow("Location", document.path)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_info_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String = "Move to Recycle Bin?",
    message: String = "You can restore this file anytime from the Recycle Bin in Settings.",
    confirmText: String = "Move to Trash",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
