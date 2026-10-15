package com.katoaapps.openminilaunch.ui.widgets

internal enum class WidgetConfigurationDecision {
    COMPLETE,
    CONFIRM,
    FAIL,
}

/**
 * Some widget providers save their configuration but return RESULT_CANCELED from a wrapper
 * activity. A bound provider is therefore ambiguous rather than automatically disposable.
 */
internal fun widgetConfigurationDecision(
    providerConfirmed: Boolean,
    providerAvailable: Boolean,
): WidgetConfigurationDecision = when {
    !providerAvailable -> WidgetConfigurationDecision.FAIL
    providerConfirmed -> WidgetConfigurationDecision.COMPLETE
    else -> WidgetConfigurationDecision.CONFIRM
}
