package com.example.oneread.excel.formula

import com.example.oneread.excel.model.CellRange
import com.example.oneread.excel.model.ExcelCellModel
import com.example.oneread.excel.model.ExcelSheetModel
import com.example.oneread.excel.model.ExcelWorkbookModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

/**
 * Robust Formula Evaluation Engine for Excel spreadsheets.
 * Evaluates standard spreadsheet functions and mathematical expressions
 * while preserving the original formula string intact.
 */
object ExcelFormulaEvaluator {

    fun evaluate(formula: String, currentSheet: ExcelSheetModel, workbook: ExcelWorkbookModel? = null): String {
        val clean = formula.trim().removePrefix("=").trim()
        if (clean.isBlank()) return ""

        return runCatching {
            // 1. Function Call evaluation (e.g. SUM(A1:A5))
            val funcRegex = Regex("^([A-Za-z0-9_]+)\\((.*)\\)$", RegexOption.DOT_MATCHES_ALL)
            val match = funcRegex.find(clean)
            if (match != null) {
                val funcName = match.groupValues[1].uppercase()
                val argsStr = match.groupValues[2]
                return evaluateFunction(funcName, argsStr, currentSheet, workbook)
            }

            // 2. Simple arithmetic expression or cell reference (e.g. A1 + B1 or A1)
            val singleCell = ExcelCellModel.parseCellRef(clean)
            if (singleCell != null) {
                return currentSheet.getCellValue(singleCell.first, singleCell.second)
            }

            // 3. String concatenation with '&'
            if (clean.contains("&")) {
                val parts = splitTopLevel(clean, '&')
                return parts.joinToString("") { part ->
                    val trimmed = part.trim().removeSurrounding("\"")
                    val cell = ExcelCellModel.parseCellRef(trimmed)
                    if (cell != null) currentSheet.getCellValue(cell.first, cell.second) else trimmed
                }
            }

            // 4. Arithmetic operators: + - * /
            if (clean.contains("+") || clean.contains("-") || clean.contains("*") || clean.contains("/")) {
                return evaluateArithmetic(clean, currentSheet)
            }

            // Return clean raw value if cannot evaluate
            clean
        }.getOrElse { "#ERROR!" }
    }

