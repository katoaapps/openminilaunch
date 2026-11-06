package com.katoaapps.openminilaunch.ui.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.keyboard.LauncherKeyboardKey
import com.katoaapps.openminilaunch.features.keyboard.deleteLauncherKeyboardText
import com.katoaapps.openminilaunch.features.keyboard.insertLauncherKeyboardText
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardDisplayLabel
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardHotkeys
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardNumberKeys
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardOutput
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardRows
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardUsesUppercase
import com.katoaapps.openminilaunch.features.keyboard.shouldCapitalizeLauncherKeyboardSentence
import com.katoaapps.openminilaunch.features.keyboard.splitLauncherKeyboardRow
import com.katoaapps.openminilaunch.model.LauncherKeyboardColorSource
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle
import com.katoaapps.openminilaunch.ui.theme.MagicAppColor
import com.katoaapps.openminilaunch.ui.theme.MagicCallColor
import com.katoaapps.openminilaunch.ui.theme.MagicEventColor
import com.katoaapps.openminilaunch.ui.theme.MagicNoteColor
import com.katoaapps.openminilaunch.ui.theme.MagicTextColor
import com.katoaapps.openminilaunch.ui.theme.MagicTodoColor
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.readableContentColor

internal data class LauncherKeyboardColors(
    val board: Color,
    val key: Color,
    val content: Color,
    val outline: Color,
)

