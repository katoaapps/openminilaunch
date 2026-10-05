package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.features.minkpad.MinkPadPreferences
import org.json.JSONObject

/** Portable controller preferences only. Addresses, host names and input never enter backups. */
internal object MinkPadBackupCodec {
    fun encode(value: MinkPadPreferences) = JSONObject().apply {
        put("keepAwake", value.keepAwake)
        put("pointerSensitivity", value.pointerSensitivity); put("scrollSensitivity", value.scrollSensitivity)
        put("naturalScroll", value.naturalScroll); put("tapToClick", value.tapToClick); put("haptics", value.haptics)
    }
    fun decode(json: JSONObject) = MinkPadPreferences(
        // Old mode and experimental touch-delivery values are intentionally ignored.
        keepAwake = json.optBoolean("keepAwake", true),
        pointerSensitivity = sensitivity(json, "pointerSensitivity"),
        scrollSensitivity = sensitivity(json, "scrollSensitivity"),
        naturalScroll = json.optBoolean("naturalScroll", true),
        tapToClick = json.optBoolean("tapToClick", true),
        haptics = json.optBoolean("haptics", true),
    )
    private fun sensitivity(json: JSONObject, key: String): Float = json.optDouble(key, 1.0)
        .takeIf { it.isFinite() }?.toFloat()?.coerceIn(.25f, 3f) ?: 1f
}
