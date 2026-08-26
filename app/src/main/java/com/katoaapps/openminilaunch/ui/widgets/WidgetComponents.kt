package com.katoaapps.openminilaunch.ui.widgets

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*
import com.katoaapps.openminilaunch.ui.launcher.MainActivity

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

/**
 * Keeps a gesture that starts inside a widget with that widget. Without this,
 * the Compose LazyColumn can intercept vertical drags before a RemoteViews list
 * or calendar has a chance to scroll.
 */
internal class InteractiveAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo,
    ): AppWidgetHostView = InteractiveAppWidgetHostView(context)
}

private class InteractiveAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(handled)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }
        return handled
    }

    override fun onDetachedFromWindow() {
        parent?.requestDisallowInterceptTouchEvent(false)
        super.onDetachedFromWindow()
    }
}

@Composable
internal fun UnavailableWidgetPanel(onRetry: () -> Unit, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.dp24),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(Dimens.dp20),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            Icon(Icons.Default.Widgets, null, Modifier.size(Dimens.dp34), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.widget_temporarily_unavailable), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.widget_temporarily_unavailable_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = Dimens.sp12,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                TextButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(Dimens.dp18))
                    Text(stringResource(R.string.retry), Modifier.padding(start = Dimens.dp6))
                }
                TextButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, null, Modifier.size(Dimens.dp18), tint = MaterialTheme.colorScheme.error)
                    Text(stringResource(R.string.remove), Modifier.padding(start = Dimens.dp6), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
internal fun WidgetPanel(
    host: AppWidgetHost,
    id: Int,
    info: AppWidgetProviderInfo,
    gridSize: WidgetGridSize,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onResize: (WidgetGridSize) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizeRange = remember(info.provider, info.minWidth, info.minHeight) {
        widgetSizeRange(info, context.resources.displayMetrics.density)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var showResize by remember { mutableStateOf(false) }
    var measuredSize by remember(id) { mutableStateOf(IntSize.Zero) }
    val lastReportedSize = remember(id) { intArrayOf(0, 0) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = Dimens.dp4, bottom = Dimens.dp6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                info.loadLabel(context.packageManager),
                Modifier.weight(1f),
                color = Muted,
                fontSize = Dimens.sp11,
                fontWeight = FontWeight.Bold,
                letterSpacing = Dimens.sp0_5,
                maxLines = 1,
            )
            Box {
                IconButton(onClick = { menuExpanded = true }, Modifier.size(Dimens.dp32)) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.widget_options), Modifier.size(Dimens.dp19), tint = Muted)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (sizeRange.isResizable) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.resize_widget, gridSize.displayLabel())) },
                            leadingIcon = { Icon(Icons.Default.Widgets, null) },
                            onClick = { menuExpanded = false; showResize = true },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_up)) },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, null) },
                        enabled = canMoveUp,
                        onClick = { menuExpanded = false; onMoveUp() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_down)) },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, null) },
                        enabled = canMoveDown,
                        onClick = { menuExpanded = false; onMoveDown() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onRemove() },
                    )
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cellWidth = maxWidth / 4
            val panelWidth = (cellWidth * gridSize.columns)
                .coerceAtLeast(Dimens.dp96)
                .coerceAtMost(maxWidth)
            val panelHeight = (cellWidth * gridSize.rows)
                .coerceAtLeast(Dimens.dp72)
                .coerceAtMost(Dimens.dp420)

            AndroidView(
                factory = {
                    host.createView(it, id, info).apply {
                        setAppWidget(id, info)
                        ViewCompat.setNestedScrollingEnabled(this, true)
                    }
                },
                update = { view ->
                    if (measuredSize != IntSize.Zero &&
                        (lastReportedSize[0] != measuredSize.width || lastReportedSize[1] != measuredSize.height)
                    ) {
                        reportWidgetSize(view, measuredSize, density.density)
                        lastReportedSize[0] = measuredSize.width
                        lastReportedSize[1] = measuredSize.height
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(panelWidth / maxWidth)
                    .height(panelHeight)
                    .clip(RoundedCornerShape(Dimens.dp24))
                    .onSizeChanged { measuredSize = it },
            )
        }
    }
    if (showResize) {
        WidgetSizeDialog(
            info = info,
            current = gridSize,
            confirmLabelRes = R.string.apply,
            onConfirm = { showResize = false; onResize(it) },
            onDismiss = { showResize = false },
        )
    }
}