private object LauncherKeyboardGeometry {
    val compactHorizontalPadding = 8.dp
    val wideHorizontalPadding = 10.dp
    val horizontalGap = 5.dp
    val verticalGap = 5.dp
    const val hotkeyRowWeight = .78f
    const val numberRowWeight = .9f
    const val letterRowWeight = 1f
    const val utilityRowWeight = 1.05f
    const val modifierKeyWeight = 1.35f
}
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
private fun LauncherKeyboardColors.forAction(
    actionColor: Color,
    contentColor: Color? = null,
): LauncherKeyboardColors {
    val resolvedContentColor = contentColor ?: readableContentColor(actionColor)
    return copy(
        key = actionColor,
        content = resolvedContentColor,
        outline = MinkWhite,
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
    onOpenSettings: () -> Unit = {},
    extendBehindNavigationBar: Boolean = false,
) {
    var symbols by remember { mutableStateOf(false) }
    var shiftOverride by remember { mutableStateOf<Boolean?>(null) }
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
    val uppercase = !symbols && preferences.layout.supportsCase &&
        launcherKeyboardUsesUppercase(shiftOverride, sentenceCaps)
    val hotkeyColors = mapOf(
        "@" to colors.forAction(MagicTextColor),
        "#" to colors.forAction(MagicCallColor),
        "-" to colors.forAction(MagicTodoColor, contentColor = MinkWhite),
        "/" to colors.forAction(MagicNoteColor),
        "+" to colors.forAction(MagicEventColor),
        "?" to colors.forAction(MagicAppColor),
    )
    val navigationBarBottom = if (extendBehindNavigationBar) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    fun insert(text: String) {
        onValueChange(insertLauncherKeyboardText(value, text))
    }

    fun insertKey(key: LauncherKeyboardKey) {
        insert(
            launcherKeyboardOutput(
                key = key,
                uppercase = uppercase,
                supportsCase = preferences.layout.supportsCase,
            ),
        )
        shiftOverride = null
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(preferences.heightDp.dp + navigationBarBottom),
        shape = RectangleShape,
        color = colors.board,
        contentColor = colors.content,
        shadowElevation = 10.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val horizontalPadding = if (maxWidth < 400.dp) {
                LauncherKeyboardGeometry.compactHorizontalPadding
            } else {
                LauncherKeyboardGeometry.wideHorizontalPadding
            }
            val splitGap = (maxWidth * .18f).coerceIn(72.dp, 180.dp)
            val keyboardRows = launcherKeyboardRows(preferences.layout, symbols)
            Column(
                Modifier.fillMaxSize().padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 7.dp,
                    bottom = 6.dp + navigationBarBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.verticalGap),
            ) {
                val hotkeyKeys = launcherKeyboardHotkeys.map(::LauncherKeyboardKey)
                val numberKeys = launcherKeyboardNumberKeys(symbols)
                if (split) {
                    SplitKeyboardKeyRow(
                        keys = hotkeyKeys,
                        splitGap = splitGap,
                        uppercaseLabels = false,
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        onKey = ::insertKey,
                        rowWeight = LauncherKeyboardGeometry.hotkeyRowWeight,
                        keyColors = { key -> hotkeyColors[key.label] },
                    )
                    SplitKeyboardKeyRow(
                        keys = numberKeys,
                        splitGap = splitGap,
                        uppercaseLabels = false,
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        onKey = ::insertKey,
                        rowWeight = LauncherKeyboardGeometry.numberRowWeight,
                    )
                } else {
                    KeyboardKeyRow(
                        keys = hotkeyKeys,
                        uppercaseLabels = false,
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        onKey = ::insertKey,
                        rowWeight = LauncherKeyboardGeometry.hotkeyRowWeight,
                        keyColors = { key -> hotkeyColors[key.label] },
                    )
                    KeyboardKeyRow(
                        keys = numberKeys,
                        uppercaseLabels = false,
                        colors = colors,
                        style = style,
                        haptic = preferences.hapticFeedbackEnabled,
                        onKey = ::insertKey,
                        rowWeight = LauncherKeyboardGeometry.numberRowWeight,
                    )
                }
                keyboardRows.forEachIndexed { rowIndex, row ->
                    val isBottomLetterRow = rowIndex == keyboardRows.lastIndex
                    when {
                        isBottomLetterRow -> KeyboardBottomLetterRow(
                            keys = row,
                            split = split,
                            splitGap = splitGap,
                            uppercaseLabels = uppercase,
                            supportsCase = preferences.layout.supportsCase,
                            shiftSelected = uppercase,
                            colors = colors,
                            style = style,
                            haptic = preferences.hapticFeedbackEnabled,
                            onShift = {
                                if (!symbols && preferences.layout.supportsCase) {
                                    shiftOverride = !uppercase
                                }
                            },
                            onBackspace = { onValueChange(deleteLauncherKeyboardText(value)) },
                            onKey = ::insertKey,
                        )

                        split -> {
                            val splitRow = splitLauncherKeyboardRow(row)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(LauncherKeyboardGeometry.letterRowWeight),
                                horizontalArrangement = Arrangement.spacedBy(
                                    LauncherKeyboardGeometry.horizontalGap,
                                ),
                            ) {
                                KeyboardSplitGroup(
                                    keys = splitRow.left,
                                    uppercaseLabels = uppercase,
                                    supportsCase = preferences.layout.supportsCase,
                                    colors = colors,
                                    style = style,
                                    haptic = preferences.hapticFeedbackEnabled,
                                    onKey = ::insertKey,
                                    weight = splitRow.left.size.toFloat(),
                                )
                                InvisibleReachKeyGap(
                                    splitRow = splitRow,
                                    width = splitGap,
                                    haptic = preferences.hapticFeedbackEnabled,
                                    onKey = ::insertKey,
                                )
                                KeyboardSplitGroup(
                                    keys = splitRow.right,
                                    uppercaseLabels = uppercase,
                                    supportsCase = preferences.layout.supportsCase,
                                    colors = colors,
                                    style = style,
                                    haptic = preferences.hapticFeedbackEnabled,
                                    onKey = ::insertKey,
                                    weight = splitRow.right.size.toFloat(),
                                )
                            }
                        }

                        else -> {
                            KeyboardKeyRow(
                                keys = row,
                                uppercaseLabels = uppercase,
                                supportsCase = preferences.layout.supportsCase,
                                colors = colors,
                                style = style,
                                haptic = preferences.hapticFeedbackEnabled,
                                onKey = ::insertKey,
                                rowWeight = LauncherKeyboardGeometry.letterRowWeight,
                                centerOnTenKeyGrid = true,
                            )
                        }
                    }
                }
                KeyboardUtilityRow(
                    split = split,
                    splitGap = splitGap,
                    symbols = symbols,
                    multiline = multiline,
                    colors = colors,
                    style = style,
                    haptic = preferences.hapticFeedbackEnabled,
                    onToggleSymbols = {
                        symbols = !symbols
                        shiftOverride = null
                    },
                    onOpenSettings = onOpenSettings,
                    onComma = { insert(",") },
                    onSpace = { insert(" ") },
                    onPeriod = { insert(".") },
                    onSubmit = {
                        if (multiline) insert("\n") else onSubmit()
                    },
                )
            }
        }
    }
}

