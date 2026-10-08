package com.example.oneread.ppt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PptSlideLayout(val label: String, val description: String) {
    TITLE_AND_CONTENT("Title & Content", "Header with bulleted content points"),
    TITLE_ONLY("Title Slide", "Prominent presentation or section title"),
    SECTION_HEADER("Section Header", "Large title with category text"),
    BLANK("Blank", "Clean empty white slide canvas")
}

@Composable
fun PptNewSlideDialog(
    onConfirm: (title: String, content: String, layout: PptSlideLayout) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLayout by remember { mutableStateOf(PptSlideLayout.TITLE_AND_CONTENT) }
    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Add New Slide",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Layout selector chips
                Text(
                    text = "SELECT LAYOUT",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PptSlideLayout.values().forEach { layout ->
                        val isSelected = layout == selectedLayout
                        Surface(
                            onClick = { selectedLayout = layout },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFEA580C) else Color(0xFF1E293B),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.weight(1f).testTag("ppt_layout_${layout.name}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = layout.label.split(" ").first(),
                                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Title Input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Slide Title") },
                    placeholder = { Text("e.g. Project Overview") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ppt_new_slide_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFB923C),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFFFB923C)
                    )
                )

                // Content Input (if not Blank)
                if (selectedLayout != PptSlideLayout.BLANK) {
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        label = { Text("Slide Content (One line per bullet)") },
                        placeholder = { Text("• Point 1\n• Point 2\n• Point 3") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().testTag("ppt_new_slide_content"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFB923C),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFFB923C)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(titleText, contentText, selectedLayout)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("ppt_new_slide_confirm")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Slide", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(14.dp)
    )
}
