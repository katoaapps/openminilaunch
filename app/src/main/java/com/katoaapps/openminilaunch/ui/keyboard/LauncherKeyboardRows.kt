package com.katoaapps.openminilaunch.ui.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.keyboard.LauncherKeyboardKey
import com.katoaapps.openminilaunch.features.keyboard.SplitKeyboardRow
import com.katoaapps.openminilaunch.features.keyboard.launcherKeyboardDisplayLabel
import com.katoaapps.openminilaunch.features.keyboard.splitLauncherKeyboardRow
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle

internal object LauncherKeyboardGeometry {
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

@Composable
internal fun RowScope.KeyboardSplitGroup(
    keys: List<LauncherKeyboardKey>,
    uppercaseLabels: Boolean,
    supportsCase: Boolean,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
    onAlternateKey: ((LauncherKeyboardKey) -> Unit)? = null,
    weight: Float,
    keyColors: ((LauncherKeyboardKey) -> LauncherKeyboardColors?)? = null,
    solidKeysWhenFlat: Boolean = false,
) {
    Row(
        Modifier.weight(weight).fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(
            if (style.isFlatKeyboardStyle && solidKeysWhenFlat) 0.dp else LauncherKeyboardGeometry.horizontalGap,
        ),
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
                solidWhenFlat = solidKeysWhenFlat,
                onLongClick = alternateKeyAction(key, onAlternateKey),
            ) { onKey(key) }
        }
    }
}

@Composable
internal fun ColumnScope.SplitKeyboardKeyRow(
    keys: List<LauncherKeyboardKey>,
    splitGap: Dp,
    uppercaseLabels: Boolean,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
    onAlternateKey: ((LauncherKeyboardKey) -> Unit)? = null,
    rowWeight: Float,
    keyColors: ((LauncherKeyboardKey) -> LauncherKeyboardColors?)? = null,
    solidKeysWhenFlat: Boolean = false,
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
            onAlternateKey = onAlternateKey,
            weight = leftKeys.size.toFloat(),
            keyColors = keyColors,
            solidKeysWhenFlat = solidKeysWhenFlat,
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
            onAlternateKey = onAlternateKey,
            weight = rightKeys.size.toFloat(),
            keyColors = keyColors,
            solidKeysWhenFlat = solidKeysWhenFlat,
        )
    }
}

@Composable
internal fun InvisibleReachKeyGap(
    splitRow: SplitKeyboardRow,
    width: Dp,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
) {
    Row(Modifier.width(width).fillMaxHeight()) {
        InvisibleReachKey(splitRow.leftReachKey, haptic, onKey)
        InvisibleReachKey(splitRow.rightReachKey, haptic, onKey)
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
                if (haptic) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKey(key)
            }
            .clearAndSetSemantics { },
    )
}

@Composable
internal fun ColumnScope.KeyboardKeyRow(
    keys: List<LauncherKeyboardKey>,
    uppercaseLabels: Boolean,
    supportsCase: Boolean = true,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onKey: (LauncherKeyboardKey) -> Unit,
    onAlternateKey: ((LauncherKeyboardKey) -> Unit)? = null,
    modifier: Modifier = Modifier,
    rowWeight: Float = LauncherKeyboardGeometry.letterRowWeight,
    centerOnTenKeyGrid: Boolean = false,
    keyColors: ((LauncherKeyboardKey) -> LauncherKeyboardColors?)? = null,
    solidKeysWhenFlat: Boolean = false,
) {
    val edgeInsetWeight = if (centerOnTenKeyGrid) ((10f - keys.size) / 2f).coerceAtLeast(0f) else 0f
    Row(
        modifier.fillMaxWidth().weight(rowWeight),
        horizontalArrangement = Arrangement.spacedBy(
            if (style.isFlatKeyboardStyle && solidKeysWhenFlat) 0.dp else LauncherKeyboardGeometry.horizontalGap,
        ),
    ) {
        if (edgeInsetWeight > 0f) Spacer(Modifier.weight(edgeInsetWeight))
        keys.forEach { key ->
            KeyboardTextActionKey(
                label = launcherKeyboardDisplayLabel(key, uppercaseLabels, supportsCase),
                secondaryLabel = key.secondaryLabel,
                colors = keyColors?.invoke(key) ?: colors,
                style = style,
                haptic = haptic,
                solidWhenFlat = solidKeysWhenFlat,
                onLongClick = alternateKeyAction(key, onAlternateKey),
            ) { onKey(key) }
        }
        if (edgeInsetWeight > 0f) Spacer(Modifier.weight(edgeInsetWeight))
    }
}

