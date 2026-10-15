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

        // 1. Draw Slide Background (Image, Gradient, or Solid Color)
        if (slide.backgroundImage != null && !slide.backgroundImage.isRecycled) {
            val src = Rect(0, 0, slide.backgroundImage.width, slide.backgroundImage.height)
            val dst = Rect(0, 0, width, height)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(slide.backgroundImage, src, dst, bgPaint)
        } else if (slide.gradientEndColor != null) {
            val gradPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    slide.backgroundColor.toArgb(),
                    slide.gradientEndColor.toArgb(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradPaint)
        } else {
            val bgPaint = Paint().apply {
                color = slide.backgroundColor.toArgb()
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        }

        // Exact points-to-pixels ratio based on slide height in EMUs (1 pt = 12700 EMUs)
        val slideHeightPt = (slide.slideHeightEmus / 12700f).coerceIn(360f, 1200f)
        val pxPerPt = height.toFloat() / slideHeightPt
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
                    drawTextElement(canvas, element, elemRect, scaleFactor, pxPerPt, searchQuery)
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
                strokeWidth = (shape.strokeWidth * scaleFactor).coerceAtLeast(1.5f * scaleFactor)
            }
        } else null

        when (shape.shapeType) {
            ShapeType.LINE, ShapeType.CONNECTOR -> {
                val linePaint = strokePaint ?: fillPaint ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(30, 41, 59)
                    strokeWidth = 2.5f * scaleFactor
                }
                canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), linePaint)
            }
            ShapeType.LEFT_BRACE -> {
                val bracePaint = strokePaint ?: fillPaint ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(30, 41, 59)
                    strokeWidth = 2.5f * scaleFactor
                    style = Paint.Style.STROKE
                }
                bracePaint.style = Paint.Style.STROKE
                val path = Path().apply {
                    val midX = rect.left + rect.width() * 0.45f
                    val tipX = rect.left
                    val rightX = rect.right
                    val topY = rect.top
                    val botY = rect.bottom
                    val midY = rect.centerY()
                    val cornerH = (rect.height() * 0.08f).coerceAtMost(24f * scaleFactor)

                    moveTo(rightX, topY)
                    cubicTo(midX, topY, midX, topY + cornerH, midX, topY + cornerH)
                    lineTo(midX, midY - cornerH)
                    cubicTo(midX, midY, tipX, midY, tipX, midY)
                    cubicTo(midX, midY, midX, midY + cornerH, midX, midY + cornerH)
                    lineTo(midX, botY - cornerH)
                    cubicTo(midX, botY, rightX, botY, rightX, botY)
                }
                canvas.drawPath(path, bracePaint)
            }
            ShapeType.RIGHT_BRACE -> {
                val bracePaint = strokePaint ?: fillPaint ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(30, 41, 59)
                    strokeWidth = 2.5f * scaleFactor
                    style = Paint.Style.STROKE
                }
                bracePaint.style = Paint.Style.STROKE
                val path = Path().apply {
                    val midX = rect.right - rect.width() * 0.45f
                    val tipX = rect.right
                    val leftX = rect.left
                    val topY = rect.top
                    val botY = rect.bottom
                    val midY = rect.centerY()
                    val cornerH = (rect.height() * 0.08f).coerceAtMost(24f * scaleFactor)

                    moveTo(leftX, topY)
                    cubicTo(midX, topY, midX, topY + cornerH, midX, topY + cornerH)
                    lineTo(midX, midY - cornerH)
                    cubicTo(midX, midY, tipX, midY, tipX, midY)
                    cubicTo(midX, midY, midX, midY + cornerH, midX, midY + cornerH)
                    lineTo(midX, botY - cornerH)
                    cubicTo(midX, botY, leftX, botY, leftX, botY)
                }
                canvas.drawPath(path, bracePaint)
            }
            ShapeType.LEFT_BRACKET -> {
                val bracketPaint = strokePaint ?: fillPaint ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(30, 41, 59)
                    strokeWidth = 2.5f * scaleFactor
                    style = Paint.Style.STROKE
                }
                bracketPaint.style = Paint.Style.STROKE
                val path = Path().apply {
                    moveTo(rect.right, rect.top)
                    lineTo(rect.left, rect.top)
                    lineTo(rect.left, rect.bottom)
                    lineTo(rect.right, rect.bottom)
                }
                canvas.drawPath(path, bracketPaint)
            }
            ShapeType.RIGHT_BRACKET -> {
                val bracketPaint = strokePaint ?: fillPaint ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(30, 41, 59)
                    strokeWidth = 2.5f * scaleFactor
                    style = Paint.Style.STROKE
                }
                bracketPaint.style = Paint.Style.STROKE
                val path = Path().apply {
                    moveTo(rect.left, rect.top)
                    lineTo(rect.right, rect.top)
                    lineTo(rect.right, rect.bottom)
                    lineTo(rect.left, rect.bottom)
                }
                canvas.drawPath(path, bracketPaint)
            }
            ShapeType.DIAMOND -> {
                val path = Path().apply {
                    moveTo(rect.centerX(), rect.top)
                    lineTo(rect.right, rect.centerY())
                    lineTo(rect.centerX(), rect.bottom)
                    lineTo(rect.left, rect.centerY())
                    close()
                }
                fillPaint?.let { canvas.drawPath(path, it) }
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            ShapeType.ROUNDED_RECTANGLE -> {
                val cr = (shape.cornerRadius * scaleFactor).coerceAtLeast(8f * scaleFactor)
                fillPaint?.let { canvas.drawRoundRect(rect, cr, cr, it) }
                strokePaint?.let { canvas.drawRoundRect(rect, cr, cr, it) }
            }
            ShapeType.ELLIPSE -> {
                fillPaint?.let { canvas.drawOval(rect, it) }
                strokePaint?.let { canvas.drawOval(rect, it) }
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(rect.centerX(), rect.top)
                    lineTo(rect.right, rect.bottom)
                    lineTo(rect.left, rect.bottom)
                    close()
                }
                fillPaint?.let { canvas.drawPath(path, it) }
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            ShapeType.ARROW -> {
                val headW = rect.width() * 0.3f
                val shaftH = rect.height() * 0.45f
                val shaftTop = rect.centerY() - shaftH / 2f
                val shaftBottom = rect.centerY() + shaftH / 2f
                val path = Path().apply {
                    moveTo(rect.left, shaftTop)
                    lineTo(rect.right - headW, shaftTop)
                    lineTo(rect.right - headW, rect.top)
                    lineTo(rect.right, rect.centerY())
                    lineTo(rect.right - headW, rect.bottom)
                    lineTo(rect.right - headW, shaftBottom)
                    lineTo(rect.left, shaftBottom)
                    close()
                }
                fillPaint?.let { canvas.drawPath(path, it) }
                strokePaint?.let { canvas.drawPath(path, it) }
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
        if (image.bitmap.isRecycled) return
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

    private data class WordRun(
        val word: String,
        val run: SlideTextRun,
        val width: Float
    )

    private data class MeasuredLine(
        val words: List<WordRun>,
        val lineWidth: Float,
        val lineHeight: Float,
        val isFirstLineOfBullet: Boolean = false,
        val bulletChar: String = "•",
        val bulletIndent: Float = 0f,
        val alignment: TextAlignment = TextAlignment.LEFT
    )

    private fun drawTextElement(
        canvas: Canvas,
        element: TextElement,
        rect: RectF,
        scaleFactor: Float,
        pxPerPt: Float,
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
                strokeWidth = (element.borderWidth * scaleFactor).coerceAtLeast(1f)
            }
            canvas.drawRect(rect, borderPaint)
        }

        val paddingX = 4f * scaleFactor
        val paddingY = 4f * scaleFactor
        val maxAvailableWidth = (rect.width() - 2 * paddingX).coerceAtLeast(20f)
        val measuredLines = mutableListOf<MeasuredLine>()

        // 1. Line breaking with Word-Wrapping across all paragraphs
        val tempPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        for (para in element.paragraphs) {
            if (para.runs.isEmpty() || para.runs.all { it.text.isEmpty() }) {
                // Empty paragraph spacer
                measuredLines.add(
                    MeasuredLine(
                        words = emptyList(),
                        lineWidth = 0f,
                        lineHeight = 14f * pxPerPt * 0.7f,
                        alignment = para.alignment
                    )
                )
                continue
            }

            val bulletIndent = if (para.isBullet) (16f + para.bulletLevel * 12f) * pxPerPt else 0f
            val effectiveLineWidth = (maxAvailableWidth - bulletIndent).coerceAtLeast(15f)

            // Split into word tokens
            val wordsInPara = mutableListOf<WordRun>()
            for (run in para.runs) {
                tempPaint.textSize = (run.fontSizePt * pxPerPt).coerceAtLeast(8f * scaleFactor)
                tempPaint.typeface = when {
                    run.isBold && run.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                    run.isBold -> Typeface.DEFAULT_BOLD
                    run.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    else -> Typeface.DEFAULT
                }

                // Preserve individual words and spacing
                val tokens = run.text.split(Regex("(?<=\\s)|(?=\\s)"))
                for (token in tokens) {
                    if (token.isEmpty()) continue
                    val w = tempPaint.measureText(token)
                    wordsInPara.add(WordRun(token, run, w))
                }
            }

            // Group tokens into wrapped lines
            var currentLineWords = mutableListOf<WordRun>()
            var currentLineWidth = 0f
            var maxRunSizeInLine = 12f
            var isFirstLine = true

            for (wr in wordsInPara) {
                // Check if adding this token overflows line
                if (currentLineWidth + wr.width > effectiveLineWidth && currentLineWords.isNotEmpty()) {
                    val lHeight = (maxRunSizeInLine * pxPerPt * 1.25f).coerceAtLeast(10f * scaleFactor)
                    measuredLines.add(
                        MeasuredLine(
                            words = currentLineWords,
                            lineWidth = currentLineWidth,
                            lineHeight = lHeight,
                            isFirstLineOfBullet = isFirstLine && para.isBullet,
                            bulletChar = para.bulletChar,
                            bulletIndent = bulletIndent,
                            alignment = para.alignment
                        )
                    )
                    isFirstLine = false
                    currentLineWords = mutableListOf()
                    currentLineWidth = 0f
                    maxRunSizeInLine = 12f
                }

                // Skip leading whitespace on wrapped lines
                if (currentLineWords.isEmpty() && wr.word.isBlank()) {
                    continue
                }

                currentLineWords.add(wr)
                currentLineWidth += wr.width
                if (wr.run.fontSizePt > maxRunSizeInLine) {
                    maxRunSizeInLine = wr.run.fontSizePt
                }
            }

            if (currentLineWords.isNotEmpty()) {
                val lHeight = (maxRunSizeInLine * pxPerPt * 1.25f).coerceAtLeast(10f * scaleFactor)
                measuredLines.add(
                    MeasuredLine(
                        words = currentLineWords,
                        lineWidth = currentLineWidth,
                        lineHeight = lHeight,
                        isFirstLineOfBullet = isFirstLine && para.isBullet,
                        bulletChar = para.bulletChar,
                        bulletIndent = bulletIndent,
                        alignment = para.alignment
                    )
                )
            }
        }

        // 2. Vertical Alignment (TOP, CENTER, BOTTOM)
        val totalTextHeight = measuredLines.sumOf { it.lineHeight.toDouble() }.toFloat()
        var yCursor = when (element.verticalAnchor) {
            VerticalAnchor.CENTER -> (rect.centerY() - totalTextHeight / 2f).coerceAtLeast(rect.top + paddingY)
            VerticalAnchor.BOTTOM -> (rect.bottom - paddingY - totalTextHeight).coerceAtLeast(rect.top + paddingY)
            else -> rect.top + paddingY
        }

        // 3. Draw each line
        val runPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        val bulletPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            textSize = (13f * pxPerPt).coerceAtLeast(8f * scaleFactor)
            typeface = Typeface.DEFAULT_BOLD
        }

        for (line in measuredLines) {
            if (yCursor > canvas.height.toFloat()) break

            var xStart = when (line.alignment) {
                TextAlignment.CENTER -> (rect.centerX() - line.lineWidth / 2f).coerceAtLeast(rect.left + paddingX)
                TextAlignment.RIGHT -> (rect.right - paddingX - line.lineWidth).coerceAtLeast(rect.left + paddingX)
                else -> rect.left + paddingX + line.bulletIndent
            }

            // Draw bullet point if first line
            if (line.isFirstLineOfBullet) {
                val bulletX = rect.left + paddingX + (line.bulletIndent - 12f * pxPerPt).coerceAtLeast(0f)
                canvas.drawText(line.bulletChar, bulletX, yCursor + line.lineHeight * 0.75f, bulletPaint)
            }

            var xCursor = xStart
            for (wr in line.words) {
                runPaint.color = wr.run.color.toArgb()
                runPaint.textSize = (wr.run.fontSizePt * pxPerPt).coerceAtLeast(8f * scaleFactor)
                runPaint.typeface = when {
                    wr.run.isBold && wr.run.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                    wr.run.isBold -> Typeface.DEFAULT_BOLD
                    wr.run.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    else -> Typeface.DEFAULT
                }
                runPaint.isUnderlineText = wr.run.isUnderline

                // Search match highlight
                if (searchQuery.isNotBlank() && wr.word.contains(searchQuery, ignoreCase = true)) {
                    val matchPaint = Paint().apply {
                        color = android.graphics.Color.argb(160, 250, 204, 21) // Gold highlight
                        style = Paint.Style.FILL
                    }
                    val fm = runPaint.fontMetrics
                    val hlTop = yCursor + fm.ascent
                    val hlBottom = yCursor + fm.descent
                    canvas.drawRect(xCursor, hlTop, xCursor + wr.width, hlBottom, matchPaint)
                }

                canvas.drawText(wr.word, xCursor, yCursor + line.lineHeight * 0.75f, runPaint)
                xCursor += wr.width
            }

            yCursor += line.lineHeight
        }
    }

    private fun drawTable(canvas: Canvas, table: TableElement, rect: RectF, scaleFactor: Float) {
        val rowCount = table.rows.size.coerceAtLeast(1)
        val rowHeight = rect.height() / rowCount

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = table.borderColor.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = (1f * scaleFactor).coerceAtLeast(1f)
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(30, 41, 59)
            textSize = (13f * 1.4f * scaleFactor).coerceAtLeast(9f)
            typeface = Typeface.DEFAULT
        }

        // Calculate column widths based on weights or uniform division
        val firstRowCells = table.rows.firstOrNull()?.cells?.size ?: 1
        val colCount = firstRowCells.coerceAtLeast(1)
        val weights = if (table.colWidthWeights.size == colCount && table.colWidthWeights.all { it > 0 }) {
            val sum = table.colWidthWeights.sum()
            table.colWidthWeights.map { it / sum }
        } else {
            List(colCount) { 1f / colCount }
        }

        table.rows.forEachIndexed { rIdx, row ->
            val rTop = rect.top + rIdx * rowHeight
            val rBottom = rTop + rowHeight

            var colLeft = rect.left
            row.cells.forEachIndexed { cIdx, cell ->
                val colFraction = weights.getOrElse(cIdx) { 1f / colCount }
                val colWidth = rect.width() * colFraction
                val colRight = colLeft + colWidth
                val cellRect = RectF(colLeft, rTop, colRight, rBottom)

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

                // Cell text with wrapping
                if (cell.text.isNotBlank()) {
                    textPaint.color = cell.textColor.toArgb()
                    textPaint.typeface = if (cell.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    textPaint.textSize = (13f * 1.4f * scaleFactor).coerceAtLeast(9f)

                    val maxCellWidth = (cellRect.width() - 8f * scaleFactor).coerceAtLeast(20f)
                    val words = cell.text.split(" ")
                    val lines = mutableListOf<String>()
                    var curLine = ""

                    for (w in words) {
                        val testLine = if (curLine.isEmpty()) w else "$curLine $w"
                        if (textPaint.measureText(testLine) <= maxCellWidth) {
                            curLine = testLine
                        } else {
                            if (curLine.isNotEmpty()) lines.add(curLine)
                            curLine = w
                        }
                    }
                    if (curLine.isNotEmpty()) lines.add(curLine)

                    val cellLineHeight = textPaint.textSize * 1.25f
                    val totalH = lines.size * cellLineHeight
                    var cellY = cellRect.centerY() - totalH / 2f + textPaint.textSize * 0.75f

                    for (line in lines) {
                        val lineW = textPaint.measureText(line)
                        val lineX = when (cell.alignment) {
                            TextAlignment.CENTER -> cellRect.centerX() - lineW / 2f
                            TextAlignment.RIGHT -> cellRect.right - 4f * scaleFactor - lineW
                            else -> cellRect.left + 4f * scaleFactor
                        }
                        canvas.drawText(line, lineX, cellY, textPaint)
                        cellY += cellLineHeight
                    }
                }

                colLeft = colRight
            }
        }
    }
}
