package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.katoaapps.openminilaunch.ui.launcher.layout.currentFoldLayoutFeature
import com.katoaapps.openminilaunch.ui.launcher.layout.twoPanelGeometry
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Uses bottom tabs on compact devices and a persistent side menu on large/two-panel layouts. */
@Composable
internal fun SettingsAdaptiveLayout(
    twoPanelEnabled: Boolean,
    destination: SettingsDestination,
    selectedSection: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
    navigatingBack: Boolean,
    showPageBackButton: Boolean,
    overviewContent: @Composable () -> Unit,
    destinationContent: @Composable (SettingsDestination) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val fold = currentFoldLayoutFeature()

    val pageContent: @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalSettingsShowBackButton provides (
                destination == SettingsDestination.OVERVIEW || showPageBackButton
            ),
        ) {
            if (destination == SettingsDestination.OVERVIEW) {
                overviewContent()
            } else {
                SettingsDestinationTransition(destination, navigatingBack, destinationContent)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val geometry = twoPanelGeometry(
            enabled = twoPanelEnabled,
            smallestWidthDp = configuration.smallestScreenWidthDp,
            orientation = configuration.orientation,
            windowWidthPx = constraints.maxWidth,
            density = density.density,
            fold = fold,
        )
        val useSideNavigation = configuration.smallestScreenWidthDp >= 600

        when {
            geometry != null -> {
                val layoutDirection = LocalLayoutDirection.current
                val navigationX = if (layoutDirection == LayoutDirection.Ltr) {
                    geometry.outerLeftPx
                } else {
                    constraints.maxWidth - geometry.outerRightPx - geometry.paneWidthPx
                }
                val contentX = if (layoutDirection == LayoutDirection.Ltr) {
                    geometry.outerLeftPx + geometry.paneWidthPx + geometry.gapPx
                } else {
                    geometry.outerLeftPx
                }
                val paneWidth = with(density) { geometry.paneWidthPx.toDp() }

                Box(
                    Modifier.offset { IntOffset(navigationX, 0) }
                        .width(paneWidth)
                        .fillMaxHeight(),
                ) {
                    SettingsSideNavigation(selectedSection, onSectionSelected)
                }
                Box(
                    Modifier.offset { IntOffset(contentX, 0) }
                        .width(paneWidth)
                        .fillMaxHeight(),
                ) {
                    pageContent()
                }
            }

            useSideNavigation -> Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(Dimens.dp240).fillMaxHeight()) {
                    SettingsSideNavigation(selectedSection, onSectionSelected)
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { pageContent() }
            }

            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxSize()) { pageContent() }
                SettingsBottomNavigation(selectedSection, onSectionSelected)
            }
        }
    }
}

@Composable
private fun SettingsDestinationTransition(
    destination: SettingsDestination,
    navigatingBack: Boolean,
    content: @Composable (SettingsDestination) -> Unit,
) {
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val direction = if (navigatingBack) -1 else 1
            (slideInHorizontally(tween(180)) { width -> direction * width / 6 } + fadeIn(tween(150)))
                .togetherWith(
                    slideOutHorizontally(tween(180)) { width -> -direction * width / 6 } +
                        fadeOut(tween(120)),
                )
        },
        label = "settings-page",
    ) { target ->
        content(target)
    }
}
