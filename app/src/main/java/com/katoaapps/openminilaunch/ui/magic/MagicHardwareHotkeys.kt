package com.katoaapps.openminilaunch.ui.magic

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.features.magic.MAGIC_HOTKEY_TRIGGERS
import com.katoaapps.openminilaunch.features.magic.printableHardwareText

/** Lets Home catch Magic Box hotkeys before focus routing on unusual physical keyboards. */
interface MagicHardwareHotkeyHost {
    fun registerMagicHardwareHotkeyHandler(handler: (KeyEvent) -> Boolean): () -> Unit
}

/** Registers only while the collapsed Home Magic Box is eligible to receive hotkeys. */
@Composable
internal fun MagicHardwareHotkeyEffect(
    enabled: Boolean,
    onHotkey: (trigger: String, keyCode: Int) -> Unit,
) {
    val host = LocalContext.current as? MagicHardwareHotkeyHost
    val currentOnHotkey by rememberUpdatedState(onHotkey)

    DisposableEffect(host, enabled) {
        if (!enabled || host == null) return@DisposableEffect onDispose { }

        val unregister = host.registerMagicHardwareHotkeyHandler { event ->
            val trigger = printableHardwareText(event.unicodeChar)
                ?.singleOrNull()
                ?.takeIf { it in MAGIC_HOTKEY_TRIGGERS }
            if (event.action != KeyEvent.ACTION_DOWN || trigger == null) {
                false
            } else {
                if (event.repeatCount == 0) currentOnHotkey(trigger.toString(), event.keyCode)
                true
            }
        }
        onDispose(unregister)
    }
}
