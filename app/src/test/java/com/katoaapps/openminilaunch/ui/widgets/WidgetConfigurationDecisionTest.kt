package com.katoaapps.openminilaunch.ui.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetConfigurationDecisionTest {
    @Test fun confirmedResultCompletesAvailableWidget() {
        assertEquals(
            WidgetConfigurationDecision.COMPLETE,
            widgetConfigurationDecision(providerConfirmed = true, providerAvailable = true),
        )
    }

    @Test fun canceledResultAsksBeforeDiscardingBoundWidget() {
        assertEquals(
            WidgetConfigurationDecision.CONFIRM,
            widgetConfigurationDecision(providerConfirmed = false, providerAvailable = true),
        )
    }

    @Test fun unavailableProviderCannotBeKept() {
        assertEquals(
            WidgetConfigurationDecision.FAIL,
            widgetConfigurationDecision(providerConfirmed = false, providerAvailable = false),
        )
        assertEquals(
            WidgetConfigurationDecision.FAIL,
            widgetConfigurationDecision(providerConfirmed = true, providerAvailable = false),
        )
    }
}
