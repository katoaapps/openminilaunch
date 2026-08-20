package com.katoaapps.openminilaunch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun Shortcut.defaultIcon(): ImageVector = when (this) {
    Shortcut.NOTE -> Icons.Default.EditNote
    Shortcut.EVENT -> Icons.Default.Event
    Shortcut.WEATHER -> Icons.Default.Cloud
    Shortcut.CALL -> Icons.Default.Call
    Shortcut.MESSAGE -> Icons.AutoMirrored.Filled.Message
    Shortcut.FILES -> Icons.Default.FolderOpen
    Shortcut.TODO -> Icons.Default.CheckCircle
    Shortcut.DRAWER -> Icons.Default.GridView
}

@Composable
internal fun Shortcut.displayLabel(): String = stringResource(labelRes)

@Composable
internal fun Shortcut.displaySlotLabel(): String {
    val slot = configurableShortcuts.indexOf(this)
    return if (slot >= 0) stringResource(R.string.shortcut_slot, slot + 1) else displayLabel()
}

@Composable
internal fun ShortcutAssignmentRow(
    shortcut: Shortcut,
    packageName: String?,
    actions: DeviceActions,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick).padding(Dimens.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(Dimens.dp42).clip(RoundedCornerShape(Dimens.dp12))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (packageName != null) {
                AppIcon(packageName, actions, Dimens.dp32, contentDescription = actions.appLabel(packageName))
            } else {
                Icon(shortcut.defaultIcon(), null, Modifier.size(Dimens.dp25), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
            Text(shortcut.displaySlotLabel(), fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = Dimens.sp12, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}
