package com.example.oneread.workspace.viewers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.workspace.model.DocumentTab

/**
 * Common viewer toolbar for zoom, search, night mode, and save actions.
 */
@Composable
fun DocumentViewerToolbar(
    tab: DocumentTab,
    onZoomIn: (() -> Unit)? = null,
    onZoomOut: (() -> Unit)? = null,
    onResetZoom: (() -> Unit)? = null,
    onToggleNightMode: (() -> Unit)? = null,
    isNightMode: Boolean = false,
    onToggleSearch: (() -> Unit)? = null,
    isSearchActive: Boolean = false,
    onSave: (() -> Unit)? = null,
    canSave: Boolean = false,
    customLeadingContent: (@Composable () -> Unit)? = null,
    customTrailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF13192B),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Document type badge
                Surface(
                    color = tab.fileType.badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tab.displayExtension,
                        color = tab.fileType.badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = tab.title,
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                if (tab.isModified) {
                    Text(
                        text = " ● (unsaved)",
                        color = Color(0xFFF59E0B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                customLeadingContent?.invoke()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                customTrailingContent?.invoke()

                if (onToggleSearch != null) {
                    IconButton(
                        onClick = onToggleSearch,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FindInPage,
                            contentDescription = "Search",
                            tint = if (isSearchActive) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onZoomOut != null) {
                    IconButton(
                        onClick = onZoomOut,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onResetZoom != null) {
                    IconButton(
                        onClick = onResetZoom,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitScreen,
                            contentDescription = "Fit Screen",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onZoomIn != null) {
                    IconButton(
                        onClick = onZoomIn,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onToggleNightMode != null) {
                    IconButton(
                        onClick = onToggleNightMode,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = "Night Mode",
                            tint = if (isNightMode) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onSave != null && (canSave || tab.isModified)) {
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Document",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentLoadingState(message: String = "Loading document…") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
    }
}

@Composable
fun DocumentErrorState(error: String, onRetry: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "⚠️", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Could not load document",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
