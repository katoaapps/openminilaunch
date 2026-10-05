package com.katoaapps.openminilaunch.features.minkpad.hid

import android.os.Handler
import android.os.HandlerThread

/**
 * Owns one worker for all reports. Reset cancels input, never disconnect/close callbacks.
 * Bluetooth callbacks and UI code must not mutate the keyboard/pointer engines directly.
 */
internal class MinkPadReportSender(
    private val send: (Int, ByteArray) -> Boolean,
    private val onKeyboardReport: (active: Boolean) -> Unit = {},
) {
    private val thread = HandlerThread("MinkPadReports").apply { start() }
    private val handler = Handler(thread.looper)
    private val submissionLock = Any()
    private var generation = 0
    private var closed = false
    private var pendingPointer: PointerSubmission? = null
    private val keyboard = MinkPadKeyboardReports(handler) {
        onKeyboardReport(it.any { value -> value.toInt() != 0 })
        send(MinkPadReportId.KEYBOARD, it)
    }
    private val pointer = MinkPadPointerReports {
        send(MinkPadReportId.POINTER, it)
    }

    val keyboardReport get() = keyboard.latestReport
    val pointerReport get() = pointer.latestReport

    private fun enqueue(operation: () -> Unit) = synchronized(submissionLock) {
        if (closed) return@synchronized
        // Later motion must not merge across a click, key or other ordered operation.
        pendingPointer = null
        val epoch = generation
        handler.post {
            val current = synchronized(submissionLock) { !closed && generation == epoch }
            if (current) operation()
        }
    }

    fun key(usage: Int, pressed: Boolean, modifiers: Int) =
        enqueue { keyboard.key(usage, pressed, modifiers) }

    fun modifiers(value: Int) = enqueue { keyboard.modifiers(value) }
    fun tapKey(usage: Int) = enqueue { keyboard.tap(usage) }
    fun shortcut(usage: Int, modifiers: Int) = enqueue { keyboard.shortcut(usage, modifiers) }
    fun consumer(usage: Int) = enqueue {
        send(MinkPadReportId.CONSUMER, byteArrayOf(usage.toByte(), (usage shr 8).toByte()))
        send(MinkPadReportId.CONSUMER, ByteArray(2))
    }

    fun type(text: String): Boolean = synchronized(submissionLock) {
        if (closed) return false
        val mapped = keyboard.reserveText(text) ?: return false
        pendingPointer = null
        val epoch = generation
        handler.post {
            val current = synchronized(submissionLock) { !closed && generation == epoch }
            if (current) keyboard.text(mapped)
        }
        true
    }

    fun pointer(dx: Float = 0f, dy: Float = 0f, vertical: Float = 0f, horizontal: Float = 0f) {
        if (!dx.isFinite() || !dy.isFinite() || !vertical.isFinite() || !horizontal.isFinite()) return
        if (dx == 0f && dy == 0f && vertical == 0f && horizontal == 0f) return
        synchronized(submissionLock) {
            if (closed) return
            pendingPointer?.let {
                it.dx += dx
                it.dy += dy
                it.vertical += vertical
                it.horizontal += horizontal
                return
            }

            // One worker task for adjacent touch updates, rather than a runnable per event.
            val submission = PointerSubmission(generation, dx, dy, vertical, horizontal)
            pendingPointer = submission
            handler.post {
                val motion = synchronized(submissionLock) {
                    if (pendingPointer === submission) pendingPointer = null
                    if (closed || generation != submission.generation) null else submission.copy()
                }
                motion?.let {
                    pointer.move(it.dx, it.dy, it.vertical, it.horizontal)
                }
            }
        }
    }

    fun mouseButton(button: Int, pressed: Boolean) = enqueue { pointer.button(button, pressed) }

    fun click(button: Int) = enqueue {
        pointer.button(button, true)
        pointer.button(button, false)
    }

    fun releaseAll(onReleased: () -> Unit = {}) = synchronized(submissionLock) {
        if (closed) return@synchronized
        generation++
        pendingPointer = null
        keyboard.cancelReservations()
        // Do not remove all Handler callbacks: a pending connect/disconnect may own one.
        handler.post {
            resetReports()
            onReleased()
        }
    }

    private fun resetReports() {
        keyboard.reset()
        pointer.reset()
        send(MinkPadReportId.CONSUMER, ByteArray(2))
    }

    fun close(onReleased: () -> Unit) = synchronized(submissionLock) {
        if (closed) return@synchronized
        closed = true
        generation++
        pendingPointer = null
        keyboard.cancelReservations()
        handler.post {
            try {
                resetReports()
                onReleased()
            } finally {
                thread.quitSafely()
            }
        }
    }

    /** Mutable only under submissionLock; the worker receives a detached snapshot. */
    private data class PointerSubmission(
        val generation: Int,
        var dx: Float,
        var dy: Float,
        var vertical: Float,
        var horizontal: Float,
    )
}
