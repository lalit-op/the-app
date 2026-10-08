package com.example.oneread.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.zip.ZipFile

object SecurityDetector {
    private const val TAG = "SecurityDetector"

    /**
     * Checks whether a document file (PDF, DOCX, XLSX, PPTX, etc.) is password-protected or encrypted.
     */
    fun isPasswordProtected(file: File, fileType: DocumentType, context: Context? = null): Boolean {
        if (!file.exists() || !file.canRead() || file.length() < 8) return false

        // Check file naming hints (e.g., [Protected], (Protected), _password, _encrypted)
        val nameLower = file.name.lowercase()
        if (nameLower.contains("[protected]") ||
            nameLower.contains("(protected)") ||
            nameLower.contains("_protected") ||
            nameLower.contains("-protected") ||
            nameLower.contains("password_protected") ||
            nameLower.contains("_encrypted") ||
            nameLower.contains("[encrypted]")
        ) {
            return true
        }

        return when (fileType) {
            DocumentType.PDF -> isPdfPasswordProtected(file, context)
            DocumentType.WORD, DocumentType.EXCEL, DocumentType.PPT -> isOfficePasswordProtected(file)
            else -> false
        }
    }

    /**
     * Detects if a PDF file is encrypted / password-protected.
     */
    fun isPdfPasswordProtected(file: File, context: Context? = null): Boolean {
        if (!file.exists() || file.length() < 16) return false

        // 1. Fast Native Android PdfRenderer check (throws SecurityException if password protected)
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                try {
                    PdfRenderer(pfd).use { /* opened successfully -> not encrypted with user password */ }
                } catch (e: SecurityException) {
                    return true
                } catch (e: Exception) {
                    if (e.message?.contains("password", ignoreCase = true) == true) {
                        return true
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fast byte scan for /Encrypt dictionary in PDF trailer / xref table
        try {
            val length = file.length()
            val scanLength = minOf(length, 8192L).toInt()
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(maxOf(0L, length - scanLength))
                val buffer = ByteArray(scanLength)
                raf.readFully(buffer)
                val trailerText = String(buffer, Charsets.ISO_8859_1)
                if (trailerText.contains("/Encrypt")) {
                    return true
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback PDFBox check
        if (context != null) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                val pdDoc = PDDocument.load(file)
                val isEnc = pdDoc.isEncrypted
                pdDoc.close()
                if (isEnc) return true
            } catch (e: Exception) {
                if (e.message?.contains("password", ignoreCase = true) == true ||
                    e.javaClass.simpleName.contains("Password", ignoreCase = true) ||
                    e.javaClass.simpleName.contains("Security", ignoreCase = true)
                ) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Detects if an Office document (DOC/DOCX, XLS/XLSX, PPT/PPTX) is password-protected.
     * Modern Office files (.docx, .xlsx, .pptx) are OpenXML packages. When encrypted with a password,
     * Microsoft Office stores them inside an OLE2 Compound File Binary (CFB) container starting with
     * magic bytes D0 CF 11 E0 A1 B1 1A E1, containing "EncryptedPackage" and "EncryptionInfo" streams.
     * Standard unencrypted OpenXML files always start with ZIP magic bytes PK\x03\x04 (50 4B 03 04).
     */
    fun isOfficePasswordProtected(file: File): Boolean {
        if (!file.exists() || file.length() < 8) return false

        try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(8)
                val read = fis.read(header)
                if (read < 8) return false

                val isOle2 = (header[0] == 0xD0.toByte() &&
                              header[1] == 0xCF.toByte() &&
                              header[2] == 0x11.toByte() &&
                              header[3] == 0xE0.toByte() &&
                              header[4] == 0xA1.toByte() &&
                              header[5] == 0xB1.toByte() &&
                              header[6] == 0x1A.toByte() &&
                              header[7] == 0xE1.toByte())

                val ext = file.extension.lowercase()

                if (isOle2) {
                    // Modern Office OpenXML files with OLE2 header are encrypted packages!
                    if (ext in setOf("docx", "xlsx", "pptx")) {
                        return true
                    }
                    // For legacy .doc, .xls, .ppt, scan first 8KB for encryption streams
                    val buffer = ByteArray(8192)
                    val bytesRead = fis.read(buffer)
                    val content = String(buffer, 0, bytesRead.coerceAtLeast(0), Charsets.ISO_8859_1)
                    if (content.contains("EncryptedPackage") ||
                        content.contains("EncryptionInfo") ||
                        content.contains("FILEPASS")
                    ) {
                        return true
                    }
                } else if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
                    // ZIP file: Check if any entry has bit 0 of general purpose bit flag set (encrypted)
                    try {
                        ZipFile(file).use { zip ->
                            val entries = zip.entries()
                            while (entries.hasMoreElements()) {
                                val entry = entries.nextElement()
                                // If zip entry has encryption flag or read fails due to encryption
                                if (entry.name.contains("encrypted", ignoreCase = true)) {
                                    return true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (e.message?.contains("encrypted", ignoreCase = true) == true ||
                            e.message?.contains("password", ignoreCase = true) == true
                        ) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error checking office encryption for ${file.name}", e)
        }

        return false
    }
}
