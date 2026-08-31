package com.katoaapps.openminilaunch.ui.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.UsageInsightsRepository
import com.katoaapps.openminilaunch.features.wellbeing.effectiveTrackedPackages
import com.katoaapps.openminilaunch.model.LaunchableApp
import com.katoaapps.openminilaunch.ui.components.AppIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Selects the apps whose usage contributes to Mink's Day. */
@Composable
internal fun SocialAppsDialog(
    store: LauncherStore,
    repository: UsageInsightsRepository,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val apps by produceState<List<LaunchableApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { repository.launchableApps() }
    }
    val visible = remember(apps, query) {
        apps.orEmpty().filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val automaticPackages by produceState<Set<String>>(initialValue = emptySet(), apps) {
        value = withContext(Dispatchers.IO) { repository.automaticSocialPackages(apps.orEmpty()) }
    }
    val selectedPackages = effectiveTrackedPackages(
        store.socialPackages.toSet(),
        automaticPackages,
        store.usesAutomaticSocialApps,
    )
    LaunchedEffect(apps) {
        apps?.takeIf { it.isNotEmpty() }
            ?.let { store.reconcileSocialApps(it.map(LaunchableApp::packageName).toSet()) }
    }

    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.96f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp14)) {
                SocialAppsDialogHeader(store, automaticPackages, onDismiss)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.find_app)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.dp8),
                )
                SelectedSocialAppsRow(store, apps.orEmpty(), selectedPackages)
                SocialAppResults(
                    store = store,
                    apps = apps,
                    visibleApps = visible,
                    selectedPackages = selectedPackages,
                    automaticPackages = automaticPackages,
                    query = query,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = store::clearSocialApps) {
                        Text(stringResource(R.string.restore_android_defaults))
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onDismiss) { Text(stringResource(R.string.done)) }
                }
            }
        }
    }
}

@Composable
private fun SocialAppsDialogHeader(
    store: LauncherStore,
    automaticPackages: Set<String>,
    onDismiss: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (store.usesAutomaticSocialApps) {
                stringResource(
                    R.string.automatic_social_count,
                    automaticPackages.size,
                    stringResource(if (automaticPackages.size == 1) R.string.app_singular else R.string.app_plural),
                )
            } else if (store.socialPackages.isEmpty()) {
                stringResource(R.string.no_tracked_apps)
            } else {
                pluralStringResource(
                    R.plurals.tracked_selected_count,
                    store.socialPackages.size,
                    store.socialPackages.size,
                )
            },
            color = Muted,
            fontSize = Dimens.sp13,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, stringResource(R.string.close))
        }
    }
}

@Composable
private fun SelectedSocialAppsRow(
    store: LauncherStore,
    apps: List<LaunchableApp>,
    selectedPackages: Set<String>,
) {
    if (selectedPackages.isEmpty()) return
    Text(
        stringResource(R.string.tracked_tap_to_remove),
        color = Muted,
        fontSize = Dimens.sp10,
        fontWeight = FontWeight.Bold,
        letterSpacing = Dimens.sp1,
        modifier = Modifier.padding(bottom = Dimens.dp4),
    )
    LazyRow(
        Modifier.fillMaxWidth().padding(bottom = Dimens.dp6),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
    ) {
        items(apps.filter { it.packageName in selectedPackages }, key = { it.packageName }) { app ->
            Column(
                Modifier.width(Dimens.dp64).clip(RoundedCornerShape(Dimens.dp12))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { store.replaceSocialApps(selectedPackages - app.packageName) }
                    .padding(horizontal = Dimens.dp4, vertical = Dimens.dp7),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    AppIcon(app.packageName, actions = null, size = Dimens.dp28)
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = Dimens.dp5, y = -Dimens.dp5),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                    ) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.remove_app, app.label),
                            Modifier.size(Dimens.dp14),
                            tint = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
                Text(
                    app.label,
                    fontSize = Dimens.sp9,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp4),
                )
            }
        }
    }
}

@Composable
private fun SocialAppResults(
    store: LauncherStore,
    apps: List<LaunchableApp>?,
    visibleApps: List<LaunchableApp>,
    selectedPackages: Set<String>,
    automaticPackages: Set<String>,
    query: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        when {
            apps == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            visibleApps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (query.isBlank()) R.string.no_launchable_apps else R.string.no_matching_apps),
                    color = Muted,
                )
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(visibleApps, key = { it.packageName }) { app ->
                    val selected = app.packageName in selectedPackages
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            store.replaceSocialApps(
                                if (selected) selectedPackages - app.packageName else selectedPackages + app.packageName,
                            )
                        }.padding(vertical = Dimens.dp5),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.packageName, actions = null, size = Dimens.dp34)
                        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp10)) {
                            Text(app.label, maxLines = 1)
                            if (app.packageName in automaticPackages) {
                                Text(
                                    stringResource(R.string.android_default_social),
                                    color = Rust,
                                    fontSize = Dimens.sp10,
                                )
                            }
                        }
                        Checkbox(
                            checked = selected,
                            onCheckedChange = {
                                store.replaceSocialApps(
                                    if (it) selectedPackages + app.packageName else selectedPackages - app.packageName,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
