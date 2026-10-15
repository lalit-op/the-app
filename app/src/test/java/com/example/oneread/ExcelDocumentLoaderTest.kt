package com.example.oneread

import com.example.oneread.excel.loader.ExcelDocumentLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExcelDocumentLoaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun putEntry(zip: ZipOutputStream, name: String, xml: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(xml.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun createSparseXlsx(): File {
        val file = tempFolder.newFile("sparse.xlsx")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            putEntry(
                zip,
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="Grades" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>"""
            )
            putEntry(
                zip,
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                        Target="worksheets/sheet1.xml"/>
                    </Relationships>"""
            )
            putEntry(
                zip,
                "xl/worksheets/sheet1.xml",
                """<?xml version="1.0" encoding="UTF-8"?>
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <sheetData>
                        <row r="1">
                          <c r="A1" t="inlineStr"><is><t>Name</t></is></c>
                          <c r="C1" t="inlineStr"><is><t>Score</t></is></c>
                        </row>
                        <row r="3"><c r="C3"><v>95</v></c></row>
                      </sheetData>
                    </worksheet>"""
            )
        }
        return file
    }

    @Test
    fun sparseXlsxPreservesRealCellCoordinatesAndWorksheetName() = runBlocking {
        val workbook = ExcelDocumentLoader.loadWorkbook(createSparseXlsx())

        assertEquals(1, workbook.sheets.size)
        val sheet = workbook.sheets.single()
        assertEquals("Grades", sheet.name)
        assertEquals("Name", sheet.getCellValue(1, 1))
        assertEquals("", sheet.getCellValue(1, 2))
        assertEquals("Score", sheet.getCellValue(1, 3))
        assertEquals("95", sheet.getCellValue(3, 3))
    }

    @Test
    fun binaryLegacyXlsReportsUnsupportedFormatInsteadOfParsingBytesAsCells() = runBlocking {
        val file = tempFolder.newFile("legacy.xls")
        val ole2 = byteArrayOf(
            0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
            0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()
        )
        file.writeBytes(ole2 + ByteArray(64))

        val error = runCatching { ExcelDocumentLoader.loadWorkbook(file) }.exceptionOrNull()
        assertNotNull("Binary legacy XLS should be rejected with a clear error", error)
        assertTrue(error!!.message.orEmpty().contains("legacy binary .xls"))
    }
}
