package com.example.oneread.ui.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.R

/**
 * HR Read Splash Screen
 *
 * Professional, minimal, futuristic document-reader splash screen:
 * - Single HR Read logo centered vertically in the upper-middle area
 * - Clean transparent rounded background with zero white corner artifacts
 * - Centered typography: "HR Read" & "ALL IN ONE READER"
 * - Deep navy / almost black background with subtle blue-to-red atmospheric gradient
 * - Thin glowing horizontal edge-lighting line along the BOTTOM EDGE
 * - Red -> Orange gradient that represents exact loading percentage:
 *   10% = 10% screen width, 50% = exactly half glowing, 100% = full edge glow
 * - Dynamic loading text just above bottom edge: "Loading... 50%" -> "Ready" at 100%
 * - System UI (status bar and gesture navigation bar) kept cleanly visible and uncropped
 */
@Composable
fun SplashScreen(
    progress: Int,
    statusText: String,
    onFinished: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")

    // Ambient gentle breathing animation for the bottom neon edge glow
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edge_pulse"
    )

    // Smoothly animate progress percentage changes (0f..1f, never exceeds actual progress)
    val targetFraction = (progress.coerceIn(0, 100) / 100f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "splash_progress_anim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        // 1. Subtle atmospheric background gradient (Dark navy with blue & deep red ambience)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Upper atmospheric blue glow centered behind the logo area
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x221E40AF),
                        Color(0x0C172554),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                    radius = size.width * 0.65f
                )
            )

            // Lower atmospheric deep red glow blending upward from bottom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x15DC2626),
                        Color(0x087F1D1D),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.95f),
                    radius = size.width * 0.75f
                )
            )
        }

        // 2. Central Branding Content:
        // Centered vertically in the upper-middle area with single HR Read logo
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 56.dp), // Offsets slightly upward to rest in upper-middle area
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // HR Read Logo (Shown ONLY ONCE, clean transparent background, zero artifacts)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(116.dp)
            ) {
                // Soft radial backlight behind logo for futuristic depth
                Canvas(modifier = Modifier.size(136.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x383B82F6),
                                Color(0x121D4ED8),
                                Color.Transparent
                            )
                        )
                    )
                }

                // HR Read Logo with smooth rounded corners and subtle glass border
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "HR Read Logo",
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            BorderStroke(1.dp, Color(0x3360A5FA)),
                            RoundedCornerShape(24.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main App Title
            Text(
                text = "HR Read",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "ALL IN ONE READER",
                color = Color(0xFF60A5FA),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.2.sp
            )
        }

        // 3. Loading Text just above the bottom edge
        // Clean branded text: "Loading..." -> "Ready"
        val displayText = if (progress >= 100 || statusText.equals("Ready", ignoreCase = true)) {
            "Ready"
        } else {
            "Loading..."
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayText,
                color = if (progress >= 100) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }

        // 4. Glowing Red -> Orange Edge Lighting Loading Line along the BOTTOM EDGE
        // - Thin glowing horizontal edge-lighting line directly on bottom edge
        // - 0% -> no visible active line
        // - 20% -> first 20% of bottom edge illuminated
        // - 50% -> exactly 50% illuminated
        // - 75% -> exactly 75% illuminated
        // - 100% -> entire bottom edge illuminated
        // - Red -> Orange gradient with soft glow
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val activeWidth = size.width * animatedFraction.coerceIn(0f, 1f)
                val lineY = size.height - 1.5.dp.toPx()
                val coreStrokePx = 3.dp.toPx()

                // Neutral subtle track along entire width (inactive portion has zero red)
                drawLine(
                    color = Color(0x10FFFFFF),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 1.dp.toPx()
                )

                // Active portion: Glowing Red -> Orange gradient & upward diffused ambient glow
                // When 0% -> activeWidth is 0, so NO visible active line is drawn
                if (activeWidth > 0f) {
                    val gradientBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF1744), // Vibrant Red
                            Color(0xFFFF5722), // Deep Orange
                            Color(0xFFFF9100)  // Bright Orange
                        ),
                        startX = 0f,
                        endX = size.width
                    )

                    // Strictly clip ambient upward glow and glowing line to the active width
                    clipRect(left = 0f, top = 0f, right = activeWidth, bottom = size.height) {
                        // Soft upward atmospheric glow illuminating from active section
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFE53935).copy(alpha = 0.05f * pulseAlpha),
                                    Color(0xFFFF5722).copy(alpha = 0.16f * pulseAlpha),
                                    Color(0xFFFF6D00).copy(alpha = 0.35f * pulseAlpha)
                                ),
                                startY = 0f,
                                endY = size.height
                            )
                        )

                        // Wide soft neon bloom
                        drawLine(
                            brush = gradientBrush,
                            start = Offset(0f, lineY),
                            end = Offset(size.width, lineY),
                            strokeWidth = 20.dp.toPx(),
                            alpha = 0.22f * pulseAlpha
                        )

                        // Medium neon halo
                        drawLine(
                            brush = gradientBrush,
                            start = Offset(0f, lineY),
                            end = Offset(size.width, lineY),
                            strokeWidth = 9.dp.toPx(),
                            alpha = 0.48f * pulseAlpha
                        )

                        // Core bright neon edge line
                        drawLine(
                            brush = gradientBrush,
                            start = Offset(0f, lineY),
                            end = Offset(size.width, lineY),
                            strokeWidth = coreStrokePx,
                            alpha = 1.0f
                        )
                    }

                    // Bright flare accent at the leading edge (only while loading < 100%)
                    if (activeWidth < size.width) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFF3E0).copy(alpha = 0.95f * pulseAlpha),
                                    Color(0xFFFF9100).copy(alpha = 0.65f * pulseAlpha),
                                    Color.Transparent
                                ),
                                center = Offset(activeWidth, lineY),
                                radius = 7.dp.toPx()
                            )
                        )
                    }
                }
            }
        }
    }
}