@Composable
private fun RowScope.KeyboardSplitGroup(
    keys: List<LauncherKeyboardKey>,
    uppercaseLabels: Boolean,
    supportsCase: Boolean,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
    weight: Float,
    keyColors: ((LauncherKeyboardKey) -> LauncherKeyboardColors?)? = null,
) {
    Row(
        Modifier.weight(weight).fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
    ) {
        keys.forEach { key ->
            val displayLabel = launcherKeyboardDisplayLabel(
                key = key,
                uppercase = uppercaseLabels,
                supportsCase = supportsCase,
            )
            KeyboardTextActionKey(
                label = displayLabel,
                secondaryLabel = key.secondaryLabel,
                colors = keyColors?.invoke(key) ?: colors,
                style = style,
                haptic = haptic,
            ) { onKey(key) }
        }
    }
}

@Composable
private fun ColumnScope.SplitKeyboardKeyRow(
    keys: List<LauncherKeyboardKey>,
    splitGap: androidx.compose.ui.unit.Dp,
    uppercaseLabels: Boolean,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
    rowWeight: Float,
    keyColors: ((LauncherKeyboardKey) -> LauncherKeyboardColors?)? = null,
) {
    val splitIndex = (keys.size + 1) / 2
    val leftKeys = keys.take(splitIndex)
    val rightKeys = keys.drop(splitIndex)
    Row(
        Modifier.fillMaxWidth().weight(rowWeight),
        horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
    ) {
        KeyboardSplitGroup(
            keys = leftKeys,
            uppercaseLabels = uppercaseLabels,
            supportsCase = false,
            colors = colors,
            style = style,
            haptic = haptic,
            onKey = onKey,
            weight = leftKeys.size.toFloat(),
            keyColors = keyColors,
        )
        Spacer(Modifier.width(splitGap))
        KeyboardSplitGroup(
            keys = rightKeys,
            uppercaseLabels = uppercaseLabels,
            supportsCase = false,
            colors = colors,
            style = style,
            haptic = haptic,
            onKey = onKey,
            weight = rightKeys.size.toFloat(),
            keyColors = keyColors,
        )
    }
}

@Composable
private fun InvisibleReachKeyGap(
    splitRow: com.katoaapps.openminilaunch.features.keyboard.SplitKeyboardRow,
    width: androidx.compose.ui.unit.Dp,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
) {
    Row(Modifier.width(width).fillMaxHeight()) {
        InvisibleReachKey(
            key = splitRow.leftReachKey,
            haptic = haptic,
            onKey = onKey,
        )
        InvisibleReachKey(
            key = splitRow.rightReachKey,
            haptic = haptic,
            onKey = onKey,
        )
    }
}

@Composable
private fun RowScope.InvisibleReachKey(
    key: LauncherKeyboardKey?,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
) {
    if (key == null) {
        Spacer(Modifier.weight(1f))
        return
    }
    val hapticFeedback = LocalHapticFeedback.current
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (haptic) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onKey(key)
            }
            .clearAndSetSemantics { },
    )
}

