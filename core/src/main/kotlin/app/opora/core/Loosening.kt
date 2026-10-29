package app.opora.core

import app.opora.core.model.GuardSettings
import app.opora.core.model.contains

/**
 * Ослабить защиту можно только в окне WINDOW_MS, которое открывается через
 * looseningDelayMin минут после нажатия «Ослабить защиту». Ужесточать можно всегда.
 */
object Loosening {
    const val WINDOW_MS = 10 * 60_000L

    fun isLooser(old: GuardSettings, new: GuardSettings): Boolean {
        if (old.enabled && !new.enabled) return true
        if (old.adultFilter && !new.adultFilter) return true
        if ((0 until 24 * 60).any { old.night.contains(it) && !new.night.contains(it) }) return true
        if (!new.extraWords.containsAll(old.extraWords)) return true
        if (!old.allowWords.containsAll(new.allowWords)) return true
        if (new.pauseSeconds < old.pauseSeconds) return true
        if (new.looseningDelayMin < old.looseningDelayMin) return true
        for (o in old.targets) {
            val n = new.targets.firstOrNull { it.id == o.id } ?: return true
            if (o.pause && !n.pause) return true
            if (o.alwaysBlocked && !n.alwaysBlocked) return true
            if (o.nightBlocked && !n.nightBlocked) return true
            val oldLimit = o.dailyLimitMin
            val newLimit = n.dailyLimitMin
            if (oldLimit != null && (newLimit == null || newLimit > oldLimit)) return true
        }
        return false
    }

    fun windowOpen(requestedAt: Long?, delayMin: Int, now: Long): Boolean = windowLeftMs(requestedAt, delayMin, now) != null

    /** Сколько ждать до открытия окна; null — не запрошено, уже открыто или истекло. */
    fun waitLeftMs(requestedAt: Long?, delayMin: Int, now: Long): Long? {
        val openAt = (requestedAt ?: return null) + delayMin * 60_000L
        return if (now < openAt) openAt - now else null
    }

    /** Сколько окно ещё открыто; null — закрыто. */
    fun windowLeftMs(requestedAt: Long?, delayMin: Int, now: Long): Long? {
        val openAt = (requestedAt ?: return null) + delayMin * 60_000L
        val closeAt = openAt + WINDOW_MS
        return if (now in openAt until closeAt) closeAt - now else null
    }
}
