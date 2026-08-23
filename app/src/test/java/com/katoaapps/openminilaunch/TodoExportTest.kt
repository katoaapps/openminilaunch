package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.todos.*
import com.katoaapps.openminilaunch.model.*

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoExportTest {
    @Test fun includesCompletionStateAndText() {
        val export = formatTodoExport(
            "MinkLauncher To-do List",
            listOf(
                TodoItem("1", "Book train tickets"),
                TodoItem("2", "Send revised deck", completed = true),
            ),
        )

        assertEquals(
            "MinkLauncher To-do List\n\n[ ] Book train tickets\n[x] Send revised deck",
            export,
        )
    }
}
