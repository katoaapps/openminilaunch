package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.ui.theme.Dimens

private const val MINK_SPACE_EXPANSION_DURATION_MILLIS = 320

private fun <T> minkSpaceExpansionSpec(): FiniteAnimationSpec<T> = tween(
    durationMillis = MINK_SPACE_EXPANSION_DURATION_MILLIS,
    easing = FastOutSlowInEasing,
)

@Immutable
internal data class MinkSpaceExpansionLayout(
    val contentPadding: Dp,
    val contentSpacing: Dp,
    val appGridWidth: Dp,
    val appGridAlpha: Float,
)

@Composable
internal fun animateMinkSpaceExpansionLayout(
    expanded: Boolean,
    compact: Boolean,
    availableWidth: Dp,
): MinkSpaceExpansionLayout {
    val collapsedPadding = if (compact) Dimens.dp10 else Dimens.dp14
    val collapsedSpacing = if (compact) Dimens.dp8 else Dimens.dp12
    val collapsedShortcutWidth = (
        (availableWidth - collapsedPadding * 2 - collapsedSpacing) / 3f
        ).coerceAtLeast(Dimens.dp0)
    val contentPadding by animateDpAsState(
        targetValue = if (expanded) Dimens.dp0 else collapsedPadding,
        animationSpec = minkSpaceExpansionSpec(),
        label = "MinkSpace panel padding",
    )
    val contentSpacing by animateDpAsState(
        targetValue = if (expanded) Dimens.dp0 else collapsedSpacing,
        animationSpec = minkSpaceExpansionSpec(),
        label = "MinkSpace panel spacing",
    )
    val appGridWidth by animateDpAsState(
        targetValue = if (expanded) Dimens.dp0 else collapsedShortcutWidth,
        animationSpec = minkSpaceExpansionSpec(),
        label = "MinkSpace shortcut width",
    )
    val appGridAlpha by animateFloatAsState(
        targetValue = if (expanded) 0f else 1f,
        animationSpec = minkSpaceExpansionSpec(),
        label = "MinkSpace shortcut alpha",
    )
    return MinkSpaceExpansionLayout(
        contentPadding = contentPadding,
        contentSpacing = contentSpacing,
        appGridWidth = appGridWidth,
        appGridAlpha = appGridAlpha,
    )
}

/**
 * Lets nested controls and the surrounding pager arbitrate the background expansion gesture.
 * A child action wins its tap, and a pager drag cancels this background tap.
 */
internal fun Modifier.minkSpaceExpansionBackgroundTap(
    expansion: MinkSpaceExpansionController,
    enabled: Boolean = expansion.enabled,
): Modifier = composed {
    val currentExpansion by rememberUpdatedState(expansion)
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        detectTapGestures(
            onTap = {
                if (currentExpansion.expanded) {
                    currentExpansion.collapse()
                } else {
                    currentExpansion.expand()
                }
            },
        )
    }
}
