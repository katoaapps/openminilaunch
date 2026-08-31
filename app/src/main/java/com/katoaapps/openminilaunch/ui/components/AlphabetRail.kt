package com.katoaapps.openminilaunch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Rust
import kotlin.math.abs
import kotlin.math.roundToInt

/** A height-aware A-Z rail that maps the full drag range even when letters must be sampled. */
@Composable
internal fun AlphabetRail(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val letters = remember { ('A'..'Z').toList() }
    val density = LocalDensity.current
    var railHeightPx by remember { mutableIntStateOf(1) }
    var renderedLetterHeightPx by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    val fallbackLetterHeightPx = with(density) { Dimens.dp13.toPx() } * density.fontScale
    val minimumLetterHeightPx = maxOf(
        fallbackLetterHeightPx,
        renderedLetterHeightPx * LETTER_HEIGHT_SAFETY_MULTIPLIER,
    )
    val visibleIndices = remember(railHeightPx, minimumLetterHeightPx, selectedIndex) {
        visibleAlphabetIndices(
            letterCount = letters.size,
            availableHeightPx = railHeightPx,
            minimumLetterHeightPx = minimumLetterHeightPx,
            selectedIndex = selectedIndex,
        )
    }

    fun select(index: Int) {
        selectedIndex = index.coerceIn(letters.indices)
        onLetterSelected(letters[selectedIndex])
    }

    Box(modifier.width(Dimens.dp84).fillMaxHeight()) {
        Box(
            Modifier.align(Alignment.CenterEnd)
                .width(Dimens.dp28)
                .fillMaxHeight()
                .onSizeChanged { railHeightPx = it.height.coerceAtLeast(1) }
                .clip(RoundedCornerShape(Dimens.dp12))
                .background(MaterialTheme.colorScheme.background.copy(alpha = .94f))
                .pointerInput(railHeightPx) {
                    fun selectAt(y: Float) {
                        select(((y / railHeightPx) * letters.size).toInt())
                    }
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragging = true
                            selectAt(it.y)
                        },
                        onVerticalDrag = { change, _ -> selectAt(change.position.y) },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    )
                },
        ) {
            Column(
                Modifier.fillMaxSize().padding(vertical = Dimens.dp3),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                visibleIndices.forEach { index ->
                    val active = index == selectedIndex
                    Text(
                        letters[index].toString(),
                        fontSize = Dimens.sp9,
                        fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colorScheme.onSurface else Rust.copy(alpha = .72f),
                        modifier = Modifier
                            .graphicsLayer {
                                val scale = if (active) 1.35f else .9f
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable { select(index) }
                            .onSizeChanged { size ->
                                if (size.height > renderedLetterHeightPx) {
                                    renderedLetterHeightPx = size.height
                                }
                            }
                            .padding(horizontal = Dimens.dp6),
                    )
                }
            }
        }
        if (dragging) {
            Surface(
                modifier = Modifier.align(Alignment.CenterStart),
                shape = CircleShape,
                color = Rust,
                shadowElevation = Dimens.dp8,
            ) {
                Box(Modifier.size(Dimens.dp48), contentAlignment = Alignment.Center) {
                    Text(
                        letters[selectedIndex].toString(),
                        color = MinkWhite,
                        fontSize = Dimens.sp22,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

private const val LETTER_HEIGHT_SAFETY_MULTIPLIER = 1.12f

internal fun visibleAlphabetIndices(
    letterCount: Int,
    availableHeightPx: Int,
    minimumLetterHeightPx: Float,
    selectedIndex: Int,
): List<Int> {
    if (letterCount <= 0) return emptyList()
    if (letterCount == 1) return listOf(0)
    val capacity = (availableHeightPx / minimumLetterHeightPx.coerceAtLeast(1f)).toInt()
        .coerceIn(5.coerceAtMost(letterCount), letterCount)
    if (capacity == letterCount) return (0 until letterCount).toList()

    val lastIndex = letterCount - 1
    val sampled = (0 until capacity)
        .map { position -> (position * lastIndex.toFloat() / (capacity - 1)).roundToInt() }
        .distinct()
        .toMutableList()
    val safeSelected = selectedIndex.coerceIn(0, lastIndex)
    if (safeSelected !in sampled && sampled.size > 2) {
        val replacement = (1 until sampled.lastIndex).minBy { abs(sampled[it] - safeSelected) }
        sampled[replacement] = safeSelected
    }
    return sampled.distinct().sorted()
}
