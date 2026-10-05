package com.katoaapps.openminilaunch.features.minkpad.hid

/** Worker-confined pointer state. Split large movements without adding a delay per packet. */
internal class MinkPadPointerReports(private val send: (ByteArray) -> Unit) {
    private var buttons = 0
    private var x = 0f
    private var y = 0f
    private var wheel = 0f
    private var pan = 0f
    @Volatile var latestReport = ByteArray(5)
        private set

    fun move(dx: Float, dy: Float, vertical: Float, horizontal: Float): Int {
        if (!dx.isFinite() || !dy.isFinite() || !vertical.isFinite() || !horizontal.isFinite()) return 0
        x += dx
        y += dy
        wheel += vertical
        pan += horizontal
        return emitMotion()
    }

    fun button(button: Int, pressed: Boolean) {
        buttons = if (pressed) buttons or button else buttons and button.inv()
        // Movement is already drained before the next queued button operation.
        emit(0, 0, 0, 0)
    }

    private fun emitMotion(): Int {
        var dx = x.toInt()
        var dy = y.toInt()
        var vertical = wheel.toInt()
        var horizontal = pan.toInt()
        // Retain fractional counts, rather than dropping small movements between touch events.
        x -= dx
        y -= dy
        wheel -= vertical
        pan -= horizontal
        var packets = 0

        // The paired descriptor has signed eight-bit axes. Keep that contract and all travel,
        // but send its overflow packets in this worker operation, not one every eight ms.
        while (dx != 0 || dy != 0 || vertical != 0 || horizontal != 0) {
            val stepX = dx.coerceIn(-127, 127)
            val stepY = dy.coerceIn(-127, 127)
            val stepWheel = vertical.coerceIn(-127, 127)
            val stepPan = horizontal.coerceIn(-127, 127)
            emit(stepX, stepY, stepWheel, stepPan)
            packets++
            dx -= stepX
            dy -= stepY
            vertical -= stepWheel
            horizontal -= stepPan
        }
        return packets
    }

    private fun emit(dx: Int, dy: Int, vertical: Int, horizontal: Int) {
        latestReport = byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte(), vertical.toByte(), horizontal.toByte())
        send(latestReport)
    }

    fun reset() {
        buttons = 0
        x = 0f
        y = 0f
        wheel = 0f
        pan = 0f
        latestReport = ByteArray(5)
        send(latestReport)
    }
}
