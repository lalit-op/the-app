package com.example.oneread.excel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.excel.model.ExcelCellModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExcelCellEditDialog(
    cellRef: String,
    initialValue: String,
    initialFormula: String?,
    onCommit: (value: String, formula: String?) -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(initialFormula ?: initialValue) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = Color(0xFF0F172A),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Cell Reference & Quick Clipboard actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = cellRef,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text("Edit Cell", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCut, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.ContentCut, contentDescription = "Cut", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onPaste, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Input Field
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Value or formula (=SUM...)") },
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedLabelColor = Color(0xFF10B981),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val isFormula = text.startsWith("=")
                    onCommit(text, if (isFormula) text else null)
                    onDismiss()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("excel_cell_edit_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Formula Helper Chips
            Text("Insert Formula:", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val formulas = listOf("=SUM()", "=AVERAGE()", "=COUNT()", "=IF()", "=MIN()", "=MAX()", "=ROUND()")
                for (f in formulas) {
                    SuggestionChip(
                        onClick = {
                            text = if (text.isBlank()) f else "$text$f"
                        },
                        label = { Text(f, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF38BDF8)
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = Color(0xFF334155)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Done / Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        val isFormula = text.startsWith("=")
                        onCommit(text, if (isFormula) text else null)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.height(40.dp).testTag("excel_cell_edit_commit")
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