    private fun evaluateFunction(
        func: String,
        argsStr: String,
        currentSheet: ExcelSheetModel,
        workbook: ExcelWorkbookModel?
    ): String {
        val args = splitArgs(argsStr)

        when (func) {
            "SUM" -> {
                val values = resolveNumberList(args, currentSheet, workbook)
                val sum = values.sum()
                return if (sum % 1.0 == 0.0) sum.toLong().toString() else "%.2f".format(Locale.US, sum)
            }
            "AVERAGE", "AVG" -> {
                val values = resolveNumberList(args, currentSheet, workbook)
                if (values.isEmpty()) return "#DIV/0!"
                val avg = values.average()
                return "%.2f".format(Locale.US, avg)
            }
            "COUNT" -> {
                val values = resolveNumberList(args, currentSheet, workbook)
                return values.size.toString()
            }
            "COUNTA" -> {
                val allValues = resolveStringList(args, currentSheet, workbook).filter { it.isNotBlank() }
                return allValues.size.toString()
            }
            "MIN" -> {
                val values = resolveNumberList(args, currentSheet, workbook)
                if (values.isEmpty()) return "0"
                val min = values.minOrNull() ?: 0.0
                return if (min % 1.0 == 0.0) min.toLong().toString() else "%.2f".format(Locale.US, min)
            }
            "MAX" -> {
                val values = resolveNumberList(args, currentSheet, workbook)
                if (values.isEmpty()) return "0"
                val max = values.maxOrNull() ?: 0.0
                return if (max % 1.0 == 0.0) max.toLong().toString() else "%.2f".format(Locale.US, max)
            }
            "ROUND" -> {
                val num = resolveValue(args.getOrNull(0), currentSheet).toDoubleOrNull() ?: 0.0
                val digits = resolveValue(args.getOrNull(1), currentSheet).toIntOrNull() ?: 0
                val factor = 10.0.pow(digits)
                val res = round(num * factor) / factor
                return if (digits <= 0) res.toLong().toString() else "%.${digits}f".format(Locale.US, res)
            }
            "ROUNDUP" -> {
                val num = resolveValue(args.getOrNull(0), currentSheet).toDoubleOrNull() ?: 0.0
                val digits = resolveValue(args.getOrNull(1), currentSheet).toIntOrNull() ?: 0
                val factor = 10.0.pow(digits)
                val res = ceil(num * factor) / factor
                return if (digits <= 0) res.toLong().toString() else "%.${digits}f".format(Locale.US, res)
            }
            "ROUNDDOWN" -> {
                val num = resolveValue(args.getOrNull(0), currentSheet).toDoubleOrNull() ?: 0.0
                val digits = resolveValue(args.getOrNull(1), currentSheet).toIntOrNull() ?: 0
                val factor = 10.0.pow(digits)
                val res = floor(num * factor) / factor
                return if (digits <= 0) res.toLong().toString() else "%.${digits}f".format(Locale.US, res)
            }
            "ABS" -> {
                val num = resolveValue(args.getOrNull(0), currentSheet).toDoubleOrNull() ?: 0.0
                val res = abs(num)
                return if (res % 1.0 == 0.0) res.toLong().toString() else "%.2f".format(Locale.US, res)
            }
            "IF" -> {
                val cond = evaluateCondition(args.getOrNull(0) ?: "", currentSheet)
                return if (cond) {
                    resolveValue(args.getOrNull(1), currentSheet)
                } else {
                    resolveValue(args.getOrNull(2) ?: "\"\"", currentSheet)
                }
            }
            "AND" -> {
                val results = args.map { evaluateCondition(it, currentSheet) }
                return (results.all { it }).toString().uppercase()
            }
            "OR" -> {
                val results = args.map { evaluateCondition(it, currentSheet) }
                return (results.any { it }).toString().uppercase()
            }
            "NOT" -> {
                val res = evaluateCondition(args.getOrNull(0) ?: "", currentSheet)
                return (!res).toString().uppercase()
            }
            "SUMIF" -> {
                val rangeStr = args.getOrNull(0) ?: return "0"
                val criteria = resolveValue(args.getOrNull(1), currentSheet).removeSurrounding("\"")
                val sumRangeStr = args.getOrNull(2) ?: rangeStr
                val range = CellRange.parse(rangeStr) ?: return "0"
                val sumRange = CellRange.parse(sumRangeStr) ?: range

                var total = 0.0
                for (r in range.startRow..range.endRow) {
                    val condVal = currentSheet.getCellValue(r, range.startCol)
                    if (matchesCriteria(condVal, criteria)) {
                        val sumVal = currentSheet.getCellValue(r, sumRange.startCol).replace(",", "").toDoubleOrNull() ?: 0.0
                        total += sumVal
                    }
                }
                return if (total % 1.0 == 0.0) total.toLong().toString() else "%.2f".format(Locale.US, total)
            }
            "COUNTIF" -> {
                val rangeStr = args.getOrNull(0) ?: return "0"
                val criteria = resolveValue(args.getOrNull(1), currentSheet).removeSurrounding("\"")
                val range = CellRange.parse(rangeStr) ?: return "0"
                var count = 0
                for (r in range.startRow..range.endRow) {
                    for (c in range.startCol..range.endCol) {
                        if (matchesCriteria(currentSheet.getCellValue(r, c), criteria)) {
                            count++
                        }
                    }
                }
                return count.toString()
            }
            "CONCAT", "CONCATENATE" -> {
                return args.joinToString("") { resolveValue(it, currentSheet).removeSurrounding("\"") }
            }
            "LEFT" -> {
                val str = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                val count = resolveValue(args.getOrNull(1) ?: "1", currentSheet).toIntOrNull() ?: 1
                return str.take(count)
            }
            "RIGHT" -> {
                val str = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                val count = resolveValue(args.getOrNull(1) ?: "1", currentSheet).toIntOrNull() ?: 1
                return str.takeLast(count)
            }
            "MID" -> {
                val str = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                val start = (resolveValue(args.getOrNull(1) ?: "1", currentSheet).toIntOrNull() ?: 1) - 1
                val length = resolveValue(args.getOrNull(2) ?: "1", currentSheet).toIntOrNull() ?: 1
                if (start in str.indices) {
                    return str.substring(start, minOf(start + length, str.length))
                }
                return ""
            }
            "LEN" -> {
                val str = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                return str.length.toString()
            }
            "TRIM" -> {
                val str = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                return str.trim()
            }
            "UPPER" -> {
                val str = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                return str.uppercase()
            }
            "LOWER" -> {
                val str = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                return str.lowercase()
            }
            "TODAY" -> {
                return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
            "NOW" -> {
                return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            }
            "IFS" -> {
                var idx = 0
                while (idx < args.size) {
                    val cond = evaluateCondition(args.getOrNull(idx) ?: "", currentSheet)
                    if (cond) {
                        return resolveValue(args.getOrNull(idx + 1), currentSheet)
                    }
                    idx += 2
                }
                return "#N/A"
            }
            "AVERAGEIF" -> {
                val rangeStr = args.getOrNull(0) ?: return "#DIV/0!"
                val criteria = resolveValue(args.getOrNull(1), currentSheet).removeSurrounding("\"")
                val avgRangeStr = args.getOrNull(2) ?: rangeStr
                val range = CellRange.parse(rangeStr) ?: return "#DIV/0!"
                val avgRange = CellRange.parse(avgRangeStr) ?: range

                var total = 0.0
                var count = 0
                for (r in range.startRow..range.endRow) {
                    val condVal = currentSheet.getCellValue(r, range.startCol)
                    if (matchesCriteria(condVal, criteria)) {
                        val num = currentSheet.getCellValue(r, avgRange.startCol).replace(",", "").toDoubleOrNull()
                        if (num != null) {
                            total += num
                            count++
                        }
                    }
                }
                return if (count > 0) "%.2f".format(Locale.US, total / count) else "#DIV/0!"
            }
            "SUMIFS" -> {
                val sumRangeStr = args.getOrNull(0) ?: return "0"
                val sumRange = CellRange.parse(sumRangeStr) ?: return "0"
                var total = 0.0
                for (r in sumRange.startRow..sumRange.endRow) {
                    var matchesAll = true
                    var pairIdx = 1
                    while (pairIdx < args.size) {
                        val cRange = CellRange.parse(args.getOrNull(pairIdx) ?: "")
                        val crit = resolveValue(args.getOrNull(pairIdx + 1), currentSheet).removeSurrounding("\"")
                        if (cRange != null) {
                            val cVal = currentSheet.getCellValue(r, cRange.startCol)
                            if (!matchesCriteria(cVal, crit)) {
                                matchesAll = false
                                break
                            }
                        }
                        pairIdx += 2
                    }
                    if (matchesAll) {
                        val v = currentSheet.getCellValue(r, sumRange.startCol).replace(",", "").toDoubleOrNull() ?: 0.0
                        total += v
                    }
                }
                return if (total % 1.0 == 0.0) total.toLong().toString() else "%.2f".format(Locale.US, total)
            }
            "COUNTIFS" -> {
                val range1 = CellRange.parse(args.getOrNull(0) ?: "") ?: return "0"
                var count = 0
                for (r in range1.startRow..range1.endRow) {
                    var matchesAll = true
                    var pairIdx = 0
                    while (pairIdx < args.size) {
                        val cRange = CellRange.parse(args.getOrNull(pairIdx) ?: "")
                        val crit = resolveValue(args.getOrNull(pairIdx + 1), currentSheet).removeSurrounding("\"")
                        if (cRange != null) {
                            val cVal = currentSheet.getCellValue(r, cRange.startCol)
                            if (!matchesCriteria(cVal, crit)) {
                                matchesAll = false
                                break
                            }
                        }
                        pairIdx += 2
                    }
                    if (matchesAll) count++
                }
                return count.toString()
            }
            "VLOOKUP" -> {
                val lookupVal = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                val tableRangeStr = args.getOrNull(1) ?: return "#N/A"
                val range = CellRange.parse(tableRangeStr) ?: return "#N/A"
                val colOffset = (resolveValue(args.getOrNull(2), currentSheet).toIntOrNull() ?: 1) - 1
                val targetCol = range.startCol + colOffset

                for (r in range.startRow..range.endRow) {
                    val firstColVal = currentSheet.getCellValue(r, range.startCol)
                    if (firstColVal.equals(lookupVal, ignoreCase = true)) {
                        return currentSheet.getCellValue(r, targetCol)
                    }
                }
                return "#N/A"
            }
            "HLOOKUP" -> {
                val lookupVal = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                val tableRangeStr = args.getOrNull(1) ?: return "#N/A"
                val range = CellRange.parse(tableRangeStr) ?: return "#N/A"
                val rowOffset = (resolveValue(args.getOrNull(2), currentSheet).toIntOrNull() ?: 1) - 1
                val targetRow = range.startRow + rowOffset

                for (c in range.startCol..range.endCol) {
                    val firstRowVal = currentSheet.getCellValue(range.startRow, c)
                    if (firstRowVal.equals(lookupVal, ignoreCase = true)) {
                        return currentSheet.getCellValue(targetRow, c)
                    }
                }
                return "#N/A"
            }
            "XLOOKUP" -> {
                val lookupVal = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                val lookupRange = CellRange.parse(args.getOrNull(1) ?: "") ?: return "#N/A"
                val returnRange = CellRange.parse(args.getOrNull(2) ?: "") ?: return "#N/A"
                val notFound = args.getOrNull(3)?.let { resolveValue(it, currentSheet) } ?: "#N/A"

                var offset = 0
                for (r in lookupRange.startRow..lookupRange.endRow) {
                    val valInLookup = currentSheet.getCellValue(r, lookupRange.startCol)
                    if (valInLookup.equals(lookupVal, ignoreCase = true)) {
                        val retRow = returnRange.startRow + offset
                        return currentSheet.getCellValue(retRow, returnRange.startCol)
                    }
                    offset++
                }
                return notFound
            }
            "INDEX" -> {
                val rangeStr = args.getOrNull(0) ?: return "#VALUE!"
                val range = CellRange.parse(rangeStr) ?: return "#VALUE!"
                val rowNum = (resolveValue(args.getOrNull(1), currentSheet).toIntOrNull() ?: 1) - 1
                val colNum = (resolveValue(args.getOrNull(2), currentSheet).toIntOrNull() ?: 1) - 1
                val targetRow = range.startRow + rowNum
                val targetCol = range.startCol + colNum
                return currentSheet.getCellValue(targetRow, targetCol)
            }
            "MATCH" -> {
                val lookupVal = resolveValue(args.getOrNull(0), currentSheet).removeSurrounding("\"")
                val range = CellRange.parse(args.getOrNull(1) ?: "") ?: return "#N/A"
                var pos = 1
                if (range.startRow == range.endRow) {
                    for (c in range.startCol..range.endCol) {
                        if (currentSheet.getCellValue(range.startRow, c).equals(lookupVal, ignoreCase = true)) return pos.toString()
                        pos++
                    }
                } else {
                    for (r in range.startRow..range.endRow) {
                        if (currentSheet.getCellValue(r, range.startCol).equals(lookupVal, ignoreCase = true)) return pos.toString()
                        pos++
                    }
                }
                return "#N/A"
            }
            else -> {
                return "#NAME?"
            }
        }
    }

    private fun resolveValue(arg: String?, sheet: ExcelSheetModel): String {
        if (arg == null) return ""
        val trimmed = arg.trim()
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.removeSurrounding("\"")
        }
        val cell = ExcelCellModel.parseCellRef(trimmed)
        if (cell != null) {
            return sheet.getCellValue(cell.first, cell.second)
        }
        return trimmed
    }

