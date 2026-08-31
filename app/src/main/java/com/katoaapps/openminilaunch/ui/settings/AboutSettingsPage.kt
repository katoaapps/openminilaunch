package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.BuildConfig
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.NotificationHub
import com.katoaapps.openminilaunch.features.demo.DemoSearchData
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Sage
import com.katoaapps.openminilaunch.ui.updates.GitHubUpdateDialog
import com.katoaapps.openminilaunch.ui.updates.previewUpdateVersion

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight

private const val DEMO_MODE_TAP_COUNT = 8

@Composable
internal fun AboutSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    onRepeatTutorial: () -> Unit,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val enabledMessage = stringResource(R.string.demo_search_data_enabled)
    val disabledMessage = stringResource(R.string.demo_search_data_disabled)
    var versionTapCount by remember { mutableIntStateOf(0) }
    var showDemoProfilePicker by remember { mutableStateOf(false) }
    var showDemoUpdatePreview by remember { mutableStateOf(false) }
    SettingsPage(stringResource(R.string.about), goBack) {
        SettingsSwitchRow(
            title = stringResource(R.string.github_update_checks),
            subtitle = stringResource(R.string.github_update_checks_description),
            checked = store.githubUpdateChecksEnabled,
            onCheckedChange = store::setGitHubUpdateChecksEnabled,
        )
        HorizontalDivider(color = Sage)
        SettingsRow(
            stringResource(R.string.email_us),
            stringResource(R.string.support_email),
            Icons.Default.Email,
            onClick = actions::emailSupport,
        )
        SettingsRow(
            stringResource(R.string.privacy_policy),
            stringResource(R.string.privacy_policy_summary, appName),
            Icons.Default.PrivacyTip,
            onClick = actions::openPrivacyPolicy,
        )
        SettingsRow(
            stringResource(R.string.terms_of_use),
            stringResource(R.string.terms_summary),
            Icons.Default.Gavel,
            onClick = actions::openTermsOfUse,
        )
        SettingsRow(
            stringResource(R.string.repeat_tutorial),
            stringResource(R.string.repeat_tutorial_summary),
            Icons.Default.School,
            onClick = onRepeatTutorial,
        )
        if (store.demoSearchDataEnabled) {
            SettingsRow(
                stringResource(R.string.demo_home_profile),
                stringResource(store.demoHomeProfile.labelRes),
                Icons.Default.Palette,
                onClick = { showDemoProfilePicker = true },
            )
            SettingsRow(
                stringResource(R.string.preview_update_alert),
                stringResource(R.string.preview_update_alert_description),
                Icons.Default.SystemUpdateAlt,
                onClick = { showDemoUpdatePreview = true },
            )
            SettingsRow(
                stringResource(R.string.demo_mode_disable),
                stringResource(R.string.demo_mode_disable_description),
                Icons.Default.VisibilityOff,
                onClick = {
                    store.toggleDemoSearchData()
                    DemoSearchData.clearFiles(context.applicationContext)
                    NotificationHub.clearDemoReplies()
                    Toast.makeText(context, disabledMessage, Toast.LENGTH_SHORT).show()
                },
            )
        }
        Row(
            Modifier.fillMaxWidth().clickable {
                versionTapCount++
                if (versionTapCount >= DEMO_MODE_TAP_COUNT) {
                    versionTapCount = 0
                    val enabled = store.toggleDemoSearchData()
                    if (!enabled) {
                        DemoSearchData.clearFiles(context.applicationContext)
                        NotificationHub.clearDemoReplies()
                    }
                    Toast.makeText(
                        context,
                        if (enabled) enabledMessage else disabledMessage,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }.padding(vertical = Dimens.dp12),
        ) {
            Text(stringResource(R.string.version), Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(BuildConfig.VERSION_NAME, color = Muted)
        }
    }
    if (showDemoProfilePicker) {
        DemoHomeProfilePicker(
            selected = store.demoHomeProfile,
            onSelect = store::selectDemoHomeProfile,
            onDismiss = { showDemoProfilePicker = false },
        )
    }
    if (showDemoUpdatePreview) {
        GitHubUpdateDialog(
            currentVersion = BuildConfig.VERSION_NAME,
            availableVersion = previewUpdateVersion(BuildConfig.VERSION_NAME),
            onOpenBrowser = {
                if (actions.openLatestGitHubReleaseDownload()) {
                    showDemoUpdatePreview = false
                } else {
                    Toast.makeText(context, R.string.no_browser_available, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showDemoUpdatePreview = false },
        )
    }
}
