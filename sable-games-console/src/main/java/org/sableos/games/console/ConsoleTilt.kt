package org.sableos.games.console

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue

/**
 * The phone's accelerometer, read only while a tilt game or the levelling screen is showing. Readings
 * go through [TiltFilter]; [x] and [y] are the lean, and [onStep] gets each step.
 */
class ConsoleTilt(
    context: Context,
) : SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val filter = TiltFilter()
    val available: Boolean get() = sensor != null

    var x by mutableDoubleStateOf(0.0)
        private set
    var y by mutableDoubleStateOf(0.0)
        private set

    var onStep: ((TiltStep) -> Unit)? = null
    private var listening = false

    /** A screen wants readings; kept while the app is in the background so [resume] can restart them. */
    private var wanted = false

    fun start() {
        wanted = true
        listen()
    }

    fun stop() {
        wanted = false
        quiet()
    }

    /** The app went to the background: stop reading, but remember a screen wanted it. */
    fun suspend() = quiet()

    fun resume() {
        if (wanted) listen()
    }

    private fun listen() {
        if (listening || sensor == null) return
        listening = manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun quiet() {
        if (!listening) return
        manager?.unregisterListener(this)
        listening = false
    }

    fun level() {
        filter.level()
        x = 0.0
        y = 0.0
    }

    override fun onSensorChanged(event: SensorEvent) {
        val step = filter.feed(event.values[0].toDouble(), event.values[1].toDouble())
        x = filter.x
        y = filter.y
        step?.let { onStep?.invoke(it) }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit
}
