package com.katoaapps.minkboard.core

data class DefaultStatus(
    val name: String,
    val colorArgb: Int,
    val isDone: Boolean = false,
)

object MinkBoardDefaults {
    val statuses = listOf(
        DefaultStatus("To do", 0xFFAD4A25.toInt()),
        DefaultStatus("Doing", 0xFFD6A300.toInt()),
        DefaultStatus("Done", 0xFF173529.toInt(), isDone = true),
    )
}
