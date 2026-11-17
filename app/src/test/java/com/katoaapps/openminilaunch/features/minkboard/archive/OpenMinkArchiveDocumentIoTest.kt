package com.katoaapps.openminilaunch.features.minkboard.archive

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMinkArchiveDocumentIoTest {
    @Test
    fun boundedReadReturnsInputWithinLimit() {
        val input = ByteArray(128) { it.toByte() }

        val result = OpenMinkArchiveDocumentIo.readBounded(
            ByteArrayInputStream(input),
            maxBytes = input.size,
        )

        assertArrayEquals(input, result)
    }

    @Test
    fun boundedReadRejectsOversizedInput() {
        val result = runCatching {
            OpenMinkArchiveDocumentIo.readBounded(
                ByteArrayInputStream(ByteArray(129)),
                maxBytes = 128,
            )
        }

        assertTrue(result.isFailure)
    }
}
