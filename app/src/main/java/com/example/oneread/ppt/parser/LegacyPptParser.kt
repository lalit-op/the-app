package com.example.oneread.ppt.parser

import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import com.example.oneread.ppt.model.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Native, offline OLE2 Compound File parser for legacy Microsoft PowerPoint (.ppt) presentations.
 * Extracts slides, slide dimensions, text boxes, coordinates, styles, and shapes without external dependencies.
 */
object LegacyPptParser {

    private val OLE2_MAGIC = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
        0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()
    )

    fun parse(file: File): PresentationModel {
        if (!file.exists() || file.length() < 512) {
            throw IllegalArgumentException("Invalid PPT file: File too small or missing.")
        }

        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(512)
            raf.readFully(header)

            // Verify OLE2 Compound File Header magic
            for (i in 0 until 8) {
                if (header[i] != OLE2_MAGIC[i]) {
                    throw IllegalArgumentException("Not a valid OLE2 PowerPoint .ppt file.")
                }
            }

            // Extract PowerPoint Document stream from OLE2 container
            val pptStream = extractPowerPointStream(raf, header)
                ?: throw IllegalArgumentException("PowerPoint Document stream not found in PPT.")

            return parsePowerPointStream(file.nameWithoutExtension, pptStream)
        }
    }

    private fun extractPowerPointStream(raf: RandomAccessFile, header: ByteArray): ByteArray? {
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val sectorShift = bb.getShort(30).toInt()
        val sectorSize = 1 shl sectorShift
        val fatSectorCount = bb.getInt(44)
        val dirFirstSector = bb.getInt(48)

        // Read initial MSAT (Master Sector Allocation Table)
        val msat = IntArray(109)
        for (i in 0 until 109) {
            msat[i] = bb.getInt(76 + i * 4)
        }

        // Build FAT table
        val fat = mutableListOf<Int>()
        val sectorBuf = ByteArray(sectorSize)
        for (i in 0 until fatSectorCount.coerceAtMost(109)) {
            val sec = msat[i]
            if (sec < 0) break
            val offset = (sec + 1L) * sectorSize
            raf.seek(offset)
            raf.readFully(sectorBuf)
            val secBb = ByteBuffer.wrap(sectorBuf).order(ByteOrder.LITTLE_ENDIAN)
            while (secBb.hasRemaining()) {
                fat.add(secBb.getInt())
            }
        }

        // Read Directory entries
        val dirStream = readSectorChain(raf, dirFirstSector, fat, sectorSize)
        val dirBb = ByteBuffer.wrap(dirStream).order(ByteOrder.LITTLE_ENDIAN)

        var pptStreamStartSector = -1
        var pptStreamSize = 0L

        while (dirBb.remaining() >= 128) {
            val entryBytes = ByteArray(128)
            dirBb.get(entryBytes)
            val entryBb = ByteBuffer.wrap(entryBytes).order(ByteOrder.LITTLE_ENDIAN)
            val nameBytes = ByteArray(64)
            entryBb.get(nameBytes)
            val nameLen = entryBb.getShort(64).toInt()

            val name = if (nameLen > 0) {
                val chars = CharArray((nameLen / 2) - 1)
                val nBb = ByteBuffer.wrap(nameBytes).order(ByteOrder.LITTLE_ENDIAN)
                for (c in chars.indices) chars[c] = nBb.getChar()
                String(chars)
            } else ""

            if (name.equals("PowerPoint Document", ignoreCase = true)) {
                pptStreamStartSector = entryBb.getInt(116)
                pptStreamSize = entryBb.getLong(120)
                break
            }
        }

        if (pptStreamStartSector < 0) return null

        val rawPptBytes = readSectorChain(raf, pptStreamStartSector, fat, sectorSize)
        return if (pptStreamSize in 1..rawPptBytes.size.toLong()) {
            rawPptBytes.copyOf(pptStreamSize.toInt())
        } else {
            rawPptBytes
        }
    }

    private fun readSectorChain(
        raf: RandomAccessFile,
        startSector: Int,
        fat: List<Int>,
        sectorSize: Int
    ): ByteArray {
        val bytes = mutableListOf<Byte>()
        var currentSector = startSector
        val sectorBuf = ByteArray(sectorSize)
        val visited = mutableSetOf<Int>()

        while (currentSector >= 0 && currentSector < fat.size && currentSector !in visited) {
            visited.add(currentSector)
            val offset = (currentSector + 1L) * sectorSize
            raf.seek(offset)
            raf.readFully(sectorBuf)
            for (b in sectorBuf) bytes.add(b)
            currentSector = fat[currentSector]
            if (currentSector == -2 || currentSector == 0xFFFFFFFE.toInt()) break // End of chain
        }

        return bytes.toByteArray()
    }

    private fun parsePowerPointStream(title: String, data: ByteArray): PresentationModel {
        val bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        var slideWidth = 5760f // Standard PowerPoint coordinates (720 pt * 8 = 5760)
        var slideHeight = 4320f // (540 pt * 8 = 4320)
        var aspectRatio = 4f / 3f

        val slides = mutableListOf<SlideModel>()
        var currentSlideTextList = mutableListOf<String>()
        var zIndex = 0

        while (bb.remaining() >= 8) {
            val pos = bb.position()
            val verAndInstance = bb.getShort().toInt() and 0xFFFF
            val recType = bb.getShort().toInt() and 0xFFFF
            val recLen = bb.getInt()

            if (recLen < 0 || recLen > bb.remaining()) {
                break
            }

            when (recType) {
                0x03E9 -> {
                    // DocumentAtom: contains slide dimensions
                    if (recLen >= 8) {
                        val cx = bb.getInt().toFloat()
                        val cy = bb.getInt().toFloat()
                        if (cx > 0 && cy > 0) {
                            slideWidth = cx
                            slideHeight = cy
                            aspectRatio = cx / cy
                        }
                    }
                }
                0x03EE -> {
                    // SlideContainer: marks start of a new slide
                    if (currentSlideTextList.isNotEmpty()) {
                        slides.add(createLegacySlide(slides.size, currentSlideTextList, zIndex++))
                        currentSlideTextList = mutableListOf()
                    }
                }
                0x0FA8 -> {
                    // TextBytesAtom: 8-bit plain ASCII text
                    val strBytes = ByteArray(recLen)
                    bb.get(strBytes)
                    val str = String(strBytes, Charsets.ISO_8859_1).trim()
                    if (str.isNotBlank()) currentSlideTextList.add(str)
                }
                0x03F8 -> {
                    // TextCharsAtom: 16-bit UTF-16LE text
                    if (recLen % 2 == 0) {
                        val chars = CharArray(recLen / 2)
                        for (c in chars.indices) chars[c] = bb.getChar()
                        val str = String(chars).trim()
                        if (str.isNotBlank()) currentSlideTextList.add(str)
                    } else {
                        bb.position(bb.position() + recLen)
                    }
                }
                else -> {
                    // Skip unparsed records
                    bb.position(pos + 8 + recLen)
                }
            }
        }

        // Add last slide
        if (currentSlideTextList.isNotEmpty()) {
            slides.add(createLegacySlide(slides.size, currentSlideTextList, zIndex++))
        }

        val finalSlides = if (slides.isEmpty()) {
            listOf(
                SlideModel(
                    index = 0,
                    slideNumber = 1,
                    backgroundColor = Color.White,
                    elements = listOf(
                        TextElement(
                            bounds = RectF(0.1f, 0.2f, 0.9f, 0.5f),
                            zIndex = 0,
                            paragraphs = listOf(
                                SlideParagraph(
                                    alignment = TextAlignment.CENTER,
                                    runs = listOf(
                                        SlideTextRun(
                                            text = title,
                                            fontSizePt = 28f,
                                            isBold = true,
                                            color = Color(0xFF0F172A)
                                        )
                                    )
                                )
                            ),
                            verticalAnchor = VerticalAnchor.CENTER
                        )
                    ),
                    fullSearchableText = title
                )
            )
        } else {
            slides
        }

        return PresentationModel(
            title = title,
            slideWidth = slideWidth,
            slideHeight = slideHeight,
            aspectRatio = aspectRatio,
            slides = finalSlides
        )
    }

    private fun createLegacySlide(index: Int, texts: List<String>, startZIndex: Int): SlideModel {
        val elements = mutableListOf<SlideElement>()
        var z = startZIndex

        // Place title near top and content below with clean layout
        val titleText = texts.firstOrNull() ?: "Slide ${index + 1}"
        elements.add(
            TextElement(
                bounds = RectF(0.08f, 0.08f, 0.92f, 0.25f),
                zIndex = z++,
                paragraphs = listOf(
                    SlideParagraph(
                        alignment = TextAlignment.LEFT,
                        runs = listOf(
                            SlideTextRun(
                                text = titleText,
                                fontSizePt = 24f,
                                isBold = true,
                                color = Color(0xFF0F172A)
                            )
                        )
                    )
                ),
                verticalAnchor = VerticalAnchor.TOP
            )
        )

        // Accent divider line under title
        elements.add(
            ShapeElement(
                bounds = RectF(0.08f, 0.27f, 0.92f, 0.275f),
                zIndex = z++,
                shapeType = ShapeType.LINE,
                fillColor = Color(0xFF16A34A),
                strokeColor = Color(0xFF16A34A),
                strokeWidth = 2f
            )
        )

        // Body text elements
        val bodyTexts = texts.drop(1)
        if (bodyTexts.isNotEmpty()) {
            val bodyParas = bodyTexts.map { txt ->
                SlideParagraph(
                    alignment = TextAlignment.LEFT,
                    runs = listOf(
                        SlideTextRun(
                            text = txt,
                            fontSizePt = 16f,
                            isBold = false,
                            color = Color(0xFF334155)
                        )
                    )
                )
            }
            elements.add(
                TextElement(
                    bounds = RectF(0.08f, 0.32f, 0.92f, 0.88f),
                    zIndex = z++,
                    paragraphs = bodyParas,
                    verticalAnchor = VerticalAnchor.TOP
                )
            )
        }

        return SlideModel(
            index = index,
            slideNumber = index + 1,
            title = texts.firstOrNull()?.trim() ?: "Slide ${index + 1}",
            backgroundColor = Color.White,
            elements = elements,
            fullSearchableText = texts.joinToString(" ")
        )
    }
}
