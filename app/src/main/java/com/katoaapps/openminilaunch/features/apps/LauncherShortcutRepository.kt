package com.katoaapps.openminilaunch.features.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.launcherShortcutIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Profile-aware source for shortcuts published by installed apps. */
internal class LauncherShortcutRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val personalUser = Process.myUserHandle()
    private val personalSerial = userManager?.getSerialNumberForUser(personalUser) ?: 0L
    private val densityDpi = appContext.resources.displayMetrics.densityDpi
    private val revisionState = MutableStateFlow(0L)
    val revision: StateFlow<Long> = revisionState.asStateFlow()

    private val cacheLock = Any()
    @Volatile private var targetsCache: List<LauncherShortcutTarget>? = null
    private val shortcutCache = mutableMapOf<String, ShortcutInfo>()
    private val pinnedScopes = mutableSetOf<ShortcutScope>()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = invalidate()
        override fun onPackageAdded(packageName: String, user: UserHandle) = invalidate()
        override fun onPackageChanged(packageName: String, user: UserHandle) = invalidate()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = invalidate()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = invalidate()
        override fun onShortcutsChanged(packageName: String, shortcuts: List<ShortcutInfo>, user: UserHandle) = invalidate()
    }

    init {
        runCatching { launcherApps?.registerCallback(callback) }
    }

    fun hasHostPermission(): Boolean = runCatching {
        launcherApps?.hasShortcutHostPermission() == true
    }.getOrDefault(false)

    fun targets(): List<LauncherShortcutTarget> {
        targetsCache?.let { return it }
        return synchronized(cacheLock) {
            targetsCache?.let { return@synchronized it }
            val service = launcherApps ?: return@synchronized emptyList()
            if (!hasHostPermission()) return@synchronized emptyList()

            shortcutCache.clear()
            pinnedScopes.clear()
            val query = LauncherApps.ShortcutQuery().setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED,
            )
            val discovered = accessibleProfiles(service).flatMap { user ->
                val serial = userManager?.getSerialNumberForUser(user)?.takeIf { it >= 0 }
                    ?: return@flatMap emptyList()
                val workProfile = serial != personalSerial
                val quiet = runCatching { userManager?.isQuietModeEnabled(user) == true }.getOrDefault(false)
                runCatching { service.getShortcuts(query, user) }.getOrNull().orEmpty()
                    .filter { info ->
                        info.isEnabled && !(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                info.isExcludedFromSurfaces(ShortcutInfo.SURFACE_LAUNCHER)
                            )
                    }
                    .map { info ->
                    LauncherShortcutTarget(
                        label = info.shortLabel?.toString()
                            ?: info.longLabel?.toString()
                            ?: packageLabel(info.`package`),
                        packageName = info.`package`,
                        shortcutId = info.id,
                        userSerial = serial,
                        isWorkProfile = workProfile,
                        isAvailable = !quiet,
                    ).also { target ->
                        shortcutCache[target.selectionKey] = info
                        if (info.isPinned) pinnedScopes += ShortcutScope(serial, info.`package`)
                    }
                }
            }.filterNot { it.packageName == appContext.packageName }
                .distinctBy(LauncherShortcutTarget::selectionKey)
                .sortedWith(
                    compareBy<LauncherShortcutTarget, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
                        .thenBy { it.isWorkProfile },
                )
            targetsCache = discovered
            discovered
        }
    }

    fun resolve(selectionKey: String): LauncherShortcutTarget? {
        val identity = launcherShortcutIdentity(selectionKey) ?: return null
        targets().firstOrNull { it.selectionKey == selectionKey }?.let { return it }
        return LauncherShortcutTarget(
            label = packageLabel(identity.packageName),
            packageName = identity.packageName,
            shortcutId = identity.shortcutId,
            userSerial = identity.userSerial,
            isWorkProfile = identity.userSerial != personalSerial,
            isAvailable = false,
            selectionKey = selectionKey,
        )
    }

    /**
     * Returns current sharing shortcuts published by the requested messaging apps.
     *
     * A non-empty category set distinguishes Android direct-share targets from ordinary launcher
     * actions such as "New message". Dynamic-only filtering avoids presenting stale cached chats
     * that the provider no longer considers recent.
     */
    fun recentConversationTargets(packageNames: Set<String>): List<LauncherShortcutTarget> {
        if (packageNames.isEmpty()) return emptyList()
        val targetsByKey = targets().associateBy(LauncherShortcutTarget::selectionKey)
        return synchronized(cacheLock) {
            shortcutCache.mapNotNull { (selectionKey, info) ->
                val target = targetsByKey[selectionKey] ?: return@mapNotNull null
                info.asRecentConversationShortcut(target, packageNames)
            }
        }.sortedWith(recentConversationShortcutOrder)
            .map(RecentConversationShortcut::target)
    }

    fun icon(target: LauncherShortcutTarget): Drawable? {
        val info = shortcutInfo(target.selectionKey) ?: return null
        return runCatching { launcherApps?.getShortcutBadgedIconDrawable(info, densityDpi) }.getOrNull()
    }

    /** Categories published with this shortcut, including its Direct Share target category. */
    fun categories(target: LauncherShortcutTarget): Set<String> =
        shortcutInfo(target.selectionKey)?.categories.orEmpty()

    fun launch(target: LauncherShortcutTarget): Boolean {
        if (!target.isAvailable) return false
        val service = launcherApps ?: return false
        val user = userManager?.getUserForSerialNumber(target.userSerial) ?: return false
        return runCatching {
            service.startShortcut(target.packageName, target.shortcutId, null, null, user)
            true
        }.getOrDefault(false)
    }

    /** Keeps app-published dynamic shortcuts alive while they occupy a Mink slot. */
    fun syncPinnedSelections(selectionKeys: Collection<String>) {
        val service = launcherApps ?: return
        if (!hasHostPermission()) return
        targets()
        val selected = selectionKeys.mapNotNull(::launcherShortcutIdentity)
            .groupBy { ShortcutScope(it.userSerial, it.packageName) }
        val scopes = synchronized(cacheLock) { pinnedScopes.toSet() } + selected.keys
        scopes.forEach { scope ->
            val user = userManager?.getUserForSerialNumber(scope.userSerial) ?: return@forEach
            val ids = selected[scope].orEmpty().map { it.shortcutId }.distinct()
            runCatching { service.pinShortcuts(scope.packageName, ids, user) }
        }
        invalidate()
    }

    fun invalidate() {
        synchronized(cacheLock) {
            targetsCache = null
            shortcutCache.clear()
            pinnedScopes.clear()
        }
        revisionState.update { it + 1 }
    }

    private fun shortcutInfo(selectionKey: String): ShortcutInfo? {
        synchronized(cacheLock) { shortcutCache[selectionKey]?.let { return it } }
        targets()
        return synchronized(cacheLock) { shortcutCache[selectionKey] }
    }

    private fun accessibleProfiles(service: LauncherApps): List<UserHandle> = runCatching {
        service.profiles
    }.getOrDefault(emptyList()).ifEmpty { listOf(personalUser) }

    private fun packageLabel(packageName: String): String = runCatching {
        val info = appContext.packageManager.getApplicationInfo(packageName, 0)
        appContext.packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    private data class ShortcutScope(val userSerial: Long, val packageName: String)

    companion object {
        @Volatile private var instance: LauncherShortcutRepository? = null

        fun get(context: Context): LauncherShortcutRepository = instance ?: synchronized(this) {
            instance ?: LauncherShortcutRepository(context.applicationContext).also { instance = it }
        }
    }
}
