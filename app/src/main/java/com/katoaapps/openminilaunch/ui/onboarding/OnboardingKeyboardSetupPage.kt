package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.model.LauncherKeyboardStyle
import com.katoaapps.openminilaunch.model.launcherKeyboardStyleOptions
import com.katoaapps.openminilaunch.ui.keyboard.KeyboardChoiceGrid
import com.katoaapps.openminilaunch.ui.keyboard.KeyboardDevicePreview
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun OnboardingKeyboardSetupPage(
    preferences: LauncherKeyboardPreferences,
    themePrimaryArgb: Int,
    themeSecondaryArgb: Int,
    onStyle: (LauncherKeyboardStyle) -> Unit,
) {
    var previewValue by remember { mutableStateOf(TextFieldValue()) }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp14)) {
        Text(
            text = stringResource(R.string.onboarding_keyboard_description),
            fontSize = Dimens.sp18,
        )
        KeyboardDevicePreview(
            value = previewValue,
            onValueChange = { previewValue = it },
            preferences = preferences,
            themePrimaryArgb = themePrimaryArgb,
            themeSecondaryArgb = themeSecondaryArgb,
            maxPreviewHeight = 300.dp,
        )
        KeyboardChoiceGrid(
            options = launcherKeyboardStyleOptions,
            columns = 3,
            selected = preferences.style,
            label = { style -> stringResource(style.labelRes) },
            onSelect = onStyle,
        )
        Text(
            text = stringResource(R.string.onboarding_keyboard_change_later),
            color = Muted,
            fontSize = Dimens.sp13,
        )
    }
}
