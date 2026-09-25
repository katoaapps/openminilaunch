package com.katoaapps.openminilaunch.ui.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.text.NumberFormat

@Composable
internal fun rememberBatteryPercentageText(): String? {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var batteryPercentage by remember {
        mutableIntStateOf(context.currentBatteryPercentage())
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                batteryPercentage = intent.batteryPercentage()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    return batteryPercentage.takeIf { it >= 0 }?.let { percentage ->
        remember(percentage, locale) {
            NumberFormat.getPercentInstance(locale).format(percentage / 100.0)
        }
    }
}

private fun Context.currentBatteryPercentage(): Int =
    registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.batteryPercentage() ?: -1

private fun Intent.batteryPercentage(): Int {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else -1
}
