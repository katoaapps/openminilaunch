package com.katoaapps.minkboard.core

object MinkBoardRules {
    const val MAX_BOARD_NAME_LENGTH = 80
    const val MAX_STATUS_NAME_LENGTH = 60
    const val MAX_CARD_TITLE_LENGTH = 500
    const val MAX_CARD_DETAILS_LENGTH = 50_000
    const val MAX_SUBTASK_LENGTH = 2_000

    fun boardName(value: String): String = requiredText(
        label = "Board name",
        value = value,
        maxLength = MAX_BOARD_NAME_LENGTH,
    )

    fun statusName(value: String): String = requiredText(
        label = "Status name",
        value = value,
        maxLength = MAX_STATUS_NAME_LENGTH,
    )

    fun cardTitle(value: String): String = requiredText(
        label = "Card title",
        value = value,
        maxLength = MAX_CARD_TITLE_LENGTH,
    )

    fun cardDetails(value: String): String = boundedText(
        label = "Card details",
        value = value,
        maxLength = MAX_CARD_DETAILS_LENGTH,
    )

    fun subtaskText(value: String): String = requiredText(
        label = "Subtask",
        value = value,
        maxLength = MAX_SUBTASK_LENGTH,
    )

    private fun requiredText(label: String, value: String, maxLength: Int): String {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "$label cannot be blank" }
        return boundedText(label, normalized, maxLength)
    }

    private fun boundedText(label: String, value: String, maxLength: Int): String {
        require(value.length <= maxLength) { "$label cannot exceed $maxLength characters" }
        return value
    }
}
