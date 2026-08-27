@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.launcher

import com.katoaapps.openminilaunch.BuildConfig
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.conversations.*
import com.katoaapps.openminilaunch.features.magic.*
import com.katoaapps.openminilaunch.features.todos.*
import com.katoaapps.openminilaunch.features.updates.GitHubReleaseChecker
import com.katoaapps.openminilaunch.features.updates.isNewerRelease
import com.katoaapps.openminilaunch.features.wellbeing.*
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*
import com.katoaapps.openminilaunch.ui.magic.MagicBox
import com.katoaapps.openminilaunch.ui.settings.LockAccessibilityDisclosureDialog
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.wellbeing.MinkHomeIcon

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

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
    val releaseChecker = remember { GitHubReleaseChecker() }
    val updateAvailable = store.githubUpdateChecksEnabled &&
        store.latestGitHubReleaseTag?.let { isNewerRelease(BuildConfig.VERSION_NAME, it) } == true
    val flightProgress = remember { Animatable(0f) }
    val homePanelColor = Color(store.effectiveHomePanelColorArgb)
    val homePanelContentColor = readableContentColor(homePanelColor)
    val homePanelMutedColor = homePanelContentColor.copy(alpha = .68f)
    val homePanelInsetColor = if (homePanelContentColor == MinkWhite) {
        MinkBlack.copy(alpha = .16f)
    } else {
        MinkWhite.copy(alpha = .24f)
    }
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
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides headerActionSize) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = homeHorizontalPadding)
                        .padding(vertical = if (qwertyHome) Dimens.dp0 else Dimens.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MinkHomeIcon(
                        store = store,
                        isActive = minkStatusActive,
                        onClick = openMinkDay,
                        modifier = Modifier.size(headerActionSize),
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
                            LocalDate.now().format(DateTimeFormatter.ofPattern(stringResource(R.string.home_date_pattern))).uppercase(),
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
                        IconButton(
                            onClick = { showUpdateConfirmation = true },
                            modifier = Modifier.size(headerActionSize),
                        ) {
                            Icon(
                                Icons.Default.SystemUpdateAlt,
                                stringResource(R.string.update_available),
                                Modifier.size(headerIconSize),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = openHub, modifier = Modifier.size(headerActionSize)) {
                        BadgedBox(
                            badge = {
                                val count = NotificationHub.conversations().size
                                if (count > 0) Badge {
                                    Text(if (count > 99) stringResource(R.string.notification_count_overflow) else count.toString())
                                }
                            },
                        ) { Icon(Icons.Default.Forum, stringResource(R.string.conversations), Modifier.size(headerIconSize)) }
                    }
                    IconButton(onClick = { openSettings(SettingsDestination.OVERVIEW) }, modifier = Modifier.size(headerActionSize)) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings), Modifier.size(headerIconSize))
                    }
                }
            }
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f)
                    .padding(horizontal = homeHorizontalPadding, vertical = if (qwertyHome) Dimens.dp2 else Dimens.dp10),
            ) {
                val todoPanelHeight = if (qwertyHome) maxHeight else minOf(maxHeight, focusPanelHeight)
                val todoItemsPerPage = visibleTodoItemsForHeight(todoPanelHeight.value)
                val focusModifier = if (qwertyHome) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth().height(focusPanelHeight).align(Alignment.TopCenter)
                }
                Surface(
                    modifier = focusModifier.widthIn(max = Dimens.dp620).align(if (qwertyHome) Alignment.Center else Alignment.TopCenter),
                    shape = RoundedCornerShape(if (qwertyHome) Dimens.dp26 else Dimens.dp34),
                    color = homePanelColor,
                    contentColor = homePanelContentColor,
                    shadowElevation = if (isSystemInDarkTheme()) Dimens.dp2 else Dimens.dp8,
                    tonalElevation = Dimens.dp1,
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(if (qwertyHome) Dimens.dp10 else Dimens.dp14),
                        horizontalArrangement = Arrangement.spacedBy(if (qwertyHome) Dimens.dp8 else Dimens.dp12),
                    ) {
                        TodoPager(
                            store,
                            openTodos,
                            todoJumpToken,
                            itemsPerPage = todoItemsPerPage,
                            compact = qwertyHome,
                            embedded = true,
                            contentColor = homePanelContentColor,
                            mutedContentColor = homePanelMutedColor,
                            insetColor = homePanelInsetColor,
                            modifier = Modifier.weight(2f).fillMaxHeight().onGloballyPositioned { coordinates ->
                                val origin = coordinates.positionInRoot()
                                widgetCenter = origin + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                            },
                        )
                        ShortcutGrid(
                            store = store,
                            actions = actions,
                            openTodos = openTodos,
                            compact = qwertyHome,
                            contentColor = homePanelContentColor,
                            itemContainerColor = homePanelContentColor.copy(alpha = .09f),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { drawerOpen = true }
                    }
                }
            }
            Spacer(
                Modifier.navigationBarsPadding()
                    .height(Dimens.dp88 + softInputRowHeight),
            )
        }
        flyingTodo?.takeIf { flightActive }?.let { text ->
            val progress = flightProgress.value
            val position = Offset(
                x = magicCenter.x + (widgetCenter.x - magicCenter.x) * progress,
                y = magicCenter.y + (widgetCenter.y - magicCenter.y) * progress,
            )
            val alpha = if (progress < .72f) 1f else ((1f - progress) / .28f).coerceIn(0f, 1f)
            Surface(
                modifier = Modifier.offset {
                    IntOffset((position.x - 100).roundToInt(), (position.y - 26).roundToInt())
                }.graphicsLayer { this.alpha = alpha }
                    .zIndex(10f).shadow(Dimens.dp8, RoundedCornerShape(Dimens.dp18)),
                color = MagicTodoColor,
                shape = RoundedCornerShape(Dimens.dp18),
            ) {
                Row(Modifier.widthIn(max = Dimens.dp200).padding(horizontal = Dimens.dp14, vertical = Dimens.dp9), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Checklist, null, Modifier.size(Dimens.dp18), tint = LightInk)
                    Text(text, Modifier.padding(start = Dimens.dp7), maxLines = 1, color = LightInk, fontWeight = FontWeight.SemiBold)
                }
            }
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
            version = store.latestGitHubReleaseTag.orEmpty().removePrefix("v"),
            onOpenBrowser = {
                showUpdateConfirmation = false
                if (!actions.openLatestGitHubReleaseDownload()) {
                    Toast.makeText(context, R.string.no_browser_available, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showUpdateConfirmation = false },
        )
    }

    if (drawerOpen) {
        ModalBottomSheet(
            onDismissRequest = { drawerOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Text(stringResource(R.string.your_drawer), Modifier.padding(horizontal = Dimens.dp24), fontWeight = FontWeight.Black, letterSpacing = Dimens.sp1)
            if (store.drawerPackages.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = Dimens.dp28, vertical = Dimens.dp24),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.Apps, null, Modifier.size(Dimens.dp48), tint = Sage)
                    Text(
                        stringResource(R.string.empty_drawer),
                        fontSize = Dimens.sp22,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = Dimens.dp14),
                    )
                    Text(
                        stringResource(R.string.choose_drawer_apps),
                        color = Muted,
                        modifier = Modifier.padding(vertical = Dimens.dp10),
                    )
                    Button(
                        onClick = {
                            drawerOpen = false
                            openSettings(SettingsDestination.SHORTCUTS)
                        },
                    ) {
                        Icon(Icons.Default.Add, null)
                        Text(stringResource(R.string.choose_apps), Modifier.padding(start = Dimens.dp8))
                    }
                }
            } else {
                val drawerRows = ceil(store.drawerPackages.size / 2f).toInt()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height(Dimens.dp72 * drawerRows),
                    contentPadding = PaddingValues(horizontal = Dimens.dp12, vertical = Dimens.dp8),
                ) {
                    items(store.drawerPackages, key = { it }) { packageName ->
                        ListItem(
                            headlineContent = { Text(actions.appLabel(packageName), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { AppIcon(packageName, actions, Dimens.dp36) },
                            modifier = Modifier.clip(RoundedCornerShape(Dimens.dp16))
                                .clickable { actions.launchPackage(packageName); drawerOpen = false },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18, vertical = Dimens.dp4),
                horizontalArrangement = Arrangement.End,
            ) {
                FilledTonalButton(
                    onClick = {
                        drawerOpen = false
                        actions.openAllApps()
                    },
                ) {
                    Text(stringResource(R.string.see_all_apps))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        modifier = Modifier.padding(start = Dimens.dp6),
                    )
                }
            }
            Spacer(Modifier.height(Dimens.dp28))
        }
    }
}

@Composable
private fun GitHubUpdateDialog(
    version: String,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.SystemUpdateAlt, null) },
        title = { Text(stringResource(R.string.github_update_title, version)) },
        text = { Text(stringResource(R.string.github_update_description)) },
        confirmButton = {
            Button(onClick = onOpenBrowser) { Text(stringResource(R.string.open_download)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) }
        },
    )
}
