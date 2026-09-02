package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.PinShortcutRequestPresentation
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun PinShortcutRequestsSettingsPage(store: LauncherStore, goBack: () -> Unit) {
    SettingsPage(stringResource(R.string.add_to_home_confirmation), goBack) {
        Text(
            stringResource(R.string.pin_shortcut_presentation_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        SectionLabel(stringResource(R.string.presentation))
        PresentationChoice(
            title = stringResource(R.string.pin_shortcut_full_page),
            subtitle = stringResource(R.string.pin_shortcut_full_page_description),
            selected = store.pinShortcutRequestPresentation == PinShortcutRequestPresentation.FULL_PAGE,
        ) {
            store.setPinShortcutRequestPresentation(PinShortcutRequestPresentation.FULL_PAGE)
        }
        PresentationChoice(
            title = stringResource(R.string.pin_shortcut_confirmation_sheet),
            subtitle = stringResource(R.string.pin_shortcut_confirmation_sheet_description),
            selected = store.pinShortcutRequestPresentation ==
                PinShortcutRequestPresentation.CONFIRMATION_SHEET,
        ) {
            store.setPinShortcutRequestPresentation(PinShortcutRequestPresentation.CONFIRMATION_SHEET)
        }
        Text(
            stringResource(R.string.pin_shortcut_test_instructions),
            color = Muted,
            fontSize = Dimens.sp12,
        )
    }
}

@Composable
private fun PresentationChoice(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .clickable(onClick = onClick)
            .padding(Dimens.dp14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = Dimens.dp10)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f) else Muted,
                fontSize = Dimens.sp12,
            )
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}
