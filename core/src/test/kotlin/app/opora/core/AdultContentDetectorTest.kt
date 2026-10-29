package app.opora.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AdultContentDetectorTest {
    private val d = AdultContentDetector()

    @Test
    fun catchesAdultSitesAndQueries() {
        listOf(
            "https://www.pornhub.com/view_video",
            "m.xhamster.com",
            "rule34.xxx",
            "XVIDEOS",
            "порно видео",
            "смотреть секс онлайн",
            "видео секса",
            "Порно",
            "google.com/search?q=%D0%BF%D0%BE%D1%80%D0%BD%D0%BE",
            "q=porn+videos",
            "beeg.com",
            "голые девушки",
        ).forEach { assertNotNull(d.check(it), "должно ловить: $it") }
    }

    @Test
    fun leavesOrdinaryTextAlone() {
        listOf(
            "Essex county news",
            "сексолог москва",
            "как бросить порно",
            "трахея воспаление",
            "погода в москве",
            "голосовые сообщения",
            "",
        ).forEach { assertNull(d.check(it), "не должно ловить: $it") }
        assertNull(d.check(null))
    }

    @Test
    fun ownWordsAreBlocked() {
        assertEquals("badsite", AdultContentDetector(extraWords = listOf("BadSite")).check("go badsite.net"))
    }

    @Test
    fun ownExceptionsWin() {
        assertNull(AdultContentDetector(allowWords = listOf("porn")).check("porn"))
    }

    @Test
    fun blankOwnWordsAreIgnored() {
        assertNull(AdultContentDetector(extraWords = listOf(" ", "")).check("погода"))
    }

    @Test
    fun brokenOrHugeInputDoesNotThrow() {
        assertNull(d.check("%E0%A4%A"))
        assertNull(d.check("%"))
        assertNull(d.check("%zz%"))
        assertNull(d.check("a".repeat(100_000)))
    }
}
