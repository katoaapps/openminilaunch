package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.ui.keyboard.LauncherKeyboard

private const val KeyboardEnterDurationMillis = 180
private const val KeyboardExitDurationMillis = 120

/**
 * Keeps the launcher keyboard mounted while Magic Box moves between docked and expanded states.
 * Only true visibility changes animate, so typing the first character never restarts the motion.
 */
@Composable
internal fun MagicLauncherKeyboardHost(
    visible: Boolean,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    preferences: LauncherKeyboardPreferences,
    themePrimaryArgb: Int,
    themeSecondaryArgb: Int,
    onHotkey: (Char) -> Unit,
    onSubmit: () -> Unit,
    multiline: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxWidth(),
        enter = fadeIn(tween(KeyboardEnterDurationMillis)) + slideInVertically(
            animationSpec = tween(
                durationMillis = KeyboardEnterDurationMillis,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetY = { fullHeight -> fullHeight / 6 },
        ),
        exit = fadeOut(tween(KeyboardExitDurationMillis)) + slideOutVertically(
            animationSpec = tween(KeyboardExitDurationMillis),
            targetOffsetY = { fullHeight -> fullHeight / 8 },
        ),
    ) {
        LauncherKeyboard(
            value = value,
            onValueChange = onValueChange,
            preferences = preferences,
            themePrimaryArgb = themePrimaryArgb,
            themeSecondaryArgb = themeSecondaryArgb,
            onHotkey = onHotkey,
            onSubmit = onSubmit,
            multiline = multiline,
            modifier = Modifier.fillMaxWidth(),
            onOpenSettings = onOpenSettings,
            extendBehindNavigationBar = true,
        )
    }
}
