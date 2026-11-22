package com.katoaapps.openminilaunch.ui.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetHost
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.katoaapps.openminilaunch.todoFeatureRuntime
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.Screen
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.applyLargeDisplayOrientation
import com.katoaapps.openminilaunch.ui.magic.MagicHardwareHotkeyHost
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.widgets.InteractiveAppWidgetHost
import com.katoaapps.openminilaunch.ui.widgets.MINK_WIDGET_HOST_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REQUEST_CONFIGURE_APP_WIDGET = 0x4D4B
private const val STATE_CONFIGURING_WIDGET_ID = "configuring_widget_id"
private const val STATE_WIDGET_RESULT_ID = "widget_result_id"
private const val STATE_WIDGET_RESULT_OK = "widget_result_ok"

open class MainActivity : FragmentActivity(), MagicHardwareHotkeyHost {
    protected open val isWorkspaceHost: Boolean = false
    private val homeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private var homeRequestToken by mutableIntStateOf(0)
    private var homeRequest by mutableStateOf<MinkHomeRequest?>(null)
    private var workspaceRequest by mutableStateOf<MinkWorkspaceRequest?>(null)
    private var configuringWidgetId: Int? = null
    internal var widgetConfigurationResult by mutableStateOf<Pair<Int, Boolean>?>(null)
        private set
    private lateinit var actions: DeviceActions
    private var magicHardwareHotkeyHandler: ((KeyEvent) -> Boolean)? = null
    internal val appWidgetHost by lazy {
        InteractiveAppWidgetHost(this, MINK_WIDGET_HOST_ID)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (magicHardwareHotkeyHandler?.invoke(event) == true) return true
        return super.dispatchKeyEvent(event)
    }

