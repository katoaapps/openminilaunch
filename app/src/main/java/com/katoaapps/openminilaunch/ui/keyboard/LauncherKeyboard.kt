package com.katoaapps.openminilaunch.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.features.keyboard.LauncherKeyboardKey
import com.katoaapps.openminilaunch.features.keyboard.deleteLauncherKeyboardText
import com.katoaapps.openminilaunch.features.keyboard.insertLauncherKeyboardText
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardHotkeys
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardNumberKeys
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardOutput
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardRows
import com.katoaapps.openminilaunch.features.keyboard.shouldCapitalizeLauncherKeyboardSentence
import com.katoaapps.openminilaunch.features.keyboard.splitLauncherKeyboardRow
import com.katoaapps.openminilaunch.model.LauncherKeyboardColorSource
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle

internal data class LauncherKeyboardColors(
    val board: Color,
    val key: Color,
    val content: Color,
    val outline: Color,
)

internal fun resolveLauncherKeyboardColors(
    preferences: LauncherKeyboardPreferences,
    themePrimaryArgb: Int,
    themeSecondaryArgb: Int,
): LauncherKeyboardColors {
    val primary = Color(
        if (preferences.colorSource == LauncherKeyboardColorSource.CUSTOM) {
            preferences.customPrimaryArgb
        } else {
            themePrimaryArgb
        },
    )
    val secondary = Color(
        if (preferences.colorSource == LauncherKeyboardColorSource.CUSTOM) {
            preferences.customSecondaryArgb
        } else {
            themeSecondaryArgb
        },
    )
    val primaryKeys = preferences.style in setOf(
        LauncherKeyboardStyle.FLAT_PRIMARY,
        LauncherKeyboardStyle.RAISED_PRIMARY,
        LauncherKeyboardStyle.OUTLINED_PRIMARY,
    )
    val dominant = if (primaryKeys) primary else secondary
    val contrast = if (primaryKeys) secondary else primary
    val outlined = preferences.style == LauncherKeyboardStyle.OUTLINED_PRIMARY ||
        preferences.style == LauncherKeyboardStyle.OUTLINED_SECONDARY
    return LauncherKeyboardColors(
        board = if (outlined) dominant else contrast,
        key = dominant,
        content = contrast,
        outline = contrast,
    )
}

