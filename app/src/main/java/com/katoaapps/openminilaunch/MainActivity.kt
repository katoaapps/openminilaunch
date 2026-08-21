@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetHost
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch

private const val FEATURE_UPDATE_ID = "open_1_1_0"
private const val REQUEST_CONFIGURE_APP_WIDGET = 0x4D4B
private const val MINK_DAY_PAGE = 0
private const val HOME_PAGE = 1
private const val WIDGET_PAGE = 2

internal fun isPermanentlyDenied(context: android.content.Context, permission: String): Boolean =
    context is Activity &&
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
        !ActivityCompat.shouldShowRequestPermissionRationale(context, permission)

internal fun mediaReadPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

internal fun hasMediaReadAccess(context: android.content.Context): Boolean = mediaReadPermissions().any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

internal fun mediaPermissionPermanentlyDenied(context: android.content.Context): Boolean =
    !hasMediaReadAccess(context) && mediaReadPermissions().all { isPermanentlyDenied(context, it) }

internal fun supportsDirectCalls(context: android.content.Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

internal fun supportsDirectSms(context: android.content.Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)

class MainActivity : ComponentActivity() {
    private val homeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private var homeRequestToken by mutableIntStateOf(0)
    private var widgetConfigurationResult: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val store = LauncherStore(this)
        val actions = DeviceActions(this)
        actions.removeLegacyLockAdmin()
        Thread({ actions.installedApps() }, "minilaunch-app-index").start()
        setContent { MiniLaunchApp(store, actions, ::requestHomeRole, homeRequestToken) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) homeRequestToken++
    }

    internal fun configureAppWidget(host: AppWidgetHost, appWidgetId: Int, onResult: (Boolean) -> Unit): Boolean {
        widgetConfigurationResult = onResult
        return runCatching {
            host.startAppWidgetConfigureActivityForResult(
                this,
                appWidgetId,
                0,
                REQUEST_CONFIGURE_APP_WIDGET,
                null,
            )
        }.onFailure {
            widgetConfigurationResult = null
        }.isSuccess
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_APP_WIDGET) {
            widgetConfigurationResult?.invoke(resultCode == Activity.RESULT_OK)
            widgetConfigurationResult = null
        }
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            if (manager?.isRoleAvailable(RoleManager.ROLE_HOME) == true && !manager.isRoleHeld(RoleManager.ROLE_HOME)) {
                homeRoleLauncher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            }
        } else {
            homeRoleLauncher.launch(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }
}

