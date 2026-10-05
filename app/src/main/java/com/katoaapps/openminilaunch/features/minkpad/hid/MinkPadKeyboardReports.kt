package com.katoaapps.openminilaunch.features.minkpad.hid

import android.os.Handler
import com.katoaapps.openminilaunch.features.minkpad.input.HidKey
import com.katoaapps.openminilaunch.features.minkpad.input.UsKeyboardLayout
import java.util.concurrent.atomic.AtomicInteger

/** Worker-confined keyboard state. Physical events and software taps share one FIFO. */
internal class MinkPadKeyboardReports(private val handler: Handler, private val send: (ByteArray) -> Unit) {
    private sealed interface Input {
        data class Key(val usage: Int, val pressed: Boolean, val modifiers: Int) : Input
        data class Modifiers(val value: Int) : Input
        data class Tap(val key: HidKey, val reservedText: Boolean, val isolated: Boolean = false) : Input
    }

    private val queue = ArrayDeque<Input>()
    private val reservedCharacters = AtomicInteger()
    private val pressedKeys = linkedSetOf<Int>()
    private var physicalModifiers = 0
    private var stickyModifiers = 0
    private var tapInProgress = false
    private var releasingTap: Runnable? = null
    private var continuingInput: Runnable? = null
    @Volatile var latestReport = ByteArray(8)
        private set

    fun reserveText(text: String): List<HidKey>? {
        if (text.length > MAX_PENDING_CHARACTERS) return null
        // A Windows-style clipboard line ending represents one Enter, not two.
        val mapped = text.replace("\r\n", "\n").map { UsKeyboardLayout.key(it) ?: return null }
        if (reservedCharacters.addAndGet(mapped.size) > MAX_PENDING_CHARACTERS) {
            reservedCharacters.addAndGet(-mapped.size)
            return null
        }
        return mapped
    }

    fun cancelReservations() { reservedCharacters.set(0) }
    fun key(usage: Int, pressed: Boolean, modifiers: Int) {
        queue.addLast(Input.Key(usage, pressed, modifiers))
        drain()
    }
    fun modifiers(value: Int) { queue.addLast(Input.Modifiers(value)); drain() }
    fun tap(usage: Int) { queue.addLast(Input.Tap(HidKey(usage), false)); drain() }
    // Shortcut reports use their own modifiers, then restore physical/sticky state.
    fun shortcut(usage: Int, modifiers: Int) {
        queue.addLast(Input.Tap(HidKey(usage, modifiers), false, isolated = true))
        drain()
    }
    fun text(mapped: List<HidKey>) {
        mapped.forEach { queue.addLast(Input.Tap(it, true)) }
        drain()
    }

    private fun drain() {
        if (tapInProgress) return
        while (queue.isNotEmpty()) {
            when (val input = queue.removeFirst()) {
                is Input.Key -> {
                    physicalModifiers = input.modifiers
                    if (input.usage != 0) {
                        if (input.pressed) pressedKeys.add(input.usage) else pressedKeys.remove(input.usage)
                    }
                    report()
                }
                is Input.Modifiers -> { stickyModifiers = input.value; report() }
                is Input.Tap -> { sendTap(input); return }
            }
        }
    }

    private fun report(extra: HidKey? = null) {
        val keys = if (extra == null) pressedKeys else pressedKeys + extra.usage
        latestReport = UsKeyboardLayout.report(keys, physicalModifiers or stickyModifiers or (extra?.modifiers ?: 0))
        send(latestReport)
    }

    private fun sendTap(input: Input.Tap) {
        tapInProgress = true
        if (input.isolated) {
            latestReport = UsKeyboardLayout.report(listOf(input.key.usage).filter { it != 0 }, input.key.modifiers)
            send(latestReport)
        } else report(input.key)
        releasingTap = Runnable {
            report()
            if (input.reservedText) reservedCharacters.updateAndGet { (it - 1).coerceAtLeast(0) }
            continuingInput = Runnable { tapInProgress = false; drain() }
            handler.postDelayed(continuingInput!!, TAP_INTERVAL_MS)
        }
        handler.postDelayed(releasingTap!!, TAP_INTERVAL_MS)
    }

    fun reset() {
        releasingTap?.let(handler::removeCallbacks)
        continuingInput?.let(handler::removeCallbacks)
        releasingTap = null
        continuingInput = null
        tapInProgress = false
        queue.clear()
        pressedKeys.clear()
        physicalModifiers = 0
        stickyModifiers = 0
        report()
    }

    companion object {
        private const val MAX_PENDING_CHARACTERS = 2048
        private const val TAP_INTERVAL_MS = 8L
    }
}
