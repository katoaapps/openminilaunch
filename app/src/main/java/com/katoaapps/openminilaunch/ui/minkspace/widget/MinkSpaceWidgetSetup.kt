package com.katoaapps.openminilaunch.ui.minkspace.widget

import android.appwidget.AppWidgetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.MainActivity
import com.katoaapps.openminilaunch.ui.widgets.WidgetConfigurationConfirmationDialog
import com.katoaapps.openminilaunch.ui.widgets.WidgetPickerPresentation
import com.katoaapps.openminilaunch.ui.widgets.WidgetProviderDialog
import com.katoaapps.openminilaunch.ui.widgets.homeScreenProviders
import com.katoaapps.openminilaunch.ui.widgets.rememberWidgetBindingController

/** Reusable picker and Android bind flow for the single optional MinkSpace placement. */
@Composable
internal fun MinkSpaceWidgetSetup(
    visible: Boolean,
    store: LauncherStore,
    actions: DeviceActions,
    onDismiss: () -> Unit,
    onComplete: () -> Unit = onDismiss,
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val density = context.resources.displayMetrics.density
    val appRevision by actions.launcherAppsRevision.collectAsState()
    val viewport = rememberMinkSpaceWidgetViewport(store.twoPanelModeForLargeDisplays)
    val binding = rememberWidgetBindingController(
        onBound = { newId, _ ->
            val oldId = store.minkSpaceWidgetId
            store.setMinkSpaceWidget(newId)
            store.setMinkSpaceMiniAppEnabled(MinkSpaceMiniApp.WIDGET, true)
            if (oldId != null && oldId != newId) {
                runCatching { activity.appWidgetHost.deleteAppWidgetId(oldId) }
            }
            onComplete()
        },
        onFailed = { onDismiss() },
    )

    if (visible && !binding.isBusy) {
        val providers = remember(manager, appRevision) {
            manager.homeScreenProviders(context.packageManager)
        }
        WidgetProviderDialog(
            providers = providers,
            actions = actions,
            presentation = WidgetPickerPresentation(
                titleRes = if (store.minkSpaceWidgetId == null) {
                    R.string.add_widget
                } else {
                    R.string.replace_widget
                },
                descriptionRes = R.string.mink_space_widget_picker_description,
                badgesFor = { info -> minkSpaceWidgetBadges(info, density, viewport) },
            ),
            onSelect = { info ->
                binding.begin(info, minkSpaceWidgetBindingOptions(viewport))
            },
            onDismiss = onDismiss,
        )
    }

    binding.configurationToConfirm?.let { info ->
        WidgetConfigurationConfirmationDialog(
            info = info,
            onUseWidget = binding::useConfiguredWidget,
            onDiscard = binding::discardConfiguredWidget,
        )
    }
}
