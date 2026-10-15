package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.CenteredActionPrompt
import com.katoaapps.openminilaunch.ui.launcher.MainActivity
import com.katoaapps.openminilaunch.ui.launcher.home.launcherBackgroundContentColor
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlin.math.roundToInt

private const val MAX_WIDGETS = 8

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
    var sizingProvider by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
    var pendingColumns by rememberSaveable { mutableIntStateOf(0) }
    var pendingRows by rememberSaveable { mutableIntStateOf(0) }
    var paneWidthPx by remember { mutableIntStateOf(0) }
    var widgetInfoRevision by remember { mutableIntStateOf(0) }
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val widgetListState = rememberLazyListState()
    val headerContentColor = launcherBackgroundContentColor(store, header = true)
    val backgroundContentColor = launcherBackgroundContentColor(store)

    val bindingController = rememberWidgetBindingController(
        onBound = { appWidgetId, info ->
            val size = if (pendingColumns > 0 && pendingRows > 0) {
                WidgetGridSize(pendingColumns, pendingRows)
            } else {
                widgetSizeRange(info, context.resources.displayMetrics.density).preferred
            }
            store.addWidget(appWidgetId, size)
            pendingColumns = 0
            pendingRows = 0
        },
        onFailed = { failure ->
            pendingColumns = 0
            pendingRows = 0
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

    fun beginWidgetBinding(info: AppWidgetProviderInfo, size: WidgetGridSize) {
        if (paneWidthPx <= 0) return
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

    LaunchedEffect(widgetInfoRevision, launcherAppsRevision, store.widgetIds.size) {
        store.widgetIds.toList().forEach { id ->
            val info = manager.getAppWidgetInfo(id)
            if (info != null && store.widgetSizes[id] == null) {
                store.setWidgetSize(id, widgetSizeRange(info, context.resources.displayMetrics.density).preferred)
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
                if (store.widgetIds.isNotEmpty()) {
                    Text(
                        stringResource(R.string.widget_count, store.widgetIds.size, MAX_WIDGETS),
                        color = headerContentColor.copy(alpha = .64f),
                        fontSize = Dimens.sp12,
                    )
                }
            }
            FilledTonalIconButton(
                onClick = {
                    if (store.widgetIds.size >= MAX_WIDGETS) {
                        Toast.makeText(
                            context,
                            context.resources.getQuantityString(
                                R.plurals.maximum_widgets,
                                MAX_WIDGETS,
                                MAX_WIDGETS,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        showPicker = true
                    }
                },
            ) { Icon(Icons.Default.Add, stringResource(R.string.add_widget)) }
        }

        if (store.widgetIds.isEmpty()) {
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
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = widgetListState,
                    modifier = Modifier.fillMaxSize().padding(start = Dimens.dp20, end = Dimens.dp48),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp20),
                ) {
                    itemsIndexed(store.widgetIds, key = { _, id -> id }) { index, id ->
                        val info = remember(id, widgetInfoRevision, launcherAppsRevision) {
                            manager.getAppWidgetInfo(id)
                        }
                        if (info != null) {
                            WidgetPanel(
                                host = host,
                                id = id,
                                info = info,
                                gridSize = store.widgetSizes[id]
                                    ?: widgetSizeRange(
                                        info,
                                        context.resources.displayMetrics.density,
                                    ).preferred,
                                canMoveUp = index > 0,
                                canMoveDown = index < store.widgetIds.lastIndex,
                                onMoveUp = { store.moveWidget(id, -1) },
                                onMoveDown = { store.moveWidget(id, 1) },
                                onRemove = {
                                    store.removeWidget(id)
                                    runCatching { host.deleteAppWidgetId(id) }
                                },
                                onResize = { store.setWidgetSize(id, it) },
                            )
                        } else {
                            UnavailableWidgetPanel(
                                onRetry = { widgetInfoRevision++ },
                                onRemove = {
                                    store.removeWidget(id)
                                    runCatching { host.deleteAppWidgetId(id) }
                                },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(Dimens.dp32)) }
                }
                if (widgetListState.canScrollBackward || widgetListState.canScrollForward) {
                    WidgetScrollIndicator(
                        state = widgetListState,
                        widgetCount = store.widgetIds.size,
                        contentDescription = stringResource(R.string.scroll_widgets),
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = Dimens.dp10),
                    )
                }
            }
        }
    }

    if (showPicker) {
        WidgetProviderDialog(
            providers = manager.homeScreenProviders(context.packageManager),
            actions = actions,
            onSelect = { info ->
                showPicker = false
                val range = widgetSizeRange(info, context.resources.displayMetrics.density)
                if (range.isResizable) sizingProvider = info else beginWidgetBinding(info, range.preferred)
            },
            onDismiss = { showPicker = false },
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
                beginWidgetBinding(info, size)
            },
            onDismiss = { sizingProvider = null },
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
