package app.opora.core

import app.opora.core.model.GuardSettings
import app.opora.core.model.TargetRule
import app.opora.core.model.contains

enum class BlockReason { ALWAYS, LIMIT, NIGHT, ADULT }

sealed interface Decision {
    data object Allow : Decision
    data class Pause(val rule: TargetRule, val opensToday: Int, val msToday: Long) : Decision
    data class Block(val rule: TargetRule?, val reason: BlockReason) : Decision
}

object GuardPolicy {
    /**
     * Что делать с целью на экране. Разрешение «открыть на N минут» снимает только паузу:
     * лимит, ночь и «закрыто всегда» оно не отменяет.
     */
    fun decide(
        targetId: String?,
        settings: GuardSettings,
        msToday: Long,
        opensToday: Int,
        minuteOfDay: Int,
        grantActive: Boolean,
    ): Decision {
        if (!settings.enabled || targetId == null) return Decision.Allow
        val rule = settings.targets.firstOrNull { it.id == targetId } ?: return Decision.Allow
        val limit = rule.dailyLimitMin
        return when {
            rule.alwaysBlocked -> Decision.Block(rule, BlockReason.ALWAYS)
            rule.nightBlocked && settings.night.contains(minuteOfDay) -> Decision.Block(rule, BlockReason.NIGHT)
            limit != null && msToday >= limit * 60_000L -> Decision.Block(rule, BlockReason.LIMIT)
            rule.pause && !grantActive -> Decision.Pause(rule, opensToday, msToday)
            else -> Decision.Allow
        }
    }

    fun adultMatch(settings: GuardSettings, detector: AdultContentDetector, texts: List<String?>): String? {
        if (!settings.enabled || !settings.adultFilter) return null
        for (t in texts) detector.check(t)?.let { return it }
        return null
    }
}
