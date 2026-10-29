package app.opora.core

import app.opora.core.model.TargetRule

/** Ленты, которые предлагаем отслеживать, разделы внутри приложений и браузеры. */
object KnownTargets {
    const val YOUTUBE = "com.google.android.youtube"
    const val SHORTS = "$YOUTUBE#shorts"
    const val INSTAGRAM = "com.instagram.android"
    const val REELS = "$INSTAGRAM#reels"

    private val PRESETS = listOf(
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        INSTAGRAM to "Instagram",
        REELS to "Instagram Reels",
        SHORTS to "YouTube Shorts",
        "com.vkontakte.android" to "ВКонтакте",
        "video.like" to "Likee",
        "com.twitter.android" to "X (Twitter)",
        "com.reddit.frontpage" to "Reddit",
        "com.pinterest" to "Pinterest",
        "com.facebook.katana" to "Facebook",
        "com.snapchat.android" to "Snapchat",
    )

    /**
     * Окончания id элементов экрана, по которым узнаём раздел. Зависят от версий YouTube и
     * Instagram: после их обновлений могут перестать совпадать.
     */
    private val SECTION_IDS = mapOf(
        YOUTUBE to (SHORTS to listOf("reel_player_page_container", "reel_recycler", "reel_watch_player")),
        INSTAGRAM to (REELS to listOf("clips_viewer_view_pager", "clips_viewer_container")),
    )

    private val BROWSERS = setOf(
        "com.android.chrome", "com.chrome.beta", "com.yandex.browser", "ru.yandex.searchplugin",
        "com.google.android.googlequicksearchbox", "org.mozilla.firefox", "org.mozilla.firefox_beta",
        "org.mozilla.focus", "com.sec.android.app.sbrowser", "com.opera.browser", "com.opera.mini.native",
        "com.opera.gx", "com.microsoft.emmx", "com.brave.browser", "com.duckduckgo.mobile.android",
        "com.kiwibrowser.browser", "com.vivaldi.browser", "com.mi.globalbrowser", "com.huawei.browser",
        "com.heytap.browser", "com.UCMobile.intl",
    )

    fun packageOf(targetId: String): String = targetId.substringBefore('#')

    fun isSection(targetId: String): Boolean = '#' in targetId

    /** Правила по умолчанию для установленных приложений. */
    fun defaultRules(installed: Set<String>): List<TargetRule> =
        PRESETS.filter { packageOf(it.first) in installed }
            .map { (id, label) -> TargetRule(id = id, label = label) }

    /** Разделы, которые можно добавить для установленных приложений. */
    fun sections(installed: Set<String>): List<TargetRule> =
        defaultRules(installed).filter { isSection(it.id) }

    /** Есть ли у пакета распознаваемые разделы. */
    fun hasSections(pkg: String): Boolean = pkg in SECTION_IDS

    fun section(pkg: String, viewIds: Collection<String>): String? {
        val (sectionId, endings) = SECTION_IDS[pkg] ?: return null
        return if (viewIds.any { id -> endings.any { id.endsWith(it) } }) sectionId else null
    }

    fun isBrowser(pkg: String): Boolean = pkg in BROWSERS || "browser" in pkg.lowercase()
}
