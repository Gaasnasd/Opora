package app.opora.core

import java.io.ByteArrayOutputStream

/**
 * Ищет сайты и запросы 18+ в строке из браузера: адрес, поиск, заголовок.
 * Возвращает сработавшее правило или null. Никогда не бросает исключений.
 */
class AdultContentDetector(
    extraWords: Collection<String> = emptyList(),
    allowWords: Collection<String> = emptyList(),
) {
    private val extra = extraWords.map(::clean).filter { it.isNotEmpty() }
    private val allow = (BUILTIN_ALLOW + allowWords.map(::clean)).filter { it.isNotEmpty() }

    fun check(text: String?): String? {
        if (text.isNullOrEmpty()) return null
        var s = normalize(text)
        for (phrase in allow) s = s.replace(phrase, " ")

        (SUBSTRINGS + DOMAINS + extra).firstOrNull { s.contains(it) }?.let { return it }

        for (token in s.split(NON_WORD)) {
            if (token.isEmpty()) continue
            if (token in WORDS) return token
            STEMS.firstOrNull { token.startsWith(it) }?.let { return it }
        }
        return null
    }

    private companion object {
        const val MAX_LEN = 4000
        val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

        /** Ловятся в любом месте строки. */
        val SUBSTRINGS = listOf(
            "porn", "порн", "порев", "xvideo", "xnxx", "xhamster", "hentai", "хентай", "brazzers",
            "onlyfans", "chaturbate", "redtube", "youporn", "spankbang", "eporner", "stripchat",
            "bongacams", "livejasmin", "rule34", "nsfw", "эротик", "erotic",
        )

        /** Сайты, в имени которых нет «говорящих» слов. */
        val DOMAINS = listOf(
            "beeg.com", "motherless.com", "tnaflix.com", "tube8.com", "fapello.com", "erome.com",
            "f95zone", "thothub", "sxyprn", "daftsex",
        )

        /** Ловятся только как отдельное слово: «sex», но не «Essex». */
        val WORDS = setOf(
            "sex", "xxx", "nude", "nudes", "naked", "milf", "boobs", "tits", "pussy", "blowjob",
            "cumshot", "anal", "camgirl", "camgirls",
            "голая", "голые", "голых", "голую", "голой", "голенькая", "голенькие",
            "обнаженная", "обнаженные", "обнаженных",
        )

        /** Русские основы: слово начинается с них («секса», «сексом»). */
        val STEMS = listOf("секс", "трах", "минет", "шлюх", "проститут", "сиськ", "дроч", "отсос")

        /** Вырезаются до поиска: просьба о помощи и медицина не должны закрываться. */
        val BUILTIN_ALLOW = listOf(
            "сексолог", "бросить порн", "отказ от порн", "отказаться от порн", "зависимость от порн",
            "зависимости от порн", "порнозависим", "nofap", "quit porn", "porn addiction", "трахе",
        )

        fun clean(word: String): String = word.trim().lowercase().replace('ё', 'е')

        fun normalize(raw: String): String {
            val cut = if (raw.length > MAX_LEN) raw.substring(0, MAX_LEN) else raw
            return percentDecode(cut.replace('+', ' ')).lowercase().replace('ё', 'е')
        }

        /** %D0%BF → «п». Битые последовательности остаются как есть. */
        fun percentDecode(s: String): String {
            if ('%' !in s) return s
            val out = ByteArrayOutputStream(s.length)
            var i = 0
            while (i < s.length) {
                if (s[i] == '%' && i + 2 < s.length) {
                    val hi = Character.digit(s[i + 1], 16)
                    val lo = Character.digit(s[i + 2], 16)
                    if (hi >= 0 && lo >= 0) {
                        out.write(hi * 16 + lo)
                        i += 3
                        continue
                    }
                }
                val cp = s.codePointAt(i)
                out.write(String(Character.toChars(cp)).toByteArray(Charsets.UTF_8))
                i += Character.charCount(cp)
            }
            return String(out.toByteArray(), Charsets.UTF_8)
        }
    }
}
