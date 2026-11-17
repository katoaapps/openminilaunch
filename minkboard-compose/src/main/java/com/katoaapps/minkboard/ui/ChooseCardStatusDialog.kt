package com.katoaapps.minkboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.minkboard.model.BoardStatus

@Composable
internal fun ChooseCardStatusDialog(
    statuses: List<BoardStatus>,
    onDismiss: () -> Unit,
    onSelect: (BoardStatus) -> Unit,
) {
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_status)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                statuses.sortedBy { it.position }.forEach { status ->
                    TextButton(
                        onClick = { onSelect(status) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .background(Color(status.colorArgb), RoundedCornerShape(99.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                        Text(status.name, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
