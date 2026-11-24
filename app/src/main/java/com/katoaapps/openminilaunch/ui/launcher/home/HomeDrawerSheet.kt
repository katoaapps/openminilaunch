@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katoaapps.openminilaunch.ui.launcher.home

import com.katoaapps.minkpad.core.R as MinkPadR

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkpad.MinkPadLauncherEntry
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.quickdrawer.QuickAppsFolderDialog
import com.katoaapps.openminilaunch.ui.quickdrawer.GamesFolderDialog
import com.katoaapps.openminilaunch.ui.quickdrawer.QUICK_DRAWER_FEATURE_TOUR_ID
import com.katoaapps.openminilaunch.ui.quickdrawer.QuickDrawerDestination
import com.katoaapps.openminilaunch.ui.quickdrawer.QuickDrawerFeatureTour
import com.katoaapps.openminilaunch.ui.quickdrawer.QuickDrawerTourTarget
import com.katoaapps.openminilaunch.ui.quickdrawer.QuickDrawerTourTargetRegistry
import com.katoaapps.openminilaunch.ui.quickdrawer.quickDrawerTourSteps
import com.katoaapps.openminilaunch.ui.quickdrawer.quickDrawerTourTarget
import com.katoaapps.openminilaunch.ui.quickdrawer.quickDrawerDestinations
import com.katoaapps.openminilaunch.ui.quickdrawer.rememberQuickDrawerTourTargetRegistry
import com.katoaapps.openminilaunch.ui.quickdrawer.shouldStartQuickDrawerFeatureTour
import com.katoaapps.openminilaunch.ui.quickdrawer.tourTarget
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkBrown
import com.katoaapps.openminilaunch.ui.theme.readableContentColor

@Composable
internal fun HomeDrawerSheet(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    onDismiss: () -> Unit,
    onManageQuickApps: () -> Unit,
    onPausedApp: (String) -> Unit,
    tourRequestToken: Long = 0L,
    onTourComplete: () -> Unit = {},
) {
    val drawerColor = store.quickDrawerColorArgb?.let(::Color) ?: MaterialTheme.colorScheme.background
    val drawerContentColor = readableContentColor(drawerColor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = drawerColor,
        contentColor = drawerContentColor,
    ) {
        QuickDrawerSurface(
            store = store,
            actions = actions,
            appAccessState = appAccessState,
            contentColor = drawerContentColor,
            onHostDismiss = onDismiss,
            onManageQuickApps = onManageQuickApps,
            onPausedApp = onPausedApp,
            tourRequestToken = tourRequestToken,
            onTourComplete = onTourComplete,
        )
    }
}

