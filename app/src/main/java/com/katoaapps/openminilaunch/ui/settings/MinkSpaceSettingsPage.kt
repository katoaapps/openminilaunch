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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppCatalog
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSpec
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceManagerDestination
import com.katoaapps.openminilaunch.ui.minkspace.widget.MinkSpaceWidgetSetup
import com.katoaapps.openminilaunch.ui.minkspace.widget.MinkSpaceWidgetSettingsControls
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun MinkSpaceSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    openTodos: () -> Unit,
    goBack: () -> Unit,
) {
    var showWidgetSetup by rememberSaveable { mutableStateOf(false) }
    SettingsPage(stringResource(R.string.mink_space_mini_apps), goBack) {
        Text(
            text = stringResource(R.string.mink_space_settings_description),
            color = Muted,
        )
        SectionLabel(stringResource(R.string.show_in_mink_space))
        val implementedOrder = store.minkSpaceMiniAppOrder.filter { it.readyForHome }
        implementedOrder.forEachIndexed { implementedIndex, miniApp ->
            MinkSpaceMiniAppRow(
                spec = MinkSpaceMiniAppCatalog.get(miniApp),
                shown = miniApp in store.enabledMinkSpaceMiniApps,
                first = implementedIndex <= 0,
                last = implementedIndex == implementedOrder.lastIndex,
                onShownChange = { shown ->
                    if (shown && miniApp == MinkSpaceMiniApp.WIDGET &&
                        store.minkSpaceWidgetId == null
                    ) {
                        showWidgetSetup = true
                    } else {
                        store.setMinkSpaceMiniAppEnabled(miniApp, shown)
                    }
                },
                onMove = { store.moveMinkSpaceMiniApp(miniApp, it) },
            )
            if (miniApp == MinkSpaceMiniApp.WIDGET) {
                MinkSpaceWidgetSettingsControls(
                    appWidgetId = store.minkSpaceWidgetId,
                    actions = actions,
                    onManage = { showWidgetSetup = true },
                )
            }
        }
        SectionLabel(stringResource(R.string.open_and_manage))
        MinkSpaceMiniAppCatalog.all.forEach { spec ->
            val destination = spec.manager ?: return@forEach
            val labelRes = spec.managerLabelRes ?: return@forEach
            MinkSpaceManagerButton(
                label = stringResource(labelRes),
                icon = spec.icon,
                onClick = {
                    when (destination) {
                        MinkSpaceManagerDestination.TODO -> openTodos()
                        MinkSpaceManagerDestination.CALCULATOR -> actions.openCalculatorLab()
                        MinkSpaceManagerDestination.MEDIA -> actions.openMediaLab()
                        MinkSpaceManagerDestination.MUSIC -> actions.openMusicLab()
                    }
                },
            )
        }
    }
    MinkSpaceWidgetSetup(
        visible = showWidgetSetup,
        store = store,
        actions = actions,
        onDismiss = { showWidgetSetup = false },
        onComplete = { showWidgetSetup = false },
    )
}

@Composable
private fun MinkSpaceManagerButton(
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
    shown: Boolean,
    first: Boolean,
    last: Boolean,
    onShownChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
) {
    val miniApp = spec.miniApp
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
                text = stringResource(R.string.swipe_vertically_to_switch),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = { onMove(-1) }, enabled = !first) {
            Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up))
        }
        IconButton(onClick = { onMove(1) }, enabled = !last) {
            Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down))
        }
        Switch(
            checked = shown,
            onCheckedChange = onShownChange,
        )
    }
}
