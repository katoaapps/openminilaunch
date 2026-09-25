package com.katoaapps.openminilaunch.features.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileQrGeneratorTest {
    @Test fun shortUtf8ProfileUsesNormalDensity() {
        val code = ProfileQrGenerator.describe("BEGIN:VCARD\r\nFN:张伟\r\nEND:VCARD\r\n")

        assertEquals(ProfileQrDensity.NORMAL, code.density)
        assertTrue(code.modules <= 77)
    }

    @Test fun densityGuardEventuallyWarnsAndBlocks() {
        val descriptions = (100..3_000 step 100).mapNotNull { length ->
            runCatching { ProfileQrGenerator.describe("x".repeat(length)) }.getOrNull()
        }

        assertTrue(descriptions.any { it.density == ProfileQrDensity.DENSE })
        assertTrue(descriptions.any { it.density == ProfileQrDensity.TOO_DENSE })
    }
}
