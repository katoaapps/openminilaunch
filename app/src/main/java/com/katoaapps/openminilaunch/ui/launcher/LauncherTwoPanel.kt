package com.katoaapps.openminilaunch.ui.launcher

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.magic.MagicBoxSessionState
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.wellbeing.MinkDayScreen
import com.katoaapps.openminilaunch.ui.widgets.WidgetPage
import kotlinx.coroutines.launch
import kotlin.math.abs

/** One continuous three-pane strip; Home is drawn once above its moving slot. */
@Composable
internal fun LauncherTwoPanel(
    store: LauncherStore,
    actions: DeviceActions,
    geometry: TwoPanelGeometry,
    viewportWidthPx: Int,
    listState: LazyListState,
    homeStateHolder: SaveableStateHolder,
    magicBoxExpanded: Boolean,
    homeRequestToken: Int,
    profileFlipResetKey: Int,
    magicBoxSessionState: MagicBoxSessionState,
    onFocusPage: (Int) -> Unit,
    onMinkDay: () -> Unit,
    onGoHome: () -> Unit,
    openSettings: (SettingsDestination) -> Unit,
    openTodos: () -> Unit,
    openVCardSettings: () -> Unit,
    openHub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val paneWidth = with(density) { geometry.paneWidthPx.toDp() }
    val gap = with(density) { geometry.gapPx.toDp() }
    val outerLeft = with(density) { geometry.outerLeftPx.toDp() }
    val outerRight = with(density) { geometry.outerRightPx.toDp() }
    val absoluteScrollPx =
        listState.firstVisibleItemIndex * (geometry.paneWidthPx + geometry.gapPx) +
            listState.firstVisibleItemScrollOffset
    val homeLeftPx = geometry.outerLeftPx + geometry.paneWidthPx + geometry.gapPx - absoluteScrollPx
    val collapsedBarOffset = with(density) {
        (homeLeftPx + geometry.paneWidthPx / 2 - viewportWidthPx / 2).toDp()
    }

    Box(modifier) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize()
                .blur(if (magicBoxExpanded) Dimens.dp10 else Dimens.dp0)
                .then(
                    if (magicBoxExpanded) Modifier.focusProperties { canFocus = false }
                    else Modifier,
                ),
            horizontalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(start = outerLeft, end = outerRight),
            flingBehavior = rememberSnapFlingBehavior(listState, snapPosition = SnapPosition.Start),
            userScrollEnabled = !magicBoxExpanded,
        ) {
            item(key = "mink-day") {
                Box(Modifier.width(paneWidth).fillMaxHeight().focusPane { onFocusPage(MINK_DAY_PAGE) }) {
                    MinkDayScreen(
                        store = store,
                        isActive = listState.firstVisibleItemIndex == 0,
                        goHome = onGoHome,
                    )
                }
            }
            item(key = "home-slot") {
                Box(Modifier.width(paneWidth).fillMaxHeight())
            }
            item(key = "widgets") {
                Box(Modifier.width(paneWidth).fillMaxHeight().focusPane { onFocusPage(WIDGET_PAGE) }) {
                    WidgetPage(store = store, actions = actions, goHome = onGoHome)
                }
            }
        }

        homeStateHolder.SaveableStateProvider("home") {
            HomeScreen(
                store = store,
                actions = actions,
                openSettings = openSettings,
                openTodos = openTodos,
                openVCardSettings = openVCardSettings,
                openHub = openHub,
                openMinkDay = onMinkDay,
                minkStatusActive = true,
                onMagicExpandedChange = { expanded ->
                    if (expanded) onFocusPage(HOME_PAGE)
                },
                keyboardInputEnabled = true,
                homeRequestToken = homeRequestToken,
                profileFlipResetKey = profileFlipResetKey,
                magicBoxSessionState = magicBoxSessionState,
                paneModifier = Modifier.offset { IntOffset(homeLeftPx, 0) }
                    .width(paneWidth).fillMaxHeight(),
                collapsedBarMaxWidth = minOf(paneWidth, Dimens.dp620),
                collapsedBarOffsetX = collapsedBarOffset,
                onPaneInteracted = { onFocusPage(HOME_PAGE) },
                onHorizontalDrag = { amount -> listState.dispatchRawDelta(-amount) },
                onHorizontalDragFinished = { draggedPx, elapsedMillis ->
                    val scrollPx = listState.firstVisibleItemIndex *
                        (geometry.paneWidthPx + geometry.gapPx) + listState.firstVisibleItemScrollOffset
                    val quickSwipe = abs(draggedPx) >= 48f * density.density && elapsedMillis < 250L
                    val targetPair = if (quickSwipe) {
                        if (draggedPx > 0f) 0 else 1
                    } else if (scrollPx >= (geometry.paneWidthPx + geometry.gapPx) / 2) 1 else 0
                    scope.launch { listState.animateScrollToItem(targetPair) }
                },
            )
        }
    }
}

/** Listen without consuming so widgets, shortcuts, and the outer swipe keep their gestures. */
private fun Modifier.focusPane(onFocused: () -> Unit): Modifier = pointerInput(onFocused) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onFocused()
    }
}
