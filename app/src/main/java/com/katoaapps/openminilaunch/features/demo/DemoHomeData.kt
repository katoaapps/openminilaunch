package com.katoaapps.openminilaunch.features.demo

import com.katoaapps.openminilaunch.model.TodoItem

/** Non-persistent home-screen content used only while the hidden demo mode is enabled. */
internal object DemoHomeData {
    fun todos(): List<TodoItem> = listOf(
        TodoItem("demo-todo-1", "Send revised deck to Maya"),
        TodoItem("demo-todo-2", "Book train tickets for Friday"),
        TodoItem("demo-todo-3", "Call Kara after work", completed = true),
        TodoItem("demo-todo-4", "Review hosting estimate"),
        TodoItem("demo-todo-5", "Confirm dinner reservation"),
    )
}
