package com.katoaapps.minkpad.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MinkPadHostSwitchPolicyTest {
    @Test fun activeHostMustReleaseBeforeAnotherHostConnects() {
        assertEquals(
            MinkPadHostSwitchPlan.DISCONNECT_THEN_CONNECT,
            minkPadHostSwitchPlan(MinkPadStatus.CONNECTED, "host-a", "host-b"),
        )
    }

    @Test fun repeatedSelectionOfTheActiveOrConnectingHostIsANoOp() {
        assertEquals(
            MinkPadHostSwitchPlan.NO_OP,
            minkPadHostSwitchPlan(MinkPadStatus.CONNECTED, "host-a", "host-a"),
        )
        assertEquals(
            MinkPadHostSwitchPlan.NO_OP,
            minkPadHostSwitchPlan(MinkPadStatus.CONNECTING, "host-a", "host-a"),
        )
    }

    @Test fun newSelectionReplacesAnUnfinishedConnection() {
        assertEquals(
            MinkPadHostSwitchPlan.REPLACE_CONNECTING_HOST,
            minkPadHostSwitchPlan(MinkPadStatus.CONNECTING, "offline", "available"),
        )
    }

    @Test fun selectionDuringDisconnectIsQueued() {
        assertEquals(
            MinkPadHostSwitchPlan.QUEUE_AFTER_DISCONNECT,
            minkPadHostSwitchPlan(MinkPadStatus.DISCONNECTING, "host-a", "host-b"),
        )
    }

    @Test fun idleSavedHostConnectsDirectly() {
        assertEquals(
            MinkPadHostSwitchPlan.CONNECT_NOW,
            minkPadHostSwitchPlan(MinkPadStatus.READY, null, "host-a"),
        )
    }
}
