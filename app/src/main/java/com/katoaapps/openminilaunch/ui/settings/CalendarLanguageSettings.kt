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
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.calendar.language.CalendarInputLanguage
import com.katoaapps.openminilaunch.features.calendar.language.CalendarLanguageRegistry
import com.katoaapps.openminilaunch.features.calendar.language.effectiveLanguageTag
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import java.util.Locale

@Composable
internal fun CalendarLanguageSetting(store: LauncherStore) {
    val context = LocalContext.current
    val selected = store.calendarInputLanguage
    val effectiveTag = selected.effectiveLanguageTag(context)
    val supported = CalendarLanguageRegistry.find(effectiveTag) != null

    Text(
        stringResource(R.string.calendar_input_language_description),
        color = Muted,
        fontSize = Dimens.sp13,
    )
    Surface(
        shape = RoundedCornerShape(Dimens.dp16),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            CalendarLanguageRow(
                title = stringResource(R.string.use_app_language),
                subtitle = localeDisplayName(context.resources.configuration.locales[0]),
                selected = selected == CalendarInputLanguage.AppLanguage,
                onClick = { store.setCalendarInputLanguage(CalendarInputLanguage.AppLanguage) },
            )
            CalendarLanguageRow(
                title = stringResource(R.string.use_system_language),
                subtitle = stringResource(R.string.follow_android_system_language),
                selected = selected == CalendarInputLanguage.SystemLanguage,
                onClick = { store.setCalendarInputLanguage(CalendarInputLanguage.SystemLanguage) },
            )
            CalendarLanguageRegistry.supportedLanguageTags.forEach { tag ->
                val option = CalendarInputLanguage.Explicit(tag)
                CalendarLanguageRow(
                    title = localeDisplayName(Locale.forLanguageTag(tag)),
                    subtitle = tag,
                    selected = selected == option,
                    onClick = { store.setCalendarInputLanguage(option) },
                )
            }
        }
    }
    if (!supported) {
        Text(
            stringResource(
                R.string.calendar_language_fallback,
                localeDisplayName(Locale.forLanguageTag(effectiveTag)),
            ),
            color = MaterialTheme.colorScheme.error,
            fontSize = Dimens.sp12,
        )
    }
}

@Composable
private fun CalendarLanguageRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = Dimens.dp12, vertical = Dimens.dp7),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.padding(start = Dimens.dp8)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = Dimens.sp11)
        }
    }
}

private fun localeDisplayName(locale: Locale?): String {
    locale ?: return ""
    return locale.getDisplayName(locale).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
    }
}
