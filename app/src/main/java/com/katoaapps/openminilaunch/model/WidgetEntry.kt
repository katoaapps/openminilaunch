package com.katoaapps.openminilaunch.model

internal const val MAX_WIDGET_ENTRIES = 64

/** Publicly identifiable widget-provider data that is safe to keep in a portable backup. */
internal data class WidgetProviderIdentity(
    val packageName: String,
    val className: String,
    val appLabel: String,
    val widgetLabel: String,
)

/** A portable request to configure a new local instance of a widget. */
internal data class WidgetRestoreRecipe(
    val entryId: String,
    val provider: WidgetProviderIdentity,
    val gridSize: WidgetGridSize,
)

/** One ordered row on the widget page. Android bindings remain local to this installation. */
internal sealed interface WidgetEntry {
    val entryId: String
    val gridSize: WidgetGridSize?

    data class Bound(
        override val entryId: String,
        val appWidgetId: Int,
        override val gridSize: WidgetGridSize?,
        val provider: WidgetProviderIdentity?,
    ) : WidgetEntry

    data class RestorePlaceholder(
        val recipe: WidgetRestoreRecipe,
    ) : WidgetEntry {
        override val entryId: String = recipe.entryId
        override val gridSize: WidgetGridSize = recipe.gridSize
    }
}