/** Shared Quick Drawer body used by the sheet and the dedicated Classic page. */
@Composable
internal fun QuickDrawerSurface(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    contentColor: Color,
    onHostDismiss: () -> Unit,
    onManageQuickApps: () -> Unit,
    onPausedApp: (String) -> Unit,
    modifier: Modifier = Modifier,
    tourRequestToken: Long = 0L,
    onTourComplete: () -> Unit = {},
) {
    val context = LocalContext.current
    var quickAppsOpen by remember { mutableStateOf(false) }
    var gamesOpen by remember { mutableStateOf(false) }
    val gamesCount = remember(actions) { actions.sableGameTargets().size }
    val demoModeEnabled = store.demoModeEnabled
    val showDemoPreview = {
        Toast.makeText(context, R.string.demo_preview_action, Toast.LENGTH_SHORT).show()
    }
    val destinations = quickDrawerDestinations(
        showMinkPad = store.showMinkPadInQuickDrawer,
        showPrivateSpace = store.showPrivateSpaceInQuickDrawer,
        privateSpaceAvailable = store.demoModeEnabled ||
            actions.privateContainerEntryPointAvailable(store.privateContainerGatewaySelectionKey),
        showWorkSpace = store.showWorkProfileInQuickDrawer,
        workSpaceAvailable = store.demoModeEnabled || actions.workProfileAvailable(),
    )
    val tourRegistry = rememberQuickDrawerTourTargetRegistry()
    val tourSteps = remember(destinations) { quickDrawerTourSteps(destinations) }
    var tourVisible by rememberSaveable {
        mutableStateOf(
            shouldStartQuickDrawerFeatureTour(
                onboardingComplete = store.onboardingComplete,
                currentTourSeen = store.hasSeenUpdate(QUICK_DRAWER_FEATURE_TOUR_ID),
                replayRequested = tourRequestToken > 0L,
            ),
        )
    }
    LaunchedEffect(tourRequestToken) {
        if (tourRequestToken > 0L) tourVisible = true
    }

    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.your_drawer),
                Modifier.padding(horizontal = Dimens.dp24),
                fontWeight = FontWeight.Black,
                letterSpacing = Dimens.sp1,
            )
            QuickDrawerAnnouncementRow(
                title = stringResource(R.string.join_google_play_beta),
                summary = stringResource(R.string.google_play_beta_summary),
                modifier = Modifier.quickDrawerTourTarget(
                    tourRegistry,
                    QuickDrawerTourTarget.ANNOUNCEMENT,
                ),
                onClick = {
                    if (demoModeEnabled) {
                        showDemoPreview()
                    } else if (actions.openGooglePlayBetaPage()) {
                        onHostDismiss()
                    } else {
                        Toast.makeText(context, R.string.no_browser_available, Toast.LENGTH_SHORT).show()
                    }
                },
            )
            QuickDrawerCollectionsRow(
                quickAppsCount = if (demoModeEnabled) 4 else store.drawerTargets.size,
                gamesCount = gamesCount,
                contentColor = contentColor,
                tourRegistry = tourRegistry,
                onQuickApps = { quickAppsOpen = true },
                onGames = {
                    if (demoModeEnabled) showDemoPreview() else gamesOpen = true
                },
            )
            QuickDrawerNavigationGrid(
                destinations = destinations,
                contentColor = contentColor,
                tourRegistry = tourRegistry,
                onDestination = { destination ->
                    if (!demoModeEnabled) {
                        onHostDismiss()
                        when (destination) {
                            QuickDrawerDestination.MINKPAD_LITE -> MinkPadLauncherEntry.open(context)
                            QuickDrawerDestination.ALL_APPS -> actions.openAllApps()
                            QuickDrawerDestination.PRIVATE_SPACE -> actions.openPrivateApps()
                            QuickDrawerDestination.WORK_SPACE -> actions.openWorkApps()
                        }
                    } else {
                        showDemoPreview()
                    }
                },
            )
            Spacer(Modifier.height(Dimens.dp28))
        }
        if (tourVisible) {
            QuickDrawerFeatureTour(
                steps = tourSteps,
                registry = tourRegistry,
                // A match-parent child overlays the measured sheet without making the modal
                // request the full screen. Classic already provides a full-page parent.
                modifier = Modifier.matchParentSize(),
                onComplete = {
                    store.markUpdateSeen(QUICK_DRAWER_FEATURE_TOUR_ID)
                    tourVisible = false
                    onTourComplete()
                },
            )
        }
    }

    if (quickAppsOpen) {
        QuickAppsFolderDialog(
            store = store,
            actions = actions,
            appAccessState = appAccessState,
            onEdit = {
                quickAppsOpen = false
                onHostDismiss()
                onManageQuickApps()
            },
            onLaunch = {
                quickAppsOpen = false
                onHostDismiss()
            },
            onPausedApp = { packageName ->
                quickAppsOpen = false
                onHostDismiss()
                onPausedApp(packageName)
            },
            onDismiss = { quickAppsOpen = false },
        )
    }

    if (gamesOpen) {
        GamesFolderDialog(
            store = store,
            actions = actions,
            onLaunch = {
                gamesOpen = false
                onHostDismiss()
            },
            onDismiss = { gamesOpen = false },
        )
    }
}

@Composable
private fun QuickDrawerAnnouncementRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = readableContentColor(MinkBrown)
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.dp18, vertical = Dimens.dp12),
        shape = RoundedCornerShape(Dimens.dp18),
        color = MinkBrown,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.dp16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    summary,
                    color = contentColor.copy(alpha = .78f),
                    fontSize = Dimens.sp12,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
    }
}

