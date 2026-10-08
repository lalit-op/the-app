package com.example.oneread.ppt.render

import android.graphics.*
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import com.example.oneread.ppt.model.*
import kotlin.math.roundToInt

object SlideRasterizer {

    /**
     * Renders a PowerPoint slide to a crisp high-resolution Bitmap matching original slide layout.
     */
    fun renderSlide(
        slide: SlideModel,
        targetWidth: Int = 1920,
        targetHeight: Int = 1080,
        searchQuery: String = "",
        highlightBounds: RectF? = null
    ): Bitmap {
        val width = targetWidth.coerceAtLeast(320)
        val height = targetHeight.coerceAtLeast(180)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Slide Background (Default White #FFFFFF, or slide/layout specific color/image)
        if (slide.backgroundImage != null) {
            val src = Rect(0, 0, slide.backgroundImage.width, slide.backgroundImage.height)
            val dst = Rect(0, 0, width, height)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(slide.backgroundImage, src, dst, bgPaint)
        } else {
            val bgPaint = Paint().apply {
                color = slide.backgroundColor.toArgb()
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        }

        // Relative scale factor for typography based on 1080p reference height
        val scaleFactor = height.toFloat() / 1080f

        // 2. Draw Visual Elements in Z-Index Order
        for (element in slide.elements) {
            val left = element.bounds.left * width
            val top = element.bounds.top * height
            val right = element.bounds.right * width
            val bottom = element.bounds.bottom * height
            val elemRect = RectF(left, top, right, bottom)

            when (element) {
                is ShapeElement -> {
                    drawShape(canvas, element, elemRect, scaleFactor)
                }
                is ImageElement -> {
                    drawImage(canvas, element, elemRect)
                }
                is TextElement -> {
                    drawTextElement(canvas, element, elemRect, scaleFactor, searchQuery)
                }
                is TableElement -> {
                    drawTable(canvas, element, elemRect, scaleFactor)
                }
            }
        }

        // 3. Optional Explicit Search Match Highlight Overlay
        if (highlightBounds != null) {
            val hlRect = RectF(
                highlightBounds.left * width,
                highlightBounds.top * height,
                highlightBounds.right * width,
                highlightBounds.bottom * height
            )
            val hlFill = Paint().apply {
                color = android.graphics.Color.argb(140, 250, 204, 21) // Glowing yellow
                style = Paint.Style.FILL
            }
            val hlStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(255, 107, 0) // Deep orange border
                style = Paint.Style.STROKE
                strokeWidth = 3f * scaleFactor
            }
            canvas.drawRoundRect(hlRect, 6f * scaleFactor, 6f * scaleFactor, hlFill)
            canvas.drawRoundRect(hlRect, 6f * scaleFactor, 6f * scaleFactor, hlStroke)
        }

        return bitmap
    }

    private fun drawShape(canvas: Canvas, shape: ShapeElement, rect: RectF, scaleFactor: Float) {
        if (shape.rotation != 0f) {
            canvas.save()
            canvas.rotate(shape.rotation, rect.centerX(), rect.centerY())
        }

        val fillPaint = shape.fillColor?.let {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = it.toArgb()
                style = Paint.Style.FILL
            }
        }

        val strokePaint = if (shape.strokeColor != null && shape.strokeWidth > 0f) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = shape.strokeColor.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = (shape.strokeWidth * scaleFactor).coerceAtLeast(1f)
            }
        } else null

        when (shape.shapeType) {
            ShapeType.LINE, ShapeType.CONNECTOR -> {
                val linePaint = strokePaint ?: fillPaint ?: Paint().apply {
                    color = android.graphics.Color.rgb(22, 163, 74) // Green
                    strokeWidth = 2f * scaleFactor
                }
                canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), linePaint)
            }
            ShapeType.ROUNDED_RECTANGLE -> {
                val cr = (shape.cornerRadius * scaleFactor).coerceAtLeast(8f)
                fillPaint?.let { canvas.drawRoundRect(rect, cr, cr, it) }
                strokePaint?.let { canvas.drawRoundRect(rect, cr, cr, it) }
            }
            ShapeType.ELLIPSE -> {
                fillPaint?.let { canvas.drawOval(rect, it) }
                strokePaint?.let { canvas.drawOval(rect, it) }
            }
            ShapeType.RECTANGLE -> {
                fillPaint?.let { canvas.drawRect(rect, it) }
                strokePaint?.let { canvas.drawRect(rect, it) }
            }
        }

        if (shape.rotation != 0f) {
            canvas.restore()
        }
    }

    private fun drawImage(canvas: Canvas, image: ImageElement, rect: RectF) {
        if (image.rotation != 0f) {
            canvas.save()
            canvas.rotate(image.rotation, rect.centerX(), rect.centerY())
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val src = Rect(0, 0, image.bitmap.width, image.bitmap.height)
        canvas.drawBitmap(image.bitmap, src, rect, paint)
        if (image.rotation != 0f) {
            canvas.restore()
        }
    }

    private fun drawTextElement(
        canvas: Canvas,
        element: TextElement,
        rect: RectF,
        scaleFactor: Float,
        searchQuery: String
    ) {
        // Draw optional background fill or border of the text box
        element.backgroundColor?.let {
            val bgPaint = Paint().apply {
                color = it.toArgb()
                style = Paint.Style.FILL
            }
            canvas.drawRect(rect, bgPaint)
        }
        if (element.borderColor != null && element.borderWidth > 0f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = element.borderColor.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = element.borderWidth * scaleFactor
            }
            canvas.drawRect(rect, borderPaint)
        }

        // Layout and draw paragraphs
        var yCursor = rect.top + 8f * scaleFactor

        for (para in element.paragraphs) {
            if (para.runs.isEmpty()) {
                yCursor += 16f * scaleFactor
                continue
            }

            val maxPt = para.runs.maxOfOrNull { it.fontSizePt } ?: 14f
            val baseTextSize = (maxPt * 1.6f * scaleFactor).coerceAtLeast(10f)
            val lineHeight = baseTextSize * 1.3f

            // Check if paragraph fits within remaining height
            if (yCursor + lineHeight > rect.bottom + 20f * scaleFactor) break

            // Calculate total width of paragraph line to determine starting X for alignment
            var totalParaWidth = 0f
            val tempPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            for (run in para.runs) {
                tempPaint.textSize = (run.fontSizePt * 1.6f * scaleFactor).coerceAtLeast(10f)
                tempPaint.typeface = if (run.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                totalParaWidth += tempPaint.measureText(run.text)
            }

            var xCursor = when (para.alignment) {
                TextAlignment.CENTER -> (rect.centerX() - totalParaWidth / 2f).coerceAtLeast(rect.left)
                TextAlignment.RIGHT -> (rect.right - totalParaWidth).coerceAtLeast(rect.left)
                else -> rect.left
            }

            for (run in para.runs) {
                val runPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = run.color.toArgb()
                    textSize = (run.fontSizePt * 1.6f * scaleFactor).coerceAtLeast(10f)
                    typeface = when {
                        run.isBold && run.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                        run.isBold -> Typeface.DEFAULT_BOLD
                        run.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                        else -> Typeface.DEFAULT
                    }
                    isUnderlineText = run.isUnderline
                }

                val runWidth = runPaint.measureText(run.text)

                // Highlight matching search text
                if (searchQuery.isNotBlank() && run.text.contains(searchQuery, ignoreCase = true)) {
                    val matchPaint = Paint().apply {
                        color = android.graphics.Color.argb(160, 250, 204, 21) // Gold highlight
                        style = Paint.Style.FILL
                    }
                    val fm = runPaint.fontMetrics
                    val hlTop = yCursor + fm.ascent
                    val hlBottom = yCursor + fm.descent
                    canvas.drawRect(xCursor, hlTop, xCursor + runWidth, hlBottom, matchPaint)
                }

                canvas.drawText(run.text, xCursor, yCursor + baseTextSize * 0.85f, runPaint)
                xCursor += runWidth
            }

            yCursor += lineHeight
        }
    }

    private fun drawTable(canvas: Canvas, table: TableElement, rect: RectF, scaleFactor: Float) {
        val rowCount = table.rows.size.coerceAtLeast(1)
        val rowHeight = rect.height() / rowCount

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(203, 213, 225) // Light gray border
            style = Paint.Style.STROKE
            strokeWidth = 1f * scaleFactor
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(30, 41, 59)
            textSize = (13f * 1.5f * scaleFactor).coerceAtLeast(10f)
            typeface = Typeface.DEFAULT
        }

        table.rows.forEachIndexed { rIdx, row ->
            val cellCount = row.cells.size.coerceAtLeast(1)
            val cellWidth = rect.width() / cellCount
            val rTop = rect.top + rIdx * rowHeight
            val rBottom = rTop + rowHeight

            row.cells.forEachIndexed { cIdx, cell ->
                val cLeft = rect.left + cIdx * cellWidth
                val cRight = cLeft + cellWidth
                val cellRect = RectF(cLeft, rTop, cRight, rBottom)

                // Fill cell background
                if (cell.fillColor != null) {
                    val fillPaint = Paint().apply {
                        color = cell.fillColor.toArgb()
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(cellRect, fillPaint)
                }

                // Cell border
                canvas.drawRect(cellRect, borderPaint)

                // Cell text
                if (cell.text.isNotBlank()) {
                    textPaint.color = cell.textColor.toArgb()
                    textPaint.typeface = if (cell.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    val textY = cellRect.centerY() + textPaint.textSize * 0.35f
                    canvas.drawText(cell.text, cellRect.left + 6f * scaleFactor, textY, textPaint)
                }
            }
        }
    }
}
