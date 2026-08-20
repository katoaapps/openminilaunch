package com.katoaapps.openminilaunch

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

@Composable
internal fun MinkHomeIcon(
    store: LauncherStore,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember { UsageInsightsRepository(context.applicationContext) }
    val summary by rememberMinkDaySummary(store, repository, isActive)
    val minkContentDescription = stringResource(
        if (summary.needsAttention()) R.string.mink_day_attention else R.string.mink_day,
    )
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = minkContentDescription
        },
    ) {
        BadgedBox(
            badge = {
                if (!summary.isLoading && summary.needsAttention()) {
                    Badge()
                }
            },
        ) {
            MinkSprite(summary.state, Modifier.size(Dimens.dp32))
        }
    }
}

@Composable
private fun rememberMinkDaySummary(
    store: LauncherStore,
    repository: UsageInsightsRepository,
    isActive: Boolean,
    externalRefreshToken: Int = 0,
): State<MinkDaySummary> {
    val context = LocalContext.current
    val socialPackages = store.socialPackages.toSet()
    val usesAutomaticSocialApps = store.usesAutomaticSocialApps
    var lifecycleRefreshToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(isActive) {
        if (isActive) {
            lifecycleRefreshToken++
            while (true) {
                delay(5 * 60_000L)
                lifecycleRefreshToken++
            }
        }
    }
    DisposableEffect(context, isActive) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isActive) lifecycleRefreshToken++
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    return produceState(
        initialValue = MinkDaySummary.loading(context),
        lifecycleRefreshToken,
        externalRefreshToken,
        store.socialGoalMinutes,
        socialPackages,
        usesAutomaticSocialApps,
    ) {
        if (isActive) {
            val socialGoalMinutes = store.socialGoalMinutes
            value = withContext(Dispatchers.IO) {
                runCatching { repository.summary(socialPackages, usesAutomaticSocialApps, socialGoalMinutes) }
                    .getOrElse { MinkDaySummary.unavailable(context, repository.hasAccess()) }
            }
        }
    }
}

@Composable
private fun MinkHero(summary: MinkDaySummary) {
    val transition = rememberInfiniteTransition(label = "mink-breathe")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (summary.state == MinkState.SLEEPING) 1.5f else -5f,
        animationSpec = infiniteRepeatable(tween(1_300), RepeatMode.Reverse),
        label = "mink-bob",
    )
    Surface(
        shape = RoundedCornerShape(Dimens.dp28),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Dimens.dp20), horizontalAlignment = Alignment.CenterHorizontally) {
            MinkSprite(summary.state, Modifier.size(Dimens.dp164).graphicsLayer { translationY = bob })
            Text(summary.headline, fontSize = Dimens.sp22, fontWeight = FontWeight.Black)
            Text(summary.detail, Modifier.padding(top = Dimens.dp7), color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = Dimens.sp14)
        }
    }
}

@Composable
private fun MinkMetric(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(Dimens.dp18)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Dimens.dp11),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp5),
    ) {
        Icon(icon, null, Modifier.size(Dimens.dp18), tint = Rust)
        Text(value, fontWeight = FontWeight.Black, fontSize = Dimens.sp18)
        Text(label, color = Muted, fontSize = Dimens.sp9, fontWeight = FontWeight.Bold, letterSpacing = Dimens.sp0_8)
    }
}

@Composable
private fun UsageAccessCard(onEnable: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp22))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Dimens.dp16),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = Rust)
            Text(stringResource(R.string.optional_usage_access), Modifier.padding(start = Dimens.dp10), fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.usage_access_explanation, stringResource(R.string.app_name)), color = Muted, fontSize = Dimens.sp13)
        Text(stringResource(R.string.usage_not_uploaded), fontSize = Dimens.sp13, fontWeight = FontWeight.SemiBold)
        Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_usage_access)) }
    }
}

@Composable
private fun MinkErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp22))
            .background(MaterialTheme.colorScheme.errorContainer).padding(Dimens.dp16),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
    ) {
        Text(stringResource(R.string.usage_data_unavailable), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = Dimens.sp13)
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, null)
            Text(stringResource(R.string.try_again), Modifier.padding(start = Dimens.dp8))
        }
    }
}

