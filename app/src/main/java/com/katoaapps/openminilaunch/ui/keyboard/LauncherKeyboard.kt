package com.katoaapps.openminilaunch.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.keyboard.LauncherKeyboardKey
import com.katoaapps.openminilaunch.features.keyboard.deleteLauncherKeyboardText
import com.katoaapps.openminilaunch.features.keyboard.insertLauncherKeyboardText
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardHotkeys
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardNumberKeys
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardOutput
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardRows
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardUsesUppercase
import com.katoaapps.openminilaunch.features.keyboard.shouldCapitalizeLauncherKeyboardSentence
import com.katoaapps.openminilaunch.features.keyboard.splitLauncherKeyboardRow
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.ui.theme.MagicAppColor
import com.katoaapps.openminilaunch.ui.theme.MagicCallColor
import com.katoaapps.openminilaunch.ui.theme.MagicEventColor
import com.katoaapps.openminilaunch.ui.theme.MagicNoteColor
import com.katoaapps.openminilaunch.ui.theme.MagicTextColor
import com.katoaapps.openminilaunch.ui.theme.MagicTodoColor
import com.katoaapps.openminilaunch.ui.theme.MinkWhite

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
    var editValue by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        editValue = value
    }
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
        shouldCapitalizeLauncherKeyboardSentence(editValue)
    val uppercase = !symbols && preferences.layout.supportsCase &&
        launcherKeyboardUsesUppercase(shiftOverride, sentenceCaps)
    @Composable
    fun actionColors(actionColor: Color, contentColor: Color? = null) =
        colors.forAction(actionColor, contentColor)
    val hotkeyColors = mapOf(
        "@" to actionColors(MagicTextColor),
        "#" to actionColors(MagicCallColor),
        "-" to actionColors(MagicTodoColor, contentColor = MinkWhite),
        "/" to actionColors(MagicNoteColor),
        "+" to actionColors(MagicEventColor),
        "?" to actionColors(MagicAppColor),
    )
    val navigationBarBottom = if (extendBehindNavigationBar) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    fun updateValue(transform: (TextFieldValue) -> TextFieldValue) {
        val updated = transform(editValue)
        editValue = updated
        onValueChange(updated)
    }

    fun insert(text: String) {
        updateValue { current -> insertLauncherKeyboardText(current, text) }
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

    fun insertAlternateKey(key: LauncherKeyboardKey) {
        key.secondaryLabel?.let(::insert)
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
                    start = if (style.isFlatKeyboardStyle) 0.dp else horizontalPadding,
                    end = if (style.isFlatKeyboardStyle) 0.dp else horizontalPadding,
                    top = if (style.isFlatKeyboardStyle) 0.dp else 7.dp,
                    bottom = if (style.isFlatKeyboardStyle) {
                        navigationBarBottom
                    } else {
                        6.dp + navigationBarBottom
                    },
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
                        solidKeysWhenFlat = true,
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
                        solidKeysWhenFlat = true,
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
                            onBackspace = {
                                updateValue(::deleteLauncherKeyboardText)
                            },
                            onKey = ::insertKey,
                            onAlternateKey = ::insertAlternateKey,
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
                                    onAlternateKey = ::insertAlternateKey,
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
                                    onAlternateKey = ::insertAlternateKey,
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
                                onAlternateKey = ::insertAlternateKey,
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
