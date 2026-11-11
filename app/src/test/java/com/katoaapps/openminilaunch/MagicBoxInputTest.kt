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
        assertEquals(MagicBoxRoute.SEARCH, session.routeState.value)
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

        assertEquals(MagicBoxRoute.SEARCH, input.route)
        assertEquals("quarterly budget", input.plainQuery)
    }

    @Test
    fun appRouteProducesSuggestionTermWithoutPlainSearch() {
        val input = parseMagicBoxInput("maps Home Depot", MagicBoxRoute.APP)

        assertEquals(MagicBoxRoute.APP, input.route)
        assertEquals("maps Home Depot", input.searchTerm)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun noteRouteDoesNotProducePlainSearch() {
        val input = parseMagicBoxInput("Draft the release announcement", MagicBoxRoute.NOTE)

        assertEquals(MagicBoxRoute.NOTE, input.route)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun assistantRouteDoesNotProducePlainSearch() {
        val input = parseMagicBoxInput("Summarize this release", MagicBoxRoute.ASSISTANT)

        assertEquals(MagicBoxRoute.ASSISTANT, input.route)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun ordinaryPunctuationRemainsPlainSearchText() {
        val input = parseMagicBoxInput("\$quarterly revenue")

        assertEquals(MagicBoxRoute.SEARCH, input.route)
        assertEquals("\$quarterly revenue", input.plainQuery)
    }

    @Test
    fun messageRouteKeepsEditorContentOutOfPlainSearch() {
        val input = parseMagicBoxInput("message body", MagicBoxRoute.MESSAGE)

        assertEquals(MagicBoxRoute.MESSAGE, input.route)
        assertEquals("message body", input.searchTerm)
        assertEquals("", input.plainQuery)
    }

    @Test
    fun hotkeySelectsRouteWithoutBecomingEditorContent() {
        val result = consumeMagicBoxTrigger("", "@")

        assertEquals(MagicBoxRoute.MESSAGE, result?.route)
        assertEquals("", result?.content)
    }

    @Test
    fun emptyTodoCommandOpensTodoList() {
        assertEquals(true, isOpenTodosCommand("", MagicBoxRoute.TODO))
        assertEquals(true, isOpenTodosCommand("   ", MagicBoxRoute.TODO))
        assertEquals(false, isOpenTodosCommand("Buy milk", MagicBoxRoute.TODO))
        assertEquals(false, isOpenTodosCommand("", MagicBoxRoute.MESSAGE))
    }

    @Test
    fun commandAndPlainTextAreConsideredDrafts() {
        assertEquals(true, hasMagicBoxDraftText("Book train tickets"))
        assertEquals(true, hasMagicBoxDraftText("Dinner Friday at 7"))
        assertEquals(true, hasMagicBoxDraftText("Summarize this release"))
        assertEquals(true, hasMagicBoxDraftText("quarterly budget"))
    }

    @Test
    fun lockedContactRequiresMessageTextBeforeItIsADraft() {
        assertEquals(false, hasMagicBoxDraftText(""))
        assertEquals(true, hasMagicBoxDraftText("On my way"))
    }

    @Test
    fun pastedLegacyCommandIsSplitIntoRouteAndPayload() {
        val result = consumeMagicBoxTrigger("", "-Buy milk")

        assertEquals(MagicBoxRoute.TODO, result?.route)
        assertEquals("Buy milk", result?.content)
    }

    @Test
    fun commandCharactersRemainLiteralAfterTypingHasStarted() {
        assertEquals(
            null,
            consumeMagicBoxTrigger("email", "email@example.com"),
        )
    }

    @Test
    fun selectedRecipientBodyCanStartWithLiteralHotkeyCharacter() {
        assertEquals(
            null,
            consumeMagicBoxTrigger("", "@home", routeSelectionEnabled = false),
        )
    }
}
