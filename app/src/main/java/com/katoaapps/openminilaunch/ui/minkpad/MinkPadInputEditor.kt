package com.katoaapps.openminilaunch.ui.minkpad

import android.content.Context
import android.graphics.Rect
import android.text.InputType
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.katoaapps.openminilaunch.R

/** Only transient IME composition lives here. Committed host text is immediately cleared. */
internal class MinkPadInputEditor(context: Context) : EditText(context) {
    var commit: (String) -> Unit = {}
    var backspace: () -> Unit = {}
    var forwardDelete: () -> Unit = {}
    var enter: () -> Unit = {}
    private var hardwareKeyboard = false
    private var softwareInput = false
    private val inputMethod get() = context.getSystemService(InputMethodManager::class.java)
    private val canSendInput get() = isEnabled && hasWindowFocus()

    init {
        isEnabled = false
        hint = context.getString(R.string.minkpad_type_directly_hint)
        // A remote keyboard sends literal input, not locally corrected prose. Some physical
        // IMEs ignore NO_SUGGESTIONS for smart caps/spacing but honor this standard variation.
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        imeOptions = EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        setPadding(24, 20, 24, 20)
        isSaveEnabled = false
        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }

    fun configureInput(enabled: Boolean, hasHardwareKeyboard: Boolean, showSoftwareInput: Boolean) {
        val activate = enabled && (!isEnabled || hardwareKeyboard != hasHardwareKeyboard ||
            softwareInput != showSoftwareInput)
        if (!enabled) {
            if (hasFocus() && softwareInput) inputMethod?.hideSoftInputFromWindow(windowToken, 0)
            isEnabled = false
            setText("")
            clearFocus()
        }
        hardwareKeyboard = hasHardwareKeyboard
        softwareInput = showSoftwareInput
        showSoftInputOnFocus = showSoftwareInput
        isEnabled = enabled
        if (activate) activateEditor()
    }

    private fun activateEditor() {
        if (!isEnabled) return
        requestFocus()
        post {
            // A pending focus/IME request must not reactivate input after a dialog or disconnect.
            if (!isEnabled || !hasWindowFocus()) return@post
            requestFocusFromTouch()
            if (softwareInput) {
                inputMethod?.restartInput(this)
                inputMethod?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && isEnabled && (hardwareKeyboard || softwareInput)) activateEditor()
        else if (!hasWindowFocus) setText("")
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) setText("")
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val base = super.onCreateInputConnection(outAttrs) ?: return null
        outAttrs.initialCapsMode = 0
        return object : InputConnectionWrapper(base, false) {
            // Clearing committed text must not make each next key a new sentence.
            override fun getCursorCapsMode(reqModes: Int): Int = 0

            override fun commitText(value: CharSequence?, newCursorPosition: Int): Boolean {
                if (canSendInput) value?.toString()?.takeIf(String::isNotEmpty)?.let(commit)
                setText("")
                return true
            }

            override fun finishComposingText(): Boolean {
                val editable = text
                if (editable != null && BaseInputConnection.getComposingSpanStart(editable) >= 0) {
                    if (canSendInput) editable.toString().takeIf(String::isNotEmpty)?.let(commit)
                    setText("")
                }
                return super.finishComposingText()
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (!canSendInput) return true
                if (!text.isNullOrEmpty()) return super.deleteSurroundingText(beforeLength, afterLength)
                repeat(beforeLength.coerceIn(0, 64)) { backspace() }
                repeat(afterLength.coerceIn(0, 64)) { forwardDelete() }
                return true
            }

            override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean =
                deleteSurroundingText(beforeLength, afterLength)

            override fun performEditorAction(editorAction: Int): Boolean {
                if (canSendInput) enter()
                return true
            }
        }
    }
}
