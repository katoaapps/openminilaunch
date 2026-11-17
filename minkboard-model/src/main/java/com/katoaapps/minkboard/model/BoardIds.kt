package com.katoaapps.minkboard.model

private const val MAX_ID_LENGTH = 128

private fun validateId(kind: String, value: String) {
    require(value.isNotBlank()) { "$kind cannot be blank" }
    require(value.length <= MAX_ID_LENGTH) { "$kind cannot exceed $MAX_ID_LENGTH characters" }
}

@JvmInline
value class BoardId(val value: String) {
    init { validateId("BoardId", value) }
}

@JvmInline
value class StatusId(val value: String) {
    init { validateId("StatusId", value) }
}

@JvmInline
value class CardId(val value: String) {
    init { validateId("CardId", value) }
}

@JvmInline
value class SubtaskId(val value: String) {
    init { validateId("SubtaskId", value) }
}
