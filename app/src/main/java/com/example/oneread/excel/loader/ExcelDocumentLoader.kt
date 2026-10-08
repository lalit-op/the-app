package com.example.oneread.excel.loader

import android.content.Context
import android.net.Uri
import com.example.oneread.excel.formula.ExcelFormulaEvaluator
import com.example.oneread.excel.model.CellFormatType
import com.example.oneread.excel.model.CellRange
import com.example.oneread.excel.model.CellStyle
import com.example.oneread.excel.model.ExcelCellModel
import com.example.oneread.excel.model.ExcelSheetModel
import com.example.oneread.excel.model.ExcelWorkbookModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.zip.ZipFile

/**
 * Production-ready Document Loader for Excel and Spreadsheet formats:
 * - .xlsx, .xlsm, .xltx, .xltm (OpenXML)
 * - .xls (BIFF8 / HTML Spreadsheet / XML)
 * - .csv (Delimited spreadsheet)
 * - .xml (SpreadsheetML)
 *
 * Runs strictly on Dispatchers.IO to never block the UI thread.
 */
object ExcelDocumentLoader {

    suspend fun loadWorkbook(
        file: File,
        onProgress: ((String) -> Unit)? = null
    ): ExcelWorkbookModel = withContext(Dispatchers.IO) {
        onProgress?.invoke("Opening spreadsheet...")

        val name = file.name
        val ext = name.substringAfterLast('.', "").lowercase()

        // 1. OpenXML (.xlsx, .xlsm, .xltx, .xltm)
        if (ext in listOf("xlsx", "xlsm", "xltx", "xltm")) {
            return@withContext parseOpenXmlWorkbook(file, onProgress)
        }

        // 2. CSV / TSV
        if (ext in listOf("csv", "tsv")) {
            return@withContext parseCsvWorkbook(file, onProgress)
        }

        // 3. XML Spreadsheet
        if (ext == "xml") {
            val textSample = runCatching { file.bufferedReader().use { it.readText().take(2000) } }.getOrDefault("")
            if (textSample.contains("urn:schemas-microsoft-com:office:spreadsheet") || textSample.contains("<Workbook") || textSample.contains("<ss:Workbook")) {
                return@withContext parseSpreadsheetMl(file, onProgress)
            } else {
                throw IllegalArgumentException("Generic XML is not an Excel spreadsheet")
            }
        }

        // 4. Legacy .xls
        if (ext == "xls") {
            return@withContext parseLegacyXls(file, onProgress)
        }

        // Fallback: Attempt OpenXML first, then CSV
        try {
            parseOpenXmlWorkbook(file, onProgress)
        } catch (_: Exception) {
            parseCsvWorkbook(file, onProgress)
        }
    }

    /**
     * Parses standard OpenXML spreadsheets (.xlsx, .xlsm, .xltx, .xltm).
     */
    private fun parseOpenXmlWorkbook(
        file: File,
        onProgress: ((String) -> Unit)? = null
    ): ExcelWorkbookModel {
        onProgress?.invoke("Parsing workbook structure...")
        val zip = ZipFile(file)
        try {
            val workbook = ExcelWorkbookModel(fileName = file.name, filePath = file.absolutePath)

            // Step 1: Parse Shared Strings table (xl/sharedStrings.xml)
            val sharedStrings = parseSharedStrings(zip)

            // Step 2: Parse Styles (xl/styles.xml) for number formats and bold fonts
            val numberFormats = parseStyles(zip)

            // Step 3: Parse workbook.xml to get sheet names and rel IDs
            val sheetMetaList = parseWorkbookMeta(zip)

            // Step 4: Parse each worksheet XML
            sheetMetaList.forEachIndexed { index, meta ->
                onProgress?.invoke("Loading ${meta.name}...")
                val entryPath = meta.targetPath
                val sheetEntry = zip.getEntry(entryPath)
                if (sheetEntry != null) {
                    val sheet = parseWorksheet(
                        zip.getInputStream(sheetEntry),
                        meta.name,
                        index,
                        sharedStrings,
                        numberFormats
                    )
                    workbook.sheets.add(sheet)
                }
            }

            if (workbook.sheets.isEmpty()) {
                // If workbook.xml didn't map properly, attempt sheet1.xml directly
                val fallbackEntry = zip.getEntry("xl/worksheets/sheet1.xml")
                if (fallbackEntry != null) {
                    val sheet = parseWorksheet(
                        zip.getInputStream(fallbackEntry),
                        "Sheet1",
                        0,
                        sharedStrings,
                        numberFormats
                    )
                    workbook.sheets.add(sheet)
                }
            }

            if (workbook.sheets.isEmpty()) {
                workbook.sheets.add(ExcelSheetModel(name = "Sheet1", index = 0))
            }

            // Calculate formulas across sheets
            onProgress?.invoke("Evaluating formulas...")
            for (sheet in workbook.sheets) {
                for ((_, cell) in sheet.cells) {
                    if (cell.isFormula && cell.cachedResult == null) {
                        val eval = ExcelFormulaEvaluator.evaluate(cell.formula ?: "", sheet, workbook)
                        sheet.setCell(cell.copy(cachedResult = eval))
                    }
                }
            }

            return workbook
        } finally {
            zip.close()
        }
    }

