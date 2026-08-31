package com.katoaapps.openminilaunch.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.katoaapps.openminilaunch.model.WidgetGridSize

internal class WidgetStore(
    private val persistence: LauncherStorePersistence,
    private val maxWidgets: Int,
) {
    val ids = mutableStateListOf<Int>()
    val sizes = mutableStateMapOf<Int, WidgetGridSize>()

    fun restore(snapshot: LauncherStoreSnapshot) {
        ids.addAll(snapshot.widgetIds)
        sizes.putAll(snapshot.widgetSizes)
    }

    fun add(appWidgetId: Int, size: WidgetGridSize) {
        if (appWidgetId !in ids && ids.size < maxWidgets) {
            ids += appWidgetId
            sizes[appWidgetId] = size
            save()
        }
    }

    fun remove(appWidgetId: Int) {
        ids.remove(appWidgetId)
        sizes.remove(appWidgetId)
        save()
    }

    fun setSize(appWidgetId: Int, size: WidgetGridSize) {
        if (appWidgetId !in ids || size.columns !in 1..4 || size.rows !in 1..5) return
        sizes[appWidgetId] = size
        save()
    }

    fun move(appWidgetId: Int, direction: Int) {
        val from = ids.indexOf(appWidgetId)
        val target = from + direction
        if (from < 0 || target !in ids.indices) return
        ids.removeAt(from)
        ids.add(target, appWidgetId)
        save()
    }

    private fun save() {
        persistence.saveWidgets(ids, sizes)
    }
}
