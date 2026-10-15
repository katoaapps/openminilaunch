package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.CenteredActionPrompt

/** Permanent final MinkSpace page that leads to mini-app configuration. */
@Composable
internal fun MinkSpaceSettingsShortcutPage(
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    containerColor: Color,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MinkSpaceMiniAppSurface(
        compact = compact,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        CenteredActionPrompt(
            icon = Icons.Default.ViewCarousel,
            title = stringResource(R.string.mink_space_mini_apps),
            description = stringResource(R.string.mink_space_settings_summary),
            actionLabel = stringResource(R.string.open_settings),
            actionIcon = Icons.Default.Settings,
            onAction = onOpenSettings,
            contentColor = contentColor,
            mutedContentColor = mutedContentColor,
            iconColor = contentColor,
            compact = compact,
            clickEntirePrompt = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
