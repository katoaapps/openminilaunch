package com.katoaapps.openminilaunch.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import com.katoaapps.openminilaunch.BuildConfig
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.demo.DemoCaptureMoment
import com.katoaapps.openminilaunch.features.demo.DemoConversationState
import com.katoaapps.openminilaunch.features.demo.DemoHomeProfile
import com.katoaapps.openminilaunch.features.demo.DemoScenario
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownOption
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownRow
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.updates.GitHubUpdateDialog
import com.katoaapps.openminilaunch.ui.updates.previewUpdateVersion

/** One owner for the hidden capture fixtures that used to be scattered through About. */
@Composable
internal fun DemoStudioSettingsPage(
    store: LauncherStore,
    onCaptureMoment: (DemoCaptureMoment) -> Unit,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    var showUpdatePreview by remember { mutableStateOf(false) }
    var captureMoment by remember { mutableStateOf(DemoCaptureMoment.HOME) }

    SettingsPage(stringResource(R.string.demo_studio), goBack) {
        Text(
            text = stringResource(R.string.demo_studio_description),
            modifier = Modifier.padding(horizontal = Dimens.dp4),
            color = Muted,
            fontSize = Dimens.sp12,
        )
        SectionLabel(stringResource(R.string.demo_scenario))
        SettingsDropdownRow(
            title = stringResource(R.string.demo_scenario),
            selectedValue = store.demoScenario,
            options = DemoScenario.entries.map { scenario ->
                SettingsDropdownOption(scenario, stringResource(scenario.labelRes))
            },
            onValueSelected = store::selectDemoScenario,
        )
        Text(
            text = stringResource(store.demoScenario.descriptionRes),
            modifier = Modifier.padding(horizontal = Dimens.dp4),
            color = Muted,
            fontSize = Dimens.sp12,
        )
        SettingsDropdownRow(
            title = stringResource(R.string.demo_visual_profile),
            selectedValue = store.demoHomeProfile,
            options = DemoHomeProfile.entries.map { profile ->
                SettingsDropdownOption(profile, stringResource(profile.labelRes))
            },
            onValueSelected = store::selectDemoHomeProfile,
            valuePreview = {
                DemoProfileColors(store.demoHomeProfile)
            },
        )
        SettingsDropdownRow(
            title = stringResource(R.string.demo_conversation_state),
            selectedValue = store.demoConversationState,
            options = DemoConversationState.entries.map { state ->
                SettingsDropdownOption(state, stringResource(state.labelRes))
            },
            onValueSelected = store::selectDemoConversationState,
        )
        SettingsDropdownRow(
            title = stringResource(R.string.demo_capture_moment),
            selectedValue = captureMoment,
            options = DemoCaptureMoment.entries.map { moment ->
                SettingsDropdownOption(moment, stringResource(moment.labelRes))
            },
            onValueSelected = { moment ->
                captureMoment = moment
                onCaptureMoment(moment)
            },
        )

        SectionLabel(stringResource(R.string.settings))
        SettingsRow(
            stringResource(R.string.preview_update_alert),
            stringResource(R.string.preview_update_alert_description),
            Icons.Default.SystemUpdateAlt,
            onClick = { showUpdatePreview = true },
        )
        SettingsRow(
            stringResource(R.string.demo_reset_scene),
            stringResource(R.string.demo_reset_scene_description),
            Icons.Default.Refresh,
            onClick = {
                store.resetDemoScene()
                Toast.makeText(context, R.string.demo_scene_reset, Toast.LENGTH_SHORT).show()
            },
        )
        SettingsRow(
            stringResource(R.string.demo_mode_disable),
            stringResource(R.string.demo_mode_disable_description),
            Icons.Default.VisibilityOff,
            onClick = {
                store.disableDemoMode()
                Toast.makeText(
                    context,
                    R.string.demo_search_data_disabled,
                    Toast.LENGTH_SHORT,
                ).show()
                goBack()
            },
        )
    }

    if (showUpdatePreview) {
        GitHubUpdateDialog(
            currentVersion = BuildConfig.VERSION_NAME,
            availableVersion = previewUpdateVersion(BuildConfig.VERSION_NAME),
            onOpenGitHub = {
                Toast.makeText(context, R.string.demo_preview_action, Toast.LENGTH_SHORT).show()
            },
            onOpenFdroid = {
                Toast.makeText(context, R.string.demo_preview_action, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showUpdatePreview = false },
        )
    }
}

@Composable
private fun DemoProfileColors(profile: DemoHomeProfile) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp4)) {
        Box(
            Modifier.size(Dimens.dp16).clip(CircleShape)
                .background(colorResource(profile.backgroundColorRes)),
        )
        Box(
            Modifier.size(Dimens.dp16).clip(CircleShape)
                .background(colorResource(profile.panelColorRes)),
        )
    }
}
