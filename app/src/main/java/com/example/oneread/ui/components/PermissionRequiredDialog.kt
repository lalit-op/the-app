package com.example.oneread.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Opens the appropriate Android Settings screen for HR Read's required file access.
 * On Android 11+ (API 30+), targets ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION.
 * On earlier versions, targets ACTION_APPLICATION_DETAILS_SETTINGS.
 */
fun openAllFilesPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    } else {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/**
 * Permission Required Onboarding Screen & Overlay matching reference UI:
 * - Dark HR Read background
 * - Rounded dialog panel
 * - HR Read 3D vault illustration (folder, documents & security shield)
 * - "Permission Required" headline
 * - Description with highlighted HR Read name
 * - "Allow access to manage all files" interactive row
 * - Vibrant "ALLOW" action button
 * - Optional Close X button
 */
@Composable
fun PermissionRequiredDialog(
    visible: Boolean,
    onClose: () -> Unit,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6030712))
                .clickable(enabled = false) {}, // Scrim blocks clicks from reaching back layers
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF0B142A),
                border = BorderStroke(1.dp, Color(0xFF1B2A52)),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .testTag("permission_required_panel")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 22.dp)
                ) {
                    // Close X button at top right
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                            .testTag("permission_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // 3D Vault Illustration matching the reference design
                        PermissionVaultIllustration(
                            modifier = Modifier
                                .size(width = 240.dp, height = 175.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Title
                        Text(
                            text = "Permission Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Explanation text with highlighted "HR Read"
                        val explanationText = buildAnnotatedString {
                            append("To read, organize, and manage documents on your device, please allow ")
                            withStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)) {
                                append("HR Read")
                            }
                            append(" to access all your files.")
                        }

                        Text(
                            text = explanationText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // "Allow access to manage all files" Row
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F1C38),
                            border = BorderStroke(1.dp, Color(0xFF1E356A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAllow() }
                                .testTag("permission_manage_files_row")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Blue folder icon container
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF1D4ED8), Color(0xFF1E40AF))
                                            ),
                                            RoundedCornerShape(11.dp)
                                        )
                                        .border(
                                            BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                                            RoundedCornerShape(11.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color(0xFFE0F2FE),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Allow access to manage all files",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tap to open settings and enable access",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFF8899AC)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Large vibrant gradient "ALLOW" button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0xFF2563EB))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF0080FF),
                                            Color(0xFF1D4ED8),
                                            Color(0xFF2563EB)
                                        )
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onAllow() }
                                .testTag("permission_allow_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ALLOW",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * High fidelity vector illustration of the 3D secure file vault:
 * - Glowing radial backdrop
 * - Blue folder base
 * - Layered document badges: Red PDF, Blue Word, Green Excel, Orange PPT
 * - White document card with text lines
 * - Glowing 3D blue shield with security padlock
 */
@Composable
fun PermissionVaultIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // 1. Ambient blue glow behind vault
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2563EB).copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(cx, cy * 0.9f),
                    radius = w * 0.48f
                )
            )

            // 2. Back folder flap (darker blue)
            val backFolderW = w * 0.72f
            val backFolderH = h * 0.52f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
                ),
                topLeft = Offset(cx - backFolderW / 2f, cy - backFolderH / 2f - 6.dp.toPx()),
                size = Size(backFolderW, backFolderH),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )

            // Sparkle rays
            val rayPaint = Color(0xFF60A5FA).copy(alpha = 0.7f)
            drawLine(rayPaint, Offset(cx - w * 0.38f, cy - h * 0.15f), Offset(cx - w * 0.42f, cy - h * 0.20f), strokeWidth = 2.dp.toPx())
            drawLine(rayPaint, Offset(cx + w * 0.38f, cy - h * 0.18f), Offset(cx + w * 0.43f, cy - h * 0.23f), strokeWidth = 2.dp.toPx())
            drawLine(rayPaint, Offset(cx + w * 0.39f, cy + h * 0.05f), Offset(cx + w * 0.44f, cy + h * 0.08f), strokeWidth = 2.dp.toPx())
        }

        // Layered document cards emerging from folder
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 30.dp),
            horizontalArrangement = Arrangement.spacedBy((-8).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF card (Red)
            MiniIllustrationDoc(
                color = Color(0xFFDC2626),
                label = "PDF",
                rotation = -12f,
                modifier = Modifier.size(width = 38.dp, height = 48.dp)
            )

            // Word card (Blue)
            MiniIllustrationDoc(
                color = Color(0xFF2563EB),
                label = "W",
                rotation = -5f,
                modifier = Modifier.size(width = 38.dp, height = 50.dp)
            )

            // Excel card (Green)
            MiniIllustrationDoc(
                color = Color(0xFF16A34A),
                label = "X",
                rotation = 4f,
                modifier = Modifier.size(width = 38.dp, height = 50.dp)
            )

            // PPT card (Orange)
            MiniIllustrationDoc(
                color = Color(0xFFEA580C),
                label = "P",
                rotation = 12f,
                modifier = Modifier.size(width = 38.dp, height = 48.dp)
            )
        }

        // Front white document sheet with lines
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 6.dp, end = 40.dp)
                .size(width = 54.dp, height = 64.dp)
                .rotate(-7f)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.width(26.dp).height(3.5.dp).background(Color(0xFF2563EB), RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.width(36.dp).height(3.dp).background(Color(0xFF94A3B8), RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.width(32.dp).height(3.dp).background(Color(0xFF94A3B8), RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.width(22.dp).height(3.dp).background(Color(0xFF94A3B8), RoundedCornerShape(2.dp)))
            }
        }

        // Front folder flap
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            val frontW = w * 0.76f
            val frontH = h * 0.44f

            // Front blue folder body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF1E3A8A))
                ),
                topLeft = Offset(cx - frontW / 2f, cy - 2.dp.toPx()),
                size = Size(frontW, frontH),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )

            // Folder top highlight rim
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF60A5FA).copy(alpha = 0.8f), Color(0xFF3B82F6))
                ),
                topLeft = Offset(cx - frontW / 2f, cy - 2.dp.toPx()),
                size = Size(frontW, 2.5.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        // Foreground 3D Shield with Lock
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 18.dp, start = 22.dp)
                .size(width = 62.dp, height = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Shield Path
                val shieldPath = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    cubicTo(w * 0.88f, 0f, w, h * 0.15f, w, h * 0.45f)
                    cubicTo(w, h * 0.75f, w * 0.5f, h, w * 0.5f, h)
                    cubicTo(w * 0.5f, h, 0f, h * 0.75f, 0f, h * 0.45f)
                    cubicTo(0f, h * 0.15f, w * 0.12f, 0f, w * 0.5f, 0f)
                    close()
                }

                // Shield fill gradient
                drawPath(
                    path = shieldPath,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF1E40AF))
                    )
                )

                // Shield glowing cyan border
                drawPath(
                    path = shieldPath,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFBAE6FD), Color(0xFF38BDF8))
                    ),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Inner shield dark core
                val innerShield = Path().apply {
                    val inset = 5.dp.toPx()
                    moveTo(w * 0.5f, inset)
                    cubicTo(w * 0.85f, inset, w - inset, h * 0.18f, w - inset, h * 0.45f)
                    cubicTo(w - inset, h * 0.72f, w * 0.5f, h - inset, w * 0.5f, h - inset)
                    cubicTo(w * 0.5f, h - inset, inset, h * 0.72f, inset, h * 0.45f)
                    cubicTo(inset, h * 0.18f, w * 0.15f, inset, w * 0.5f, inset)
                    close()
                }
                drawPath(
                    path = innerShield,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0369A1), Color(0xFF0C4A6E))
                    )
                )
            }

            // White Padlock in the center of shield
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .shadow(4.dp, shape = CircleShape, spotColor = Color.Cyan)
            )
        }
    }
}

@Composable
private fun MiniIllustrationDoc(
    color: Color,
    label: String,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .rotate(rotation)
            .background(color, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
    }
}
