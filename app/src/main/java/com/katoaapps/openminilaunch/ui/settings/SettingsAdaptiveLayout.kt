package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.launcher.currentFoldLayoutFeature
import com.katoaapps.openminilaunch.ui.launcher.twoPanelGeometry
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

/** Keeps Settings navigation single-pane on phones and master-detail on eligible large displays. */
@Composable
internal fun SettingsAdaptiveLayout(
    twoPanelEnabled: Boolean,
    destination: SettingsDestination,
    navigatingBack: Boolean,
    showDetailBackButton: Boolean,
    overviewContent: @Composable () -> Unit,
    destinationContent: @Composable (SettingsDestination) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val fold = currentFoldLayoutFeature()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val geometry = twoPanelGeometry(
            enabled = twoPanelEnabled,
            smallestWidthDp = configuration.smallestScreenWidthDp,
            orientation = configuration.orientation,
            windowWidthPx = constraints.maxWidth,
            density = density.density,
            fold = fold,
        )

        if (geometry == null) {
            SettingsDestinationTransition(destination, navigatingBack, destinationContent)
        } else {
            val layoutDirection = LocalLayoutDirection.current
            val overviewX = if (layoutDirection == LayoutDirection.Ltr) {
                geometry.outerLeftPx
            } else {
                constraints.maxWidth - geometry.outerRightPx - geometry.paneWidthPx
            }
            val detailX = if (layoutDirection == LayoutDirection.Ltr) {
                geometry.outerLeftPx + geometry.paneWidthPx + geometry.gapPx
            } else {
                geometry.outerLeftPx
            }
            val paneWidth = with(density) { geometry.paneWidthPx.toDp() }

            Box(
                Modifier.offset { IntOffset(overviewX, 0) }
                    .width(paneWidth)
                    .fillMaxHeight(),
            ) {
                overviewContent()
            }
            Box(
                Modifier.offset { IntOffset(detailX, 0) }
                    .width(paneWidth)
                    .fillMaxHeight(),
            ) {
                if (destination == SettingsDestination.OVERVIEW) {
                    EmptySettingsDetail()
                } else {
                    CompositionLocalProvider(
                        LocalSettingsShowBackButton provides showDetailBackButton,
                    ) {
                        SettingsDestinationTransition(destination, navigatingBack, destinationContent)
                    }
                }
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

@Composable
private fun EmptySettingsDetail() {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(Dimens.dp32),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.settings_overview_description, stringResource(R.string.app_name)),
                    modifier = Modifier.padding(top = Dimens.dp12),
                    color = Muted,
                )
            }
        }
    }
}
