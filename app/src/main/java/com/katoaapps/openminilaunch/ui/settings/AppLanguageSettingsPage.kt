package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.localization.SupportedAppLanguages
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun AppLanguageSettingsPage(goBack: () -> Unit) {
    val context = LocalContext.current
    val languageSelectionSupported = SupportedAppLanguages.isSupportedByDevice()
    val selectedTag = SupportedAppLanguages.selectedLanguageTag(context)
    SettingsPage(stringResource(R.string.language), goBack) {
        Text(
            stringResource(R.string.app_language_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        Surface(
            shape = RoundedCornerShape(Dimens.dp16),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            if (languageSelectionSupported) {
                Column {
                    LanguageChoiceRow(
                        title = stringResource(R.string.system_default),
                        subtitle = stringResource(R.string.system_default_language_description),
                        selected = selectedTag == null,
                        onClick = { SupportedAppLanguages.useSystemLanguage(context) },
                    )
                    SupportedAppLanguages.all.forEach { language ->
                        LanguageChoiceRow(
                            title = language.nativeDisplayName(),
                            subtitle = language.languageTag,
                            selected = language.languageTag.equals(selectedTag, ignoreCase = true),
                            onClick = { SupportedAppLanguages.select(context, language.languageTag) },
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.app_language_requires_android_13),
                    modifier = Modifier.padding(Dimens.dp16),
                    color = Muted,
                    fontSize = Dimens.sp13,
                )
            }
        }
        if (languageSelectionSupported) {
            Text(
                stringResource(R.string.language_screen_refresh_notice),
                color = Muted,
                fontSize = Dimens.sp12,
            )
        }
    }
}

@Composable
private fun LanguageChoiceRow(
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
