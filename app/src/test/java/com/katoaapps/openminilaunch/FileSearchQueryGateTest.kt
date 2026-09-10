package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.files.FileSearchQueryGate
import com.katoaapps.openminilaunch.features.files.FileSearchScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSearchQueryGateTest {
    private val localFiles = FileSearchScope(
        folderUris = listOf("content://documents/local"),
        includesMedia = true,
        usesDemoData = false,
    )

    @Test fun zeroResultsStopLongerDescendantQueries() {
        val gate = FileSearchQueryGate()
        gate.recordResult("invoicez", localFiles, hasResults = false)

        assertFalse(gate.shouldSearch("invoicez", localFiles))
        assertFalse(gate.shouldSearch("invoicezz", localFiles))
        assertFalse(gate.shouldSearch("InvoiceZ Report", localFiles))
    }

    @Test fun backspaceOrEarlierEditAllowsSearchingAgain() {
        val gate = FileSearchQueryGate()
        gate.recordResult("invoicez", localFiles, hasResults = false)

        assertTrue(gate.shouldSearch("invoice", localFiles))
        gate.recordResult("invoicez", localFiles, hasResults = false)
        assertTrue(gate.shouldSearch("invoicex", localFiles))
    }

    @Test fun changedFileScopeAllowsSearchingAgain() {
        val gate = FileSearchQueryGate()
        gate.recordResult("invoicez", localFiles, hasResults = false)
        val withAnotherFolder = localFiles.copy(
            folderUris = localFiles.folderUris + "content://documents/archive",
        )

        assertTrue(gate.shouldSearch("invoicezz", withAnotherFolder))
    }

    @Test fun resetAndSuccessfulResultsClearTheKnownEmptyPrefix() {
        val gate = FileSearchQueryGate()
        gate.recordResult("invoicez", localFiles, hasResults = false)
        gate.reset()
        assertTrue(gate.shouldSearch("invoicezz", localFiles))

        gate.recordResult("invoicez", localFiles, hasResults = false)
        gate.recordResult("invoicez", localFiles, hasResults = true)
        assertTrue(gate.shouldSearch("invoicezz", localFiles))
    }

    @Test fun scopeReportsWhetherAnyLocalSourceCanBeSearched() {
        assertTrue(localFiles.hasSearchableSources)
        assertFalse(
            FileSearchScope(
                folderUris = emptyList(),
                includesMedia = false,
                usesDemoData = false,
            ).hasSearchableSources,
        )
    }
}
