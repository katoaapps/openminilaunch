package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MagicCallColor
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog

@Composable
internal fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    icon: ImageVector,
    onGrant: () -> Unit,
    onManage: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Dimens.dp14),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp9),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
            Column(Modifier.weight(1f).padding(start = Dimens.dp10)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, fontSize = Dimens.sp12)
            }
            if (granted) {
                Icon(Icons.Default.CheckCircle, stringResource(R.string.granted), tint = MagicCallColor)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onManage) { Text(stringResource(R.string.manage)) }
            if (!granted) FilledTonalButton(onClick = onGrant) { Text(stringResource(R.string.allow)) }
        }
    }
}

@Composable
internal fun LockAccessibilityDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.Lock, null, tint = Rust) },
        title = { Text(stringResource(R.string.enable_double_tap_lock)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                Text(stringResource(R.string.double_tap_disclosure_one, appName))
                Text(stringResource(R.string.double_tap_disclosure_two))
                Text(stringResource(R.string.double_tap_disclosure_three))
            }
        },
        confirmButton = { Button(onClick = onContinue) { Text(stringResource(R.string.continue_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) } },
    )
}

@Composable
internal fun AssistantDisclosureDialog(active: Boolean, onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.Assistant, null, tint = Rust) },
        title = { Text(stringResource(if (active) R.string.assistant_active_title else R.string.assistant_enable_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                Text(stringResource(R.string.assistant_disclosure_one))
                Text(stringResource(R.string.assistant_disclosure_two, appName))
                Text(stringResource(R.string.assistant_disclosure_three, appName))
            }
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(stringResource(if (active) R.string.manage_assistant else R.string.choose_assistant))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun NotificationAccessDisclosureDialog(
    onOpenAppInfo: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    val showRestrictedSettingsStep = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            modifier = Modifier.minkDialogWidth().fillMaxHeight(0.94f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp18)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Forum, null, tint = Rust)
                    Text(
                        stringResource(R.string.enable_conversations),
                        Modifier.weight(1f).padding(start = Dimens.dp10),
                        fontWeight = FontWeight.Black,
                        fontSize = Dimens.sp20,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
                ) {
                    Text(stringResource(R.string.conversations_disclosure_one, appName))
                    if (showRestrictedSettingsStep) {
                        RestrictedSettingsSteps(onOpenNotificationAccess, onOpenAppInfo)
                    }
                    Text(
                        stringResource(R.string.conversations_disclosure_two, appName),
                        color = Muted,
                        fontSize = Dimens.sp12,
                    )
                    Text(stringResource(R.string.conversations_disclosure_three), color = Muted, fontSize = Dimens.sp12)
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = Dimens.dp12),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.dp8, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) }
                    Button(onClick = onOpenNotificationAccess) {
                        Text(
                            stringResource(
                                if (showRestrictedSettingsStep) R.string.open_notification_access_again
                                else R.string.open_notification_access,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestrictedSettingsSteps(
    onOpenNotificationAccess: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(Dimens.dp14)) {
        Text(
            stringResource(R.string.restricted_settings_setup_intro),
            Modifier.padding(Dimens.dp12),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = Dimens.sp12,
        )
    }
    ConversationAccessStep(
        number = "1",
        title = stringResource(R.string.restricted_settings_step_one_title),
        body = stringResource(R.string.restricted_settings_step_one_body),
    )
    OutlinedButton(onClick = onOpenNotificationAccess, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
        Text(stringResource(R.string.try_notification_access), Modifier.padding(start = Dimens.dp8))
    }
    ConversationAccessStep(
        number = "2",
        title = stringResource(R.string.restricted_settings_step_two_title),
        body = stringResource(R.string.restricted_settings_step_two_body),
    )
    Image(
        painter = painterResource(R.drawable.restricted_settings_openmink_example),
        contentDescription = stringResource(R.string.restricted_settings_example_description),
        modifier = Modifier.fillMaxWidth().aspectRatio(1280f / 579f).clip(RoundedCornerShape(Dimens.dp14)),
        contentScale = ContentScale.Crop,
    )
    Text(stringResource(R.string.restricted_settings_example_caption), color = Muted, fontSize = Dimens.sp11)
    OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
        Text(stringResource(R.string.open_app_info), Modifier.padding(start = Dimens.dp8))
    }
    ConversationAccessStep(
        number = "3",
        title = stringResource(R.string.restricted_settings_step_three_title),
        body = stringResource(R.string.restricted_settings_step_three_body),
    )
}

@Composable
private fun ConversationAccessStep(number: String, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
            Text(
                number,
                modifier = Modifier.size(Dimens.dp26).wrapContentSize(Alignment.Center),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Black,
                fontSize = Dimens.sp12,
            )
        }
        Column(Modifier.padding(start = Dimens.dp10)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = Muted, fontSize = Dimens.sp12)
        }
    }
}
