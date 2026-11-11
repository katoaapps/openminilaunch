package com.katoaapps.openminilaunch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.features.demo.DemoConversationState
import com.katoaapps.openminilaunch.features.demo.DemoFavoriteData
import com.katoaapps.openminilaunch.features.demo.DemoHomeData
import com.katoaapps.openminilaunch.features.demo.DemoHomeProfile
import com.katoaapps.openminilaunch.features.demo.DemoScenario
import com.katoaapps.openminilaunch.features.minkspace.todo.TodoStore
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.FavoriteContactId

/**
 * Owns persisted demo configuration and its temporary Home presentation.
 *
 * The old `demo_search_data_enabled` key intentionally remains unchanged so existing internal
 * demo installs migrate without another hidden activation. Real launcher data is never replaced;
 * disabling demo mode simply restores the saved todo list and normal appearance getters.
 */
internal class DemoModeStore(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val todoStore: TodoStore,
) {
    var profile by mutableStateOf(
        DemoHomeProfile.fromStoredName(prefs.getString(DEMO_HOME_PROFILE_KEY, null)),
    )
        private set
    var scenario by mutableStateOf(
        DemoScenario.fromStoredName(prefs.getString(DEMO_SCENARIO_KEY, null)),
    )
        private set
    var conversationState by mutableStateOf(
        DemoConversationState.fromStoredName(
            prefs.getString(DEMO_CONVERSATION_STATE_KEY, null),
        ),
    )
        private set
    var enabled by mutableStateOf(prefs.getBoolean(DEMO_MODE_ENABLED_KEY, false))
        private set
    var sessionStartedAt by mutableLongStateOf(System.currentTimeMillis())
        private set
    private var panelColorArgb by mutableIntStateOf(
        ContextCompat.getColor(context, profile.panelColorRes),
    )
    private var backgroundColorArgb by mutableStateOf<Int?>(
        ContextCompat.getColor(context, profile.backgroundColorRes),
    )
    private val favoriteAttentionColorOverrides = mutableStateMapOf<String, Int?>()

    fun applyIfEnabled() {
        if (enabled) applyScene()
    }

    fun toggle(): Boolean {
        if (enabled) disable() else enable()
        return enabled
    }

    fun enable() {
        if (enabled) return
        enabled = true
        restartSessionClock()
        applyScene()
        persistEnabled()
    }

    fun disable() {
        if (!enabled) return
        enabled = false
        favoriteAttentionColorOverrides.clear()
        todoStore.restoreSaved()
        persistEnabled()
    }

    fun resetScene() {
        if (!enabled) return
        favoriteAttentionColorOverrides.clear()
        restartSessionClock()
        applyScene()
    }

    fun selectScenario(newScenario: DemoScenario) {
        if (newScenario == scenario) return
        scenario = newScenario
        prefs.edit().putString(DEMO_SCENARIO_KEY, newScenario.name).apply()
        if (enabled) {
            favoriteAttentionColorOverrides.clear()
            restartSessionClock()
            applyScene()
        }
    }

    fun selectConversationState(newState: DemoConversationState) {
        if (newState == conversationState) return
        conversationState = newState
        prefs.edit().putString(DEMO_CONVERSATION_STATE_KEY, newState.name).apply()
        if (enabled) restartSessionClock()
    }

    fun selectProfile(newProfile: DemoHomeProfile) {
        if (newProfile == profile) return
        profile = newProfile
        prefs.edit().putString(DEMO_HOME_PROFILE_KEY, newProfile.name).apply()
        if (enabled) applyScene()
    }

    fun setPanelColor(argb: Int): Boolean {
        if (!enabled) return false
        panelColorArgb = argb or 0xFF000000.toInt()
        return true
    }

    fun setBackgroundColor(argb: Int?): Boolean {
        if (!enabled) return false
        backgroundColorArgb = argb?.or(0xFF000000.toInt())
        return true
    }

    fun effectivePanelColor(normalColor: Int): Int = if (enabled) panelColorArgb else normalColor

    fun effectiveBackgroundColor(normalColor: Int?): Int? =
        if (enabled) backgroundColorArgb else normalColor

    fun effectiveAppGridOrder(normalOrder: List<AppGridSlot>): List<AppGridSlot> =
        if (enabled) DemoHomeData.appGridOrder(profile) else normalOrder

    fun favoriteAttentionColor(id: FavoriteContactId): Int? =
        if (favoriteAttentionColorOverrides.containsKey(id.storageKey)) {
            favoriteAttentionColorOverrides[id.storageKey]
        } else {
            DemoFavoriteData.attentionColor(id)
        }

    fun updateFavoriteAttentionColor(id: FavoriteContactId, colorArgb: Int?) {
        if (!enabled) return
        favoriteAttentionColorOverrides[id.storageKey] = colorArgb?.or(0xFF000000.toInt())
    }

    private fun applyScene() {
        panelColorArgb = ContextCompat.getColor(context, profile.panelColorRes)
        backgroundColorArgb = ContextCompat.getColor(context, profile.backgroundColorRes)
        todoStore.showDemo(DemoHomeData.todos(profile, scenario))
    }

    private fun restartSessionClock() {
        sessionStartedAt = System.currentTimeMillis()
    }

    private fun persistEnabled() {
        prefs.edit().putBoolean(DEMO_MODE_ENABLED_KEY, enabled).apply()
    }

    private companion object {
        const val DEMO_HOME_PROFILE_KEY = "demo_home_profile"
        const val DEMO_SCENARIO_KEY = "demo_scenario"
        const val DEMO_CONVERSATION_STATE_KEY = "demo_conversation_state"
        const val DEMO_MODE_ENABLED_KEY = "demo_search_data_enabled"
    }
}
