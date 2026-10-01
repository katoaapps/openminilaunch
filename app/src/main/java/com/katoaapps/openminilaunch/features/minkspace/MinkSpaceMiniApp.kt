package com.katoaapps.openminilaunch.features.minkspace

/** Stable mini-app identities used by settings, persistence, and the eventual MinkSpace host. */
internal enum class MinkSpaceMiniApp(
    val stableId: String,
    val readyForHome: Boolean,
    val alwaysEnabled: Boolean = false,
) {
    TODO("todo", readyForHome = true, alwaysEnabled = true),
    CALCULATOR("calculator", readyForHome = true),
    MEDIA("media", readyForHome = true),
    RECORDER("recorder", readyForHome = false),
    MUSIC("music", readyForHome = false),
    ;

    companion object {
        val defaultOrder: List<MinkSpaceMiniApp> = entries

        fun fromStableId(stableId: String): MinkSpaceMiniApp? =
            entries.firstOrNull { it.stableId == stableId }
    }
}
