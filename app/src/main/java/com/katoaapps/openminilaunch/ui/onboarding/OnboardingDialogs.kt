package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun FeatureUpdateDialog(
    onOpenSettings: () -> Unit,
    onReviewTutorial: () -> Unit,
    onNotNow: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onNotNow,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.Pets, null, tint = Rust) },
        title = { Text(stringResource(R.string.whats_new)) },
        text = {
            Column(
                Modifier.heightIn(max = Dimens.dp560).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp14),
            ) {
                Text(
                    stringResource(R.string.mink_day_can_help_you_step_away),
                    fontSize = Dimens.sp18,
                    fontWeight = FontWeight.Bold,
                )
                UpdatePoint(Icons.Default.Timer, stringResource(R.string.update_notice_daily_limit_title), stringResource(R.string.update_notice_daily_limit_description))
                UpdatePoint(Icons.Default.Block, stringResource(R.string.update_notice_pause_apps_title), stringResource(R.string.update_notice_pause_apps_description))
                UpdatePoint(Icons.Default.Tune, stringResource(R.string.update_notice_home_browser_title), stringResource(R.string.update_notice_home_browser_description))
                TextButton(onClick = onReviewTutorial, contentPadding = PaddingValues(Dimens.dp0)) {
                    Text(stringResource(R.string.review_updated_tutorial))
                }
            }
        },
        confirmButton = { Button(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) } },
        dismissButton = { TextButton(onClick = onNotNow) { Text(stringResource(R.string.not_now)) } },
    )
}

@Composable
internal fun FileSearchScopeDialog(
    onChooseFolder: () -> Unit,
    onSkip: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onSkip,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.FolderOpen, null, tint = Rust) },
        title = { Text(stringResource(R.string.file_scope_title, appName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp14)) {
                Text(stringResource(R.string.file_scope_description))
                Surface(
                    onClick = onChooseFolder,
                    shape = RoundedCornerShape(Dimens.dp16),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Box(Modifier.padding(Dimens.dp14)) {
                        UpdatePoint(
                            Icons.Default.Folder,
                            stringResource(R.string.choose_folder),
                            stringResource(R.string.choose_folder_description, appName),
                        )
                    }
                }
                Text(
                    stringResource(R.string.queries_files_stay_local),
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onSkip) { Text(stringResource(R.string.skip_for_now)) } },
    )
}

@Composable
private fun UpdatePoint(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(Dimens.dp22), tint = Rust)
        Column(Modifier.padding(start = Dimens.dp10)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = Dimens.sp13)
        }
    }
}

@Composable
internal fun UsageAccessDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.Pets, null, tint = Rust) },
        title = { Text(stringResource(R.string.usage_disclosure_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                Text(stringResource(R.string.usage_disclosure_body_one, appName))
                Text(stringResource(R.string.usage_disclosure_body_two, appName))
                Text(stringResource(R.string.usage_disclosure_body_three))
            }
        },
        confirmButton = { Button(onClick = onContinue) { Text(stringResource(R.string.continue_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) } },
    )
}
