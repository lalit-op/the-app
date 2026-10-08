package com.example.oneread.util

import android.content.Context
import android.util.LruCache
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

data class PdfHighlightRect(
    val normalizedLeft: Float,   // 0.0 .. 1.0 relative to page width
    val normalizedTop: Float,    // 0.0 .. 1.0 relative to page height
    val normalizedWidth: Float,  // 0.0 .. 1.0 relative to page width
    val normalizedHeight: Float  // 0.0 .. 1.0 relative to page height
)

data class PdfSearchOccurrence(
    val matchIndex: Int,         // 0-based across whole document
    val pageIndex: Int,          // 0-based page number
    val rects: List<PdfHighlightRect>,
    val snippet: String
)

data class CharPos(
    val char: Char,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val pageWidth: Float,
    val pageHeight: Float
)

data class PageData(
    val pageIndex: Int,
    val text: String,
    val characters: List<CharPos>,
    val pageWidth: Float,
    val pageHeight: Float
)

object PdfSearchEngine {
    private var isInitialized = false
    private val cache = LruCache<String, List<PageData>>(5)

    fun init(context: Context) {
        if (!isInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private class MultiPageStripper : PDFTextStripper() {
        init {
            sortByPosition = true
        }

        val pageDataList = mutableListOf<PageData>()
        private var currentPageCharacters = mutableListOf<CharPos>()
        private var currentPageSb = StringBuilder()
        private var lastX = -1f
        private var lastY = -1f
        private var lastWidth = 0f
        private var pWidth = 0f
        private var pHeight = 0f
        private var currPageIndex = 0

        override fun startPage(page: PDPage) {
            currPageIndex = (currentPageNo - 1).coerceAtLeast(0)
            currentPageCharacters = mutableListOf()
            currentPageSb = StringBuilder()
            lastX = -1f
            lastY = -1f
            lastWidth = 0f
            val box = page.cropBox ?: page.mediaBox
            pWidth = box.width
            pHeight = box.height
            super.startPage(page)
        }

        override fun endPage(page: PDPage) {
            super.endPage(page)
            val box = page.cropBox ?: page.mediaBox
            val pw = if (pWidth > 0f) pWidth else box.width
            val ph = if (pHeight > 0f) pHeight else box.height
            pageDataList.add(
                PageData(
                    pageIndex = currPageIndex,
                    text = currentPageSb.toString(),
                    characters = currentPageCharacters,
                    pageWidth = pw,
                    pageHeight = ph
                )
            )
        }

        override fun processTextPosition(text: TextPosition) {
            val unicode = text.unicode ?: return
            if (unicode.isEmpty()) return

            if (text.pageWidth > 0f) pWidth = text.pageWidth
            if (text.pageHeight > 0f) pHeight = text.pageHeight

            val x = text.xDirAdj
            val y = text.yDirAdj
            val w = text.widthDirAdj
            val h = if (text.heightDir > 0f) text.heightDir else text.fontSizeInPt

            if (lastX >= 0f) {
                val yDiff = abs(y - lastY)
                if (yDiff > h * 0.6f) {
                    currentPageSb.append('\n')
                    currentPageCharacters.add(CharPos('\n', lastX + lastWidth, lastY, 0f, h, pWidth, pHeight))
                } else {
                    val spaceDist = x - (lastX + lastWidth)
                    val spaceThreshold = text.widthOfSpace.takeIf { it > 0f } ?: (h * 0.25f)
                    if (spaceDist > spaceThreshold) {
                        currentPageSb.append(' ')
                        currentPageCharacters.add(CharPos(' ', lastX + lastWidth, y, spaceDist, h, pWidth, pHeight))
                    }
                }
            }

            val charWidth = if (unicode.isNotEmpty()) w / unicode.length else w
            for (i in unicode.indices) {
                val charX = x + i * charWidth
                currentPageSb.append(unicode[i])
                currentPageCharacters.add(CharPos(unicode[i], charX, y, charWidth, h, pWidth, pHeight))
            }

            lastX = x
            lastY = y
            lastWidth = w
        }
    }

    suspend fun getPageData(filePath: String): List<PageData> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext emptyList()
        val cacheKey = "${file.absolutePath}:${file.lastModified()}"
        cache.get(cacheKey)?.let { return@withContext it }

        var doc: PDDocument? = null
        try {
            doc = PDDocument.load(file)
            val stripper = MultiPageStripper()
            stripper.getText(doc)
            val pages = stripper.pageDataList
            cache.put(cacheKey, pages)
            return@withContext pages
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { doc?.close() } catch (_: Exception) {}
        }
        emptyList()
    }

    suspend fun search(
        filePath: String,
        query: String
    ): List<PdfSearchOccurrence> = withContext(Dispatchers.Default) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext emptyList()

        val pages = getPageData(filePath)
        val occurrences = mutableListOf<PdfSearchOccurrence>()
        var globalIndex = 0

        for (page in pages) {
            val pageText = page.text
            if (pageText.isEmpty() || page.characters.isEmpty()) continue

            var searchStart = 0
            while (searchStart < pageText.length) {
                val matchStart = pageText.indexOf(cleanQuery, searchStart, ignoreCase = true)
                if (matchStart == -1) break

                val matchEnd = matchStart + cleanQuery.length
                val matchedChars = page.characters.subList(
                    matchStart.coerceIn(0, page.characters.size),
                    matchEnd.coerceIn(0, page.characters.size)
                ).filter { it.char != '\n' }

                if (matchedChars.isNotEmpty()) {
                    val lineGroups = mutableListOf<MutableList<CharPos>>()
                    var currentGroup = mutableListOf<CharPos>()

                    for (charPos in matchedChars) {
                        if (currentGroup.isEmpty()) {
                            currentGroup.add(charPos)
                        } else {
                            val prev = currentGroup.last()
                            if (abs(charPos.y - prev.y) > prev.height * 0.5f) {
                                lineGroups.add(currentGroup)
                                currentGroup = mutableListOf(charPos)
                            } else {
                                currentGroup.add(charPos)
                            }
                        }
                    }
                    if (currentGroup.isNotEmpty()) {
                        lineGroups.add(currentGroup)
                    }

                    val pw = page.pageWidth.coerceAtLeast(1f)
                    val ph = page.pageHeight.coerceAtLeast(1f)

                    val rects = lineGroups.mapNotNull { group ->
                        if (group.isEmpty()) return@mapNotNull null
                        val minX = group.minOf { it.x }
                        val maxX = group.maxOf { it.x + it.width }
                        val avgY = group.map { it.y }.average().toFloat()
                        val maxH = group.maxOf { it.height }

                        // In PDFBox text.yDirAdj is baseline
                        val topY = (avgY - maxH * 0.95f).coerceAtLeast(0f)
                        val bottomY = avgY + maxH * 0.25f
                        val heightPx = (bottomY - topY).coerceAtLeast(maxH)

                        val padX = 1.5f
                        PdfHighlightRect(
                            normalizedLeft = ((minX - padX) / pw).coerceIn(0f, 1f),
                            normalizedTop = (topY / ph).coerceIn(0f, 1f),
                            normalizedWidth = ((maxX - minX + padX * 2) / pw).coerceIn(0.005f, 1f),
                            normalizedHeight = (heightPx / ph).coerceIn(0.005f, 1f)
                        )
                    }

                    if (rects.isNotEmpty()) {
                        val snippetStart = (matchStart - 20).coerceAtLeast(0)
                        val snippetEnd = (matchEnd + 20).coerceAtMost(pageText.length)
                        val snippet = pageText.substring(snippetStart, snippetEnd).replace('\n', ' ')

                        occurrences.add(
                            PdfSearchOccurrence(
                                matchIndex = globalIndex++,
                                pageIndex = page.pageIndex,
                                rects = rects,
                                snippet = snippet
                            )
                        )
                    }
                }

                searchStart = matchStart + 1
            }
        }

        occurrences
    }
}