    private fun resolveNumberList(args: List<String>, sheet: ExcelSheetModel, workbook: ExcelWorkbookModel?): List<Double> {
        val list = mutableListOf<Double>()
        for (arg in args) {
            val trimmed = arg.trim()
            if (trimmed.contains(":")) {
                val range = CellRange.parse(trimmed)
                if (range != null) {
                    for (r in range.startRow..range.endRow) {
                        for (c in range.startCol..range.endCol) {
                            val v = sheet.getCellValue(r, c).replace(",", "").replace("$", "").replace("€", "").trim()
                            v.toDoubleOrNull()?.let { list.add(it) }
                        }
                    }
                }
            } else {
                val cell = ExcelCellModel.parseCellRef(trimmed)
                if (cell != null) {
                    val v = sheet.getCellValue(cell.first, cell.second).replace(",", "").replace("$", "").replace("€", "").trim()
                    v.toDoubleOrNull()?.let { list.add(it) }
                } else {
                    trimmed.toDoubleOrNull()?.let { list.add(it) }
                }
            }
        }
        return list
    }

    private fun resolveStringList(args: List<String>, sheet: ExcelSheetModel, workbook: ExcelWorkbookModel?): List<String> {
        val list = mutableListOf<String>()
        for (arg in args) {
            val trimmed = arg.trim()
            if (trimmed.contains(":")) {
                val range = CellRange.parse(trimmed)
                if (range != null) {
                    for (r in range.startRow..range.endRow) {
                        for (c in range.startCol..range.endCol) {
                            list.add(sheet.getCellValue(r, c))
                        }
                    }
                }
            } else {
                list.add(resolveValue(trimmed, sheet))
            }
        }
        return list
    }

