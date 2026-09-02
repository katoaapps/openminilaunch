package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

/** Chooses the exact Top 8 position replaced when the drawer is already full. */
@Composable
internal fun DrawerReplacementDialog(
    replacement: LauncherTarget,
    currentSelectionKeys: List<String>,
    actions: DeviceActions,
    onReplace: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentTargets = remember(currentSelectionKeys) {
        currentSelectionKeys.map(actions::resolveLauncherSelection)
    }
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            modifier = Modifier.minkDialogWidth().fillMaxHeight(.9f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp14)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.replace_drawer_item_title),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Black,
                        fontSize = Dimens.sp18,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                Text(
                    stringResource(R.string.replace_drawer_item_description, replacement.label),
                    modifier = Modifier.padding(bottom = Dimens.dp8),
                    color = Muted,
                    fontSize = Dimens.sp11,
                )
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(
                        items = currentTargets,
                        key = { index, target -> "$index:${target.selectionKey}" },
                    ) { index, target ->
                        ListItem(
                            headlineContent = {
                                Text(target.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text(
                                    stringResource(R.string.replace_top_eight_item, index + 1),
                                    color = Muted,
                                )
                            },
                            leadingContent = {
                                LauncherTargetIcon(target, actions, Dimens.dp36)
                            },
                            modifier = Modifier.fillMaxWidth().clickable { onReplace(index) },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    }
                }
            }
        }
    }
}
