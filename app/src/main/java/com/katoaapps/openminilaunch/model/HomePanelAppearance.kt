package com.katoaapps.openminilaunch.model

/** Full range: from the selected pill color to a completely clear container. */
internal const val MAX_HOME_PANEL_TRANSPARENCY = 1f

/** Keeps persisted and imported appearance values safe for Compose sliders and color alpha. */
internal fun normalizeHomePanelTransparency(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, MAX_HOME_PANEL_TRANSPARENCY) else 0f
