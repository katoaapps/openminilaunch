package com.katoaapps.openminilaunch.features.privatespace

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Standard Private Space and certified OEM gateways, isolated from the normal app library. */
internal class PrivateSpaceRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val roleManager = appContext.getSystemService(RoleManager::class.java)
    private val profiles = AndroidProfileClassifier(launcherApps)
    private val oemGateways = OemPrivateContainerGatewayResolver(appContext)
    private val densityDpi = appContext.resources.displayMetrics.densityDpi
    private val revisionState = MutableStateFlow(0L)
    val revision: StateFlow<Long> = revisionState.asStateFlow()

    private val launcherCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()
        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()
    }

    private val profileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refresh()
    }

    init {
        runCatching { launcherApps?.registerCallback(launcherCallback) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }
            runCatching {
                ContextCompat.registerReceiver(
                    appContext,
                    profileReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED,
                )
            }
        }
    }

    fun snapshot(): PrivateSpaceSnapshot {
        if (roleManager?.isRoleHeld(RoleManager.ROLE_HOME) != true) {
            return PrivateSpaceSnapshot(PrivateSpaceStatus.HOME_ROLE_REQUIRED)
        }

        oemGateways.resolve()?.let { gateway ->
            return PrivateSpaceSnapshot(
                status = PrivateSpaceStatus.OEM_GATEWAY,
                gateway = gateway,
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return PrivateSpaceSnapshot(PrivateSpaceStatus.UNSUPPORTED)
        }

        val user = privateProfile() ?: return PrivateSpaceSnapshot(PrivateSpaceStatus.NOT_CONFIGURED)
        if (userManager?.isQuietModeEnabled(user) == true) {
            return PrivateSpaceSnapshot(PrivateSpaceStatus.LOCKED)
        }
        val serial = userManager?.getSerialNumberForUser(user)?.takeIf { it >= 0 }
            ?: return PrivateSpaceSnapshot(PrivateSpaceStatus.NOT_CONFIGURED)
        val apps = runCatching { launcherApps?.getActivityList(null, user).orEmpty() }
            .getOrDefault(emptyList())
            .filterNot { it.applicationInfo.packageName == appContext.packageName }
            .map { info ->
                PrivateSpaceApp(
                    label = info.label.toString(),
                    packageName = info.applicationInfo.packageName,
                    componentName = info.componentName,
                    user = user,
                    userSerial = serial,
                    icon = runCatching { info.getBadgedIcon(densityDpi) }.getOrNull(),
                )
            }
            .distinctBy(PrivateSpaceApp::key)
            .sortedBy { it.label.lowercase() }
        return PrivateSpaceSnapshot(PrivateSpaceStatus.UNLOCKED, apps)
    }

    /**
     * Only advertise a container Mink can actually reach. Some OEMs expose fragments of the
     * Android contract while suppressing setup, so a settings IntentSender alone is not proof.
     */
    fun isEntryPointAvailable(): Boolean {
        if (roleManager?.isRoleHeld(RoleManager.ROLE_HOME) != true) return false
        if (oemGateways.resolve() != null) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false
        return privateProfile() != null
    }

    fun openSettings(context: Context): Boolean {
        val sender = privateSpaceSettingsIntentSender() ?: return false
        return runCatching {
            context.startIntentSender(sender, null, 0, 0, 0)
            true
        }.getOrDefault(false)
    }

    fun setLocked(locked: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false
        val user = privateProfile() ?: return false
        return runCatching {
            val manager = userManager ?: return false
            // A false result while unlocking means Android opened its credential screen; it is
            // still a valid request and the profile broadcasts/onResume will deliver the result.
            manager.requestQuietModeEnabled(locked, user)
            true
        }.getOrDefault(false).also { refresh() }
    }

    fun launch(app: PrivateSpaceApp): Boolean = runCatching {
        val service = launcherApps ?: return false
        service.startMainActivity(app.componentName, app.user, null, null)
        true
    }.getOrDefault(false)

    fun launchGateway(gateway: PrivateSpaceGateway): Boolean = runCatching {
        val intent = appContext.packageManager.getLaunchIntentForPackage(gateway.packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
        true
    }.getOrDefault(false)

    fun refresh() {
        revisionState.update { it + 1 }
    }

    private fun privateProfile(): UserHandle? = runCatching {
        launcherApps?.profiles.orEmpty()
            .firstOrNull { profiles.classify(it) == AndroidProfileKind.PRIVATE }
    }.getOrNull()

    /** Uses reflection until OpenMink moves its compile SDK beyond Android 15. */
    private fun privateSpaceSettingsIntentSender(): IntentSender? {
        if (Build.VERSION.SDK_INT < ANDROID_16_API_LEVEL) return null
        val service = launcherApps ?: return null
        return runCatching {
            LauncherApps::class.java
                .getMethod("getPrivateSpaceSettingsIntent")
                .invoke(service) as? IntentSender
        }.getOrNull()
    }

    companion object {
        private const val ANDROID_16_API_LEVEL = 36
        @Volatile private var instance: PrivateSpaceRepository? = null

        fun get(context: Context): PrivateSpaceRepository = instance ?: synchronized(this) {
            instance ?: PrivateSpaceRepository(context).also { instance = it }
        }
    }
}
