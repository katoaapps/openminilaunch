package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

internal enum class HomeTourTarget {
    APP_GRID,
    MINK_SPACE,
    MAGIC_BOX,
    KEYBOARD_ACTION,
    HEADER,
    GESTURES,
}

/**
 * Keeps tutorial geometry out of the Home feature components. Targets only publish their bounds;
 * the tour owns all spotlight rendering and navigation.
 */
@Stable
internal class HomeTourTargetRegistry {
    private val targetBounds = mutableStateMapOf<HomeTourTarget, Rect>()

    operator fun get(target: HomeTourTarget): Rect? = targetBounds[target]

    internal fun update(target: HomeTourTarget, bounds: Rect) {
        targetBounds[target] = bounds
    }

    internal fun remove(target: HomeTourTarget) {
        targetBounds.remove(target)
    }
}

internal val LocalHomeTourTargetRegistry = staticCompositionLocalOf<HomeTourTargetRegistry?> { null }

@Composable
internal fun rememberHomeTourTargetRegistry(): HomeTourTargetRegistry = remember {
    HomeTourTargetRegistry()
}

internal fun Modifier.homeTourTarget(target: HomeTourTarget): Modifier = composed {
    val registry = LocalHomeTourTargetRegistry.current
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
