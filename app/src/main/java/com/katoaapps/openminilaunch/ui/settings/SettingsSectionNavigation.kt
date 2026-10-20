package com.katoaapps.openminilaunch.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens

internal enum class SettingsSection(
    @param:StringRes val labelRes: Int,
    @param:StringRes val compactLabelRes: Int = labelRes,
    val icon: ImageVector,
) {
    HOME(R.string.settings_section_home, icon = Icons.Default.Home),
    MAGIC_BOX(R.string.magic_box, icon = Icons.Default.AutoAwesome),
    ADVANCED(
        R.string.settings_section_advanced,
        R.string.settings_tab_advanced,
        Icons.Default.Star,
    ),
    MORE(R.string.settings_section_more, icon = Icons.Default.MoreHoriz),
}

internal fun settingsSectionFor(destination: SettingsDestination): SettingsSection = when (destination) {
    SettingsDestination.APP_BUBBLES,
    SettingsDestination.FILE_SEARCH -> SettingsSection.MAGIC_BOX

    SettingsDestination.MINK_ASSISTANT,
    SettingsDestination.MINK_DAY -> SettingsSection.ADVANCED

    SettingsDestination.BACKUP_RESTORE,
    SettingsDestination.PERMISSIONS,
    SettingsDestination.ABOUT -> SettingsSection.MORE

    else -> SettingsSection.HOME
}

@Composable
internal fun SettingsBottomNavigation(
    selectedSection: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = Dimens.dp0,
    ) {
        SettingsSection.entries.forEach { section ->
            NavigationBarItem(
                selected = section == selectedSection,
                onClick = { onSectionSelected(section) },
                icon = { Icon(section.icon, contentDescription = null) },
                label = { Text(stringResource(section.compactLabelRes), maxLines = 1) },
            )
        }
    }
}

@Composable
internal fun SettingsSideNavigation(
    selectedSection: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.statusBarsPadding().padding(
                horizontal = Dimens.dp12,
                vertical = Dimens.dp16,
            ),
        ) {
            Text(
                text = stringResource(R.string.settings),
                modifier = Modifier.padding(
                    horizontal = Dimens.dp16,
                    vertical = Dimens.dp12,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            SettingsSection.entries.forEach { section ->
                NavigationDrawerItem(
                    label = { Text(stringResource(section.labelRes)) },
                    selected = section == selectedSection,
                    onClick = { onSectionSelected(section) },
                    icon = { Icon(section.icon, contentDescription = null) },
                    modifier = Modifier.padding(vertical = Dimens.dp2),
                )
            }
        }
    }
}
