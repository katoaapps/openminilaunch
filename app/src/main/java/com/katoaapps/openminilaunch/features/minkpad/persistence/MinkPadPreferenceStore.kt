package com.katoaapps.openminilaunch.features.minkpad.persistence

import android.content.Context
import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Controller preferences and remembered hosts only. Never receives text or input reports. */
internal class MinkPadPreferenceStore(context: Context) {
    private val prefs = context.getSharedPreferences("minkpad", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())
    val state = mutable.asStateFlow()
    private val mutablePlatforms = MutableStateFlow(readPlatforms())
    val hostPlatforms = mutablePlatforms.asStateFlow()

    private fun readPlatforms(): Map<String, MinkPadHostPlatform> = prefs.all.mapNotNull { (key, value) ->
        if (!key.startsWith("platform:")) null else {
            val platform = runCatching { MinkPadHostPlatform.valueOf(value as String) }.getOrNull()
            platform?.let { key.removePrefix("platform:") to it }
        }
    }.toMap()

    fun setHostPlatform(address: String, platform: MinkPadHostPlatform) {
        prefs.edit().putString("platform:$address", platform.name).apply()
        mutablePlatforms.value = readPlatforms()
    }

    private fun read() = MinkPadPreferences(
        keepAwake = prefs.getBoolean("awake", true),
        pointerSensitivity = prefs.getFloat("pointer", 1f).coerceIn(.25f, 3f),
        scrollSensitivity = prefs.getFloat("scroll", 1f).coerceIn(.25f, 3f),
        naturalScroll = prefs.getBoolean("natural", true),
        tapToClick = prefs.getBoolean("tap", true),
        haptics = prefs.getBoolean("haptics", true),
    )

    fun update(value: MinkPadPreferences) {
        val normalized = value.copy(pointerSensitivity = value.pointerSensitivity.coerceIn(.25f, 3f),
            scrollSensitivity = value.scrollSensitivity.coerceIn(.25f, 3f))
        prefs.edit().remove("mode").putBoolean("awake", normalized.keepAwake)
            .putFloat("pointer", normalized.pointerSensitivity).putFloat("scroll", normalized.scrollSensitivity)
            .putBoolean("natural", normalized.naturalScroll).putBoolean("tap", normalized.tapToClick)
            .putBoolean("haptics", normalized.haptics).remove("unbufferedTouch").apply()
        mutable.value = normalized
    }

    /** Remember every host the user connects. Android owns the actual Bluetooth pairings. */
    fun rememberHost(address: String) {
        prefs.edit().putLong("host:$address", System.currentTimeMillis()).apply()
    }
    fun lastUsed(address: String): Long = prefs.getLong("host:$address", 0)
    fun forgetHost(address: String) {
        prefs.edit().remove("host:$address").remove("platform:$address").apply()
        mutablePlatforms.value = readPlatforms()
    }

    fun originalBluetoothName(): String? = prefs.getString(ORIGINAL_BLUETOOTH_NAME, null)
        ?: prefs.getString(LEGACY_ORIGINAL_BLUETOOTH_NAME, null)

    fun rememberOriginalBluetoothName(originalBluetoothName: String) {
        prefs.edit()
            .putString(ORIGINAL_BLUETOOTH_NAME, originalBluetoothName)
            .remove(LEGACY_CLASSIC_LATENCY_EXPERIMENT)
            .remove(LEGACY_ORIGINAL_BLUETOOTH_NAME)
            .apply()
    }

    private companion object {
        const val LEGACY_CLASSIC_LATENCY_EXPERIMENT = "classic_latency_experiment"
        const val LEGACY_ORIGINAL_BLUETOOTH_NAME = "classic_latency_original_name"
        const val ORIGINAL_BLUETOOTH_NAME = "original_bluetooth_name"
    }
}
