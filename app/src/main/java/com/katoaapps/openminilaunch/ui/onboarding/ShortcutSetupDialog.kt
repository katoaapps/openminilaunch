@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katoaapps.openminilaunch.ui.onboarding

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.Shortcut
import com.katoaapps.openminilaunch.model.configurableShortcuts
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.launcher.ShortcutAssignmentRow
import com.katoaapps.openminilaunch.ui.launcher.displayLabel
import com.katoaapps.openminilaunch.ui.launcher.displaySlotLabel
import com.katoaapps.openminilaunch.ui.settings.LauncherTargetPickerDialog
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.LightInk
import com.katoaapps.openminilaunch.ui.theme.LightPaper
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ShortcutSetupDialog(store: LauncherStore, actions: DeviceActions, onFinish: () -> Unit) {
    val context = LocalContext.current
    var pickingShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var installedAppsLoaded by remember { mutableStateOf(false) }
    var installedShortcutsLoaded by remember { mutableStateOf(false) }
    var appListRefresh by remember { mutableIntStateOf(0) }
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val launcherShortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val installedApps by produceState<List<LauncherAppTarget>>(
        initialValue = emptyList(),
        appListRefresh,
        launcherAppsRevision,
    ) {
        value = withContext(Dispatchers.IO) { actions.installedApps() }
        installedAppsLoaded = true
    }
    val installedShortcuts by produceState<List<LauncherShortcutTarget>>(
        initialValue = emptyList(),
        appListRefresh,
        launcherShortcutsRevision,
    ) {
        value = withContext(Dispatchers.IO) { actions.installedShortcuts() }
        installedShortcutsLoaded = true
    }
    DisposableEffect(context) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                actions.invalidateInstalledApps()
                appListRefresh++
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.82f),
            shape = RoundedCornerShape(Dimens.dp30),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().padding(Dimens.dp26)) {
                Icon(Icons.Default.Apps, null, Modifier.size(Dimens.dp46), tint = Rust)
                Text(
                    stringResource(R.string.choose_your_shortcut_apps),
                    fontSize = Dimens.sp28,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = Dimens.dp16),
                )
                Text(
                    stringResource(R.string.shortcut_setup_description),
                    color = Muted,
                    modifier = Modifier.padding(top = Dimens.dp10, bottom = Dimens.dp18),
                )
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
                ) {
                    configurableShortcuts.forEach { shortcut ->
                        val targetKey = store.shortcutTargets[shortcut]
                        ShortcutAssignmentRow(
                            shortcut = shortcut,
                            targetKey = targetKey,
                            actions = actions,
                            subtitle = when {
                                targetKey != null -> actions.launcherTargetLabel(targetKey)
                                shortcut in store.confirmedShortcutChoices -> stringResource(
                                    R.string.shortcut_default,
                                    shortcut.displayLabel(),
                                )
                                else -> stringResource(
                                    R.string.choose_app_or_keep_default,
                                    shortcut.displayLabel(),
                                )
                            },
                        ) { pickingShortcut = shortcut }
                    }
                    if (!store.hasConfirmedAllShortcutChoices()) {
                        OutlinedButton(
                            onClick = store::confirmSystemDefaultsForUnselectedShortcuts,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Restore, null)
                            Text(stringResource(R.string.keep_built_in_defaults_remaining), Modifier.padding(start = Dimens.dp8))
                        }
                    }
                }
                Button(
                    onClick = onFinish,
                    enabled = store.hasConfirmedAllShortcutChoices(),
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp18),
                ) {
                    Text(stringResource(R.string.finish_launcher_setup))
                }
            }
        }
    }

    pickingShortcut?.let { shortcut ->
        val showSamsungWeatherGuide = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        LauncherTargetPickerDialog(
            title = stringResource(R.string.choose_app_for_shortcut, shortcut.displaySlotLabel()),
            apps = installedApps,
            shortcuts = installedShortcuts,
            selected = setOfNotNull(store.shortcutTargets[shortcut]),
            actions = actions,
            appsLoading = !installedAppsLoaded,
            shortcutsLoading = !installedShortcutsLoaded,
            supportingText = if (showSamsungWeatherGuide) stringResource(R.string.samsung_weather_guide) else null,
            supportingActionLabel = if (showSamsungWeatherGuide) stringResource(R.string.open_apps_settings) else null,
            onSupportingAction = actions::openInstalledAppsSettings,
            onTarget = {
                store.assignShortcut(shortcut, it.selectionKey)
                actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
                pickingShortcut = null
            },
            onReset = {
                store.resetShortcut(shortcut)
                actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
                pickingShortcut = null
            },
            resetLabel = stringResource(R.string.restore_shortcut_default, shortcut.displayLabel()),
            onDismiss = { pickingShortcut = null },
        )
    }
}

@Composable
internal fun OnboardingPoint(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Rust, modifier = Modifier.size(Dimens.dp24))
        Column(Modifier.padding(start = Dimens.dp12)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = Dimens.sp14)
        }
    }
}

@Composable
internal fun MagicKeyRow(key: String, description: String) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.dp14))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Dimens.dp11),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(Dimens.dp36).clip(CircleShape).background(LightInk),
            contentAlignment = Alignment.Center,
        ) {
            Text(key, color = LightPaper, fontSize = Dimens.sp20, fontWeight = FontWeight.Black)
        }
        Text(description, Modifier.padding(start = Dimens.dp12), fontWeight = FontWeight.SemiBold)
    }
}
