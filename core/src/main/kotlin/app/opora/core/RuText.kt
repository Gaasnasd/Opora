package app.opora.core

object RuText {
    private const val MIN = 60_000L
    private const val HOUR = 60 * MIN
    private const val DAY = 24 * HOUR

    /** «1 день», «3 дня», «11 дней». */
    fun plural(n: Long, one: String, few: String, many: String): String {
        val mod10 = n % 10
        val mod100 = n % 100
        val word = when {
            mod10 == 1L && mod100 != 11L -> one
            mod10 in 2..4 && mod100 !in 12..14 -> few
            else -> many
        }
        return "$n $word"
    }

    /** Две старшие единицы: «3 дн. 5 ч», «5 ч 12 мин», «12 мин». */
    fun duration(ms: Long): String {
        if (ms <= 0) return "0 мин"
        val days = ms / DAY
        val hours = ms % DAY / HOUR
        val mins = ms % HOUR / MIN
        return when {
            days > 0 -> "$days дн. $hours ч"
            hours > 0 -> "$hours ч $mins мин"
            else -> "$mins мин"
        }
    }

    /** Время в лентах: «1 ч 05 мин», «45 мин». */
    fun minutes(ms: Long): String {
        val total = ms.coerceAtLeast(0) / MIN
        return if (total >= 60) "${total / 60} ч ${"%02d".format(total % 60)} мин" else "$total мин"
    }
}
