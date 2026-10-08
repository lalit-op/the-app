package com.example.oneread.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.data.DocumentItem
import com.example.oneread.ui.MainViewModel
import com.example.oneread.ui.components.ConfirmDeleteDialog
import com.example.oneread.ui.components.DocumentCard
import com.example.oneread.ui.components.FileInfoDialog
import com.example.oneread.ui.components.RenameDialog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favoriteDocs by viewModel.favorites.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var docToRename by remember { mutableStateOf<DocumentItem?>(null) }
    var docForInfo by remember { mutableStateOf<DocumentItem?>(null) }
    var docToDelete by remember { mutableStateOf<DocumentItem?>(null) }

    // Close in-place search on system back
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
        focusManager.clearFocus()
    }

    val searchResults = remember(allDocs, searchQuery) {
        if (searchQuery.isBlank()) {
            allDocs
        } else {
            val query = searchQuery.trim().lowercase()
            allDocs.filter { doc ->
                doc.title.lowercase().contains(query) ||
                doc.displayTitle.lowercase().contains(query) ||
                doc.extension.lowercase().contains(query) ||
                doc.directoryName.lowercase().contains(query) ||
                doc.fileType.name.lowercase().contains(query) ||
                doc.fileType.displayName.lowercase().contains(query) ||
                doc.path.lowercase().contains(query)
            }
        }
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
            // Header with Animated Transition between Default View and In-Place Search
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(150))
                },
                label = "fav_search_header_anim"
            ) { active ->
                if (!active) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Favorite Documents",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                letterSpacing = 0.3.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "HR READ • ALL IN ONE READER",
                                color = Color(0xFF8899AC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.6.sp
                            )
                        }

                        // Search button is ONLY shown if there are favorite documents
                        if (favoriteDocs.isNotEmpty()) {
                            val searchBtnInteraction = remember { MutableInteractionSource() }
                            val searchBtnPressed by searchBtnInteraction.collectIsPressedAsState()
                            val searchBtnScale by animateFloatAsState(
                                targetValue = if (searchBtnPressed) 0.90f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "fav_search_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .scale(searchBtnScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF131D33).copy(alpha = 0.85f))
                                    .border(
                                        BorderStroke(1.dp, Color(0xFF233252)),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(
                                        interactionSource = searchBtnInteraction,
                                        indication = ripple(bounded = true),
                                        onClick = { isSearchActive = true }
                                    )
                                    .testTag("favorites_search_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // In-Place Search Bar: Searches across file explorer without redirecting!
                    LaunchedEffect(Unit) {
                        searchFocusRequester.requestFocus()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                searchQuery = ""
                                focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("favorites_search_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close search",
                                tint = Color(0xFFF8FAFC)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            color = Color(0xFF131D33).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(0xFF233252))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search all documents...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color(0xFFF8FAFC),
                                            fontSize = 15.sp
                                        ),
                                        cursorBrush = SolidColor(Color(0xFF3B82F6)),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(searchFocusRequester)
                                            .testTag("favorites_search_input")
                                    )
                                }

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isSearchActive) {
                // Normal Favorites View
                if (favoriteDocs.isEmpty()) {
                    // Cosmic Empty State matching screenshot 1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CosmicEmptyFavoritesIllustration(modifier = Modifier.size(220.dp))

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "No Starred Documents",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Star documents from any list or viewer to\naccess them here quickly.",
                                color = Color(0xFF8899AC),
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = if (favoriteDocs.size == 1) "1 starred file" else "${favoriteDocs.size} starred files",
                                color = Color(0xFF8899AC),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(favoriteDocs, key = { "fav_${it.id}" }) { doc ->
                            DocumentCard(
                                document = doc,
                                modifier = Modifier.animateItem(),
                                onClick = { viewModel.openDocument(doc) },
                                onToggleFavorite = { viewModel.toggleFavorite(doc) },
                                onRename = { docToRename = doc },
                                onShare = { viewModel.repository.shareDocument(context, doc) },
                                onPrint = { viewModel.repository.printDocument(context, doc) },
                                onInfo = { docForInfo = doc },
                                onDelete = { docToDelete = doc }
                            )
                        }
                    }
                }
            } else {
                // Active In-Place Search across All Documents in File Explorer
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No matching documents",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No documents found for \"$searchQuery\" in file explorer.",
                                color = Color(0xFF8899AC),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) {
                                        "All documents in explorer (${searchResults.size})"
                                    } else {
                                        "Found ${searchResults.size} document${if (searchResults.size == 1) "" else "s"}"
                                    },
                                    color = Color(0xFF8899AC),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                if (searchQuery.isNotBlank()) {
                                    Text(
                                        text = "Searching all files",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        items(searchResults, key = { "fav_search_${it.id}" }) { doc ->
                            DocumentCard(
                                document = doc,
                                modifier = Modifier.animateItem(),
                                onClick = { viewModel.openDocument(doc) },
                                onToggleFavorite = { viewModel.toggleFavorite(doc) },
                                onRename = { docToRename = doc },
                                onShare = { viewModel.repository.shareDocument(context, doc) },
                                onPrint = { viewModel.repository.printDocument(context, doc) },
                                onInfo = { docForInfo = doc },
                                onDelete = { docToDelete = doc }
                            )
                        }
                    }
                }
            }
        }
    }


    // Dialogs
    docToRename?.let { doc ->
        RenameDialog(
            document = doc,
            onDismiss = { docToRename = null },
            onConfirm = { newName ->
                viewModel.renameDocument(doc, newName)
                docToRename = null
            }
        )
    }

    docForInfo?.let { doc ->
        FileInfoDialog(
            document = doc,
            onDismiss = { docForInfo = null }
        )
    }

    docToDelete?.let { doc ->
        ConfirmDeleteDialog(
            title = "Move to Recycle Bin?",
            message = "Move '${doc.title}' to the recycle bin? You can restore it anytime.",
            confirmText = "Move to Trash",
            onDismiss = { docToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(doc)
                docToDelete = null
            }
        )
    }
}

