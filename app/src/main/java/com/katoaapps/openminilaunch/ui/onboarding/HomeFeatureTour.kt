package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.PagedTutorialDialog
import com.katoaapps.openminilaunch.ui.components.TutorialPage

private val homeFeatureTourPages = listOf(
    TutorialPage(
        icon = Icons.Default.SwipeVertical,
        titleRes = R.string.home_tour_mini_apps_title,
        descriptionRes = R.string.home_tour_mini_apps_description,
    ),
    TutorialPage(
        icon = Icons.Default.DragIndicator,
        titleRes = R.string.home_tour_shortcuts_title,
        descriptionRes = R.string.home_tour_shortcuts_description,
    ),
    TutorialPage(
        icon = Icons.Default.Forum,
        titleRes = R.string.home_tour_conversations_title,
        descriptionRes = R.string.home_tour_conversations_description,
    ),
    TutorialPage(
        icon = Icons.Default.Widgets,
        titleRes = R.string.home_tour_widgets_title,
        descriptionRes = R.string.home_tour_widgets_description,
    ),
    TutorialPage(
        icon = Icons.Default.Schedule,
        titleRes = R.string.home_tour_mink_day_title,
        descriptionRes = R.string.home_tour_mink_day_description,
    ),
    TutorialPage(
        icon = Icons.Default.Palette,
        titleRes = R.string.home_tour_appearance_title,
        descriptionRes = R.string.home_tour_appearance_description,
    ),
)

@Composable
internal fun HomeFeatureTour(onDismiss: () -> Unit) {
    PagedTutorialDialog(
        titleRes = R.string.home_tour_title,
        pages = homeFeatureTourPages,
        onDismiss = onDismiss,
    )
}