@Composable
private fun ColumnScope.KeyboardKeyRow(
    keys: List<LauncherKeyboardKey>,
    uppercaseLabels: Boolean,
    supportsCase: Boolean = true,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
    modifier: Modifier = Modifier,
    rowWeight: Float = LauncherKeyboardGeometry.letterRowWeight,
    centerOnTenKeyGrid: Boolean = false,
    keyColors: ((LauncherKeyboardKey) -> LauncherKeyboardColors?)? = null,
) {
    val edgeInsetWeight = if (centerOnTenKeyGrid) {
        ((10f - keys.size) / 2f).coerceAtLeast(0f)
    } else {
        0f
    }
    Row(
        modifier.fillMaxWidth().weight(rowWeight),
        horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
    ) {
        if (edgeInsetWeight > 0f) Spacer(Modifier.weight(edgeInsetWeight))
        keys.forEach { key ->
            val displayLabel = launcherKeyboardDisplayLabel(
                key = key,
                uppercase = uppercaseLabels,
                supportsCase = supportsCase,
            )
            KeyboardTextActionKey(
                label = displayLabel,
                secondaryLabel = key.secondaryLabel,
                colors = keyColors?.invoke(key) ?: colors,
                style = style,
                haptic = haptic,
            ) { onKey(key) }
        }
        if (edgeInsetWeight > 0f) Spacer(Modifier.weight(edgeInsetWeight))
    }
}

@Composable
private fun ColumnScope.KeyboardBottomLetterRow(
    keys: List<LauncherKeyboardKey>,
    split: Boolean,
    splitGap: androidx.compose.ui.unit.Dp,
    uppercaseLabels: Boolean,
    supportsCase: Boolean,
    shiftSelected: Boolean,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onShift: () -> Unit,
    onBackspace: () -> Unit,
    onKey: (LauncherKeyboardKey) -> Unit,
) {
    val innerInsetWeight = ((7f - keys.size) / 2f).coerceAtLeast(0f)
    Row(
        Modifier
            .fillMaxWidth()
            .weight(LauncherKeyboardGeometry.letterRowWeight),
        horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
    ) {
        KeyboardActionKey(
            icon = Icons.Default.KeyboardCapslock,
            selected = shiftSelected,
            contentDescription = "Shift",
            colors = colors,
            style = style,
            haptic = haptic,
            weight = LauncherKeyboardGeometry.modifierKeyWeight,
            onClick = onShift,
        )
        if (innerInsetWeight > 0f) Spacer(Modifier.weight(innerInsetWeight))
        if (split) {
            val splitRow = splitLauncherKeyboardRow(keys)
            KeyboardSplitGroup(
                keys = splitRow.left,
                uppercaseLabels = uppercaseLabels,
                supportsCase = supportsCase,
                colors = colors,
                style = style,
                haptic = haptic,
                onKey = onKey,
                weight = splitRow.left.size.toFloat(),
            )
            InvisibleReachKeyGap(
                splitRow = splitRow,
                width = splitGap,
                haptic = haptic,
                onKey = onKey,
            )
            KeyboardSplitGroup(
                keys = splitRow.right,
                uppercaseLabels = uppercaseLabels,
                supportsCase = supportsCase,
                colors = colors,
                style = style,
                haptic = haptic,
                onKey = onKey,
                weight = splitRow.right.size.toFloat(),
            )
        } else {
            keys.forEach { key ->
                KeyboardTextActionKey(
                    label = launcherKeyboardDisplayLabel(
                        key = key,
                        uppercase = uppercaseLabels,
                        supportsCase = supportsCase,
                    ),
                    secondaryLabel = key.secondaryLabel,
                    colors = colors,
                    style = style,
                    haptic = haptic,
                    onClick = { onKey(key) },
                )
            }
        }
        if (innerInsetWeight > 0f) Spacer(Modifier.weight(innerInsetWeight))
        KeyboardActionKey(
            icon = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            colors = colors,
            style = style,
            haptic = haptic,
            weight = LauncherKeyboardGeometry.modifierKeyWeight,
            repeatOnHold = true,
            onClick = onBackspace,
        )
    }
}

