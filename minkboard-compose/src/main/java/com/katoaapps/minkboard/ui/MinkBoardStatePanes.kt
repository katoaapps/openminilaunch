package com.katoaapps.minkboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun StatusPane(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun LockedBoardPane(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(stringResource(R.string.locked_board_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.locked_board_body), textAlign = TextAlign.Center)
            Button(onClick = onUnlock) {
                Text(stringResource(R.string.unlock_board))
            }
        }
    }
}

@Composable
internal fun ErrorPane(
    error: MinkBoardFatalErrorDisplay,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (error == MinkBoardFatalErrorDisplay.Decryption) {
                        R.string.data_locked_title
                    } else {
                        R.string.data_storage_error_title
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    if (error == MinkBoardFatalErrorDisplay.Decryption) {
                        R.string.data_locked_body
                    } else {
                        R.string.data_storage_error_body
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
