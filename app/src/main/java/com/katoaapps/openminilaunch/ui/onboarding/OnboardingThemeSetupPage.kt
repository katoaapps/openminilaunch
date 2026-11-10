package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.appearance.BundledHomeTheme
import com.katoaapps.openminilaunch.ui.settings.HomeThemeGallery
import com.katoaapps.openminilaunch.ui.theme.Dimens

@Composable
internal fun OnboardingThemeSetupPage(
    store: LauncherStore,
    applyingThemeId: String?,
    onSelectTheme: (BundledHomeTheme) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
        Text(
            stringResource(R.string.home_themes_description),
            fontSize = Dimens.sp18,
        )
        HomeThemeGallery(
            selectedThemeId = store.selectedHomeTheme?.id,
            applyingThemeId = applyingThemeId,
            onSelectTheme = onSelectTheme,
        )
    }
}
