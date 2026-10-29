package com.katoaapps.minkpad.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MinkPadManagedHostPolicyTest {
    @Test fun ordersRememberedHostsByMostRecentUse() {
        val hosts = listOf(
            ManagedHostCandidate("AA", "Laptop", 10L, "laptop"),
            ManagedHostCandidate("BB", "TV", 30L, "tv"),
            ManagedHostCandidate("CC", "Tablet", 20L, "tablet"),
        )

        assertEquals(listOf("tv", "tablet", "laptop"), orderManagedHosts(hosts))
    }

    @Test fun excludesAndroidBondsThatAreNotAppManaged() {
        val hosts = listOf(
            ManagedHostCandidate("AA", "Saved", 1L, "saved"),
            ManagedHostCandidate("BB", "Unrelated bond", 0L, "unrelated"),
        )

        assertEquals(listOf("saved"), orderManagedHosts(hosts))
    }

    @Test fun tiesAreDeterministic() {
        val hosts = listOf(
            ManagedHostCandidate("BB", "Host", 1L, "second"),
            ManagedHostCandidate("AA", "host", 1L, "first"),
        )

        assertEquals(listOf("first", "second"), orderManagedHosts(hosts))
    }

    @Test fun ignoresKeyLossForHostTheAppDoesNotManage() {
        assertEquals(
            MinkPadKeyLossPlan.IGNORE,
            minkPadKeyLossPlan(0L, "AA", activeAddress = "AA", pendingAddress = null),
        )
    }

    @Test fun suppressesBackgroundReconnectAfterManagedHostLosesItsKey() {
        assertEquals(
            MinkPadKeyLossPlan.SUPPRESS_RECONNECT,
            minkPadKeyLossPlan(1L, "AA", activeAddress = "BB", pendingAddress = null),
        )
    }

    @Test fun tearsDownAnActiveOrPendingAttemptAfterKeyLoss() {
        assertEquals(
            MinkPadKeyLossPlan.DISCONNECT_ACTIVE_ATTEMPT,
            minkPadKeyLossPlan(1L, "AA", activeAddress = null, pendingAddress = "AA"),
        )
    }
}
