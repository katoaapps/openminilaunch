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

/** The small slice of Classic pager state that affects Home-level input and overlays. */
internal data class ClassicLauncherUiState(
    val page: ClassicLauncherPage? = null,
    val conversationReplyActive: Boolean = false,
) {
    val conversationsActive: Boolean
        get() = page == ClassicLauncherPage.Conversations

    val widgetsActive: Boolean
        get() = page == ClassicLauncherPage.Widgets

    val verticalGesturesEnabled: Boolean
        get() = page?.supportsLauncherVerticalGestures == true

    private val replyEditorOwnsInput: Boolean
        get() = conversationsActive && conversationReplyActive

    val homeKeyboardInputAllowed: Boolean
        get() = !widgetsActive && !replyEditorOwnsInput

    fun shouldShowHomeMagicBox(magicExpanded: Boolean): Boolean =
        !conversationsActive || (magicExpanded && !replyEditorOwnsInput)
}

internal fun classicLauncherPages(slots: List<AppGridSlot>): List<ClassicLauncherPage> = buildList {
    add(ClassicLauncherPage.Conversations)
    add(ClassicLauncherPage.MinkSpace)
    slots.forEachIndexed { index, slot ->
        add(ClassicLauncherPage.Folder(slot = slot, position = index + 1))
    }
    add(ClassicLauncherPage.QuickApps)
    add(ClassicLauncherPage.Widgets)
}

internal fun classicHomePageIndex(
    pages: List<ClassicLauncherPage>,
    homeSlot: AppGridSlot?,
): Int = homeSlot
    ?.let { slot -> pages.indexOfFirst { it is ClassicLauncherPage.Folder && it.slot == slot } }
    ?.takeIf { it >= 0 }
    ?: pages.indexOf(ClassicLauncherPage.QuickApps).coerceAtLeast(0)

/** Conversation replies own typing; every other completed page change returns it to Magic Box. */
internal fun shouldRearmMagicBox(
    previousPage: ClassicLauncherPage?,
    currentPage: ClassicLauncherPage?,
): Boolean = previousPage != null &&
    currentPage != null &&
    previousPage != currentPage &&
    currentPage != ClassicLauncherPage.Conversations

/** Scrollable embedded tools retain vertical motion; app pages retain launcher gestures. */
internal val ClassicLauncherPage.supportsLauncherVerticalGestures: Boolean
    get() = this == ClassicLauncherPage.QuickApps ||
        this is ClassicLauncherPage.Folder
