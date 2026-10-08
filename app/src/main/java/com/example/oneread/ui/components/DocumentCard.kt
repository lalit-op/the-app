package com.example.oneread.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oneread.data.DocumentItem
import com.example.oneread.data.DocumentType

@Composable
fun DocumentCard(
    document: DocumentItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isPressed by cardInteractionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_press_scale"
    )

    val starScale by animateFloatAsState(
        targetValue = if (document.isFavorite) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "star_scale"
    )

    val starColor by animateColorAsState(
        targetValue = if (document.isFavorite) Color(0xFFFBBF24) else Color(0xFF64748B),
        animationSpec = tween(220),
        label = "star_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clickable(
                interactionSource = cardInteractionSource,
                indication = ripple(),
                onClick = onClick
            )
            .testTag("document_card_${document.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0C1322)
        ),
        border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Format Icon Badge with password protected symbol
            DocumentBadge(
                fileType = document.fileType,
                size = 44,
                isPasswordProtected = document.isPasswordProtected,
                extension = document.extension
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Meta Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = document.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (document.isPasswordProtected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEAB308).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.65f)),
                            modifier = Modifier.testTag("password_protected_badge_${document.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Password protected",
                                    tint = Color(0xFFFACC15),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "PROTECTED",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFACC15)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val metaText = buildString {
                    if (document.isPasswordProtected) {
                        append("Password Protected • ")
                    }
                    if (document.extension.isNotBlank()) {
                        append("${document.extension.uppercase()} • ")
                    }
                    append(document.formattedSize)
                    append(" • ")
                    append(document.formattedDate)
                    if (document.fileType == DocumentType.PDF && document.pageCount > 0) {
                        append(" • ")
                        append("${document.pageCount} pgs")
                    }
                }

                Text(
                    text = metaText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = if (document.isPasswordProtected) Color(0xFFFACC15).copy(alpha = 0.85f) else Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite Button [ ☆ ] / [ ★ ] with animated bounce and color
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("favorite_button_${document.id}")
            ) {
                Icon(
                    imageVector = if (document.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (document.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = starColor,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(starScale)
                )
            }

            // Three-dot File Menu [ ⋮ ]
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("more_options_button_${document.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More actions",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF0F172A))
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = Color(0xFFF1F5F9)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = Color(0xFFF1F5F9)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("File Info", color = Color(0xFFF1F5F9)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onInfo()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFEF4444)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Format-specific icon badge matching the reference:
 * - PDF: Red PDF document icon
 * - Word: Blue Word icon with W
 * - Excel: Green Excel icon with X
 * - PowerPoint: Orange PowerPoint icon with P
 * - Text: Neutral document/text icon
 */
@Composable
fun DocumentBadge(
    fileType: DocumentType,
    size: Int = 44,
    isPasswordProtected: Boolean = false,
    extension: String = ""
) {
    Box(contentAlignment = Alignment.BottomEnd) {
        val sizeDp: Dp = size.dp
        when (fileType) {
        DocumentType.PDF -> {
            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .background(Color(0xFF380C10), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF icon",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size((size * 0.46f).dp)
                    )
                    Text(
                        text = "PDF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
        DocumentType.WORD -> {
            val isRtf = extension.equals("rtf", ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .background(if (isRtf) Color(0xFF1E1B4B) else Color(0xFF0C1E45), RoundedCornerShape(10.dp))
                    .border(
                        BorderStroke(1.dp, if (isRtf) Color(0xFF818CF8).copy(alpha = 0.6f) else Color(0xFF3B82F6).copy(alpha = 0.5f)),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((size * 0.70f).dp)
                        .background(if (isRtf) Color(0xFF4F46E5) else Color(0xFF2563EB), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRtf) "RTF" else "W",
                        fontSize = if (isRtf) (size * 0.28f).sp else (size * 0.42f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
        DocumentType.EXCEL -> {
            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .background(Color(0xFF062A17), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((size * 0.70f).dp)
                        .background(Color(0xFF059669), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "X",
                        fontSize = (size * 0.42f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
        DocumentType.PPT -> {
            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .background(Color(0xFF3B1506), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.5f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((size * 0.70f).dp)
                        .background(Color(0xFFEA580C), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        fontSize = (size * 0.42f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
        DocumentType.TXT -> {
            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .background(Color(0xFF151D2C), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.5f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Article,
                        contentDescription = "Text icon",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size((size * 0.46f).dp)
                    )
                    Text(
                        text = "TXT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
        DocumentType.ALL -> {
            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .background(Color(0xFF161F38), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.5f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Document icon",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size((size * 0.55f).dp)
                )
            }
        }
    }

    if (isPasswordProtected) {
        Box(
            modifier = Modifier
                .size((size * 0.44f).coerceAtLeast(17f).dp)
                .offset(x = 3.dp, y = 3.dp)
                .background(Color(0xFFEAB308), CircleShape)
                .border(1.5.dp, Color(0xFF0C1322), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Password protected file",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size((size * 0.26f).coerceAtLeast(10f).dp)
            )
        }
    }
}
}

/**
 * Small format icon used inside the filter chips
 */
@Composable
fun FilterChipMiniIcon(fileType: DocumentType, isSelected: Boolean) {
    when (fileType) {
        DocumentType.ALL -> {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF93C5FD),
                modifier = Modifier.size(15.dp)
            )
        }
        DocumentType.PDF -> {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFFEF4444), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PDF",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
        DocumentType.WORD -> {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFF2563EB), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "W",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
        DocumentType.EXCEL -> {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFF10B981), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
        DocumentType.PPT -> {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFFF97316), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
        DocumentType.TXT -> {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFF64748B), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "T",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}
