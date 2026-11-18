package com.katoaapps.openminilaunch.ui.magic

import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val context = LocalContext.current
    // LocalContext may be a themed/configuration wrapper rather than MainActivity itself.
    // Resolve through that chain so collapsed Home hotkeys do not depend on a prior tap
    // giving the Compose focus target a chance to handle the event instead.
    val host = remember(context) { context.findMagicHardwareHotkeyHost() }
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

private fun Context.findMagicHardwareHotkeyHost(): MagicHardwareHotkeyHost? {
    var current: Context? = this
    while (current != null) {
        if (current is MagicHardwareHotkeyHost) return current
        current = (current as? ContextWrapper)
            ?.baseContext
            ?.takeUnless { it === current }
    }
    return null
}
