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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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
        title = { Text("Receiving devices") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.hosts.isEmpty()) {
                    Text("MinkPad has not saved a receiving device yet.")
                }
                state.hosts.forEach { host ->
                    val current = state.host?.address == host.address
                    Text(host.name, style = MaterialTheme.typography.titleSmall)
                    if (current) when (state.status) {
                        MinkPadStatus.CONNECTED -> Text("Connected", color = MaterialTheme.colorScheme.primary)
                        MinkPadStatus.WAITING_FOR_HOST -> Text("Waiting for host…")
                        MinkPadStatus.CONNECTING -> Text("Connecting…")
                        MinkPadStatus.DISCONNECTING -> Text("Disconnecting…")
                        else -> Unit
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            current && state.status == MinkPadStatus.CONNECTED ->
                                TextButton(onClick = { repository.disconnect(); onDismiss() }) { Text("Disconnect") }
                            current && state.status == MinkPadStatus.WAITING_FOR_HOST ->
                                TextButton(onClick = repository::cancelPairing) { Text("Close pairing") }
                            current && state.status in setOf(
                                MinkPadStatus.CONNECTING,
                                MinkPadStatus.DISCONNECTING,
                            ) -> TextButton(onClick = repository::cancelConnection) { Text("Cancel") }
                            else -> TextButton(
                                onClick = { repository.connect(host.address); onDismiss() },
                                enabled = state.canChooseHost,
                            ) { Text("Connect") }
                        }
                        if (host.remembered) TextButton(
                            onClick = { forgetPrompt = ForgetDevicePrompt.Confirm(host) },
                        ) { Text("Forget device") }
                    }
                }
                onPairNew?.let { pairNew ->
                    TextButton(onClick = pairNew) { Text("Pair new device") }
                }
                if (state.hosts.isNotEmpty()) {
                    Text(
                        "Reconnect asks Android's native keyboard and mouse profile to resume this pairing. " +
                            "The receiving device may still require you to tap Connect in its Bluetooth settings.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Android Bluetooth settings complete the final unpairing step.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
    when (val prompt = forgetPrompt) {
        is ForgetDevicePrompt.Confirm -> AlertDialog(
            onDismissRequest = { forgetPrompt = null },
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(.92f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Forget ${prompt.host.name}?") },
            text = {
                Text(
                    "MinkPad will disconnect this device and remove it from saved receiving devices. " +
                        "Android keeps the Bluetooth pairing until you remove it in system settings.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.forget(prompt.host.address)
                    forgetPrompt = ForgetDevicePrompt.OpenSettings(prompt.host)
                }) { Text("Forget") }
            },
            dismissButton = { TextButton(onClick = { forgetPrompt = null }) { Text("Cancel") } },
        )

        is ForgetDevicePrompt.OpenSettings -> AlertDialog(
            onDismissRequest = { forgetPrompt = null },
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(.92f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Remove the Bluetooth pairing") },
            text = {
                Text(
                    "To finish forgetting ${prompt.host.name}, open Bluetooth settings, select it, and tap Forget or Unpair.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    context.openMinkPadBluetoothSettings()
                    forgetPrompt = null
                    onDismiss()
                }) { Text("Open Bluetooth settings") }
            },
            dismissButton = {
                TextButton(onClick = { forgetPrompt = null }) { Text("Not now") }
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
