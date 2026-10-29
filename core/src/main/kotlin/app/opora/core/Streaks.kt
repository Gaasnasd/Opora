package app.opora.core

import app.opora.core.model.Habit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Streaks {
    /** Сколько прошло с последнего срыва (или с начала). Не меньше нуля, даже если часы перевели назад. */
    fun current(h: Habit, now: Long): Long {
        val last = maxOf(h.startedAt, h.slips.maxOrNull() ?: Long.MIN_VALUE)
        return (now - last).coerceAtLeast(0)
    }

    /** Самый длинный промежуток без срыва, включая текущий. События «из будущего» прижимаются к now. */
    fun best(h: Habit, now: Long): Long {
        val points = (listOf(h.startedAt) + h.slips.sorted()).map { minOf(it, now) } + now
        return points.zipWithNext { a, b -> b - a }.maxOrNull()?.coerceAtLeast(0) ?: 0
    }

    /** Календарные дни от даты начала до сегодня включительно, в которые не было срыва. */
    fun cleanDays(h: Habit, now: Long, zone: ZoneId): Int {
        if (now < h.startedAt) return 0
        val start = date(h.startedAt, zone)
        val today = date(now, zone)
        val total = today.toEpochDay() - start.toEpochDay() + 1
        val slipDays = h.slips.map { date(it, zone) }.filter { it in start..today }.toSet()
        return (total - slipDays.size).toInt()
    }

    private fun date(ms: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
}