@Composable
private fun MinkSprite(state: MinkState, modifier: Modifier = Modifier) {
    val sheet = ImageBitmap.imageResource(R.drawable.mink_states)
    val index = when (state) {
        MinkState.WALKING -> 0
        MinkState.PURPOSEFUL -> 1
        MinkState.PHONE -> 2
        MinkState.DISTRACTED -> 3
        MinkState.RESTING -> 4
        MinkState.SLEEPING -> 5
    }
    Canvas(modifier.aspectRatio(1f)) {
        val cellWidth = sheet.width / 3
        val cellHeight = sheet.height / 2
        drawImage(
            image = sheet,
            srcOffset = IntOffset((index % 3) * cellWidth, (index / 3) * cellHeight),
            srcSize = IntSize(cellWidth, cellHeight),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
    }
}

@Composable
internal fun SocialAppsDialog(store: LauncherStore, repository: UsageInsightsRepository, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val apps by produceState<List<LaunchableApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { repository.launchableApps() }
    }
    val visible = remember(apps, query) { apps.orEmpty().filter { it.label.contains(query.trim(), ignoreCase = true) } }
    val automaticPackages by produceState<Set<String>>(initialValue = emptySet(), apps) {
        value = withContext(Dispatchers.IO) { repository.automaticSocialPackages(apps.orEmpty()) }
    }
    val selectedPackages = effectiveTrackedPackages(
        store.socialPackages.toSet(),
        automaticPackages,
        store.usesAutomaticSocialApps,
    )
    LaunchedEffect(apps) {
        apps?.takeIf { it.isNotEmpty() }
            ?.let { store.reconcileSocialApps(it.map(LaunchableApp::packageName).toSet()) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Apps, null, tint = Rust) },
        title = { Text(stringResource(R.string.choose_tracked_apps)) },
        text = {
            Column {
                Text(
                    if (store.usesAutomaticSocialApps) {
                        stringResource(
                            R.string.automatic_social_count,
                            automaticPackages.size,
                            stringResource(if (automaticPackages.size == 1) R.string.app_singular else R.string.app_plural),
                        )
                    } else if (store.socialPackages.isEmpty()) {
                        stringResource(R.string.no_tracked_apps)
                    } else {
                        pluralStringResource(
                            R.plurals.tracked_selected_count,
                            store.socialPackages.size,
                            store.socialPackages.size,
                        )
                    },
                    color = Muted,
                    fontSize = Dimens.sp13,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.find_app)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.dp10),
                )
                if (selectedPackages.isNotEmpty()) {
                    Text(
                        stringResource(R.string.tracked_tap_to_remove),
                        color = Muted,
                        fontSize = Dimens.sp10,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = Dimens.sp1,
                        modifier = Modifier.padding(bottom = Dimens.dp6),
                    )
                    LazyRow(
                        Modifier.fillMaxWidth().padding(bottom = Dimens.dp10),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
                    ) {
                        items(apps.orEmpty().filter { it.packageName in selectedPackages }, key = { it.packageName }) { app ->
                            Column(
                                Modifier.width(Dimens.dp70).clip(RoundedCornerShape(Dimens.dp12))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .clickable {
                                        store.replaceSocialApps(selectedPackages - app.packageName)
                                    }.padding(horizontal = Dimens.dp4, vertical = Dimens.dp7),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box {
                                    AppIcon(app.packageName, actions = null, size = Dimens.dp31)
                                    Surface(
                                        modifier = Modifier.align(Alignment.TopEnd).offset(x = Dimens.dp5, y = -Dimens.dp5),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error,
                                    ) {
                                        Icon(Icons.Default.Close, stringResource(R.string.remove_app, app.label), Modifier.size(Dimens.dp14), tint = MaterialTheme.colorScheme.onError)
                                    }
                                }
                                Text(
                                    app.label,
                                    fontSize = Dimens.sp9,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp4),
                                )
                            }
                        }
                    }
                }
                when {
                    apps == null -> Box(Modifier.fillMaxWidth().height(Dimens.dp180), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                    visible.isEmpty() -> Box(Modifier.fillMaxWidth().height(Dimens.dp140), contentAlignment = Alignment.Center) {
                        Text(stringResource(if (query.isBlank()) R.string.no_launchable_apps else R.string.no_matching_apps), color = Muted)
                    }
                    else -> LazyColumn(Modifier.heightIn(min = Dimens.dp140, max = Dimens.dp330)) {
                        items(visible, key = { it.packageName }) { app ->
                            val selected = app.packageName in selectedPackages
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                store.replaceSocialApps(
                                    if (selected) selectedPackages - app.packageName else selectedPackages + app.packageName,
                                )
                            }.padding(vertical = Dimens.dp7),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(app.packageName, actions = null, size = Dimens.dp34)
                            Column(Modifier.weight(1f).padding(horizontal = Dimens.dp10)) {
                                Text(app.label, maxLines = 1)
                                if (app.packageName in automaticPackages) {
                                    Text(stringResource(R.string.android_default_social), color = Rust, fontSize = Dimens.sp10)
                                }
                            }
                            Checkbox(
                                checked = selected,
                                onCheckedChange = {
                                    store.replaceSocialApps(
                                        if (it) selectedPackages + app.packageName else selectedPackages - app.packageName,
                                    )
                                },
                            )
                        }
                    }
                        }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        dismissButton = {
            TextButton(onClick = store::clearSocialApps) { Text(stringResource(R.string.restore_android_defaults)) }
        },
    )
}
