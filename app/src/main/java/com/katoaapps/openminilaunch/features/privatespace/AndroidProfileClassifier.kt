package com.katoaapps.openminilaunch.features.privatespace

import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

/** Keeps Android 15 Private Space profiles out of ordinary launcher discovery. */
internal class AndroidProfileClassifier(
    private val launcherApps: LauncherApps?,
) {
    fun classify(user: UserHandle): AndroidProfileKind {
        if (user == Process.myUserHandle()) return AndroidProfileKind.PERSONAL
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return AndroidProfileKind.OTHER_VISIBLE
        }

        val userType = runCatching {
            launcherApps?.getLauncherUserInfo(user)?.userType
        }.getOrNull() ?: return AndroidProfileKind.UNKNOWN

        return if (userType == UserManager.USER_TYPE_PROFILE_PRIVATE) {
            AndroidProfileKind.PRIVATE
        } else {
            AndroidProfileKind.OTHER_VISIBLE
        }
    }
}

internal enum class AndroidProfileKind {
    PERSONAL,
    OTHER_VISIBLE,
    PRIVATE,
    UNKNOWN,
}
