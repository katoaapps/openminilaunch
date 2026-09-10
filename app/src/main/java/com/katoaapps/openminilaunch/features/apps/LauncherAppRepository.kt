package com.katoaapps.openminilaunch.features.apps

import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.model.launcherAppIdentity

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Profile-aware source of truth for apps that can appear on launcher surfaces. */
internal class LauncherAppRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val personalUser = Process.myUserHandle()
    private val personalSerial = userManager?.getSerialNumberForUser(personalUser) ?: 0L
    private val densityDpi = appContext.resources.displayMetrics.densityDpi
    private val dynamicCalendarIcons = DynamicCalendarIconResolver(appContext, densityDpi)
    private val revisionState = MutableStateFlow(0L)
    val revision: StateFlow<Long> = revisionState.asStateFlow()

    private val cacheLock = Any()
    @Volatile private var targetsCache: List<LauncherAppTarget>? = null
    private val activityCache = mutableMapOf<String, LauncherActivityInfo>()
    private val dynamicIconMonitor = DynamicLauncherIconMonitor(appContext, ::invalidate)

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = invalidate()
        override fun onPackageAdded(packageName: String, user: UserHandle) = invalidate()
        override fun onPackageChanged(packageName: String, user: UserHandle) = invalidate()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = invalidate()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = invalidate()
    }

    init {
        runCatching { launcherApps?.registerCallback(callback) }
    }

    fun targets(): List<LauncherAppTarget> {
        targetsCache?.let { return it }
        return synchronized(cacheLock) {
            targetsCache?.let { return@synchronized it }
            val service = launcherApps ?: return@synchronized emptyList()
            activityCache.clear()
            val discovered = accessibleProfiles(service).flatMap { user ->
                val serial = userManager?.getSerialNumberForUser(user)?.takeIf { it >= 0 }
                    ?: return@flatMap emptyList()
                // LauncherApps exposes the current profile plus managed profiles. A different
                // serial therefore identifies a work-profile copy, even when packages match.
                val workProfile = serial != personalSerial
                val quiet = runCatching { userManager?.isQuietModeEnabled(user) == true }.getOrDefault(false)
                runCatching { service.getActivityList(null, user) }.getOrDefault(emptyList()).map { info ->
                    val available = !quiet && runCatching {
                        service.isActivityEnabled(info.componentName, user)
                    }.getOrDefault(true)
                    LauncherAppTarget(
                        label = info.label.toString(),
                        packageName = info.applicationInfo.packageName,
                        componentName = info.componentName,
                        userSerial = serial,
                        isWorkProfile = workProfile,
                        isAvailable = available,
                    ).also { target ->
                        activityCache[target.selectionKey] = info
                    }
                }
            }.filterNot { it.packageName == appContext.packageName }
                .distinctBy(LauncherAppTarget::selectionKey)
                .sortedWith(
                    compareBy<LauncherAppTarget, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
                        .thenBy { it.isWorkProfile },
                )
            targetsCache = discovered
            discovered
        }
    }

    /** Resolves new profile-aware keys and legacy package-only selections. */
    fun resolve(selectionKey: String): LauncherAppTarget {
        val currentTargets = targets()
        currentTargets.firstOrNull { it.selectionKey == selectionKey }?.let { return it }
        val identity = launcherAppIdentity(selectionKey)
        if (identity == null) {
            currentTargets.firstOrNull { !it.isWorkProfile && it.packageName == selectionKey }?.let { return it }
            return unavailableLegacyTarget(selectionKey)
        }
        val workProfile = identity.userSerial != personalSerial
        return LauncherAppTarget(
            label = identity.componentName.packageName,
            packageName = identity.componentName.packageName,
            componentName = identity.componentName,
            userSerial = identity.userSerial,
            isWorkProfile = workProfile,
            isAvailable = false,
            selectionKey = selectionKey,
        )
    }

    fun normalizedSelectionKey(selectionKey: String): String? {
        if (launcherAppIdentity(selectionKey) != null) return selectionKey
        return targets().firstOrNull { !it.isWorkProfile && it.packageName == selectionKey }?.selectionKey
    }

    fun icon(target: LauncherAppTarget): Drawable? {
        dynamicCalendarIcons.iconFor(target)?.let { return it }
        val info = activityInfo(target.selectionKey)
        if (info != null) return runCatching { info.getBadgedIcon(densityDpi) }.getOrNull()
        if (target.isWorkProfile) return null
        return runCatching { appContext.packageManager.getApplicationIcon(target.packageName) }.getOrNull()
    }

    fun launch(target: LauncherAppTarget): Boolean {
        if (!target.isAvailable) return false
        val service = launcherApps ?: return false
        val user = userManager?.getUserForSerialNumber(target.userSerial) ?: return false
        return runCatching {
            service.startMainActivity(target.componentName, user, null, null)
            true
        }.getOrDefault(false)
    }

    fun openAppDetails(target: LauncherTarget): Boolean {
        val service = launcherApps ?: return false
        val user = userManager?.getUserForSerialNumber(target.userSerial) ?: return false
        val component = appDetailsComponent(target, targets()) ?: return false
        return runCatching {
            service.startAppDetailsActivity(component, user, null, null)
            true
        }.getOrDefault(false)
    }

    fun invalidate() {
        synchronized(cacheLock) {
            targetsCache = null
            activityCache.clear()
        }
        revisionState.update { it + 1 }
    }

    fun refreshDynamicIconsIfDateChanged() {
        dynamicIconMonitor.refreshIfDateChanged()
    }

    private fun accessibleProfiles(service: LauncherApps): List<UserHandle> = runCatching {
        service.profiles
    }.getOrDefault(emptyList()).ifEmpty { listOf(personalUser) }

    private fun activityInfo(selectionKey: String): LauncherActivityInfo? {
        synchronized(cacheLock) { activityCache[selectionKey]?.let { return it } }
        targets()
        return synchronized(cacheLock) { activityCache[selectionKey] }
    }

    private fun unavailableLegacyTarget(packageName: String) = LauncherAppTarget(
        label = packageName,
        packageName = packageName,
        componentName = ComponentName(packageName, ""),
        userSerial = personalSerial,
        isWorkProfile = false,
        isAvailable = false,
        selectionKey = packageName,
    )

    companion object {
        @Volatile private var instance: LauncherAppRepository? = null

        fun get(context: Context): LauncherAppRepository = instance ?: synchronized(this) {
            instance ?: LauncherAppRepository(context.applicationContext).also { instance = it }
        }
    }
}
