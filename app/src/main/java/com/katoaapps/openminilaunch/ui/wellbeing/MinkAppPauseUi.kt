package com.katoaapps.openminilaunch.ui.wellbeing

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.features.wellbeing.UsageInsightsRepository
import com.katoaapps.openminilaunch.model.MAX_SOCIAL_GOAL_HOURS
import com.katoaapps.openminilaunch.model.MIN_SOCIAL_GOAL_HOURS
import com.katoaapps.openminilaunch.model.MinkAppPauseMode
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.AppBlockedRed
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun rememberMinkAppAccessState(
    store: LauncherStore,
    isActive: Boolean = true,
): State<MinkAppAccessState> {
    val context = LocalContext.current
    val repository = remember { UsageInsightsRepository(context.applicationContext) }
    val selectedPackages = store.socialPackages.toSet()
    val automatic = store.usesAutomaticSocialApps
    val mode = store.minkAppPauseMode
    val dailyLimitMinutes = store.socialGoalMinutes
    var refreshToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(isActive, mode) {
        if (isActive) {
            refreshToken++
            while (true) {
                delay(60_000L)
                refreshToken++
            }
        }
    }
    DisposableEffect(context, isActive) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isActive) refreshToken++
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    return produceState(
        initialValue = MinkAppAccessState(
            mode = mode,
            trackedPackages = if (automatic) emptySet() else selectedPackages,
            collectiveUsageMillis = 0,
            dailyLimitMinutes = dailyLimitMinutes,
            usageAccessGranted = false,
            isResolved = mode == MinkAppPauseMode.NEVER ||
                mode == MinkAppPauseMode.ALWAYS && !automatic,
        ),
        refreshToken,
        selectedPackages,
        automatic,
        mode,
        dailyLimitMinutes,
    ) {
        if (!isActive) return@produceState
        value = withContext(Dispatchers.IO) {
            val trackedPackages = if (automatic) {
                repository.automaticSocialPackages(repository.launchableApps())
            } else {
                selectedPackages
            }
            val accessGranted = repository.hasAccess()
            val usageMillis = if (mode == MinkAppPauseMode.AFTER_DAILY_LIMIT && accessGranted) {
                repository.summary(selectedPackages, automatic, dailyLimitMinutes).socialMillis
            } else {
                0L
            }
            MinkAppAccessState(
                mode = mode,
                trackedPackages = trackedPackages,
                collectiveUsageMillis = usageMillis,
                dailyLimitMinutes = dailyLimitMinutes,
                usageAccessGranted = accessGranted,
                isResolved = true,
            )
        }
    }
}

@Composable
internal fun MinkDailyLimitControl(store: LauncherStore) {
    var selectedHours by remember(store.socialGoalHours) { mutableIntStateOf(store.socialGoalHours) }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp5)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.daily_collective_limit), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.daily_limit_hours, selectedHours, selectedHours),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
            )
        }
        Slider(
            value = selectedHours.toFloat(),
            onValueChange = {
                selectedHours = it.roundToInt().coerceIn(MIN_SOCIAL_GOAL_HOURS, MAX_SOCIAL_GOAL_HOURS)
            },
            onValueChangeFinished = { store.updateSocialGoalHours(selectedHours) },
            valueRange = MIN_SOCIAL_GOAL_HOURS.toFloat()..MAX_SOCIAL_GOAL_HOURS.toFloat(),
            steps = MAX_SOCIAL_GOAL_HOURS - MIN_SOCIAL_GOAL_HOURS - 1,
        )
        Text(
            stringResource(R.string.daily_collective_limit_description),
            color = Muted,
            fontSize = Dimens.sp12,
        )
    }
}

@Composable
internal fun MinkPauseModeControl(store: LauncherStore) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp5)) {
        Text(stringResource(R.string.pause_selected_apps), fontWeight = FontWeight.Bold)
        MinkAppPauseMode.entries.forEach { mode ->
            Surface(
                onClick = { store.updateMinkAppPauseMode(mode) },
                shape = RoundedCornerShape(Dimens.dp14),
                color = if (store.minkAppPauseMode == mode) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.background
                },
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Dimens.dp8, vertical = Dimens.dp6),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = store.minkAppPauseMode == mode,
                        onClick = { store.updateMinkAppPauseMode(mode) },
                    )
                    Column(Modifier.padding(start = Dimens.dp4)) {
                        Text(stringResource(mode.labelRes), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(mode.descriptionRes), color = Muted, fontSize = Dimens.sp11)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MinkPausedBadge(modifier: Modifier = Modifier) {
    Icon(
        Icons.Default.Block,
        stringResource(R.string.app_paused),
        modifier.size(Dimens.dp36),
        tint = AppBlockedRed,
    )
}

@Composable
internal fun MinkPausedAppDialog(appLabel: String, onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.Block, null) },
        title = { Text(stringResource(R.string.app_paused_for_mink_day)) },
        text = { Text(stringResource(R.string.app_paused_explanation, appLabel)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.got_it)) }
        },
    )
}
