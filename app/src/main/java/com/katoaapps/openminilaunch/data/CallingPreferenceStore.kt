package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Launcher-wide defaults for phone calls. Per-person routes may override only the app. */
internal class CallingPreferenceStore(private val prefs: SharedPreferences) {
    var callAutomatically by mutableStateOf(
        prefs.getBoolean(CALL_AUTOMATICALLY_KEY, false),
    )
        private set

    /** A null package means the system calling app. */
    var preferredCallingPackage by mutableStateOf(
        prefs.getString(PREFERRED_CALLING_PACKAGE_KEY, null),
    )
        private set

    fun updateCallAutomatically(enabled: Boolean) {
        callAutomatically = enabled
        prefs.edit().putBoolean(CALL_AUTOMATICALLY_KEY, enabled).apply()
    }

    fun setPreferredCallingApp(packageName: String?) {
        preferredCallingPackage = packageName
        prefs.edit().apply {
            packageName?.let { putString(PREFERRED_CALLING_PACKAGE_KEY, it) }
                ?: remove(PREFERRED_CALLING_PACKAGE_KEY)
        }.apply()
    }

    private companion object {
        const val CALL_AUTOMATICALLY_KEY = "call_automatically"
        const val PREFERRED_CALLING_PACKAGE_KEY = "preferred_calling_package"
    }
}