@Composable
private fun ColumnScope.KeyboardUtilityRow(
    split: Boolean,
    splitGap: androidx.compose.ui.unit.Dp,
    symbols: Boolean,
    multiline: Boolean,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onToggleSymbols: () -> Unit,
    onOpenSettings: () -> Unit,
    onComma: () -> Unit,
    onSpace: () -> Unit,
    onPeriod: () -> Unit,
    onSubmit: () -> Unit,
) {
    if (split) {
        Row(
            Modifier
                .fillMaxWidth()
                .weight(LauncherKeyboardGeometry.utilityRowWeight),
            horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
        ) {
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(
                    LauncherKeyboardGeometry.horizontalGap,
                ),
            ) {
                KeyboardTextActionKey(
                    label = "Sym",
                    selected = symbols,
                    colors = colors,
                    style = style,
                    haptic = haptic,
                    weight = 1.4f,
                    onClick = onToggleSymbols,
                    onLongClick = onOpenSettings,
                )
                KeyboardTextActionKey(
                    label = ",",
                    colors = colors,
                    style = style,
                    haptic = haptic,
                    weight = .65f,
                    onClick = onComma,
                )
                KeyboardActionKey(
                    icon = Icons.AutoMirrored.Filled.TrendingFlat,
                    contentDescription = "Space",
                    colors = colors,
                    style = style,
                    haptic = haptic,
                    weight = 4.6f,
                    onClick = onSpace,
                )
            }
            Spacer(Modifier.width(splitGap))
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(
                    LauncherKeyboardGeometry.horizontalGap,
                ),
            ) {
                KeyboardActionKey(
                    icon = Icons.AutoMirrored.Filled.TrendingFlat,
                    contentDescription = "Space",
                    colors = colors,
                    style = style,
                    haptic = haptic,
                    weight = 4.6f,
                    onClick = onSpace,
                )
                KeyboardTextActionKey(
                    label = ".",
                    colors = colors,
                    style = style,
                    haptic = haptic,
                    weight = .65f,
                    onClick = onPeriod,
                )
                KeyboardActionKey(
                    icon = if (multiline) {
                        Icons.AutoMirrored.Filled.KeyboardReturn
                    } else {
                        Icons.Default.Search
                    },
                    contentDescription = if (multiline) "New line" else "Search",
                    colors = colors,
                    style = style,
                    haptic = haptic,
                    weight = 1.35f,
                    onClick = onSubmit,
                )
            }
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .weight(LauncherKeyboardGeometry.utilityRowWeight),
            horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
        ) {
            KeyboardTextActionKey(
                label = "Sym",
                selected = symbols,
                colors = colors,
                style = style,
                haptic = haptic,
                weight = 1.4f,
                onClick = onToggleSymbols,
                onLongClick = onOpenSettings,
            )
            KeyboardTextActionKey(
                label = ",",
                colors = colors,
                style = style,
                haptic = haptic,
                weight = .65f,
                onClick = onComma,
            )
            KeyboardActionKey(
                icon = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = "Space",
                colors = colors,
                style = style,
                haptic = haptic,
                weight = 4.6f,
                onClick = onSpace,
            )
            KeyboardTextActionKey(
                label = ".",
                colors = colors,
                style = style,
                haptic = haptic,
                weight = .65f,
                onClick = onPeriod,
            )
            KeyboardActionKey(
                icon = if (multiline) {
                    Icons.AutoMirrored.Filled.KeyboardReturn
                } else {
                    Icons.Default.Search
                },
                contentDescription = if (multiline) "New line" else "Search",
                colors = colors,
                style = style,
                haptic = haptic,
                weight = 1.35f,
                onClick = onSubmit,
            )
        }
    }
}
