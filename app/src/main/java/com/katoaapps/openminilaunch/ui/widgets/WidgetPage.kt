package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.data.toWidgetProviderIdentity
import com.katoaapps.openminilaunch.model.MAX_WIDGET_ENTRIES
import com.katoaapps.openminilaunch.model.WidgetEntry
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.CenteredActionPrompt
import com.katoaapps.openminilaunch.ui.launcher.MainActivity
import com.katoaapps.openminilaunch.ui.launcher.home.launcherBackgroundContentColor
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlin.math.roundToInt

internal data class WidgetAppGroup(
    val packageName: String,
    val appName: String,
    val providers: List<AppWidgetProviderInfo>,
)

internal data class WidgetSizeRange(
    val minColumns: Int,
    val maxColumns: Int,
    val minRows: Int,
    val maxRows: Int,
    val preferred: WidgetGridSize,
) {
    val isResizable: Boolean
        get() = minColumns != maxColumns || minRows != maxRows
}

@Composable
internal fun WidgetPage(store: LauncherStore, actions: DeviceActions, goHome: () -> Unit) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val density = LocalDensity.current.density
    val widgetHorizontalPaddingDp = Dimens.dp20.value + Dimens.dp48.value
    val manager = remember { AppWidgetManager.getInstance(context) }
    val host = activity.appWidgetHost
    var showPicker by remember { mutableStateOf(false) }
    var replacementPlaceholder by remember {
        mutableStateOf<WidgetEntry.RestorePlaceholder?>(null)
    }
    var sizingProvider by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
    var pendingReplacementEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingColumns by rememberSaveable { mutableIntStateOf(0) }
    var pendingRows by rememberSaveable { mutableIntStateOf(0) }
    var paneWidthPx by remember { mutableIntStateOf(0) }
    var widgetInfoRevision by remember { mutableIntStateOf(0) }
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val headerContentColor = launcherBackgroundContentColor(store, header = true)
    val backgroundContentColor = launcherBackgroundContentColor(store)
    val homeScreenProviders = remember(launcherAppsRevision, widgetInfoRevision) {
        manager.homeScreenProviders(context.packageManager)
    }
    val restorablePackageNames = store.widgetEntries.mapNotNull { entry ->
        when (entry) {
            is WidgetEntry.Bound -> entry.provider?.packageName
            is WidgetEntry.RestorePlaceholder -> entry.recipe.provider.packageName
        }
    }.toSet()
    val installedRestorablePackages = remember(restorablePackageNames, launcherAppsRevision) {
        restorablePackageNames.filterTo(mutableSetOf()) { packageName ->
            runCatching { context.packageManager.getApplicationInfo(packageName, 0) }.isSuccess
        }
    }

    val bindingController = rememberWidgetBindingController(
        onBound = { appWidgetId, info ->
            val size = if (pendingColumns > 0 && pendingRows > 0) {
                WidgetGridSize(pendingColumns, pendingRows)
            } else {
                widgetSizeRange(info, context.resources.displayMetrics.density).preferred
            }
            val replacedEntry = pendingReplacementEntryId?.let { replacementId ->
                store.widgetEntries.firstOrNull { it.entryId == replacementId }
            }
            store.addWidget(
                appWidgetId = appWidgetId,
                size = size,
                provider = info.toWidgetProviderIdentity(context.packageManager),
                replacementEntryId = pendingReplacementEntryId,
            )
            if (replacedEntry is WidgetEntry.Bound && replacedEntry.appWidgetId != appWidgetId) {
                runCatching { host.deleteAppWidgetId(replacedEntry.appWidgetId) }
            }
            pendingColumns = 0
            pendingRows = 0
            pendingReplacementEntryId = null
        },
        onFailed = { failure ->
            pendingColumns = 0
            pendingRows = 0
            pendingReplacementEntryId = null
            if (failure == WidgetBindingFailure.BIND_LAUNCH_FAILED ||
                failure == WidgetBindingFailure.CONFIGURATION_LAUNCH_FAILED
            ) {
                Toast.makeText(
                    context,
                    context.getString(R.string.widget_configuration_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
    )

    fun beginWidgetBinding(
        info: AppWidgetProviderInfo,
        size: WidgetGridSize,
        replacementEntryId: String? = null,
    ) {
        if (paneWidthPx <= 0) return
        pendingReplacementEntryId = replacementEntryId
        pendingColumns = size.columns
        pendingRows = size.rows
        val contentWidthDp = paneWidthPx / density - widgetHorizontalPaddingDp
        val dimensions = widgetPanelDimensions(contentWidthDp, size)
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, dimensions.widthDp.roundToInt())
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, dimensions.widthDp.roundToInt())
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, dimensions.heightDp.roundToInt())
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, dimensions.heightDp.roundToInt())
        }
        bindingController.begin(info, options)
    }

    LaunchedEffect(widgetInfoRevision, launcherAppsRevision, store.widgetEntries.size) {
        store.widgetEntries.toList().filterIsInstance<WidgetEntry.Bound>().forEach { entry ->
            val info = manager.getAppWidgetInfo(entry.appWidgetId)
            if (info != null) {
                if (entry.gridSize == null) {
                    store.setWidgetSize(
                        entry.appWidgetId,
                        widgetSizeRange(info, context.resources.displayMetrics.density).preferred,
                    )
                }
                store.updateWidgetProvider(
                    entry.appWidgetId,
                    info.toWidgetProviderIdentity(context.packageManager),
                )
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .onSizeChanged { paneWidthPx = it.width },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.dp20, vertical = Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).clickable(onClick = goHome)) {
                Text(
                    stringResource(R.string.widgets),
                    color = headerContentColor,
                    fontSize = Dimens.sp26,
                    fontWeight = FontWeight.Black,
                )
            }
            FilledTonalIconButton(
                onClick = {
                    if (store.widgetEntries.size >= MAX_WIDGET_ENTRIES) {
                        Toast.makeText(
                            context,
                            context.resources.getQuantityString(
                                R.plurals.maximum_widgets,
                                MAX_WIDGET_ENTRIES,
                                MAX_WIDGET_ENTRIES,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        showPicker = true
                    }
                },
            ) { Icon(Icons.Default.Add, stringResource(R.string.add_widget)) }
        }

        if (store.widgetEntries.isEmpty()) {
            CenteredActionPrompt(
                icon = Icons.Default.Widgets,
                title = stringResource(R.string.your_widget_page),
                description = stringResource(R.string.your_widget_page_description),
                actionLabel = stringResource(R.string.add_widget),
                actionIcon = Icons.Default.Add,
                onAction = { showPicker = true },
                contentColor = backgroundContentColor,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ReorderableWidgetColumn(
                entries = store.widgetEntries,
                host = host,
                manager = manager,
                availableProviders = homeScreenProviders,
                installedPackages = installedRestorablePackages,
                widgetInfoRevision = widgetInfoRevision,
                launcherAppsRevision = launcherAppsRevision,
                onRetryUnavailableWidget = { widgetInfoRevision++ },
                onOrderChange = store::setWidgetOrder,
                onRemove = { entry ->
                    val removed = store.removeWidget(entry.entryId)
                    if (removed is WidgetEntry.Bound) {
                        runCatching { host.deleteAppWidgetId(removed.appWidgetId) }
                    }
                },
                onResize = store::setWidgetSize,
                onBindReplacement = { entryId, info, size ->
                    beginWidgetBinding(info, size, entryId)
                },
                onChooseReplacement = { replacementPlaceholder = it },
                onReinstall = { provider ->
                    actions.webSearch(
                        query = "Download ${provider.appLabel.ifBlank { provider.packageName }} Android app",
                        preferredPackage = store.preferredWebPackage,
                    )
                },
            )
        }
    }

    if (showPicker) {
        WidgetProviderDialog(
            providers = homeScreenProviders,
            actions = actions,
            onSelect = { info ->
                showPicker = false
                pendingReplacementEntryId = null
                val range = widgetSizeRange(info, context.resources.displayMetrics.density)
                if (range.isResizable) sizingProvider = info else beginWidgetBinding(info, range.preferred)
            },
            onDismiss = { showPicker = false },
        )
    }
    replacementPlaceholder?.let { placeholder ->
        WidgetProviderDialog(
            providers = homeScreenProviders
                .filter { it.provider.packageName == placeholder.recipe.provider.packageName },
            actions = actions,
            onSelect = { info ->
                replacementPlaceholder = null
                pendingReplacementEntryId = placeholder.entryId
                val range = widgetSizeRange(info, context.resources.displayMetrics.density)
                if (range.isResizable) sizingProvider = info
                else beginWidgetBinding(info, range.preferred, placeholder.entryId)
            },
            onDismiss = { replacementPlaceholder = null },
            presentation = WidgetPickerPresentation(
                titleRes = R.string.choose_replacement_widget,
                descriptionRes = R.string.choose_replacement_widget_description,
            ),
        )
    }
    sizingProvider?.let { info ->
        val preferred = widgetSizeRange(info, context.resources.displayMetrics.density).preferred
        WidgetSizeDialog(
            info = info,
            current = preferred,
            confirmLabelRes = R.string.add_widget,
            onConfirm = { size ->
                sizingProvider = null
                beginWidgetBinding(info, size, pendingReplacementEntryId)
            },
            onDismiss = {
                sizingProvider = null
                pendingReplacementEntryId = null
            },
        )
    }
    bindingController.configurationToConfirm?.let { info ->
        WidgetConfigurationConfirmationDialog(
            info = info,
            onUseWidget = bindingController::useConfiguredWidget,
            onDiscard = bindingController::discardConfiguredWidget,
        )
    }
}
