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
import kotlinx.coroutines.delay

/** One state, one next step. Detailed pairing guidance appears only during active discoverability. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MinkPadConnectionPanel(
    repository: MinkPadRepository,
    state: MinkPadSessionState,
    onStart: () -> Unit,
    onDiscover: () -> Unit,
    onDialogStateChanged: (Boolean) -> Unit,
) {
    var hostsOpen by remember { mutableStateOf(false) }
    LaunchedEffect(hostsOpen) { onDialogStateChanged(hostsOpen) }
    val supported = repository.supported()
    val pairing = state.status == MinkPadStatus.WAITING_FOR_HOST
    val busy = state.status in busyStatuses || pairing && state.host != null

    fun openHosts() {
        repository.refreshHosts()
        hostsOpen = true
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Text(if (supported) state.title() else "MinkPad is unavailable", style = MaterialTheme.typography.titleMedium)
            }
            if (!pairing) state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (!supported) {
                Text("Requires Android 9 or later and a phone with Bluetooth HID Device support.")
            }

            if (pairing) PairingInstructions(state)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (state.status) {
                    MinkPadStatus.IDLE -> if (supported) {
                        Button(onClick = onStart) {
                            Text("Start MinkPad")
                        }
                    }

                    MinkPadStatus.READY -> {
                        if (state.hosts.isEmpty()) {
                            Button(onClick = onDiscover) { Text("Pair a device") }
                        } else {
                            Button(onClick = ::openHosts) { Text("Choose a device") }
                        }
                    }

                    MinkPadStatus.WAITING_FOR_HOST ->
                        TextButton(onClick = repository::cancelPairing) { Text("Cancel pairing") }

                    MinkPadStatus.CONNECTING,
                    MinkPadStatus.DISCONNECTING ->
                        TextButton(onClick = repository::cancelConnection) { Text("Cancel connection") }

                    MinkPadStatus.DISCONNECTED -> {
                        state.host?.let { host ->
                            Button(onClick = { repository.connect(host.address) }) {
                                Text("Reconnect")
                            }
                        }
                        TextButton(onClick = ::openHosts) {
                            Text(if (state.host == null) "Choose a device" else "Choose another device")
                        }
                    }

                    MinkPadStatus.ERROR ->
                        TextButton(onClick = repository::stop) { Text("Reset MinkPad") }

                    MinkPadStatus.REGISTERING,
                    MinkPadStatus.CONNECTED,
                    MinkPadStatus.STOPPING,
                    MinkPadStatus.UNSUPPORTED -> Unit
                }

                if (state.status in closableStatuses) {
                    TextButton(onClick = repository::stop) { Text("Close MinkPad") }
                }
            }

        }
    }
    if (hostsOpen) MinkPadHostsDialog(
        repository = repository,
        state = state,
        onPairNew = {
            hostsOpen = false
            onDiscover()
        },
        onDismiss = { hostsOpen = false },
    )
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
            Text(
                "%d:%02d remaining".format(minutes, seconds),
                style = MaterialTheme.typography.labelMedium,
            )
            state.message
                ?.takeIf { state.host != null || it.startsWith("Pairing failed") }
                ?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (state.host == null) {
                Text("1. Open Bluetooth settings on the device you want to control.")
                Text("2. Select “${state.advertisedName ?: "this phone"}”.")
                Text("3. Confirm the matching pairing code on both devices.")
            } else {
                Text(
                    "Confirm the matching pairing code on both devices. MinkPad will open the controller when keyboard and mouse input are ready.",
                )
            }
            Text(
                "If this device still has an older MinkPad pairing, remove that entry on both devices before trying again.",
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
    MinkPadStatus.READY -> if (hosts.isEmpty()) "Ready to pair" else "Choose a receiving device"
    MinkPadStatus.WAITING_FOR_HOST -> if (host == null) "Pair a device" else "Pairing with ${host.name}"
    MinkPadStatus.CONNECTING -> "Connecting${host?.let { " · ${it.name}" } ?: ""}"
    MinkPadStatus.CONNECTED -> "Connected${host?.let { " · ${it.name}" } ?: ""}"
    MinkPadStatus.DISCONNECTING -> "Disconnecting"
    MinkPadStatus.DISCONNECTED -> "Disconnected${host?.let { " · ${it.name}" } ?: ""}"
    MinkPadStatus.STOPPING -> "Closing MinkPad"
    MinkPadStatus.UNSUPPORTED -> "MinkPad is unavailable"
    MinkPadStatus.ERROR -> "MinkPad needs attention"
}

private val busyStatuses = setOf(
    MinkPadStatus.REGISTERING,
    MinkPadStatus.CONNECTING,
    MinkPadStatus.DISCONNECTING,
    MinkPadStatus.STOPPING,
)

private val closableStatuses = setOf(
    MinkPadStatus.REGISTERING,
    MinkPadStatus.READY,
    MinkPadStatus.WAITING_FOR_HOST,
    MinkPadStatus.CONNECTING,
    MinkPadStatus.DISCONNECTING,
    MinkPadStatus.DISCONNECTED,
)
