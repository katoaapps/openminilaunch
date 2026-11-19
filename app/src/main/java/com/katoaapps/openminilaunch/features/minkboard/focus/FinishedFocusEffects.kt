package com.katoaapps.openminilaunch.features.minkboard.focus

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Owns the bounded wake/vibration effects that accompany a finished focus timer. */
internal class FinishedFocusEffects(context: Context) {
    private val applicationContext = context.applicationContext

    fun start() {
        wakeSleepingDisplay()
        vibrateAsAlarm()
    }

    fun stop() {
        runCatching { systemVibrator().cancel() }
    }

    @Suppress("DEPRECATION")
    private fun wakeSleepingDisplay() {
        val powerManager = applicationContext.getSystemService(PowerManager::class.java)
        if (powerManager.isInteractive) return
        runCatching {
            powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "${applicationContext.packageName}:minkboard-focus-finished",
            ).acquire(SCREEN_WAKE_DURATION_MILLIS)
        }
    }

    private fun vibrateAsAlarm() {
        runCatching {
            val vibrator = systemVibrator()
            if (!vibrator.hasVibrator()) return
            val effect = VibrationEffect.createWaveform(SPENCER_VIBRATION_PATTERN, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }
    }

    private fun systemVibrator(): Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        applicationContext.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        applicationContext.getSystemService(Vibrator::class.java)
    }

    private companion object {
        const val SCREEN_WAKE_DURATION_MILLIS = 5_000L
        val SPENCER_VIBRATION_PATTERN = longArrayOf(0L, 1_000L, 500L, 1_000L)
    }
}
