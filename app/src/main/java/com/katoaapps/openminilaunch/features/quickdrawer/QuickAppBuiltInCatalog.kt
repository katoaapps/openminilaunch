package com.katoaapps.openminilaunch.features.quickdrawer

import android.content.Context
import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.LauncherActionTarget

/** Launcher-owned actions that may be arranged beside apps and published shortcuts. */
internal enum class QuickAppBuiltIn(
    val selectionKey: String,
    @param:StringRes val labelRes: Int,
    val appGridSlot: AppGridSlot? = null,
) {
    NOTE(
        selectionKey = "internal:quick-app:note",
        labelRes = R.string.shortcut_note,
        appGridSlot = AppGridSlot.NOTE,
    ),
    CALENDAR(
        selectionKey = "internal:quick-app:calendar",
        labelRes = R.string.shortcut_calendar,
        appGridSlot = AppGridSlot.EVENT,
    ),
    WEATHER(
        selectionKey = "internal:quick-app:weather",
        labelRes = R.string.shortcut_weather,
        appGridSlot = AppGridSlot.WEATHER,
    ),
    TODOS(
        selectionKey = "internal:quick-app:todos",
        labelRes = R.string.open_todos,
    ),
    CALL(
        selectionKey = "internal:quick-app:call",
        labelRes = R.string.shortcut_call,
        appGridSlot = AppGridSlot.CALL,
    ),
    MESSAGES(
        selectionKey = "internal:quick-app:messages",
        labelRes = R.string.shortcut_messenger,
        appGridSlot = AppGridSlot.MESSAGE,
    ),
    FILES(
        selectionKey = "internal:quick-app:files",
        labelRes = R.string.shortcut_files,
        appGridSlot = AppGridSlot.FILES,
    ),
    ;

    companion object {
        val defaultOrder: List<QuickAppBuiltIn> = listOf(
            NOTE,
            CALENDAR,
            WEATHER,
            TODOS,
            CALL,
            MESSAGES,
            FILES,
        )

        fun fromSelectionKey(selectionKey: String): QuickAppBuiltIn? =
            entries.firstOrNull { it.selectionKey == selectionKey }
    }
}

internal class QuickAppBuiltInCatalog(private val context: Context) {
    fun targets(): List<LauncherActionTarget> = QuickAppBuiltIn.entries.map(::target)

    fun resolve(selectionKey: String): LauncherActionTarget? =
        QuickAppBuiltIn.fromSelectionKey(selectionKey)?.let(::target)

    private fun target(action: QuickAppBuiltIn) = LauncherActionTarget(
        label = context.getString(action.labelRes),
        packageName = context.packageName,
        selectionKey = action.selectionKey,
    )
}
