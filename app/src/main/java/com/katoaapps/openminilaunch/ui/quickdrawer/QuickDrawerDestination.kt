package com.katoaapps.openminilaunch.ui.quickdrawer

internal enum class QuickDrawerDestination {
    MINKPAD_LITE,
    ALL_APPS,
    PRIVATE_SPACE,
    WORK_SPACE,
}

internal fun quickDrawerDestinations(
    showMinkPad: Boolean,
    showPrivateSpace: Boolean,
    privateSpaceAvailable: Boolean,
    showWorkSpace: Boolean,
    workSpaceAvailable: Boolean,
): List<QuickDrawerDestination> = buildList {
    if (showMinkPad) add(QuickDrawerDestination.MINKPAD_LITE)
    add(QuickDrawerDestination.ALL_APPS)
    if (showPrivateSpace && privateSpaceAvailable) add(QuickDrawerDestination.PRIVATE_SPACE)
    if (showWorkSpace && workSpaceAvailable) add(QuickDrawerDestination.WORK_SPACE)
}
