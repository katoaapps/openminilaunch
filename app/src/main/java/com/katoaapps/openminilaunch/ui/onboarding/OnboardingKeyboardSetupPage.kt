package com.katoaapps.openminilaunch.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.model.resolveLauncherKeyboardHeightDp
import com.katoaapps.openminilaunch.ui.keyboard.LauncherKeyboard
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MagicAppColor
import com.katoaapps.openminilaunch.ui.theme.MagicCallColor
import com.katoaapps.openminilaunch.ui.theme.MagicEventColor
import com.katoaapps.openminilaunch.ui.theme.MagicNoteColor
import com.katoaapps.openminilaunch.ui.theme.MagicTextColor
import com.katoaapps.openminilaunch.ui.theme.MagicTodoColor
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import com.katoaapps.openminilaunch.ui.theme.readableContentColor
import kotlinx.coroutines.delay

private val PreviewHotkeys = setOf('@', '#', '-', '/', '+', '?')

@Composable
internal fun OnboardingKeyboardSetupPage(
    preferences: LauncherKeyboardPreferences,
    themePrimaryArgb: Int,
    themeSecondaryArgb: Int,
    onUseMinkKeyboard: (Boolean) -> Unit,
) {
    var previewValue by remember { mutableStateOf(TextFieldValue()) }
    var activeHotkey by remember { mutableStateOf<Char?>(null) }
    val configuration = LocalConfiguration.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val systemKeyboardFocusRequester = remember { FocusRequester() }

    LaunchedEffect(preferences.useMinkKeyboard) {
        activeHotkey = null
        if (preferences.useMinkKeyboard) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        } else {
            // Let AnimatedContent place the field before asking Android to attach its IME.
            delay(150)
            systemKeyboardFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fittedKeyboardHeight = (maxHeight - 248.dp).coerceIn(160.dp, 320.dp)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            Text(
                text = stringResource(R.string.onboarding_keyboard_description),
                fontSize = Dimens.sp15,
                maxLines = 2,
            )
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
            ) {
                KeyboardSourceChoice(
                    title = stringResource(R.string.onboarding_mink_keyboard_title),
                    description = stringResource(R.string.onboarding_mink_keyboard_description),
                    selected = preferences.useMinkKeyboard,
                    icon = { Icon(Icons.Default.Keyboard, null) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onUseMinkKeyboard(true) },
                )
                KeyboardSourceChoice(
                    title = stringResource(R.string.onboarding_system_keyboard_title),
                    description = stringResource(R.string.onboarding_system_keyboard_description),
                    selected = !preferences.useMinkKeyboard,
                    icon = { Icon(Icons.Default.PhoneAndroid, null) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onUseMinkKeyboard(false) },
                )
            }
            AnimatedContent(
                targetState = preferences.useMinkKeyboard,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding-keyboard-preview",
            ) { useMinkKeyboard ->
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp6)) {
                    Text(
                        text = stringResource(R.string.keyboard_live_preview),
                        color = Muted,
                        fontSize = Dimens.sp11,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MagicKeyboardPreviewField(
                        value = previewValue,
                        activeHotkey = activeHotkey,
                        systemInputEnabled = !useMinkKeyboard,
                        onValueChange = { nextValue ->
                            val trigger = nextValue.text.singleOrNull()
                                ?.takeIf { previewValue.text.isBlank() && it in PreviewHotkeys }
                            if (trigger != null) {
                                activeHotkey = trigger
                                previewValue = TextFieldValue()
                            } else {
                                activeHotkey = null
                                previewValue = nextValue
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = systemKeyboardFocusRequester,
                    )
                    if (useMinkKeyboard) {
                        val previewPreferences = preferences.copy(
                            heightDp = minOf(
                                resolveLauncherKeyboardHeightDp(
                                    preferences = preferences,
                                    screenWidthDp = configuration.screenWidthDp,
                                    screenHeightDp = configuration.screenHeightDp,
                                ),
                                fittedKeyboardHeight.value,
                            ),
                        )
                        LauncherKeyboard(
                            value = previewValue,
                            onValueChange = {
                                activeHotkey = null
                                previewValue = it
                            },
                            preferences = previewPreferences,
                            themePrimaryArgb = themePrimaryArgb,
                            themeSecondaryArgb = themeSecondaryArgb,
                            onSubmit = {},
                            multiline = false,
                            onHotkey = { trigger ->
                                if (previewValue.text.isBlank()) {
                                    activeHotkey = trigger
                                    true
                                } else {
                                    false
                                }
                            },
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.onboarding_keyboard_change_later),
                color = Muted,
                fontSize = Dimens.sp11,
                maxLines = 2,
            )
        }
    }
}

private data class KeyboardHotkeyPreview(
    @param:StringRes val labelRes: Int,
    val color: Color,
    val icon: ImageVector,
)

private fun keyboardHotkeyPreview(trigger: Char?): KeyboardHotkeyPreview? = when (trigger) {
    '@' -> KeyboardHotkeyPreview(
        R.string.onboarding_keyboard_hotkey_text,
        MagicTextColor,
        Icons.AutoMirrored.Filled.Send,
    )
    '#' -> KeyboardHotkeyPreview(
        R.string.onboarding_keyboard_hotkey_call,
        MagicCallColor,
        Icons.Default.Phone,
    )
    '-' -> KeyboardHotkeyPreview(
        R.string.onboarding_keyboard_hotkey_todo,
        MagicTodoColor,
        Icons.Default.Checklist,
    )
    '/' -> KeyboardHotkeyPreview(
        R.string.onboarding_keyboard_hotkey_note,
        MagicNoteColor,
        Icons.AutoMirrored.Filled.NoteAdd,
    )
    '+' -> KeyboardHotkeyPreview(
        R.string.onboarding_keyboard_hotkey_event,
        MagicEventColor,
        Icons.Default.Event,
    )
    '?' -> KeyboardHotkeyPreview(
        R.string.onboarding_keyboard_hotkey_app,
        MagicAppColor,
        Icons.Default.Apps,
    )
    else -> null
}

@Composable
private fun MagicKeyboardPreviewField(
    value: TextFieldValue,
    activeHotkey: Char?,
    systemInputEnabled: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val action = keyboardHotkeyPreview(activeHotkey)
    val backgroundColor by animateColorAsState(
        targetValue = action?.color ?: MaterialTheme.colorScheme.primary,
        label = "onboarding-magic-preview-color",
    )
    val contentColor by animateColorAsState(
        targetValue = readableContentColor(backgroundColor),
        label = "onboarding-magic-preview-content-color",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.dp24),
        color = backgroundColor,
        contentColor = contentColor,
        shadowElevation = Dimens.dp8,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Dimens.dp16, end = Dimens.dp6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            activeHotkey?.let { trigger ->
                Surface(
                    modifier = Modifier.size(Dimens.dp34),
                    shape = RoundedCornerShape(Dimens.dp12),
                    color = contentColor.copy(alpha = .14f),
                    contentColor = contentColor,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(trigger.toString(), fontWeight = FontWeight.Black)
                    }
                }
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Dimens.dp10, vertical = Dimens.dp12)
                    .then(
                        if (systemInputEnabled) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                    ),
                readOnly = !systemInputEnabled,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(contentColor),
                decorationBox = { innerTextField ->
                    when {
                        action != null -> Text(
                            text = stringResource(action.labelRes),
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                        )
                        value.text.isEmpty() -> Text(
                            text = stringResource(R.string.onboarding_keyboard_preview_hint),
                            color = contentColor.copy(alpha = .72f),
                        )
                        else -> innerTextField()
                    }
                },
            )
            Surface(
                modifier = Modifier.size(Dimens.dp40),
                shape = RoundedCornerShape(Dimens.dp20),
                color = contentColor.copy(alpha = .14f),
                contentColor = contentColor,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = action?.icon ?: Icons.Default.Search,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardSourceChoice(
    title: String,
    description: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.dp14),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (selected) {
            BorderStroke(Dimens.dp2, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(
            Modifier.padding(horizontal = Dimens.dp8, vertical = Dimens.dp8),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = Dimens.sp13,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Surface(
                modifier = Modifier.size(Dimens.dp40),
                shape = RoundedCornerShape(Dimens.dp12),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (selected) MaterialTheme.colorScheme.primary else Rust,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    icon()
                }
            }
            Text(
                description,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .78f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = Dimens.sp11,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
