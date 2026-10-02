package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp

/** Owns MinkSpace ordering and enablement invariants independently of its UI host. */
internal class MinkSpacePreferenceStore(private val prefs: SharedPreferences) {
    var order by mutableStateOf(loadOrder())
        private set

    var enabledMiniApps by mutableStateOf(loadEnabledMiniApps())
        private set

    fun setEnabled(miniApp: MinkSpaceMiniApp, enabled: Boolean) {
        if (miniApp.alwaysEnabled && !enabled) return
        enabledMiniApps = if (enabled) {
            enabledMiniApps + miniApp
        } else {
            enabledMiniApps - miniApp
        }.withRequiredMiniApps()
        saveEnabledMiniApps()
    }

    fun move(miniApp: MinkSpaceMiniApp, direction: Int) {
        val currentIndex = order.indexOf(miniApp)
        val targetIndex = (currentIndex + direction).coerceIn(order.indices)
        if (currentIndex == -1 || currentIndex == targetIndex) return

        order = order.toMutableList().apply {
            add(targetIndex, removeAt(currentIndex))
        }
        prefs.edit { putString(KEY_ORDER, order.joinToString(",") { it.stableId }) }
    }

    fun replaceFromBackup(
        restoredOrder: List<MinkSpaceMiniApp>,
        restoredEnabledMiniApps: Set<MinkSpaceMiniApp>,
    ) {
        val readyMiniApps = MinkSpaceMiniApp.entries.filter(MinkSpaceMiniApp::readyForHome)
        val normalizedReadyOrder = restoredOrder
            .filter(MinkSpaceMiniApp::readyForHome)
            .distinct() + readyMiniApps.filterNot(restoredOrder::contains)

        order = normalizedReadyOrder + MinkSpaceMiniApp.entries.filterNot(MinkSpaceMiniApp::readyForHome)
        enabledMiniApps = restoredEnabledMiniApps
            .filterTo(mutableSetOf(), MinkSpaceMiniApp::readyForHome)
            .withRequiredMiniApps()

        prefs.edit {
            putString(KEY_ORDER, order.joinToString(",") { it.stableId })
            putStringSet(KEY_ENABLED, enabledMiniApps.mapTo(mutableSetOf()) { it.stableId })
        }
    }

    private fun loadOrder(): List<MinkSpaceMiniApp> {
        val saved = prefs.getString(KEY_ORDER, null)
            ?.split(',')
            .orEmpty()
            .mapNotNull(MinkSpaceMiniApp::fromStableId)
            .distinct()
        return saved + MinkSpaceMiniApp.defaultOrder.filterNot(saved::contains)
    }

    private fun loadEnabledMiniApps(): Set<MinkSpaceMiniApp> = prefs
        .getStringSet(KEY_ENABLED, null)
        ?.mapNotNull(MinkSpaceMiniApp::fromStableId)
        ?.toSet()
        .orEmpty()
        .withRequiredMiniApps()

    private fun saveEnabledMiniApps() {
        prefs.edit {
            putStringSet(KEY_ENABLED, enabledMiniApps.mapTo(mutableSetOf()) { it.stableId })
        }
    }

    private fun Set<MinkSpaceMiniApp>.withRequiredMiniApps(): Set<MinkSpaceMiniApp> =
        this + MinkSpaceMiniApp.entries.filter(MinkSpaceMiniApp::alwaysEnabled)

    private companion object {
        const val KEY_ORDER = "mink_space_mini_app_order"
        const val KEY_ENABLED = "mink_space_enabled_mini_apps"
    }
}
