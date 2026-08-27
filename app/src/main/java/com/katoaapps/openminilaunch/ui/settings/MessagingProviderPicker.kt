package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption
import com.katoaapps.openminilaunch.features.messaging.MessagingSupportTier
import com.katoaapps.openminilaunch.ui.components.AppIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog

@Composable
internal fun MessagingProviderPickerDialog(
    title: String,
    options: List<MessagingProviderOption>,
    loading: Boolean,
    selectedProviderId: String?,
    showUnavailable: Boolean,
    onProvider: (MessagingProviderOption) -> Unit,
    onDismiss: () -> Unit,
    onSeeAllApps: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    val visibleOptions = remember(options, query, showUnavailable) {
        options.filter { option ->
            (showUnavailable || option.selectable) &&
                (query.isBlank() || option.label.contains(query.trim(), ignoreCase = true))
        }
    }
    val fullSupport = visibleOptions.filter {
        it.supportTier != MessagingSupportTier.RECIPIENT_IN_APP
    }
    val chooseContact = visibleOptions.filter {
        it.supportTier == MessagingSupportTier.RECIPIENT_IN_APP
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
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.dp8),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text(stringResource(R.string.search_messaging_apps)) },
                )
                when {
                    loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(Dimens.dp24),
                    )
                    visibleOptions.isEmpty() -> Text(
                        stringResource(R.string.no_messaging_providers_found),
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(Dimens.dp24),
                        color = Muted,
                    )
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp6),
                    ) {
                        providerSection(
                            title = R.string.messaging_full_support,
                            options = fullSupport,
                            selectedProviderId = selectedProviderId,
                            onProvider = onProvider,
                        )
                        providerSection(
                            title = R.string.messaging_reselect_contact,
                            options = chooseContact,
                            selectedProviderId = selectedProviderId,
                            onProvider = onProvider,
                        )
                    }
                }
                onSeeAllApps?.let { seeAll ->
                    HorizontalDivider(Modifier.padding(top = Dimens.dp8))
                    Button(
                        onClick = seeAll,
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp10),
                    ) {
                        Icon(Icons.Default.MoreHoriz, null)
                        Text(stringResource(R.string.see_all_messaging_apps), Modifier.padding(start = Dimens.dp8))
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.providerSection(
    @androidx.annotation.StringRes title: Int,
    options: List<MessagingProviderOption>,
    selectedProviderId: String?,
    onProvider: (MessagingProviderOption) -> Unit,
) {
    if (options.isEmpty()) return
    item(key = "header-$title") {
        Text(
            stringResource(title),
            color = Muted,
            fontSize = Dimens.sp10,
            fontWeight = FontWeight.Bold,
            letterSpacing = Dimens.sp1,
            modifier = Modifier.padding(top = Dimens.dp8, bottom = Dimens.dp2),
        )
    }
    items(options, key = MessagingProviderOption::id) { option ->
        MessagingProviderRow(
            option = option,
            selected = option.id == selectedProviderId,
            onClick = { onProvider(option) },
        )
    }
}

@Composable
private fun MessagingProviderRow(
    option: MessagingProviderOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = option.selectable,
        modifier = Modifier.fillMaxWidth().alpha(if (option.selectable) 1f else .42f),
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
            MessagingProviderIcon(option)
            Column(Modifier.weight(1f).padding(start = Dimens.dp10)) {
                Text(
                    option.label,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    providerSubtitle(option),
                    color = Muted,
                    fontSize = Dimens.sp11,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Spacer(Modifier.width(Dimens.dp8))
                Icon(
                    Icons.Default.CheckCircle,
                    stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MessagingProviderIcon(option: MessagingProviderOption) {
    when {
        option.installedPackageName != null -> AppIcon(
            packageName = option.installedPackageName,
            actions = null,
            size = Dimens.dp46,
            contentDescription = option.label,
        )
        option.bundledIconRes != null -> Image(
            painter = painterResource(option.bundledIconRes),
            contentDescription = option.label,
            modifier = Modifier.size(Dimens.dp46),
        )
        option.systemDefault -> Icon(
            Icons.Default.Sms,
            contentDescription = option.label,
            modifier = Modifier.size(Dimens.dp40),
        )
        else -> Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = option.label,
            modifier = Modifier.size(Dimens.dp40),
        )
    }
}

@Composable
private fun providerSubtitle(option: MessagingProviderOption): String = when {
    option.systemDefault -> stringResource(R.string.messaging_system_default_description)
    !option.installed -> stringResource(R.string.messaging_not_installed)
    !option.selectable -> stringResource(R.string.messaging_version_not_supported)
    option.supportTier == MessagingSupportTier.RECIPIENT_IN_APP ->
        stringResource(R.string.messaging_body_only_support)
    else -> stringResource(R.string.messaging_contact_and_body_support)
}
