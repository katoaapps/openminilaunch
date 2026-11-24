package com.katoaapps.openminilaunch.ui.magic.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

internal enum class MagicBoxTourTarget {
    MESSAGE_HOTKEY,
    CALL_HOTKEY,
    TODO_HOTKEY,
    NOTE_HOTKEY,
    EVENT_HOTKEY,
    APP_HOTKEY,
    APP_RESULT,
    DOCUMENT_SEARCH,
    WEB_SEARCH,
    AI_SEARCH,
}

/** Geometry published by Magic Box components for the first-open spotlight lesson. */
@Stable
internal class MagicBoxTourTargetRegistry {
    private val targetBounds = mutableStateMapOf<MagicBoxTourTarget, Rect>()

    operator fun get(target: MagicBoxTourTarget): Rect? = targetBounds[target]

    internal fun update(target: MagicBoxTourTarget, bounds: Rect) {
        targetBounds[target] = bounds
    }

    internal fun remove(target: MagicBoxTourTarget) {
        targetBounds.remove(target)
    }
}

@Composable
internal fun rememberMagicBoxTourTargetRegistry(): MagicBoxTourTargetRegistry = remember {
    MagicBoxTourTargetRegistry()
}

internal fun Modifier.magicBoxTourTarget(
    registry: MagicBoxTourTargetRegistry?,
    target: MagicBoxTourTarget,
): Modifier = composed {
    DisposableEffect(registry, target) {
        onDispose { registry?.remove(target) }
    }
    if (registry == null) {
        this
    } else {
        onGloballyPositioned { coordinates ->
            registry.update(target, coordinates.boundsInRoot())
        }
    }
}
