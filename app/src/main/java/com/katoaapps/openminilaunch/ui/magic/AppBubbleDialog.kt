package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight

/** First-pass app bubble actions opened by long-pressing a Magic Box app result. */
@Composable
internal fun AppBubbleActionDialog(
    target: LauncherAppTarget,
    actions: DeviceActions,
    automatic: Boolean,
    onAutomaticChange: (Boolean) -> Unit,
    onOpenNormally: () -> Unit,
    onOpenBubble: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LauncherTargetIcon(target, actions, Dimens.dp36)
                Column(Modifier.padding(start = Dimens.dp12)) {
                    Text(target.label, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.app_bubbles_beta), color = Muted, fontSize = Dimens.sp11)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
                Text(stringResource(R.string.app_bubble_action_description))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = Dimens.dp12)) {
                        Text(stringResource(R.string.always_open_in_app_bubble), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.always_open_in_app_bubble_description),
                            color = Muted,
                            fontSize = Dimens.sp11,
                        )
                    }
                    Switch(checked = automatic, onCheckedChange = onAutomaticChange)
                }
                Button(onClick = onOpenBubble, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.BubbleChart, null)
                    Text(stringResource(R.string.open_in_app_bubble), Modifier.padding(start = Dimens.dp8))
                }
                OutlinedButton(onClick = onOpenNormally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.OpenInNew, null)
                    Text(stringResource(R.string.open_normally), Modifier.padding(start = Dimens.dp8))
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
internal fun MagicAppBubbleDialogHost(
    flow: MagicAppBubbleFlow,
    actions: DeviceActions,
) {
    flow.actionTarget?.let { target ->
        AppBubbleActionDialog(
            target = target,
            actions = actions,
            automatic = flow.opensAutomatically(target),
            onAutomaticChange = { flow.updateAutomatic(target, it) },
            onOpenNormally = { flow.openNormally(target) },
            onOpenBubble = { flow.launchInBubble(target) },
            onDismiss = flow::dismissActions,
        )
    }
}
