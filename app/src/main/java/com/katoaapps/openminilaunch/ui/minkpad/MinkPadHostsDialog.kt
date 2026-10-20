package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.MinkPadHost
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.MinkPadSessionState
import com.katoaapps.openminilaunch.features.minkpad.MinkPadStatus
import com.katoaapps.openminilaunch.features.minkpad.canChooseHost

/** Receiving devices that have successfully connected to MinkPad. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MinkPadHostsDialog(
    repository: MinkPadRepository,
    state: MinkPadSessionState,
    onPairNew: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var forgetPrompt by remember { mutableStateOf<ForgetDevicePrompt?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(.96f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.minkpad_receiving_devices)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.hosts.isEmpty()) {
                    Text(stringResource(R.string.minkpad_no_saved_devices))
                }
                state.hosts.forEach { host ->
                    val current = state.host?.address == host.address
                    Text(host.name, style = MaterialTheme.typography.titleSmall)
                    if (current) when (state.status) {
                        MinkPadStatus.CONNECTED -> Text(
                            stringResource(R.string.minkpad_state_connected),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        MinkPadStatus.WAITING_FOR_HOST -> Text(stringResource(R.string.minkpad_waiting_for_host))
                        MinkPadStatus.CONNECTING -> Text(stringResource(R.string.minkpad_connecting_ellipsis))
                        MinkPadStatus.DISCONNECTING -> Text(stringResource(R.string.minkpad_disconnecting_ellipsis))
                        else -> Unit
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            current && state.status == MinkPadStatus.CONNECTED ->
                                TextButton(onClick = { repository.disconnect(); onDismiss() }) {
                                    Text(stringResource(R.string.minkpad_disconnect))
                                }
                            current && state.status == MinkPadStatus.WAITING_FOR_HOST ->
                                TextButton(onClick = repository::cancelPairing) {
                                    Text(stringResource(R.string.minkpad_close_pairing))
                                }
                            current && state.status in setOf(
                                MinkPadStatus.CONNECTING,
                                MinkPadStatus.DISCONNECTING,
                            ) -> TextButton(onClick = repository::cancelConnection) { Text(stringResource(R.string.cancel)) }
                            else -> TextButton(
                                onClick = { repository.connect(host.address); onDismiss() },
                                enabled = state.canChooseHost,
                            ) { Text(stringResource(R.string.minkpad_connect)) }
                        }
                        if (host.remembered) TextButton(
                            onClick = { forgetPrompt = ForgetDevicePrompt.Confirm(host) },
                        ) { Text(stringResource(R.string.minkpad_forget_device)) }
                    }
                }
                onPairNew?.let { pairNew ->
                    TextButton(onClick = pairNew) { Text(stringResource(R.string.minkpad_pair_new_device)) }
                }
                if (state.hosts.isNotEmpty()) {
                    Text(
                        stringResource(R.string.minkpad_reconnect_explanation),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(R.string.minkpad_unpair_explanation),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
    when (val prompt = forgetPrompt) {
        is ForgetDevicePrompt.Confirm -> AlertDialog(
            onDismissRequest = { forgetPrompt = null },
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(.92f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(stringResource(R.string.minkpad_forget_title, prompt.host.name)) },
            text = {
                Text(
                    stringResource(R.string.minkpad_forget_description),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.forget(prompt.host.address)
                    forgetPrompt = ForgetDevicePrompt.OpenSettings(prompt.host)
                }) { Text(stringResource(R.string.minkpad_forget)) }
            },
            dismissButton = { TextButton(onClick = { forgetPrompt = null }) { Text(stringResource(R.string.cancel)) } },
        )

        is ForgetDevicePrompt.OpenSettings -> AlertDialog(
            onDismissRequest = { forgetPrompt = null },
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(.92f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(stringResource(R.string.minkpad_remove_pairing_title)) },
            text = {
                Text(
                    stringResource(R.string.minkpad_remove_pairing_description, prompt.host.name),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    context.openMinkPadBluetoothSettings()
                    forgetPrompt = null
                    onDismiss()
                }) { Text(stringResource(R.string.minkpad_open_bluetooth_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { forgetPrompt = null }) { Text(stringResource(R.string.minkpad_not_now)) }
            },
        )

        null -> Unit
    }
}

private sealed interface ForgetDevicePrompt {
    val host: MinkPadHost

    data class Confirm(override val host: MinkPadHost) : ForgetDevicePrompt
    data class OpenSettings(override val host: MinkPadHost) : ForgetDevicePrompt
}
