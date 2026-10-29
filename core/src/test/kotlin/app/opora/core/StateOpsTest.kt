package app.opora.core

import app.opora.core.model.AppState
import app.opora.core.model.Habit
import app.opora.core.model.Trigger
import app.opora.core.model.UrgeEvent
import app.opora.core.model.UrgeOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class StateOpsTest {
    private val state = AppState().withHabit("Порно", now = 100, id = "p")

    @Test
    fun habitsAreAddedRenamedRemoved() {
        assertEquals(listOf(Habit(id = "p", name = "Порно", startedAt = 100)), state.habits)
        assertEquals("Порнография", state.renameHabit("p", "Порнография").habits.single().name)
        assertEquals(emptyList(), state.removeHabit("p").habits)
    }

    @Test
    fun slippedUrgeAddsSlipToHabit() {
        val next = state.withUrge(at = 500, outcome = UrgeOutcome.SLIPPED, trigger = Trigger.NIGHT, habitId = "p")
        assertEquals(listOf(500L), next.habits.single().slips)
        assertEquals(listOf(UrgeEvent(500, UrgeOutcome.SLIPPED, Trigger.NIGHT, "p")), next.urges)
    }

    @Test
    fun passedUrgeDoesNotTouchHabit() {
        val next = state.withUrge(at = 500, outcome = UrgeOutcome.PASSED, trigger = Trigger.BOREDOM, habitId = "p")
        assertEquals(emptyList(), next.habits.single().slips)
        assertEquals(1, next.urges.size)
    }

    @Test
    fun slipForUnknownHabitIsStillRecorded() {
        val next = state.withUrge(at = 500, outcome = UrgeOutcome.SLIPPED, trigger = null, habitId = "gone")
        assertEquals(state.habits, next.habits)
        assertEquals(1, next.urges.size)
    }

    @Test
    fun urgeLogIsCapped() {
        val full = AppState(urges = List(2000) { UrgeEvent(it.toLong(), UrgeOutcome.PASSED) })
        val next = full.withUrge(at = 5000, outcome = UrgeOutcome.PASSED, trigger = null, habitId = null)
        assertEquals(2000, next.urges.size)
        assertEquals(1L, next.urges.first().at)
        assertEquals(5000L, next.urges.last().at)
    }
}
