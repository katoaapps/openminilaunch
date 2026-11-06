package com.katoaapps.openminilaunch.features.keyboard

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.katoaapps.openminilaunch.model.LauncherKeyboardLayout
import java.util.Locale

internal data class LauncherKeyboardKey(
    val label: String,
    val output: String = label,
    val secondaryLabel: String? = null,
)

internal data class SplitKeyboardRow(
    val left: List<LauncherKeyboardKey>,
    val right: List<LauncherKeyboardKey>,
    val leftReachKey: LauncherKeyboardKey?,
    val rightReachKey: LauncherKeyboardKey?,
)

internal val launcherKeyboardHotkeys = listOf("@", "#", "-", "/", "+", "?")
internal val launcherKeyboardNumbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val numberAlternates = listOf("!", "@", "#", "\$", "%", "^", "&", "*", "(", ")")

private val symbolRows = listOf(
    listOf("[", "]", "{", "}", "\\", "|", "~", "^", "`", "•"),
    listOf(";", ":", "\"", "'", "<", ">", "€", "£", "¥"),
    listOf("_", "=", "*", "&", "%", "\$", "!"),
)

internal fun launcherKeyboardNumberKeys(symbols: Boolean): List<LauncherKeyboardKey> =
    launcherKeyboardNumbers.mapIndexed { index, number ->
        if (symbols) {
            LauncherKeyboardKey(numberAlternates[index])
        } else {
            LauncherKeyboardKey(number)
        }
    }

internal fun launcherKeyboardRows(
    layout: LauncherKeyboardLayout,
    symbols: Boolean,
): List<List<LauncherKeyboardKey>> {
    if (symbols) return symbolRows.map { row -> row.map(::LauncherKeyboardKey) }
    return layoutRows.getValue(layout).mapIndexed { rowIndex, row ->
        row.mapIndexed { keyIndex, label ->
            LauncherKeyboardKey(
                label = label,
                secondaryLabel = symbolRows.getOrNull(rowIndex)?.getOrNull(keyIndex),
            )
        }
    }
}

internal fun splitLauncherKeyboardRow(row: List<LauncherKeyboardKey>): SplitKeyboardRow {
    if (row.size < 2) {
        return SplitKeyboardRow(
            left = row,
            right = emptyList(),
            leftReachKey = null,
            rightReachKey = null,
        )
    }
    val splitIndex = (row.size + 1) / 2
    val left = row.take(splitIndex)
    val right = row.drop(splitIndex)
    return SplitKeyboardRow(
        left = left,
        right = right,
        // The center gap repeats the two boundary keys as invisible touch targets. This improves
        // thumb reach without visually presenting duplicate letters to the user.
        leftReachKey = right.firstOrNull(),
        rightReachKey = left.lastOrNull(),
    )
}

internal fun insertLauncherKeyboardText(
    value: TextFieldValue,
    insertedText: String,
): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    val updated = value.text.replaceRange(start, end, insertedText)
    val cursor = start + insertedText.length
    return value.copy(text = updated, selection = TextRange(cursor))
}

internal fun deleteLauncherKeyboardText(value: TextFieldValue): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    if (start != end) {
        return value.copy(
            text = value.text.removeRange(start, end),
            selection = TextRange(start),
        )
    }
    if (start == 0) return value
    val previous = value.text.offsetByCodePoints(start, -1)
    return value.copy(
        text = value.text.removeRange(previous, start),
        selection = TextRange(previous),
    )
}

internal fun shouldCapitalizeLauncherKeyboardSentence(value: TextFieldValue): Boolean {
    val beforeCursor = value.text.take(value.selection.min.coerceIn(0, value.text.length)).trimEnd()
    return beforeCursor.isEmpty() || beforeCursor.last() in SENTENCE_ENDINGS
}

internal fun launcherKeyboardOutput(
    key: LauncherKeyboardKey,
    uppercase: Boolean,
    supportsCase: Boolean,
): String = if (uppercase && supportsCase) {
    key.output.uppercase(Locale.ROOT)
} else {
    key.output
}

internal fun launcherKeyboardDisplayLabel(
    key: LauncherKeyboardKey,
    uppercase: Boolean,
    supportsCase: Boolean,
): String = if (uppercase && supportsCase) {
    key.label.uppercase(Locale.ROOT)
} else {
    key.label
}

internal fun launcherKeyboardUsesUppercase(
    shiftOverride: Boolean?,
    sentenceCaps: Boolean,
): Boolean = shiftOverride ?: sentenceCaps

private val SENTENCE_ENDINGS = setOf('.', '!', '?', '\n')

private val layoutRows = mapOf(
    LauncherKeyboardLayout.QWERTY to listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m"),
    ),
    LauncherKeyboardLayout.AZERTY to listOf(
        listOf("a", "z", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("q", "s", "d", "f", "g", "h", "j", "k", "l", "m"),
        listOf("w", "x", "c", "v", "b", "n"),
    ),
    LauncherKeyboardLayout.QWERTZ to listOf(
        listOf("q", "w", "e", "r", "t", "z", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("y", "x", "c", "v", "b", "n", "m"),
    ),
    LauncherKeyboardLayout.DVORAK to listOf(
        listOf("'", ",", ".", "p", "y", "f", "g", "c", "r", "l"),
        listOf("a", "o", "e", "u", "i", "d", "h", "t", "n", "s"),
        listOf(";", "q", "j", "k", "x", "b", "m", "w", "v", "z"),
    ),
    // Pinyin uses a Latin phonetic key arrangement. Candidate conversion is intentionally outside
    // this launcher keyboard's first-pass scope; the typed phonetics still search and dispatch.
    LauncherKeyboardLayout.PINYIN to listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m"),
    ),
    LauncherKeyboardLayout.CANGJIE to listOf(
        listOf("日", "月", "金", "木", "水", "火", "土", "竹", "戈", "十"),
        listOf("大", "中", "一", "弓", "人", "心", "手", "口", "尸"),
        listOf("廿", "山", "女", "田", "難", "卜", "重"),
    ),
    LauncherKeyboardLayout.HANGUL to listOf(
        listOf("ㅂ", "ㅈ", "ㄷ", "ㄱ", "ㅅ", "ㅛ", "ㅕ", "ㅑ", "ㅐ", "ㅔ"),
        listOf("ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ", "ㅗ", "ㅓ", "ㅏ", "ㅣ"),
        listOf("ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ"),
    ),
    // The first Abugida option uses a compact Devanagari arrangement. Additional scripts can be
    // added as layouts without changing the keyboard renderer or preference contract.
    LauncherKeyboardLayout.ABUGIDA to listOf(
        listOf("क", "ख", "ग", "घ", "ङ", "च", "छ", "ज", "झ", "ञ"),
        listOf("ट", "ठ", "ड", "ढ", "ण", "त", "थ", "द", "ध"),
        listOf("न", "प", "फ", "ब", "भ", "म", "य"),
    ),
)
