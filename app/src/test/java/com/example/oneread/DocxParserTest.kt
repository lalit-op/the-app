package com.example.oneread

import com.example.oneread.word.model.DocxBlock
import com.example.oneread.word.parser.DocxParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSampleDocx(
        documentXmlContent: String,
        relsXmlContent: String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            </Relationships>"""
    ): File {
        val file = tempFolder.newFile("sample_assignment.docx")
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>""".toByteArray())
            zos.closeEntry()

            // word/_rels/document.xml.rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(relsXmlContent.toByteArray())
            zos.closeEntry()

            // word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(documentXmlContent.toByteArray())
            zos.closeEntry()
        }
        return file
    }

    @Test
    fun testDocxParsingRemovesXmlMarkupAndDecodesEntities() {
        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:pPr>
                            <w:pStyle w:val="Heading1"/>
                            <w:jc w:val="center"/>
                        </w:pPr>
                        <w:r>
                            <w:rPr>
                                <w:b w:val="true"/>
                                <w:sz w:val="36"/>
                            </w:rPr>
                            <w:t xml:space="preserve">Department of Computer Science &amp; Engineering</w:t>
                        </w:r>
                    </w:p>
                    <w:p>
                        <w:pPr>
                            <w:jc w:val="center"/>
                        </w:pPr>
                        <w:r>
                            <w:t xml:space="preserve">Mid-Term Examination &amp; Assignment 1</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>""".trimIndent()

        val docxFile = createSampleDocx(documentXml)
        val doc = DocxParser.parse(docxFile)

        assertNotNull(doc)
        assertEquals(2, doc.bodyElements.size)

        val firstBlock = doc.bodyElements[0] as DocxBlock.Paragraph
        val secondBlock = doc.bodyElements[1] as DocxBlock.Paragraph

        // 1. Entities decoded: &amp; becomes &
        assertTrue("Entity &amp; must be decoded to &", firstBlock.fullText.contains("Computer Science & Engineering"))
        assertTrue("Entity &amp; must be decoded to &", secondBlock.fullText.contains("Examination & Assignment 1"))

        // 2. xml:space="preserve" must NEVER appear in the content
        assertFalse("xml:space must not appear in text", firstBlock.fullText.contains("xml:space"))
        assertFalse("preserve must not appear in text", firstBlock.fullText.contains("preserve"))
        assertFalse("xml:space must not appear in text", secondBlock.fullText.contains("xml:space"))

        // 3. Headings preserved
        assertTrue("Heading style must be detected", firstBlock.isHeading)
    }

    @Test
    fun testDocxPreservesStructuredTableWithBloomsTaxonomyAndMarks() {
        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:tbl>
                        <w:tblGrid>
                            <w:gridCol w:val="1000"/>
                            <w:gridCol w:val="5000"/>
                            <w:gridCol w:val="2000"/>
                            <w:gridCol w:val="1000"/>
                            <w:gridCol w:val="1000"/>
                        </w:tblGrid>
                        <!-- Header Row -->
                        <w:tr>
                            <w:trPr>
                                <w:tblHeader/>
                            </w:trPr>
                            <w:tc><w:p><w:r><w:t>Q.No</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Question Statement</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Bloom's Level</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>CO</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Marks</w:t></w:r></w:p></w:tc>
                        </w:tr>
                        <!-- Data Row 1 -->
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>1(a)</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Explain the architecture of OpenXML packaging &amp; relationships.</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Understand (L2)</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>CO1</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>5</w:t></w:r></w:p></w:tc>
                        </w:tr>
                        <!-- Data Row 2 -->
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>1(b)</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Design a fault-tolerant document rendering engine in Jetpack Compose.</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Create (L6)</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>CO3</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>10</w:t></w:r></w:p></w:tc>
                        </w:tr>
                    </w:tbl>
                </w:body>
            </w:document>""".trimIndent()

        val docxFile = createSampleDocx(documentXml)
        val doc = DocxParser.parse(docxFile)

        assertNotNull(doc)
        assertEquals(1, doc.bodyElements.size)

        val tableBlock = doc.bodyElements[0] as DocxBlock.Table
        assertEquals(3, tableBlock.rows.size)

        // Verify Header Row
        val headerRow = tableBlock.rows[0]
        assertTrue(headerRow.isHeaderRow)
        assertEquals(5, headerRow.cells.size)
        assertEquals("Q.No", headerRow.cells[0].fullText)
        assertEquals("Question Statement", headerRow.cells[1].fullText)
        assertEquals("Bloom's Level", headerRow.cells[2].fullText)
        assertEquals("CO", headerRow.cells[3].fullText)
        assertEquals("Marks", headerRow.cells[4].fullText)

        // Verify Data Row 1 (Question remains in table column with Bloom's level, CO, and marks)
        val row1 = tableBlock.rows[1]
        assertFalse(row1.isHeaderRow)
        assertEquals(5, row1.cells.size)
        assertEquals("1(a)", row1.cells[0].fullText)
        assertTrue(row1.cells[1].fullText.contains("packaging & relationships"))
        assertEquals("Understand (L2)", row1.cells[2].fullText)
        assertEquals("CO1", row1.cells[3].fullText)
        assertEquals("5", row1.cells[4].fullText)

        // Verify Data Row 2
        val row2 = tableBlock.rows[2]
        assertEquals("1(b)", row2.cells[0].fullText)
        assertTrue(row2.cells[1].fullText.contains("Design a fault-tolerant document"))
        assertEquals("Create (L6)", row2.cells[2].fullText)
        assertEquals("CO3", row2.cells[3].fullText)
        assertEquals("10", row2.cells[4].fullText)

        // Verify column count and weights
        assertEquals(5, tableBlock.colWidthWeights.size)
    }

    @Test
    fun testRtfDocumentFallbackParsing() {
        val rtfContent = "{\\rtf1\\ansi\\deff0 {\\fonttbl {\\f0 Arial;}}\n\\f0\\fs24 This is a Rich Text Format assignment.\\par\nWith multiple paragraphs and symbols: \\'26 \\'3c \\'3e.\\par\n}"
        val rtfFile = tempFolder.newFile("sample_assignment.rtf")
        rtfFile.writeText(rtfContent)

        val doc = DocxParser.parse(rtfFile)
        assertNotNull(doc)
        assertTrue(doc.bodyElements.isNotEmpty())

        val allText = doc.bodyElements.joinToString("\n") { (it as DocxBlock.Paragraph).fullText }
        assertTrue(allText.contains("This is a Rich Text Format assignment"))
        assertFalse(allText.contains("\\fonttbl"))
        assertFalse(allText.contains("\\rtf1"))
    }
}
