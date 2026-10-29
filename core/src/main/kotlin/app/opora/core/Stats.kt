package app.opora.core

import app.opora.core.model.NightSchedule
import app.opora.core.model.Trigger
import app.opora.core.model.UrgeEvent
import app.opora.core.model.UrgeOutcome
import app.opora.core.model.contains
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class UrgeStats(
    val total: Int,
    val passed: Int,
    val slipped: Int,
    val byHour: List<Int>,
    val byTrigger: Map<Trigger, Int>,
)

object Stats {
    private const val DAY = 24 * 60 * 60_000L

    fun urges(events: List<UrgeEvent>, now: Long, days: Int, zone: ZoneId): UrgeStats {
        val recent = events.filter { it.at >= now - days * DAY }
        val byHour = MutableList(24) { 0 }
        recent.forEach { byHour[Instant.ofEpochMilli(it.at).atZone(zone).hour]++ }
        return UrgeStats(
            total = recent.size,
            passed = recent.count { it.outcome == UrgeOutcome.PASSED },
            slipped = recent.count { it.outcome == UrgeOutcome.SLIPPED },
            byHour = byHour,
            byTrigger = recent.mapNotNull { it.trigger }.groupingBy { it }.eachCount(),
        )
    }

    /** Сумма по всем целям за каждый из последних [days] дней, от старых к новым. */
    fun usageByDay(usage: Map<String, Map<String, Long>>, today: LocalDate, days: Int): List<Pair<LocalDate, Long>> =
        (days - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            date to (usage[date.toString()]?.values?.sum() ?: 0L)
        }

    fun usageByTarget(usage: Map<String, Map<String, Long>>, today: LocalDate, days: Int): List<Pair<String, Long>> {
        val oldest = today.minusDays(days - 1L).toString()
        val newest = today.toString()
        val sums = mutableMapOf<String, Long>()
        usage.filterKeys { it in oldest..newest }.values.forEach { day ->
            day.forEach { (target, ms) -> sums[target] = (sums[target] ?: 0L) + ms }
        }
        return sums.toList().sortedByDescending { it.second }
    }

    fun hints(s: UrgeStats, night: NightSchedule): List<String> {
        val hints = mutableListOf<String>()

        if (s.total >= 5) {
            val peak = (0 until 24).maxBy { s.byHour[it] + s.byHour[(it + 1) % 24] }
            val lateHour = peak >= 18 || peak <= 5
            if (lateHour && !night.contains(peak * 60)) {
                hints += "Чаще всего тянет с %02d:00 до %02d:00, а ночной режим это время не закрывает."
                    .format(peak, (peak + 2) % 24)
            }
        }

        val triggered = s.byTrigger.values.sum()
        val main = s.byTrigger.maxByOrNull { it.value }
        if (main != null && main.value >= 2 && main.value * 10 >= triggered * 4) {
            ADVICE[main.key]?.let { hints += it }
        }

        if (s.total >= 3 && s.slipped * 2 < s.total) {
            hints += "Из ${RuText.plural(s.total.toLong(), "тяги", "тяг", "тяг")} вы пережили ${s.passed}. Это работает."
        }
        return hints
    }

    private val ADVICE = mapOf(
        Trigger.BOREDOM to "Главная причина — скука. Решите заранее, чем занять руки: прогулка, отжимания, звонок.",
        Trigger.STRESS to "Главная причина — стресс. Помогает дыхание на экране «Тяга» и короткая прогулка.",
        Trigger.LONELINESS to "Главная причина — одиночество. Когда тянет, напишите или позвоните кому-нибудь.",
        Trigger.TIRED to "Главная причина — усталость. Ложитесь раньше, а телефон заряжайте вне спальни.",
        Trigger.NIGHT to "Чаще всего тянет ночью в кровати. Заряжайте телефон в другой комнате.",
        Trigger.FEED to "Тянет после лент. Поставьте им лимит или закройте совсем.",
    )
}