@Composable
internal fun WidgetSizeDialog(
    info: AppWidgetProviderInfo,
    current: WidgetGridSize,
    @androidx.annotation.StringRes confirmLabelRes: Int,
    onConfirm: (WidgetGridSize) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val range = remember(info.provider, info.minWidth, info.minHeight) {
        widgetSizeRange(info, context.resources.displayMetrics.density)
    }
    var selected by remember(info.provider, current) {
        mutableStateOf(
            WidgetGridSize(
                current.columns.coerceIn(range.minColumns, range.maxColumns),
                current.rows.coerceIn(range.minRows, range.maxRows),
            ),
        )
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.dp26),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp22), verticalArrangement = Arrangement.spacedBy(Dimens.dp14)) {
                Text(stringResource(R.string.widget_size), fontSize = Dimens.sp22, fontWeight = FontWeight.Black)
                Text(info.loadLabel(context.packageManager), color = Muted, fontSize = Dimens.sp13)
                Text(selected.displayLabel(), fontSize = Dimens.sp32, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.width), color = Muted, fontSize = Dimens.sp10, fontWeight = FontWeight.Bold, letterSpacing = Dimens.sp1)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                    (range.minColumns..range.maxColumns).forEach { columns ->
                        FilterChip(
                            selected = selected.columns == columns,
                            onClick = { selected = selected.copy(columns = columns) },
                            label = { Text("$columns") },
                        )
                    }
                }
                Text(stringResource(R.string.height), color = Muted, fontSize = Dimens.sp10, fontWeight = FontWeight.Bold, letterSpacing = Dimens.sp1)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                    (range.minRows..range.maxRows).forEach { rows ->
                        FilterChip(
                            selected = selected.rows == rows,
                            onClick = { selected = selected.copy(rows = rows) },
                            label = { Text("$rows") },
                        )
                    }
                }
                Text(
                    stringResource(R.string.widget_grid_description, stringResource(R.string.app_name)),
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirm(selected) }, modifier = Modifier.padding(start = Dimens.dp8)) { Text(stringResource(confirmLabelRes)) }
                }
            }
        }
    }
}

internal fun widgetSizeRange(info: AppWidgetProviderInfo, density: Float): WidgetSizeRange {
    fun cellsForPixels(pixels: Int, fallback: Int): Int =
        if (pixels > 0) estimateWidgetCells(pixels / density) else fallback

    val defaultColumns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
        info.targetCellWidth
    } else {
        cellsForPixels(info.minWidth, 2)
    }.coerceIn(1, 4)
    val defaultRows = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
        info.targetCellHeight
    } else {
        cellsForPixels(info.minHeight, 2)
    }.coerceIn(1, 5)
    val horizontallyResizable = info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0
    val verticallyResizable = info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0
    val minColumns = (if (horizontallyResizable) cellsForPixels(info.minResizeWidth, defaultColumns) else defaultColumns)
        .coerceIn(1, 4)
    val minRows = (if (verticallyResizable) cellsForPixels(info.minResizeHeight, defaultRows) else defaultRows)
        .coerceIn(1, 5)
    val declaredMaxColumns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) cellsForPixels(info.maxResizeWidth, 4) else 4
    val declaredMaxRows = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) cellsForPixels(info.maxResizeHeight, 5) else 5
    val maxColumns = (if (horizontallyResizable) declaredMaxColumns else defaultColumns).coerceIn(minColumns, 4)
    val maxRows = (if (verticallyResizable) declaredMaxRows else defaultRows).coerceIn(minRows, 5)
    return WidgetSizeRange(
        minColumns = minColumns,
        maxColumns = maxColumns,
        minRows = minRows,
        maxRows = maxRows,
        preferred = WidgetGridSize(defaultColumns.coerceIn(minColumns, maxColumns), defaultRows.coerceIn(minRows, maxRows)),
    )
}

