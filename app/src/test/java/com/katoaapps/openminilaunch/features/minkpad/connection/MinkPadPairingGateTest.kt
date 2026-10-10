package com.katoaapps.openminilaunch.features.minkpad.connection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinkPadPairingGateTest {
    @Test
    fun pairingWindowBlocksHostsUntilBondingChoosesTarget() {
        val gate = MinkPadPairingGate()

        gate.begin()

        assertTrue(gate.blocks("old-host"))
        assertTrue(gate.blocks("new-host"))

        gate.admit("new-host")

        assertTrue(gate.blocks("old-host"))
        assertFalse(gate.blocks("new-host"))
    }

    @Test
    fun finishingPairingRestoresNormalHostConnections() {
        val gate = MinkPadPairingGate()
        gate.begin()
        gate.admit("new-host")

        gate.finish()

        assertFalse(gate.active)
        assertNull(gate.targetAddress)
        assertFalse(gate.blocks("old-host"))
        assertFalse(gate.blocks("new-host"))
    }
}
