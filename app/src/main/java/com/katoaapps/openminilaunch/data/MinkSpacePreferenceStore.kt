package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp

/** Owns MinkSpace ordering and visibility independently of its UI host. */
internal class MinkSpacePreferenceStore(private val prefs: SharedPreferences) {
    var order by mutableStateOf(loadOrder())
        private set

    var enabledMiniApps by mutableStateOf(loadEnabledMiniApps())
        private set

    fun setEnabled(miniApp: MinkSpaceMiniApp, enabled: Boolean) {
        enabledMiniApps = if (enabled) {
            enabledMiniApps + miniApp
        } else {
            enabledMiniApps - miniApp
        }
        saveEnabledMiniApps()
    }

    fun updateOrder(visibleOrder: List<MinkSpaceMiniApp>) {
        val updatedOrder = mergeVisibleMinkSpaceMiniAppOrder(order, visibleOrder)
        if (updatedOrder == order) return

        order = updatedOrder
        saveOrder()
    }

    fun replaceFromBackup(
        restoredOrder: List<MinkSpaceMiniApp>,
        restoredEnabledMiniApps: Set<MinkSpaceMiniApp>,
    ) {
        order = mergeVisibleMinkSpaceMiniAppOrder(order, restoredOrder)
        enabledMiniApps = restoredEnabledMiniApps
            .filterTo(mutableSetOf(), MinkSpaceMiniApp::readyForHome)

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

    private fun loadEnabledMiniApps(): Set<MinkSpaceMiniApp> =
        enabledMinkSpaceMiniAppsFromStableIds(prefs.getStringSet(KEY_ENABLED, null))

    private fun saveEnabledMiniApps() {
        prefs.edit {
            putStringSet(KEY_ENABLED, enabledMiniApps.mapTo(mutableSetOf()) { it.stableId })
        }
    }

    private fun saveOrder() {
        prefs.edit { putString(KEY_ORDER, order.joinToString(",") { it.stableId }) }
    }

    private companion object {
        const val KEY_ORDER = "mink_space_mini_app_order"
        const val KEY_ENABLED = "mink_space_enabled_mini_apps"
    }
}

internal fun mergeVisibleMinkSpaceMiniAppOrder(
    currentOrder: List<MinkSpaceMiniApp>,
    requestedVisibleOrder: List<MinkSpaceMiniApp>,
): List<MinkSpaceMiniApp> {
    val completeCurrentOrder = currentOrder.distinct() +
        MinkSpaceMiniApp.entries.filterNot(currentOrder::contains)
    val requestedReadyOrder = requestedVisibleOrder
        .filter(MinkSpaceMiniApp::readyForHome)
        .distinct()
    val completeReadyOrder = requestedReadyOrder + MinkSpaceMiniApp.entries.filter { miniApp ->
        miniApp.readyForHome && miniApp !in requestedReadyOrder
    }
    val readyIterator = completeReadyOrder.iterator()
    return completeCurrentOrder.map { miniApp ->
        if (miniApp.readyForHome) readyIterator.next() else miniApp
    }
}

internal fun enabledMinkSpaceMiniAppsFromStableIds(
    savedStableIds: Set<String>?,
): Set<MinkSpaceMiniApp> = savedStableIds
    ?.mapNotNull(MinkSpaceMiniApp::fromStableId)
    ?.filterTo(mutableSetOf(), MinkSpaceMiniApp::readyForHome)
    ?: MinkSpaceMiniApp.defaultEnabled
