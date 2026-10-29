package app.opora.core

import app.opora.core.model.GuardSettings
import app.opora.core.model.NightSchedule
import app.opora.core.model.TargetRule
import app.opora.core.model.contains
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuardPolicyTest {
    private val tiktok = TargetRule(id = "tiktok", label = "TikTok")
    private val noon = 12 * 60
    private val midnight = 0

    private fun settings(vararg rules: TargetRule) = GuardSettings(targets = rules.toList())

    private fun decide(
        s: GuardSettings,
        id: String? = "tiktok",
        msToday: Long = 0,
        opens: Int = 0,
        minute: Int = noon,
        grant: Boolean = false,
    ) = GuardPolicy.decide(id, s, msToday, opens, minute, grant)

    @Test
    fun nightWrapsMidnight() {
        val n = NightSchedule()
        assertTrue(n.contains(23 * 60))
        assertTrue(n.contains(0))
        assertTrue(n.contains(6 * 60 + 59))
        assertFalse(n.contains(7 * 60))
        assertFalse(n.contains(12 * 60))
    }

    @Test
    fun nightWithinOneDay() {
        val n = NightSchedule(startMin = 60, endMin = 5 * 60)
        assertTrue(n.contains(60))
        assertTrue(n.contains(4 * 60 + 59))
        assertFalse(n.contains(5 * 60))
        assertFalse(n.contains(30))
    }

    @Test
    fun emptyOrDisabledNightCoversNothing() {
        assertFalse(NightSchedule(startMin = 600, endMin = 600).contains(600))
        assertFalse(NightSchedule(enabled = false).contains(0))
    }

    @Test
    fun guardOffAllowsEverything() {
        val s = settings(tiktok.copy(alwaysBlocked = true)).copy(enabled = false)
        assertEquals(Decision.Allow, decide(s))
    }

    @Test
    fun unknownOrMissingTargetIsAllowed() {
        assertEquals(Decision.Allow, decide(settings(tiktok), id = "com.other", grant = true))
        assertEquals(Decision.Allow, decide(settings(tiktok), id = null))
    }

    @Test
    fun alwaysBlockedComesFirst() {
        val s = settings(tiktok.copy(alwaysBlocked = true, dailyLimitMin = 15))
        assertEquals(Decision.Block(s.targets[0], BlockReason.ALWAYS), decide(s, minute = midnight, msToday = DAY))
    }

    @Test
    fun nightBeatsLimit() {
        val s = settings(tiktok.copy(dailyLimitMin = 15))
        assertEquals(BlockReason.NIGHT, assertIs<Decision.Block>(decide(s, minute = midnight, msToday = DAY)).reason)
    }

    @Test
    fun nightBlockCanBeOff() {
        val s = settings(tiktok.copy(nightBlocked = false, pause = false))
        assertEquals(Decision.Allow, decide(s, minute = midnight))
    }

    @Test
    fun limitReached() {
        val s = settings(tiktok.copy(dailyLimitMin = 15))
        assertEquals(BlockReason.LIMIT, assertIs<Decision.Block>(decide(s, msToday = 15 * MIN)).reason)
        assertIs<Decision.Pause>(decide(s, msToday = 15 * MIN - 1))
    }

    @Test
    fun grantDoesNotLiftLimitOrNight() {
        val s = settings(tiktok.copy(dailyLimitMin = 15))
        assertIs<Decision.Block>(decide(s, msToday = 15 * MIN, grant = true))
        assertIs<Decision.Block>(decide(s, minute = midnight, grant = true))
    }

    @Test
    fun pauseCarriesTodayNumbers() {
        val s = settings(tiktok)
        assertEquals(Decision.Pause(tiktok, opensToday = 4, msToday = 7 * MIN), decide(s, msToday = 7 * MIN, opens = 4))
    }

    @Test
    fun grantSkipsPause() {
        assertEquals(Decision.Allow, decide(settings(tiktok), grant = true))
        assertEquals(Decision.Allow, decide(settings(tiktok.copy(pause = false))))
    }

    @Test
    fun adultMatchRespectsSwitches() {
        val detector = AdultContentDetector()
        val texts = listOf(null, "погода", "pornhub.com")
        assertNotNull(GuardPolicy.adultMatch(GuardSettings(), detector, texts))
        assertNull(GuardPolicy.adultMatch(GuardSettings(adultFilter = false), detector, texts))
        assertNull(GuardPolicy.adultMatch(GuardSettings(enabled = false), detector, texts))
        assertNull(GuardPolicy.adultMatch(GuardSettings(), detector, listOf("погода")))
    }
}
