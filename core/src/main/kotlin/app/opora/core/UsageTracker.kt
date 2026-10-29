package app.opora.core

import java.time.Instant
import java.time.ZoneId

data class UsageDelta(val date: String, val targetId: String, val ms: Long)

/** [opened] — цель, которую только что открыли (была другая или ничего). */
data class ForegroundChange(val deltas: List<UsageDelta>, val opened: String?)

/**
 * Считает время цели на экране по событиям сторожа. Отдаёт приросты по дням;
 * сессия через полночь делится между датами.
 */
class UsageTracker(private val zone: ZoneId) {
    var current: String? = null
        private set
    private var since = 0L

    fun onForeground(targetId: String?, now: Long): ForegroundChange {
        if (targetId == current) return ForegroundChange(flush(now), opened = null)
        val deltas = flush(now)
        current = targetId
        since = now
        return ForegroundChange(deltas, opened = targetId)
    }

    fun onScreenOff(now: Long): List<UsageDelta> {
        val deltas = flush(now)
        current = null
        return deltas
    }

    /** Отдаёт накопленное с прошлого раза; сессия продолжается. */
    fun flush(now: Long): List<UsageDelta> {
        val target = current
        if (target == null || now <= since) {
            since = now
            return emptyList()
        }
        val deltas = mutableListOf<UsageDelta>()
        var from = since
        while (from < now) {
            val date = Instant.ofEpochMilli(from).atZone(zone).toLocalDate()
            val midnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val to = minOf(now, midnight)
            deltas += UsageDelta(date.toString(), target, to - from)
            from = to
        }
        since = now
        return deltas
    }
}
