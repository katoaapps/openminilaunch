package com.katoaapps.openminilaunch.features.apps

/** A package-removal callback scoped to the Android user/profile that owned the app. */
internal data class LauncherPackageRemoval(
    val packageName: String,
    val userSerial: Long,
)