internal fun estimateWidgetCells(sizeDp: Float): Int = ceil((sizeDp + 30f) / 70f).toInt().coerceAtLeast(1)

@Suppress("DEPRECATION")
internal fun reportWidgetSize(view: AppWidgetHostView, size: IntSize, density: Float) {
    val widthDp = size.width / density
    val heightDp = size.height / density
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
    } else {
        view.updateAppWidgetSize(null, widthDp.roundToInt(), heightDp.roundToInt(), widthDp.roundToInt(), heightDp.roundToInt())
    }
}

@Composable
internal fun WidgetProviderDialog(
    providers: List<AppWidgetProviderInfo>,
    actions: DeviceActions,
    onSelect: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val groups = remember(providers) {
        providers.groupBy { it.provider.packageName }
            .map { (packageName, appProviders) ->
                WidgetAppGroup(
                    packageName = packageName,
                    appName = actions.appLabel(packageName),
                    providers = appProviders.sortedBy { it.loadLabel(context.packageManager).toString().lowercase() },
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(.86f),
            shape = RoundedCornerShape(Dimens.dp26),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().padding(Dimens.dp16)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.add_a_widget), Modifier.weight(1f), fontSize = Dimens.sp22, fontWeight = FontWeight.Black)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
                }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
                ) {
                    groups.forEach { group ->
                        item(key = "header:${group.packageName}") {
                            Row(
                                Modifier.fillMaxWidth().padding(top = Dimens.dp10, bottom = Dimens.dp2),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(group.packageName, actions, Dimens.dp34)
                                Text(group.appName, Modifier.padding(start = Dimens.dp10), fontWeight = FontWeight.Bold)
                            }
                        }
                        items(
                            items = group.providers.chunked(2),
                            key = { row -> row.joinToString("|") { it.provider.flattenToShortString() } },
                        ) { rowProviders ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                                rowProviders.forEach { info ->
                                    WidgetPreviewCard(info, Modifier.weight(1f)) { onSelect(info) }
                                }
                                if (rowProviders.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    if (groups.isEmpty()) {
                        item { Text(stringResource(R.string.no_widget_providers), color = Muted, modifier = Modifier.padding(Dimens.dp12)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WidgetPreviewCard(info: AppWidgetProviderInfo, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.densityDpi
    val sizeRange = remember(info.provider, info.minWidth, info.minHeight) {
        widgetSizeRange(info, context.resources.displayMetrics.density)
    }
    val bitmap = remember(info.provider, info.previewImage, density) {
        val drawable = runCatching { info.loadPreviewImage(context, density) }.getOrNull()
            ?: runCatching { info.loadIcon(context, density) }.getOrNull()
        drawable?.let {
            runCatching {
                val sourceWidth = it.intrinsicWidth.coerceAtLeast(1)
                val sourceHeight = it.intrinsicHeight.coerceAtLeast(1)
                val scale = minOf(360f / sourceWidth, 220f / sourceHeight, 1f)
                it.toBitmap(
                    width = (sourceWidth * scale).roundToInt().coerceAtLeast(1),
                    height = (sourceHeight * scale).roundToInt().coerceAtLeast(1),
                ).asImageBitmap()
            }.getOrNull()
        }
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.dp16),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(Dimens.dp10)) {
            Box(
                Modifier.fillMaxWidth().height(Dimens.dp116)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Dimens.dp12))
                    .padding(Dimens.dp8),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Icon(Icons.Default.Widgets, null, Modifier.size(Dimens.dp42), tint = Muted)
                }
            }
            Text(
                info.loadLabel(context.packageManager),
                modifier = Modifier.padding(top = Dimens.dp8),
                fontSize = Dimens.sp12,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                if (sizeRange.isResizable) {
                    stringResource(R.string.widget_resizable, sizeRange.preferred.displayLabel())
                } else {
                    sizeRange.preferred.displayLabel()
                },
                color = Muted,
                fontSize = Dimens.sp10,
                modifier = Modifier.padding(top = Dimens.dp3),
            )
        }
    }
}
