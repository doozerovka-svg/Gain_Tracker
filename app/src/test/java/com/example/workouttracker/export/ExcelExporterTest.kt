package com.example.workouttracker.export

import com.example.workouttracker.domain.model.Exercise
import com.example.workouttracker.domain.model.SetEntry
import com.example.workouttracker.domain.model.WorkoutSession
import com.example.workouttracker.domain.model.WorkoutSessionWithSets
import com.example.workouttracker.domain.model.WorkoutStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ExcelExporterTest {

    private val exercises = mapOf(
        1L to Exercise(id = 1, name = "Жим лёжа", categoryId = 1),
        2L to Exercise(id = 2, name = "Приседания", categoryId = 2)
    )

    private fun createTestSessions(): List<WorkoutSessionWithSets> {
        return listOf(
            WorkoutSessionWithSets(
                session = WorkoutSession(id = 1, date = 1723939200000L, status = WorkoutStatus.COMPLETED, notes = "Хорошая тренировка"),
                sets = listOf(
                    SetEntry(id = 1, workoutSessionId = 1, exerciseId = 1, setNumber = 1, weightKg = 80.0, reps = 8, rir = 2),
                    SetEntry(id = 2, workoutSessionId = 1, exerciseId = 1, setNumber = 2, weightKg = 82.5, reps = 7, rir = 1),
                    SetEntry(id = 3, workoutSessionId = 1, exerciseId = 2, setNumber = 1, weightKg = 100.0, reps = 5, rir = 3)
                )
            ),
            WorkoutSessionWithSets(
                session = WorkoutSession(id = 2, date = 1724025600000L, status = WorkoutStatus.DRAFT, notes = ""),
                sets = listOf(
                    SetEntry(id = 4, workoutSessionId = 2, exerciseId = 1, setNumber = 1, weightKg = 85.0, reps = 6, rir = 0)
                )
            )
        )
    }

    @Test
    fun `exported xlsx is valid zip containing required ooxml entries`() {
        val output = ByteArrayOutputStream()
        ExcelExporter.exportToStream(createTestSessions(), exercises, output)

        val bytes = output.toByteArray()
        assertThat(bytes.size).isGreaterThan(0)

        val entries = mutableListOf<String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        assertThat(entries).contains("[Content_Types].xml")
        assertThat(entries).contains("_rels/.rels")
        assertThat(entries).contains("xl/workbook.xml")
        assertThat(entries).contains("xl/_rels/workbook.xml.rels")
        assertThat(entries).contains("xl/styles.xml")
        assertThat(entries).contains("xl/sharedStrings.xml")
        assertThat(entries).contains("xl/worksheets/sheet1.xml")
        assertThat(entries).contains("xl/worksheets/sheet2.xml")
    }

    @Test
    fun `workbook xml contains two sheets with russian names`() {
        val output = ByteArrayOutputStream()
        ExcelExporter.exportToStream(createTestSessions(), exercises, output)

        val workbookXml = extractZipEntry(output.toByteArray(), "xl/workbook.xml")
        assertThat(workbookXml).contains("Тренировки")
        assertThat(workbookXml).contains("Подходы")
    }

    @Test
    fun `shared strings contain russian headers and exercise names`() {
        val output = ByteArrayOutputStream()
        ExcelExporter.exportToStream(createTestSessions(), exercises, output)

        val sharedStrings = extractZipEntry(output.toByteArray(), "xl/sharedStrings.xml")
        assertThat(sharedStrings).contains("Дата")
        assertThat(sharedStrings).contains("Статус")
        assertThat(sharedStrings).contains("Заметки")
        assertThat(sharedStrings).contains("Объём (кг)")
        assertThat(sharedStrings).contains("Упражнение")
        assertThat(sharedStrings).contains("Повторения")
        assertThat(sharedStrings).contains("Жим лёжа")
        assertThat(sharedStrings).contains("Приседания")
        assertThat(sharedStrings).contains("Завершена")
        assertThat(sharedStrings).contains("Черновик")
    }

    @Test
    fun `sheet1 contains correct number of data rows plus header`() {
        val sessions = createTestSessions()
        val output = ByteArrayOutputStream()
        ExcelExporter.exportToStream(sessions, exercises, output)

        val sheet1 = extractZipEntry(output.toByteArray(), "xl/worksheets/sheet1.xml")
        // Header row + 2 session rows = 3 total <row> elements
        val rowCount = "<row ".toRegex().findAll(sheet1).count()
        assertThat(rowCount).isEqualTo(3)
    }

    @Test
    fun `sheet2 contains correct number of set rows plus header`() {
        val sessions = createTestSessions()
        val output = ByteArrayOutputStream()
        ExcelExporter.exportToStream(sessions, exercises, output)

        val sheet2 = extractZipEntry(output.toByteArray(), "xl/worksheets/sheet2.xml")
        // Header + 4 sets = 5 total rows
        val rowCount = "<row ".toRegex().findAll(sheet2).count()
        assertThat(rowCount).isEqualTo(5)
    }

    @Test
    fun `empty sessions produce valid xlsx with only headers`() {
        val output = ByteArrayOutputStream()
        ExcelExporter.exportToStream(emptyList(), exercises, output)

        val bytes = output.toByteArray()
        assertThat(bytes.size).isGreaterThan(0)

        val sheet1 = extractZipEntry(bytes, "xl/worksheets/sheet1.xml")
        val rowCount = "<row ".toRegex().findAll(sheet1).count()
        assertThat(rowCount).isEqualTo(1) // Header only
    }

    @Test
    fun `xml special characters in notes are escaped`() {
        val sessions = listOf(
            WorkoutSessionWithSets(
                session = WorkoutSession(id = 1, date = 1723939200000L, status = WorkoutStatus.COMPLETED, notes = "Тест <script> & \"кавычки\""),
                sets = emptyList()
            )
        )
        val output = ByteArrayOutputStream()
        ExcelExporter.exportToStream(sessions, exercises, output)

        val sharedStrings = extractZipEntry(output.toByteArray(), "xl/sharedStrings.xml")
        assertThat(sharedStrings).doesNotContain("<script>")
        assertThat(sharedStrings).contains("&lt;script&gt;")
        assertThat(sharedStrings).contains("&amp;")
        assertThat(sharedStrings).contains("&quot;кавычки&quot;")
    }

    @Test
    fun `unknown exercise id shows fallback name in shared strings`() {
        val sessions = listOf(
            WorkoutSessionWithSets(
                session = WorkoutSession(id = 1, date = 1723939200000L, status = WorkoutStatus.COMPLETED),
                sets = listOf(
                    SetEntry(id = 1, workoutSessionId = 1, exerciseId = 999, setNumber = 1, weightKg = 50.0, reps = 10, rir = 2)
                )
            )
        )
        val output = ByteArrayOutputStream()
        ExcelExporter.exportToStream(sessions, exercises, output)

        val sharedStrings = extractZipEntry(output.toByteArray(), "xl/sharedStrings.xml")
        assertThat(sharedStrings).contains("Упражнение #999")
    }

    private fun extractZipEntry(zipBytes: ByteArray, entryName: String): String {
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (entry.name == entryName) {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        throw AssertionError("Entry $entryName not found in ZIP")
    }
}
