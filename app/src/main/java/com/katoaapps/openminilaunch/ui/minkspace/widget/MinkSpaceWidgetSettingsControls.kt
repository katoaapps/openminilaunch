package com.katoaapps.openminilaunch.ui.minkspace.widget

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

/** Widget-specific settings kept outside the generic mini-app ordering row. */
@Composable
internal fun MinkSpaceWidgetSettingsControls(
    appWidgetId: Int?,
    actions: DeviceActions,
    onManage: () -> Unit,
) {
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val appRevision by actions.launcherAppsRevision.collectAsState()
    val selectedWidgetLabel = remember(appWidgetId, appRevision) {
        appWidgetId
            ?.let(manager::getAppWidgetInfo)
            ?.loadLabel(context.packageManager)
            ?.toString()
    }

    selectedWidgetLabel?.let { label ->
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = Dimens.dp6),
        )
    }
    FilledTonalButton(
        onClick = onManage,
        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp6),
    ) {
        Text(
            stringResource(
                if (appWidgetId == null) R.string.add_widget else R.string.replace_widget,
            ),
        )
    }
    Text(
        text = stringResource(R.string.mink_space_widget_compact_warning),
        color = Muted,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = Dimens.dp6),
    )
}
