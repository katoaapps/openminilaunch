package com.katoaapps.openminilaunch.features.minkspace.todo

data class TodoItem(
    val id: String,
    val text: String,
    val completed: Boolean = false,
)