@Composable
private fun MiniLaunchApp(
    store: LauncherStore,
    actions: DeviceActions,
    requestHomeRole: () -> Unit,
    homeRequestToken: Int,
) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var showTutorial by rememberSaveable { mutableStateOf(!store.onboardingComplete) }
    var showUpdateNotice by rememberSaveable {
        mutableStateOf(store.onboardingComplete && !store.hasSeenUpdate(FEATURE_UPDATE_ID))
    }
    var showShortcutSetup by rememberSaveable { mutableStateOf(false) }
    var showNotificationAccessPrompt by rememberSaveable { mutableStateOf(false) }
    var showUsageAccessPrompt by rememberSaveable { mutableStateOf(false) }
    var tutorialRun by rememberSaveable { mutableIntStateOf(0) }
    var homeMagicExpanded by remember { mutableStateOf(false) }
    val launcherPagerState = rememberPagerState(initialPage = HOME_PAGE, pageCount = { WIDGET_PAGE + 1 })
    val launcherScope = rememberCoroutineScope()
    LaunchedEffect(homeRequestToken) {
        if (homeRequestToken > 0) {
            screen = Screen.HOME
            launcherPagerState.scrollToPage(HOME_PAGE)
        }
    }
    val context = LocalContext.current
    val usageInsightsRepository = remember(context) { UsageInsightsRepository(context.applicationContext) }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (store.themePreference) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val fallbackColors = if (darkTheme) {
        darkColorScheme(
            primary = DarkPrimary, onPrimary = DarkOnPrimary,
            background = DarkBackground, surface = DarkSurface,
            surfaceContainerLow = DarkSurfaceContainerLow, onSurface = DarkOnSurface, secondary = Rust,
        )
    } else {
        lightColorScheme(
            primary = LightInk, onPrimary = LightPaper, background = LightPaper,
            surface = LightPaper, surfaceContainerLow = MinkWhite, onSurface = LightInk, secondary = Rust,
        )
    }
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else fallbackColors
    val view = LocalView.current
    val finishOnboardingSetup = {
        if (store.hasConfirmedAllShortcutChoices()) requestHomeRole()
        else showShortcutSetup = true
    }
    val finishSpecialAccessSetup = {
        if (usageInsightsRepository.hasAccess()) finishOnboardingSetup()
        else showUsageAccessPrompt = true
    }
    val finishPermissionSetup = {
        if (NotificationHub.hasAccess(context)) finishSpecialAccessSetup()
        else showNotificationAccessPrompt = true
    }
    val onboardingNotificationAccess = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (NotificationHub.hasAccess(context)) NotificationHub.requestReconnect(context)
        finishSpecialAccessSetup()
    }
    val onboardingUsageAccess = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        finishOnboardingSetup()
    }
    val onboardingSmsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && isPermanentlyDenied(context, Manifest.permission.SEND_SMS)) actions.openAppSettings()
        finishPermissionSetup()
    }
    val continueAfterCallPermission = {
        val directSmsCanBeUsed = supportsDirectSms(context) &&
            actions.isAssistantRoleHeld() &&
            store.messageSendMode != MessageSendMode.MESSAGING_APP
        if (directSmsCanBeUsed && ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            onboardingSmsPermission.launch(Manifest.permission.SEND_SMS)
        } else {
            finishPermissionSetup()
        }
    }
    val onboardingCallPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && isPermanentlyDenied(context, Manifest.permission.CALL_PHONE)) actions.openAppSettings()
        continueAfterCallPermission()
    }
    val onboardingPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)) {
            actions.openAppSettings()
            continueAfterCallPermission()
        } else if (!supportsDirectCalls(context) || ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            continueAfterCallPermission()
        } else {
            onboardingCallPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }
    val transparent = MinkTransparent
    SideEffect {
        val window = (context as Activity).window
        window.statusBarColor = transparent.toArgb()
        window.navigationBarColor = transparent.toArgb()
        WindowInsetsControllerCompat(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
    ) {
        BackHandler(enabled = !showTutorial && screen != Screen.HOME) { screen = Screen.HOME }
        BackHandler(enabled = !showTutorial && screen == Screen.HOME && launcherPagerState.currentPage != HOME_PAGE) {
            launcherScope.launch { launcherPagerState.animateScrollToPage(HOME_PAGE) }
        }
        val imeVisible = WindowInsets.isImeVisible
        BackHandler(enabled = !showTutorial && screen == Screen.HOME && launcherPagerState.currentPage == HOME_PAGE && !imeVisible) { }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (showTutorial) {
                key(tutorialRun) {
                    OnboardingScreen(
                        store = store,
                        actions = actions,
                        onFinish = {
                            store.completeOnboarding()
                            store.markUpdateSeen(FEATURE_UPDATE_ID)
                            showTutorial = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                if (!supportsDirectCalls(context) || ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                    continueAfterCallPermission()
                                } else {
                                    onboardingCallPermission.launch(Manifest.permission.CALL_PHONE)
                                }
                            } else {
                                onboardingPermission.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                    )
                }
            } else when (screen) {
                Screen.HOME -> HorizontalPager(
                    state = launcherPagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !homeMagicExpanded,
                ) { page ->
                    if (page == MINK_DAY_PAGE) {
                        MinkDayScreen(
                            store = store,
                            isActive = launcherPagerState.currentPage == MINK_DAY_PAGE,
                            goHome = { launcherScope.launch { launcherPagerState.animateScrollToPage(HOME_PAGE) } },
                        )
                    } else if (page == HOME_PAGE) {
                        HomeScreen(
                            store = store,
                            actions = actions,
                            openSettings = { screen = Screen.SETTINGS },
                            openTodos = { screen = Screen.TODOS },
                            openHub = { screen = Screen.HUB },
                            openMinkDay = { launcherScope.launch { launcherPagerState.animateScrollToPage(MINK_DAY_PAGE) } },
                            minkStatusActive = launcherPagerState.currentPage == HOME_PAGE,
                            onMagicExpandedChange = { homeMagicExpanded = it },
                            keyboardInputEnabled = launcherPagerState.currentPage == HOME_PAGE && !showTutorial && !showUpdateNotice && !showShortcutSetup,
                        )
                    } else {
                        WidgetPage(
                            store = store,
                            actions = actions,
                            goHome = { launcherScope.launch { launcherPagerState.animateScrollToPage(HOME_PAGE) } },
                        )
                    }
                }
                Screen.SETTINGS -> SettingsScreen(
                    store,
                    actions,
                    requestHomeRole,
                    onRepeatTutorial = { tutorialRun++; showTutorial = true },
                ) { screen = Screen.HOME }
                Screen.TODOS -> TodosScreen(store, actions) { screen = Screen.HOME }
                Screen.HUB -> NotificationHubScreen(actions) { screen = Screen.HOME }
            }
        }
        if (showShortcutSetup && !showTutorial && !showUpdateNotice) {
            ShortcutSetupDialog(
                store = store,
                actions = actions,
                onFinish = {
                    showShortcutSetup = false
                    requestHomeRole()
                },
            )
        }
        if (showNotificationAccessPrompt && !showTutorial && !showUpdateNotice) {
            NotificationAccessDisclosureDialog(
                onContinue = {
                    showNotificationAccessPrompt = false
                    onboardingNotificationAccess.launch(NotificationHub.accessSettingsIntent())
                },
                onDismiss = {
                    showNotificationAccessPrompt = false
                    finishSpecialAccessSetup()
                },
            )
        }
        if (showUsageAccessPrompt && !showTutorial && !showUpdateNotice) {
            UsageAccessDisclosureDialog(
                onContinue = {
                    showUsageAccessPrompt = false
                    onboardingUsageAccess.launch(usageInsightsRepository.accessSettingsIntent())
                },
                onDismiss = {
                    showUsageAccessPrompt = false
                    finishOnboardingSetup()
                },
            )
        }
        if (showUpdateNotice && !showTutorial) {
            FeatureUpdateDialog(
                onOpenSettings = {
                    store.markUpdateSeen(FEATURE_UPDATE_ID)
                    showUpdateNotice = false
                    screen = Screen.SETTINGS
                },
                onReviewTutorial = {
                    store.markUpdateSeen(FEATURE_UPDATE_ID)
                    showUpdateNotice = false
                    tutorialRun++
                    showTutorial = true
                },
                onNotNow = {
                    store.markUpdateSeen(FEATURE_UPDATE_ID)
                    showUpdateNotice = false
                },
            )
        }
    }
}
