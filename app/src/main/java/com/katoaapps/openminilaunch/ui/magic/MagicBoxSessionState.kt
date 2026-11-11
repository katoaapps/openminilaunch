package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.katoaapps.openminilaunch.features.magic.MagicBoxRoute
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget

/**
 * The editable part of a Magic Box session.
 *
 * Home can move between a pager and a two-panel host while a fold changes size. Keeping this state
 * above both hosts prevents that necessary layout replacement from becoming an editor reset.
 */
@Stable
internal class MagicBoxSessionState private constructor(
    text: TextFieldValue,
    selectedRecipient: SelectedMessageRecipient?,
    route: MagicBoxRoute,
    expanded: Boolean,
) {
    val textState: MutableState<TextFieldValue> = mutableStateOf(text)
    val selectedRecipientState: MutableState<SelectedMessageRecipient?> =
        mutableStateOf(selectedRecipient)
    val routeState: MutableState<MagicBoxRoute> = mutableStateOf(route)
    val expandedState: MutableState<Boolean> = mutableStateOf(expanded)

    val expanded: Boolean
        get() = expandedState.value

    /** Starts a message draft with the person already selected by another Mink surface. */
    fun prepareMessage(
        recipient: CommunicationRecipient,
        providerPackage: String?,
        providerRouteOverridden: Boolean,
    ) {
        textState.value = TextFieldValue()
        selectedRecipientState.value = SelectedMessageRecipient.AddressRecipient(
            recipient = recipient,
            providerPackage = providerPackage,
            providerRouteOverridden = providerRouteOverridden,
        )
        routeState.value = MagicBoxRoute.MESSAGE
        expandedState.value = true
    }

    /** Starts a message draft for an exact app-published conversation shortcut. */
    fun prepareConversationMessage(conversation: LauncherShortcutTarget) {
        textState.value = TextFieldValue()
        selectedRecipientState.value = SelectedMessageRecipient.ConversationRecipient(conversation)
        routeState.value = MagicBoxRoute.MESSAGE
        expandedState.value = true
    }

    /** Prepares a fictional search without passing content to another app. */
    fun prepareDemoSearch(query: String) {
        textState.value = TextFieldValue(query, selection = TextRange(query.length))
        selectedRecipientState.value = null
        routeState.value = MagicBoxRoute.SEARCH
        expandedState.value = true
    }

    /** Returns the persistent Home editor to its clean opening frame. */
    fun resetForDemoHome() {
        textState.value = TextFieldValue()
        selectedRecipientState.value = null
        routeState.value = MagicBoxRoute.SEARCH
        expandedState.value = false
    }

    companion object {
        private const val TEXT_KEY = "text"
        private const val SELECTION_START_KEY = "selection_start"
        private const val SELECTION_END_KEY = "selection_end"
        private const val RECIPIENT_KEY = "recipient"
        private const val ROUTE_KEY = "route"
        private const val LEGACY_PREFIX_KEY = "prefix"
        private const val EXPANDED_KEY = "expanded"

        fun empty(initiallyExpanded: Boolean = false): MagicBoxSessionState = MagicBoxSessionState(
            text = TextFieldValue(),
            selectedRecipient = null,
            route = MagicBoxRoute.SEARCH,
            expanded = initiallyExpanded,
        )

        val Saver = mapSaver(
            save = { state ->
                mapOf(
                    TEXT_KEY to state.textState.value.text,
                    SELECTION_START_KEY to state.textState.value.selection.start,
                    SELECTION_END_KEY to state.textState.value.selection.end,
                    RECIPIENT_KEY to state.selectedRecipientState.value?.snapshot().orEmpty(),
                    ROUTE_KEY to state.routeState.value.name,
                    EXPANDED_KEY to state.expandedState.value,
                )
            },
            restore = { saved ->
                val text = saved.getValue(TEXT_KEY) as String
                val selectionStart = (saved.getValue(SELECTION_START_KEY) as Int)
                    .coerceIn(0, text.length)
                val selectionEnd = (saved.getValue(SELECTION_END_KEY) as Int)
                    .coerceIn(0, text.length)
                val recipientSnapshot = saved.getValue(RECIPIENT_KEY) as String
                val restoredRoute = (saved[ROUTE_KEY] as? String)
                    ?.let { savedRoute ->
                        MagicBoxRoute.entries.firstOrNull { it.name == savedRoute }
                    }
                    ?: (saved[LEGACY_PREFIX_KEY] as? Int)
                        ?.takeIf { it >= 0 }
                        ?.toChar()
                        ?.let(MagicBoxRoute::fromTrigger)
                    ?: MagicBoxRoute.SEARCH
                MagicBoxSessionState(
                    text = TextFieldValue(
                        text = text,
                        selection = TextRange(selectionStart, selectionEnd),
                    ),
                    selectedRecipient = recipientSnapshot
                        .takeIf(String::isNotBlank)
                        ?.let(::recipientFromSnapshot),
                    route = restoredRoute,
                    expanded = saved.getValue(EXPANDED_KEY) as Boolean,
                )
            },
        )
    }
}

@Composable
internal fun rememberMagicBoxSessionState(
    initiallyExpanded: Boolean = false,
): MagicBoxSessionState = rememberSaveable(saver = MagicBoxSessionState.Saver) {
    MagicBoxSessionState.empty(initiallyExpanded)
}
