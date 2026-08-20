package com.katoaapps.openminilaunch

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri

internal fun formatTodoExport(todos: List<TodoItem>): String = buildString {
    appendLine("MinkLauncher To-do List")
    appendLine()
    todos.forEach { todo ->
        append(if (todo.completed) "[x] " else "[ ] ")
        appendLine(todo.text.trim())
    }
}.trimEnd()

internal object TodoPdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val LINE_HEIGHT = 22f

    fun write(context: Context, uri: Uri, todos: List<TodoItem>): Boolean = runCatching {
        val document = PdfDocument()
        try {
            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var y = 0f
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(96, 44, 0)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(28, 28, 28)
                textSize = 14f
            }

            fun startPage() {
                pageNumber += 1
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
                )
                y = MARGIN
                page!!.canvas.drawText("MinkLauncher To-do List", MARGIN, y, titlePaint)
                y += 38f
            }

            fun finishPage() {
                page?.let(document::finishPage)
                page = null
            }

            startPage()
            todos.forEach { todo ->
                val lines = wrapPdfText(
                    text = (if (todo.completed) "[x] " else "[ ] ") + todo.text.trim(),
                    paint = bodyPaint,
                    maxWidth = PAGE_WIDTH - MARGIN * 2,
                )
                if (y + lines.size * LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
                    finishPage()
                    startPage()
                }
                lines.forEach { line ->
                    page!!.canvas.drawText(line, MARGIN, y, bodyPaint)
                    y += LINE_HEIGHT
                }
                y += 5f
            }
            finishPage()
            context.contentResolver.openOutputStream(uri)?.use(document::writeTo)
                ?: error("Unable to open the selected document")
        } finally {
            document.close()
        }
        true
    }.getOrDefault(false)
}

private fun wrapPdfText(text: String, paint: Paint, maxWidth: Float): List<String> {
    if (text.isBlank()) return listOf("")
    val lines = mutableListOf<String>()
    var current = ""
    text.split(Regex("\\s+")).forEach { word ->
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
            current = candidate
        } else {
            lines += current
            current = word
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}
