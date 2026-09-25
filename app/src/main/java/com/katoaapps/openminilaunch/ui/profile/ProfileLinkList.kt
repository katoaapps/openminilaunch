package com.katoaapps.openminilaunch.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun SelectedProfileLinks(
    store: LauncherStore,
    selected: List<ProfileLink>,
    onEdit: (ProfileLink) -> Unit,
) {
    val repository = store.profileRepository
    val visible = remember { mutableStateListOf<ProfileLink>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(selected, dragging) {
        if (!dragging) {
            visible.clear()
            visible.addAll(selected)
        }
    }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        visible.add(to.index, visible.removeAt(from.index))
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = Dimens.dp280),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(Dimens.dp6),
    ) {
        items(visible, key = ProfileLink::id) { link ->
            ReorderableItem(reorder, key = link.id) { isDragging ->
                val interaction = remember { MutableInteractionSource() }
                ProfileLinkRow(
                    link = link,
                    selected = true,
                    onToggle = { repository.setLinkSelected(link.id, false) },
                    onEdit = { onEdit(link) },
                    dragHandle = {
                        Icon(
                            Icons.Default.DragHandle,
                            stringResource(R.string.hold_drag_reorder),
                            Modifier.size(Dimens.dp44).longPressDraggableHandle(
                                interactionSource = interaction,
                                onDragStarted = {
                                    dragging = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    repository.setSelectedLinkOrder(visible.map(ProfileLink::id))
                                    dragging = false
                                },
                            ).padding(Dimens.dp10),
                            tint = if (isDragging) Rust else Muted,
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun ProfileLinkRow(
    link: ProfileLink,
    selected: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onEdit).padding(Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Icon(Icons.Default.Link, null, tint = Rust)
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp10)) {
            Text(link.label.ifBlank { profileLinkTypeLabel(link.type) }, fontWeight = FontWeight.SemiBold)
            Text(link.value, color = Muted, fontSize = Dimens.sp12, maxLines = 1)
        }
        dragHandle?.invoke() ?: Icon(Icons.Default.Edit, stringResource(R.string.edit), tint = Muted)
    }
}
