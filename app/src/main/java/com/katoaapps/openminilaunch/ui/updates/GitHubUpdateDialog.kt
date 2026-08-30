package com.katoaapps.openminilaunch.ui.updates

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.wellbeing.MinkState
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.wellbeing.MinkSprite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog

@Composable
internal fun GitHubUpdateDialog(
    currentVersion: String,
    availableVersion: String,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
    ) {
        Surface(
            modifier = Modifier.minkDialogWidth(),
            shape = RoundedCornerShape(Dimens.dp28),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = Dimens.dp8,
            shadowElevation = Dimens.dp12,
        ) {
            Column(
                Modifier.padding(Dimens.dp20),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(Dimens.dp140)
                        .clip(RoundedCornerShape(Dimens.dp22))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    MinkSprite(MinkState.PURPOSEFUL, Modifier.size(Dimens.dp116))
                }
                Text(
                    stringResource(R.string.github_update_dialog_title),
                    modifier = Modifier.padding(top = Dimens.dp18),
                    fontSize = Dimens.sp24,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.github_update_version_line, availableVersion, currentVersion),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.github_update_description),
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp18),
                )
                Text(
                    stringResource(R.string.github_update_fdroid_note),
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp10),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .64f),
                    fontSize = Dimens.sp12,
                )
                Button(
                    onClick = onOpenBrowser,
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp18),
                ) {
                    Icon(Icons.Default.OpenInBrowser, null)
                    Text(stringResource(R.string.view_update), Modifier.padding(start = Dimens.dp8))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.remind_me_later))
                }
            }
        }
    }
}

internal fun previewUpdateVersion(currentVersion: String): String {
    val parts = currentVersion.substringBefore('-').substringBefore('+').split('.')
    if (parts.size < 3) return currentVersion
    val patch = parts[2].toIntOrNull() ?: return currentVersion
    return "${parts[0]}.${parts[1]}.${patch + 1}"
}
