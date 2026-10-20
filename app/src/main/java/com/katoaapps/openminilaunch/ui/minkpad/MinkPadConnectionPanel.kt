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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
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
    onBrowseNearby: () -> Unit,
    onDialogStateChanged: (Boolean) -> Unit,
) {
    var hostsOpen by remember { mutableStateOf(false) }
    var nearbyDevicesOpen by remember { mutableStateOf(false) }
    LaunchedEffect(hostsOpen, nearbyDevicesOpen) { onDialogStateChanged(hostsOpen || nearbyDevicesOpen) }
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
                Text(
                    if (supported) state.title() else stringResource(R.string.minkpad_state_unavailable),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (!pairing) state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (!supported) {
                Text(stringResource(R.string.minkpad_requires_hid))
            }

            if (pairing) PairingInstructions(
                state = state,
                onBrowseNearby = {
                    if (repository.supportsNearbyDiscovery()) nearbyDevicesOpen = true
                    onBrowseNearby()
                },
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (state.status) {
                    MinkPadStatus.IDLE -> if (supported) {
                        Button(onClick = onStart) {
                            Text(stringResource(R.string.minkpad_start))
                        }
                    }

                    MinkPadStatus.READY -> {
                        if (state.hosts.isEmpty()) {
                            Button(onClick = onDiscover) { Text(stringResource(R.string.minkpad_pair_device)) }
                        } else {
                            Button(onClick = ::openHosts) { Text(stringResource(R.string.minkpad_choose_device)) }
                        }
                    }

                    MinkPadStatus.WAITING_FOR_HOST ->
                        TextButton(onClick = repository::cancelPairing) {
                            Text(stringResource(R.string.minkpad_cancel_pairing))
                        }

                    MinkPadStatus.CONNECTING,
                    MinkPadStatus.DISCONNECTING ->
                        TextButton(onClick = repository::cancelConnection) {
                            Text(stringResource(R.string.minkpad_cancel_connection))
                        }

                    MinkPadStatus.DISCONNECTED -> {
                        state.host?.let { host ->
                            Button(onClick = { repository.connect(host.address) }) {
                                Text(stringResource(R.string.minkpad_reconnect))
                            }
                        }
                        TextButton(onClick = ::openHosts) {
                            Text(stringResource(
                                if (state.host == null) R.string.minkpad_choose_device
                                else R.string.minkpad_choose_another_device,
                            ))
                        }
                    }

                    MinkPadStatus.ERROR ->
                        TextButton(onClick = repository::stop) { Text(stringResource(R.string.minkpad_reset)) }

                    MinkPadStatus.REGISTERING,
                    MinkPadStatus.CONNECTED,
                    MinkPadStatus.STOPPING,
                    MinkPadStatus.UNSUPPORTED -> Unit
                }

                if (state.status in closableStatuses) {
                    TextButton(onClick = repository::stop) { Text(stringResource(R.string.minkpad_close)) }
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
    if (nearbyDevicesOpen) MinkPadNearbyDevicesDialog(
        repository = repository,
        onRefresh = onBrowseNearby,
        onDismiss = {
            repository.stopNearbyDiscovery()
            nearbyDevicesOpen = false
        },
    )
}

@Composable
private fun PairingInstructions(state: MinkPadSessionState, onBrowseNearby: () -> Unit) {
    val context = LocalContext.current
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
    val pairingFailed = stringResource(R.string.minkpad_pairing_failed)

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
                stringResource(R.string.minkpad_time_remaining, minutes, seconds),
                style = MaterialTheme.typography.labelMedium,
            )
            state.message
                ?.takeIf { state.host != null || it.startsWith(pairingFailed) }
                ?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (state.host == null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(R.string.minkpad_pair_from_phone), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.minkpad_search_nearby_description), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onBrowseNearby) { Text(stringResource(R.string.minkpad_see_nearby_devices)) }
                    }
                }
                Text(stringResource(R.string.minkpad_pair_step_open_settings))
                Text(stringResource(
                    R.string.minkpad_pair_step_select,
                    state.advertisedName ?: stringResource(R.string.minkpad_this_phone),
                ))
                Text(stringResource(R.string.minkpad_pair_step_confirm))
                Text(
                    stringResource(R.string.minkpad_pair_tv_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = {
                    context.openMinkPadBluetoothSettings()
                }) { Text(stringResource(R.string.minkpad_open_phone_bluetooth_settings)) }
            } else {
                Text(
                    stringResource(R.string.minkpad_pair_confirm_both),
                )
            }
            Text(
                stringResource(R.string.minkpad_pair_old_entry_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun Long?.remainingSeconds(): Int = this?.let {
    ((it - SystemClock.elapsedRealtime()).coerceAtLeast(0L) / 1_000L).toInt()
} ?: 0

@Composable
private fun MinkPadSessionState.title(): String = when (status) {
    MinkPadStatus.IDLE -> stringResource(R.string.minkpad_state_off)
    MinkPadStatus.REGISTERING -> stringResource(R.string.minkpad_state_preparing)
    MinkPadStatus.READY -> stringResource(
        if (hosts.isEmpty()) R.string.minkpad_state_ready_to_pair else R.string.minkpad_state_choose_device,
    )
    MinkPadStatus.WAITING_FOR_HOST -> host?.let {
        stringResource(R.string.minkpad_state_pairing_with, it.name)
    } ?: stringResource(R.string.minkpad_state_pair_device)
    MinkPadStatus.CONNECTING -> host?.let {
        stringResource(R.string.minkpad_state_connecting_with, it.name)
    } ?: stringResource(R.string.minkpad_state_connecting)
    MinkPadStatus.CONNECTED -> host?.let {
        stringResource(R.string.minkpad_state_connected_with, it.name)
    } ?: stringResource(R.string.minkpad_state_connected)
    MinkPadStatus.DISCONNECTING -> stringResource(R.string.minkpad_state_disconnecting)
    MinkPadStatus.DISCONNECTED -> host?.let {
        stringResource(R.string.minkpad_state_disconnected_from, it.name)
    } ?: stringResource(R.string.minkpad_state_disconnected)
    MinkPadStatus.STOPPING -> stringResource(R.string.minkpad_state_closing)
    MinkPadStatus.UNSUPPORTED -> stringResource(R.string.minkpad_state_unavailable)
    MinkPadStatus.ERROR -> stringResource(R.string.minkpad_state_attention)
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
