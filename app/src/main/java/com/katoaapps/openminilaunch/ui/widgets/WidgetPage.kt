package com.katoaapps.openminilaunch.ui.widgets

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*
import com.katoaapps.openminilaunch.ui.launcher.MainActivity

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val MINK_WIDGET_HOST_ID = 0x4D494E4B

private enum class WidgetBindingStage { IDLE, BOUND, CONFIGURING }

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
    val manager = remember { AppWidgetManager.getInstance(context) }
    val host = remember { InteractiveAppWidgetHost(context, MINK_WIDGET_HOST_ID) }
    var showPicker by remember { mutableStateOf(false) }
    var sizingProvider by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
    var pendingId by remember { mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }
    var pendingSize by remember { mutableStateOf<WidgetGridSize?>(null) }
    var bindingStage by remember { mutableStateOf(WidgetBindingStage.IDLE) }
    var widgetInfoRevision by remember { mutableIntStateOf(0) }
    val widgetListState = rememberLazyListState()

    fun abandonPendingWidget() {
        if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) host.deleteAppWidgetId(pendingId)
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingSize = null
        bindingStage = WidgetBindingStage.IDLE
    }

    fun finishPendingWidget() {
        if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val size = pendingSize ?: manager.getAppWidgetInfo(pendingId)?.let {
                widgetSizeRange(it, context.resources.displayMetrics.density).preferred
            }
            if (size != null) store.addWidget(pendingId, size)
        }
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingSize = null
        bindingStage = WidgetBindingStage.IDLE
    }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) bindingStage = WidgetBindingStage.BOUND else abandonPendingWidget()
    }

    fun beginWidgetBinding(info: AppWidgetProviderInfo, size: WidgetGridSize) {
        val id = host.allocateAppWidgetId()
        pendingId = id
        pendingSize = size
        val cellWidthDp = ((context.resources.configuration.screenWidthDp - 40).coerceAtLeast(280) / 4f)
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, (cellWidthDp * size.columns).roundToInt())
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, (cellWidthDp * size.columns).roundToInt())
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, (cellWidthDp * size.rows).roundToInt())
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, (cellWidthDp * size.rows).roundToInt())
        }
        if (manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, options)) {
            bindingStage = WidgetBindingStage.BOUND
        } else {
            bindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options),
            )
        }
    }

    // Widget binding and provider configuration are separate Android activities. Keep the
    // allocated ID pending until both stages succeed so cancelled setup leaves no orphan ID.
    LaunchedEffect(bindingStage, pendingId) {
        if (bindingStage != WidgetBindingStage.BOUND || pendingId == AppWidgetManager.INVALID_APPWIDGET_ID) return@LaunchedEffect
        val info = manager.getAppWidgetInfo(pendingId)
        if (info == null) {
            abandonPendingWidget()
        } else if (info.configure != null) {
            bindingStage = WidgetBindingStage.CONFIGURING
            val launched = activity.configureAppWidget(host, pendingId) { configured ->
                if (configured) finishPendingWidget() else abandonPendingWidget()
            }
            if (!launched) {
                abandonPendingWidget()
                Toast.makeText(context, context.getString(R.string.widget_configuration_failed), Toast.LENGTH_SHORT).show()
            }
        } else {
            finishPendingWidget()
        }
    }

    // AppWidgetHost only receives RemoteViews updates while listening. Mirror the activity
    // lifecycle rather than leaving the host active when this page is no longer visible.
    DisposableEffect(activity, host) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    runCatching { host.startListening() }
                    widgetInfoRevision++
                }
                Lifecycle.Event.ON_STOP -> host.stopListening()
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            runCatching { host.startListening() }
            widgetInfoRevision++
        }
        onDispose {
            activity.lifecycle.removeObserver(observer)
            host.stopListening()
        }
    }

    LaunchedEffect(widgetInfoRevision, store.widgetIds.size) {
        store.widgetIds.toList().forEach { id ->
            val info = manager.getAppWidgetInfo(id)
            if (info != null && store.widgetSizes[id] == null) {
                store.setWidgetSize(id, widgetSizeRange(info, context.resources.displayMetrics.density).preferred)
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.dp20, vertical = Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).clickable(onClick = goHome)) {
                Text(stringResource(R.string.widgets), fontSize = Dimens.sp26, fontWeight = FontWeight.Black)
                if (store.widgetIds.isNotEmpty()) {
                    Text(stringResource(R.string.widget_count, store.widgetIds.size, 4), color = Muted, fontSize = Dimens.sp12)
                }
            }
            FilledTonalIconButton(
                onClick = {
                    if (store.widgetIds.size >= 4) {
                        Toast.makeText(
                            context,
                            context.resources.getQuantityString(R.plurals.maximum_widgets, 4, 4),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    else showPicker = true
                },
            ) { Icon(Icons.Default.Add, stringResource(R.string.add_widget)) }
        }

        if (store.widgetIds.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(Dimens.dp28),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Widgets, null, Modifier.size(Dimens.dp58), tint = Sage)
                Text(stringResource(R.string.your_widget_page), fontSize = Dimens.sp24, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = Dimens.dp16))
                Text(stringResource(R.string.your_widget_page_description), color = Muted, modifier = Modifier.padding(vertical = Dimens.dp12))
                Button(onClick = { showPicker = true }) { Icon(Icons.Default.Add, null); Text(stringResource(R.string.add_widget), Modifier.padding(start = Dimens.dp8)) }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = widgetListState,
                    modifier = Modifier.fillMaxSize().padding(start = Dimens.dp20, end = Dimens.dp48),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp20),
                ) {
                    itemsIndexed(store.widgetIds, key = { _, id -> id }) { index, id ->
                        val info = remember(id, widgetInfoRevision) { manager.getAppWidgetInfo(id) }
                        if (info != null) {
                            WidgetPanel(
                                host = host,
                                id = id,
                                info = info,
                                gridSize = store.widgetSizes[id]
                                    ?: widgetSizeRange(info, context.resources.displayMetrics.density).preferred,
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
            providers = manager.installedProviders
                .filter { it.widgetCategory == 0 || it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
                .sortedBy { it.loadLabel(context.packageManager).toString().lowercase() },
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
}
