package app.opora.core

import app.opora.core.model.AppState
import java.time.LocalDate

/** Сколько дней хранить учёт времени и открытий, включая сегодня. */
const val USAGE_KEEP_DAYS = 60L

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
