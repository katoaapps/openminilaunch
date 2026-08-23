package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoOrderingTest {
    @Test
    fun unfinishedTodosComeBeforeCompletedTodosWithoutChangingGroupOrder() {
        val todos = listOf(
            todo("completed-first", completed = true),
            todo("unfinished-first"),
            todo("completed-second", completed = true),
            todo("unfinished-second"),
        )

        val ordered = unfinishedFirst(todos)

        assertEquals(
            listOf("unfinished-first", "unfinished-second", "completed-first", "completed-second"),
            ordered.map(TodoItem::id),
        )
        assertTrue(ordered.take(2).none(TodoItem::completed))
        assertTrue(ordered.drop(2).all(TodoItem::completed))
    }

    private fun todo(id: String, completed: Boolean = false) = TodoItem(
        id = id,
        text = id,
        completed = completed,
    )
}
