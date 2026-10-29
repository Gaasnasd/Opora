package app.opora.core

import app.opora.core.model.AppState
import app.opora.core.model.GuardSettings
import app.opora.core.model.NightSchedule
import app.opora.core.model.TargetRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LooseningTest {
    private val rule = TargetRule(id = "t", label = "TikTok", dailyLimitMin = 30, alwaysBlocked = true)
    private val free = TargetRule(id = "f", label = "Reddit", pause = false, nightBlocked = false)
    private val base = GuardSettings(
        targets = listOf(rule, free),
        extraWords = listOf("a"),
        allowWords = listOf("x"),
    )

    private fun withRule(r: TargetRule) = base.copy(targets = listOf(r, free))

    @Test
    fun looserChanges() {
        mapOf(
            "сторож выключен" to base.copy(enabled = false),
            "фильтр 18+ выключен" to base.copy(adultFilter = false),
            "ночь выключена" to base.copy(night = base.night.copy(enabled = false)),
            "ночь начинается позже" to base.copy(night = base.night.copy(startMin = 0)),
            "ночь сдвинута на час раньше" to base.copy(night = NightSchedule(startMin = 22 * 60, endMin = 6 * 60)),
            "убрали своё слово" to base.copy(extraWords = emptyList()),
            "добавили исключение" to base.copy(allowWords = listOf("x", "y")),
            "пауза короче" to base.copy(pauseSeconds = 5),
            "задержка короче" to base.copy(looseningDelayMin = 5),
            "убрали правило" to base.copy(targets = listOf(free)),
            "сняли паузу" to withRule(rule.copy(pause = false)),
            "сняли «закрыто всегда»" to withRule(rule.copy(alwaysBlocked = false)),
            "сняли «закрыто ночью»" to withRule(rule.copy(nightBlocked = false)),
            "сняли лимит" to withRule(rule.copy(dailyLimitMin = null)),
            "увеличили лимит" to withRule(rule.copy(dailyLimitMin = 60)),
        ).forEach { (what, new) -> assertTrue(Loosening.isLooser(base, new), what) }
    }

    @Test
    fun stricterOrSameChanges() {
        mapOf(
            "ничего не поменяли" to base,
            "ночь начинается раньше" to base.copy(night = base.night.copy(startMin = 22 * 60)),
            "уменьшили лимит" to withRule(rule.copy(dailyLimitMin = 15)),
            "поставили лимит" to base.copy(targets = listOf(rule, free.copy(dailyLimitMin = 15))),
            "добавили правило" to base.copy(targets = base.targets + TargetRule(id = "n", label = "VK")),
            "добавили своё слово" to base.copy(extraWords = listOf("a", "b")),
            "убрали исключение" to base.copy(allowWords = emptyList()),
            "пауза длиннее" to base.copy(pauseSeconds = 20),
            "задержка длиннее" to base.copy(looseningDelayMin = 30),
            "включили паузу" to base.copy(targets = listOf(rule, free.copy(pause = true))),
        ).forEach { (what, new) -> assertFalse(Loosening.isLooser(base, new), what) }
    }

    @Test
    fun windowOpensAfterDelayForTenMinutes() {
        assertFalse(Loosening.windowOpen(0, 15, 15 * MIN - 1))
        assertTrue(Loosening.windowOpen(0, 15, 15 * MIN))
        assertTrue(Loosening.windowOpen(0, 15, 25 * MIN - 1))
        assertFalse(Loosening.windowOpen(0, 15, 25 * MIN))
        assertFalse(Loosening.windowOpen(null, 15, 15 * MIN))
    }

    @Test
    fun timeLeft() {
        assertEquals(10 * MIN, Loosening.waitLeftMs(0, 15, 5 * MIN))
        assertNull(Loosening.waitLeftMs(0, 15, 15 * MIN))
        assertNull(Loosening.waitLeftMs(0, 15, 30 * MIN))
        assertNull(Loosening.waitLeftMs(null, 15, 0))
        assertEquals(5 * MIN, Loosening.windowLeftMs(0, 15, 20 * MIN))
        assertNull(Loosening.windowLeftMs(0, 15, 10 * MIN))
        assertNull(Loosening.windowLeftMs(0, 15, 26 * MIN))
    }

    @Test
    fun guardUpdateNeedsOpenWindowOnlyWhenLooser() {
        val state = AppState(guard = base)
        val looser = base.copy(enabled = false)
        val stricter = base.copy(pauseSeconds = 30)

        assertNull(state.tryGuardUpdate(looser, now = 0))
        assertEquals(stricter, state.tryGuardUpdate(stricter, now = 0)?.guard)

        val requested = state.copy(looseningRequestedAt = 0)
        assertNull(requested.tryGuardUpdate(looser, now = 5 * MIN))
        assertEquals(looser, requested.tryGuardUpdate(looser, now = 16 * MIN)?.guard)
    }

    @Test
    fun resetIsLoosening() {
        val state = AppState(onboarded = true, guard = base)
        assertNull(state.tryReset(now = 0))
        assertEquals(AppState(), state.copy(looseningRequestedAt = 0).tryReset(now = 16 * MIN))
    }
}
