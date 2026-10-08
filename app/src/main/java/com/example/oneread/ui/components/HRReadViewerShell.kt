package com.example.oneread.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Shared Viewer Shell architecture across ALL document formats in HR Read.
 * Concept:
 * HRReadViewerShell
 *    ├── CommonViewerHeader
 *    ├── SearchController
 *    └── DocumentContent
 */
@Composable
fun HRReadViewerShell(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isHeaderVisible: Boolean = true,
    isFullscreen: Boolean = false,
    backgroundColor: Color = Color(0xFF0F172A),
    overlayContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val view = LocalView.current

    // Maintain dark status bar with light icons across all viewers in HR Read
    DisposableEffect(isFullscreen) {
        val window = (view.context as? Activity)?.window
        val prevLightBars = window?.let { WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars } ?: false
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.isAppearanceLightStatusBars = false
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.isAppearanceLightStatusBars = prevLightBars
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Format-specific Document Content
        content()

        // Pinned True Edge-to-Edge Common Viewer Header
        AnimatedVisibility(
            visible = isHeaderVisible && !isFullscreen,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            header()
        }

        // Additional overlays (bottom bars, dialogs, page indicators)
        overlayContent?.invoke(this)
    }
}
