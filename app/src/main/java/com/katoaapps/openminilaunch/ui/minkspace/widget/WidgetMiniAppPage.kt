package com.katoaapps.openminilaunch.ui.minkspace.widget

import android.appwidget.AppWidgetManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.ViewCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.CenteredActionPrompt
import com.katoaapps.openminilaunch.ui.launcher.MainActivity
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSurface
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpacePresentationState

/** Home-facing widget placement with explicit browse and interaction gesture ownership. */
@Composable
internal fun WidgetMiniAppPage(
    store: LauncherStore,
    actions: DeviceActions,
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    containerColor: Color,
    presentationState: MinkSpacePresentationState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val appRevision by actions.launcherAppsRevision.collectAsState()
    val appWidgetId = store.minkSpaceWidgetId
    val info = remember(appWidgetId, appRevision) {
        appWidgetId?.let(manager::getAppWidgetInfo)
    }
    val expanded = presentationState.isExpanded(MinkSpaceMiniApp.WIDGET)
    var showSetup by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = expanded) {
        presentationState.collapse(MinkSpaceMiniApp.WIDGET)
    }

    MinkSpaceMiniAppSurface(
        compact = compact,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        when {
            appWidgetId == null -> CenteredActionPrompt(
                icon = Icons.Default.Widgets,
                title = stringResource(R.string.widgets),
                description = stringResource(R.string.mink_space_widget_description),
                actionLabel = stringResource(R.string.add_widget),
                actionIcon = Icons.Default.Add,
                onAction = { showSetup = true },
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                compact = compact,
                modifier = Modifier.fillMaxSize(),
            )
            info == null -> CenteredActionPrompt(
                icon = Icons.Default.Widgets,
                title = stringResource(R.string.widget_temporarily_unavailable),
                description = stringResource(R.string.widget_temporarily_unavailable_description),
                actionLabel = stringResource(R.string.remove),
                actionIcon = Icons.Default.Delete,
                onAction = {
                    store.clearMinkSpaceWidget()
                    runCatching { activity.appWidgetHost.deleteAppWidgetId(appWidgetId) }
                },
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                compact = compact,
                modifier = Modifier.fillMaxSize(),
            )
            else -> key(appWidgetId) {
                WidgetMiniAppContent(
                    appWidgetId = appWidgetId,
                    infoLabel = info.loadLabel(context.packageManager).toString(),
                    compact = compact,
                    expanded = expanded,
                    contentColor = contentColor,
                    hostViewFactory = {
                        activity.appWidgetHost.createView(context, appWidgetId, info).apply {
                            setAppWidget(appWidgetId, info)
                            ViewCompat.setNestedScrollingEnabled(this, true)
                        }
                    },
                    onExpand = {
                        presentationState.setExpanded(MinkSpaceMiniApp.WIDGET, true)
                    },
                    onCollapse = {
                        presentationState.collapse(MinkSpaceMiniApp.WIDGET)
                    },
                    onReplace = { showSetup = true },
                    onRemove = {
                        presentationState.collapse(MinkSpaceMiniApp.WIDGET)
                        store.clearMinkSpaceWidget()
                        runCatching { activity.appWidgetHost.deleteAppWidgetId(appWidgetId) }
                    },
                )
            }
        }
    }

    MinkSpaceWidgetSetup(
        visible = showSetup,
        store = store,
        actions = actions,
        onDismiss = { showSetup = false },
        onComplete = { showSetup = false },
    )
}
