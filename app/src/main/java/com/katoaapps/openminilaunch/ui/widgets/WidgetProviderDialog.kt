package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.AppIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import java.util.Locale

@Composable
internal fun WidgetProviderDialog(
    providers: List<AppWidgetProviderInfo>,
    actions: DeviceActions,
    onSelect: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
    presentation: WidgetPickerPresentation = WidgetPickerPresentation(),
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    var query by remember { mutableStateOf("") }
    val allGroups = remember(providers, locale) {
        providers.groupBy { it.provider.packageName }
            .map { (packageName, appProviders) ->
                WidgetAppGroup(
                    packageName = packageName,
                    appName = actions.appLabel(packageName),
                    providers = appProviders.sortedBy {
                        it.loadLabel(context.packageManager).toString().lowercase(locale)
                    },
                )
            }
            .sortedBy { it.appName.lowercase(locale) }
    }
    val groups = remember(allGroups, query, locale) {
        val search = query.trim().lowercase(locale)
        if (search.isEmpty()) {
            allGroups
        } else {
            allGroups.mapNotNull { group ->
                if (group.appName.lowercase(locale).contains(search)) {
                    group
                } else {
                    group.copy(
                        providers = group.providers.filter {
                            it.loadLabel(context.packageManager).toString()
                                .lowercase(locale)
                                .contains(search)
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
                        stringResource(presentation.titleRes),
                        Modifier.weight(1f),
                        fontSize = Dimens.sp22,
                        fontWeight = FontWeight.Black,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                presentation.descriptionRes?.let { descriptionRes ->
                    Text(
                        stringResource(descriptionRes),
                        color = Muted,
                        fontSize = Dimens.sp12,
                        modifier = Modifier.padding(bottom = Dimens.dp10),
                    )
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
                    badgesFor = presentation.badgesFor,
                    onSelect = onSelect,
                )
            }
        }
    }
}

internal data class WidgetProviderBadges(
    @param:StringRes val fitLabelRes: Int,
    val needsSetup: Boolean,
)

internal data class WidgetPickerPresentation(
    @param:StringRes val titleRes: Int = R.string.add_a_widget,
    @param:StringRes val descriptionRes: Int? = null,
    val badgesFor: ((AppWidgetProviderInfo) -> WidgetProviderBadges)? = null,
)

internal fun AppWidgetManager.homeScreenProviders(
    packageManager: PackageManager,
): List<AppWidgetProviderInfo> = installedProviders
    .filter { info ->
        info.widgetCategory == 0 ||
            info.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0
    }
    .sortedBy { info ->
        info.loadLabel(packageManager).toString().lowercase(Locale.getDefault())
    }

@Composable
private fun WidgetProviderList(
    groups: List<WidgetAppGroup>,
    query: String,
    actions: DeviceActions,
    badgesFor: ((AppWidgetProviderInfo) -> WidgetProviderBadges)?,
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
                        WidgetPreviewCard(
                            info = info,
                            actions = actions,
                            modifier = Modifier.weight(1f),
                            badges = badgesFor?.invoke(info),
                            onClick = { onSelect(info) },
                        )
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
