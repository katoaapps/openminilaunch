package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.features.messaging.restoredAutomaticMessageSend

internal class MessagingPreferenceStore(private val prefs: SharedPreferences) {
    var sendMessagesAutomatically by mutableStateOf(
        restoredAutomaticMessageSend(
            saved = prefs.getBoolean(SEND_MESSAGES_AUTOMATICALLY_KEY, false)
                .takeIf { prefs.contains(SEND_MESSAGES_AUTOMATICALLY_KEY) },
            legacyMode = prefs.getString(LEGACY_MESSAGE_SEND_MODE_KEY, null),
        ),
    )
        private set
    // A null package represents System Messages. Integrated providers persist the exact installed
    // package so every draft intent can remain explicit.
    var preferredMessagingPackage by mutableStateOf(
        prefs.getString(PREFERRED_MESSAGING_PACKAGE_KEY, null),
    )
        private set
    var preferredAiPackage by mutableStateOf(prefs.getString(PREFERRED_AI_PACKAGE_KEY, null))
        private set
    var preferredWebPackage by mutableStateOf(prefs.getString(PREFERRED_WEB_PACKAGE_KEY, null))
        private set

    init {
        prefs.edit()
            .putBoolean(SEND_MESSAGES_AUTOMATICALLY_KEY, sendMessagesAutomatically)
            .remove(LEGACY_MESSAGE_SEND_MODE_KEY)
            .apply()
    }

    fun updateAutomaticSend(enabled: Boolean) {
        sendMessagesAutomatically = enabled
        prefs.edit().putBoolean(SEND_MESSAGES_AUTOMATICALLY_KEY, enabled).apply()
    }

    fun setPreferredMessagingApp(packageName: String?) {
        preferredMessagingPackage = packageName
        prefs.edit().apply {
            packageName?.let { putString(PREFERRED_MESSAGING_PACKAGE_KEY, it) }
                ?: remove(PREFERRED_MESSAGING_PACKAGE_KEY)
        }.apply()
    }

    fun setPreferredAiApp(packageName: String?) {
        preferredAiPackage = packageName
        prefs.edit().apply {
            packageName?.let { putString(PREFERRED_AI_PACKAGE_KEY, it) }
                ?: remove(PREFERRED_AI_PACKAGE_KEY)
        }.apply()
    }

    fun setPreferredWebApp(packageName: String?) {
        preferredWebPackage = packageName
        prefs.edit().apply {
            packageName?.let { putString(PREFERRED_WEB_PACKAGE_KEY, it) }
                ?: remove(PREFERRED_WEB_PACKAGE_KEY)
        }.apply()
    }

    private companion object {
        const val LEGACY_MESSAGE_SEND_MODE_KEY = "message_send_mode"
        const val PREFERRED_AI_PACKAGE_KEY = "preferred_ai_package"
        const val PREFERRED_MESSAGING_PACKAGE_KEY = "preferred_messaging_package"
        const val PREFERRED_WEB_PACKAGE_KEY = "preferred_web_package"
        const val SEND_MESSAGES_AUTOMATICALLY_KEY = "send_messages_automatically"
    }
}
