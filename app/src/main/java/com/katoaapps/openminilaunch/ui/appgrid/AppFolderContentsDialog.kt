package com.katoaapps.openminilaunch.ui.appgrid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.AppFolderConfig
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.UnavailableLauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.wellbeing.MinkPausedBadge

/** Functional folder surface used until the final Home folder design is chosen. */
@Composable
internal fun AppFolderContentsDialog(
    folder: AppFolderConfig,
    actions: DeviceActions,
    isPaused: (LauncherTarget) -> Boolean,
    onTarget: (LauncherTarget) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val targets = folder.orderedTargetKeys.map(actions::resolveLauncherSelection)
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.72f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp14)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        folder.customName ?: stringResource(R.string.apps),
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Black,
                        fontSize = Dimens.sp18,
                    )
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                ) {
                    items(targets, key = LauncherTarget::selectionKey) { target ->
                        Column(
                            Modifier.clip(RoundedCornerShape(Dimens.dp16))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable { onTarget(target) }
                                .padding(Dimens.dp8),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.size(Dimens.dp48), contentAlignment = Alignment.Center) {
                                if (target.isAvailable) {
                                    LauncherTargetIcon(target, actions, Dimens.dp48)
                                } else {
                                    UnavailableLauncherTargetIcon(
                                        size = Dimens.dp48,
                                        contentDescription = stringResource(R.string.app_unavailable),
                                    )
                                }
                                if (target.isAvailable && isPaused(target)) {
                                    MinkPausedBadge(Modifier.align(Alignment.Center))
                                }
                            }
                            Text(
                                if (target.isAvailable) {
                                    target.label
                                } else {
                                    stringResource(R.string.app_unavailable)
                                },
                                modifier = Modifier.padding(top = Dimens.dp6),
                                textAlign = TextAlign.Center,
                                fontSize = Dimens.sp11,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}
