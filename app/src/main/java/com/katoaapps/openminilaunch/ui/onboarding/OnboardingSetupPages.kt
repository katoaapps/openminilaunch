package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MagicCallColor
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun OnboardingHomeSetupPage(
    appName: String,
    active: Boolean,
    onChooseHome: () -> Unit,
    onManageHome: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
        Text(stringResource(R.string.onboarding_home_setup_description, appName), fontSize = Dimens.sp18)
        SetupStatusCard(
            icon = { Icon(Icons.Default.Home, null, tint = Rust) },
            title = stringResource(R.string.default_home_app),
            description = if (active) {
                stringResource(R.string.onboarding_home_setup_active, appName)
            } else {
                stringResource(R.string.onboarding_home_setup_inactive)
            },
            active = active,
        )
        if (active) {
            FilledTonalButton(onClick = onManageHome, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, null)
                Text(stringResource(R.string.manage), Modifier.padding(start = Dimens.dp8))
            }
        } else {
            Button(onClick = onChooseHome, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Home, null)
                Text(stringResource(R.string.default_home_app), Modifier.padding(start = Dimens.dp8))
            }
        }
    }
}
@Composable
internal fun OnboardingContactsSetupPage(
    granted: Boolean,
    onAllow: () -> Unit,
    onManage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
        Text(stringResource(R.string.onboarding_contacts_setup_description), fontSize = Dimens.sp18)
        SetupStatusCard(
            icon = { Icon(Icons.Default.Contacts, null, tint = Rust) },
            title = stringResource(R.string.contacts),
            description = stringResource(R.string.contacts_permission_description),
            active = granted,
        )
        if (granted) {
            OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.manage))
            }
        } else {
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.allow_contact_access))
            }
        }
    }
}

@Composable
internal fun OnboardingMessagingSetupPage(
    sendAutomatically: Boolean,
    onSendAutomaticallyChanged: (Boolean) -> Unit,
    selectedProvider: String,
    onChooseProvider: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
        Text(stringResource(R.string.onboarding_messages_setup_description), fontSize = Dimens.sp18)
        OutlinedButton(onClick = onChooseProvider, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Chat, null)
            Column(Modifier.padding(start = Dimens.dp10).weight(1f)) {
                Text(stringResource(R.string.integrated_messaging_app_onboarding), fontWeight = FontWeight.SemiBold)
                Text(selectedProvider, color = Muted, fontSize = Dimens.sp12)
            }
        }
        SettingsSwitchRow(
            title = stringResource(R.string.onboarding_send_automatically_title),
            subtitle = stringResource(R.string.onboarding_send_automatically_description),
            checked = sendAutomatically,
            onCheckedChange = onSendAutomaticallyChanged,
        )
    }
}

@Composable
internal fun OnboardingAssistantSetupPage(
    active: Boolean,
    onChooseAssistant: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
        Text(stringResource(R.string.onboarding_assistant_setup_description), fontSize = Dimens.sp18)
        SetupStatusCard(
            icon = { Icon(Icons.Default.Assistant, null, tint = Rust) },
            title = stringResource(R.string.mink_assistant),
            description = stringResource(
                if (active) {
                    R.string.onboarding_assistant_active_description
                } else {
                    R.string.onboarding_assistant_inactive_description
                },
            ),
            active = active,
        )
        OnboardingAssistantBenefit(
            icon = Icons.Default.Sms,
            title = stringResource(R.string.onboarding_assistant_sms_title),
            description = stringResource(R.string.onboarding_assistant_sms_description),
        )
        OnboardingAssistantBenefit(
            icon = Icons.Default.GridView,
            title = stringResource(R.string.onboarding_assistant_quick_apps_title),
            description = stringResource(R.string.onboarding_assistant_quick_apps_description),
        )
        Text(
            stringResource(R.string.deliberate_choice_description),
            color = Muted,
            fontSize = Dimens.sp12,
        )
        if (active) {
            OutlinedButton(onClick = onChooseAssistant, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.manage_mink_assistant))
            }
        } else {
            Button(onClick = onChooseAssistant, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Assistant, null)
                Text(stringResource(R.string.choose_mink_assistant), Modifier.padding(start = Dimens.dp8))
            }
        }
    }
}

@Composable
private fun OnboardingAssistantBenefit(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(Dimens.dp24), tint = Rust)
        Column(Modifier.padding(start = Dimens.dp12).weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = Dimens.sp13)
        }
    }
}

@Composable
private fun SetupStatusCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    active: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(Dimens.dp16))
            .padding(Dimens.dp16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Column(Modifier.padding(start = Dimens.dp12).weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = Dimens.sp13)
        }
        if (active) {
            Icon(
                Icons.Default.CheckCircle,
                stringResource(R.string.granted),
                tint = MagicCallColor,
                modifier = Modifier.size(Dimens.dp24),
            )
        }
    }
}
