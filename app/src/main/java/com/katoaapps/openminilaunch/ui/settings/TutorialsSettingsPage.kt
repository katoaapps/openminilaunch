package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.SettingsRow

/** Replayable teaching surfaces. First-run launch policy remains with each feature owner. */
@Composable
internal fun TutorialsSettingsPage(
    onRepeatSetup: () -> Unit,
    onReplayHomeTour: () -> Unit,
    onReplayMagicBoxTour: () -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.tutorials), goBack) {
        SettingsRow(
            stringResource(R.string.repeat_tutorial),
            stringResource(R.string.repeat_tutorial_summary),
            Icons.Default.RestartAlt,
            onClick = onRepeatSetup,
        )
        SettingsRow(
            stringResource(R.string.replay_home_tour),
            stringResource(R.string.replay_home_tour_summary),
            Icons.Default.Home,
            onClick = onReplayHomeTour,
        )
        SettingsRow(
            stringResource(R.string.replay_magic_box_tour),
            stringResource(R.string.replay_magic_box_tour_summary),
            Icons.Default.AutoAwesome,
            onClick = onReplayMagicBoxTour,
        )
    }
}