@Composable
private fun QuickDrawerCollectionsRow(
    quickAppsCount: Int,
    gamesCount: Int,
    contentColor: Color,
    tourRegistry: QuickDrawerTourTargetRegistry,
    onQuickApps: () -> Unit,
    onGames: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = Dimens.dp18, vertical = Dimens.dp12),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        QuickDrawerCollectionButton(
            title = stringResource(R.string.quick_apps),
            count = stringResource(R.string.count_of_max, quickAppsCount, MAX_DRAWER_APPS),
            contentColor = contentColor,
            icon = { Icon(Icons.Default.GridView, null) },
            onClick = onQuickApps,
            modifier = Modifier.weight(1f).quickDrawerTourTarget(
                tourRegistry,
                QuickDrawerTourTarget.QUICK_APPS,
            ),
        )
        QuickDrawerCollectionButton(
            title = stringResource(R.string.games_beta),
            count = gamesCount.toString(),
            contentColor = contentColor,
            icon = { Icon(Icons.Default.SportsEsports, null) },
            onClick = onGames,
            modifier = Modifier.weight(1f).quickDrawerTourTarget(
                tourRegistry,
                QuickDrawerTourTarget.GAMES,
            ),
        )
    }
}

@Composable
private fun QuickDrawerCollectionButton(
    title: String,
    count: String,
    contentColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(Dimens.dp84),
        shape = RoundedCornerShape(Dimens.dp18),
        color = contentColor.copy(alpha = .09f),
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            icon()
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    count,
                    color = contentColor.copy(alpha = .68f),
                    fontSize = Dimens.sp12,
                    fontWeight = FontWeight.Bold,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(Dimens.dp18))
        }
    }
}

@Composable
private fun QuickDrawerNavigationGrid(
    destinations: List<QuickDrawerDestination>,
    contentColor: Color,
    tourRegistry: QuickDrawerTourTargetRegistry,
    onDestination: (QuickDrawerDestination) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        destinations.chunked(2).forEach { rowDestinations ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
            ) {
                rowDestinations.forEach { destination ->
                    QuickDrawerNavigationButton(
                        destination = destination,
                        contentColor = contentColor,
                        onClick = { onDestination(destination) },
                        modifier = Modifier.weight(1f).let { base ->
                            destination.tourTarget()?.let { target ->
                                base.quickDrawerTourTarget(tourRegistry, target)
                            } ?: base
                        },
                    )
                }
                if (rowDestinations.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickDrawerNavigationButton(
    destination: QuickDrawerDestination,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(Dimens.dp84),
        shape = RoundedCornerShape(Dimens.dp18),
        color = contentColor.copy(alpha = .09f),
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.dp12),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (destination) {
                QuickDrawerDestination.MINKPAD_LITE -> MinkPadDrawerIcon()
                QuickDrawerDestination.ALL_APPS -> Icon(Icons.Default.Apps, null)
                QuickDrawerDestination.PRIVATE_SPACE -> Icon(Icons.Default.Shield, null)
                QuickDrawerDestination.WORK_SPACE -> Icon(Icons.Default.BusinessCenter, null)
            }
            Spacer(Modifier.height(Dimens.dp6))
            Text(
                text = when (destination) {
                    QuickDrawerDestination.MINKPAD_LITE -> stringResource(R.string.minkpad_lite)
                    QuickDrawerDestination.ALL_APPS -> stringResource(R.string.all_apps)
                    QuickDrawerDestination.PRIVATE_SPACE -> stringResource(R.string.private_space)
                    QuickDrawerDestination.WORK_SPACE -> stringResource(R.string.work_space)
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MinkPadDrawerIcon() {
    Box(Modifier.size(Dimens.dp28)) {
        Icon(
            painterResource(R.drawable.ic_minkpad_notification),
            stringResource(MinkPadR.string.minkpad_name),
            modifier = Modifier.size(Dimens.dp24).align(Alignment.TopStart),
        )
        Box(
            Modifier
                .size(Dimens.dp14)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = 1.55f, scaleY = 1.55f),
            )
        }
    }
}
