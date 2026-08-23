package com.katoaapps.openminilaunch.ui.wellbeing

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.wellbeing.*
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*
import com.katoaapps.openminilaunch.ui.components.SectionLabel

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun MinkDayScreen(store: LauncherStore, isActive: Boolean, goHome: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { UsageInsightsRepository(context.applicationContext) }
    var showSocialApps by remember { mutableStateOf(false) }
    var permissionReturnToken by remember { mutableIntStateOf(0) }
    val summary by rememberMinkDaySummary(store, repository, isActive, permissionReturnToken)
    val errorMessage = summary.errorMessage
    val usageSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { permissionReturnToken++ }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dimens.dp22, vertical = Dimens.dp12),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp14),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.mink_day_heading), letterSpacing = Dimens.sp1_6, fontSize = Dimens.sp12, fontWeight = FontWeight.Black, color = Rust)
                    Text(stringResource(R.string.tracked_apps_today), fontSize = Dimens.sp22, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = goHome) { Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.go_home)) }
            }
        }
        item { MinkHero(summary) }
        if (summary.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().height(Dimens.dp84), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        } else if (errorMessage != null) {
            item {
                MinkErrorCard(errorMessage) { permissionReturnToken++ }
            }
        } else if (!summary.accessGranted) {
            item {
                UsageAccessCard(
                    onEnable = {
                        val intent = repository.accessSettingsIntent()
                        runCatching { usageSettings.launch(intent) }.onFailure {
                            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                        }
                    },
                )
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp9)) {
                    MinkMetric(stringResource(R.string.metric_social), formatDuration(context, summary.socialMillis), Icons.Default.Schedule, Modifier.weight(1f))
                    MinkMetric(stringResource(R.string.metric_goal), socialGoalLabel(store.socialGoalMinutes), Icons.Default.Flag, Modifier.weight(1f))
                    MinkMetric(stringResource(R.string.metric_opens), summary.socialOpensToday.toString(), Icons.Default.TouchApp, Modifier.weight(1f))
                }
            }
            if (summary.topApps.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.tracked_app_trail)) }
                items(summary.topApps, key = { it.packageName }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp18))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = Dimens.dp14, vertical = Dimens.dp12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.packageName, actions = null, size = Dimens.dp38)
                        Column(Modifier.weight(1f).padding(start = Dimens.dp11)) {
                            Text(app.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.tracked), color = Muted, fontSize = Dimens.sp12)
                        }
                        Text(formatDuration(context, app.foregroundMillis), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.make_it_yours)) }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp20))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Dimens.dp15),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
            ) {
                Text(stringResource(R.string.daily_social_goal_title), fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp7)) {
                    SOCIAL_GOAL_OPTIONS.forEach { minutes ->
                        FilterChip(
                            selected = store.socialGoalMinutes == minutes,
                            onClick = { store.updateSocialGoalMinutes(minutes) },
                            label = { Text(socialGoalLabel(minutes)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Surface(
                    onClick = { showSocialApps = true },
                    shape = RoundedCornerShape(Dimens.dp15),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Row(Modifier.fillMaxWidth().padding(Dimens.dp12), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Apps, null, tint = Rust)
                        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp10)) {
                            Text(stringResource(R.string.apps_you_want_to_limit), fontWeight = FontWeight.SemiBold)
                            Text(
                                if (store.usesAutomaticSocialApps) stringResource(R.string.automatic_android_categories)
                                else stringResource(R.string.selected_count, store.socialPackages.size),
                                color = Muted,
                                fontSize = Dimens.sp12,
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = Dimens.dp4), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Lock, null, Modifier.size(Dimens.dp17), tint = Muted)
                Text(
                    stringResource(R.string.activity_local_description),
                    Modifier.padding(start = Dimens.dp8).weight(1f),
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
            }
        }
    }
    if (showSocialApps) {
        SocialAppsDialog(store, repository) {
            showSocialApps = false
            permissionReturnToken++
        }
    }
}