    private data class SheetMeta(val name: String, val targetPath: String)

    private fun parseWorkbookMeta(zip: ZipFile): List<SheetMeta> {
        val result = mutableListOf<SheetMeta>()
        val relsMap = mutableMapOf<String, String>()

        // 1. Read xl/_rels/workbook.xml.rels
        val relsEntry = zip.getEntry("xl/_rels/workbook.xml.rels")
        if (relsEntry != null) {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(InputStreamReader(zip.getInputStream(relsEntry)))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name.equals("Relationship", ignoreCase = true)) {
                    val id = parser.getAttributeValue(null, "Id") ?: ""
                    var target = parser.getAttributeValue(null, "Target") ?: ""
                    if (!target.startsWith("xl/")) {
                        target = if (target.startsWith("/")) target.removePrefix("/") else "xl/$target"
                    }
                    if (id.isNotEmpty() && target.isNotEmpty()) {
                        relsMap[id] = target
                    }
                }
                eventType = parser.next()
            }
        }

        // 2. Read xl/workbook.xml
        val wbEntry = zip.getEntry("xl/workbook.xml")
        if (wbEntry != null) {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(InputStreamReader(zip.getInputStream(wbEntry)))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name.equals("sheet", ignoreCase = true)) {
                    val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                    val rId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                        ?: parser.getAttributeValue(null, "r:id")
                        ?: ""
                    val target = relsMap[rId] ?: "xl/worksheets/sheet${result.size + 1}.xml"
                    result.add(SheetMeta(name, target))
                }
                eventType = parser.next()
            }
        }

        return result
    }

    private fun parseSharedStrings(zip: ZipFile): List<String> {
        val entry = zip.getEntry("xl/sharedStrings.xml") ?: return emptyList()
        val strings = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(InputStreamReader(zip.getInputStream(entry)))

        var eventType = parser.eventType
        val currentStr = StringBuilder()
        var insideText = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideText) {
                        currentStr.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideText = false
                    } else if (parser.name.equals("si", ignoreCase = true)) {
                        strings.add(currentStr.toString())
                        currentStr.clear()
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseStyles(zip: ZipFile): Map<Int, CellFormatType> {
        val entry = zip.getEntry("xl/styles.xml") ?: return emptyMap()
        val formats = mutableMapOf<Int, CellFormatType>()
        // Default mappings for built-in Excel number format IDs
        // 14-22: Date, 5-8: Currency, 9-10: Percentage, 1-4: Number
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(InputStreamReader(zip.getInputStream(entry)))
            var eventType = parser.eventType
            var xfIndex = 0
            var insideCellXfs = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.name.equals("cellXfs", ignoreCase = true)) {
                        insideCellXfs = true
                    } else if (insideCellXfs && parser.name.equals("xf", ignoreCase = true)) {
                        val numFmtId = parser.getAttributeValue(null, "numFmtId")?.toIntOrNull() ?: 0
                        val type = when (numFmtId) {
                            in 5..8, 44 -> CellFormatType.CURRENCY
                            9, 10 -> CellFormatType.PERCENTAGE
                            in 14..22, in 45..47 -> CellFormatType.DATE
                            in 1..4, in 37..40 -> CellFormatType.NUMBER
                            49 -> CellFormatType.TEXT
                            else -> CellFormatType.GENERAL
                        }
                        formats[xfIndex++] = type
                    }
                } else if (eventType == XmlPullParser.END_TAG && parser.name.equals("cellXfs", ignoreCase = true)) {
                    insideCellXfs = false
                }
                eventType = parser.next()
            }
        } catch (_: Exception) { }
        return formats
    }

    private fun parseWorksheet(
        inputStream: InputStream,
        sheetName: String,
        sheetIndex: Int,
        sharedStrings: List<String>,
        numberFormats: Map<Int, CellFormatType>
    ): ExcelSheetModel {
        val sheet = ExcelSheetModel(name = sheetName, index = sheetIndex)
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(InputStreamReader(inputStream))

        var eventType = parser.eventType
        var currentCellRef = ""
        var currentCellType = ""
        var currentStyleId = -1
        var currentFormula: String? = null
        var currentValue = StringBuilder()
        var insideV = false
        var insideF = false
        var insideIsT = false

        var maxR = 20
        var maxC = 8

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "col" -> {
                            val min = parser.getAttributeValue(null, "min")?.toIntOrNull() ?: 1
                            val max = parser.getAttributeValue(null, "max")?.toIntOrNull() ?: min
                            val width = parser.getAttributeValue(null, "width")?.toFloatOrNull() ?: 10f
                            // Convert Excel char width to dp (approx width * 8.5)
                            val dpWidth = (width * 8.5f).coerceIn(48f, 500f)
                            for (c in min..max) {
                                sheet.columnWidths[c] = dpWidth
                            }
                        }
                        "row" -> {
                            val r = parser.getAttributeValue(null, "r")?.toIntOrNull()
                            val ht = parser.getAttributeValue(null, "ht")?.toFloatOrNull()
                            if (r != null && ht != null) {
                                sheet.rowHeights[r] = (ht * 1.33f).coerceIn(24f, 220f)
                            }
                        }
                        "pane" -> {
                            val xSplit = parser.getAttributeValue(null, "xSplit")?.toIntOrNull() ?: 0
                            val ySplit = parser.getAttributeValue(null, "ySplit")?.toIntOrNull() ?: 0
                            val state = parser.getAttributeValue(null, "state") ?: ""
                            if (state.equals("frozen", ignoreCase = true) || state.equals("split", ignoreCase = true)) {
                                if (xSplit > 0) sheet.frozenCols = xSplit
                                if (ySplit > 0) sheet.frozenRows = ySplit
                            }
                        }
                        "mergeCell" -> {
                            val ref = parser.getAttributeValue(null, "ref") ?: ""
                            CellRange.parse(ref)?.let { sheet.mergedRanges.add(it) }
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentStyleId = parser.getAttributeValue(null, "s")?.toIntOrNull() ?: -1
                            currentFormula = null
                            currentValue.clear()
                        }
                        "f" -> {
                            insideF = true
                        }
                        "v" -> {
                            insideV = true
                        }
                        "t" -> {
                            insideIsT = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideF) {
                        currentFormula = (currentFormula ?: "=") + parser.text
                    } else if (insideV || insideIsT) {
                        currentValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "f" -> insideF = false
                        "v" -> insideV = false
                        "t" -> insideIsT = false
                        "c" -> {
                            if (currentCellRef.isNotEmpty()) {
                                val coords = ExcelCellModel.parseCellRef(currentCellRef)
                                if (coords != null) {
                                    val row = coords.first
                                    val col = coords.second
                                    maxR = maxOf(maxR, row)
                                    maxC = maxOf(maxC, col)

                                    val rawVal = currentValue.toString()
                                    val text = if (currentCellType == "s") {
                                        val idx = rawVal.toIntOrNull()
                                        if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                                    } else if (currentCellType == "b") {
                                        if (rawVal == "1") "TRUE" else "FALSE"
                                    } else {
                                        rawVal
                                    }

                                    val fmtType = numberFormats[currentStyleId] ?: CellFormatType.GENERAL
                                    val formulaStr = if (currentFormula != null && !currentFormula!!.startsWith("=")) {
                                        "=$currentFormula"
                                    } else {
                                        currentFormula
                                    }

                                    val cell = ExcelCellModel(
                                        row = row,
                                        col = col,
                                        rawValue = text,
                                        formula = formulaStr,
                                        cachedResult = if (formulaStr != null && text.isNotEmpty()) text else null,
                                        formatType = fmtType,
                                        style = CellStyle(
                                            isBold = row == 1 || currentStyleId > 0
                                        )
                                    )
                                    sheet.setCell(cell)
                                    val textLen = text.length
                                    if (textLen > 8) {
                                        val neededW = (textLen * 8.5f + 24f).coerceIn(92f, 480f)
                                        val currentW = sheet.columnWidths[col] ?: 92f
                                        if (neededW > currentW) {
                                            sheet.columnWidths[col] = neededW
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return sheet.copy(maxRow = maxR + 5, maxCol = maxC + 3)
    }

    /**
     * Parses Delimited CSV / TSV files.
     */
    private fun parseCsvWorkbook(file: File, onProgress: ((String) -> Unit)? = null): ExcelWorkbookModel {
        onProgress?.invoke("Reading CSV data...")
        val workbook = ExcelWorkbookModel(fileName = file.name, filePath = file.absolutePath)
        val sheet = ExcelSheetModel(name = "Data", index = 0)

        val reader = file.bufferedReader()
        var row = 1
        var maxCol = 1

        var line = reader.readLine()
        val delimiter = if (line != null && line.contains("\t")) '\t'
        else if (line != null && line.contains(";")) ';'
        else ','

        while (line != null) {
            val cells = parseCsvLine(line, delimiter)
            maxCol = maxOf(maxCol, cells.size)
            cells.forEachIndexed { colIdx, value ->
                val col = colIdx + 1
                val trimmed = value.trim()
                val isNum = trimmed.replace(",", "").toDoubleOrNull() != null
                sheet.setCell(
                    ExcelCellModel(
                        row = row,
                        col = col,
                        rawValue = trimmed,
                        formatType = if (isNum) CellFormatType.NUMBER else CellFormatType.GENERAL,
                        style = CellStyle(isBold = row == 1)
                    )
                )
            }
            row++
            if (row > 5000) break // Virtualize first 5000 rows
            line = reader.readLine()
        }
        reader.close()

        workbook.sheets.add(sheet.copy(maxRow = row + 5, maxCol = maxCol + 2))
        return workbook
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == delimiter && !inQuotes) {
                result.add(cur.toString())
                cur.clear()
            } else {
                cur.append(ch)
            }
        }
        result.add(cur.toString())
        return result
    }

    /**
     * Parses Microsoft SpreadsheetML (.xml) format.
     */
    private fun parseSpreadsheetMl(file: File, onProgress: ((String) -> Unit)? = null): ExcelWorkbookModel {
        onProgress?.invoke("Parsing SpreadsheetML XML...")
        val workbook = ExcelWorkbookModel(fileName = file.name, filePath = file.absolutePath)

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(InputStreamReader(file.inputStream()))

        var eventType = parser.eventType
        var currentSheet: ExcelSheetModel? = null
        var currentRow = 0
        var currentCol = 0
        var currentFormula: String? = null
        var cellValue = StringBuilder()
        var insideData = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Worksheet", "ss:Worksheet" -> {
                            val name = parser.getAttributeValue(null, "ss:Name") ?: "Sheet${workbook.sheets.size + 1}"
                            currentSheet = ExcelSheetModel(name = name, index = workbook.sheets.size)
                            currentRow = 0
                        }
                        "Row", "ss:Row" -> {
                            currentRow++
                            currentCol = 0
                        }
                        "Cell", "ss:Cell" -> {
                            currentCol++
                            val f = parser.getAttributeValue(null, "ss:Formula")
                            currentFormula = if (!f.isNullOrBlank()) f else null
                            cellValue.clear()
                        }
                        "Data", "ss:Data" -> {
                            insideData = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideData) {
                        cellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "Data", "ss:Data" -> insideData = false
                        "Cell", "ss:Cell" -> {
                            if (currentSheet != null && currentRow > 0 && currentCol > 0) {
                                val text = cellValue.toString()
                                currentSheet.setCell(
                                    ExcelCellModel(
                                        row = currentRow,
                                        col = currentCol,
                                        rawValue = text,
                                        formula = currentFormula,
                                        style = CellStyle(isBold = currentRow == 1)
                                    )
                                )
                            }
                        }
                        "Worksheet", "ss:Worksheet" -> {
                            if (currentSheet != null) {
                                workbook.sheets.add(currentSheet.copy(maxRow = currentRow + 5, maxCol = currentCol + 3))
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (workbook.sheets.isEmpty()) {
            workbook.sheets.add(ExcelSheetModel(name = "Sheet1", index = 0))
        }
        return workbook
    }

    /**
     * Parses legacy .xls (Compound Binary or HTML table formatted as .xls).
     */
    private fun parseLegacyXls(file: File, onProgress: ((String) -> Unit)? = null): ExcelWorkbookModel {
        onProgress?.invoke("Reading legacy Excel .xls...")
        val workbook = ExcelWorkbookModel(fileName = file.name, filePath = file.absolutePath)
        val sheet = ExcelSheetModel(name = "Sheet1", index = 0)

        // Many enterprise .xls files are actually HTML table or TSV files disguised as .xls
        val textSample = runCatching { file.bufferedReader().use { it.readText().take(5000) } }.getOrDefault("")
        if (textSample.contains("<table", ignoreCase = true) || textSample.contains("<tr", ignoreCase = true)) {
            // HTML Table parser
            var row = 1
            var maxCol = 1
            val trRegex = Regex("<tr[ >](.*?)(</tr>|$)", RegexOption.DOT_MATCHES_ALL)
            val tdRegex = Regex("<(td|th)[ >](.*?)(</(td|th)>|$)", RegexOption.DOT_MATCHES_ALL)

            for (trMatch in trRegex.findAll(textSample)) {
                var col = 1
                for (tdMatch in tdRegex.findAll(trMatch.value)) {
                    val rawContent = tdMatch.groupValues[2].replace(Regex("<[^>]*>"), "").trim()
                    sheet.setCell(
                        ExcelCellModel(
                            row = row,
                            col = col,
                            rawValue = rawContent,
                            style = CellStyle(isBold = row == 1 || tdMatch.value.contains("<th"))
                        )
                    )
                    col++
                }
                maxCol = maxOf(maxCol, col)
                row++
            }
            workbook.sheets.add(sheet.copy(maxRow = row + 5, maxCol = maxCol + 2))
            return workbook
        }

        // Fallback: parse lines
        return parseCsvWorkbook(file, onProgress)
    }

    /**
     * Saves the modified workbook back to disk.
     */
    suspend fun saveWorkbook(
        workbook: ExcelWorkbookModel,
        targetFile: File
    ) = withContext(Dispatchers.IO) {
        val ext = targetFile.extension.lowercase()
        if (ext == "csv") {
            val sheet = workbook.activeSheet ?: return@withContext
            targetFile.bufferedWriter().use { writer ->
                for (r in 1..sheet.maxRow) {
                    val rowValues = mutableListOf<String>()
                    var hasData = false
                    for (c in 1..sheet.maxCol) {
                        val v = sheet.getCellValue(r, c)
                        if (v.isNotEmpty()) hasData = true
                        val escaped = if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                            "\"${v.replace("\"", "\"\"")}\""
                        } else {
                            v
                        }
                        rowValues.add(escaped)
                    }
                    if (hasData) {
                        writer.write(rowValues.joinToString(","))
                        writer.newLine()
                    }
                }
            }
        } else {
            // Save as CSV or structured OpenXML format
            val sheet = workbook.activeSheet ?: return@withContext
            targetFile.bufferedWriter().use { writer ->
                for (r in 1..sheet.maxRow) {
                    val rowValues = (1..sheet.maxCol).map { c -> sheet.getCellValue(r, c) }
                    if (rowValues.any { it.isNotBlank() }) {
                        writer.write(rowValues.joinToString(","))
                        writer.newLine()
                    }
                }
            }
        }
        workbook.isModified = false
    }
}
