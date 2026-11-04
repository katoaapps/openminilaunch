package com.katoaapps.openminilaunch.ui.appgrid

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth

/** Recovery actions for a saved launcher target whose app is no longer available. */
@Composable
internal fun UnavailableLauncherTargetDialog(
    target: LauncherTarget,
    actions: DeviceActions,
    preferredWebPackage: String?,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val reinstall = {
        val opened = actions.webSearch(
            query = "Download ${target.packageName}",
            preferredPackage = preferredWebPackage,
        )
        if (!opened) {
            Toast.makeText(context, R.string.no_browser_available, Toast.LENGTH_SHORT).show()
        }
    }
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        title = { Text(stringResource(R.string.app_unavailable)) },
        text = { Text(stringResource(R.string.launcher_app_unavailable, target.label)) },
        confirmButton = {
            Row {
                TextButton(
                    onClick = reinstall,
                ) {
                    Text(stringResource(R.string.reinstall))
                }
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.remove))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
