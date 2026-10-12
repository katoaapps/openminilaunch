package com.katoaapps.openminilaunch.ui.magic

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.features.magic.MAGIC_COMMAND_PREFIXES
import com.katoaapps.openminilaunch.features.magic.printableHardwareText

/** Lets Home catch command hotkeys before focus routing on unusual physical keyboards. */
interface MagicHardwareHotkeyHost {
    fun registerMagicHardwareHotkeyHandler(handler: (KeyEvent) -> Boolean): () -> Unit
}

/** Registers only while the collapsed Home Magic Box is eligible to receive command hotkeys. */
@Composable
internal fun MagicHardwareCommandHotkeyEffect(
    enabled: Boolean,
    onCommand: (prefix: String, keyCode: Int) -> Unit,
) {
    val host = LocalContext.current as? MagicHardwareHotkeyHost
    val currentOnCommand by rememberUpdatedState(onCommand)

    DisposableEffect(host, enabled) {
        if (!enabled || host == null) return@DisposableEffect onDispose { }

        val unregister = host.registerMagicHardwareHotkeyHandler { event ->
            val prefix = printableHardwareText(event.unicodeChar)
                ?.singleOrNull()
                ?.takeIf { it in MAGIC_COMMAND_PREFIXES }
            if (event.action != KeyEvent.ACTION_DOWN || prefix == null) {
                false
            } else {
                if (event.repeatCount == 0) currentOnCommand(prefix.toString(), event.keyCode)
                true
            }
        }
        onDispose(unregister)
    }
}
