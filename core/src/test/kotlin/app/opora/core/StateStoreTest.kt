package app.opora.core

import app.opora.core.model.AppState
import app.opora.core.model.Habit
import app.opora.core.model.TargetRule
import app.opora.core.model.Trigger
import app.opora.core.model.UrgeEvent
import app.opora.core.model.UrgeOutcome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateStoreTest {
    private fun tempDir(): File = Files.createTempDirectory("opora").toFile()

    @Test
    fun roundTrip() {
        val dir = tempDir()
        val state = AppState(
            onboarded = true,
            habits = listOf(Habit(id = "h1", name = "Порно", startedAt = 100, slips = listOf(200))),
            urges = listOf(UrgeEvent(at = 300, outcome = UrgeOutcome.PASSED, trigger = Trigger.BOREDOM)),
        ).let { it.copy(guard = it.guard.copy(targets = listOf(TargetRule(id = "com.zhiliaoapp.musically", label = "TikTok")))) }

        val store = StateStore(dir) { 1000 }
        store.save(state)

        assertEquals(state, StateStore(dir) { 1000 }.load().state)
    }

    @Test
    fun missingFileGivesDefault() {
        val result = StateStore(tempDir()) { 1000 }.load()
        assertEquals(AppState(), result.state)
        assertNull(result.brokenBackup)
    }

    @Test
    fun brokenFileIsKeptAside() {
        val dir = tempDir()
        File(dir, "state.json").writeText("{не json")

        val result = StateStore(dir) { 1000 }.load()

        assertEquals(AppState(), result.state)
        val backup = assertNotNull(result.brokenBackup)
        assertEquals("state.broken-1000.json", backup.name)
        assertEquals("{не json", backup.readText())
    }

    @Test
    fun unknownAndMissingFieldsAreTolerated() {
        val dir = tempDir()
        File(dir, "state.json").writeText(
            """{"habits":[{"id":"a","name":"Порно","startedAt":5}],"future":1}""",
        )

        val state = StateStore(dir) { 1000 }.load().state

        assertEquals(listOf(Habit(id = "a", name = "Порно", startedAt = 5)), state.habits)
        assertEquals(AppState().guard, state.guard)
    }

    @Test
    fun saveLeavesNoTempFile() {
        val dir = tempDir()
        StateStore(dir) { 1000 }.save(AppState(onboarded = true))
        StateStore(dir) { 1000 }.save(AppState(reasons = "Ради семьи"))

        assertEquals(listOf("state.json"), dir.list()!!.toList())
        assertTrue(File(dir, "state.json").readText().contains("Ради семьи"))
    }
}