/**
 * Beautiful cosmic document and glowing star illustration matching Screenshot 1
 */
@Composable
private fun CosmicEmptyFavoritesIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_stars")
    val starGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_glow"
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )
    val twinkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_twinkle"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Ambient Nebula Glow with subtle breathing
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF142755).copy(alpha = 0.55f * starGlowPulse.coerceIn(0.8f, 1.2f)), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.48f * starGlowPulse
            )
        )

        // 2. Dashed Orbit Ellipse
        val orbitPath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    left = cx - w * 0.42f,
                    top = cy - h * 0.28f,
                    right = cx + w * 0.42f,
                    bottom = cy + h * 0.28f
                )
            )
        }
        drawPath(
            path = orbitPath,
            color = Color(0xFF283A62).copy(alpha = 0.70f),
            style = Stroke(
                width = 1.6.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )

        // 3. Orbiting Sparkle / Star points with smooth celestial motion
        val rad = (orbitAngle * PI / 180.0)
        val starX1 = (cx + (w * 0.42f) * cos(rad).toFloat())
        val starY1 = (cy + (h * 0.28f) * sin(rad).toFloat())
        val starX2 = (cx + (w * 0.42f) * cos(rad + PI).toFloat())
        val starY2 = (cy + (h * 0.28f) * sin(rad + PI).toFloat())

        drawCircle(
            color = Color(0xFFA5B4FC),
            radius = 2.5.dp.toPx(),
            center = Offset(starX1, starY1)
        )
        drawCircle(
            color = Color(0xFF67E8F9),
            radius = 2.dp.toPx(),
            center = Offset(starX2, starY2)
        )
        // 4-point sparkle star top-right with gentle twinkle
        drawSparkle(
            center = Offset(cx + w * 0.30f, cy - h * 0.22f),
            radius = 6.dp.toPx() * twinkleAlpha,
            color = Color(0xFFC7D2FE).copy(alpha = twinkleAlpha)
        )

        // 4. Frosted Document Sheet in the center
        val docW = w * 0.44f
        val docH = h * 0.52f
        val docLeft = cx - docW * 0.58f
        val docTop = cy - docH * 0.52f
        val foldSize = docW * 0.30f

        val docPath = Path().apply {
            moveTo(docLeft, docTop + 14.dp.toPx())
            // Top edge up to fold
            lineTo(docLeft, docTop + 14.dp.toPx())
            lineTo(docLeft, docTop + docH - 14.dp.toPx())
            quadraticTo(docLeft, docTop + docH, docLeft + 14.dp.toPx(), docTop + docH)
            lineTo(docLeft + docW - 14.dp.toPx(), docTop + docH)
            quadraticTo(docLeft + docW, docTop + docH, docLeft + docW, docTop + docH - 14.dp.toPx())
            lineTo(docLeft + docW, docTop + foldSize)
            lineTo(docLeft + docW - foldSize, docTop)
            lineTo(docLeft + 14.dp.toPx(), docTop)
            quadraticTo(docLeft, docTop, docLeft, docTop + 14.dp.toPx())
            close()
        }

        // Draw document background
        drawPath(
            path = docPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF22304A), Color(0xFF131D30)),
                start = Offset(docLeft, docTop),
                end = Offset(docLeft + docW, docTop + docH)
            )
        )
        // Draw document border
        drawPath(
            path = docPath,
            color = Color(0xFF33496E).copy(alpha = 0.8f),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Fold corner triangle
        val foldPath = Path().apply {
            moveTo(docLeft + docW - foldSize, docTop)
            lineTo(docLeft + docW - foldSize, docTop + foldSize)
            lineTo(docLeft + docW, docTop + foldSize)
            close()
        }
        drawPath(
            path = foldPath,
            color = Color(0xFF2C3E60)
        )
        drawPath(
            path = foldPath,
            color = Color(0xFF33496E),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Horizontal line bars on the document
        val lineStartX = docLeft + docW * 0.18f
        val lineY1 = docTop + docH * 0.38f
        val lineY2 = docTop + docH * 0.52f
        val lineY3 = docTop + docH * 0.66f

        drawRoundRect(
            color = Color(0xFF455A80).copy(alpha = 0.8f),
            topLeft = Offset(lineStartX, lineY1),
            size = Size(docW * 0.35f, 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF384B6E).copy(alpha = 0.8f),
            topLeft = Offset(lineStartX, lineY2),
            size = Size(docW * 0.50f, 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF384B6E).copy(alpha = 0.8f),
            topLeft = Offset(lineStartX, lineY3),
            size = Size(docW * 0.28f, 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )

        // 5. Radiant Glowing Electric Blue Star at bottom-right of document
        val starCenter = Offset(docLeft + docW * 0.86f, docTop + docH * 0.76f)
        val outerRadius = w * 0.16f
        val innerRadius = outerRadius * 0.44f

        // Star outer glow with gentle breathing
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00B0FF).copy(alpha = 0.55f * starGlowPulse.coerceIn(0.8f, 1.2f)), Color.Transparent),
                center = starCenter,
                radius = outerRadius * 1.6f * starGlowPulse
            )
        )

        // 5-point star polygon
        val starPath = createStarPath(starCenter, 5, outerRadius, innerRadius)
        drawPath(
            path = starPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB), Color(0xFF4F46E5)),
                start = Offset(starCenter.x - outerRadius, starCenter.y - outerRadius),
                end = Offset(starCenter.x + outerRadius, starCenter.y + outerRadius)
            )
        )
        drawPath(
            path = starPath,
            color = Color(0xFF7DD3FC).copy(alpha = 0.6f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkle(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path = path, color = color)
}

private fun createStarPath(
    center: Offset,
    points: Int,
    outerRadius: Float,
    innerRadius: Float
): Path {
    val path = Path()
    val angleStep = PI / points
    var angle = -PI / 2.0

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = (center.x + r * cos(angle)).toFloat()
        val y = (center.y + r * sin(angle)).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
        angle += angleStep
    }
    path.close()
    return path
}
