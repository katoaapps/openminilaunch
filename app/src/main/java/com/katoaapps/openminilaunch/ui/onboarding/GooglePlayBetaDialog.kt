package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun GooglePlayBetaDialog(
    onOpenBeta: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.PlayArrow, null, tint = Rust) },
        title = { Text(stringResource(R.string.google_play_beta_notice_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
                Text(stringResource(R.string.google_play_beta_notice_description))
                Text(
                    stringResource(R.string.google_play_beta_notice_backup),
                    color = Muted,
                    fontSize = Dimens.sp13,
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenBeta) {
                Text(stringResource(R.string.view_beta_instructions))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.not_now))
            }
        },
    )
}