    private fun matchesCriteria(cellVal: String, criteria: String): Boolean {
        val cleanCell = cellVal.trim()
        val cleanCrit = criteria.trim()
        if (cleanCrit.startsWith(">=")) {
            val n1 = cleanCell.toDoubleOrNull() ?: return false
            val n2 = cleanCrit.removePrefix(">=").trim().toDoubleOrNull() ?: return false
            return n1 >= n2
        } else if (cleanCrit.startsWith("<=")) {
            val n1 = cleanCell.toDoubleOrNull() ?: return false
            val n2 = cleanCrit.removePrefix("<=").trim().toDoubleOrNull() ?: return false
            return n1 <= n2
        } else if (cleanCrit.startsWith(">")) {
            val n1 = cleanCell.toDoubleOrNull() ?: return false
            val n2 = cleanCrit.removePrefix(">").trim().toDoubleOrNull() ?: return false
            return n1 > n2
        } else if (cleanCrit.startsWith("<")) {
            val n1 = cleanCell.toDoubleOrNull() ?: return false
            val n2 = cleanCrit.removePrefix("<").trim().toDoubleOrNull() ?: return false
            return n1 < n2
        } else if (cleanCrit.startsWith("=")) {
            return cleanCell.equals(cleanCrit.removePrefix("=").trim(), ignoreCase = true)
        }
        return cleanCell.equals(cleanCrit, ignoreCase = true)
    }

