package com.example.oneread.ppt.parser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import com.example.oneread.ppt.model.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

object PptxParser {

    private fun createPullParser(): XmlPullParser {
        return try {
            XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = true
            }.newPullParser()
        } catch (e: Throwable) {
            try {
                val clazz = Class.forName("org.kxml2.io.KXmlParser")
                (clazz.getDeclaredConstructor().newInstance() as XmlPullParser).apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                }
            } catch (ex: Throwable) {
                throw IllegalStateException("No XmlPullParser implementation available", ex)
            }
        }
    }

    fun parse(file: File): PresentationModel {
        if (!file.exists() || file.length() < 30) {
            throw IllegalArgumentException("PowerPoint presentation is missing or empty.")
        }

        val zip = try {
            ZipFile(file)
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted or invalid presentation archive (.${file.extension}): ${e.message}", e)
        }

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

            if (slidePaths.isEmpty()) {
                throw IllegalArgumentException("No presentation slides found in ${file.name}")
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
                    // Add fallback placeholder for unparseable slide rather than breaking the whole presentation
                    slides.add(
                        SlideModel(
                            index = index,
                            slideNumber = index + 1,
                            title = "Slide ${index + 1}",
                            backgroundColor = Color.White,
                            elements = listOf(
                                TextElement(
                                    bounds = RectF(0.1f, 0.3f, 0.9f, 0.7f),
                                    zIndex = 0,
                                    paragraphs = listOf(
                                        SlideParagraph(
                                            alignment = TextAlignment.CENTER,
                                            runs = listOf(
                                                SlideTextRun(
                                                    text = "Slide ${index + 1}",
                                                    fontSizePt = 24f,
                                                    isBold = true,
                                                    color = Color(0xFF0F172A)
                                                )
                                            )
                                        )
                                    ),
                                    verticalAnchor = VerticalAnchor.CENTER
                                )
                            )
                        )
                    )
                }
            }

            return PresentationModel(
                title = file.nameWithoutExtension,
                slideWidth = slideWidthEmus.toFloat(),
                slideHeight = slideHeightEmus.toFloat(),
                aspectRatio = aspectRatio,
                slideWidthEmus = slideWidthEmus,
                slideHeightEmus = slideHeightEmus,
                slides = slides,
                themeColors = themeColors
            )
        } finally {
            runCatching { zip.close() }
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

        val parser = createPullParser().apply { setInput(StringReader(xml)) }
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "sldSz" -> {
                        val type = parser.getAttributeValue(null, "type")?.lowercase()
                        val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                        val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                        if (cx != null && cy != null && cx > 0 && cy > 0) {
                            width = cx
                            height = cy
                        } else if (type == "screen4x3") {
                            width = 9144000L
                            height = 6858000L
                        } else if (type == "screen16x9") {
                            width = 12192000L
                            height = 6858000L
                        } else if (type == "screen16x10") {
                            width = 10972800L
                            height = 6858000L
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

        val parser = createPullParser().apply { setInput(StringReader(xml)) }
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

        val parser = createPullParser().apply { setInput(StringReader(xml)) }
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
        "accent1" to Color(0xFF16A34A),
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
        var backgroundImage: Bitmap? = null
        var gradientEndColor: Color? = null
        val elements = mutableListOf<SlideElement>()
        val searchableTextBuilder = StringBuilder()
        var zIndexCounter = 0

        val parser = createPullParser().apply { setInput(StringReader(xml)) }
        var eventType = parser.eventType

        var inBg = false
        var inSp = false
        var inPic = false
        var inGraphicFrame = false
        var inTbl = false
        var inTr = false
        var inTc = false
        var inTcPr = false
        var inTxBody = false
        var inParagraph = false
        var inRun = false
        var inLn = false

        // Group transformation stack for diagram and grouped shapes
        data class GroupTransform(
            val offX: Long, val offY: Long,
            val extCx: Long, val extCy: Long,
            val chOffX: Long, val chOffY: Long,
            val chExtCx: Long, val chExtCy: Long
        )
        val groupStack = mutableListOf<GroupTransform>()
        var inGrpSpPr = false
        var grpOffX = 0L; var grpOffY = 0L; var grpExtCx = 0L; var grpExtCy = 0L
        var grpChOffX = 0L; var grpChOffY = 0L; var grpChExtCx = 0L; var grpChExtCy = 0L

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
        var phType: String? = null

        // Current picture state
        var picEmbedId: String? = null

        // Current text state
        var textParagraphs = mutableListOf<SlideParagraph>()
        var currentParaRuns = mutableListOf<SlideTextRun>()
        var currentParaAlign = TextAlignment.LEFT
        var currentParaIsBullet = false
        var currentParaBulletChar = "•"
        var currentParaBulletLevel = 0

        // Paragraph default run properties are inherited by runs whose <a:rPr>
        // omits font attributes. Resetting every run to 14pt loses the title/body sizing
        // authored by PowerPoint and makes otherwise correct slides look miniature.
        var paragraphDefaultRunSize = 14f
        var paragraphDefaultRunBold = false
        var paragraphDefaultRunItalic = false
        var paragraphDefaultRunUnderline = false
        var paragraphDefaultRunColor = Color(0xFF1E293B)
        var paragraphDefaultRunFamily: String? = null
        var inDefRPr = false

        var currentRunSize = 14f
        var currentRunBold = false
        var currentRunItalic = false
        var currentRunUnderline = false
        var currentRunColor = Color(0xFF1E293B)
        var currentRunFamily: String? = null
        var verticalAnchor = VerticalAnchor.TOP

        // Current table state
        var tableColWeights = mutableListOf<Float>()
        var tableRows = mutableListOf<SlideTableRow>()
        var currentRowCells = mutableListOf<SlideTableCell>()
        var currentCellText = StringBuilder()
        var currentCellFillColor: Color? = null
        var currentCellTextColor = Color(0xFF1E293B)
        var currentCellBold = false
        var currentCellAlign = TextAlignment.LEFT

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "bg" -> inBg = true
                        "grpSp" -> {
                            grpOffX = 0L; grpOffY = 0L; grpExtCx = 0L; grpExtCy = 0L
                            grpChOffX = 0L; grpChOffY = 0L; grpChExtCx = 0L; grpChExtCy = 0L
                        }
                        "grpSpPr" -> inGrpSpPr = true
                        "sp", "cxnSp" -> {
                            inSp = true
                            shapeType = if (parser.name == "cxnSp") ShapeType.CONNECTOR else ShapeType.RECTANGLE
                            offX = 0L; offY = 0L; extCx = 0L; extCy = 0L; rotation = 0f
                            shapeFillColor = null; shapeStrokeColor = null; shapeStrokeWidth = 0f
                            textParagraphs = mutableListOf()
                            verticalAnchor = VerticalAnchor.TOP
                            inLn = false
                            phType = null
                        }
                        "ph" -> {
                            val typeAttr = parser.getAttributeValue(null, "type")?.lowercase()
                            phType = typeAttr ?: "body"
                        }
                        "pic" -> {
                            inPic = true
                            offX = 0L; offY = 0L; extCx = 0L; extCy = 0L; rotation = 0f
                            picEmbedId = null
                        }
                        "graphicFrame" -> {
                            inGraphicFrame = true
                            offX = 0L; offY = 0L; extCx = 0L; extCy = 0L
                            tableColWeights = mutableListOf()
                            tableRows = mutableListOf()
                        }
                        "tbl" -> inTbl = true
                        "gridCol" -> {
                            val w = parser.getAttributeValue(null, "w")?.toFloatOrNull() ?: 1000f
                            tableColWeights.add(w)
                        }
                        "tr" -> {
                            inTr = true
                            currentRowCells = mutableListOf()
                        }
                        "tc" -> {
                            inTc = true
                            currentCellText = StringBuilder()
                            currentCellFillColor = null
                            currentCellTextColor = Color(0xFF1E293B)
                            currentCellBold = false
                            currentCellAlign = TextAlignment.LEFT
                        }
                        "tcPr" -> inTcPr = true
                        "off" -> {
                            val xVal = parser.getAttributeValue(null, "x")?.toLongOrNull()
                            val yVal = parser.getAttributeValue(null, "y")?.toLongOrNull()
                            if (inGrpSpPr) {
                                if (xVal != null) grpOffX = xVal
                                if (yVal != null) grpOffY = yVal
                            } else {
                                if (xVal != null) offX = xVal
                                if (yVal != null) offY = yVal
                            }
                        }
                        "ext" -> {
                            val cxVal = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                            val cyVal = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                            if (inGrpSpPr) {
                                if (cxVal != null) grpExtCx = cxVal
                                if (cyVal != null) grpExtCy = cyVal
                            } else {
                                if (cxVal != null) extCx = cxVal
                                if (cyVal != null) extCy = cyVal
                            }
                        }
                        "chOff" -> {
                            val xVal = parser.getAttributeValue(null, "x")?.toLongOrNull()
                            val yVal = parser.getAttributeValue(null, "y")?.toLongOrNull()
                            if (xVal != null) grpChOffX = xVal
                            if (yVal != null) grpChOffY = yVal
                        }
                        "chExt" -> {
                            val cxVal = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                            val cyVal = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                            if (cxVal != null) grpChExtCx = cxVal
                            if (cyVal != null) grpChExtCy = cyVal
                        }
                        "rot" -> {
                            val rVal = parser.getAttributeValue(null, "val")?.toFloatOrNull() ?: 0f
                            rotation = rVal / 60000f
                        }
                        "prstGeom" -> {
                            val prst = parser.getAttributeValue(null, "prst")?.lowercase() ?: ""
                            shapeType = when {
                                prst.contains("leftbrace") || (prst.contains("brace") && (prst.contains("left") || prst.contains("open"))) -> ShapeType.LEFT_BRACE
                                prst.contains("rightbrace") || (prst.contains("brace") && (prst.contains("right") || prst.contains("close"))) -> ShapeType.RIGHT_BRACE
                                prst.contains("brace") -> ShapeType.LEFT_BRACE
                                prst.contains("leftbracket") || (prst.contains("bracket") && (prst.contains("left") || prst.contains("open"))) -> ShapeType.LEFT_BRACKET
                                prst.contains("rightbracket") || (prst.contains("bracket") && (prst.contains("right") || prst.contains("close"))) -> ShapeType.RIGHT_BRACKET
                                prst.contains("bracket") -> ShapeType.LEFT_BRACKET
                                prst.contains("diamond") -> ShapeType.DIAMOND
                                prst.contains("roundrect") -> ShapeType.ROUNDED_RECTANGLE
                                prst.contains("line") -> ShapeType.LINE
                                prst.contains("ellipse") || prst.contains("oval") || prst.contains("circle") -> ShapeType.ELLIPSE
                                prst.contains("triangle") -> ShapeType.TRIANGLE
                                prst.contains("arrow") || prst.contains("chevron") -> ShapeType.ARROW
                                prst.contains("connector") -> ShapeType.CONNECTOR
                                else -> ShapeType.RECTANGLE
                            }
                        }
                        "srgbClr" -> {
                            val hex = parser.getAttributeValue(null, "val")
                            val clr = hex?.let { parseHexColor(it) }
                            if (clr != null) {
                                if (inBg) {
                                    backgroundColor = clr
                                } else if (inTcPr) {
                                    currentCellFillColor = clr
                                } else if (inDefRPr) {
                                    paragraphDefaultRunColor = clr
                                } else if (inRun) {
                                    currentRunColor = clr
                                    if (inTc) currentCellTextColor = clr
                                } else if (inLn) {
                                    shapeStrokeColor = clr
                                } else if (inSp && !inTxBody) {
                                    shapeFillColor = clr
                                }
                            }
                        }
                        "schemeClr" -> {
                            val schemeKey = parser.getAttributeValue(null, "val")
                            val clr = themeColors[schemeKey]
                            if (clr != null) {
                                if (inBg) {
                                    backgroundColor = clr
                                } else if (inTcPr) {
                                    currentCellFillColor = clr
                                } else if (inDefRPr) {
                                    paragraphDefaultRunColor = clr
                                } else if (inRun) {
                                    currentRunColor = clr
                                    if (inTc) currentCellTextColor = clr
                                } else if (inLn) {
                                    shapeStrokeColor = clr
                                } else if (inSp && !inTxBody) {
                                    shapeFillColor = clr
                                }
                            }
                        }
                        "ln" -> {
                            inLn = true
                            val w = parser.getAttributeValue(null, "w")?.toLongOrNull() ?: 12700L
                            shapeStrokeWidth = (w / 12700f).coerceAtLeast(1f)
                            if (shapeStrokeColor == null) {
                                shapeStrokeColor = shapeFillColor ?: Color(0xFF1E293B)
                            }
                        }
                        "blip" -> {
                            val embed = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed")
                                ?: parser.getAttributeValue(null, "r:embed")
                                ?: parser.getAttributeValue(null, "embed")
                            if (embed != null) {
                                if (inBg) {
                                    val bgPath = slideRelMap[embed]?.let { resolveMediaPath(slidePath, it) }
                                    if (bgPath != null) {
                                        backgroundImage = loadBitmapFromZip(zip, bgPath)
                                    }
                                } else if (inPic) {
                                    picEmbedId = embed
                                }
                            }
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
                            currentParaIsBullet = false
                            currentParaBulletChar = "•"
                            currentParaBulletLevel = 0
                            paragraphDefaultRunSize = 14f
                            paragraphDefaultRunBold = false
                            paragraphDefaultRunItalic = false
                            paragraphDefaultRunUnderline = false
                            paragraphDefaultRunColor = Color(0xFF1E293B)
                            paragraphDefaultRunFamily = null
                        }
                        "pPr" -> {
                            val algn = parser.getAttributeValue(null, "algn")
                            currentParaAlign = when (algn) {
                                "ctr" -> TextAlignment.CENTER
                                "r" -> TextAlignment.RIGHT
                                "just" -> TextAlignment.JUSTIFY
                                else -> TextAlignment.LEFT
                            }
                            if (inTc) currentCellAlign = currentParaAlign

                            val lvl = parser.getAttributeValue(null, "lvl")?.toIntOrNull() ?: 0
                            currentParaBulletLevel = lvl
                            if (lvl > 0) {
                                currentParaIsBullet = true
                            }
                        }
                        "buChar" -> {
                            val ch = parser.getAttributeValue(null, "char")
                            if (!ch.isNullOrBlank()) {
                                currentParaIsBullet = true
                                currentParaBulletChar = ch
                            }
                        }
                        "buAutoNum" -> {
                            currentParaIsBullet = true
                            currentParaBulletChar = "1."
                        }
                        "buNone" -> {
                            currentParaIsBullet = false
                        }
                        "defRPr" -> {
                            inDefRPr = true
                            parser.getAttributeValue(null, "sz")?.toFloatOrNull()?.let {
                                paragraphDefaultRunSize = (it / 100f).coerceIn(8f, 72f)
                            }
                            parser.getAttributeValue(null, "b")?.let {
                                paragraphDefaultRunBold = it == "1" || it.equals("true", ignoreCase = true)
                            }
                            parser.getAttributeValue(null, "i")?.let {
                                paragraphDefaultRunItalic = it == "1" || it.equals("true", ignoreCase = true)
                            }
                            parser.getAttributeValue(null, "u")?.let {
                                paragraphDefaultRunUnderline = it != "none" && it != "0"
                            }
                        }
                        "r" -> {
                            inRun = true
                            currentRunSize = paragraphDefaultRunSize
                            currentRunBold = paragraphDefaultRunBold
                            currentRunItalic = paragraphDefaultRunItalic
                            currentRunUnderline = paragraphDefaultRunUnderline
                            currentRunColor = paragraphDefaultRunColor
                            currentRunFamily = paragraphDefaultRunFamily
                        }
                        "rPr" -> {
                            parser.getAttributeValue(null, "sz")?.toFloatOrNull()?.let {
                                currentRunSize = (it / 100f).coerceIn(8f, 72f)
                            }
                            parser.getAttributeValue(null, "b")?.let {
                                currentRunBold = it == "1" || it.equals("true", ignoreCase = true)
                            }
                            if (inTc && currentRunBold) currentCellBold = true
                            parser.getAttributeValue(null, "i")?.let {
                                currentRunItalic = it == "1" || it.equals("true", ignoreCase = true)
                            }
                            parser.getAttributeValue(null, "u")?.let {
                                currentRunUnderline = it != "none" && it != "0"
                            }
                        }
                        "latin" -> {
                            val family = parser.getAttributeValue(null, "typeface")
                            if (!family.isNullOrBlank()) {
                                if (inDefRPr) paragraphDefaultRunFamily = family else currentRunFamily = family
                            }
                        }
                        "t" -> {
                            val text = parser.nextText()
                            if (text.isNotEmpty()) {
                                if (inTc) {
                                    currentCellText.append(text).append(" ")
                                }
                                currentParaRuns.add(
                                    SlideTextRun(
                                        text = text,
                                        fontSizePt = currentRunSize,
                                        isBold = currentRunBold,
                                        isItalic = currentRunItalic,
                                        isUnderline = currentRunUnderline,
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
                        "ln" -> inLn = false
                        "r" -> inRun = false
                        "defRPr" -> inDefRPr = false
                        "p" -> {
                            if (currentParaRuns.isNotEmpty()) {
                                textParagraphs.add(
                                    SlideParagraph(
                                        alignment = currentParaAlign,
                                        runs = currentParaRuns,
                                        isBullet = currentParaIsBullet,
                                        bulletChar = currentParaBulletChar,
                                        bulletLevel = currentParaBulletLevel
                                    )
                                )
                            }
                            inParagraph = false
                        }
                        "tcPr" -> inTcPr = false
                        "tc" -> {
                            inTc = false
                            currentRowCells.add(
                                SlideTableCell(
                                    text = currentCellText.toString().trim(),
                                    fillColor = currentCellFillColor,
                                    textColor = currentCellTextColor,
                                    isBold = currentCellBold,
                                    alignment = currentCellAlign
                                )
                            )
                        }
                        "tr" -> {
                            inTr = false
                            if (currentRowCells.isNotEmpty()) {
                                tableRows.add(SlideTableRow(cells = currentRowCells))
                            }
                        }
                        "tbl" -> inTbl = false
                        "txBody" -> inTxBody = false
                        "grpSpPr" -> {
                            inGrpSpPr = false
                            if (grpExtCx > 0 && grpExtCy > 0 && grpChExtCx > 0 && grpChExtCy > 0) {
                                groupStack.add(GroupTransform(grpOffX, grpOffY, grpExtCx, grpExtCy, grpChOffX, grpChOffY, grpChExtCx, grpChExtCy))
                            }
                        }
                        "grpSp" -> {
                            if (groupStack.isNotEmpty()) {
                                groupStack.removeAt(groupStack.size - 1)
                            }
                        }
                        "sp", "cxnSp" -> {
                            inSp = false
                            val isLineGeometry = shapeType == ShapeType.LINE || shapeType == ShapeType.CONNECTOR
                            val hasUsableExtent = extCx > 0 && extCy > 0 ||
                                (isLineGeometry && (extCx > 0 || extCy > 0))
                            val bounds = if (hasUsableExtent && slideWidthEmus > 0 && slideHeightEmus > 0) {
                                var curX = offX
                                var curY = offY
                                var curCx = extCx
                                var curCy = extCy
                                for (i in groupStack.indices.reversed()) {
                                    val g = groupStack[i]
                                    if (g.chExtCx > 0 && g.chExtCy > 0) {
                                        curX = g.offX + ((curX - g.chOffX).toDouble() * g.extCx / g.chExtCx).toLong()
                                        curY = g.offY + ((curY - g.chOffY).toDouble() * g.extCy / g.chExtCy).toLong()
                                        curCx = (curCx.toDouble() * g.extCx / g.chExtCx).toLong()
                                        curCy = (curCy.toDouble() * g.extCy / g.chExtCy).toLong()
                                    }
                                }
                                RectF(
                                    (curX.toFloat() / slideWidthEmus.toFloat()).coerceIn(-0.1f, 1.1f),
                                    (curY.toFloat() / slideHeightEmus.toFloat()).coerceIn(-0.1f, 1.1f),
                                    ((curX + curCx).toFloat() / slideWidthEmus.toFloat()).coerceIn(-0.1f, 1.2f),
                                    ((curY + curCy).toFloat() / slideHeightEmus.toFloat()).coerceIn(-0.1f, 1.2f)
                                )
                            } else if (phType != null || (textParagraphs.isNotEmpty() && offX == 0L && extCx == 0L)) {
                                val pType = phType ?: if (index == 0) "ctrtitle" else "body"
                                when (pType) {
                                    "ctrtitle" -> RectF(0.08f, 0.28f, 0.92f, 0.54f)
                                    "subtitle" -> RectF(0.08f, 0.56f, 0.92f, 0.80f)
                                    "title" -> RectF(0.06f, 0.05f, 0.94f, 0.22f)
                                    "body" -> RectF(0.06f, 0.24f, 0.94f, 0.90f)
                                    "ftr", "footer" -> RectF(0.06f, 0.92f, 0.60f, 0.97f)
                                    "sldnum", "dt" -> RectF(0.82f, 0.92f, 0.96f, 0.97f)
                                    else -> RectF(0.08f, 0.24f, 0.92f, 0.88f)
                                }
                            } else null

                            if (bounds != null) {
                                // Add shape element if it has visible fill/stroke or is a structural line/brace/bracket/arrow
                                val isStructuralShape = shapeType in setOf(
                                    ShapeType.LINE, ShapeType.CONNECTOR, ShapeType.ARROW,
                                    ShapeType.LEFT_BRACE, ShapeType.RIGHT_BRACE,
                                    ShapeType.LEFT_BRACKET, ShapeType.RIGHT_BRACKET,
                                    ShapeType.DIAMOND
                                )
                                if (shapeFillColor != null || shapeStrokeColor != null || isStructuralShape) {
                                    elements.add(
                                        ShapeElement(
                                            bounds = bounds,
                                            zIndex = zIndexCounter++,
                                            shapeType = shapeType,
                                            cornerRadius = if (shapeType == ShapeType.ROUNDED_RECTANGLE) 16f else 0f,
                                            fillColor = shapeFillColor,
                                            strokeColor = shapeStrokeColor ?: if (isStructuralShape) Color(0xFF1E293B) else null,
                                            strokeWidth = if (shapeStrokeWidth > 0f) shapeStrokeWidth else if (isStructuralShape) 2f else 0f,
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
                                    var curX = offX
                                    var curY = offY
                                    var curCx = extCx
                                    var curCy = extCy
                                    for (i in groupStack.indices.reversed()) {
                                        val g = groupStack[i]
                                        if (g.chExtCx > 0 && g.chExtCy > 0) {
                                            curX = g.offX + ((curX - g.chOffX).toDouble() * g.extCx / g.chExtCx).toLong()
                                            curY = g.offY + ((curY - g.chOffY).toDouble() * g.extCy / g.chExtCy).toLong()
                                            curCx = (curCx.toDouble() * g.extCx / g.chExtCx).toLong()
                                            curCy = (curCy.toDouble() * g.extCy / g.chExtCy).toLong()
                                        }
                                    }
                                    val bounds = RectF(
                                        (curX.toFloat() / slideWidthEmus.toFloat()).coerceIn(-0.1f, 1.1f),
                                        (curY.toFloat() / slideHeightEmus.toFloat()).coerceIn(-0.1f, 1.1f),
                                        ((curX + curCx).toFloat() / slideWidthEmus.toFloat()).coerceIn(-0.1f, 1.2f),
                                        ((curY + curCy).toFloat() / slideHeightEmus.toFloat()).coerceIn(-0.1f, 1.2f)
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
                        "graphicFrame" -> {
                            inGraphicFrame = false
                            if (tableRows.isNotEmpty() && extCx > 0 && extCy > 0 && slideWidthEmus > 0 && slideHeightEmus > 0) {
                                var curX = offX
                                var curY = offY
                                var curCx = extCx
                                var curCy = extCy
                                for (i in groupStack.indices.reversed()) {
                                    val g = groupStack[i]
                                    if (g.chExtCx > 0 && g.chExtCy > 0) {
                                        curX = g.offX + ((curX - g.chOffX).toDouble() * g.extCx / g.chExtCx).toLong()
                                        curY = g.offY + ((curY - g.chOffY).toDouble() * g.extCy / g.chExtCy).toLong()
                                        curCx = (curCx.toDouble() * g.extCx / g.chExtCx).toLong()
                                        curCy = (curCy.toDouble() * g.extCy / g.chExtCy).toLong()
                                    }
                                }
                                val bounds = RectF(
                                    (curX.toFloat() / slideWidthEmus.toFloat()).coerceIn(-0.1f, 1.1f),
                                    (curY.toFloat() / slideHeightEmus.toFloat()).coerceIn(-0.1f, 1.1f),
                                    ((curX + curCx).toFloat() / slideWidthEmus.toFloat()).coerceIn(-0.1f, 1.2f),
                                    ((curY + curCy).toFloat() / slideHeightEmus.toFloat()).coerceIn(-0.1f, 1.2f)
                                )
                                elements.add(
                                    TableElement(
                                        bounds = bounds,
                                        zIndex = zIndexCounter++,
                                        rows = tableRows,
                                        colWidthWeights = tableColWeights
                                    )
                                )
                            }
                        }
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
            backgroundImage = backgroundImage,
            gradientEndColor = gradientEndColor,
            elements = elements.sortedBy { it.zIndex },
            notes = notes,
            fullSearchableText = searchableTextBuilder.toString().trim(),
            slideWidthEmus = slideWidthEmus,
            slideHeightEmus = slideHeightEmus
        )
    }

    private fun parseNotesText(zip: ZipFile, notesPath: String): String {
        return runCatching {
            val entry = zip.getEntry(notesPath) ?: return@runCatching ""
            val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            val parser = createPullParser().apply { setInput(StringReader(xml)) }
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