    override fun registerMagicHardwareHotkeyHandler(
        handler: (KeyEvent) -> Boolean,
    ): () -> Unit {
        magicHardwareHotkeyHandler = handler
        return {
            if (magicHardwareHotkeyHandler === handler) magicHardwareHotkeyHandler = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeRequest = MinkHomeRequestIntent.parse(intent)
        workspaceRequest = MinkWorkspaceActivity.request(intent)
        handleLauncherIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        allowContentBesideDisplayCutouts()
        val store = LauncherStore.get(this)
        store.maybeEnableTwoPanelForDisplay(this)
        applyLargeDisplayOrientation(this, store.twoPanelModeForLargeDisplays)
        configuringWidgetId = savedInstanceState?.takeIf { it.containsKey(STATE_CONFIGURING_WIDGET_ID) }
            ?.getInt(STATE_CONFIGURING_WIDGET_ID)
        widgetConfigurationResult = savedInstanceState?.takeIf { it.containsKey(STATE_WIDGET_RESULT_ID) }
            ?.let { it.getInt(STATE_WIDGET_RESULT_ID) to it.getBoolean(STATE_WIDGET_RESULT_OK) }
        actions = DeviceActions(this)
        store.initializeFreshQuickApps(actions.defaultQuickAppSelectionKeys())
        actions.removeLegacyLockAdmin()
        lifecycleScope.launch {
            val freshHomeFolderSeeds = withContext(Dispatchers.IO) {
                actions.freshHomeFolderSeeds()
            }
            store.initializeFreshHomeFolders(freshHomeFolderSeeds)
            store.migrateLauncherSelections(actions::normalizedLauncherSelectionKey)
        }
        setContent {
            val configuration = LocalConfiguration.current
            LaunchedEffect(store.twoPanelModeForLargeDisplays, configuration) {
                store.maybeEnableTwoPanelForDisplay(this@MainActivity)
                applyLargeDisplayOrientation(this@MainActivity, store.twoPanelModeForLargeDisplays)
            }
            MiniLaunchApp(
                store = store,
                actions = actions,
                requestHomeRole = ::requestHomeRole,
                homeRequestToken = homeRequestToken,
                homeRequest = homeRequest,
                isWorkspaceHost = isWorkspaceHost,
                workspaceRequest = workspaceRequest,
                openWorkspace = ::openWorkspace,
                openHome = ::openHome,
                closeWorkspace = ::finish,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun allowContentBesideDisplayCutouts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        window.attributes = window.attributes.apply {
            // Home handles the cutout's real bounds; do not letterbox its entire top edge.
            layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    override fun onStart() {
        super.onStart()
        todoFeatureRuntime.onForegrounded()
        if (!isWorkspaceHost) runCatching { appWidgetHost.startListening() }
        actions.refreshDynamicLauncherIcons()
    }

    override fun onStop() {
        if (!isWorkspaceHost) runCatching { appWidgetHost.stopListening() }
        if (!isChangingConfigurations) todoFeatureRuntime.onBackgrounded()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!isWorkspaceHost) {
            MinkHomeRequestIntent.parse(intent)?.let { homeRequest = it }
        }
        if (isWorkspaceHost) {
            MinkWorkspaceActivity.request(intent)?.let { workspaceRequest = it }
        }
        handleLauncherIntent(intent)
        // Home requests reuse the launcher activity. This token resets transient Compose pages.
        if (!isWorkspaceHost && intent.action == Intent.ACTION_MAIN) homeRequestToken++
    }

    private fun handleLauncherIntent(intent: Intent?) {
        todoFeatureRuntime.handleNavigationIntent(intent)
        if (intent?.action == ACTION_OPEN_TODOS) {
            todoFeatureRuntime.requestHomeBoardNavigation()
            // This handoff is a one-shot command, not durable Activity state. Consuming it keeps
            // configuration changes or process restoration from reopening To-dos unexpectedly.
            intent.action = null
        }
    }

    internal fun configureAppWidget(
        host: AppWidgetHost,
        appWidgetId: Int,
    ): Boolean {
        configuringWidgetId = appWidgetId
        widgetConfigurationResult = null
        return runCatching {
            host.startAppWidgetConfigureActivityForResult(
                this,
                appWidgetId,
                0,
                REQUEST_CONFIGURE_APP_WIDGET,
                null,
            )
        }.onFailure {
            configuringWidgetId = null
        }.isSuccess
    }

    internal fun consumeWidgetConfigurationResult(appWidgetId: Int): Boolean? {
        val (resultId, configured) = widgetConfigurationResult ?: return null
        if (resultId != appWidgetId) return null
        widgetConfigurationResult = null
        return configured
    }

    override fun onSaveInstanceState(outState: Bundle) {
        configuringWidgetId?.let { outState.putInt(STATE_CONFIGURING_WIDGET_ID, it) }
        widgetConfigurationResult?.let { (id, configured) ->
            outState.putInt(STATE_WIDGET_RESULT_ID, id)
            outState.putBoolean(STATE_WIDGET_RESULT_OK, configured)
        }
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_APP_WIDGET) {
            configuringWidgetId?.let { widgetConfigurationResult = it to (resultCode == Activity.RESULT_OK) }
            configuringWidgetId = null
        }
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            if (manager?.isRoleAvailable(RoleManager.ROLE_HOME) == true &&
                !manager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                homeRoleLauncher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            }
        } else {
            homeRoleLauncher.launch(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    private fun openWorkspace(
        screen: Screen,
        settingsDestination: SettingsDestination,
    ) {
        if (isWorkspaceHost) {
            workspaceRequest = MinkWorkspaceRequest(
                token = android.os.SystemClock.elapsedRealtimeNanos(),
                screen = screen,
                settingsDestination = settingsDestination,
            )
        } else {
            startActivity(MinkWorkspaceActivity.intent(this, screen, settingsDestination))
        }
    }

    private fun openHome(request: MinkHomeRequest) {
        if (isWorkspaceHost) {
            startActivity(MinkHomeRequestIntent.create(this, request))
        } else {
            homeRequest = request
        }
    }

    companion object {
        private const val ACTION_OPEN_TODOS =
            "com.katoaapps.openminilaunch.action.OPEN_TODOS"

        internal fun openTodosIntent(context: Context): Intent =
            MinkWorkspaceActivity.todosIntent(context)
    }
}
