package com.example.oneread.excel.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Format type of an Excel cell.
 */
enum class CellFormatType {
    GENERAL,
    TEXT,
    NUMBER,
    CURRENCY,
    PERCENTAGE,
    DATE,
    TIME,
    BOOLEAN,
    ERROR
}

/**
 * Visual styling attributes for an Excel cell.
 */
data class CellStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val textColor: Color? = null,
    val backgroundColor: Color? = null,
    val textAlign: TextAlign = TextAlign.Start,
    val fontSizeSp: Float = 13f,
    val numberFormatPattern: String? = null,
    val hasBorder: Boolean = true
)

/**
 * Represents a single cell in an Excel sheet.
 * Preserves raw value, formula, and formatted display output.
 */
data class ExcelCellModel(
    val row: Int, // 1-based index (1, 2, 3...)
    val col: Int, // 1-based index (1 = A, 2 = B, 3 = C...)
    val rawValue: String = "",
    val formula: String? = null, // e.g. "=SUM(B2:B11)"
    val cachedResult: String? = null,
    val formatType: CellFormatType = CellFormatType.GENERAL,
    val style: CellStyle = CellStyle(),
    val hyperlink: String? = null,
    val comment: String? = null
) {
    val cellRef: String
        get() = "${colIndexToLetters(col)}$row"

    /**
     * Display value evaluated or formatted according to format type.
     */
    val displayValue: String
        get() {
            if (!cachedResult.isNullOrEmpty()) {
                return formatDisplay(cachedResult, formatType, style.numberFormatPattern)
            }
            if (rawValue.isNotEmpty()) {
                return formatDisplay(rawValue, formatType, style.numberFormatPattern)
            }
            if (!formula.isNullOrEmpty()) {
                return formula
            }
            return ""
        }

    val isFormula: Boolean
        get() = !formula.isNullOrBlank() && formula.startsWith("=")

    companion object {
        fun colIndexToLetters(colIndex: Int): String {
            var col = colIndex
            val sb = StringBuilder()
            while (col > 0) {
                val rem = (col - 1) % 26
                sb.append(('A'.code + rem).toChar())
                col = (col - 1) / 26
            }
            return sb.reverse().toString()
        }

        fun lettersToColIndex(letters: String): Int {
            var result = 0
            for (ch in letters.uppercase()) {
                if (ch in 'A'..'Z') {
                    result = result * 26 + (ch - 'A' + 1)
                }
            }
            return result
        }

        fun parseCellRef(ref: String): Pair<Int, Int>? {
            val clean = ref.trim().replace("$", "").uppercase()
            val letters = clean.takeWhile { it.isLetter() }
            val numbers = clean.dropWhile { it.isLetter() }
            val col = lettersToColIndex(letters)
            val row = numbers.toIntOrNull() ?: return null
            if (col < 1 || row < 1) return null
            return Pair(row, col)
        }

        fun formatDisplay(raw: String, type: CellFormatType, pattern: String?): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""

            // Currency detection or explicit format
            if (type == CellFormatType.CURRENCY || trimmed.startsWith("$") || trimmed.startsWith("€") || trimmed.startsWith("£") || trimmed.startsWith("₹")) {
                val num = trimmed.replace(Regex("[^0-9.-]"), "").toDoubleOrNull()
                if (num != null) {
                    val symbol = when {
                        trimmed.contains("₹") -> "₹"
                        trimmed.contains("€") -> "€"
                        trimmed.contains("£") -> "£"
                        else -> "$"
                    }
                    val df = DecimalFormat("#,##0.00")
                    return "$symbol${df.format(num)}"
                }
            }

            // Percentage detection
            if (type == CellFormatType.PERCENTAGE || trimmed.endsWith("%")) {
                val num = trimmed.removeSuffix("%").trim().toDoubleOrNull()
                if (num != null) {
                    val actual = if (trimmed.endsWith("%")) num else num * 100.0
                    val df = DecimalFormat("0.0%")
                    return df.format(actual / 100.0)
                }
            }

            // Number formatting
            if (type == CellFormatType.NUMBER) {
                val num = trimmed.toDoubleOrNull()
                if (num != null) {
                    val df = if (num % 1.0 == 0.0) DecimalFormat("#,##0") else DecimalFormat("#,##0.00")
                    return df.format(num)
                }
            }

            // Date formatting
            if (type == CellFormatType.DATE) {
                // If serial date number from Excel (days since 1899-12-30)
                val serial = trimmed.toDoubleOrNull()
                if (serial != null && serial in 1000.0..100000.0) {
                    val millis = ((serial - 25569.0) * 86400.0 * 1000.0).toLong()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    return sdf.format(Date(millis))
                }
            }

            return raw
        }
    }
}
