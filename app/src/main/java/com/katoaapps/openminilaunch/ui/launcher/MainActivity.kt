package com.katoaapps.openminilaunch.ui.launcher

import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetHost
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.applyLargeDisplayOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REQUEST_CONFIGURE_APP_WIDGET = 0x4D4B
private const val STATE_CONFIGURING_WIDGET_ID = "configuring_widget_id"
private const val STATE_WIDGET_RESULT_ID = "widget_result_id"
private const val STATE_WIDGET_RESULT_OK = "widget_result_ok"

class MainActivity : ComponentActivity() {
    private val homeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private var homeRequestToken by mutableIntStateOf(0)
    private var configuringWidgetId: Int? = null
    internal var widgetConfigurationResult by mutableStateOf<Pair<Int, Boolean>?>(null)
        private set
    private lateinit var actions: DeviceActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val store = LauncherStore.get(this)
        store.maybeEnableTwoPanelForDisplay(this)
        applyLargeDisplayOrientation(this, store.twoPanelModeForLargeDisplays)
        configuringWidgetId = savedInstanceState?.takeIf { it.containsKey(STATE_CONFIGURING_WIDGET_ID) }
            ?.getInt(STATE_CONFIGURING_WIDGET_ID)
        widgetConfigurationResult = savedInstanceState?.takeIf { it.containsKey(STATE_WIDGET_RESULT_ID) }
            ?.let { it.getInt(STATE_WIDGET_RESULT_ID) to it.getBoolean(STATE_WIDGET_RESULT_OK) }
        actions = DeviceActions(this)
        actions.removeLegacyLockAdmin()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { actions.installedApps() }
            store.migrateLauncherSelections(actions::normalizedLauncherSelectionKey)
        }
        setContent {
            val configuration = LocalConfiguration.current
            LaunchedEffect(store.twoPanelModeForLargeDisplays, configuration) {
                store.maybeEnableTwoPanelForDisplay(this@MainActivity)
                applyLargeDisplayOrientation(this@MainActivity, store.twoPanelModeForLargeDisplays)
            }
            MiniLaunchApp(store, actions, ::requestHomeRole, homeRequestToken)
        }
    }

    override fun onStart() {
        super.onStart()
        actions.refreshDynamicLauncherIcons()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Home requests reuse the launcher activity. This token resets transient Compose pages.
        if (intent.action == Intent.ACTION_MAIN) homeRequestToken++
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
}
