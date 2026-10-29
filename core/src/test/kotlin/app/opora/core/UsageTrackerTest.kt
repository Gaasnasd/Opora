package app.opora.core

import app.opora.core.model.AppState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UsageTrackerTest {
    private val t0 = Instant.parse("2026-10-29T12:00:00Z").toEpochMilli()
    private val day = "2026-10-29"
    private fun tracker() = UsageTracker(ZoneOffset.UTC)

    @Test
    fun switchingClosesPreviousSession() {
        val t = tracker()
        assertEquals("A", t.onForeground("A", t0).opened)
        val change = t.onForeground("B", t0 + 60_000)
        assertEquals(listOf(UsageDelta(day, "A", 60_000)), change.deltas)
        assertEquals("B", change.opened)
        assertEquals("B", t.current)
    }

    @Test
    fun sameTargetAgainIsNotANewOpen() {
        // Свой экран паузы, шторка, клавиатура: сторож снова сообщает то же приложение.
        val t = tracker()
        t.onForeground("A", t0)
        val second = t.onForeground("A", t0 + 10_000)
        val third = t.onForeground("A", t0 + 25_000)
        assertNull(second.opened)
        assertNull(third.opened)
        assertEquals(25_000, (second.deltas + third.deltas).sumOf { it.ms })
    }

    @Test
    fun sessionOverMidnightIsSplit() {
        val t = tracker()
        val start = Instant.parse("2026-10-01T23:59:30Z").toEpochMilli()
        t.onForeground("A", start)
        val deltas = t.flush(start + 60_000)
        assertEquals(
            listOf(UsageDelta("2026-10-01", "A", 30_000), UsageDelta("2026-10-02", "A", 30_000)),
            deltas,
        )
    }

    @Test
    fun screenOffClosesSession() {
        val t = tracker()
        t.onForeground("A", t0)
        assertEquals(listOf(UsageDelta(day, "A", 30_000)), t.onScreenOff(t0 + 30_000))
        assertNull(t.current)
        assertTrue(t.flush(t0 + 60_000).isEmpty())
        assertEquals("A", t.onForeground("A", t0 + 90_000).opened)
    }

    @Test
    fun flushKeepsSessionOpen() {
        val t = tracker()
        t.onForeground("A", t0)
        assertEquals(listOf(UsageDelta(day, "A", 15_000)), t.flush(t0 + 15_000))
        assertEquals(listOf(UsageDelta(day, "A", 5_000)), t.flush(t0 + 20_000))
        assertEquals("A", t.current)
    }

    @Test
    fun untrackedForegroundAccruesNothing() {
        val t = tracker()
        t.onForeground(null, t0)
        val change = t.onForeground("A", t0 + 50_000)
        assertTrue(change.deltas.isEmpty())
        assertEquals("A", change.opened)
    }

    @Test
    fun clockGoingBackwardsGivesNoNegativeTime() {
        val t = tracker()
        t.onForeground("A", t0)
        assertTrue(t.flush(t0 - 50_000).isEmpty())
        assertEquals(listOf(UsageDelta(day, "A", 10_000)), t.flush(t0 - 40_000))
    }

    @Test
    fun usageIsAddedAndOldDaysDropped() {
        val today = LocalDate.parse(day)
        val state = AppState(usage = mapOf("2026-08-30" to mapOf("A" to 5L), "2026-08-31" to mapOf("A" to 7L)))
        val next = state.withUsage(listOf(UsageDelta(day, "A", 1000), UsageDelta(day, "A", 500)), today)
        assertEquals(mapOf("2026-08-31" to mapOf("A" to 7L), day to mapOf("A" to 1500L)), next.usage)
        assertEquals(1500, next.msToday("A", today))
        assertEquals(0, next.msToday("B", today))
    }

    @Test
    fun opensAreCountedPerDay() {
        val today = LocalDate.parse(day)
        val state = AppState().withOpen(day, "A").withOpen(day, "A").withOpen("2026-10-28", "A")
        assertEquals(2, state.opensToday("A", today))
        assertEquals(0, state.opensToday("B", today))
    }

    @Test
    fun oldOpensAreDropped() {
        val state = AppState(opens = mapOf("2026-08-30" to mapOf("A" to 1), "2026-08-31" to mapOf("A" to 2)))
        assertEquals(setOf("2026-08-31", day), state.withOpen(day, "A").opens.keys)
    }
}
