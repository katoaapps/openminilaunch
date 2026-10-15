package com.katoaapps.openminilaunch.features.minkspace

/** Stable mini-app identities used by settings, persistence, and the eventual MinkSpace host. */
internal enum class MinkSpaceMiniApp(
    val stableId: String,
    val readyForHome: Boolean,
) {
    TODO("todo", readyForHome = true),
    CALCULATOR("calculator", readyForHome = true),
    MEDIA("media", readyForHome = true),
    RECORDER("recorder", readyForHome = false),
    MUSIC("music", readyForHome = true),
    WIDGET("widget", readyForHome = true),
    ;

    companion object {
        val defaultOrder: List<MinkSpaceMiniApp> = entries
        val defaultEnabled: Set<MinkSpaceMiniApp> = setOf(TODO)

        fun fromStableId(stableId: String): MinkSpaceMiniApp? =
            entries.firstOrNull { it.stableId == stableId }
    }
}
