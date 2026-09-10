package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.AppIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.displayLabel
import kotlin.math.roundToInt

@Composable
internal fun WidgetProviderDialog(
    providers: List<AppWidgetProviderInfo>,
    actions: DeviceActions,
    onSelect: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val allGroups = remember(providers) {
        providers.groupBy { it.provider.packageName }
            .map { (packageName, appProviders) ->
                WidgetAppGroup(
                    packageName = packageName,
                    appName = actions.appLabel(packageName),
                    providers = appProviders.sortedBy {
                        it.loadLabel(context.packageManager).toString().lowercase()
                    },
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
    val groups = remember(allGroups, query) {
        val search = query.trim().lowercase()
        if (search.isEmpty()) {
            allGroups
        } else {
            allGroups.mapNotNull { group ->
                if (group.appName.lowercase().contains(search)) {
                    group
                } else {
                    group.copy(
                        providers = group.providers.filter {
                            it.loadLabel(context.packageManager).toString().lowercase().contains(search)
                        },
                    ).takeIf { it.providers.isNotEmpty() }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.94f),
            shape = RoundedCornerShape(Dimens.dp26),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().padding(Dimens.dp16)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.add_a_widget),
                        Modifier.weight(1f),
                        fontSize = Dimens.sp22,
                        fontWeight = FontWeight.Black,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.dp6),
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_widgets)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) ({
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, stringResource(R.string.clear_search))
                        }
                    }) else null,
                )
                WidgetProviderList(
                    groups = groups,
                    query = query,
                    actions = actions,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun WidgetProviderList(
    groups: List<WidgetAppGroup>,
    query: String,
    actions: DeviceActions,
    onSelect: (AppWidgetProviderInfo) -> Unit,
) {
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
                        WidgetPreviewCard(info, actions, Modifier.weight(1f)) { onSelect(info) }
                    }
                    if (rowProviders.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (groups.isEmpty()) {
            item {
                Text(
                    stringResource(if (query.isBlank()) R.string.no_widget_providers else R.string.no_widgets_match),
                    color = Muted,
                    modifier = Modifier.padding(Dimens.dp12),
                )
            }
        }
    }
}

@Composable
internal fun WidgetPreviewCard(
    info: AppWidgetProviderInfo,
    actions: DeviceActions,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.densityDpi
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val sizeRange = remember(info.provider, info.minWidth, info.minHeight) {
        widgetSizeRange(info, context.resources.displayMetrics.density)
    }
    val bitmap = remember(info.provider, info.previewImage, density, launcherAppsRevision) {
        val drawable = runCatching { info.loadPreviewImage(context, density) }.getOrNull()
            ?: runCatching { info.loadIcon(context, density) }.getOrNull()
            ?: actions.appIcon(info.provider.packageName)
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
