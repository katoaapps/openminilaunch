package com.katoaapps.openminilaunch.model

data class TodoItem(
    val id: String,
    val text: String,
    val completed: Boolean = false,
)
