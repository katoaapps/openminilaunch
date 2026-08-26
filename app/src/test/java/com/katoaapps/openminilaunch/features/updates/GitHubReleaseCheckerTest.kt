package com.katoaapps.openminilaunch.features.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseCheckerTest {
    @Test
    fun parsesReleaseTag() {
        assertEquals("v1.2.3", releaseTagFromJson("""{"tag_name":"v1.2.3"}"""))
    }

    @Test
    fun rejectsMissingOrMalformedReleaseTag() {
        assertNull(releaseTagFromJson("{}"))
        assertNull(releaseTagFromJson("not json"))
    }

    @Test
    fun comparesSemanticVersions() {
        assertTrue(isNewerRelease("1.2.2", "v1.2.3"))
        assertTrue(isNewerRelease("1.9.9", "v1.10.0"))
        assertFalse(isNewerRelease("1.2.2", "v1.2.2"))
        assertFalse(isNewerRelease("1.2.2", "v1.2.1"))
        assertFalse(isNewerRelease("1.2.2", "unexpected"))
    }
}
