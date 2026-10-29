package app.opora.core

import kotlin.test.Test
import kotlin.test.assertEquals

class RuTextTest {
    private fun days(n: Long) = RuText.plural(n, "день", "дня", "дней")

    @Test
    fun pluralForms() {
        assertEquals("1 день", days(1))
        assertEquals("3 дня", days(3))
        assertEquals("5 дней", days(5))
        assertEquals("11 дней", days(11))
        assertEquals("12 дней", days(12))
        assertEquals("21 день", days(21))
        assertEquals("22 дня", days(22))
        assertEquals("112 дней", days(112))
        assertEquals("0 дней", days(0))
    }

    @Test
    fun durationPicksTwoUnits() {
        assertEquals("3 дн. 5 ч", RuText.duration(3 * DAY + 5 * HOUR + 7 * MIN))
        assertEquals("5 ч 12 мин", RuText.duration(5 * HOUR + 12 * MIN))
        assertEquals("12 мин", RuText.duration(12 * MIN + 30_000))
        assertEquals("0 мин", RuText.duration(-5))
    }

    @Test
    fun minutesShowsHoursWhenLong() {
        assertEquals("1 ч 05 мин", RuText.minutes(65 * MIN))
        assertEquals("45 мин", RuText.minutes(45 * MIN))
        assertEquals("0 мин", RuText.minutes(0))
    }
}
