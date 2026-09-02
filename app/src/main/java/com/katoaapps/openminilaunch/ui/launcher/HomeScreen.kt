@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.launcher

import com.katoaapps.openminilaunch.BuildConfig
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.updates.GitHubReleaseChecker
import com.katoaapps.openminilaunch.features.updates.isNewerRelease
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.magic.MagicBox
import com.katoaapps.openminilaunch.ui.settings.LockAccessibilityDisclosureDialog
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.updates.GitHubUpdateDialog
import com.katoaapps.openminilaunch.ui.wellbeing.MinkPausedAppDialog
import com.katoaapps.openminilaunch.ui.wellbeing.rememberMinkAppAccessState

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun HomeScreen(
    store: LauncherStore,
    actions: DeviceActions,
    openSettings: (SettingsDestination) -> Unit,
    openTodos: () -> Unit,
    openHub: () -> Unit,
    openMinkDay: () -> Unit,
    minkStatusActive: Boolean,
    onMagicExpandedChange: (Boolean) -> Unit,
    keyboardInputEnabled: Boolean,
    homeRequestToken: Int,
) {
    val context = LocalContext.current
    var drawerOpen by remember { mutableStateOf(false) }
    var todoJumpToken by remember { mutableIntStateOf(0) }
    var flyingTodo by remember { mutableStateOf<String?>(null) }
    var flightActive by remember { mutableStateOf(false) }
    var widgetCenter by remember { mutableStateOf(Offset.Zero) }
    var magicCenter by remember { mutableStateOf(Offset.Zero) }
    var magicExpanded by remember { mutableStateOf(false) }
    var showLockDisclosure by remember { mutableStateOf(false) }
    var showUpdateConfirmation by remember { mutableStateOf(false) }
    var pausedAppPackage by remember { mutableStateOf<String?>(null) }
    val appAccessState by rememberMinkAppAccessState(store, minkStatusActive)
    val releaseChecker = remember { GitHubReleaseChecker() }
    val updateAvailable = store.githubUpdateChecksEnabled &&
        store.latestGitHubReleaseTag?.let { isNewerRelease(BuildConfig.VERSION_NAME, it) } == true
    val flightProgress = remember { Animatable(0f) }
    val lockServiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (actions.isLockServiceEnabled()) actions.lockDevice()
    }

    fun lockFromHome() {
        if (!actions.supportsLockScreenAction()) {
            Toast.makeText(context, context.getString(R.string.double_tap_lock_requires_android_9), Toast.LENGTH_SHORT).show()
        } else if (!actions.lockDevice()) {
            showLockDisclosure = true
        }
    }

    LaunchedEffect(flyingTodo) {
        if (flyingTodo != null && widgetCenter != Offset.Zero && magicCenter != Offset.Zero) {
            flightProgress.snapTo(0f)
            flightActive = true
            flightProgress.animateTo(1f, tween(1_300, easing = FastOutSlowInEasing))
            flightActive = false
            flyingTodo = null
        }
    }

    LaunchedEffect(homeRequestToken, store.onboardingComplete, store.githubUpdateChecksEnabled) {
        if (store.onboardingComplete && store.shouldCheckGitHubRelease()) {
            store.markGitHubReleaseCheckStarted()
            withContext(Dispatchers.IO) { releaseChecker.latestReleaseTag() }
                ?.let(store::cacheLatestGitHubReleaseTag)
        }
    }

    LaunchedEffect(updateAvailable, store.latestGitHubReleaseTag, homeRequestToken) {
        val releaseTag = store.latestGitHubReleaseTag
        if (
            updateAvailable && releaseTag != null &&
            store.shouldShowGitHubUpdateReminder(releaseTag)
        ) {
            showUpdateConfirmation = true
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().pointerInput(Unit) {
            var distance = 0f
            detectVerticalDragGestures(
                onDragStart = { distance = 0f },
                onVerticalDrag = { _, amount -> if (amount > 0) distance += amount },
                onDragEnd = { if (distance > 140f) actions.expandNotificationShade() },
            )
        }.pointerInput(magicExpanded) {
            if (!magicExpanded) detectTapGestures(onDoubleTap = { lockFromHome() })
        },
    ) {
        val qwertyHome = maxHeight <= maxWidth * 1.55f
        val homeHorizontalPadding = if (qwertyHome) Dimens.dp14 else Dimens.dp22
        val headerActionSize = if (qwertyHome) Dimens.dp40 else Dimens.dp48
        val headerIconSize = if (qwertyHome) Dimens.dp21 else Dimens.dp24
        val focusPanelHeight = (maxWidth * .78f).coerceIn(Dimens.dp310, Dimens.dp350)
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val softInputRowHeight = if (magicExpanded) Dimens.dp0 else (imeBottom - navigationBottom).coerceAtLeast(Dimens.dp0)
        Column(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .blur(if (magicExpanded) Dimens.dp10 else Dimens.dp0),
        ) {
            HomeHeader(
                store = store,
                actions = actions,
                minkStatusActive = minkStatusActive,
                qwertyHome = qwertyHome,
                horizontalPadding = homeHorizontalPadding,
                actionSize = headerActionSize,
                iconSize = headerIconSize,
                updateAvailable = updateAvailable,
                onMinkDay = openMinkDay,
                onUpdate = { showUpdateConfirmation = true },
                onHub = openHub,
                openSettings = openSettings,
            )
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f)
                    .padding(horizontal = homeHorizontalPadding, vertical = if (qwertyHome) Dimens.dp2 else Dimens.dp10),
            ) {
                HomeFocusPanel(
                    store = store,
                    actions = actions,
                    appAccessState = appAccessState,
                    qwertyHome = qwertyHome,
                    availableHeight = maxHeight,
                    focusPanelHeight = focusPanelHeight,
                    todoJumpToken = todoJumpToken,
                    openTodos = openTodos,
                    onTodoCenterChanged = { widgetCenter = it },
                    onPausedApp = { pausedAppPackage = it },
                    onOpenDrawer = { drawerOpen = true },
                )
            }
            Spacer(
                Modifier.navigationBarsPadding()
                    .height(Dimens.dp88 + softInputRowHeight),
            )
        }
        flyingTodo?.takeIf { flightActive }?.let { text ->
            TodoFlightChip(
                text = text,
                progress = flightProgress,
                start = magicCenter,
                destination = widgetCenter,
            )
        }
        MagicBox(
            store = store,
            actions = actions,
            modifier = Modifier.fillMaxSize().zIndex(if (magicExpanded) 20f else 0f),
            collapsedModifier = Modifier.widthIn(max = Dimens.dp620).fillMaxWidth().navigationBarsPadding().imePadding()
                .padding(horizontal = Dimens.dp22, vertical = Dimens.dp12)
                .onGloballyPositioned { coordinates ->
                    val origin = coordinates.positionInRoot()
                    magicCenter = origin + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                },
            keyboardInputEnabled = keyboardInputEnabled,
            autoOpenSoftwareKeyboardOnHome = store.openSoftwareKeyboardOnHome,
            homeRequestToken = homeRequestToken,
            onTodoAdded = { text ->
                flyingTodo = text
                todoJumpToken++
            },
            onExpandedChange = { magicExpanded = it; onMagicExpandedChange(it) },
            appAccessState = appAccessState,
        )
    }

    if (showLockDisclosure) {
        LockAccessibilityDisclosureDialog(
            onContinue = {
                showLockDisclosure = false
                lockServiceLauncher.launch(actions.lockAccessibilitySettingsIntent())
            },
            onDismiss = { showLockDisclosure = false },
        )
    }

    if (showUpdateConfirmation) {
        GitHubUpdateDialog(
            currentVersion = BuildConfig.VERSION_NAME,
            availableVersion = store.latestGitHubReleaseTag.orEmpty().removePrefix("v"),
            onOpenBrowser = {
                if (actions.openLatestGitHubReleaseDownload()) {
                    store.latestGitHubReleaseTag?.let(store::snoozeGitHubUpdateReminder)
                    showUpdateConfirmation = false
                } else {
                    Toast.makeText(context, R.string.no_browser_available, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                store.latestGitHubReleaseTag?.let(store::snoozeGitHubUpdateReminder)
                showUpdateConfirmation = false
            },
        )
    }

    if (drawerOpen) {
        HomeDrawerSheet(
            store = store,
            actions = actions,
            appAccessState = appAccessState,
            onDismiss = { drawerOpen = false },
            openSettings = openSettings,
        )
    }

    pausedAppPackage?.let { packageName ->
        MinkPausedAppDialog(actions.appLabel(packageName)) { pausedAppPackage = null }
    }
}
