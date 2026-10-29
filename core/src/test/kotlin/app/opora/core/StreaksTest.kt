package app.opora.core

import app.opora.core.model.Habit
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

const val MIN = 60_000L
const val HOUR = 60 * MIN
const val DAY = 24 * HOUR

class StreaksTest {
    @Test
    fun currentWithoutSlipsCountsFromStart() {
        val h = Habit(id = "a", name = "Порно", startedAt = 1000)
        assertEquals(5 * HOUR, Streaks.current(h, 1000 + 5 * HOUR))
    }

    @Test
    fun currentCountsFromLastSlip() {
        val h = Habit(id = "a", name = "Порно", startedAt = 0, slips = listOf(10 * HOUR))
        assertEquals(3 * HOUR, Streaks.current(h, 13 * HOUR))
    }

    @Test
    fun bestIsLongestGap() {
        val h = Habit(id = "a", name = "Порно", startedAt = 0, slips = listOf(10 * HOUR))
        assertEquals(10 * HOUR, Streaks.best(h, 13 * HOUR))
    }

    @Test
    fun bestIncludesCurrentStreak() {
        val h = Habit(id = "a", name = "Порно", startedAt = 0, slips = listOf(2 * HOUR))
        assertEquals(10 * HOUR, Streaks.best(h, 12 * HOUR))
    }

    @Test
    fun cleanDaysSkipDaysWithSlips() {
        val start = Instant.parse("2026-10-01T10:00:00Z").toEpochMilli()
        val slip = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli()
        val now = Instant.parse("2026-10-05T09:00:00Z").toEpochMilli()
        val h = Habit(id = "a", name = "Порно", startedAt = start, slips = listOf(slip))
        assertEquals(4, Streaks.cleanDays(h, now, ZoneOffset.UTC))
    }

    @Test
    fun cleanDaysUseLocalDate() {
        // По UTC начало и срыв — 1 октября, а сейчас 2-е: один чистый день.
        // В Москве (UTC+3) всё это 2 октября: чистых дней нет.
        val start = Instant.parse("2026-10-01T22:00:00Z").toEpochMilli()
        val slip = Instant.parse("2026-10-01T23:30:00Z").toEpochMilli()
        val now = Instant.parse("2026-10-02T12:00:00Z").toEpochMilli()
        val h = Habit(id = "a", name = "Порно", startedAt = start, slips = listOf(slip))
        assertEquals(1, Streaks.cleanDays(h, now, ZoneOffset.UTC))
        assertEquals(0, Streaks.cleanDays(h, now, ZoneOffset.ofHours(3)))
    }

    @Test
    fun slipInTheFutureMeansStreakIsZero() {
        // Часы телефона перевели назад: срыв записан «позже», чем сейчас.
        val h = Habit(id = "a", name = "Порно", startedAt = 0, slips = listOf(20 * HOUR))
        assertEquals(0, Streaks.current(h, 10 * HOUR))
        assertEquals(10 * HOUR, Streaks.best(h, 10 * HOUR))
    }

    @Test
    fun startInTheFutureIsNotNegative() {
        val h = Habit(id = "a", name = "Порно", startedAt = 20 * HOUR)
        assertEquals(0, Streaks.current(h, 10 * HOUR))
        assertEquals(0, Streaks.best(h, 10 * HOUR))
        assertEquals(0, Streaks.cleanDays(h, 10 * HOUR, ZoneOffset.UTC))
    }
}
