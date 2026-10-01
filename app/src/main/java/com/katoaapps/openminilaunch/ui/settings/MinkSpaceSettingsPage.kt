package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppCatalog
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSpec
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpacePreviewDestination
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun MinkSpaceSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.mink_space_mini_apps), goBack) {
        Text(
            text = stringResource(R.string.mink_space_settings_description),
            color = Muted,
        )
        SectionLabel(stringResource(R.string.mini_apps))
        val implementedOrder = store.minkSpaceMiniAppOrder.filter { it.readyForHome }
        store.minkSpaceMiniAppOrder.forEach { miniApp ->
            val implementedIndex = implementedOrder.indexOf(miniApp)
            MinkSpaceMiniAppRow(
                spec = MinkSpaceMiniAppCatalog.get(miniApp),
                enabled = miniApp in store.enabledMinkSpaceMiniApps,
                first = implementedIndex <= 0,
                last = implementedIndex == implementedOrder.lastIndex,
                onEnabledChange = { store.setMinkSpaceMiniAppEnabled(miniApp, it) },
                onMove = { store.moveMinkSpaceMiniApp(miniApp, it) },
            )
        }
        SectionLabel(stringResource(R.string.preview))
        MinkSpaceMiniAppCatalog.all.forEach { spec ->
            val destination = spec.preview ?: return@forEach
            val labelRes = spec.previewLabelRes ?: return@forEach
            MinkSpacePreviewButton(
                label = stringResource(labelRes),
                icon = spec.icon,
                onClick = {
                    when (destination) {
                        MinkSpacePreviewDestination.CALCULATOR -> actions.openCalculatorLab()
                        MinkSpacePreviewDestination.MEDIA -> actions.openMediaLab()
                    }
                },
            )
        }
    }
}

@Composable
private fun MinkSpacePreviewButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, contentDescription = null)
        Text(label, modifier = Modifier.padding(start = Dimens.dp8))
    }
}

@Composable
private fun MinkSpaceMiniAppRow(
    spec: MinkSpaceMiniAppSpec,
    enabled: Boolean,
    first: Boolean,
    last: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
) {
    val miniApp = spec.miniApp
    val implemented = miniApp.readyForHome
    val toggleEnabled = implemented && !miniApp.alwaysEnabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            modifier = Modifier.padding(end = Dimens.dp10),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(spec.labelRes),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when {
                    miniApp.alwaysEnabled -> stringResource(R.string.always_enabled)
                    !implemented -> stringResource(R.string.coming_later)
                    else -> stringResource(R.string.swipe_vertically_to_switch)
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = { onMove(-1) }, enabled = implemented && !first) {
            Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up))
        }
        IconButton(onClick = { onMove(1) }, enabled = implemented && !last) {
            Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down))
        }
        Switch(
            checked = enabled,
            enabled = toggleEnabled,
            onCheckedChange = onEnabledChange,
        )
    }
}
