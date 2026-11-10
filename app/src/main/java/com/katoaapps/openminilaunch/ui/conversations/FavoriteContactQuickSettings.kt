package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.settings.AppearanceColorDialog
import com.katoaapps.openminilaunch.ui.settings.SettingsColorPickerButton
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.MinkFeatureColors

@Composable
internal fun FavoriteContactQuickSettings(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            shape = RoundedCornerShape(Dimens.dp16),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dimens.dp14, vertical = Dimens.dp12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.contact_action_apps),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.contact_action_apps_description),
                        color = Muted,
                        fontSize = Dimens.sp11,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = Dimens.dp10)) { content() }
        }
    }
}

@Composable
internal fun FavoriteAttentionColorSetting(
    colorArgb: Int?,
    onColorChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val previewColor = colorArgb?.let(::Color) ?: MinkFeatureColors.ConversationAttention
    SettingsColorPickerButton(
        label = stringResource(R.string.favorite_attention_color),
        color = previewColor,
        supportingText = stringResource(R.string.favorite_attention_color_description),
        onClick = { showPicker = true },
        modifier = modifier.fillMaxWidth(),
    )

    if (showPicker) {
        AppearanceColorDialog(
            initialArgb = previewColor.toArgb(),
            titleRes = R.string.favorite_attention_color,
            descriptionRes = R.string.favorite_attention_color_description,
            colorHotphrases = emptyMap(),
            onConfirm = { _, argb ->
                onColorChange(argb)
                showPicker = false
            },
            onUseDefaultAppearance = {
                onColorChange(null)
                showPicker = false
            },
            defaultAppearanceLabelRes = R.string.use_default_attention_color,
            onDismiss = { showPicker = false },
        )
    }
}
