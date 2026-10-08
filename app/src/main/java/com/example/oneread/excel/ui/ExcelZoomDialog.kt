package com.example.oneread.excel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Compact Spreadsheet Zoom Dialog / Bottom Sheet
 * Supports:
 * - Zoom In (+) & Zoom Out (-)
 * - Direct percentage presets: 50%, 75%, 100% (Reset), 125%, 150%, 175%, 200%, 300%
 * - Quick "Reset to 100%"
 * - "Fit to Width"
 */
@Composable
fun ExcelZoomDialog(
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    onFitToWidth: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPct = (currentZoom * 100).roundToInt()
    val presets = listOf(50, 75, 80, 90, 100, 110, 120, 130, 150, 175, 200, 250, 300)

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
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Spreadsheet Zoom",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("excel_zoom_dialog_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Stepper Control: [ - ]   100%   [ + ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Zoom Out Button
                IconButton(
                    onClick = {
                        val nextPct = when {
                            currentPct <= 150 -> ((currentPct - 1) / 10) * 10
                            else -> ((currentPct - 1) / 25) * 25
                        }.coerceIn(50, 300)
                        onZoomChange(nextPct / 100f)
                    },
                    enabled = currentPct > 50,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (currentPct > 50) Color(0xFF334155) else Color(0xFF1E293B))
                        .testTag("excel_zoom_dialog_minus")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = if (currentPct > 50) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Current Percentage Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currentPct%",
                        color = if (currentPct == 100) Color(0xFF10B981) else Color(0xFF38BDF8),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (currentPct == 100) "Default (100%)" else "Spreadsheet Scale",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                // Zoom In Button
                IconButton(
                    onClick = {
                        val nextPct = when {
                            currentPct < 150 -> ((currentPct / 10) + 1) * 10
                            else -> ((currentPct / 25) + 1) * 25
                        }.coerceIn(50, 300)
                        onZoomChange(nextPct / 100f)
                    },
                    enabled = currentPct < 300,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (currentPct < 300) Color(0xFF334155) else Color(0xFF1E293B))
                        .testTag("excel_zoom_dialog_plus")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = if (currentPct < 300) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Header
            Text(
                text = "QUICK PRESETS",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal Presets Scroll
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { pct ->
                    val isSelected = pct == currentPct
                    Surface(
                        onClick = { onZoomChange(pct / 100f) },
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isSelected -> Color(0xFF10B981)
                            pct == 100 -> Color(0xFF1E3A8A)
                            else -> Color(0xFF1E293B)
                        },
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.testTag("excel_zoom_preset_$pct")
                    ) {
                        Text(
                            text = if (pct == 100) "100% (Default)" else "$pct%",
                            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected || pct == 100) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons: Fit to Width & Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fit to Width
                Button(
                    onClick = {
                        onFitToWidth()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(42.dp).testTag("excel_zoom_fit_width")
                ) {
                    Icon(imageVector = Icons.Default.FitScreen, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fit to Width", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Reset to 100%
                Button(
                    onClick = {
                        onZoomChange(1.0f)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(42.dp).testTag("excel_zoom_reset_100")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset to 100%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
