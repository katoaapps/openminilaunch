package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R

/** Brief visual confirmation while the new icon style propagates to launcher surfaces. */
@Composable
internal fun IconStyleApplyingScreen(styleLabel: String) {
    AppearanceApplyingScreen(
        title = stringResource(R.string.applying_icon_style),
        selectionLabel = styleLabel,
        description = stringResource(R.string.applying_icon_style_description),
    )
}
