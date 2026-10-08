package com.example.oneread.workspace.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentTab

enum class MenuSection {
    FILE, EDIT, VIEW, TOOLS, WINDOW, HELP
}

@Composable
fun WorkspaceMenuBar(
    activeTab: DocumentTab?,
    allTabs: List<DocumentTab>,
    canReopenClosed: Boolean,
    onNewTab: () -> Unit,
    onOpenFile: () -> Unit,
    onSave: () -> Unit,
    onCloseTab: () -> Unit,
    onCloseAllTabs: () -> Unit,
    onReopenClosedTab: () -> Unit,
    onNextTab: () -> Unit,
    onPrevTab: () -> Unit,
    onSelectTab: (String) -> Unit,
    onRenameTab: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleNightMode: () -> Unit,
    onShowDocInfo: () -> Unit,
    onShowShortcuts: () -> Unit,
    onShowSupportedFormats: () -> Unit,
    onReturnToLibrary: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeMenu by remember { mutableStateOf<MenuSection?>(null) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        color = Color(0xFF13192B),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu Items: File, Edit, View, Tools, Window, Help
            val menus = listOf(
                Pair(MenuSection.FILE, "File"),
                Pair(MenuSection.EDIT, "Edit"),
                Pair(MenuSection.VIEW, "View"),
                Pair(MenuSection.TOOLS, "Tools"),
                Pair(MenuSection.WINDOW, "Window"),
                Pair(MenuSection.HELP, "Help")
            )

            menus.forEach { (section, title) ->
                Box {
                    val isSelected = (activeMenu == section)
                    Surface(
                        color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable {
                                activeMenu = if (isSelected) null else section
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    // Dropdown Content
                    when (section) {
                        MenuSection.FILE -> FileDropdownMenu(
                            expanded = isSelected,
                            onDismiss = { activeMenu = null },
                            activeTab = activeTab,
                            onNewTab = onNewTab,
                            onOpenFile = onOpenFile,
                            onSave = onSave,
                            onCloseTab = onCloseTab,
                            onCloseAllTabs = onCloseAllTabs,
                            onReopenClosedTab = onReopenClosedTab,
                            canReopenClosed = canReopenClosed,
                            onShowDocInfo = onShowDocInfo,
                            onReturnToLibrary = onReturnToLibrary
                        )
                        MenuSection.EDIT -> EditDropdownMenu(
                            expanded = isSelected,
                            onDismiss = { activeMenu = null },
                            activeTab = activeTab,
                            onRenameTab = onRenameTab
                        )
                        MenuSection.VIEW -> ViewDropdownMenu(
                            expanded = isSelected,
                            onDismiss = { activeMenu = null },
                            onZoomIn = onZoomIn,
                            onZoomOut = onZoomOut,
                            onResetZoom = onResetZoom,
                            onToggleNightMode = onToggleNightMode
                        )
                        MenuSection.TOOLS -> ToolsDropdownMenu(
                            expanded = isSelected,
                            onDismiss = { activeMenu = null },
                            activeTab = activeTab,
                            onShare = onShare,
                            onPrint = onPrint,
                            onShowDocInfo = onShowDocInfo
                        )
                        MenuSection.WINDOW -> WindowDropdownMenu(
                            expanded = isSelected,
                            onDismiss = { activeMenu = null },
                            allTabs = allTabs,
                            activeTab = activeTab,
                            canReopenClosed = canReopenClosed,
                            onNewTab = onNewTab,
                            onNextTab = onNextTab,
                            onPrevTab = onPrevTab,
                            onCloseTab = onCloseTab,
                            onCloseAllTabs = onCloseAllTabs,
                            onReopenClosedTab = onReopenClosedTab,
                            onSelectTab = onSelectTab
                        )
                        MenuSection.HELP -> HelpDropdownMenu(
                            expanded = isSelected,
                            onDismiss = { activeMenu = null },
                            onShowShortcuts = onShowShortcuts,
                            onShowSupportedFormats = onShowSupportedFormats
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    activeTab: DocumentTab?,
    onNewTab: () -> Unit,
    onOpenFile: () -> Unit,
    onSave: () -> Unit,
    onCloseTab: () -> Unit,
    onCloseAllTabs: () -> Unit,
    onReopenClosedTab: () -> Unit,
    canReopenClosed: Boolean,
    onShowDocInfo: () -> Unit,
    onReturnToLibrary: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF13192B), RoundedCornerShape(8.dp))
    ) {
        MenuItem(icon = Icons.Default.Add, title = "New Tab", shortcut = "Ctrl+T") {
            onDismiss(); onNewTab()
        }
        MenuItem(icon = Icons.Default.FileOpen, title = "Open File…", shortcut = "Ctrl+O") {
            onDismiss(); onOpenFile()
        }
        if (activeTab != null) {
            MenuItem(icon = Icons.Default.Save, title = "Save", shortcut = "Ctrl+S") {
                onDismiss(); onSave()
            }
            MenuItem(icon = Icons.Default.Close, title = "Close Tab", shortcut = "Ctrl+W") {
                onDismiss(); onCloseTab()
            }
        }
        MenuItem(icon = Icons.Default.Close, title = "Close All Tabs") {
            onDismiss(); onCloseAllTabs()
        }
        if (canReopenClosed) {
            MenuItem(icon = Icons.Default.Restore, title = "Reopen Closed Tab", shortcut = "Ctrl+Shift+T") {
                onDismiss(); onReopenClosedTab()
            }
        }
        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
        if (activeTab != null) {
            MenuItem(icon = Icons.Default.Info, title = "Document Properties") {
                onDismiss(); onShowDocInfo()
            }
        }
        MenuItem(icon = Icons.Default.Folder, title = "Exit to Document Library") {
            onDismiss(); onReturnToLibrary()
        }
    }
}

@Composable
private fun EditDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    activeTab: DocumentTab?,
    onRenameTab: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF13192B), RoundedCornerShape(8.dp))
    ) {
        if (activeTab != null) {
            MenuItem(icon = Icons.Default.Edit, title = "Rename Tab Label") {
                onDismiss(); onRenameTab()
            }
        }
        MenuItem(icon = Icons.Default.FindInPage, title = "Find in Document", shortcut = "Ctrl+F") {
            onDismiss()
        }
    }
}

