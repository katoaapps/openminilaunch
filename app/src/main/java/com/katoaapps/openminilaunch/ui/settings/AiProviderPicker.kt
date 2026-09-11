package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.ai.AiHandoffMode
import com.katoaapps.openminilaunch.features.ai.AiProviderOption
import com.katoaapps.openminilaunch.ui.components.AppIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun AiProviderPickerDialog(
    title: String,
    options: List<AiProviderOption>,
    selectedPackage: String?,
    loading: Boolean,
    showUnavailable: Boolean,
    onProvider: (AiProviderOption) -> Unit,
    onInstall: (AiProviderOption) -> Unit,
    onDismiss: () -> Unit,
    onSeeAllApps: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    resetLabel: String? = null,
) {
    val visibleOptions = remember(options, showUnavailable) {
        options.filter { showUnavailable || it.installed }
    }
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            modifier = Modifier.minkDialogWidth().fillMaxHeight(.96f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp14)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Black,
                        fontSize = Dimens.sp18,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                Text(
                    stringResource(R.string.ai_supported_provider_description),
                    color = Muted,
                    fontSize = Dimens.sp11,
                    modifier = Modifier.padding(bottom = Dimens.dp8),
                )
                when {
                    loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(Dimens.dp24),
                    )
                    visibleOptions.isEmpty() -> Text(
                        stringResource(R.string.no_curated_ai_apps_short),
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(Dimens.dp24),
                        color = Muted,
                    )
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp6),
                    ) {
                        items(visibleOptions, key = AiProviderOption::id) { option ->
                            AiProviderRow(
                                option = option,
                                selected = option.installedPackageName == selectedPackage,
                                onClick = {
                                    if (option.installed) onProvider(option) else onInstall(option)
                                },
                            )
                        }
                    }
                }
                onReset?.let { reset ->
                    OutlinedButton(
                        onClick = reset,
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp8),
                    ) {
                        Text(resetLabel ?: stringResource(R.string.reset_to_chooser))
                    }
                }
                onSeeAllApps?.let { seeAll ->
                    HorizontalDivider(Modifier.padding(top = Dimens.dp8))
                    Button(
                        onClick = seeAll,
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp10),
                    ) {
                        Text(stringResource(R.string.other_compatible_app))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiProviderRow(
    option: AiProviderOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val enabled = !option.installed || option.canHandoff
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else .52f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(Dimens.dp16),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.dp12, vertical = Dimens.dp9),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            option.installedPackageName?.let { packageName ->
                AppIcon(
                    packageName = packageName,
                    actions = null,
                    size = Dimens.dp46,
                    contentDescription = option.label,
                )
            } ?: Image(
                painter = painterResource(option.bundledIconRes),
                contentDescription = option.label,
                modifier = Modifier.size(Dimens.dp46),
            )
            Column(Modifier.weight(1f).padding(start = Dimens.dp10)) {
                Text(
                    option.label,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    aiProviderSubtitle(option),
                    color = Muted,
                    fontSize = Dimens.sp11,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Dimens.dp8))
            Icon(
                imageVector = when {
                    selected -> Icons.Default.CheckCircle
                    !option.installed -> Icons.Default.OpenInNew
                    !option.canHandoff -> Icons.Default.Warning
                    else -> Icons.Default.AutoAwesome
                },
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else Muted,
            )
        }
    }
}

@Composable
private fun aiProviderSubtitle(option: AiProviderOption): String = when {
    !option.installed -> stringResource(R.string.ai_provider_not_installed)
    !option.canHandoff -> stringResource(R.string.ai_provider_handoff_unavailable)
    option.handoffMode == AiHandoffMode.COPY_AND_LAUNCH ->
        stringResource(R.string.ai_provider_copy_and_launch)
    else -> stringResource(R.string.ai_provider_text_share)
}
