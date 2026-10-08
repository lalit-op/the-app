package com.example.oneread.excel.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.excel.search.ExcelSearchManager
import com.example.oneread.excel.search.SearchMatch
import com.example.oneread.excel.search.SearchOptions
import kotlinx.coroutines.launch

@Composable
fun ExcelSearchDialog(
    searchManager: ExcelSearchManager,
    onNavigateToMatch: (SearchMatch) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showReplaceOptions by remember { mutableStateOf(false) }

    var searchEntireWorkbook by remember { mutableStateOf(true) }
    var matchCase by remember { mutableStateOf(false) }
    var matchEntireCell by remember { mutableStateOf(false) }
    var searchFormulas by remember { mutableStateOf(false) }

    var showReplaceAllConfirm by remember { mutableStateOf(false) }

    val matches by searchManager.matches.collectAsState()
    val currentIndex by searchManager.currentMatchIndex.collectAsState()
    val isSearching by searchManager.isSearching.collectAsState()

    fun triggerSearch() {
        coroutineScope.launch {
            searchManager.search(
                SearchOptions(
                    query = query,
                    searchEntireWorkbook = searchEntireWorkbook,
                    matchCase = matchCase,
                    matchEntireCell = matchEntireCell,
                    searchFormulas = searchFormulas
                )
            )
        }
    }

    LaunchedEffect(query, searchEntireWorkbook, matchCase, matchEntireCell, searchFormulas) {
        triggerSearch()
    }

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
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showReplaceOptions) "Find and Replace" else "Search in Workbook",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showReplaceOptions = !showReplaceOptions },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = "Toggle Replace",
                            tint = if (showReplaceOptions) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("excel_search_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search text, numbers, formulas...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                    } else if (matches.isNotEmpty()) {
                        Text(
                            text = "${currentIndex + 1}/${matches.size}",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("excel_search_input")
            )

            // Replace field (collapsible)
            AnimatedVisibility(visible = showReplaceOptions) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        label = { Text("Replace with...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF10B981),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("excel_replace_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (searchManager.replaceCurrent(replaceText)) {
                                    triggerSearch()
                                }
                            },
                            enabled = matches.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Replace", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showReplaceAllConfirm = true },
                            enabled = matches.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Replace All (${matches.size})", color = Color(0xFF10B981), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Controls: Previous / Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (query.isNotBlank()) "${matches.size} matches" else "Type to search",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Row {
                    Button(
                        onClick = {
                            searchManager.prevMatch()?.let { onNavigateToMatch(it) }
                        },
                        enabled = matches.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(34.dp).testTag("excel_search_prev")
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            searchManager.nextMatch()?.let { onNavigateToMatch(it) }
                        },
                        enabled = matches.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(34.dp).testTag("excel_search_next")
                    ) {
                        Text("Next", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Options Checkboxes Row 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = searchEntireWorkbook,
                    onCheckedChange = { searchEntireWorkbook = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
                )
                Text(if (searchEntireWorkbook) "Entire workbook" else "Current sheet", color = Color(0xFFCBD5E1), fontSize = 12.sp)

                Spacer(modifier = Modifier.width(16.dp))

                Checkbox(
                    checked = matchCase,
                    onCheckedChange = { matchCase = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
                )
                Text("Match case", color = Color(0xFFCBD5E1), fontSize = 12.sp)
            }

            // Options Checkboxes Row 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = matchEntireCell,
                    onCheckedChange = { matchEntireCell = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
                )
                Text("Match entire cell", color = Color(0xFFCBD5E1), fontSize = 12.sp)

                Spacer(modifier = Modifier.width(16.dp))

                Checkbox(
                    checked = searchFormulas,
                    onCheckedChange = { searchFormulas = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
                )
                Text("Search formulas", color = Color(0xFFCBD5E1), fontSize = 12.sp)
            }

            // Match Results List (clickable jump)
            if (matches.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .padding(top = 6.dp)
                ) {
                    itemsIndexed(matches) { index, match ->
                        val isSelected = index == currentIndex
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF13192B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    searchManager.selectMatch(index)
                                    onNavigateToMatch(match)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${match.sheetName}!${match.cellRef}",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = match.value,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before Replace All
    if (showReplaceAllConfirm) {
        AlertDialog(
            onDismissRequest = { showReplaceAllConfirm = false },
            title = { Text("Confirm Replace All", color = Color.White) },
            text = { Text("Replace ${matches.size} occurrences of \"$query\" with \"$replaceText\"?", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        showReplaceAllConfirm = false
                        searchManager.replaceAll(query, replaceText, matchCase)
                        triggerSearch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Replace All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceAllConfirm = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
