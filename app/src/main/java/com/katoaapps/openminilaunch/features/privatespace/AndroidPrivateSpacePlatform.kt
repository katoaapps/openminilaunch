package com.katoaapps.openminilaunch.features.privatespace

import android.content.Context
import android.content.IntentSender
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager

/** Android's standard Private Space API, kept separate from OEM gateway policy. */
internal class AndroidPrivateSpacePlatform(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val profiles = AndroidProfileClassifier(launcherApps)
    private val densityDpi = appContext.resources.displayMetrics.densityDpi

    fun snapshot(): PrivateSpaceSnapshot {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return PrivateSpaceSnapshot(PrivateSpaceStatus.UNSUPPORTED)
        }

        val user = privateProfile()
            ?: return PrivateSpaceSnapshot(PrivateSpaceStatus.NOT_CONFIGURED)
        if (userManager?.isQuietModeEnabled(user) == true) {
            return PrivateSpaceSnapshot(PrivateSpaceStatus.LOCKED)
        }
        val serial = userManager?.getSerialNumberForUser(user)?.takeIf { it >= 0 }
            ?: return PrivateSpaceSnapshot(PrivateSpaceStatus.NOT_CONFIGURED)
        return PrivateSpaceSnapshot(
            status = PrivateSpaceStatus.UNLOCKED,
            apps = appsFor(user, serial),
        )
    }

    fun isConfigured(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            privateProfile() != null

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
            // A false result while unlocking means Android opened its credential screen. The
            // profile broadcasts and Activity.onResume deliver the eventual state change.
            manager.requestQuietModeEnabled(locked, user)
            true
        }.getOrDefault(false)
    }

    fun launch(app: PrivateSpaceApp): Boolean = runCatching {
        val service = launcherApps ?: return false
        service.startMainActivity(app.componentName, app.user, null, null)
        true
    }.getOrDefault(false)

    private fun appsFor(user: UserHandle, serial: Long): List<PrivateSpaceApp> =
        runCatching { launcherApps?.getActivityList(null, user).orEmpty() }
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

    private companion object {
        const val ANDROID_16_API_LEVEL = 36
    }
}
