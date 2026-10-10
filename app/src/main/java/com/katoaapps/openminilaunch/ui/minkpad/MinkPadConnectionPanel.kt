package com.katoaapps.openminilaunch.ui.minkpad

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.MinkPadSessionState
import com.katoaapps.openminilaunch.features.minkpad.MinkPadStatus
import com.katoaapps.openminilaunch.features.minkpad.canChooseHost
import kotlinx.coroutines.delay

/** Native-HID pairing and connection state. Discoverability must never look like an idle Ready state. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MinkPadConnectionPanel(
    repository: MinkPadRepository,
    state: MinkPadSessionState,
    onStart: () -> Unit,
    onDiscover: () -> Unit,
    onDialogStateChanged: (Boolean) -> Unit,
) {
    var hosts by remember { mutableStateOf(false) }
    LaunchedEffect(hosts) { onDialogStateChanged(hosts) }
    val running = state.status !in listOf(
        MinkPadStatus.IDLE,
        MinkPadStatus.UNSUPPORTED,
        MinkPadStatus.ERROR,
    )
    val pairing = state.status == MinkPadStatus.WAITING_FOR_HOST && state.host == null

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(state.title(), style = MaterialTheme.typography.titleMedium)
            state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (!repository.supported()) {
                Text("Requires Android 9 or later and a phone with Bluetooth HID Device support.")
            }

            if (pairing) PairingInstructions(state)

            when {
                !running && state.status == MinkPadStatus.IDLE ->
                    Button(onClick = onStart, enabled = repository.supported()) {
                        Text("Start MinkPad")
                    }

                !running -> TextButton(onClick = repository::stop) { Text("Pause session") }

                state.status != MinkPadStatus.STOPPING -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when {
                        state.status == MinkPadStatus.CONNECTED ->
                            Button(onClick = onDiscover) { Text("Add device") }

                        pairing || state.status == MinkPadStatus.WAITING_FOR_HOST ->
                            TextButton(onClick = repository::cancelPairing) { Text("Close pairing") }

                        state.status == MinkPadStatus.CONNECTING ||
                            state.status == MinkPadStatus.DISCONNECTING ->
                            TextButton(onClick = repository::cancelConnection) { Text("Cancel connection") }

                        state.status == MinkPadStatus.DISCONNECTED && state.host != null -> {
                            Button(onClick = { repository.connect(state.host.address) }) {
                                Text("Reconnect")
                            }
                            TextButton(onClick = onDiscover) { Text("Pair a different device") }
                        }

                        state.canChooseHost ->
                            Button(onClick = onDiscover) { Text("Start 2-minute pairing") }
                    }
                    if (!pairing && state.hosts.isNotEmpty()) {
                        TextButton(onClick = {
                            repository.refreshHosts()
                            hosts = true
                        }) { Text("Saved devices") }
                    }
                    TextButton(onClick = repository::stop) { Text("Pause") }
                }
            }

            if (running && !pairing && state.status != MinkPadStatus.CONNECTED) {
                Text(
                    "MinkPad is not discoverable until you start pairing. Saved receiving devices remain available for switching.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.status == MinkPadStatus.CONNECTED) {
                Text(
                    "Mouse and keyboard input now go directly to ${state.host?.name ?: "the receiving device"}.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Add device temporarily releases this connection and opens system pairing. Saved devices lets you switch hosts. " +
                        "Pause closes MinkPad's native HID session; Start asks the most recently used device to reconnect.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "Nearby devices access is used for Bluetooth HID connection and system discoverability, not location. " +
                    "MinkPad never removes system pairings automatically.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (hosts) MinkPadHostsDialog(repository, state) { hosts = false }
}

@Composable
private fun PairingInstructions(state: MinkPadSessionState) {
    val deadline = state.pairingDeadlineElapsedRealtime
    var remaining by remember(deadline) { mutableIntStateOf(deadline.remainingSeconds()) }
    LaunchedEffect(deadline) {
        while (deadline != null && remaining > 0) {
            delay(1_000)
            remaining = deadline.remainingSeconds()
        }
    }
    val minutes = remaining / 60
    val seconds = remaining % 60

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Column {
                    Text("This phone is discoverable now", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "%d:%02d remaining".format(minutes, seconds),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text("1. On the device you want to control, open its Bluetooth settings.")
            Text(
                "2. Select “${state.advertisedName ?: "this phone"}”. This is the phone's normal " +
                    "Bluetooth identity; MinkPad does not create a second BLE device.",
            )
            Text("3. Accept the pairing request on both devices. MinkPad will change to Connected automatically.")
            Text(
                "Android controls this discoverability timer. If the receiving device still has an older " +
                    "MinkPad prototype pairing, remove that older entry before testing this native profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun Long?.remainingSeconds(): Int = this?.let {
    ((it - SystemClock.elapsedRealtime()).coerceAtLeast(0L) / 1_000L).toInt()
} ?: 0

private fun MinkPadSessionState.title(): String = when (status) {
    MinkPadStatus.IDLE -> "MinkPad is off"
    MinkPadStatus.REGISTERING -> "Preparing Bluetooth input"
    MinkPadStatus.READY -> "Ready to pair"
    MinkPadStatus.WAITING_FOR_HOST -> if (host == null) "Pair one device" else "Waiting for ${host.name}"
    MinkPadStatus.CONNECTING -> "Finishing connection${host?.let { " · ${it.name}" } ?: ""}"
    MinkPadStatus.CONNECTED -> "Connected${host?.let { " · ${it.name}" } ?: ""}"
    MinkPadStatus.DISCONNECTING -> "Disconnecting"
    MinkPadStatus.DISCONNECTED -> "Disconnected${host?.let { " · ${it.name}" } ?: ""}"
    MinkPadStatus.STOPPING -> "Pausing MinkPad"
    MinkPadStatus.UNSUPPORTED -> "MinkPad is unavailable"
    MinkPadStatus.ERROR -> "MinkPad needs attention"
}
