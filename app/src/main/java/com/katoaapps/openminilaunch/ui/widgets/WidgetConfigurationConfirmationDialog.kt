package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R

@Composable
internal fun WidgetConfigurationConfirmationDialog(
    info: AppWidgetProviderInfo,
    onUseWidget: () -> Unit,
    onDiscard: () -> Unit,
) {
    val context = LocalContext.current
    val label = info.loadLabel(context.packageManager).toString()

    AlertDialog(
        onDismissRequest = onDiscard,
        title = { Text(stringResource(R.string.widget_setup_confirmation_title, label)) },
        text = { Text(stringResource(R.string.widget_setup_confirmation_description)) },
        confirmButton = {
            TextButton(onClick = onUseWidget) {
                Text(stringResource(R.string.use_widget))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard))
            }
        },
    )
}
