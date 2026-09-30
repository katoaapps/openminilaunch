package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Device-specific private-container gateway. It is intentionally excluded from portable backup. */
internal class PrivateContainerPreferenceStore(private val prefs: SharedPreferences) {
    var gatewaySelectionKey by mutableStateOf(
        prefs.getString(GATEWAY_SELECTION_KEY, null)?.takeIf(String::isNotBlank),
    )
        private set

    fun setGateway(selectionKey: String) {
        gatewaySelectionKey = selectionKey
        prefs.edit().putString(GATEWAY_SELECTION_KEY, selectionKey).apply()
    }

    fun clearGateway() {
        gatewaySelectionKey = null
        prefs.edit().remove(GATEWAY_SELECTION_KEY).apply()
    }

    private companion object {
        const val GATEWAY_SELECTION_KEY = "private_container_gateway_selection"
    }
}
