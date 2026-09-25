package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

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
    lockedPrefix: Char?,
    expanded: Boolean,
) {
    val textState: MutableState<TextFieldValue> = mutableStateOf(text)
    val selectedRecipientState: MutableState<SelectedMessageRecipient?> =
        mutableStateOf(selectedRecipient)
    val lockedPrefixState: MutableState<Char?> = mutableStateOf(lockedPrefix)
    val expandedState: MutableState<Boolean> = mutableStateOf(expanded)

    val expanded: Boolean
        get() = expandedState.value

    companion object {
        private const val TEXT_KEY = "text"
        private const val SELECTION_START_KEY = "selection_start"
        private const val SELECTION_END_KEY = "selection_end"
        private const val RECIPIENT_KEY = "recipient"
        private const val PREFIX_KEY = "prefix"
        private const val EXPANDED_KEY = "expanded"

        fun empty(initiallyExpanded: Boolean = false): MagicBoxSessionState = MagicBoxSessionState(
            text = TextFieldValue(),
            selectedRecipient = null,
            lockedPrefix = null,
            expanded = initiallyExpanded,
        )

        val Saver = mapSaver(
            save = { state ->
                mapOf(
                    TEXT_KEY to state.textState.value.text,
                    SELECTION_START_KEY to state.textState.value.selection.start,
                    SELECTION_END_KEY to state.textState.value.selection.end,
                    RECIPIENT_KEY to state.selectedRecipientState.value?.snapshot().orEmpty(),
                    PREFIX_KEY to (state.lockedPrefixState.value?.code ?: -1),
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
                val prefixCode = saved.getValue(PREFIX_KEY) as Int
                MagicBoxSessionState(
                    text = TextFieldValue(
                        text = text,
                        selection = TextRange(selectionStart, selectionEnd),
                    ),
                    selectedRecipient = recipientSnapshot
                        .takeIf(String::isNotBlank)
                        ?.let(::recipientFromSnapshot),
                    lockedPrefix = prefixCode.takeIf { it >= 0 }?.toChar(),
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
