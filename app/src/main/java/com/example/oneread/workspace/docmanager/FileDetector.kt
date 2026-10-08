package com.example.oneread.workspace.docmanager

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.oneread.workspace.model.DocumentFormat
import java.io.File
import java.io.FileInputStream

/**
 * Modular file type detection service for the document workspace.
 * Determines document format using:
 * 1. File extension
 * 2. MIME type (with strict OpenXML / Excel mappings)
 * 3. Content signature/magic bytes
 * 4. XML structure inspection (SpreadsheetML vs generic XML)
 */
object FileDetector {

    fun detectFormat(fileName: String, mimeType: String? = null, file: File? = null): DocumentFormat {
        val ext = fileName.substringAfterLast('.', "").lowercase().trim()

        // 1. Check if XML: inspect if it's a SpreadsheetML XML or generic XML
        if (ext == "xml" || mimeType == "application/xml" || mimeType == "text/xml") {
            if (file != null && file.exists()) {
                val isSpreadsheet = isSpreadsheetXml(file)
                if (isSpreadsheet) return DocumentFormat.EXCEL
            }
            return DocumentFormat.TEXT // Generic XML opens in text/code viewer
        }

        // 2. Extension check
        val formatFromExt = DocumentFormat.fromExtension(ext)
        if (formatFromExt != DocumentFormat.UNKNOWN) {
            return formatFromExt
        }

        // 3. MIME type check
        if (!mimeType.isNullOrBlank()) {
            val lowerMime = mimeType.lowercase().trim()
            val formatFromMime = when {
                lowerMime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                lowerMime == "application/vnd.ms-excel" ||
                lowerMime == "application/vnd.ms-excel.sheet.macroenabled.12" ||
                lowerMime == "application/vnd.openxmlformats-officedocument.spreadsheetml.template" ||
                lowerMime == "application/vnd.ms-excel.template.macroenabled.12" ||
                lowerMime.contains("spreadsheet") || lowerMime.contains("excel") -> DocumentFormat.EXCEL

                lowerMime == "text/csv" || lowerMime.contains("csv") -> DocumentFormat.CSV
                lowerMime.contains("pdf") -> DocumentFormat.PDF
                lowerMime.contains("word") || lowerMime.contains("officedocument.wordprocessing") -> DocumentFormat.WORD
                lowerMime.contains("presentation") || lowerMime.contains("powerpoint") -> DocumentFormat.PPT
                lowerMime.contains("markdown") -> DocumentFormat.MARKDOWN
                lowerMime.contains("image") -> DocumentFormat.IMAGE
                lowerMime.contains("text") -> DocumentFormat.TEXT
                else -> DocumentFormat.UNKNOWN
            }
            if (formatFromMime != DocumentFormat.UNKNOWN) {
                return formatFromMime
            }
        }

        // 4. Magic bytes content signature
        if (file != null && file.exists() && file.length() >= 4) {
            val sigFormat = detectByMagicBytes(file)
            if (sigFormat != DocumentFormat.UNKNOWN) {
                return sigFormat
            }
        }

        return DocumentFormat.UNKNOWN
    }

    fun detectFormat(file: File): DocumentFormat {
        return detectFormat(file.name, null, file)
    }

    private fun detectByMagicBytes(file: File): DocumentFormat {
        return runCatching {
            val ext = file.name.substringAfterLast('.', "").lowercase()
            FileInputStream(file).use { input ->
                val header = ByteArray(8)
                val bytesRead = input.read(header)
                if (bytesRead >= 4) {
                    // PDF signature: %PDF (0x25, 0x50, 0x44, 0x46)
                    if (header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte()) {
                        return DocumentFormat.PDF
                    }

                    // ZIP signature: PK\x03\x04 (0x50, 0x4B, 0x03, 0x04) -> OpenXML (.xlsx, .docx, .pptx)
                    if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
                        // Inspect zip entries
                        try {
                            val zip = java.util.zip.ZipFile(file)
                            val isXlsx = zip.getEntry("xl/workbook.xml") != null || zip.getEntry("xl/worksheets/sheet1.xml") != null
                            val isDocx = zip.getEntry("word/document.xml") != null
                            val isPptx = zip.getEntry("ppt/presentation.xml") != null || ext in setOf("pptx", "pptm", "potx", "potm", "ppsx", "ppsm")
                            val isOdp = ext == "odp"
                            zip.close()
                            if (isXlsx) return DocumentFormat.EXCEL
                            if (isDocx) return DocumentFormat.WORD
                            if (isPptx || isOdp) return DocumentFormat.PPT
                        } catch (_: Exception) { }
                    }

                    // OLE2 Compound Document: 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1 (.xls, .doc, .ppt)
                    if (header[0] == 0xD0.toByte() && header[1] == 0xCF.toByte() && header[2] == 0x11.toByte() && header[3] == 0xE0.toByte()) {
                        val ext = file.name.substringAfterLast('.', "").lowercase()
                        if (ext == "xls") return DocumentFormat.EXCEL
                        if (ext == "doc") return DocumentFormat.WORD
                        if (ext in setOf("ppt", "pps", "pot")) return DocumentFormat.PPT
                        return DocumentFormat.EXCEL
                    }
                }
                DocumentFormat.UNKNOWN
            }
        }.getOrDefault(DocumentFormat.UNKNOWN)
    }

    private fun isSpreadsheetXml(file: File): Boolean {
        return runCatching {
            file.bufferedReader().use { reader ->
                val sample = reader.readText().take(4000)
                sample.contains("urn:schemas-microsoft-com:office:spreadsheet") ||
                        sample.contains("<Workbook") ||
                        sample.contains("<ss:Workbook") ||
                        sample.contains("<ss:Worksheet")
            }
        }.getOrDefault(false)
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) { }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1 && result != null) {
                result = result?.substring(cut + 1)
            }
        }

        var fileName = result ?: "document"
        // Ensure extension is preserved with the dot if missing
        if (!fileName.contains(".")) {
            val mime = context.contentResolver.getType(uri)
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            if (!ext.isNullOrBlank()) {
                fileName = "$fileName.$ext"
            }
        }
        return fileName
    }
}
