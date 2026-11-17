package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.HomeLayoutStyle
import com.katoaapps.openminilaunch.model.appGridSlotFromStoredName
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.model.parseHomeLayoutStyle

/** Persists which Home structure is active while both structures share the same content stores. */
internal class HomeLayoutPreferenceStore(private val prefs: SharedPreferences) {
    var style by mutableStateOf(parseHomeLayoutStyle(prefs.getString(HOME_LAYOUT_STYLE_KEY, null)))
        private set
    var classicHomeSlot by mutableStateOf(
        prefs.getString(CLASSIC_HOME_SLOT_KEY, null)
            ?.let(::appGridSlotFromStoredName)
            ?.takeIf(configurableAppGridSlots::contains),
    )
        private set

    fun update(style: HomeLayoutStyle) {
        if (this.style == style) return
        this.style = style
        prefs.edit { putString(HOME_LAYOUT_STYLE_KEY, style.name) }
    }

    fun updateClassicHomeSlot(slot: AppGridSlot?) {
        val normalized = slot?.takeIf(configurableAppGridSlots::contains)
        if (classicHomeSlot == normalized) return
        classicHomeSlot = normalized
        prefs.edit {
            if (normalized == null) remove(CLASSIC_HOME_SLOT_KEY)
            else putString(CLASSIC_HOME_SLOT_KEY, normalized.name)
        }
    }

    private companion object {
        const val HOME_LAYOUT_STYLE_KEY = "home_layout_style"
        const val CLASSIC_HOME_SLOT_KEY = "classic_home_slot"
    }
}