@Composable
internal fun LauncherKeyboard(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    preferences: LauncherKeyboardPreferences,
    themePrimaryArgb: Int,
    themeSecondaryArgb: Int,
    onSubmit: () -> Unit,
    multiline: Boolean,
    modifier: Modifier = Modifier,
) {
    var symbols by remember { mutableStateOf(false) }
    var shifted by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val split = preferences.splitOnWideDisplays && (
        configuration.screenWidthDp >= 600 ||
            configuration.screenWidthDp > configuration.screenHeightDp
        )
    val colors = resolveLauncherKeyboardColors(
        preferences = preferences,
        themePrimaryArgb = themePrimaryArgb,
        themeSecondaryArgb = themeSecondaryArgb,
    )
    val style = preferences.style
    val sentenceCaps = !symbols && preferences.layout.supportsCase &&
        shouldCapitalizeLauncherKeyboardSentence(value)

    fun insert(text: String) {
        onValueChange(insertLauncherKeyboardText(value, text))
    }

    fun insertKey(key: LauncherKeyboardKey) {
        insert(
            launcherKeyboardOutput(
                key = key,
                uppercase = shifted || sentenceCaps,
                supportsCase = preferences.layout.supportsCase,
            ),
        )
        if (shifted) shifted = false
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(preferences.heightDp.dp),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = colors.board,
        contentColor = colors.content,
        shadowElevation = 10.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val horizontalPadding = if (maxWidth < 400.dp) 5.dp else 8.dp
            val splitGap = if (maxWidth >= 600.dp) 46.dp else 26.dp
            Column(
                Modifier.fillMaxSize().padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 7.dp,
                    bottom = 6.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                KeyboardKeyRow(
                    keys = launcherKeyboardHotkeys.map(::LauncherKeyboardKey),
                    colors = colors,
                    style = style,
                    haptic = preferences.hapticFeedbackEnabled,
                    onKey = ::insertKey,
                )
                KeyboardKeyRow(
                    keys = launcherKeyboardNumberKeys(symbols),
                    colors = colors,
                    style = style,
                    haptic = preferences.hapticFeedbackEnabled,
                    onKey = ::insertKey,
                )
                launcherKeyboardRows(preferences.layout, symbols).forEach { row ->
                    if (split) {
                        val splitRow = splitLauncherKeyboardRow(row)
                        Row(
                            Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            KeyboardSplitGroup(
                                keys = splitRow.left,
                                colors = colors,
                                style = style,
                                haptic = preferences.hapticFeedbackEnabled,
                                onKey = ::insertKey,
                            )
                            Spacer(Modifier.width(splitGap))
                            KeyboardSplitGroup(
                                keys = splitRow.right,
                                colors = colors,
                                style = style,
                                haptic = preferences.hapticFeedbackEnabled,
                                onKey = ::insertKey,
                            )
                        }
                    } else {
                        KeyboardKeyRow(
                            keys = row,
                            colors = colors,
                            style = style,
                            haptic = preferences.hapticFeedbackEnabled,
                            onKey = ::insertKey,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    KeyboardActionKey(
                        icon = Icons.Default.KeyboardCapslock,
                        selected = shifted || sentenceCaps,
                        contentDescription = "Shift",
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        weight = 1.05f,
                    ) { shifted = !shifted }
                    KeyboardTextActionKey(
                        label = "Sym",
                        selected = symbols,
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        weight = 1.05f,
                    ) { symbols = !symbols }
                    KeyboardTextActionKey(
                        label = ",",
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        weight = .75f,
                    ) { insert(",") }
                    KeyboardActionKey(
                        icon = Icons.AutoMirrored.Filled.TrendingFlat,
                        contentDescription = "Space",
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        weight = 3.2f,
                    ) { insert(" ") }
                    KeyboardTextActionKey(
                        label = ".",
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        weight = .75f,
                    ) { insert(".") }
                    KeyboardActionKey(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        weight = 1.05f,
                        repeatableHint = true,
                    ) { onValueChange(deleteLauncherKeyboardText(value)) }
                    KeyboardActionKey(
                        icon = if (multiline) {
                            Icons.AutoMirrored.Filled.KeyboardReturn
                        } else {
                            Icons.Default.Search
                        },
                        contentDescription = if (multiline) "New line" else "Search",
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        weight = 1.05f,
                    ) {
                        if (multiline) insert("\n") else onSubmit()
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyboardSplitGroup(
    keys: List<LauncherKeyboardKey>,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
) {
    Row(
        Modifier.weight(1f).fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keys.forEach { key ->
            KeyboardTextActionKey(
                label = key.label,
                secondaryLabel = key.secondaryLabel,
                colors = colors,
                style = style,
                haptic = haptic,
            ) { onKey(key) }
        }
    }
}

@Composable
private fun ColumnScope.KeyboardKeyRow(
    keys: List<LauncherKeyboardKey>,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keys.forEach { key ->
            KeyboardTextActionKey(
                label = key.label,
                secondaryLabel = key.secondaryLabel,
                colors = colors,
                style = style,
                haptic = haptic,
            ) { onKey(key) }
        }
    }
}

@Composable
private fun RowScope.KeyboardTextActionKey(
    label: String,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    selected: Boolean = false,
    weight: Float = 1f,
    onClick: () -> Unit,
) {
    KeyboardKeyCap(
        popupLabel = label,
        secondaryLabel = secondaryLabel,
        selected = selected,
        colors = colors,
        style = style,
        haptic = haptic,
        weight = weight,
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 2) 11.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.KeyboardActionKey(
    icon: ImageVector,
    contentDescription: String,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    selected: Boolean = false,
    weight: Float = 1f,
    repeatableHint: Boolean = false,
    onClick: () -> Unit,
) {
    KeyboardKeyCap(
        popupLabel = null,
        selected = selected,
        colors = colors,
        style = style,
        haptic = haptic,
        weight = weight,
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp).alpha(if (repeatableHint) .92f else 1f),
        )
    }
}

@Composable
private fun RowScope.KeyboardKeyCap(
    popupLabel: String?,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    weight: Float,
    selected: Boolean,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
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
    val selectedColor = if (selected) colors.content else colors.key
    val selectedContent = if (selected) colors.key else colors.content

    Box(
        modifier = modifier.weight(weight).fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(if (raised) 10.dp else 7.dp),
            color = selectedColor,
            contentColor = selectedContent,
            border = if (outlined) BorderStroke(1.25.dp, colors.outline) else null,
            shadowElevation = if (raised) 4.dp else 0.dp,
            interactionSource = interactionSource,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                content()
                secondaryLabel?.let { secondary ->
                    Text(
                        text = secondary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = selectedContent.copy(alpha = .7f),
                    )
                }
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
