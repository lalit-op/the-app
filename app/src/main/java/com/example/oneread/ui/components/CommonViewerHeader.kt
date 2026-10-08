package com.example.oneread.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standardized Master Viewer Header for HR Read.
 * Provides a continuous, true edge-to-edge dark surface (#0F172A)
 * with status bar / display cutout padding and high-contrast light controls.
 */
@Composable
fun CommonViewerHeader(
    title: String,
    onBack: () -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchPlaceholder: String = "Search...",
    searchFocusRequester: FocusRequester = remember { FocusRequester() },
    isSearching: Boolean = false,
    searchMatchCount: Int = 0,
    currentMatchIndex: Int = -1,
    showResultCounter: Boolean = true,
    showNavArrows: Boolean = true,
    onPrevMatch: () -> Unit = {},
    onNextMatch: () -> Unit = {},
    onSearchExecute: () -> Unit = {},
    onClearSearch: () -> Unit = {},
    showSearchButton: Boolean = true,
    showRefreshButton: Boolean = false,
    onRefresh: () -> Unit = {},
    testTagPrefix: String = "viewer",
    openDocumentsCount: Int = 1,
    onDocumentSwitcherClick: (() -> Unit)? = null,
    customActions: @Composable RowScope.() -> Unit = {},
    overflowMenuItems: (@Composable ColumnScope.(onDismiss: () -> Unit) -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = Color(0xFF0F172A),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
        ) {
            if (isSearchActive) {
                // Full-width prominent search interface (Master design)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [🔍] Search Icon
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 4.dp, end = 8.dp)
                            .size(22.dp)
                    )

                    // Wide Search Input Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = searchPlaceholder,
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { onSearchExecute() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                                .testTag("${testTagPrefix}_search_input")
                        )
                    }

                    // Clear query button [ ✕ ]
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    // Progress or Results info
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(horizontal = 4.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else if (showResultCounter && searchQuery.isNotBlank()) {
                        if (searchMatchCount > 0) {
                            // Match Counter: e.g. "1/11"
                            val displayIndex = if (currentMatchIndex >= 0) currentMatchIndex + 1 else 1
                            Text(
                                text = "$displayIndex/$searchMatchCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            if (showNavArrows) {
                                // Previous match [ ↑ ]
                                IconButton(
                                    onClick = onPrevMatch,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("${testTagPrefix}_search_prev")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Previous match",
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                }

                                // Next match [ ↓ ]
                                IconButton(
                                    onClick = onNextMatch,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("${testTagPrefix}_search_next")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Next match",
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "0/0",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFCA5A5),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }

                    // Close Search Button [ ✕ ]
                    IconButton(
                        onClick = {
                            onSearchActiveChange(false)
                            onClearSearch()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("${testTagPrefix}_search_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close search",
                            modifier = Modifier.size(22.dp),
                            tint = Color.White
                        )
                    }
                }
            } else {
                // Full-width standard viewer toolbar (Master design)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("${testTagPrefix}_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Action 1: Search
                    if (showSearchButton) {
                        IconButton(
                            onClick = { onSearchActiveChange(true) },
                            modifier = Modifier.testTag("${testTagPrefix}_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }
                    }

                    // Action 2: Refresh/Reload
                    if (showRefreshButton) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("${testTagPrefix}_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }

                    // Action 3: Document-specific Custom Actions (e.g. Rotate for PDF, Theme for text)
                    customActions()

                    // Action 3.5: Compact WPS-style Document Switcher Counter Button [ ⊞ 3 ]
                    if (onDocumentSwitcherClick != null) {
                        Surface(
                            onClick = onDocumentSwitcherClick,
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.8f)),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(30.dp)
                                .testTag("document_switcher_button")
                                .testTag("${testTagPrefix}_document_switcher_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tab,
                                    contentDescription = "Open Tabs ($openDocumentsCount)",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$openDocumentsCount",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Action 4: Overflow (⋮) Menu
                    if (overflowMenuItems != null) {
                        var showMoreMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("${testTagPrefix}_more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = Color.White // STRICT: ALWAYS WHITE, NEVER BLACK DOTS!
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                overflowMenuItems { showMoreMenu = false }
                            }
                        }
                    }
                }
            }
        }
    }
}
