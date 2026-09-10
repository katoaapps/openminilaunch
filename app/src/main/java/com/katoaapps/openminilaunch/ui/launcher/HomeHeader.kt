package com.katoaapps.openminilaunch.ui.launcher

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.NotificationHub
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.wellbeing.MinkHomeIcon

@Composable
internal fun HomeHeader(
    store: LauncherStore,
    actions: DeviceActions,
    minkStatusActive: Boolean,
    qwertyHome: Boolean,
    horizontalPadding: Dp,
    actionSize: Dp,
    iconSize: Dp,
    updateAvailable: Boolean,
    onMinkDay: () -> Unit,
    onUpdate: () -> Unit,
    onHub: () -> Unit,
    openSettings: (SettingsDestination) -> Unit,
) {
    val context = LocalContext.current
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides actionSize) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = horizontalPadding)
                .padding(vertical = if (qwertyHome) Dimens.dp0 else Dimens.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MinkHomeIcon(
                store = store,
                isActive = minkStatusActive,
                onClick = onMinkDay,
                modifier = Modifier.size(actionSize),
            )
            Column(
                modifier = Modifier.weight(1f).clickable {
                    if (actions.openClock()) {
                        store.markClockOpenedFromDate()
                    } else {
                        Toast.makeText(context, R.string.no_clock_app_found, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Text(
                    rememberHomeDateTimeText(
                        datePattern = stringResource(R.string.home_date_pattern),
                        showClock = store.showClock,
                        use24HourClock = store.use24HourClock,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = Dimens.sp1_5,
                    fontSize = Dimens.sp13,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (!store.hasOpenedClockFromDate) {
                    Text(
                        stringResource(R.string.tap_for_clock),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .58f),
                        fontSize = Dimens.sp9,
                        maxLines = 1,
                    )
                }
            }
            if (updateAvailable) {
                IconButton(onClick = onUpdate, modifier = Modifier.size(actionSize)) {
                    Icon(
                        Icons.Default.SystemUpdateAlt,
                        stringResource(R.string.update_available),
                        Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onHub, modifier = Modifier.size(actionSize)) {
                BadgedBox(
                    badge = {
                        val count = NotificationHub.conversations().size
                        if (count > 0) {
                            Badge {
                                Text(
                                    if (count > 99) {
                                        stringResource(R.string.notification_count_overflow)
                                    } else {
                                        count.toString()
                                    },
                                )
                            }
                        }
                    },
                ) {
                    Icon(Icons.Default.Forum, stringResource(R.string.conversations), Modifier.size(iconSize))
                }
            }
            IconButton(
                onClick = { openSettings(SettingsDestination.OVERVIEW) },
                modifier = Modifier.size(actionSize),
            ) {
                Icon(Icons.Default.Settings, stringResource(R.string.settings), Modifier.size(iconSize))
            }
        }
    }
}
