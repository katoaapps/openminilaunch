package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.appearance.launcherFontFamily
import com.katoaapps.openminilaunch.model.FontSource
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun FontPickerDialog(
    store: LauncherStore,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val customFamily = remember(store.customFontRevision, store.hasCustomFont) {
        launcherFontFamily(FontSource.CUSTOM, store.customFontFile)
    }
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.9f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier.padding(Dimens.dp14).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.choose_font_style),
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Black,
                        fontSize = Dimens.sp18,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                FontChoice(
                    title = stringResource(R.string.font_default),
                    description = stringResource(R.string.font_default_description),
                    family = FontFamily.Default,
                    selected = store.fontAppearance.source == FontSource.DEFAULT,
                    onClick = { store.useFont(FontSource.DEFAULT) },
                )
                FontChoice(
                    title = stringResource(R.string.font_serif),
                    description = stringResource(R.string.font_serif_description),
                    family = FontFamily.Serif,
                    selected = store.fontAppearance.source == FontSource.SERIF,
                    onClick = { store.useFont(FontSource.SERIF) },
                )
                FontChoice(
                    title = stringResource(R.string.font_monospace),
                    description = stringResource(R.string.font_monospace_description),
                    family = FontFamily.Monospace,
                    selected = store.fontAppearance.source == FontSource.MONOSPACE,
                    onClick = { store.useFont(FontSource.MONOSPACE) },
                )
                if (store.hasCustomFont) {
                    FontChoice(
                        title = store.fontAppearance.customFontName ?: stringResource(R.string.custom_font),
                        description = stringResource(R.string.custom_font_saved_description),
                        family = customFamily,
                        selected = store.fontAppearance.source == FontSource.CUSTOM,
                        onClick = { store.useFont(FontSource.CUSTOM) },
                    )
                }
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FontDownload, null, Modifier.size(Dimens.dp18))
                    Text(stringResource(R.string.choose_font_file), Modifier.padding(start = Dimens.dp8))
                }
                Text(
                    stringResource(R.string.custom_font_file_help),
                    color = Muted,
                    fontSize = Dimens.sp11,
                )
                OutlinedButton(
                    onClick = { uriHandler.openUri(GOOGLE_FONTS_URL) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(Dimens.dp18))
                    Text(stringResource(R.string.browse_free_fonts), Modifier.padding(start = Dimens.dp8))
                }
                Text(
                    stringResource(R.string.custom_font_download_help),
                    color = Muted,
                    fontSize = Dimens.sp11,
                )
                Text(
                    stringResource(R.string.third_party_font_license_notice),
                    color = Muted,
                    fontSize = Dimens.sp11,
                )
            }
        }
    }
}

@Composable
private fun FontChoice(
    title: String,
    description: String,
    family: FontFamily,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    Row(
        Modifier.fillMaxWidth()
            .background(background, RoundedCornerShape(Dimens.dp16))
            .clickable(onClick = onClick)
            .padding(Dimens.dp12),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Aa", fontFamily = family, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = family, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = Dimens.sp11)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
internal fun fontSourceLabel(source: FontSource, customName: String?): String = when (source) {
    FontSource.DEFAULT -> stringResource(R.string.font_default)
    FontSource.SERIF -> stringResource(R.string.font_serif)
    FontSource.MONOSPACE -> stringResource(R.string.font_monospace)
    FontSource.CUSTOM -> customName ?: stringResource(R.string.custom_font)
}

private const val GOOGLE_FONTS_URL = "https://fonts.google.com/"
