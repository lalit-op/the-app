package com.example.oneread

import androidx.compose.ui.graphics.Color
import com.example.oneread.ui.screens.TextReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextViewerTest {

    @Test
    fun test1_allThemesHaveRequiredIdsAndLabels() {
        assertEquals("white", TextReaderTheme.WHITE.id)
        assertEquals("White", TextReaderTheme.WHITE.label)
        assertEquals(Color(0xFFFFFFFF), TextReaderTheme.WHITE.bg)
        assertEquals(Color(0xFF111827), TextReaderTheme.WHITE.text)

        assertEquals("light", TextReaderTheme.LIGHT.id)
        assertEquals("Light", TextReaderTheme.LIGHT.label)
        assertEquals(Color(0xFFF3F4F6), TextReaderTheme.LIGHT.bg)
        assertEquals(Color(0xFF1F2937), TextReaderTheme.LIGHT.text)

        assertEquals("sepia", TextReaderTheme.SEPIA.id)
        assertEquals("Yellow / Sepia", TextReaderTheme.SEPIA.label)
        assertEquals(Color(0xFFFFF4CC), TextReaderTheme.SEPIA.bg)
        assertEquals(Color(0xFF292524), TextReaderTheme.SEPIA.text)

        assertEquals("dark", TextReaderTheme.DARK.id)
        assertEquals("Dark", TextReaderTheme.DARK.label)
        assertEquals(Color(0xFF111827), TextReaderTheme.DARK.bg)
        assertEquals(Color(0xFFF9FAFB), TextReaderTheme.DARK.text)
    }

    @Test
    fun test2_fromIdReturnsCorrectTheme() {
        assertEquals(TextReaderTheme.WHITE, TextReaderTheme.fromId("white"))
        assertEquals(TextReaderTheme.LIGHT, TextReaderTheme.fromId("light"))
        assertEquals(TextReaderTheme.SEPIA, TextReaderTheme.fromId("sepia"))
        assertEquals(TextReaderTheme.DARK, TextReaderTheme.fromId("dark"))

        // Case insensitivity
        assertEquals(TextReaderTheme.WHITE, TextReaderTheme.fromId("WHITE"))
        assertEquals(TextReaderTheme.SEPIA, TextReaderTheme.fromId("Sepia"))

        // Unknown / null fallback to DARK
        assertEquals(TextReaderTheme.DARK, TextReaderTheme.fromId(null))
        assertEquals(TextReaderTheme.DARK, TextReaderTheme.fromId("unknown_theme"))
    }

    @Test
    fun test3_nextThemeCyclesThroughAllThemes() {
        assertEquals(TextReaderTheme.LIGHT, TextReaderTheme.WHITE.nextTheme())
        assertEquals(TextReaderTheme.SEPIA, TextReaderTheme.LIGHT.nextTheme())
        assertEquals(TextReaderTheme.DARK, TextReaderTheme.SEPIA.nextTheme())
        assertEquals(TextReaderTheme.WHITE, TextReaderTheme.DARK.nextTheme())
    }

    @Test
    fun test4_themeContrastAdjustments() {
        // Dark theme has light text
        assertTrue(TextReaderTheme.DARK.text.red > 0.8f && TextReaderTheme.DARK.text.green > 0.8f)
        // White theme has dark text
        assertTrue(TextReaderTheme.WHITE.text.red < 0.2f && TextReaderTheme.WHITE.text.green < 0.2f)
        // Sepia has dark text
        assertTrue(TextReaderTheme.SEPIA.text.red < 0.3f && TextReaderTheme.SEPIA.text.green < 0.3f)
        // Light has dark text
        assertTrue(TextReaderTheme.LIGHT.text.red < 0.3f && TextReaderTheme.LIGHT.text.green < 0.3f)
    }

    @Test
    fun test5_expandTabsPreservesColumnsAndIndentations() {
        fun expandTabs(text: String, tabSize: Int = 4): String {
            if (!text.contains('\t')) return text
            val sb = StringBuilder(text.length + 16)
            var col = 0
            for (i in 0 until text.length) {
                val ch = text[i]
                if (ch == '\t') {
                    val spaces = tabSize - (col % tabSize)
                    for (s in 0 until spaces) sb.append(' ')
                    col += spaces
                } else {
                    sb.append(ch)
                    col++
                }
            }
            return sb.toString()
        }

        val leadingTab = "\tHello"
        assertEquals("    Hello", expandTabs(leadingTab))

        val midTab = "a\tb"
        assertEquals("a   b", expandTabs(midTab))

        val noTab = "Hello World"
        assertEquals("Hello World", expandTabs(noTab))
    }

    @Test
    fun test6_linesPreservesBlankLinesAndFormatting() {
        val sampleText = "Heading 1\n\n    Indented line\nhttps://example.com/very/long/url/that/should/not/break\n\nEnd"
        val lines = sampleText.lines()

        assertEquals(6, lines.size)
        assertEquals("Heading 1", lines[0])
        assertEquals("", lines[1]) // Blank line preserved
        assertEquals("    Indented line", lines[2]) // Indentation preserved
        assertEquals("https://example.com/very/long/url/that/should/not/break", lines[3]) // URL preserved
        assertEquals("", lines[4]) // Blank line preserved
        assertEquals("End", lines[5])
    }
}
