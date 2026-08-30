package com.katoaapps.openminilaunch.ui.updates

import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubUpdateDialogTest {
    @Test
    fun `preview version advances patch number`() {
        assertEquals("1.3.7", previewUpdateVersion("1.3.6"))
    }

    @Test
    fun `preview version ignores build labels`() {
        assertEquals("1.3.7", previewUpdateVersion("1.3.6-beta+5"))
    }

    @Test
    fun `preview version leaves an unknown format unchanged`() {
        assertEquals("development", previewUpdateVersion("development"))
    }
}
