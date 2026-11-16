package com.katoaapps.openminilaunch.ui.launcher.home

import com.katoaapps.openminilaunch.model.AppGridSlot

/** Stable page identities used by the Classic Home pager and its navigation rail. */
internal sealed interface ClassicLauncherPage {
    val stableKey: String

    data object Conversations : ClassicLauncherPage {
        override val stableKey = "conversations"
    }

    data object QuickApps : ClassicLauncherPage {
        override val stableKey = "quick-apps"
    }

    data object Widgets : ClassicLauncherPage {
        override val stableKey = "widgets"
    }

    data class Folder(val slot: AppGridSlot, val position: Int) : ClassicLauncherPage {
        override val stableKey = "folder:${slot.name}"
    }

    data object MinkSpace : ClassicLauncherPage {
        override val stableKey = "mink-space"
    }
}

internal fun classicLauncherPages(slots: List<AppGridSlot>): List<ClassicLauncherPage> = buildList {
    add(ClassicLauncherPage.Conversations)
    add(ClassicLauncherPage.QuickApps)
    add(ClassicLauncherPage.Widgets)
    slots.forEachIndexed { index, slot ->
        add(ClassicLauncherPage.Folder(slot = slot, position = index + 1))
    }
    add(ClassicLauncherPage.MinkSpace)
}
