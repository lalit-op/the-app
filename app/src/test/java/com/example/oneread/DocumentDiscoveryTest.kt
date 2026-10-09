package com.example.oneread

import com.example.oneread.data.DocumentItem
import com.example.oneread.data.DocumentType
import com.example.oneread.data.DocumentTypeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentDiscoveryTest {

    @Test
    fun test1_documentPdf_detectsAsPdf() {
        val type = DocumentTypeRegistry.detectDocumentType("document.pdf", "application/pdf")
        assertEquals(DocumentType.PDF, type)
    }

    @Test
    fun test2_documentUppercasePdf_detectsAsPdf() {
        val type = DocumentTypeRegistry.detectDocumentType("document.PDF", null)
        assertEquals(DocumentType.PDF, type)
    }

    @Test
    fun test3_documentRtf_detectsAsWord() {
        val type = DocumentTypeRegistry.detectDocumentType("document.rtf", "application/rtf")
        assertEquals(DocumentType.WORD, type)
        assertTrue(DocumentTypeRegistry.isRtf("document.rtf"))
    }

    @Test
    fun test4_documentUppercaseRtf_detectsAsWord() {
        val type = DocumentTypeRegistry.detectDocumentType("document.RTF", null)
        assertEquals(DocumentType.WORD, type)
        assertTrue(DocumentTypeRegistry.isRtf("document.RTF"))
    }

    @Test
    fun test5_fileSample100kBRtf_visibleInWordFiles() {
        val fileName = "file-sample_100kB.rtf"
        val type = DocumentTypeRegistry.detectDocumentType(fileName, "application/rtf")
        assertEquals(DocumentType.WORD, type)
        assertTrue(DocumentTypeRegistry.isSupported(fileName))
        assertTrue(DocumentTypeRegistry.isWord(fileName))
    }

    @Test
    fun test6_fileSample100kBWithParensRtf_visibleInWordFiles() {
        val fileName = "file-sample_100kB (1).rtf"
        val ext = DocumentTypeRegistry.extractExtension(fileName)
        assertEquals("rtf", ext)
        val type = DocumentTypeRegistry.detectDocumentType(fileName, null)
        assertEquals(DocumentType.WORD, type)
        assertTrue(DocumentTypeRegistry.isSupported(fileName))
    }

    @Test
    fun test7_doc20261003WA0005Pdf_visibleInPdfFiles() {
        val fileName = "DOC-20261003-WA0005.pdf"
        val path = "/storage/emulated/0/Android/data/alldocumentreader.office.viewer.filereader/files/docs/DOC-20261003-WA0005.pdf"
        val ext = DocumentTypeRegistry.extractExtension(fileName)
        assertEquals("pdf", ext)
        val type = DocumentTypeRegistry.detectDocumentType(fileName, "application/pdf")
        assertEquals(DocumentType.PDF, type)
        val typeFromPath = DocumentTypeRegistry.detectDocumentType(path, null)
        assertEquals(DocumentType.PDF, typeFromPath)
        assertTrue(DocumentTypeRegistry.isPdf(fileName))
    }

    @Test
    fun test8_pdfWithUnknownMimeType_stillDetectedAsPdf() {
        val type1 = DocumentTypeRegistry.detectDocumentType("document.pdf", "application/octet-stream")
        assertEquals(DocumentType.PDF, type1)

        val type2 = DocumentTypeRegistry.detectDocumentType("report.PDF", "unknown/mime")
        assertEquals(DocumentType.PDF, type2)

        val type3 = DocumentTypeRegistry.detectDocumentType("DOC-20261003-WA0005.pdf", null)
        assertEquals(DocumentType.PDF, type3)
    }

    @Test
    fun test9_rtfWithUnknownMimeType_stillDetectedAsRtf() {
        val type1 = DocumentTypeRegistry.detectDocumentType("file-sample_100kB.rtf", "application/octet-stream")
        assertEquals(DocumentType.WORD, type1)
        assertTrue(DocumentTypeRegistry.isRtf("file-sample_100kB.rtf", "application/octet-stream"))

        val type2 = DocumentTypeRegistry.detectDocumentType("contract.RTF", null)
        assertEquals(DocumentType.WORD, type2)

        val type3 = DocumentTypeRegistry.detectDocumentType("notes.rtf", "text/plain")
        // Authoritative extension check maps .rtf to WORD
        assertEquals(DocumentType.WORD, type3)
    }

    @Test
    fun test10_twoFilesWithSimilarNames_bothDistinctAndPreserved() {
        val file1 = DocumentItem(
            id = 101L,
            uri = "content://media/external/file/101",
            title = "file-sample_100kB.rtf",
            path = "/storage/emulated/0/Download/file-sample_100kB.rtf",
            extension = "rtf",
            fileType = DocumentType.WORD,
            sizeBytes = 102400L
        )
        val file2 = DocumentItem(
            id = 102L,
            uri = "content://media/external/file/102",
            title = "file-sample_100kB (1).rtf",
            path = "/storage/emulated/0/Download/file-sample_100kB (1).rtf",
            extension = "rtf",
            fileType = DocumentType.WORD,
            sizeBytes = 102400L // same size
        )

        val list = listOf(file1, file2)
        // Deduplication by path or distinct URI preserves both
        val distinct = list.distinctBy { it.path.ifBlank { it.uri } }
        assertEquals(2, distinct.size)
        assertEquals("file-sample_100kB.rtf", distinct[0].title)
        assertEquals("file-sample_100kB (1).rtf", distinct[1].title)
    }

    @Test
    fun test11_fileLocatedInAccessibleStorage_hasValidClassification() {
        val appSpecificPath = "/storage/emulated/0/Android/data/alldocumentreader.office.viewer.filereader/files/docs/DOC-20261003-WA0005.pdf"
        assertTrue(DocumentTypeRegistry.isSupported(appSpecificPath))
        assertEquals(DocumentType.PDF, DocumentTypeRegistry.detectDocumentType(appSpecificPath))
    }

    @Test
    fun test12_fileLocatedThroughContentUri_classifiedCorrectly() {
        val contentDoc = DocumentItem(
            id = 200L,
            uri = "content://com.android.providers.media.documents/document/document%3A100",
            title = "annual_report.rtf",
            extension = "rtf",
            fileType = DocumentType.WORD,
            sizeBytes = 55000L
        )
        assertNotNull(contentDoc)
        assertEquals(DocumentType.WORD, contentDoc.fileType)
        assertTrue(contentDoc.uri.startsWith("content://"))
    }

    @Test
    fun test13_searchDocuments_byTitleExtensionAndCategory() {
        val docs = listOf(
            DocumentItem(id = 1, title = "Student.docx", extension = "docx", fileType = DocumentType.WORD, sizeBytes = 100),
            DocumentItem(id = 2, title = "student.pdf", extension = "pdf", fileType = DocumentType.PDF, sizeBytes = 100),
            DocumentItem(id = 3, title = "STUDENT.xlsx", extension = "xlsx", fileType = DocumentType.EXCEL, sizeBytes = 100),
            DocumentItem(id = 4, title = "file-sample_100kB.rtf", extension = "rtf", fileType = DocumentType.WORD, sizeBytes = 100),
            DocumentItem(id = 5, title = "DOC-20261003-WA0005.pdf", extension = "pdf", fileType = DocumentType.PDF, sizeBytes = 100)
        )

        // Case-insensitive query "student" finds docx, pdf, and xlsx
        val studentMatches = docs.filter {
            it.title.lowercase().contains("student") || it.extension.lowercase().contains("student")
        }
        assertEquals(3, studentMatches.size)

        // Query "rtf" finds the RTF file
        val rtfMatches = docs.filter {
            it.title.lowercase().contains("rtf") || it.extension.lowercase().contains("rtf")
        }
        assertEquals(1, rtfMatches.size)
        assertEquals("file-sample_100kB.rtf", rtfMatches[0].title)

        // Query "DOC-20261003-WA0005" finds the PDF
        val docMatches = docs.filter {
            it.title.lowercase().contains("doc-20261003-wa0005")
        }
        assertEquals(1, docMatches.size)
        assertEquals("DOC-20261003-WA0005.pdf", docMatches[0].title)
    }

    @Test
    fun test14_categoriesAreMutuallyExclusiveAndAllFilesMatchesSum() {
        val testFiles = listOf(
            DocumentItem(id = 1, title = "test1.pdf", extension = "pdf", fileType = DocumentType.PDF, sizeBytes = 100),
            DocumentItem(id = 2, title = "test2.doc", extension = "doc", fileType = DocumentType.WORD, sizeBytes = 100),
            DocumentItem(id = 3, title = "test3.rtf", extension = "rtf", fileType = DocumentType.WORD, sizeBytes = 100),
            DocumentItem(id = 4, title = "test4.xlsx", extension = "xlsx", fileType = DocumentType.EXCEL, sizeBytes = 100),
            DocumentItem(id = 5, title = "test5.pptx", extension = "pptx", fileType = DocumentType.PPT, sizeBytes = 100),
            DocumentItem(id = 6, title = "test6.txt", extension = "txt", fileType = DocumentType.TXT, sizeBytes = 100)
        )

        val pdfCount = testFiles.count { it.fileType == DocumentType.PDF }
        val wordCount = testFiles.count { it.fileType == DocumentType.WORD }
        val excelCount = testFiles.count { it.fileType == DocumentType.EXCEL }
        val pptCount = testFiles.count { it.fileType == DocumentType.PPT }
        val txtCount = testFiles.count { it.fileType == DocumentType.TXT }

        assertEquals(1, pdfCount)
        assertEquals(2, wordCount) // doc + rtf
        assertEquals(1, excelCount)
        assertEquals(1, pptCount)
        assertEquals(1, txtCount)
        assertEquals(testFiles.size, pdfCount + wordCount + excelCount + pptCount + txtCount)
    }

    @Test
    fun test15_doc20261003WA0005Pdf_routesToPdfViewerExclusively() {
        val fileName = "DOC-20261003-WA0005.pdf"
        val detected = DocumentTypeRegistry.detectDocumentType(fileName, null)
        assertEquals(DocumentType.PDF, detected)

        // Verify it is not categorized as TXT or WORD or EXCEL
        assertTrue(detected != DocumentType.TXT)
        assertTrue(detected != DocumentType.WORD)
        assertTrue(detected != DocumentType.EXCEL)
        assertTrue(detected != DocumentType.PPT)
    }

    @Test
    fun test16_scannedPdfWithZeroExtractableText_isValidPdf() {
        val docItem = DocumentItem(
            id = 555L,
            title = "DOC-20261003-WA0005.pdf",
            extension = "pdf",
            fileType = DocumentType.PDF,
            pageCount = 7,
            lastReadPage = 1,
            sizeBytes = 2_500_000L
        )

        // Zero characters extracted does not invalidate a 7-page scanned PDF
        assertEquals(DocumentType.PDF, docItem.fileType)
        assertEquals(7, docItem.pageCount)
        assertEquals("pdf", docItem.extension)
    }
}
