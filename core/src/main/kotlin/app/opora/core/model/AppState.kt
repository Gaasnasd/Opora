package app.opora.core.model

import kotlinx.serialization.Serializable

/** Всё, что приложение хранит. Лежит одним JSON-файлом во внутренней памяти. */
@Serializable
data class AppState(
    val onboarded: Boolean = false,
    val habits: List<Habit> = emptyList(),
    val urges: List<UrgeEvent> = emptyList(),
    val reasons: String = "",
    val guard: GuardSettings = GuardSettings(),
    /** "yyyy-MM-dd" → цель → миллисекунды на экране. */
    val usage: Map<String, Map<String, Long>> = emptyMap(),
    /** "yyyy-MM-dd" → цель → сколько раз открывали. */
    val opens: Map<String, Map<String, Int>> = emptyMap(),
    val pin: PinData? = null,
    /** Когда нажали «Ослабить защиту»; окно откроется через looseningDelayMin. */
    val looseningRequestedAt: Long? = null,
)

@Serializable
data class Habit(
    val id: String,
    val name: String,
    val startedAt: Long,
    val slips: List<Long> = emptyList(),
)

@Serializable
data class UrgeEvent(
    val at: Long,
    val outcome: UrgeOutcome,
    val trigger: Trigger? = null,
    val habitId: String? = null,
)

enum class UrgeOutcome { PASSED, SLIPPED }

enum class Trigger(val title: String) {
    BOREDOM("Скука"),
    STRESS("Стресс"),
    LONELINESS("Одиночество"),
    TIRED("Усталость"),
    NIGHT("Ночь в кровати"),
    FEED("Увидел в ленте"),
    OTHER("Другое"),
}

@Serializable
data class GuardSettings(
    val enabled: Boolean = true,
    val adultFilter: Boolean = true,
    val extraWords: List<String> = emptyList(),
    val allowWords: List<String> = emptyList(),
    val targets: List<TargetRule> = emptyList(),
    val night: NightSchedule = NightSchedule(),
    val pauseSeconds: Int = 10,
    val looseningDelayMin: Int = 15,
)

/**
 * Правило для приложения (id — имя пакета) или раздела внутри него
 * (id — "пакет#раздел", например Shorts в YouTube).
 */
@Serializable
data class TargetRule(
    val id: String,
    val label: String,
    val pause: Boolean = true,
    val dailyLimitMin: Int? = null,
    val alwaysBlocked: Boolean = false,
    val nightBlocked: Boolean = true,
)

/** Ночной интервал в минутах от полуночи; может переходить через полночь. */
@Serializable
data class NightSchedule(
    val enabled: Boolean = true,
    val startMin: Int = 23 * 60,
    val endMin: Int = 7 * 60,
)

/** Попадает ли минута суток в ночь. Выключенная или пустая (start == end) ночь — никогда. */
fun NightSchedule.contains(minuteOfDay: Int): Boolean = when {
    !enabled || startMin == endMin -> false
    startMin < endMin -> minuteOfDay in startMin until endMin
    else -> minuteOfDay >= startMin || minuteOfDay < endMin
}

@Serializable
data class PinData(val salt: String, val hash: String)
