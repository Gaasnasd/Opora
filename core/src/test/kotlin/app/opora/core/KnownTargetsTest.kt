package app.opora.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KnownTargetsTest {
    @Test
    fun defaultsOnlyForInstalledAndYoutubeItselfIsFree() {
        val ids = KnownTargets.defaultRules(setOf(KnownTargets.YOUTUBE, KnownTargets.INSTAGRAM, "com.example.notes"))
            .map { it.id }.toSet()
        assertEquals(setOf(KnownTargets.INSTAGRAM, KnownTargets.REELS, KnownTargets.SHORTS), ids)
    }

    @Test
    fun telegramIsNotWatchedByDefault() {
        assertTrue(KnownTargets.defaultRules(setOf("org.telegram.messenger")).isEmpty())
    }

    @Test
    fun sectionsAndPackages() {
        assertEquals(KnownTargets.YOUTUBE, KnownTargets.packageOf(KnownTargets.SHORTS))
        assertEquals("com.zhiliaoapp.musically", KnownTargets.packageOf("com.zhiliaoapp.musically"))
        assertTrue(KnownTargets.isSection(KnownTargets.REELS))
        assertFalse(KnownTargets.isSection(KnownTargets.INSTAGRAM))
    }

    @Test
    fun shortsAreFoundByViewId() {
        assertEquals(
            KnownTargets.SHORTS,
            KnownTargets.section(KnownTargets.YOUTUBE, listOf("com.google.android.youtube:id/reel_recycler")),
        )
        assertNull(
            KnownTargets.section(KnownTargets.YOUTUBE, listOf("com.google.android.youtube:id/results", "x:id/reel_shelf")),
        )
    }

    @Test
    fun reelsAreFoundByViewId() {
        assertEquals(
            KnownTargets.REELS,
            KnownTargets.section(KnownTargets.INSTAGRAM, listOf("com.instagram.android:id/clips_viewer_view_pager")),
        )
        assertNull(KnownTargets.section("com.zhiliaoapp.musically", listOf("x:id/reel_recycler")))
    }

    @Test
    fun browsers() {
        assertTrue(KnownTargets.isBrowser("com.android.chrome"))
        assertTrue(KnownTargets.isBrowser("com.yandex.browser"))
        assertTrue(KnownTargets.isBrowser("com.example.mybrowser"))
        assertFalse(KnownTargets.isBrowser("com.zhiliaoapp.musically"))
    }
}

class KnownSectionsTest {
    @Test
    fun onlyYoutubeAndInstagramHaveSections() {
        assertTrue(KnownTargets.hasSections(KnownTargets.YOUTUBE))
        assertTrue(KnownTargets.hasSections(KnownTargets.INSTAGRAM))
        assertFalse(KnownTargets.hasSections("com.zhiliaoapp.musically"))
    }

    @Test
    fun sectionsForInstalledParents() {
        assertEquals(
            listOf(KnownTargets.SHORTS),
            KnownTargets.sections(setOf(KnownTargets.YOUTUBE, "com.zhiliaoapp.musically")).map { it.id },
        )
    }
}
