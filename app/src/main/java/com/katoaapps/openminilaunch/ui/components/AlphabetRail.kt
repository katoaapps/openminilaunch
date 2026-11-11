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
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Rust
import kotlin.math.roundToInt

/** A compact A-to-Z rail: endpoints stay visible and the active letter replaces one middle dot. */
@Composable
internal fun AlphabetRail(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val letters = remember { ('A'..'Z').toList() }
    var railHeightPx by remember { mutableIntStateOf(1) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    val markers = remember(selectedIndex) {
        alphabetRailMarkers(
            letterCount = letters.size,
            markerCount = ALPHABET_RAIL_MARKER_COUNT,
            selectedIndex = selectedIndex,
        )
    }

    fun select(index: Int, force: Boolean = false) {
        val nextIndex = index.coerceIn(letters.indices)
        if (!force && nextIndex == selectedIndex) return
        selectedIndex = nextIndex
        onLetterSelected(letters[nextIndex])
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
                markers.forEachIndexed { markerIndex, displayedLetterIndex ->
                    val active = displayedLetterIndex == selectedIndex
                    val tapIndex = alphabetIndexForMarker(
                        markerIndex = markerIndex,
                        markerCount = markers.size,
                        letterCount = letters.size,
                    )
                    Text(
                        displayedLetterIndex?.let { letters[it].toString() } ?: "·",
                        fontSize = Dimens.sp12,
                        fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colorScheme.onSurface else Rust.copy(alpha = .72f),
                        modifier = Modifier
                            .graphicsLayer {
                                val scale = if (active) 1.35f else .9f
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable { select(tapIndex, force = true) }
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

private const val ALPHABET_RAIL_MARKER_COUNT = 8

internal fun alphabetRailMarkers(
    letterCount: Int,
    markerCount: Int,
    selectedIndex: Int,
): List<Int?> {
    if (letterCount <= 0) return emptyList()
    if (letterCount == 1) return listOf(0)
    val safeMarkerCount = markerCount.coerceAtLeast(2)
    val lastLetterIndex = letterCount - 1
    val safeSelectedIndex = selectedIndex.coerceIn(0, lastLetterIndex)
    val selectedMarker = (
        safeSelectedIndex * (safeMarkerCount - 1).toFloat() / lastLetterIndex
        ).roundToInt()
    return List(safeMarkerCount) { markerIndex ->
        when (markerIndex) {
            0 -> 0
            safeMarkerCount - 1 -> lastLetterIndex
            selectedMarker -> safeSelectedIndex
            else -> null
        }
    }
}

private fun alphabetIndexForMarker(markerIndex: Int, markerCount: Int, letterCount: Int): Int {
    if (letterCount <= 1 || markerCount <= 1) return 0
    return (markerIndex * (letterCount - 1).toFloat() / (markerCount - 1)).roundToInt()
}
