package com.katoaapps.openminilaunch.ui.appgrid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.AddCircle
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
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

internal fun AppGridSlot.defaultIcon(): ImageVector = when (this) {
    AppGridSlot.NOTE -> Icons.Default.EditNote
    AppGridSlot.EVENT -> Icons.Default.Event
    AppGridSlot.WEATHER -> Icons.Default.Cloud
    AppGridSlot.CALL -> Icons.Default.Call
    AppGridSlot.MESSAGE -> Icons.AutoMirrored.Filled.Message
    AppGridSlot.FILES -> Icons.Default.FolderOpen
    AppGridSlot.EXTRA -> Icons.Default.AddCircle
    AppGridSlot.DRAWER -> Icons.Default.GridView
}

@Composable
internal fun AppGridSlot.displayLabel(): String = stringResource(labelRes)

@Composable
internal fun AppGridSlot.displaySlotLabel(): String {
    val slot = configurableAppGridSlots.indexOf(this)
    return if (slot >= 0) stringResource(R.string.shortcut_slot, slot + 1) else displayLabel()
}

@Composable
internal fun AppGridSlotAssignmentRow(
    slot: AppGridSlot,
    targetKey: String?,
    actions: DeviceActions,
    subtitle: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val outerPadding = if (compact) Dimens.dp8 else Dimens.dp12
    val iconContainerSize = if (compact) Dimens.dp34 else Dimens.dp42
    val iconContainerRadius = if (compact) Dimens.dp10 else Dimens.dp12
    val targetIconSize = if (compact) Dimens.dp26 else Dimens.dp32
    val builtInIconSize = if (compact) Dimens.dp21 else Dimens.dp25
    val textPadding = if (compact) Dimens.dp8 else Dimens.dp12
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick).padding(outerPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(iconContainerSize).clip(RoundedCornerShape(iconContainerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (targetKey != null) {
                val target = actions.resolveLauncherSelection(targetKey)
                LauncherTargetIcon(target, actions, targetIconSize, contentDescription = target.label)
            } else {
                Icon(
                    slot.defaultIcon(),
                    null,
                    Modifier.size(builtInIconSize),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = textPadding)) {
            Text(
                slot.displaySlotLabel(),
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) Dimens.sp13 else Dimens.sp14,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = Muted,
                fontSize = if (compact) Dimens.sp11 else Dimens.sp12,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            null,
            modifier = Modifier.size(if (compact) Dimens.dp18 else Dimens.dp24),
            tint = Muted,
        )
    }
}
