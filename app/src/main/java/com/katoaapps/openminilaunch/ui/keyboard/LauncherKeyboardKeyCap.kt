package com.katoaapps.openminilaunch.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val LauncherKeyboardStyle.isFlatKeyboardStyle: Boolean
    get() = this == LauncherKeyboardStyle.FLAT_PRIMARY ||
        this == LauncherKeyboardStyle.FLAT_SECONDARY

@Composable
internal fun RowScope.KeyboardTextActionKey(
    label: String,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    selected: Boolean = false,
    solidWhenFlat: Boolean = false,
    weight: Float = 1f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    KeyboardKeyCap(
        popupLabel = label,
        secondaryLabel = secondaryLabel,
        selected = selected,
        solidWhenFlat = solidWhenFlat,
        colors = colors,
        style = style,
        haptic = haptic,
        weight = weight,
        modifier = modifier,
        onLongClick = onLongClick,
        onClick = onClick,
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 2) 11.sp else 15.sp,
            fontWeight = if (style.isFlatKeyboardStyle && solidWhenFlat) {
                FontWeight.Bold
            } else {
                FontWeight.SemiBold
            },
            maxLines = 1,
        )
    }
}

@Composable
internal fun RowScope.KeyboardActionKey(
    icon: ImageVector?,
    contentDescription: String,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    selected: Boolean = false,
    solidWhenFlat: Boolean = false,
    weight: Float = 1f,
    repeatOnHold: Boolean = false,
    onClick: () -> Unit,
) {
    KeyboardKeyCap(
        popupLabel = null,
        selected = selected,
        solidWhenFlat = solidWhenFlat,
        colors = colors,
        style = style,
        haptic = haptic,
        weight = weight,
        repeatOnHold = repeatOnHold,
        onClick = onClick,
    ) {
        Box(
            Modifier.fillMaxSize().semantics {
                this.contentDescription = contentDescription
            },
            contentAlignment = Alignment.Center,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.KeyboardKeyCap(
    popupLabel: String?,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    weight: Float,
    selected: Boolean,
    solidWhenFlat: Boolean,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    repeatOnHold: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val indication = LocalIndication.current
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(pressed) {
        if (pressed && haptic) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val raised = style == LauncherKeyboardStyle.RAISED_PRIMARY ||
        style == LauncherKeyboardStyle.RAISED_SECONDARY
    val outlined = style == LauncherKeyboardStyle.OUTLINED_PRIMARY ||
        style == LauncherKeyboardStyle.OUTLINED_SECONDARY
    val flat = style.isFlatKeyboardStyle
    val selectedColor = when {
        flat && solidWhenFlat && selected -> colors.key.copy(alpha = .76f)
        selected -> colors.content
        else -> colors.key
    }
    val selectedContent = if (selected && !flat) colors.key else colors.content
    val inputModifier = when {
        repeatOnHold -> Modifier
            .pointerInput(interactionSource) {
                coroutineScope {
                    val repeatScope = this
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val press = PressInteraction.Press(down.position)
                        interactionSource.tryEmit(press)
                        var repeated = false
                        val repeatJob = repeatScope.launch {
                            delay(360)
                            while (true) {
                                repeated = true
                                currentOnClick()
                                delay(75)
                            }
                        }
                        val up = waitForUpOrCancellation()
                        repeatJob.cancel()
                        if (up != null) {
                            if (!repeated) currentOnClick()
                            interactionSource.tryEmit(PressInteraction.Release(press))
                        } else {
                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                        }
                    }
                }
            }
            .semantics {
                onClick {
                    currentOnClick()
                    true
                }
            }

        onLongClick != null -> Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = indication,
            onClick = { currentOnClick() },
            onLongClick = { currentOnLongClick?.invoke() },
        )

        else -> Modifier.clickable(
            interactionSource = interactionSource,
            indication = indication,
            onClick = { currentOnClick() },
        )
    }

    Box(
        modifier = modifier.weight(weight).fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        if (flat && !solidWhenFlat) {
            Box(
                modifier = Modifier.fillMaxSize().then(inputModifier),
                contentAlignment = Alignment.Center,
            ) {
                KeyboardKeyContent(selectedContent, secondaryLabel, content)
                if (selected) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .width(18.dp)
                            .height(2.dp)
                            .background(colors.content),
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxSize().then(inputModifier),
                shape = if (flat && solidWhenFlat) {
                    RectangleShape
                } else {
                    RoundedCornerShape(if (raised) 10.dp else 7.dp)
                },
                color = selectedColor,
                contentColor = selectedContent,
                border = if (outlined) BorderStroke(1.25.dp, colors.outline) else null,
                shadowElevation = if (raised) 4.dp else 0.dp,
            ) {
                KeyboardKeyContent(selectedContent, secondaryLabel, content)
            }
        }
        if (pressed && popupLabel != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-36).dp).zIndex(10f),
                shape = RoundedCornerShape(10.dp),
                color = colors.key,
                contentColor = colors.content,
                shadowElevation = 8.dp,
                border = if (outlined) BorderStroke(1.25.dp, colors.outline) else null,
            ) {
                Text(
                    popupLabel,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun KeyboardKeyContent(
    contentColor: androidx.compose.ui.graphics.Color,
    secondaryLabel: String?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
            secondaryLabel?.let { secondary ->
                Text(
                    text = secondary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = .7f),
                )
            }
        }
    }
}
