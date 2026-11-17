package com.katoaapps.minkboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardStatus

@Composable
internal fun CardViewDialog(
    card: BoardCard,
    status: BoardStatus,
    isFocusRunningForCard: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onManageSubtasks: () -> Unit,
    onFocus: () -> Unit,
    canSendToTop: Boolean,
    canSendToBottom: Boolean,
    onSendToTop: () -> Unit,
    onSendToBottom: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = Color(status.colorArgb)
    var moreMenuOpen by remember(card.id.value) { mutableStateOf(false) }
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (card.isMajor) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = stringResource(R.string.important),
                        tint = IMPORTANT_COLOR,
                    )
                }
                Text(card.title, modifier = Modifier.weight(1f))
                IconButton(onClick = { moreMenuOpen = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.card_options),
                    )
                }
                DropdownMenu(
                    expanded = moreMenuOpen,
                    onDismissRequest = { moreMenuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.subtasks)) },
                        leadingIcon = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
                        onClick = {
                            moreMenuOpen = false
                            onManageSubtasks()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (isFocusRunningForCard) {
                                        R.string.stop_focus
                                    } else {
                                        R.string.start_focus_timer
                                    },
                                ),
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                        onClick = {
                            moreMenuOpen = false
                            onFocus()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.send_to_top)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.VerticalAlignTop, contentDescription = null)
                        },
                        enabled = canSendToTop,
                        onClick = {
                            moreMenuOpen = false
                            onSendToTop()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.send_to_bottom)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.VerticalAlignBottom, contentDescription = null)
                        },
                        enabled = canSendToBottom,
                        onClick = {
                            moreMenuOpen = false
                            onSendToBottom()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.clone_card)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        },
                        onClick = {
                            moreMenuOpen = false
                            onDuplicate()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            moreMenuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = status.name,
                    modifier = Modifier
                        .background(accent, RoundedCornerShape(99.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    color = readableContentColor(accent),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (card.details.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            card.details,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                card.dueDate?.let { date ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                        Text(date.toString())
                    }
                }
                if (card.subtasks.isNotEmpty()) {
                    val completedCount = card.subtasks.count { it.isCompleted }
                    Text(
                        pluralStringResource(
                            R.plurals.subtask_progress,
                            card.subtasks.size,
                            completedCount,
                            card.subtasks.size,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    card.subtasks.sortedBy { it.position }.forEach { subtask ->
                        Text(
                            text = if (subtask.isCompleted) "✓ ${subtask.text}" else "• ${subtask.text}",
                            color = if (subtask.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Text(stringResource(R.string.edit_card), modifier = Modifier.padding(start = 6.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

internal val IMPORTANT_COLOR = Color(0xFFD6A300)
