package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.files.*

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSearchRequestTrackerTest {
    @Test
    fun emptyResultFromPreviousQueryCannotReplaceNextKeystrokeResults() {
        val tracker = FileSearchRequestTracker()
        val emptyResultRequest = tracker.begin("invoic")
        val nextKeystrokeRequest = tracker.begin("invoice")

        assertFalse(tracker.isCurrent(emptyResultRequest))
        assertTrue(tracker.isCurrent(nextKeystrokeRequest))
    }

    @Test
    fun clearingMagicBoxInvalidatesRunningSearch() {
        val tracker = FileSearchRequestTracker()
        val runningRequest = tracker.begin("invoice")

        tracker.invalidate()

        assertFalse(tracker.isCurrent(runningRequest))
    }
}