@Composable
internal fun ColumnScope.KeyboardBottomLetterRow(
    keys: List<LauncherKeyboardKey>,
    split: Boolean,
    splitGap: Dp,
    uppercaseLabels: Boolean,
    supportsCase: Boolean,
    shiftSelected: Boolean,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onShift: () -> Unit,
    onBackspace: () -> Unit,
    onKey: (LauncherKeyboardKey) -> Unit,
    onAlternateKey: (LauncherKeyboardKey) -> Unit,
) {
    val innerInsetWeight = ((7f - keys.size) / 2f).coerceAtLeast(0f)
    Row(
        Modifier.fillMaxWidth().weight(LauncherKeyboardGeometry.letterRowWeight),
        horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
    ) {
        KeyboardActionKey(
            icon = Icons.Default.KeyboardCapslock,
            selected = shiftSelected,
            contentDescription = stringResource(R.string.keyboard_key_shift),
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
                onAlternateKey = onAlternateKey,
                weight = splitRow.left.size.toFloat(),
            )
            InvisibleReachKeyGap(splitRow, splitGap, haptic, onKey)
            KeyboardSplitGroup(
                keys = splitRow.right,
                uppercaseLabels = uppercaseLabels,
                supportsCase = supportsCase,
                colors = colors,
                style = style,
                haptic = haptic,
                onKey = onKey,
                onAlternateKey = onAlternateKey,
                weight = splitRow.right.size.toFloat(),
            )
        } else {
            keys.forEach { key ->
                KeyboardTextActionKey(
                    label = launcherKeyboardDisplayLabel(key, uppercaseLabels, supportsCase),
                    secondaryLabel = key.secondaryLabel,
                    colors = colors,
                    style = style,
                    haptic = haptic,
                    onLongClick = alternateKeyAction(key, onAlternateKey),
                    onClick = { onKey(key) },
                )
            }
        }
        if (innerInsetWeight > 0f) Spacer(Modifier.weight(innerInsetWeight))
        KeyboardActionKey(
            icon = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = stringResource(R.string.keyboard_key_backspace),
            colors = colors,
            style = style,
            haptic = haptic,
            weight = LauncherKeyboardGeometry.modifierKeyWeight,
            repeatOnHold = true,
            onClick = onBackspace,
        )
    }
}

private fun alternateKeyAction(
    key: LauncherKeyboardKey,
    onAlternateKey: ((LauncherKeyboardKey) -> Unit)?,
): (() -> Unit)? = if (key.secondaryLabel != null && onAlternateKey != null) {
    { onAlternateKey(key) }
} else {
    null
}

@Composable
internal fun ColumnScope.KeyboardUtilityRow(
    split: Boolean,
    splitGap: Dp,
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
            Modifier.fillMaxWidth().weight(LauncherKeyboardGeometry.utilityRowWeight),
            horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
        ) {
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
            ) {
                SymbolKey(symbols, colors, style, haptic, onToggleSymbols, onOpenSettings)
                PunctuationKey(",", colors, style, haptic, onComma)
                SpaceKey(colors, style, haptic, onSpace)
            }
            Spacer(Modifier.width(splitGap))
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
            ) {
                SpaceKey(colors, style, haptic, onSpace)
                PunctuationKey(".", colors, style, haptic, onPeriod)
                SubmitKey(multiline, colors, style, haptic, onSubmit)
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().weight(LauncherKeyboardGeometry.utilityRowWeight),
            horizontalArrangement = Arrangement.spacedBy(LauncherKeyboardGeometry.horizontalGap),
        ) {
            SymbolKey(symbols, colors, style, haptic, onToggleSymbols, onOpenSettings)
            PunctuationKey(",", colors, style, haptic, onComma)
            SpaceKey(colors, style, haptic, onSpace)
            PunctuationKey(".", colors, style, haptic, onPeriod)
            SubmitKey(multiline, colors, style, haptic, onSubmit)
        }
    }
}

@Composable
private fun RowScope.SymbolKey(
    selected: Boolean,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) = KeyboardTextActionKey(
    label = "Sym",
    selected = selected,
    colors = colors,
    style = style,
    haptic = haptic,
    weight = 1.4f,
    onClick = onClick,
    onLongClick = onLongClick,
)

@Composable
private fun RowScope.PunctuationKey(
    label: String,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onClick: () -> Unit,
) = KeyboardTextActionKey(
    label = label,
    colors = colors,
    style = style,
    haptic = haptic,
    weight = .65f,
    onClick = onClick,
)

@Composable
private fun RowScope.SpaceKey(
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onClick: () -> Unit,
) = KeyboardActionKey(
    icon = null,
    contentDescription = stringResource(R.string.keyboard_key_space),
    colors = colors,
    style = style,
    haptic = haptic,
    weight = 4.6f,
    onClick = onClick,
)

@Composable
private fun RowScope.SubmitKey(
    multiline: Boolean,
    colors: LauncherKeyboardColors,
    style: LauncherKeyboardStyle,
    haptic: Boolean,
    onClick: () -> Unit,
) = KeyboardActionKey(
    icon = if (multiline) Icons.AutoMirrored.Filled.KeyboardReturn else Icons.Default.Search,
    contentDescription = stringResource(
        if (multiline) R.string.keyboard_key_new_line else R.string.keyboard_key_search,
    ),
    colors = colors,
    style = style,
    haptic = haptic,
    weight = 1.35f,
    onClick = onClick,
)
