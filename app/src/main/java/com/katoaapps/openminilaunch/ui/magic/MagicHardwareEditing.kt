package com.katoaapps.openminilaunch.ui.magic

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue

internal enum class HorizontalCursorDirection { LEFT, RIGHT }
internal enum class VerticalCursorDirection { UP, DOWN }

/** Normalizes editing shortcuts that some physical-keyboard IMEs do not forward to Compose. */
internal fun handleMagicHardwareEditing(
    event: KeyEvent,
    value: TextFieldValue,
    textLayout: TextLayoutResult?,
    preferredVerticalX: Float?,
    onPreferredVerticalXChange: (Float?) -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
): Boolean {
    val nativeEvent = event.nativeKeyEvent
    if ((nativeEvent.isCtrlPressed || nativeEvent.isMetaPressed) &&
        nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_A
    ) {
        if (event.type == KeyEventType.KeyDown) {
            onPreferredVerticalXChange(null)
            onValueChange(selectAllMagicText(value))
        }
        return true
    }
    if (nativeEvent.isCtrlPressed || nativeEvent.isMetaPressed || nativeEvent.isAltPressed) return false
    if (nativeEvent.keyCode in VERTICAL_EDITING_KEYS) {
        if (event.type == KeyEventType.KeyDown && textLayout != null) {
            val direction = if (nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP) {
                VerticalCursorDirection.UP
            } else {
                VerticalCursorDirection.DOWN
            }
            val movement = moveMagicCursorVertically(
                value = value,
                direction = direction,
                textLayout = textLayout,
                preferredX = preferredVerticalX,
                extendSelection = nativeEvent.isShiftPressed,
            )
            onValueChange(movement.value)
            onPreferredVerticalXChange(movement.preferredX)
        }
        // Consume even at the first/last line so focus cannot escape into Home.
        return true
    }
    if (nativeEvent.keyCode !in HORIZONTAL_EDITING_KEYS) return false

    if (event.type == KeyEventType.KeyDown) {
        onPreferredVerticalXChange(null)
        val updatedValue = when (nativeEvent.keyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> moveMagicCursor(
                value,
                HorizontalCursorDirection.LEFT,
                nativeEvent.isShiftPressed,
            )
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> moveMagicCursor(
                value,
                HorizontalCursorDirection.RIGHT,
                nativeEvent.isShiftPressed,
            )
            AndroidKeyEvent.KEYCODE_MOVE_HOME -> moveMagicCursorToEdge(
                value,
                end = false,
                extendSelection = nativeEvent.isShiftPressed,
            )
            else -> moveMagicCursorToEdge(
                value,
                end = true,
                extendSelection = nativeEvent.isShiftPressed,
            )
        }
        onValueChange(updatedValue)
    }
    return true
}

internal data class VerticalCursorMovement(
    val value: TextFieldValue,
    val preferredX: Float,
)

internal fun moveMagicCursorVertically(
    value: TextFieldValue,
    direction: VerticalCursorDirection,
    textLayout: TextLayoutResult,
    preferredX: Float?,
    extendSelection: Boolean,
): VerticalCursorMovement {
    val cursor = value.selection.end.coerceIn(0, value.text.length)
    val currentLine = textLayout.getLineForOffset(cursor)
    val targetLine = when (direction) {
        VerticalCursorDirection.UP -> currentLine - 1
        VerticalCursorDirection.DOWN -> currentLine + 1
    }
    val cursorX = preferredX ?: textLayout.getCursorRect(cursor).left
    if (targetLine !in 0 until textLayout.lineCount) {
        return VerticalCursorMovement(value, cursorX)
    }

    val targetY = (textLayout.getLineTop(targetLine) + textLayout.getLineBottom(targetLine)) / 2f
    val targetOffset = textLayout.getOffsetForPosition(Offset(cursorX, targetY))
        .coerceIn(0, value.text.length)
    val selection = if (extendSelection) {
        TextRange(value.selection.start, targetOffset)
    } else {
        TextRange(targetOffset)
    }
    return VerticalCursorMovement(value.copy(selection = selection), cursorX)
}

internal fun selectAllMagicText(value: TextFieldValue): TextFieldValue =
    value.copy(selection = TextRange(0, value.text.length))

internal fun moveMagicCursor(
    value: TextFieldValue,
    direction: HorizontalCursorDirection,
    extendSelection: Boolean,
): TextFieldValue {
    val selection = value.selection
    if (!extendSelection && !selection.collapsed) {
        val edge = when (direction) {
            HorizontalCursorDirection.LEFT -> minOf(selection.start, selection.end)
            HorizontalCursorDirection.RIGHT -> maxOf(selection.start, selection.end)
        }
        return value.copy(selection = TextRange(edge))
    }

    val cursor = selection.end.coerceIn(0, value.text.length)
    val movedCursor = when (direction) {
        HorizontalCursorDirection.LEFT -> value.text.previousCodePointOffset(cursor)
        HorizontalCursorDirection.RIGHT -> value.text.nextCodePointOffset(cursor)
    }
    val movedSelection = if (extendSelection) {
        TextRange(selection.start, movedCursor)
    } else {
        TextRange(movedCursor)
    }
    return value.copy(selection = movedSelection)
}

internal fun moveMagicCursorToEdge(
    value: TextFieldValue,
    end: Boolean,
    extendSelection: Boolean,
): TextFieldValue {
    val position = if (end) value.text.length else 0
    return value.copy(
        selection = if (extendSelection) {
            TextRange(value.selection.start, position)
        } else {
            TextRange(position)
        },
    )
}

private fun String.previousCodePointOffset(offset: Int): Int {
    if (offset <= 0) return 0
    return offset - Character.charCount(Character.codePointBefore(this, offset))
}

private fun String.nextCodePointOffset(offset: Int): Int {
    if (offset >= length) return length
    return offset + Character.charCount(Character.codePointAt(this, offset))
}

private val HORIZONTAL_EDITING_KEYS = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
    AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
    AndroidKeyEvent.KEYCODE_MOVE_HOME,
    AndroidKeyEvent.KEYCODE_MOVE_END,
)

private val VERTICAL_EDITING_KEYS = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_UP,
    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
)
