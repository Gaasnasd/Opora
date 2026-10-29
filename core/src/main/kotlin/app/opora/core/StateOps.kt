package app.opora.core

import app.opora.core.model.AppState
import app.opora.core.model.GuardSettings
import app.opora.core.model.Habit
import app.opora.core.model.Trigger
import app.opora.core.model.UrgeEvent
import app.opora.core.model.UrgeOutcome
import java.time.LocalDate

/** Сколько дней хранить учёт времени и открытий, включая сегодня. */
const val USAGE_KEEP_DAYS = 60L

/** Сколько последних тяг хранить. */
const val URGES_KEEP = 2000

private fun AppState.looseningAllowed(now: Long) =
    Loosening.windowOpen(looseningRequestedAt, guard.looseningDelayMin, now)

/** Новые настройки сторожа или null, если они ослабляют защиту, а окно не открыто. */
fun AppState.tryGuardUpdate(new: GuardSettings, now: Long): AppState? =
    if (Loosening.isLooser(guard, new) && !looseningAllowed(now)) null else copy(guard = new)

/** Удаление всех данных снимает и защиту, поэтому тоже ждёт окна. */
fun AppState.tryReset(now: Long): AppState? = if (looseningAllowed(now)) AppState() else null

fun AppState.withHabit(name: String, now: Long, id: String): AppState =
    copy(habits = habits + Habit(id = id, name = name, startedAt = now))

fun AppState.renameHabit(id: String, name: String): AppState =
    copy(habits = habits.map { if (it.id == id) it.copy(name = name) else it })

fun AppState.removeHabit(id: String): AppState = copy(habits = habits.filterNot { it.id == id })

/** Записывает тягу; срыв с привычкой добавляет срыв в неё. */
fun AppState.withUrge(at: Long, outcome: UrgeOutcome, trigger: Trigger?, habitId: String?): AppState {
    val nextHabits = if (outcome == UrgeOutcome.SLIPPED && habitId != null) {
        habits.map { if (it.id == habitId) it.copy(slips = it.slips + at) else it }
    } else {
        habits
    }
    return copy(habits = nextHabits, urges = (urges + UrgeEvent(at, outcome, trigger, habitId)).takeLast(URGES_KEEP))
}

fun AppState.withUsage(deltas: List<UsageDelta>, today: LocalDate): AppState {
    if (deltas.isEmpty()) return this
    val usage = usage.mapValues { it.value.toMutableMap() }.toMutableMap()
    for (d in deltas) {
        val day = usage.getOrPut(d.date) { mutableMapOf() }
        day[d.targetId] = (day[d.targetId] ?: 0L) + d.ms
    }
    val oldest = today.minusDays(USAGE_KEEP_DAYS - 1).toString()
    return copy(usage = usage.filterKeys { it >= oldest })
}

fun AppState.withOpen(date: String, targetId: String): AppState {
    val day = opens[date].orEmpty()
    val oldest = LocalDate.parse(date).minusDays(USAGE_KEEP_DAYS - 1).toString()
    val next = opens + (date to day + (targetId to (day[targetId] ?: 0) + 1))
    return copy(opens = next.filterKeys { it >= oldest })
}

fun AppState.msToday(targetId: String, today: LocalDate): Long = usage[today.toString()]?.get(targetId) ?: 0L

fun AppState.opensToday(targetId: String, today: LocalDate): Int = opens[today.toString()]?.get(targetId) ?: 0
