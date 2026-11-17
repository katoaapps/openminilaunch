package com.katoaapps.minkboard.ui

/**
 * Host-owned state for the single MinkBoard editor that can be open at a time.
 *
 * IDs intentionally remain opaque strings so a host can persist this state without depending on
 * Android parcelables or serializing repository models. [MinkBoardApp] resolves every ID against
 * the current editable workspace before rendering it.
 *
 * This type contains values only. Repository callbacks, authentication requests, destructive
 * confirmations, and in-flight operations must remain outside this state and must never be
 * replayed when a host restores it. Drafts contain plaintext user-authored content; a host that
 * persists them across process death must encrypt the serialized form before placing it in Android
 * saved-instance state or any other storage.
 */
sealed interface MinkBoardEditorState {
    data object None : MinkBoardEditorState

    data class NewBoard(
        val draft: BoardEditorDraft = BoardEditorDraft(),
    ) : MinkBoardEditorState

    data class EditBoard(
        val boardIdValue: String,
        val draft: BoardEditorDraft,
    ) : MinkBoardEditorState

    data class NewStatus(
        val boardIdValue: String,
        val draft: StatusEditorDraft = StatusEditorDraft(),
    ) : MinkBoardEditorState

    data class EditStatus(
        val boardIdValue: String,
        val statusIdValue: String,
        val draft: StatusEditorDraft,
    ) : MinkBoardEditorState

    data class NewCard(
        val boardIdValue: String,
        val statusIdValue: String,
        val draft: CardEditorDraft = CardEditorDraft(),
    ) : MinkBoardEditorState

    data class EditCard(
        val boardIdValue: String,
        val cardIdValue: String,
        val draft: CardEditorDraft,
    ) : MinkBoardEditorState

    data class ManageCardSubtasks(
        val boardIdValue: String,
        val cardIdValue: String,
        val draft: SubtaskManagerDraft = SubtaskManagerDraft(),
    ) : MinkBoardEditorState

    data class FocusCard(
        val boardIdValue: String,
        val cardIdValue: String,
        val draft: FocusTimerEditorDraft = FocusTimerEditorDraft(),
        /** Unsaved card fields to restore when the focus-duration overlay closes. */
        val returnCardDraft: CardEditorDraft? = null,
    ) : MinkBoardEditorState
}

data class BoardEditorDraft(
    val name: String = "",
    val templateIdValue: String? = null,
    val isLocked: Boolean = false,
)

data class StatusEditorDraft(
    val name: String = "",
    val colorArgb: Int = DEFAULT_STATUS_COLOR_ARGB,
    val isDone: Boolean = false,
    /** Non-null only while the nested custom-color editor is open. */
    val customColorDraft: StatusCustomColorDraft? = null,
)

data class StatusCustomColorDraft(
    val hue: Float,
    val saturation: Float,
    val brightness: Float,
    val hexText: String,
)

data class CardEditorDraft(
    val title: String = "",
    val details: String = "",
    val dueDateText: String = "",
    val isMajor: Boolean = false,
    /** Destination status selected in the editor; null means keep the card's current status. */
    val destinationStatusIdValue: String? = null,
)

data class SubtaskManagerDraft(
    val pendingSubtaskText: String = "",
    val editingSubtaskIdValue: String? = null,
    val editingSubtaskText: String = "",
)

data class FocusTimerEditorDraft(
    val hours: String = "0",
    val minutes: String = "5",
    val seconds: String = "0",
)

const val DEFAULT_STATUS_COLOR_ARGB: Int = 0xFF602C00.toInt()
