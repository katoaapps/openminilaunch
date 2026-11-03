@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.notifications.NotificationLightProbe
import com.katoaapps.openminilaunch.features.notifications.NotificationLightProbeColor
import com.katoaapps.openminilaunch.features.notifications.NotificationLightProbeResult
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun NotificationLightProbeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val probe = remember(context) { NotificationLightProbe(context) }
    var result by remember { mutableStateOf<NotificationLightProbeResult?>(null) }
    var physicalResult by remember { mutableStateOf<PhysicalLightResult?>(null) }

    Dialog(
        onDismissRequest = {
            probe.cancel()
            onDismiss()
        },
        properties = MinkDialogDefaults.properties,
    ) {
        Surface(
            modifier = Modifier.minkDialogWidth(),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier.padding(Dimens.dp18).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    stringResource(R.string.notification_light_probe_title),
                    fontSize = Dimens.sp22,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    stringResource(R.string.notification_light_probe_description),
                    color = Muted,
                    modifier = Modifier.padding(top = Dimens.dp8),
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(Dimens.dp14),
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp14),
                ) {
                    Text(
                        stringResource(R.string.notification_light_probe_instructions),
                        modifier = Modifier.padding(Dimens.dp12),
                    )
                }
                Text(
                    stringResource(R.string.notification_light_probe_choose_color),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Dimens.dp18, bottom = Dimens.dp8),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                ) {
                    NotificationLightProbeColor.entries.forEach { color ->
                        OutlinedButton(
                            onClick = {
                                result = probe.post(color)
                                physicalResult = null
                            },
                        ) {
                            Spacer(
                                Modifier.size(Dimens.dp12).background(
                                    ComposeColor(color.argb),
                                    CircleShape,
                                ),
                            )
                            Text(color.label, Modifier.padding(start = Dimens.dp7))
                        }
                    }
                }

                result?.let { current ->
                    HorizontalDivider(Modifier.padding(vertical = Dimens.dp16))
                    Text(
                        stringResource(R.string.notification_light_probe_android_state),
                        fontWeight = FontWeight.Bold,
                    )
                    ProbeStateRow(
                        stringResource(R.string.notification_light_probe_notification_posted),
                        current.notificationPosted,
                    )
                    ProbeStateRow(
                        stringResource(R.string.notification_light_probe_channel_enabled),
                        current.channelAllowed,
                    )
                    ProbeStateRow(
                        stringResource(R.string.notification_light_probe_lights_requested),
                        current.lightsRequested,
                    )
                    Text(
                        stringResource(
                            R.string.notification_light_probe_registered_color,
                            current.configuredColor?.let(::colorHex)
                                ?: stringResource(R.string.notification_light_probe_unknown),
                        ),
                        color = Muted,
                        fontSize = Dimens.sp13,
                        modifier = Modifier.padding(top = Dimens.dp4),
                    )
                    Text(
                        stringResource(R.string.notification_light_probe_android_not_confirmation),
                        color = Muted,
                        fontSize = Dimens.sp13,
                        modifier = Modifier.padding(top = Dimens.dp10),
                    )
                    if (current.notificationPosted) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = Dimens.dp12),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                        ) {
                            Button(
                                onClick = { physicalResult = PhysicalLightResult.SEEN },
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.notification_light_probe_seen)) }
                            OutlinedButton(
                                onClick = { physicalResult = PhysicalLightResult.NOT_SEEN },
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.notification_light_probe_not_seen)) }
                        }
                    }
                    physicalResult?.let { observed ->
                        Text(
                            stringResource(
                                if (observed == PhysicalLightResult.SEEN) {
                                    R.string.notification_light_probe_seen_result
                                } else {
                                    R.string.notification_light_probe_not_seen_result
                                },
                            ),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = Dimens.dp10),
                        )
                    }
                    TextButton(
                        onClick = { context.startSafely(probe.channelSettingsIntent(current.color)) },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text(stringResource(R.string.notification_light_probe_channel_settings)) }
                }

                Spacer(Modifier.height(Dimens.dp8))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        probe.cancel()
                        result = null
                        physicalResult = null
                    }) { Text(stringResource(R.string.notification_light_probe_cancel_test)) }
                    TextButton(onClick = {
                        probe.cancel()
                        onDismiss()
                    }) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}

@Composable
private fun ProbeStateRow(label: String, enabled: Boolean) {
    Text(
        stringResource(
            R.string.notification_light_probe_state_line,
            label,
            stringResource(
                if (enabled) R.string.notification_light_probe_yes
                else R.string.notification_light_probe_no,
            ),
        ),
        color = Muted,
        fontSize = Dimens.sp13,
        modifier = Modifier.padding(top = Dimens.dp4),
    )
}

private fun colorHex(color: Int): String = String.format("#%06X", Color.rgb(Color.red(color), Color.green(color), Color.blue(color)) and 0xFFFFFF)

private fun Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent) }
}

private enum class PhysicalLightResult { SEEN, NOT_SEEN }
