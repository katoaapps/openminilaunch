package com.katoaapps.openminilaunch.ui.minkspace.widget

import android.appwidget.AppWidgetManager
import android.os.UserManager
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.ViewCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.CenteredActionPrompt
import com.katoaapps.openminilaunch.ui.launcher.MainActivity
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSettingsTarget
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceScope

/** Widget implementation registered with the MinkSpace platform. */
internal class WidgetMinkSpaceApp(
    private val store: LauncherStore,
    private val actions: DeviceActions,
) : MinkSpaceApp {
    override val manifest: MinkSpaceMiniAppManifest = Manifest

    @Composable
    override fun Content(scope: MinkSpaceScope, modifier: Modifier) {
        val context = LocalContext.current
        val activity = context as MainActivity
        val manager = remember(context) { AppWidgetManager.getInstance(context) }
        val appRevision by actions.launcherAppsRevision.collectAsState()
        val appWidgetId = store.minkSpaceWidgetId
        val info = remember(appWidgetId, appRevision) {
            appWidgetId?.let(manager::getAppWidgetInfo)
        }
        var showSetup by rememberSaveable { mutableStateOf(false) }

        scope.Surface(modifier = modifier) {
            when {
                appWidgetId == null -> CenteredActionPrompt(
                    icon = Icons.Default.Widgets,
                    title = stringResource(R.string.quick_widget),
                    description = stringResource(R.string.mink_space_widget_description),
                    actionLabel = stringResource(R.string.add_widget),
                    actionIcon = Icons.Default.Add,
                    onAction = { showSetup = true },
                    contentColor = scope.contentColor,
                    mutedContentColor = scope.mutedContentColor,
                    compact = scope.compact,
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
                    contentColor = scope.contentColor,
                    mutedContentColor = scope.mutedContentColor,
                    compact = scope.compact,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> key(appWidgetId) {
                    val providerPackage = info.provider.packageName
                    val infoLabel = info.loadLabel(context.packageManager).toString()
                    val providerUserSerial = remember(info.profile) {
                        context.getSystemService(UserManager::class.java)
                            ?.getSerialNumberForUser(info.profile)
                            ?.takeIf { it >= 0L }
                    }
                    val providerTarget = remember(providerPackage, providerUserSerial, appRevision) {
                        actions.installedApps().firstOrNull { target ->
                            target.packageName == providerPackage &&
                                (providerUserSerial == null || target.userSerial == providerUserSerial)
                        }
                    }
                    WidgetMiniAppContent(
                        appWidgetId = appWidgetId,
                        infoLabel = infoLabel,
                        compact = scope.compact,
                        expansion = scope.expansion,
                        contentColor = scope.contentColor,
                        hostViewFactory = {
                            activity.appWidgetHost.createView(context, appWidgetId, info).apply {
                                setAppWidget(appWidgetId, info)
                                ViewCompat.setNestedScrollingEnabled(this, true)
                            }
                        },
                        onOpenProvider = {
                            val opened = providerTarget?.let(actions::launchLauncherTarget) == true ||
                                actions.launchPackage(providerPackage)
                            if (!opened) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.could_not_open_app, infoLabel),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                        onRemove = {
                            scope.expansion.collapse()
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

    companion object {
        val Manifest = MinkSpaceMiniAppManifest(
            miniApp = MinkSpaceMiniApp.WIDGET,
            labelRes = R.string.quick_widget,
            icon = Icons.Default.Widgets,
            supportsExpansion = true,
            showExpansionButton = true,
            settingsTarget = MinkSpaceMiniAppSettingsTarget.WIDGET,
        )
    }
}