@Composable
private fun ViewDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleNightMode: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF13192B), RoundedCornerShape(8.dp))
    ) {
        MenuItem(icon = Icons.Default.ZoomIn, title = "Zoom In", shortcut = "Ctrl++") {
            onDismiss(); onZoomIn()
        }
        MenuItem(icon = Icons.Default.ZoomOut, title = "Zoom Out", shortcut = "Ctrl+-") {
            onDismiss(); onZoomOut()
        }
        MenuItem(icon = Icons.Default.FitScreen, title = "Reset Zoom / Fit Screen", shortcut = "Ctrl+0") {
            onDismiss(); onResetZoom()
        }
        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
        MenuItem(icon = Icons.Default.Nightlight, title = "Toggle Dark Mode") {
            onDismiss(); onToggleNightMode()
        }
    }
}

@Composable
private fun ToolsDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    activeTab: DocumentTab?,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onShowDocInfo: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF13192B), RoundedCornerShape(8.dp))
    ) {
        if (activeTab != null) {
            MenuItem(icon = Icons.Default.Share, title = "Share Document") {
                onDismiss(); onShare()
            }
            MenuItem(icon = Icons.Default.Print, title = "Print / Export to PDF", shortcut = "Ctrl+P") {
                onDismiss(); onPrint()
            }
            MenuItem(icon = Icons.Default.Description, title = "Word & Character Stats") {
                onDismiss(); onShowDocInfo()
            }
        }
    }
}

@Composable
private fun WindowDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    allTabs: List<DocumentTab>,
    activeTab: DocumentTab?,
    canReopenClosed: Boolean,
    onNewTab: () -> Unit,
    onNextTab: () -> Unit,
    onPrevTab: () -> Unit,
    onCloseTab: () -> Unit,
    onCloseAllTabs: () -> Unit,
    onReopenClosedTab: () -> Unit,
    onSelectTab: (String) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF13192B), RoundedCornerShape(8.dp))
    ) {
        MenuItem(icon = Icons.Default.Add, title = "New Tab", shortcut = "Ctrl+T") {
            onDismiss(); onNewTab()
        }
        MenuItem(icon = Icons.Default.Tab, title = "Next Tab", shortcut = "Ctrl+Tab") {
            onDismiss(); onNextTab()
        }
        MenuItem(icon = Icons.Default.Tab, title = "Previous Tab", shortcut = "Ctrl+Shift+Tab") {
            onDismiss(); onPrevTab()
        }
        MenuItem(icon = Icons.Default.Close, title = "Close Tab", shortcut = "Ctrl+W") {
            onDismiss(); onCloseTab()
        }
        MenuItem(icon = Icons.Default.Close, title = "Close All Tabs") {
            onDismiss(); onCloseAllTabs()
        }
        if (canReopenClosed) {
            MenuItem(icon = Icons.Default.Restore, title = "Reopen Closed Tab", shortcut = "Ctrl+Shift+T") {
                onDismiss(); onReopenClosedTab()
            }
        }

        if (allTabs.isNotEmpty()) {
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
            allTabs.forEachIndexed { index, tab ->
                val isCurrent = (tab.id == activeTab?.id)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.fileType.getIcon(),
                                contentDescription = null,
                                tint = tab.fileType.badgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${index + 1}. ${tab.title}",
                                color = if (isCurrent) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    trailingIcon = {
                        if (isCurrent) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        } else if (index < 9) {
                            Text("Ctrl+${index + 1}", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    },
                    onClick = {
                        onDismiss()
                        onSelectTab(tab.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun HelpDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShowShortcuts: () -> Unit,
    onShowSupportedFormats: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color(0xFF13192B), RoundedCornerShape(8.dp))
    ) {
        MenuItem(icon = Icons.Default.Keyboard, title = "Keyboard Shortcuts") {
            onDismiss(); onShowShortcuts()
        }
        MenuItem(icon = Icons.Default.Description, title = "Supported Formats") {
            onDismiss(); onShowSupportedFormats()
        }
        MenuItem(icon = Icons.Default.Help, title = "About Multi-Tab Workspace") {
            onDismiss(); onShowSupportedFormats()
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    shortcut: String? = null,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(text = title, color = Color(0xFFF1F5F9), fontSize = 13.sp)
        },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        },
        trailingIcon = if (shortcut != null) {
            { Text(text = shortcut, color = Color(0xFF64748B), fontSize = 11.sp) }
        } else null,
        onClick = onClick
    )
}
