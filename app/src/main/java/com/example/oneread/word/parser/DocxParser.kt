package com.example.oneread.word.parser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.oneread.word.model.DocxBlock
import com.example.oneread.word.model.DocxDocument
import com.example.oneread.word.model.DocxRun
import com.example.oneread.word.model.DocxTableCell
import com.example.oneread.word.model.DocxTableRow
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * High-fidelity, pure Kotlin OpenXML DOCX parser using native Android XML DOM.
 * Decodes all XML entities properly (&amp; -> &), isolates XML attributes (xml:space),
 * parses structured tables, embedded media/logos, headings, and character formatting.
 */
object DocxParser {

    fun parse(file: File): DocxDocument {
        if (!file.exists() || !file.canRead()) {
            return DocxDocument(title = file.name)
        }

        return try {
            val zip = ZipFile(file)
            val mediaMap = mutableMapOf<String, Bitmap>()

            // 1. Parse relationship targets to map rId -> target media/header
            val relsMap = parseRelationships(zip, "word/_rels/document.xml.rels")
            val headerRelsMap = parseRelationships(zip, "word/_rels/header1.xml.rels")
            val allRels = relsMap + headerRelsMap

            // 2. Decode embedded images (logos, signatures, diagrams)
            extractMedia(zip, allRels, mediaMap)

            // 3. Parse headers (university logo and top header elements)
            val headerBlocks = parseHeaderElements(zip, allRels, mediaMap)

            // 4. Parse main document XML
            val docEntry = zip.getEntry("word/document.xml")
            if (docEntry != null) {
                val dbf = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                }
                val dom = zip.getInputStream(docEntry).use { dbf.newDocumentBuilder().parse(it) }
                val root = dom.documentElement
                val body = findFirstChild(root, "body")

                val bodyBlocks = mutableListOf<DocxBlock>()
                var pageWidthPt = 595.28f
                var pageHeightPt = 841.89f
                var marginLeftPt = 54f
                var marginRightPt = 54f
                var marginTopPt = 54f
                var marginBottomPt = 54f

                if (body != null) {
                    var child = body.firstChild
                    while (child != null) {
                        if (child.nodeType == Node.ELEMENT_NODE) {
                            val el = child as Element
                            val local = el.localName ?: el.tagName
                            when (local) {
                                "p" -> {
                                    val paragraph = parseParagraph(el, mediaMap)
                                    if (paragraph != null) {
                                        bodyBlocks.add(paragraph)
                                    }
                                }
                                "tbl" -> {
                                    val table = parseTable(el, mediaMap)
                                    if (table != null) {
                                        bodyBlocks.add(table)
                                    }
                                }
                                "sectPr" -> {
                                    val pgSz = findFirstChild(el, "pgSz")
                                    if (pgSz != null) {
                                        val w = getAttr(pgSz, "w")?.toFloatOrNull()
                                        val h = getAttr(pgSz, "h")?.toFloatOrNull()
                                        if (w != null && w > 0) pageWidthPt = w / 20f
                                        if (h != null && h > 0) pageHeightPt = h / 20f
                                    }
                                    val pgMar = findFirstChild(el, "pgMar")
                                    if (pgMar != null) {
                                        getAttr(pgMar, "left")?.toFloatOrNull()?.let { marginLeftPt = it / 20f }
                                        getAttr(pgMar, "right")?.toFloatOrNull()?.let { marginRightPt = it / 20f }
                                        getAttr(pgMar, "top")?.toFloatOrNull()?.let { marginTopPt = it / 20f }
                                        getAttr(pgMar, "bottom")?.toFloatOrNull()?.let { marginBottomPt = it / 20f }
                                    }
                                }
                            }
                        }
                        child = child.nextSibling
                    }
                }

                zip.close()

                DocxDocument(
                    title = file.nameWithoutExtension,
                    headerElements = headerBlocks,
                    bodyElements = bodyBlocks,
                    mediaMap = mediaMap,
                    pageWidthPt = pageWidthPt,
                    pageHeightPt = pageHeightPt,
                    marginLeftPt = marginLeftPt,
                    marginRightPt = marginRightPt,
                    marginTopPt = marginTopPt,
                    marginBottomPt = marginBottomPt
                )
            } else {
                zip.close()
                parsePlainTextFallback(file)
            }
        } catch (_: Exception) {
            parsePlainTextFallback(file)
        }
    }

    private fun parseRelationships(zip: ZipFile, relsPath: String): Map<String, String> {
        val relsEntry = zip.getEntry(relsPath) ?: return emptyMap()
        val rels = mutableMapOf<String, String>()
        runCatching {
            val dbf = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val dom = zip.getInputStream(relsEntry).use { dbf.newDocumentBuilder().parse(it) }
            val list = dom.getElementsByTagName("Relationship")
            for (i in 0 until list.length) {
                val node = list.item(i)
                if (node is Element) {
                    val id = node.getAttribute("Id")
                    val target = node.getAttribute("Target")
                    if (id.isNotBlank() && target.isNotBlank()) {
                        rels[id] = target
                    }
                }
            }
        }
        return rels
    }

    private fun extractMedia(zip: ZipFile, rels: Map<String, String>, mediaMap: MutableMap<String, Bitmap>) {
        for ((rId, target) in rels) {
            if (target.contains("media/", ignoreCase = true) ||
                target.endsWith(".png", ignoreCase = true) ||
                target.endsWith(".jpeg", ignoreCase = true) ||
                target.endsWith(".jpg", ignoreCase = true) ||
                target.endsWith(".emf", ignoreCase = true) ||
                target.endsWith(".wmf", ignoreCase = true)
            ) {
                // Normalize zip path
                val zipPath = if (target.startsWith("word/")) target else "word/${target.removePrefix("../").removePrefix("/")}"
                val entry = zip.getEntry(zipPath) ?: zip.getEntry(target)
                if (entry != null) {
                    runCatching {
                        zip.getInputStream(entry).use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                mediaMap[rId] = bmp
                                mediaMap[target] = bmp
                                mediaMap[zipPath] = bmp
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseHeaderElements(zip: ZipFile, rels: Map<String, String>, mediaMap: Map<String, Bitmap>): List<DocxBlock> {
        val headerBlocks = mutableListOf<DocxBlock>()
        // Check for header entries in relationships or standard word/header1.xml
        val headerPaths = mutableListOf<String>()
        for ((_, target) in rels) {
            if (target.contains("header", ignoreCase = true) && target.endsWith(".xml", ignoreCase = true)) {
                val path = if (target.startsWith("word/")) target else "word/${target.removePrefix("../").removePrefix("/")}"
                headerPaths.add(path)
            }
        }
        if (headerPaths.isEmpty() && zip.getEntry("word/header1.xml") != null) {
            headerPaths.add("word/header1.xml")
        }

        for (path in headerPaths) {
            val entry = zip.getEntry(path) ?: continue
            runCatching {
                val dbf = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
                val dom = zip.getInputStream(entry).use { dbf.newDocumentBuilder().parse(it) }
                val root = dom.documentElement
                var child = root.firstChild
                while (child != null) {
                    if (child.nodeType == Node.ELEMENT_NODE) {
                        val el = child as Element
                        val local = el.localName ?: el.tagName
                        when (local) {
                            "p" -> {
                                val p = parseParagraph(el, mediaMap)
                                if (p != null) headerBlocks.add(p)
                            }
                            "tbl" -> {
                                val t = parseTable(el, mediaMap)
                                if (t != null) headerBlocks.add(t)
                            }
                        }
                    }
                    child = child.nextSibling
                }
            }
        }
        return headerBlocks
    }

    private fun parseParagraph(pEl: Element, mediaMap: Map<String, Bitmap>): DocxBlock.Paragraph? {
        var alignment = TextAlign.Start
        var spaceBeforeDp = 0f
        var spaceAfterDp = 4f
        var indentStartDp = 0f
        var isHeading = false
        var headingLevel = 0
        var hasBottomBorder = false

        // Parse pPr
        val pPr = findFirstChild(pEl, "pPr")
        if (pPr != null) {
            val jc = findFirstChild(pPr, "jc")
            if (jc != null) {
                when (getAttr(jc, "val")?.lowercase()) {
                    "center" -> alignment = TextAlign.Center
                    "right", "end" -> alignment = TextAlign.End
                    "both", "distribute" -> alignment = TextAlign.Justify
                    else -> alignment = TextAlign.Start
                }
            }

            val spacing = findFirstChild(pPr, "spacing")
            if (spacing != null) {
                getAttr(spacing, "before")?.toFloatOrNull()?.let { spaceBeforeDp = (it / 20f).coerceIn(0f, 48f) }
                getAttr(spacing, "after")?.toFloatOrNull()?.let { spaceAfterDp = (it / 20f).coerceIn(0f, 48f) }
            }

            val ind = findFirstChild(pPr, "ind")
            if (ind != null) {
                getAttr(ind, "left")?.toFloatOrNull()?.let { indentStartDp = (it / 20f).coerceIn(0f, 100f) }
            }

            val pStyle = findFirstChild(pPr, "pStyle")
            if (pStyle != null) {
                val s = getAttr(pStyle, "val")?.lowercase() ?: ""
                if (s.contains("heading") || s.contains("title")) {
                    isHeading = true
                    headingLevel = if (s.contains("1")) 1 else if (s.contains("2")) 2 else 3
                }
            }

            val pBdr = findFirstChild(pPr, "pBdr")
            if (pBdr != null && findFirstChild(pBdr, "bottom") != null) {
                hasBottomBorder = true
            }
        }

        // Parse runs and inline drawings
        val runs = mutableListOf<DocxRun>()
        var child = pEl.firstChild
        while (child != null) {
            if (child.nodeType == Node.ELEMENT_NODE) {
                val el = child as Element
                val local = el.localName ?: el.tagName
                when (local) {
                    "r" -> {
                        val parsedRun = parseRun(el, mediaMap)
                        if (parsedRun != null) runs.add(parsedRun)
                    }
                    "hyperlink" -> {
                        var hChild = el.firstChild
                        while (hChild != null) {
                            if (hChild.nodeType == Node.ELEMENT_NODE) {
                                val hEl = hChild as Element
                                if ((hEl.localName ?: hEl.tagName) == "r") {
                                    val parsedRun = parseRun(hEl, mediaMap)
                                    if (parsedRun != null) {
                                        runs.add(parsedRun.copy(color = Color(0xFF2563EB), isUnderline = true))
                                    }
                                }
                            }
                            hChild = hChild.nextSibling
                        }
                    }
                }
            }
            child = child.nextSibling
        }

        if (runs.isEmpty()) {
            return null
        }

        // If the runs have large bold text, treat as heading
        val firstRunSize = runs.firstOrNull()?.fontSizeSp ?: 14f
        if (firstRunSize >= 18f) {
            isHeading = true
            if (headingLevel == 0) headingLevel = if (firstRunSize >= 22f) 1 else 2
        }

        return DocxBlock.Paragraph(
            runs = runs,
            alignment = alignment,
            spaceBeforeDp = spaceBeforeDp,
            spaceAfterDp = spaceAfterDp,
            isHeading = isHeading,
            headingLevel = headingLevel,
            indentStartDp = indentStartDp,
            hasBottomBorder = hasBottomBorder
        )
    }

    private fun parseRun(rEl: Element, mediaMap: Map<String, Bitmap>): DocxRun? {
        var isBold = false
        var isItalic = false
        var isUnderline = false
        var isStrike = false
        var fontSizeSp = 14f
        var textColor: Color? = null
        var highlightColor: Color? = null
        var inlineImage: Bitmap? = null

        // Parse rPr
        val rPr = findFirstChild(rEl, "rPr")
        if (rPr != null) {
            val b = findFirstChild(rPr, "b")
            if (b != null) {
                val v = getAttr(b, "val")
                isBold = v == null || v == "1" || v == "true"
            }

            val i = findFirstChild(rPr, "i")
            if (i != null) {
                val v = getAttr(i, "val")
                isItalic = v == null || v == "1" || v == "true"
            }

            val u = findFirstChild(rPr, "u")
            if (u != null) {
                val v = getAttr(u, "val")
                isUnderline = v != "none"
            }

            val strike = findFirstChild(rPr, "strike")
            if (strike != null) isStrike = true

            val sz = findFirstChild(rPr, "sz")
            if (sz != null) {
                getAttr(sz, "val")?.toFloatOrNull()?.let {
                    fontSizeSp = (it / 2f).coerceIn(9f, 36f)
                }
            }

            val color = findFirstChild(rPr, "color")
            if (color != null) {
                val hex = getAttr(color, "val")
                if (hex != null && hex != "auto" && hex.length == 6) {
                    textColor = parseHexColor(hex)
                }
            }

            val highlight = findFirstChild(rPr, "highlight")
            if (highlight != null) {
                when (getAttr(highlight, "val")?.lowercase()) {
                    "yellow" -> highlightColor = Color(0xFFFEF08A)
                    "cyan" -> highlightColor = Color(0xFFA5F3FC)
                    "green" -> highlightColor = Color(0xFFBBF7D0)
                }
            }
        }

        // Parse text content and drawings
        val sb = StringBuilder()
        var child = rEl.firstChild
        while (child != null) {
            if (child.nodeType == Node.ELEMENT_NODE) {
                val el = child as Element
                val local = el.localName ?: el.tagName
                when (local) {
                    "t" -> {
                        // DOM node.textContent automatically decodes XML entities (&amp; -> &)
                        // and strips xml:space attributes cleanly without leaking them into text!
                        sb.append(el.textContent)
                    }
                    "br" -> sb.append("\n")
                    "tab" -> sb.append("    ")
                    "drawing", "pict" -> {
                        val blip = findDescendant(el, "blip") ?: findDescendant(el, "imagedata")
                        if (blip != null) {
                            val rId = getAttr(blip, "embed") ?: getAttr(blip, "id")
                            if (rId != null && mediaMap.containsKey(rId)) {
                                inlineImage = mediaMap[rId]
                            }
                        }
                    }
                }
            }
            child = child.nextSibling
        }

        val text = sb.toString()
        if (text.isEmpty() && inlineImage == null) {
            return null
        }

        return DocxRun(
            text = text,
            isBold = isBold,
            isItalic = isItalic,
            isUnderline = isUnderline,
            isStrike = isStrike,
            fontSizeSp = fontSizeSp,
            color = textColor,
            highlightColor = highlightColor,
            inlineImage = inlineImage
        )
    }

    private fun parseTable(tblEl: Element, mediaMap: Map<String, Bitmap>): DocxBlock.Table? {
        val colWeights = mutableListOf<Float>()
        val tblGrid = findFirstChild(tblEl, "tblGrid")
        if (tblGrid != null) {
            var gChild = tblGrid.firstChild
            while (gChild != null) {
                if (gChild.nodeType == Node.ELEMENT_NODE) {
                    val gEl = gChild as Element
                    if ((gEl.localName ?: gEl.tagName) == "gridCol") {
                        val w = getAttr(gEl, "w")?.toFloatOrNull() ?: 1000f
                        colWeights.add(w)
                    }
                }
                gChild = gChild.nextSibling
            }
        }

        val rows = mutableListOf<DocxTableRow>()
        var child = tblEl.firstChild
        while (child != null) {
            if (child.nodeType == Node.ELEMENT_NODE) {
                val el = child as Element
                if ((el.localName ?: el.tagName) == "tr") {
                    var isHeaderRow = false
                    val trPr = findFirstChild(el, "trPr")
                    if (trPr != null && findFirstChild(trPr, "tblHeader") != null) {
                        isHeaderRow = true
                    }

                    val cells = mutableListOf<DocxTableCell>()
                    var cChild = el.firstChild
                    while (cChild != null) {
                        if (cChild.nodeType == Node.ELEMENT_NODE) {
                            val cEl = cChild as Element
                            if ((cEl.localName ?: cEl.tagName) == "tc") {
                                var colSpan = 1
                                var cellBg: Color? = null
                                var alignment = TextAlign.Start

                                val tcPr = findFirstChild(cEl, "tcPr")
                                if (tcPr != null) {
                                    val gridSpan = findFirstChild(tcPr, "gridSpan")
                                    if (gridSpan != null) {
                                        colSpan = getAttr(gridSpan, "val")?.toIntOrNull() ?: 1
                                    }
                                    val shd = findFirstChild(tcPr, "shd")
                                    if (shd != null) {
                                        val fill = getAttr(shd, "fill")
                                        if (fill != null && fill != "auto" && fill != "none" && fill.length == 6) {
                                            cellBg = parseHexColor(fill)
                                        }
                                    }
                                }

                                val cellBlocks = mutableListOf<DocxBlock>()
                                var pNode = cEl.firstChild
                                while (pNode != null) {
                                    if (pNode.nodeType == Node.ELEMENT_NODE) {
                                        val pChildEl = pNode as Element
                                        if ((pChildEl.localName ?: pChildEl.tagName) == "p") {
                                            val p = parseParagraph(pChildEl, mediaMap)
                                            if (p != null) {
                                                cellBlocks.add(p)
                                                if (p.alignment != TextAlign.Start) {
                                                    alignment = p.alignment
                                                }
                                            }
                                        }
                                    }
                                    pNode = pNode.nextSibling
                                }

                                cells.add(
                                    DocxTableCell(
                                        blocks = cellBlocks,
                                        colSpan = colSpan,
                                        bgColor = cellBg,
                                        alignment = alignment
                                    )
                                )
                            }
                        }
                        cChild = cChild.nextSibling
                    }

                    if (cells.isNotEmpty()) {
                        rows.add(DocxTableRow(cells = cells, isHeaderRow = isHeaderRow))
                    }
                }
            }
            child = child.nextSibling
        }

        if (rows.isEmpty()) return null

        // Normalize column weights if missing
        val maxCols = rows.maxOfOrNull { it.cells.sumOf { c -> c.colSpan } } ?: 1
        val finalWeights = if (colWeights.size == maxCols) {
            colWeights
        } else {
            List(maxCols) { 1f }
        }

        return DocxBlock.Table(
            rows = rows,
            colWidthWeights = finalWeights
        )
    }

    private fun parsePlainTextFallback(file: File): DocxDocument {
        val lines = runCatching {
            val raw = file.readText(Charsets.ISO_8859_1)
            if (file.name.endsWith(".rtf", ignoreCase = true) || raw.trimStart().startsWith("{\\rtf")) {
                extractRtfText(raw).lines().map { it.trim() }.filter { it.isNotBlank() }
            } else {
                file.readLines()
            }
        }.getOrElse { emptyList() }
        val blocks = lines.filter { it.isNotBlank() }.map { line ->
            DocxBlock.Paragraph(
                runs = listOf(DocxRun(text = line, fontSizeSp = 14f)),
                spaceAfterDp = 6f
            )
        }
        return DocxDocument(
            title = file.nameWithoutExtension,
            bodyElements = blocks
        )
    }

    private fun extractRtfText(rtf: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = rtf.length
        var groupDepth = 0
        var skipGroupDepth = -1

        while (i < len) {
            val c = rtf[i]
            when (c) {
                '{' -> {
                    groupDepth++
                    i++
                }
                '}' -> {
                    if (groupDepth == skipGroupDepth) {
                        skipGroupDepth = -1
                    }
                    groupDepth--
                    i++
                }
                '\\' -> {
                    i++
                    if (i >= len) break
                    val next = rtf[i]
                    when (next) {
                        '\\', '{', '}' -> {
                            if (skipGroupDepth == -1) sb.append(next)
                            i++
                        }
                        '\'' -> {
                            i++
                            if (i + 2 <= len) {
                                val hex = rtf.substring(i, i + 2)
                                val byteVal = hex.toIntOrNull(16)
                                if (byteVal != null && skipGroupDepth == -1) {
                                    sb.append(byteVal.toChar())
                                }
                                i += 2
                            }
                        }
                        else -> {
                            val start = i
                            while (i < len && rtf[i].isLetter()) {
                                i++
                            }
                            val word = rtf.substring(start, i)
                            while (i < len && (rtf[i].isDigit() || rtf[i] == '-')) {
                                i++
                            }
                            if (i < len && rtf[i] == ' ') {
                                i++
                            }
                            when (word) {
                                "par", "line" -> if (skipGroupDepth == -1) sb.append("\n")
                                "tab" -> if (skipGroupDepth == -1) sb.append("\t")
                                "fonttbl", "colortbl", "stylesheet", "info", "pict" -> {
                                    skipGroupDepth = groupDepth
                                }
                            }
                        }
                    }
                }
                '\r', '\n' -> {
                    i++
                }
                else -> {
                    if (skipGroupDepth == -1) {
                        sb.append(c)
                    }
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun findFirstChild(parent: Element, localName: String): Element? {
        var child = parent.firstChild
        while (child != null) {
            if (child.nodeType == Node.ELEMENT_NODE) {
                val el = child as Element
                if ((el.localName ?: el.tagName) == localName) {
                    return el
                }
            }
            child = child.nextSibling
        }
        return null
    }

    private fun findDescendant(parent: Element, localName: String): Element? {
        val list = parent.getElementsByTagName("*")
        for (i in 0 until list.length) {
            val el = list.item(i) as? Element ?: continue
            if ((el.localName ?: el.tagName) == localName) {
                return el
            }
        }
        return null
    }

    private fun getAttr(el: Element, localName: String): String? {
        // Try localName, with w: prefix, or with any namespace
        val v = el.getAttribute(localName)
        if (v.isNotBlank()) return v
        val vPrefixed = el.getAttribute("w:$localName")
        if (vPrefixed.isNotBlank()) return vPrefixed
        // Check attributes iteration
        val attrs = el.attributes
        for (i in 0 until attrs.length) {
            val a = attrs.item(i)
            val name = a.localName ?: a.nodeName
            if (name == localName || name == "w:$localName") {
                return a.nodeValue
            }
        }
        return null
    }

    private fun parseHexColor(hex: String): Color? {
        return runCatching {
            val clean = hex.removePrefix("#")
            if (clean.length == 6) {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(r, g, b)
            } else null
        }.getOrNull()
    }
}
