package app.opora.core

import app.opora.core.model.NightSchedule
import app.opora.core.model.Trigger
import app.opora.core.model.UrgeEvent
import app.opora.core.model.UrgeOutcome
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatsTest {
    private val now = Instant.parse("2026-10-29T12:00:00Z").toEpochMilli()
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()
    private fun ev(iso: String, outcome: UrgeOutcome = UrgeOutcome.PASSED, trigger: Trigger? = null) =
        UrgeEvent(at(iso), outcome, trigger)

    @Test
    fun urgesInRangeByLocalHourAndTrigger() {
        val events = listOf(
            ev("2026-09-27T12:00:00Z", trigger = Trigger.OTHER),
            ev("2026-10-28T20:30:00Z", trigger = Trigger.BOREDOM),
            ev("2026-10-29T09:10:00Z", UrgeOutcome.SLIPPED, Trigger.STRESS),
        )
        val s = Stats.urges(events, now, days = 30, zone = ZoneOffset.ofHours(3))
        assertEquals(2, s.total)
        assertEquals(1, s.passed)
        assertEquals(1, s.slipped)
        assertEquals(24, s.byHour.size)
        assertEquals(1, s.byHour[23])
        assertEquals(1, s.byHour[12])
        assertEquals(mapOf(Trigger.BOREDOM to 1, Trigger.STRESS to 1), s.byTrigger)
    }

    @Test
    fun usageByDayFillsGaps() {
        val usage = mapOf(
            "2026-10-29" to mapOf("A" to 1000L, "B" to 500L),
            "2026-10-27" to mapOf("A" to 200L),
            "2026-10-01" to mapOf("A" to 99L),
        )
        val days = Stats.usageByDay(usage, LocalDate.parse("2026-10-29"), 7)
        assertEquals(LocalDate.parse("2026-10-23"), days.first().first)
        assertEquals(listOf(0L, 0, 0, 0, 200, 0, 1500), days.map { it.second })
    }

    @Test
    fun usageByTargetSortedDescending() {
        val usage = mapOf(
            "2026-10-29" to mapOf("A" to 1000L, "B" to 500L),
            "2026-10-27" to mapOf("A" to 200L),
            "2026-10-01" to mapOf("C" to 99_999L),
        )
        assertEquals(listOf("A" to 1200L, "B" to 500L), Stats.usageByTarget(usage, LocalDate.parse("2026-10-29"), 7))
    }

    private fun lateNight() = listOf(
        ev("2026-10-26T23:10:00Z"), ev("2026-10-27T23:20:00Z"), ev("2026-10-28T23:40:00Z"),
        ev("2026-10-27T00:15:00Z"), ev("2026-10-28T00:30:00Z"),
    )

    @Test
    fun hintAboutPeakHoursOutsideNight() {
        val s = Stats.urges(lateNight(), now, 30, ZoneOffset.UTC)
        val hints = Stats.hints(s, NightSchedule(startMin = 0, endMin = 7 * 60))
        assertTrue(hints.any { "с 23:00 до 01:00" in it }, hints.toString())
    }

    @Test
    fun noPeakHintWhenNightCoversIt() {
        val s = Stats.urges(lateNight(), now, 30, ZoneOffset.UTC)
        assertFalse(Stats.hints(s, NightSchedule()).any { "до 01:00" in it })
    }

    @Test
    fun noPeakHintWithFewUrges() {
        val s = Stats.urges(lateNight().take(4), now, 30, ZoneOffset.UTC)
        assertFalse(Stats.hints(s, NightSchedule(enabled = false)).any { "тянет с" in it })
    }

    @Test
    fun adviceForMainTrigger() {
        val events = listOf(
            ev("2026-10-28T10:00:00Z", trigger = Trigger.BOREDOM),
            ev("2026-10-28T11:00:00Z", trigger = Trigger.BOREDOM),
            ev("2026-10-28T12:00:00Z", UrgeOutcome.SLIPPED, Trigger.STRESS),
            ev("2026-10-28T13:00:00Z"),
        )
        val hints = Stats.hints(Stats.urges(events, now, 30, ZoneOffset.UTC), NightSchedule())
        assertTrue(hints.any { it.startsWith("Главная причина — скука") }, hints.toString())
        assertTrue("Из 4 тяг вы пережили 3. Это работает." in hints, hints.toString())
    }
}
