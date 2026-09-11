package com.katoaapps.openminilaunch.data

import androidx.compose.runtime.mutableStateListOf
import com.katoaapps.openminilaunch.model.TodoItem
import java.util.UUID

internal fun unfinishedFirst(items: List<TodoItem>): List<TodoItem> {
    val (unfinished, completed) = items.partition { !it.completed }
    return unfinished + completed
}

internal class TodoStore(
    private val persistence: LauncherStorePersistence,
    private val demoEnabled: () -> Boolean,
) {
    val items = mutableStateListOf<TodoItem>()

    fun restoreSaved() {
        items.clear()
        items.addAll(unfinishedFirst(persistence.loadTodos()))
    }

    fun showDemo(demoItems: List<TodoItem>) {
        items.clear()
        items.addAll(demoItems)
    }

    fun add(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val firstCompletedIndex = items.indexOfFirst(TodoItem::completed)
            .takeUnless { it == -1 }
            ?: items.size
        items.add(firstCompletedIndex, TodoItem(UUID.randomUUID().toString(), clean))
        save()
    }

    fun toggle(id: String) = update(id) { it.copy(completed = !it.completed) }

    fun rename(id: String, text: String) = update(id) { it.copy(text = text.trim()) }

    fun delete(id: String) {
        items.removeAll { it.id == id }
        save()
    }

    fun setOrder(orderedIds: List<String>) {
        if (orderedIds.size != items.size || orderedIds.toSet().size != items.size) return
        val itemsById = items.associateBy(TodoItem::id)
        val reordered = unfinishedFirst(orderedIds.mapNotNull(itemsById::get))
        if (reordered.size != items.size || reordered == items) return
        items.clear()
        items.addAll(reordered)
        save()
    }

    fun replaceFromBackup(backupItems: List<TodoItem>) {
        val usedIds = mutableSetOf<String>()
        val restored = backupItems.mapNotNull { item ->
            val text = item.text.trim()
            if (text.isEmpty()) return@mapNotNull null
            val id = item.id.takeIf { it.isNotBlank() && usedIds.add(it) }
                ?: UUID.randomUUID().toString().also(usedIds::add)
            TodoItem(id = id, text = text, completed = item.completed)
        }
        items.clear()
        items.addAll(unfinishedFirst(restored))
        persistence.saveTodos(items)
    }

    private fun update(id: String, transform: (TodoItem) -> TodoItem) {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return
        items[index] = transform(items[index])
        keepUnfinishedFirst()
        save()
    }

    private fun keepUnfinishedFirst() {
        val ordered = unfinishedFirst(items)
        if (ordered == items) return
        items.clear()
        items.addAll(ordered)
    }

    private fun save() {
        if (!demoEnabled()) persistence.saveTodos(items)
    }
}
