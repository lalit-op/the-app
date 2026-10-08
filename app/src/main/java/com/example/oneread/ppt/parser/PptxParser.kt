package com.example.oneread.ppt.parser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import com.example.oneread.ppt.model.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object PptxParser {

    private val xmlFactory = XmlPullParserFactory.newInstance().apply {
        isNamespaceAware = true
    }

    fun parse(file: File): PresentationModel {
        val zip = ZipFile(file)
        try {
            // 1. Parse presentation.xml for dimensions & slide relationship order
            val (slideWidthEmus, slideHeightEmus, slideRels) = parsePresentationXml(zip)
            val aspectRatio = if (slideHeightEmus > 0) slideWidthEmus.toFloat() / slideHeightEmus.toFloat() else 16f / 9f

            // 2. Parse presentation relationships (_rels/presentation.xml.rels)
            val presRelMap = parseRelationships(zip, "ppt/_rels/presentation.xml.rels")

            // 3. Parse theme colors (ppt/theme/theme1.xml)
            val themeColors = parseThemeColors(zip)

            // 4. Resolve ordered slide file paths
            val slidePaths = slideRels.mapNotNull { rId ->
                val target = presRelMap[rId] ?: return@mapNotNull null
                if (target.startsWith("/")) target.removePrefix("/")
                else if (target.startsWith("ppt/")) target
                else "ppt/$target"
            }.ifEmpty {
                // Fallback: search for ppt/slides/slide*.xml directly in zip
                val found = mutableListOf<String>()
                var i = 1
                while (true) {
                    val entryName = "ppt/slides/slide$i.xml"
                    if (zip.getEntry(entryName) != null) {
                        found.add(entryName)
                        i++
                    } else break
                }
                found
            }

            // 5. Parse each slide
            val slides = mutableListOf<SlideModel>()
            slidePaths.forEachIndexed { index, slidePath ->
                try {
                    val slideModel = parseSlide(
                        zip = zip,
                        slidePath = slidePath,
                        index = index,
                        slideWidthEmus = slideWidthEmus,
                        slideHeightEmus = slideHeightEmus,
                        themeColors = themeColors
                    )
                    slides.add(slideModel)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return PresentationModel(
                title = file.nameWithoutExtension,
                slideWidth = slideWidthEmus.toFloat(),
                slideHeight = slideHeightEmus.toFloat(),
                aspectRatio = aspectRatio,
                slides = slides,
                themeColors = themeColors
            )
        } finally {
            zip.close()
        }
    }

    private data class PresentationInfo(
        val widthEmus: Long,
        val heightEmus: Long,
        val slideRIdList: List<String>
    )

    private fun parsePresentationXml(zip: ZipFile): PresentationInfo {
        val entry = zip.getEntry("ppt/presentation.xml") ?: return PresentationInfo(12192000L, 6858000L, emptyList())
        val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }

        var width = 12192000L // 16:9 widescreen default in EMUs
        var height = 6858000L
        val slideIds = mutableListOf<String>()

        val parser = xmlFactory.newPullParser().apply { setInput(StringReader(xml)) }
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "sldSz" -> {
                        val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                        val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                        if (cx != null && cy != null && cx > 0 && cy > 0) {
                            width = cx
                            height = cy
                        }
                    }
                    "sldId" -> {
                        val rId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                            ?: parser.getAttributeValue(null, "r:id")
                            ?: parser.getAttributeValue(null, "id")
                        if (rId != null) slideIds.add(rId)
                    }
                }
            }
            eventType = parser.next()
        }

        return PresentationInfo(width, height, slideIds)
    }

    private fun parseRelationships(zip: ZipFile, relsPath: String): Map<String, String> {
        val entry = zip.getEntry(relsPath) ?: return emptyMap()
        val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
        val map = mutableMapOf<String, String>()

        val parser = xmlFactory.newPullParser().apply { setInput(StringReader(xml)) }
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (id != null && target != null) {
                    map[id] = target
                }
            }
            eventType = parser.next()
        }

        return map
    }

    private fun parseThemeColors(zip: ZipFile): Map<String, Color> {
        val entry = zip.getEntry("ppt/theme/theme1.xml") ?: return defaultThemeColors()
        val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
        val colors = defaultThemeColors().toMutableMap()

        val parser = xmlFactory.newPullParser().apply { setInput(StringReader(xml)) }
        var eventType = parser.eventType
        var currentSchemeTag: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (tag in setOf("dk1", "lt1", "dk2", "lt2", "accent1", "accent2", "accent3", "accent4", "accent5", "accent6", "hlink", "folHlink")) {
                    currentSchemeTag = tag
                } else if (currentSchemeTag != null) {
                    when (tag) {
                        "srgbClr" -> {
                            val hex = parser.getAttributeValue(null, "val")
                            if (hex != null) {
                                parseHexColor(hex)?.let { colors[currentSchemeTag!!] = it }
                            }
                            currentSchemeTag = null
                        }
                        "sysClr" -> {
                            val lastClr = parser.getAttributeValue(null, "lastClr")
                            if (lastClr != null) {
                                parseHexColor(lastClr)?.let { colors[currentSchemeTag!!] = it }
                            }
                            currentSchemeTag = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return colors
    }

    private fun defaultThemeColors(): Map<String, Color> = mapOf(
        "lt1" to Color(0xFFFFFFFF),
        "dk1" to Color(0xFF0F172A),
        "lt2" to Color(0xFFF8FAFC),
        "dk2" to Color(0xFF1E293B),
        "accent1" to Color(0xFF16A34A), // Natural green (standard agriculture / business accent)
        "accent2" to Color(0xFF2563EB),
        "accent3" to Color(0xFFD97706),
        "accent4" to Color(0xFFDC2626),
        "accent5" to Color(0xFF9333EA),
        "accent6" to Color(0xFF0D9488)
    )

    private fun parseSlide(
        zip: ZipFile,
        slidePath: String,
        index: Int,
        slideWidthEmus: Long,
        slideHeightEmus: Long,
        themeColors: Map<String, Color>
    ): SlideModel {
        val entry = zip.getEntry(slidePath) ?: throw IllegalArgumentException("Slide entry not found: $slidePath")
        val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }

        // Parse slide relationship mapping for images and layout references
        val slideRelPath = slidePath.substringBeforeLast('/') + "/_rels/" + slidePath.substringAfterLast('/') + ".rels"
        val slideRelMap = parseRelationships(zip, slideRelPath)

        var backgroundColor = Color.White
        val elements = mutableListOf<SlideElement>()
        val searchableTextBuilder = StringBuilder()
        var zIndexCounter = 0

        val parser = xmlFactory.newPullParser().apply { setInput(StringReader(xml)) }
        var eventType = parser.eventType

        var inBg = false
        var inSp = false
        var inPic = false
        var inGraphicFrame = false
        var inTxBody = false
        var inParagraph = false
        var inRun = false

        // Current shape state
        var shapeType = ShapeType.RECTANGLE
        var offX = 0L
        var offY = 0L
        var extCx = 0L
        var extCy = 0L
        var rotation = 0f
        var shapeFillColor: Color? = null
        var shapeStrokeColor: Color? = null
        var shapeStrokeWidth = 0f

        // Current picture state
        var picEmbedId: String? = null

        // Current text state
        var textParagraphs = mutableListOf<SlideParagraph>()
        var currentParaRuns = mutableListOf<SlideTextRun>()
        var currentParaAlign = TextAlignment.LEFT
        var currentRunSize = 14f
        var currentRunBold = false
        var currentRunItalic = false
        var currentRunColor = Color(0xFF1E293B)
        var currentRunFamily: String? = null
        var verticalAnchor = VerticalAnchor.TOP

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "bg" -> inBg = true
                        "sp" -> {
                            inSp = true
                            shapeType = ShapeType.RECTANGLE
                            offX = 0L; offY = 0L; extCx = 0L; extCy = 0L; rotation = 0f
                            shapeFillColor = null; shapeStrokeColor = null; shapeStrokeWidth = 0f
                            textParagraphs = mutableListOf()
                            verticalAnchor = VerticalAnchor.TOP
                        }
                        "pic" -> {
                            inPic = true
                            offX = 0L; offY = 0L; extCx = 0L; extCy = 0L; rotation = 0f
                            picEmbedId = null
                        }
                        "graphicFrame" -> {
                            inGraphicFrame = true
                            offX = 0L; offY = 0L; extCx = 0L; extCy = 0L
                        }
                        "off" -> {
                            offX = parser.getAttributeValue(null, "x")?.toLongOrNull() ?: offX
                            offY = parser.getAttributeValue(null, "y")?.toLongOrNull() ?: offY
                        }
                        "ext" -> {
                            extCx = parser.getAttributeValue(null, "cx")?.toLongOrNull() ?: extCx
                            extCy = parser.getAttributeValue(null, "cy")?.toLongOrNull() ?: extCy
                        }
                        "rot" -> {
                            val rVal = parser.getAttributeValue(null, "val")?.toFloatOrNull() ?: 0f
                            rotation = rVal / 60000f
                        }
                        "prstGeom" -> {
                            val prst = parser.getAttributeValue(null, "prst") ?: ""
                            shapeType = when (prst) {
                                "roundRect" -> ShapeType.ROUNDED_RECTANGLE
                                "line" -> ShapeType.LINE
                                "ellipse" -> ShapeType.ELLIPSE
                                else -> ShapeType.RECTANGLE
                            }
                        }
                        "srgbClr" -> {
                            val hex = parser.getAttributeValue(null, "val")
                            val clr = hex?.let { parseHexColor(it) }
                            if (clr != null) {
                                if (inBg) backgroundColor = clr
                                else if (inRun) currentRunColor = clr
                                else if (shapeStrokeColor == null && inSp && !inTxBody) shapeFillColor = clr
                                else if (shapeStrokeColor != null && inSp) shapeStrokeColor = clr
                            }
                        }
                        "schemeClr" -> {
                            val schemeKey = parser.getAttributeValue(null, "val")
                            val clr = themeColors[schemeKey]
                            if (clr != null) {
                                if (inBg) backgroundColor = clr
                                else if (inRun) currentRunColor = clr
                                else if (shapeStrokeColor == null && inSp && !inTxBody) shapeFillColor = clr
                                else if (shapeStrokeColor != null && inSp) shapeStrokeColor = clr
                            }
                        }
                        "ln" -> {
                            val w = parser.getAttributeValue(null, "w")?.toLongOrNull() ?: 12700L
                            shapeStrokeWidth = (w / 12700f).coerceAtLeast(1f)
                            shapeStrokeColor = shapeFillColor ?: Color(0xFF16A34A)
                        }
                        "blip" -> {
                            val embed = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed")
                                ?: parser.getAttributeValue(null, "r:embed")
                                ?: parser.getAttributeValue(null, "embed")
                            if (embed != null) picEmbedId = embed
                        }
                        "bodyPr" -> {
                            val anchor = parser.getAttributeValue(null, "anchor")
                            verticalAnchor = when (anchor) {
                                "ctr" -> VerticalAnchor.CENTER
                                "b" -> VerticalAnchor.BOTTOM
                                else -> VerticalAnchor.TOP
                            }
                        }
                        "txBody" -> inTxBody = true
                        "p" -> {
                            inParagraph = true
                            currentParaRuns = mutableListOf()
                            currentParaAlign = TextAlignment.LEFT
                        }
                        "pPr" -> {
                            val algn = parser.getAttributeValue(null, "algn")
                            currentParaAlign = when (algn) {
                                "ctr" -> TextAlignment.CENTER
                                "r" -> TextAlignment.RIGHT
                                "just" -> TextAlignment.JUSTIFY
                                else -> TextAlignment.LEFT
                            }
                        }
                        "r" -> {
                            inRun = true
                            currentRunSize = 14f
                            currentRunBold = false
                            currentRunItalic = false
                            currentRunColor = Color(0xFF1E293B)
                            currentRunFamily = null
                        }
                        "rPr" -> {
                            val sz = parser.getAttributeValue(null, "sz")?.toFloatOrNull()
                            if (sz != null) currentRunSize = (sz / 100f).coerceIn(8f, 72f)
                            val b = parser.getAttributeValue(null, "b")
                            currentRunBold = (b == "1" || b == "true")
                            val i = parser.getAttributeValue(null, "i")
                            currentRunItalic = (i == "1" || i == "true")
                        }
                        "latin" -> {
                            currentRunFamily = parser.getAttributeValue(null, "typeface")
                        }
                        "t" -> {
                            val text = parser.nextText()
                            if (text.isNotEmpty()) {
                                currentParaRuns.add(
                                    SlideTextRun(
                                        text = text,
                                        fontSizePt = currentRunSize,
                                        isBold = currentRunBold,
                                        isItalic = currentRunItalic,
                                        color = currentRunColor,
                                        fontFamily = currentRunFamily
                                    )
                                )
                                searchableTextBuilder.append(text).append(" ")
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "bg" -> inBg = false
                        "r" -> inRun = false
                        "p" -> {
                            if (currentParaRuns.isNotEmpty()) {
                                textParagraphs.add(
                                    SlideParagraph(
                                        alignment = currentParaAlign,
                                        runs = currentParaRuns
                                    )
                                )
                            }
                            inParagraph = false
                        }
                        "txBody" -> inTxBody = false
                        "sp" -> {
                            inSp = false
                            if (extCx > 0 && extCy > 0 && slideWidthEmus > 0 && slideHeightEmus > 0) {
                                val bounds = RectF(
                                    offX.toFloat() / slideWidthEmus.toFloat(),
                                    offY.toFloat() / slideHeightEmus.toFloat(),
                                    (offX + extCx).toFloat() / slideWidthEmus.toFloat(),
                                    (offY + extCy).toFloat() / slideHeightEmus.toFloat()
                                )

                                // Add shape element if it has visible fill or stroke
                                if (shapeFillColor != null || shapeStrokeColor != null) {
                                    elements.add(
                                        ShapeElement(
                                            bounds = bounds,
                                            zIndex = zIndexCounter++,
                                            shapeType = shapeType,
                                            cornerRadius = if (shapeType == ShapeType.ROUNDED_RECTANGLE) 16f else 0f,
                                            fillColor = shapeFillColor,
                                            strokeColor = shapeStrokeColor,
                                            strokeWidth = shapeStrokeWidth,
                                            rotation = rotation
                                        )
                                    )
                                }

                                // Add text element if it has paragraphs
                                if (textParagraphs.isNotEmpty()) {
                                    elements.add(
                                        TextElement(
                                            bounds = bounds,
                                            zIndex = zIndexCounter++,
                                            paragraphs = textParagraphs,
                                            verticalAnchor = verticalAnchor
                                        )
                                    )
                                }
                            }
                        }
                        "pic" -> {
                            inPic = false
                            if (picEmbedId != null && extCx > 0 && extCy > 0 && slideWidthEmus > 0 && slideHeightEmus > 0) {
                                val target = slideRelMap[picEmbedId]
                                val mediaPath = resolveMediaPath(slidePath, target)
                                val bmp = mediaPath?.let { loadBitmapFromZip(zip, it) }
                                if (bmp != null) {
                                    val bounds = RectF(
                                        offX.toFloat() / slideWidthEmus.toFloat(),
                                        offY.toFloat() / slideHeightEmus.toFloat(),
                                        (offX + extCx).toFloat() / slideWidthEmus.toFloat(),
                                        (offY + extCy).toFloat() / slideHeightEmus.toFloat()
                                    )
                                    elements.add(
                                        ImageElement(
                                            bounds = bounds,
                                            zIndex = zIndexCounter++,
                                            bitmap = bmp,
                                            rotation = rotation
                                        )
                                    )
                                }
                            }
                        }
                        "graphicFrame" -> inGraphicFrame = false
                    }
                }
            }
            eventType = parser.next()
        }

        // Extract slide title from the first non-empty text element
        val slideTitle = elements.filterIsInstance<TextElement>()
            .firstOrNull { it.paragraphs.any { p -> p.plainText.isNotBlank() } }
            ?.paragraphs?.firstOrNull { it.plainText.isNotBlank() }
            ?.plainText?.trim() ?: "Slide ${index + 1}"

        // Extract speaker notes if available in relationships
        val notesTarget = slideRelMap.values.find { it.contains("notesSlide") }
        val notes = if (notesTarget != null) {
            val notesPath = resolveMediaPath(slidePath, notesTarget)
            if (notesPath != null) parseNotesText(zip, notesPath) else ""
        } else ""

        if (notes.isNotBlank()) {
            searchableTextBuilder.append(" ").append(notes)
        }

        return SlideModel(
            index = index,
            slideNumber = index + 1,
            title = slideTitle,
            backgroundColor = backgroundColor,
            elements = elements.sortedBy { it.zIndex },
            notes = notes,
            fullSearchableText = searchableTextBuilder.toString().trim()
        )
    }

    private fun parseNotesText(zip: ZipFile, notesPath: String): String {
        return runCatching {
            val entry = zip.getEntry(notesPath) ?: return@runCatching ""
            val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            val parser = xmlFactory.newPullParser().apply { setInput(StringReader(xml)) }
            val sb = StringBuilder()
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                    val txt = parser.nextText()
                    if (txt.isNotBlank()) {
                        sb.append(txt).append("\n")
                    }
                }
                eventType = parser.next()
            }
            sb.toString().trim()
        }.getOrDefault("")
    }

    private fun resolveMediaPath(slidePath: String, target: String?): String? {
        if (target == null) return null
        return if (target.startsWith("../media/")) {
            "ppt/media/" + target.substringAfter("../media/")
        } else if (target.startsWith("media/")) {
            "ppt/media/" + target.substringAfter("media/")
        } else if (target.startsWith("/")) {
            target.removePrefix("/")
        } else {
            "ppt/" + target.removePrefix("../")
        }
    }

    private fun loadBitmapFromZip(zip: ZipFile, path: String): Bitmap? {
        return runCatching {
            val entry = zip.getEntry(path) ?: return@runCatching null
            zip.getInputStream(entry).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    private fun parseHexColor(hex: String): Color? {
        return try {
            val cleanHex = hex.trim().removePrefix("#")
            when (cleanHex.length) {
                6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
                8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
