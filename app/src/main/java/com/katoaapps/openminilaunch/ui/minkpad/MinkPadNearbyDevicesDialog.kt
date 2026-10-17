package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository

@Composable
internal fun MinkPadNearbyDevicesDialog(
    repository: MinkPadRepository,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val discovery by repository.nearbyDevices.collectAsState()
    DisposableEffect(repository) {
        onDispose { repository.stopNearbyDiscovery() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(.96f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Nearby devices") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (discovery.scanning) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(
                        if (discovery.scanning) "Searching for discoverable devices…"
                        else "Search finished",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                discovery.message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (discovery.devices.isEmpty() && !discovery.scanning && discovery.message == null) {
                    Text("No nearby devices found. Make sure the receiving device's pairing screen is open.")
                }
                discovery.devices.forEach { device ->
                    TextButton(
                        onClick = {
                            if (repository.pairNearbyDevice(device.address)) onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${device.name} · Pair")
                    }
                }
                Text(
                    "Only devices found during this search are shown. MinkPad does not save or upload scan results.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = {
                        context.openMinkPadBluetoothSettings()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open Bluetooth settings") }
            }
        },
        confirmButton = { TextButton(onClick = onRefresh) { Text("Search again") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
