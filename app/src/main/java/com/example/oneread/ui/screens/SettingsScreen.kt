package com.example.oneread.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.R
import com.example.oneread.ui.DirectoryInfo
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.navigation.Screen

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val directories by viewModel.directories.collectAsState()

    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showDirectoryBrowser by rememberSaveable { mutableStateOf(false) }
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.scanDirectoryTree(it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050811))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header: Settings / HR READ • ALL IN ONE READER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "HR READ • ALL IN ONE READER",
                    color = Color(0xFF8899AC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.6.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section: General
                item {
                    SettingsSectionHeader(title = "General")
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1425).copy(alpha = 0.85f)),
                        border = BorderStroke(1.2.dp, Color(0xFF1E2B45))
                    ) {
                        Column {
                            // File Manager
                            SettingsActionRow(
                                title = "File Manager",
                                subtitle = "Manage files and folders",
                                badgeColors = listOf(Color(0xFF00B0FF), Color(0xFF0066FF)),
                                icon = Icons.Default.Folder,
                                onClick = { showDirectoryBrowser = true }
                            )

                            SettingsDivider()

                            // Language Options
                            SettingsActionRow(
                                title = "Language Options",
                                subtitle = "English",
                                badgeColors = listOf(Color(0xFF00B0FF), Color(0xFF0288D1)),
                                icon = Icons.Default.Language,
                                onClick = { showLanguageDialog = true }
                            )

                            SettingsDivider()

                            // Replay Splash Screen
                            SettingsActionRow(
                                title = "Replay Splash Screen",
                                subtitle = "Preview loading bar & bottom ambient glow",
                                badgeColors = listOf(Color(0xFFFF5722), Color(0xFFFF9800)),
                                icon = Icons.Default.PlayArrow,
                                onClick = { viewModel.showSplash() }
                            )
                        }
                    }
                }

                // Section: PDF & Document Tools
                item {
                    SettingsSectionHeader(title = "PDF & Document Tools")
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1425).copy(alpha = 0.85f)),
                        border = BorderStroke(1.2.dp, Color(0xFF1E2B45))
                    ) {
                        Column {
                            // Image to PDF
                            SettingsActionRow(
                                title = "Image to PDF",
                                subtitle = "Convert images from gallery into a single PDF",
                                badgeColors = listOf(Color(0xFFFF5252), Color(0xFFFF1744)),
                                icon = Icons.Default.Image,
                                onClick = { viewModel.navigateTo(Screen.ImageToPdf) }
                            )

                            SettingsDivider()

                            // Keep screen on
                            SettingsToggleRow(
                                title = "Keep screen on",
                                subtitle = "Prevent screen from turning off while reading",
                                badgeColors = listOf(Color(0xFF00E676), Color(0xFF00A344)),
                                icon = Icons.Default.PhoneAndroid,
                                checked = keepScreenOn,
                                onCheckedChange = { viewModel.setKeepScreenOn(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    text = "Language Options",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    listOf("English (Default)", "Spanish", "French", "German", "Hindi", "Japanese").forEachIndexed { index, lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = false }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = index == 0,
                                onClick = { showLanguageDialog = false }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = lang,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Directory Browser Dialog
    if (showDirectoryBrowser) {
        DirectoryBrowserDialog(
            directories = directories,
            onDismiss = { showDirectoryBrowser = false },
            onSelectFolder = { dirName ->
                showDirectoryBrowser = false
                viewModel.setSearchQuery(dirName)
                viewModel.navigateTo(Screen.Files(com.example.oneread.data.DocumentType.ALL))
            },
            onBrowseDeviceTree = {
                showDirectoryBrowser = false
                directoryPickerLauncher.launch(null)
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF8899AC),
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = Color(0xFF162238),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 68.dp)
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    badgeColors: List<Color>,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded Badge Icon matching screenshot
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(badgeColors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF8899AC),
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFF8899AC),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    badgeColors: List<Color>,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(badgeColors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF8899AC),
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2979FF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF1E2C46),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun DirectoryBrowserDialog(
    directories: List<DirectoryInfo>,
    onDismiss: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onBrowseDeviceTree: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Device Directories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (directories.isEmpty()) {
                    Text(
                        text = "No document directories detected yet. Use the button below to pick and scan any folder on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8899AC)
                    )
                } else {
                    Text(
                        text = "Accessible folders with supported documents:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8899AC),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        items(directories, key = { it.name }) { dir ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectFolder(dir.name) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dir.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (dir.count == 1) "1 document" else "${dir.count} documents",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8899AC)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBrowseDeviceTree,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select & Scan Folder (SAF)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