    private fun evaluateCondition(expr: String, sheet: ExcelSheetModel): Boolean {
        val ops = listOf(">=", "<=", "!=", "=", ">", "<")
        for (op in ops) {
            if (expr.contains(op)) {
                val parts = expr.split(op, limit = 2)
                val left = resolveValue(parts[0], sheet).trim()
                val right = resolveValue(parts[1], sheet).trim()
                val lNum = left.toDoubleOrNull()
                val rNum = right.toDoubleOrNull()

                return when (op) {
                    ">=" -> if (lNum != null && rNum != null) lNum >= rNum else left >= right
                    "<=" -> if (lNum != null && rNum != null) lNum <= rNum else left <= right
                    "!=" -> if (lNum != null && rNum != null) lNum != rNum else left != right
                    "=" -> if (lNum != null && rNum != null) lNum == rNum else left.equals(right, ignoreCase = true)
                    ">" -> if (lNum != null && rNum != null) lNum > rNum else left > right
                    "<" -> if (lNum != null && rNum != null) lNum < rNum else left < right
                    else -> false
                }
            }
        }
        return expr.trim().equals("TRUE", ignoreCase = true) || (expr.trim().toDoubleOrNull() ?: 0.0) != 0.0
    }

    private fun evaluateArithmetic(expr: String, sheet: ExcelSheetModel): String {
        // Simple token evaluation for A1 + B1 or 5 * 10
        val tokens = expr.split(Regex("(?<=[-+*/])|(?=[-+*/])")).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return expr

        var currentVal = resolveNumericToken(tokens[0], sheet)
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val nextVal = resolveNumericToken(tokens.getOrNull(i + 1) ?: "0", sheet)
            currentVal = when (op) {
                "+" -> currentVal + nextVal
                "-" -> currentVal - nextVal
                "*" -> currentVal * nextVal
                "/" -> if (nextVal != 0.0) currentVal / nextVal else return "#DIV/0!"
                else -> currentVal
            }
            i += 2
        }
        return if (currentVal % 1.0 == 0.0) currentVal.toLong().toString() else "%.2f".format(Locale.US, currentVal)
    }

    private fun resolveNumericToken(token: String, sheet: ExcelSheetModel): Double {
        val cell = ExcelCellModel.parseCellRef(token)
        if (cell != null) {
            return sheet.getCellValue(cell.first, cell.second).replace(",", "").toDoubleOrNull() ?: 0.0
        }
        return token.replace(",", "").toDoubleOrNull() ?: 0.0
    }

    private fun splitArgs(argsStr: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var inQuotes = false
        val current = StringBuilder()

        for (ch in argsStr) {
            when (ch) {
                '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }
                '(' -> {
                    if (!inQuotes) depth++
                    current.append(ch)
                }
                ')' -> {
                    if (!inQuotes) depth--
                    current.append(ch)
                }
                ',' -> {
                    if (!inQuotes && depth == 0) {
                        result.add(current.toString().trim())
                        current.clear()
                    } else {
                        current.append(ch)
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        return result
    }

    private fun splitTopLevel(str: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var inQuotes = false
        val current = StringBuilder()

        for (ch in str) {
            when (ch) {
                '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }
                '(' -> {
                    if (!inQuotes) depth++
                    current.append(ch)
                }
                ')' -> {
                    if (!inQuotes) depth--
                    current.append(ch)
                }
                delimiter -> {
                    if (!inQuotes && depth == 0) {
                        result.add(current.toString())
                        current.clear()
                    } else {
                        current.append(ch)
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }
}
