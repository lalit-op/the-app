package com.example.oneread.word.model

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

/**
 * High-fidelity domain model representing a structured Word (DOCX / OpenXML) document.
 */
data class DocxDocument(
    val title: String,
    val headerElements: List<DocxBlock> = emptyList(),
    val bodyElements: List<DocxBlock> = emptyList(),
    val footerElements: List<DocxBlock> = emptyList(),
    val mediaMap: Map<String, Bitmap> = emptyMap(),
    val pageWidthPt: Float = 595.28f, // A4 standard width
    val pageHeightPt: Float = 841.89f, // A4 standard height
    val marginLeftPt: Float = 54f,
    val marginRightPt: Float = 54f,
    val marginTopPt: Float = 54f,
    val marginBottomPt: Float = 54f
) {
    val allBlocks: List<DocxBlock>
        get() = headerElements + bodyElements + footerElements

    val isEmpty: Boolean
        get() = allBlocks.isEmpty()
}

/**
 * Base building block of a Word document.
 */
sealed class DocxBlock {
    data class Paragraph(
        val runs: List<DocxRun>,
        val alignment: TextAlign = TextAlign.Start,
        val spaceBeforeDp: Float = 0f,
        val spaceAfterDp: Float = 4f,
        val isHeading: Boolean = false,
        val headingLevel: Int = 0,
        val indentStartDp: Float = 0f,
        val hasBottomBorder: Boolean = false
    ) : DocxBlock() {
        val fullText: String
            get() = runs.joinToString("") { it.text }
    }

    data class Table(
        val rows: List<DocxTableRow>,
        val colWidthWeights: List<Float> = emptyList(),
        val hasBorders: Boolean = true,
        val borderColor: Color = Color(0xFFCBD5E1)
    ) : DocxBlock() {
        val allText: String
            get() = rows.joinToString(" ") { r ->
                r.cells.joinToString(" ") { c -> c.fullText }
            }
    }

    data class ImageBlock(
        val bitmap: Bitmap,
        val widthDp: Float = 240f,
        val heightDp: Float = 120f,
        val alignment: TextAlign = TextAlign.Center,
        val description: String = "Document image"
    ) : DocxBlock()

    data class HorizontalDivider(
        val color: Color = Color(0xFFE2E8F0),
        val thicknessDp: Float = 1f
    ) : DocxBlock()
}

/**
 * A row in a structured Word table.
 */
typealias DocxTable = DocxBlock.Table

data class DocxTableRow(
    val cells: List<DocxTableCell>,
    val isHeaderRow: Boolean = false
)

/**
 * A single cell in a Word table containing structured blocks.
 */
data class DocxTableCell(
    val blocks: List<DocxBlock>,
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
    val bgColor: Color? = null,
    val hasBorders: Boolean = true,
    val borderColor: Color = Color(0xFFCBD5E1),
    val alignment: TextAlign = TextAlign.Start
) {
    val fullText: String
        get() = blocks.joinToString(" ") { block ->
            when (block) {
                is DocxBlock.Paragraph -> block.fullText
                is DocxBlock.Table -> block.allText
                else -> ""
            }
        }
}

/**
 * Formatted text run within a paragraph.
 */
data class DocxRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrike: Boolean = false,
    val fontSizeSp: Float = 14f,
    val color: Color? = null,
    val highlightColor: Color? = null,
    val inlineImage: Bitmap? = null
)
