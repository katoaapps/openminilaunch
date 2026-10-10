package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadHost
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.MinkPadSessionState
import com.katoaapps.openminilaunch.features.minkpad.MinkPadStatus
import com.katoaapps.openminilaunch.features.minkpad.canChooseHost

/** Receiving devices that have successfully connected to MinkPad. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MinkPadHostsDialog(repository: MinkPadRepository, state: MinkPadSessionState, onDismiss: () -> Unit) {
    var forget by remember { mutableStateOf<MinkPadHost?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Saved devices") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.hosts.isEmpty()) {
                Text("MinkPad has not saved a receiving device yet. Close this window and start the two-minute pairing flow.")
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
                    if (host.remembered) TextButton(onClick = { forget = host }) { Text("Forget history") }
                }
            }
            Text(
                "Reconnect asks Android's native keyboard and mouse profile to resume this pairing. " +
                    "The receiving device may still require you to tap Connect in its Bluetooth settings.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Forgetting removes MinkPad's connection history, not the Bluetooth pairing stored by either device.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
    forget?.let { host ->
        AlertDialog(onDismissRequest = { forget = null }, title = { Text("Forget ${host.name}?") },
            text = { Text("Remove this device from MinkPad's connection history?") },
            confirmButton = { TextButton(onClick = { repository.forget(host.address); forget = null }) { Text("Forget") } },
            dismissButton = { TextButton(onClick = { forget = null }) { Text("Cancel") } })
    }
}
