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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REQUEST_CONFIGURE_APP_WIDGET = 0x4D4B

class MainActivity : ComponentActivity() {
    private val homeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private var homeRequestToken by mutableIntStateOf(0)
    private var widgetConfigurationResult: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val store = LauncherStore.get(this)
        val actions = DeviceActions(this)
        actions.removeLegacyLockAdmin()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { actions.installedApps() }
            store.migrateLauncherSelections(actions::normalizedLauncherSelectionKey)
        }
        setContent { MiniLaunchApp(store, actions, ::requestHomeRole, homeRequestToken) }
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
        onResult: (Boolean) -> Unit,
    ): Boolean {
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
