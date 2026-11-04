package com.katoaapps.openminilaunch.ui.appgrid

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.configurableAppGridSlots

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
