package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.magic.*
import com.katoaapps.openminilaunch.ui.magic.MagicBoxSessionState

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test

class MagicBoxInputTest {
    @Test
    fun demoCaptureSearchCanReturnToACleanHomeFrame() {
        val session = MagicBoxSessionState.empty()

        session.prepareDemoSearch("invoice")

        assertEquals("invoice", session.textState.value.text)
        assertEquals(true, session.expanded)

        session.resetForDemoHome()

        assertEquals("", session.textState.value.text)
        assertEquals(null, session.selectedRecipientState.value)
        assertEquals(null, session.lockedPrefixState.value)
        assertEquals(false, session.expanded)
    }

    @Test
    fun printableHardwareInputKeepsShiftedHotkeysAndUnicode() {
        assertEquals("@", printableHardwareText('@'.code))
        assertEquals("M", printableHardwareText('M'.code))
        assertEquals("é", printableHardwareText('é'.code))
    }

    @Test
    fun nonPrintableHardwareInputIsIgnored() {
        assertEquals(null, printableHardwareText(0))
        assertEquals(null, printableHardwareText('\n'.code))
    }

    @Test
    fun exposedQwertyKeyboardUsesDirectTextInput() {
        assertEquals(
            true,
            hasUsableHardwareKeyboard(Configuration.KEYBOARD_QWERTY, Configuration.HARDKEYBOARDHIDDEN_NO),
        )
        assertEquals(
            false,
            hasUsableHardwareKeyboard(Configuration.KEYBOARD_NOKEYS, Configuration.HARDKEYBOARDHIDDEN_NO),
        )
        assertEquals(
            false,
            hasUsableHardwareKeyboard(Configuration.KEYBOARD_QWERTY, Configuration.HARDKEYBOARDHIDDEN_YES),
        )
    }

    @Test
    fun autoOpenKeyboardOnlyAppliesToEnabledTouchDevices() {
        assertEquals(
            true,
            shouldAutoOpenSoftwareKeyboard(
                true,
                Configuration.KEYBOARD_NOKEYS,
                Configuration.HARDKEYBOARDHIDDEN_NO,
            ),
        )
        assertEquals(
            false,
            shouldAutoOpenSoftwareKeyboard(
                false,
                Configuration.KEYBOARD_NOKEYS,
                Configuration.HARDKEYBOARDHIDDEN_NO,
            ),
        )
        assertEquals(
            false,
            shouldAutoOpenSoftwareKeyboard(
                true,
                Configuration.KEYBOARD_QWERTY,
                Configuration.HARDKEYBOARDHIDDEN_NO,
            ),
        )
    }

    @Test
    fun plainTextBecomesSearchQuery() {
        val input = parseMagicBoxInput("  quarterly budget  ")

        assertEquals(' ', input.prefix)
        assertEquals("quarterly budget", input.plainQuery)
    }

    @Test
    fun commandPrefixProducesSuggestionTermWithoutPlainSearch() {
        val input = parseMagicBoxInput("?maps Home Depot")

        assertEquals('?', input.prefix)
        assertEquals("maps", input.searchTerm)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun slashPrefixProducesNoteCommandWithoutPlainSearch() {
        val input = parseMagicBoxInput("/Draft the release announcement")

        assertEquals(MAGIC_NOTE_PREFIX, input.prefix)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun starPrefixProducesAiCommandWithoutPlainSearch() {
        val input = parseMagicBoxInput("*Summarize this release")

        assertEquals(MAGIC_AI_PREFIX, input.prefix)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun dollarPrefixIsNowPlainSearchText() {
        val input = parseMagicBoxInput("\$quarterly revenue")

        assertEquals('$', input.prefix)
        assertEquals("\$quarterly revenue", input.plainQuery)
    }

    @Test
    fun selectedContactLocksCommandAndStopsContactFiltering() {
        val input = parseMagicBoxInput("message body", lockedPrefix = '@')

        assertEquals('@', input.prefix)
        assertEquals("", input.searchTerm)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun commandPrefixAloneIsNotConsideredADraft() {
        MAGIC_COMMAND_PREFIXES.forEach { prefix ->
            assertEquals(false, hasMagicBoxDraftText(prefix.toString()))
        }
    }

    @Test
    fun emptyTodoCommandOpensTodoList() {
        assertEquals(true, isOpenTodosCommand("-"))
        assertEquals(true, isOpenTodosCommand("-   "))
        assertEquals(false, isOpenTodosCommand("-Buy milk"))
        assertEquals(false, isOpenTodosCommand("Buy milk"))
        assertEquals(false, isOpenTodosCommand("-", lockedPrefix = '@'))
    }

    @Test
    fun commandAndPlainTextAreConsideredDrafts() {
        assertEquals(true, hasMagicBoxDraftText("-Book train tickets"))
        assertEquals(true, hasMagicBoxDraftText("+Dinner Friday at 7"))
        assertEquals(true, hasMagicBoxDraftText("*Summarize this release"))
        assertEquals(true, hasMagicBoxDraftText("quarterly budget"))
    }

    @Test
    fun lockedContactRequiresMessageTextBeforeItIsADraft() {
        assertEquals(false, hasMagicBoxDraftText("", lockedPrefix = '@'))
        assertEquals(true, hasMagicBoxDraftText("On my way", lockedPrefix = '@'))
    }
}
