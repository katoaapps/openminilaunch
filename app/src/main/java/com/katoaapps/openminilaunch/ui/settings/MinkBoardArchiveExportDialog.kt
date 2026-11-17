package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.ui.MinkBoardAlertDialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun MinkBoardArchiveExportDialog(
    visible: Boolean,
    boards: List<BoardSnapshot>,
    selectedBoardIds: Set<BoardId>,
    onSelectionChange: (Set<BoardId>) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
) {
    if (!visible) return
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.minkboard_select_boards_to_export)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                Text(stringResource(R.string.minkboard_select_boards_to_export_description))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            onSelectionChange(boards.mapTo(linkedSetOf()) { it.board.id })
                        },
                    ) {
                        Text(stringResource(R.string.minkboard_select_all))
                    }
                    TextButton(onClick = { onSelectionChange(emptySet()) }) {
                        Text(stringResource(R.string.minkboard_select_none))
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    boards.forEach { snapshot ->
                        val selected = snapshot.board.id in selectedBoardIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectionChange(
                                        if (selected) {
                                            selectedBoardIds - snapshot.board.id
                                        } else {
                                            selectedBoardIds + snapshot.board.id
                                        },
                                    )
                                }
                                .padding(vertical = Dimens.dp6),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { checked ->
                                    onSelectionChange(
                                        if (checked) {
                                            selectedBoardIds + snapshot.board.id
                                        } else {
                                            selectedBoardIds - snapshot.board.id
                                        },
                                    )
                                },
                            )
                            Text(snapshot.board.name, Modifier.weight(1f))
                            if (snapshot.board.isLocked) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = stringResource(
                                        R.string.minkboard_protected_board,
                                    ),
                                    tint = Muted,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedBoardIds.isNotEmpty(),
                onClick = onExport,
            ) {
                Text(stringResource(R.string.minkboard_export_archive))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

internal fun suggestedOpenMinkArchiveName(boards: List<BoardSnapshot>): String {
    val singleBoardName = boards.singleOrNull()?.board?.name
        ?.replace(Regex("[^A-Za-z0-9._-]+"), "-")
        ?.trim('-')
        ?.take(48)
        ?.takeIf(String::isNotBlank)
    return if (singleBoardName == null) {
        "OpenMink-boards-${System.currentTimeMillis()}.minkboard"
    } else {
        "$singleBoardName.minkboard"
    }
}
