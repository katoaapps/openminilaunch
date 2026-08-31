package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.displayLabel

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
            Icon(
                Icons.Default.Widgets,
                null,
                Modifier.size(Dimens.dp34),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    Icon(
                        Icons.Default.Close,
                        null,
                        Modifier.size(Dimens.dp18),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        stringResource(R.string.remove),
                        Modifier.padding(start = Dimens.dp6),
                        color = MaterialTheme.colorScheme.error,
                    )
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
    val nestedScrollInterop = rememberNestedScrollInteropConnection()
    val sizeRange = remember(info.provider, info.minWidth, info.minHeight) {
        widgetSizeRange(info, context.resources.displayMetrics.density)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var showResize by remember { mutableStateOf(false) }
    var measuredSize by remember(id) { mutableStateOf(IntSize.Zero) }
    val lastReportedSize = remember(id) { intArrayOf(0, 0) }

    Column(Modifier.fillMaxWidth()) {
        WidgetPanelHeader(
            label = info.loadLabel(context.packageManager),
            gridSize = gridSize,
            sizeResizable = sizeRange.isResizable,
            menuExpanded = menuExpanded,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onShowMenu = { menuExpanded = true },
            onDismissMenu = { menuExpanded = false },
            onResize = { menuExpanded = false; showResize = true },
            onMoveUp = { menuExpanded = false; onMoveUp() },
            onMoveDown = { menuExpanded = false; onMoveDown() },
            onRemove = { menuExpanded = false; onRemove() },
        )
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
                    .nestedScroll(nestedScrollInterop)
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
private fun WidgetPanelHeader(
    label: String,
    gridSize: WidgetGridSize,
    sizeResizable: Boolean,
    menuExpanded: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onResize: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = Dimens.dp4, bottom = Dimens.dp6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            Modifier.weight(1f),
            color = Muted,
            fontSize = Dimens.sp11,
            fontWeight = FontWeight.Bold,
            letterSpacing = Dimens.sp0_5,
            maxLines = 1,
        )
        Box {
            IconButton(onClick = onShowMenu, Modifier.size(Dimens.dp32)) {
                Icon(
                    Icons.Default.MoreVert,
                    stringResource(R.string.widget_options),
                    Modifier.size(Dimens.dp19),
                    tint = Muted,
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                if (sizeResizable) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.resize_widget, gridSize.displayLabel())) },
                        leadingIcon = { Icon(Icons.Default.Widgets, null) },
                        onClick = onResize,
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_up)) },
                    leadingIcon = { Icon(Icons.Default.ArrowUpward, null) },
                    enabled = canMoveUp,
                    onClick = onMoveUp,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_down)) },
                    leadingIcon = { Icon(Icons.Default.ArrowDownward, null) },
                    enabled = canMoveDown,
                    onClick = onMoveDown,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = onRemove,
                )
            }
        }
    }
}
