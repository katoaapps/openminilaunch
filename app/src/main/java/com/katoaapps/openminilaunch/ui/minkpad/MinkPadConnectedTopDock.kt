package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.MinkPadSessionState

internal enum class MinkPadConnectedMenu { HOSTS, MORE }

@Composable
internal fun MinkPadTopDock(
    state: MinkPadSessionState,
    mode: MinkPadControllerMode,
    menu: MinkPadConnectedMenu?,
    onMode: (MinkPadControllerMode) -> Unit,
    onMenu: (MinkPadConnectedMenu?) -> Unit,
    onConnect: (String) -> Unit,
    onHosts: () -> Unit,
    onDiscover: () -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                TextButton(
                    onClick = { onMenu(MinkPadConnectedMenu.HOSTS) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.mouseReady || state.keyboardReady) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                            ),
                    )
                    Text(
                        state.host?.name ?: stringResource(R.string.minkpad_choose_device),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = menu == MinkPadConnectedMenu.HOSTS,
                    onDismissRequest = { onMenu(null) },
                ) {
                    val hosts = (listOfNotNull(state.host) + state.hosts).distinctBy { it.address }
                    hosts.forEach { host ->
                        val current = host.address == state.host?.address
                        DropdownMenuItem(
                            text = { Text(host.name) },
                            onClick = {
                                onMenu(null)
                                if (!current) onConnect(host.address)
                            },
                            trailingIcon = if (current) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = stringResource(R.string.minkpad_connected_description),
                                    )
                                }
                            } else null,
                        )
                    }
                    if (hosts.isNotEmpty()) HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.minkpad_pair_new_device)) },
                        onClick = { onMenu(null); onDiscover() },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.minkpad_manage_saved_devices)) },
                        onClick = { onMenu(null); onHosts() },
                    )
                }
            }
            MinkPadModeIconButton(
                selected = mode == MinkPadControllerMode.TRADITIONAL,
                onClick = { onMode(MinkPadControllerMode.TRADITIONAL) },
                icon = { Icon(Icons.Default.Mouse, contentDescription = stringResource(R.string.minkpad_trackpad_mode)) },
            )
            MinkPadModeIconButton(
                selected = mode == MinkPadControllerMode.REMOTE,
                onClick = { onMode(MinkPadControllerMode.REMOTE) },
                icon = { Icon(Icons.Default.SettingsRemote, contentDescription = stringResource(R.string.minkpad_remote_mode)) },
            )
            IconButton(onClick = onHelp, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = stringResource(R.string.minkpad_trackpad_help),
                )
            }
            Box {
                IconButton(
                    onClick = { onMenu(MinkPadConnectedMenu.MORE) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.minkpad_more_options))
                }
                DropdownMenu(
                    expanded = menu == MinkPadConnectedMenu.MORE,
                    onDismissRequest = { onMenu(null) },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.minkpad_mouse_keyboard_settings)) },
                        onClick = { onMenu(null); onSettings() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.minkpad_close)) },
                        onClick = { onMenu(null); onClose() },
                    )
                }
            }
        }
    }
}

@Composable
private fun MinkPadModeIconButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .semantics { this.selected = selected },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) { icon() }
}
