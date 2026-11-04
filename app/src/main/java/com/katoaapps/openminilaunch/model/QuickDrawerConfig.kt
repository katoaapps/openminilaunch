package com.katoaapps.openminilaunch.model

/** Persisted configuration for the fixed, system-owned Quick Drawer surface. */
data class QuickDrawerConfig(
    val orderedTargetKeys: List<String>,
    val showMinkPad: Boolean,
    val showPrivateSpace: Boolean,
    val showWorkProfile: Boolean,
    val colorArgb: Int?,
)
