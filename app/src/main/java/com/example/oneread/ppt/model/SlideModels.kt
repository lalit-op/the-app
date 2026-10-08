package com.example.oneread.ppt.model

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.graphics.Color

/**
 * High-fidelity domain model for PowerPoint presentations (PPT and PPTX).
 */
data class PresentationModel(
    val title: String,
    val slideWidth: Float = 16f,
    val slideHeight: Float = 9f,
    val aspectRatio: Float = 16f / 9f,
    val slides: List<SlideModel> = emptyList(),
    val themeColors: Map<String, Color> = emptyMap(),
    val isModified: Boolean = false
) {
    fun duplicateSlide(index: Int): PresentationModel {
        if (index !in slides.indices) return this
        val source = slides[index]
        val duplicated = source.copy(
            index = index + 1,
            slideNumber = index + 2,
            title = if (source.title.isNotBlank()) "${source.title} (Copy)" else "Slide ${index + 2}"
        )
        val newSlides = slides.toMutableList()
        newSlides.add(index + 1, duplicated)
        // Renumber slides
        val renumbered = newSlides.mapIndexed { idx, slide ->
            slide.copy(index = idx, slideNumber = idx + 1)
        }
        return copy(slides = renumbered, isModified = true)
    }

    fun deleteSlide(index: Int): PresentationModel {
        if (slides.size <= 1 || index !in slides.indices) return this
        val newSlides = slides.toMutableList()
        newSlides.removeAt(index)
        val renumbered = newSlides.mapIndexed { idx, slide ->
            slide.copy(index = idx, slideNumber = idx + 1)
        }
        return copy(slides = renumbered, isModified = true)
    }

    fun moveSlide(fromIndex: Int, toIndex: Int): PresentationModel {
        if (fromIndex !in slides.indices || toIndex !in slides.indices || fromIndex == toIndex) return this
        val newSlides = slides.toMutableList()
        val item = newSlides.removeAt(fromIndex)
        newSlides.add(toIndex, item)
        val renumbered = newSlides.mapIndexed { idx, slide ->
            slide.copy(index = idx, slideNumber = idx + 1)
        }
        return copy(slides = renumbered, isModified = true)
    }

    fun addSlide(title: String, body: String, atIndex: Int = slides.size): PresentationModel {
        val targetIdx = atIndex.coerceIn(0, slides.size)
        val titleElement = TextElement(
            bounds = RectF(0.08f, 0.12f, 0.92f, 0.28f),
            zIndex = 1,
            paragraphs = listOf(
                SlideParagraph(
                    alignment = TextAlignment.LEFT,
                    runs = listOf(
                        SlideTextRun(
                            text = title.ifBlank { "New Slide" },
                            fontSizePt = 28f,
                            isBold = true,
                            color = Color(0xFF0F172A)
                        )
                    )
                )
            )
        )

        val elements = mutableListOf<SlideElement>(titleElement)
        if (body.isNotBlank()) {
            val bodyElement = TextElement(
                bounds = RectF(0.08f, 0.32f, 0.92f, 0.85f),
                zIndex = 2,
                paragraphs = body.split("\n").map { line ->
                    SlideParagraph(
                        alignment = TextAlignment.LEFT,
                        isBullet = true,
                        runs = listOf(
                            SlideTextRun(
                                text = line,
                                fontSizePt = 18f,
                                color = Color(0xFF334155)
                            )
                        )
                    )
                }
            )
            elements.add(bodyElement)
        }

        val newSlide = SlideModel(
            index = targetIdx,
            slideNumber = targetIdx + 1,
            title = title.ifBlank { "Slide ${targetIdx + 1}" },
            backgroundColor = Color.White,
            elements = elements,
            fullSearchableText = "$title $body".trim()
        )

        val newSlides = slides.toMutableList()
        newSlides.add(targetIdx, newSlide)
        val renumbered = newSlides.mapIndexed { idx, slide ->
            slide.copy(index = idx, slideNumber = idx + 1)
        }
        return copy(slides = renumbered, isModified = true)
    }

    fun updateNotes(slideIndex: Int, newNotes: String): PresentationModel {
        if (slideIndex !in slides.indices) return this
        val newSlides = slides.toMutableList()
        val slide = newSlides[slideIndex]
        newSlides[slideIndex] = slide.copy(
            notes = newNotes,
            fullSearchableText = "${slide.fullSearchableText} $newNotes".trim()
        )
        return copy(slides = newSlides, isModified = true)
    }
}

data class SlideModel(
    val index: Int,
    val slideNumber: Int,
    val title: String = "",
    val backgroundColor: Color = Color.White,
    val backgroundImage: Bitmap? = null,
    val elements: List<SlideElement> = emptyList(),
    val notes: String = "",
    val fullSearchableText: String = ""
)

sealed interface SlideElement {
    val bounds: RectF // Normalized 0f..1f relative to slide width & height
    val zIndex: Int
}

enum class ShapeType {
    RECTANGLE,
    ROUNDED_RECTANGLE,
    LINE,
    ELLIPSE,
    CONNECTOR
}

data class ShapeElement(
    override val bounds: RectF,
    override val zIndex: Int,
    val shapeType: ShapeType = ShapeType.RECTANGLE,
    val cornerRadius: Float = 0f,
    val fillColor: Color? = null,
    val strokeColor: Color? = null,
    val strokeWidth: Float = 0f,
    val rotation: Float = 0f
) : SlideElement

data class ImageElement(
    override val bounds: RectF,
    override val zIndex: Int,
    val bitmap: Bitmap,
    val rotation: Float = 0f
) : SlideElement

data class TextElement(
    override val bounds: RectF,
    override val zIndex: Int,
    val paragraphs: List<SlideParagraph> = emptyList(),
    val backgroundColor: Color? = null,
    val borderColor: Color? = null,
    val borderWidth: Float = 0f,
    val verticalAnchor: VerticalAnchor = VerticalAnchor.TOP
) : SlideElement

enum class VerticalAnchor {
    TOP,
    CENTER,
    BOTTOM
}

enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

data class SlideParagraph(
    val alignment: TextAlignment = TextAlignment.LEFT,
    val runs: List<SlideTextRun> = emptyList(),
    val isBullet: Boolean = false
) {
    val plainText: String
        get() = runs.joinToString("") { it.text }
}

data class SlideTextRun(
    val text: String,
    val fontSizePt: Float = 14f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val color: Color = Color(0xFF1E293B),
    val fontFamily: String? = null
)

data class TableElement(
    override val bounds: RectF,
    override val zIndex: Int,
    val rows: List<SlideTableRow> = emptyList()
) : SlideElement

data class SlideTableRow(
    val cells: List<SlideTableCell> = emptyList()
)

data class SlideTableCell(
    val text: String,
    val fillColor: Color? = null,
    val textColor: Color = Color(0xFF1E293B),
    val isBold: Boolean = false,
    val alignment: TextAlignment = TextAlignment.LEFT
)
