package com.example.workouttracker.export

import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Offline Excel (.xlsx) exporter using streaming OOXML/ZIP generation.
 * No Apache POI dependency — pure Android SDK.
 */
object ExcelExporter {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))

    fun exportToStream(
        sessions: List<WorkoutSessionWithSets>,
        exercises: Map<Long, Exercise>,
        outputStream: OutputStream
    ) {
        val sharedStrings = mutableListOf<String>()
        fun addString(s: String): Int {
            val idx = sharedStrings.indexOf(s)
            return if (idx >= 0) idx else {
                sharedStrings.add(s)
                sharedStrings.size - 1
            }
        }

        // Pre-build sheet data
        val sheet1Header = listOf("Дата", "Статус", "Заметки", "Кол-во подходов", "Объём (кг)")
        val sheet2Header = listOf("Дата тренировки", "Упражнение", "Подход №", "Вес (кг)", "Повторения", "RIR")

        sheet1Header.forEach { addString(it) }
        sheet2Header.forEach { addString(it) }

        val sheet1Rows = mutableListOf<List<Any>>()
        val sheet2Rows = mutableListOf<List<Any>>()

        for (sw in sessions.sortedByDescending { it.session.date }) {
            val session = sw.session
            val dateStr = dateFormat.format(Date(session.date))
            val statusStr = if (session.status == WorkoutStatus.COMPLETED) "Завершена" else "Черновик"
            val volume = sw.sets.sumOf { it.weightKg * it.reps }

            val dateSI = addString(dateStr)
            val statusSI = addString(statusStr)
            val notesSI = addString(session.notes)

            sheet1Rows.add(listOf(dateSI, statusSI, notesSI, sw.sets.size, volume))

            for (set in sw.sets.sortedBy { it.setNumber }) {
                val exerciseName = exercises[set.exerciseId]?.name ?: "Упражнение #${set.exerciseId}"
                val sessionDateSI = addString(dateStr)
                val exNameSI = addString(exerciseName)
                sheet2Rows.add(listOf(sessionDateSI, exNameSI, set.setNumber, set.weightKg, set.reps, set.rir))
            }
        }

        val zip = ZipOutputStream(outputStream)

        // [Content_Types].xml
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write(contentTypesXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // _rels/.rels
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write(relsXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // xl/workbook.xml
        zip.putNextEntry(ZipEntry("xl/workbook.xml"))
        zip.write(workbookXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // xl/_rels/workbook.xml.rels
        zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zip.write(workbookRelsXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // xl/styles.xml
        zip.putNextEntry(ZipEntry("xl/styles.xml"))
        zip.write(stylesXml().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // xl/sharedStrings.xml
        zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
        zip.write(sharedStringsXml(sharedStrings).toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // Sheet 1 - Тренировки
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        zip.write(
            sheetXml(
                headerIndices = sheet1Header.map { sharedStrings.indexOf(it) },
                rows = sheet1Rows,
                numericColumns = setOf(3, 4) // Кол-во подходов, Объём
            ).toByteArray(Charsets.UTF_8)
        )
        zip.closeEntry()

        // Sheet 2 - Подходы
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
        zip.write(
            sheetXml(
                headerIndices = sheet2Header.map { sharedStrings.indexOf(it) },
                rows = sheet2Rows,
                numericColumns = setOf(2, 3, 4, 5) // Подход №, Вес, Повторения, RIR
            ).toByteArray(Charsets.UTF_8)
        )
        zip.closeEntry()

        zip.finish()
        zip.flush()
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

    private fun relsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Тренировки" sheetId="1" r:id="rId1"/>
    <sheet name="Подходы" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""

    private fun sharedStringsXml(strings: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">""")
        for (s in strings) {
            sb.append("<si><t>").append(escapeXml(s)).append("</t></si>")
        }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun sheetXml(
        headerIndices: List<Int>,
        rows: List<List<Any>>,
        numericColumns: Set<Int>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<sheetData>")

        // Header row (bold style s="1")
        sb.append("""<row r="1">""")
        headerIndices.forEachIndexed { colIdx, strIdx ->
            val col = colRef(colIdx)
            sb.append("""<c r="${col}1" t="s" s="1"><v>$strIdx</v></c>""")
        }
        sb.append("</row>")

        // Data rows
        rows.forEachIndexed { rowIdx, row ->
            val rowNum = rowIdx + 2
            sb.append("""<row r="$rowNum">""")
            row.forEachIndexed { colIdx, value ->
                val col = colRef(colIdx)
                val ref = "$col$rowNum"
                if (colIdx in numericColumns) {
                    val numStr = when (value) {
                        is Double -> "%.2f".format(value)
                        is Int -> value.toString()
                        else -> value.toString()
                    }
                    sb.append("""<c r="$ref"><v>$numStr</v></c>""")
                } else {
                    sb.append("""<c r="$ref" t="s"><v>$value</v></c>""")
                }
            }
            sb.append("</row>")
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun colRef(index: Int): String {
        var i = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + i % 26))
            i = i / 26 - 1
        } while (i >= 0)
        return sb.toString()
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
