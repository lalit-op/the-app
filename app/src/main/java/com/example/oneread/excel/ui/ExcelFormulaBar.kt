package com.example.oneread.excel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Professional Excel Formula and Name Box Bar:
 * [ Name Box: B12 ] [ fx ] [ Formula / Cell Content: =SUM(B2:B11) ] [ ✓ ] [ ✕ ]
 */
@Composable
fun ExcelFormulaBar(
    nameBoxText: String,
    currentValue: String,
    onValueCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFormulaBarClick: (() -> Unit)? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember(currentValue) { mutableStateOf(currentValue) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        color = Color(0xFF13192B),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Name Box (e.g. B12 or A1:C5)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .width(68.dp)
                    .height(32.dp)
                    .testTag("excel_name_box")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = nameBoxText,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // fx Icon Indicator
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "fx",
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Formula / Cell Content Input Field
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isEditing) Color(0xFF38BDF8) else Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clickable {
                        isEditing = true
                        onFormulaBarClick?.invoke()
                    }
                    .testTag("excel_formula_input_box")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            isEditing = true
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFFF1F5F9),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            onValueCommit(textValue)
                            isEditing = false
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("excel_formula_text_field")
                    )

                    if (isEditing && textValue != currentValue) {
                        IconButton(
                            onClick = {
                                onValueCommit(textValue)
                                isEditing = false
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("excel_formula_commit")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Commit",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                textValue = currentValue
                                isEditing = false
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("excel_formula_cancel")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
