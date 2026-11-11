package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceHost
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpacePresentationState
import com.katoaapps.openminilaunch.ui.minkspace.core.animateMinkSpaceExpansionLayout
import com.katoaapps.openminilaunch.ui.onboarding.HomeTourTarget
import com.katoaapps.openminilaunch.ui.onboarding.homeTourTarget
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.cathedralGlassBrush
import com.katoaapps.openminilaunch.ui.theme.cathedralGlassCompositeColor
import com.katoaapps.openminilaunch.ui.theme.resolveContentColor
import com.katoaapps.openminilaunch.ui.theme.shouldUseDarkForeground

@Composable
internal fun BoxScope.HomeFocusPanel(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    qwertyHome: Boolean,
    expandToAvailableHeight: Boolean,
    availableHeight: Dp,
    focusPanelHeight: Dp,
    todoJumpToken: Int,
    minkSpacePresentation: MinkSpacePresentationState,
    openTodos: () -> Unit,
    openVCardSettings: () -> Unit,
    openMinkSpaceSettings: () -> Unit,
    openAppGridSettings: () -> Unit,
    onTodoCenterChanged: (Offset) -> Unit,
    onPausedApp: (String) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val minkSpaceExpanded = minkSpacePresentation.isExpanded
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val panelColor = Color(store.effectiveHomePanelColorArgb)
    val panelTransparency = store.homePanelTransparency
    val panelOpacity = 1f - panelTransparency
    val visiblePanelColor = cathedralGlassCompositeColor(
        baseColor = panelColor,
        transparency = panelTransparency,
        backdropColor = launcherBackgroundColor(store),
    )
    val contentColor = resolveContentColor(
        background = visiblePanelColor,
        preferred = store.fontAppearance.colorArgb?.let(::Color),
    )
    val mutedColor = contentColor.copy(alpha = .68f)
    val insetColor = if (contentColor == com.katoaapps.openminilaunch.ui.theme.MinkWhite) {
        com.katoaapps.openminilaunch.ui.theme.MinkBlack.copy(alpha = .16f)
    } else {
        com.katoaapps.openminilaunch.ui.theme.MinkWhite.copy(alpha = .24f)
    }
    val panelHeight = if (expandToAvailableHeight) {
        availableHeight
    } else {
        minOf(availableHeight, focusPanelHeight)
    }
    val itemsPerPage = visibleTodoItemsForHeight(panelHeight.value)
    val todoTextMetrics = homeTodoTextMetrics(
        largeDisplay = configuration.smallestScreenWidthDp >= 600,
        compact = qwertyHome,
        panelHeightDp = panelHeight.value,
        itemsPerPage = itemsPerPage,
        fontScale = fontScale,
    )
    val focusModifier = if (expandToAvailableHeight) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxWidth().height(focusPanelHeight).align(
            if (store.alignHomePanelBottom) Alignment.BottomCenter else Alignment.TopCenter,
        )
    }

    Surface(
        modifier = focusModifier.widthIn(max = Dimens.dp620),
        shape = RoundedCornerShape(if (qwertyHome) Dimens.dp26 else Dimens.dp34),
        color = if (panelTransparency > 0f) Color.Transparent else panelColor,
        contentColor = contentColor,
        border = panelTransparency.takeIf { it > 0f }?.let {
            BorderStroke(Dimens.dp1, contentColor.copy(alpha = .18f))
        },
        shadowElevation = if (panelTransparency > 0f) {
            Dimens.dp0
        } else if (shouldUseDarkForeground(visiblePanelColor)) {
            Dimens.dp8
        } else {
            Dimens.dp2
        },
        tonalElevation = Dimens.dp1 * panelOpacity,
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().then(
                if (panelTransparency > 0f) {
                    Modifier.background(cathedralGlassBrush(panelColor, panelTransparency))
                } else {
                    Modifier
                },
            ),
        ) {
            val expansionLayout = animateMinkSpaceExpansionLayout(
                expanded = minkSpaceExpanded,
                compact = qwertyHome,
                availableWidth = maxWidth,
            )

            Row(
                Modifier.fillMaxSize().padding(expansionLayout.contentPadding),
                horizontalArrangement = Arrangement.spacedBy(expansionLayout.contentSpacing),
            ) {
                MinkSpaceHost(
                    store = store,
                    actions = actions,
                    openTodos = openTodos,
                    todoJumpToken = todoJumpToken,
                    todoItemsPerPage = itemsPerPage,
                    todoTextMetrics = todoTextMetrics,
                    compact = qwertyHome,
                    contentColor = contentColor,
                    mutedContentColor = mutedColor,
                    insetColor = insetColor,
                    openMediaLab = actions::openMediaLab,
                    openVCardSettings = openVCardSettings,
                    openMinkSpaceSettings = openMinkSpaceSettings,
                    presentationState = minkSpacePresentation,
                    expandedPresentation = expansionLayout.expandedPresentation,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .homeTourTarget(HomeTourTarget.MINK_SPACE)
                        .onGloballyPositioned { coordinates ->
                            val origin = coordinates.positionInRoot()
                            onTodoCenterChanged(
                                origin + Offset(
                                    coordinates.size.width / 2f,
                                    coordinates.size.height / 2f,
                                ),
                            )
                        },
                )
                if (expansionLayout.appGridWidth > Dimens.dp1) {
                    Box(
                        Modifier
                            .width(expansionLayout.appGridWidth)
                            .fillMaxHeight()
                            .graphicsLayer { alpha = expansionLayout.appGridAlpha },
                    ) {
                        AppGrid(
                            store = store,
                            actions = actions,
                            appAccessState = appAccessState,
                            onPausedApp = onPausedApp,
                            onConfigureExtraApp = openAppGridSettings,
                            compact = qwertyHome,
                            contentColor = contentColor,
                            itemContainerColor = contentColor.copy(alpha = .09f),
                            modifier = Modifier.fillMaxSize(),
                            openDrawer = onOpenDrawer,
                        )
                    }
                }
            }
        }
    }
}
