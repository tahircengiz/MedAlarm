package com.medalarm.app.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate

class DayHeaderTest {

    private val today = LocalDate.of(2026, 11, 12)

    @Test
    fun `no date subtitle on today`() {
        assertThat(dayHeaderSubtitleDate(today, today)).isNull()
    }

    @Test
    fun `yesterday and tomorrow keep the date as subtitle`() {
        assertThat(dayHeaderSubtitleDate(today.minusDays(1), today)).isEqualTo(today.minusDays(1))
        assertThat(dayHeaderSubtitleDate(today.plusDays(1), today)).isEqualTo(today.plusDays(1))
    }

    @Test
    fun `far days show the date once, not again as subtitle`() {
        // The title is already the formatted date for these days.
        assertThat(dayHeaderSubtitleDate(today.minusDays(2), today)).isNull()
        assertThat(dayHeaderSubtitleDate(today.plusDays(5), today)).isNull()
    }

    @Test
    fun `jump-to-today button text differs from the Today title`() {
        // On a past/future day the jump button sits right under the date; if its
        // text equals the "Today" title it reads as if the selected day were today.
        listOf("values", "values-tr").forEach { dir ->
            val xml = stringsXml(dir)
            assertThat(stringValue(xml, "home_jump_today"))
                .isNotEqualTo(stringValue(xml, "home_today"))
        }
    }

    private fun stringsXml(dir: String): String {
        val rel = "src/main/res/$dir/strings.xml"
        val file = listOf(File(rel), File("app/$rel")).first { it.exists() }
        return file.readText()
    }

    private fun stringValue(xml: String, name: String): String =
        Regex("""<string name="$name">(.*?)</string>""").find(xml)?.groupValues?.get(1)
            ?: error("string $name not found")
}
