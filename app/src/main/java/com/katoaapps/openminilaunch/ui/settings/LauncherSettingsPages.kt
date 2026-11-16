package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.localization.SupportedAppLanguages
import com.katoaapps.openminilaunch.model.AppFolderAnimationStyle
import com.katoaapps.openminilaunch.model.AppFolderIndicatorStyle
import com.katoaapps.openminilaunch.model.AppGridAssignment
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.HomeLayoutStyle
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.FolderAnimationInlinePreview
import com.katoaapps.openminilaunch.ui.appgrid.AppGridOrderEditor
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownOption
import com.katoaapps.openminilaunch.ui.components.SettingsDropdownRow
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val HOME_LAYOUT_APPLY_FEEDBACK_MILLIS = 650L

@Composable
internal fun LauncherSettingsPage(
    store: LauncherStore,
    onNavigate: (SettingsDestination) -> Unit,
    onHomeLayoutApplied: () -> Unit,
    goBack: () -> Unit,
) {
    var applyingHomeLayout by remember { mutableStateOf<HomeLayoutStyle?>(null) }
    val applyingStyle = applyingHomeLayout
    if (applyingStyle != null) {
        LaunchedEffect(applyingStyle) {
            store.updateHomeLayoutStyle(applyingStyle)
            delay(HOME_LAYOUT_APPLY_FEEDBACK_MILLIS)
            onHomeLayoutApplied()
        }
        AppearanceApplyingScreen(
            title = stringResource(R.string.applying_icon_style),
            selectionLabel = stringResource(
                if (applyingStyle == HomeLayoutStyle.MINK) {
                    R.string.home_layout_mink
                } else {
                    R.string.home_layout_classic
                },
            ),
            description = stringResource(R.string.home_layout),
        )
        return
    }
    val context = LocalContext.current
    val languageSummary = if (SupportedAppLanguages.isSupportedByDevice()) {
        SupportedAppLanguages.selectedLanguageTag(context)?.let { selected ->
            SupportedAppLanguages.all.firstOrNull {
                it.languageTag.equals(selected, ignoreCase = true)
            }?.nativeDisplayName() ?: selected
        } ?: stringResource(R.string.system_default)
    } else {
        stringResource(R.string.app_language_requires_android_13_short)
    }
    SettingsPage(stringResource(R.string.settings_section_home), goBack) {
        SectionLabel(stringResource(R.string.home_layout))
        Surface(
            shape = RoundedCornerShape(Dimens.dp16),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column {
                HomeLayoutChoiceRow(
                    title = stringResource(R.string.home_layout_mink),
                    subtitle = stringResource(R.string.home_layout_mink_description),
                    selected = store.homeLayoutStyle == HomeLayoutStyle.MINK,
                    onClick = {
                        if (store.homeLayoutStyle != HomeLayoutStyle.MINK) {
                            applyingHomeLayout = HomeLayoutStyle.MINK
                        }
                    },
                )
                HomeLayoutChoiceRow(
                    title = stringResource(R.string.home_layout_classic),
                    subtitle = stringResource(R.string.home_layout_classic_description),
                    selected = store.homeLayoutStyle == HomeLayoutStyle.CLASSIC,
                    onClick = {
                        if (store.homeLayoutStyle != HomeLayoutStyle.CLASSIC) {
                            applyingHomeLayout = HomeLayoutStyle.CLASSIC
                        }
                    },
                )
            }
        }
        SectionLabel(stringResource(R.string.customize))
        SettingsRow(
            stringResource(R.string.appearance),
            stringResource(R.string.appearance_summary, stringResource(store.themePreference.labelRes)),
            Icons.Default.Palette,
        ) { onNavigate(SettingsDestination.APPEARANCE) }
        SettingsRow(
            stringResource(R.string.language),
            languageSummary,
            Icons.Default.Translate,
        ) { onNavigate(SettingsDestination.LANGUAGE) }
    }
}

@Composable
private fun HomeLayoutChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = Dimens.dp12, vertical = Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.padding(start = Dimens.dp8)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = Dimens.sp11)
        }
    }
}

@Composable
internal fun AppGridSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    onPickAppGridSlot: (AppGridSlot) -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    val appsRevision by actions.launcherAppsRevision.collectAsState()
    val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val targetKeys = buildSet {
        AppGridSlot.entries.forEach { slot ->
            when (val assignment = store.appGridAssignment(slot)) {
                is AppGridAssignment.Target -> {
                    add(assignment.selectionKey)
                }
                is AppGridAssignment.Folder -> {
                    store.appFolder(assignment.folderId)?.orderedTargetKeys?.let(::addAll)
                }
                AppGridAssignment.DefaultAction -> Unit
            }
        }
    }
    val resolvedTargets by produceState<Map<String, LauncherTarget>>(
        initialValue = emptyMap(),
        targetKeys,
        appsRevision,
        shortcutsRevision,
    ) {
        value = withContext(Dispatchers.IO) {
            targetKeys.associateWith(actions::resolveLauncherSelection)
        }
    }

    SettingsPage(stringResource(R.string.app_grid), goBack) {
        SectionLabel(stringResource(R.string.home_shortcuts))
        AppGridOrderEditor(
            store = store,
            actions = actions,
            resolvedTargets = resolvedTargets,
            onSlotClick = { slot ->
                if (slot == AppGridSlot.DRAWER) {
                    onNavigate(SettingsDestination.QUICK_DRAWER)
                } else {
                    onPickAppGridSlot(slot)
                }
            },
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            SettingsDropdownRow(
                title = stringResource(R.string.folder_indicator),
                selectedValue = store.folderIndicatorStyle,
                options = listOf(
                    SettingsDropdownOption(
                        AppFolderIndicatorStyle.STACKED,
                        stringResource(R.string.folder_indicator_stacked),
                    ),
                    SettingsDropdownOption(
                        AppFolderIndicatorStyle.GRID,
                        stringResource(R.string.folder_indicator_grid),
                    ),
                ),
                onValueSelected = store::updateFolderIndicatorStyle,
                modifier = Modifier.weight(1f),
            )
            SettingsDropdownRow(
                title = stringResource(R.string.folder_animation),
                selectedValue = store.folderAnimationStyle,
                options = listOf(
                    SettingsDropdownOption(
                        AppFolderAnimationStyle.RIPPLE,
                        stringResource(R.string.folder_animation_ripple),
                    ),
                    SettingsDropdownOption(
                        AppFolderAnimationStyle.GROW_AND_SPIN,
                        stringResource(R.string.folder_animation_grow_spin),
                    ),
                    SettingsDropdownOption(
                        AppFolderAnimationStyle.APPEAR,
                        stringResource(R.string.folder_animation_appear),
                    ),
                    SettingsDropdownOption(
                        AppFolderAnimationStyle.RISE,
                        stringResource(R.string.folder_animation_rise),
                    ),
                ),
                onValueSelected = store::updateFolderAnimationStyle,
                modifier = Modifier.weight(1f),
                valuePreview = {
                    FolderAnimationInlinePreview(
                        style = store.folderAnimationStyle,
                        targets = store.appFolders.firstOrNull()?.orderedTargetKeys.orEmpty()
                            .mapNotNull(resolvedTargets::get),
                        actions = actions,
                    )
                },
            )
        }
    }
}
