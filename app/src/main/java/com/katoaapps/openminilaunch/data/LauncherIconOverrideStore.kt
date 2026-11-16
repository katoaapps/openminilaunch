package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import com.katoaapps.openminilaunch.model.LauncherIconOverride
import org.json.JSONObject

/** Persists per-target icon choices separately from the launcher's global icon style. */
internal class LauncherIconOverrideStore(private val prefs: SharedPreferences) {
    val overrides = mutableStateMapOf<String, LauncherIconOverride>()

    init {
        overrides.putAll(load())
    }

    fun set(selectionKey: String, override: LauncherIconOverride?) {
        if (selectionKey.isBlank()) return
        if (override == null) overrides.remove(selectionKey) else overrides[selectionKey] = override
        save()
    }

    fun replace(restored: Map<String, LauncherIconOverride>) {
        overrides.clear()
        overrides.putAll(restored.filter { (key, value) -> key.isNotBlank() && value.valid() })
        save()
    }

    fun prune(shouldRemove: (String) -> Boolean): Boolean {
        val removed = overrides.keys.removeAll(shouldRemove)
        if (removed) save()
        return removed
    }

    private fun load(): Map<String, LauncherIconOverride> = runCatching {
        val root = JSONObject(prefs.getString(PREF_KEY, null) ?: return emptyMap())
        buildMap {
            val keys = root.keys()
            while (keys.hasNext() && size < MAX_OVERRIDES) {
                val selectionKey = keys.next()
                val saved = root.optJSONObject(selectionKey) ?: continue
                val override = LauncherIconOverride(
                    iconPackPackage = saved.optString("iconPackPackage"),
                    drawableName = saved.optString("drawableName"),
                )
                if (selectionKey.isNotBlank() && override.valid()) put(selectionKey, override)
            }
        }
    }.getOrDefault(emptyMap())

    private fun save() {
        val root = JSONObject()
        overrides.entries.take(MAX_OVERRIDES).forEach { (selectionKey, override) ->
            root.put(
                selectionKey,
                JSONObject()
                    .put("iconPackPackage", override.iconPackPackage)
                    .put("drawableName", override.drawableName),
            )
        }
        prefs.edit().putString(PREF_KEY, root.toString()).apply()
    }

    private fun LauncherIconOverride.valid(): Boolean =
        iconPackPackage.isNotBlank() && drawableName.isNotBlank()

    private companion object {
        const val PREF_KEY = "launcher_icon_overrides"
        const val MAX_OVERRIDES = 1_000
    }
}
