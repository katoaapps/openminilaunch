package com.katoaapps.openminilaunch.features.iconpacks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class IconPackRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val discovery = IconPackDiscovery(packageManager)
    private val definitionLoader = IconPackDefinitionLoader(packageManager)
    private val drawableLoader = IconPackDrawableLoader(packageManager)
    private val revisionState = MutableStateFlow(0L)
    val revision: StateFlow<Long> = revisionState.asStateFlow()

    private val cacheLock = Any()
    private val definitions = mutableMapOf<String, IconPackDefinition>()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = invalidate()
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            appContext,
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    fun installedPacks(): List<InstalledIconPack> {
        return discovery.installedThemeActivities()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                runCatching {
                    val definition = definition(packageName)
                    InstalledIconPack(
                        packageName = packageName,
                        label = resolveInfo.loadLabel(packageManager).toString(),
                        packIcon = resolveInfo.loadIcon(packageManager),
                        previewIcons = previewIcons(packageName),
                        hasReadableMappings = definition?.iconsByComponent?.isNotEmpty() == true,
                    )
                }.getOrNull()
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, InstalledIconPack::label))
    }

    fun iconFor(target: LauncherTarget, iconPackPackage: String): Drawable? {
        val definition = definition(iconPackPackage) ?: return null
        val specification = when (target) {
            is LauncherAppTarget -> definition.iconForComponent(
                componentName = target.componentName.flattenToString(),
                packageName = target.packageName,
            )
            is LauncherShortcutTarget -> definition.iconForPackage(target.packageName)
            else -> null
        } ?: return null
        val drawable = drawableLoader.load(iconPackPackage, specification) ?: return null
        if (!target.isWorkProfile) return drawable
        val user = userManager?.getUserForSerialNumber(target.userSerial) ?: return drawable
        return packageManager.getUserBadgedIcon(drawable, user)
    }

    fun iconForPackage(packageName: String, iconPackPackage: String): Drawable? {
        val specification = definition(iconPackPackage)?.iconForPackage(packageName) ?: return null
        return drawableLoader.load(iconPackPackage, specification)
    }

    fun invalidate() {
        synchronized(cacheLock) { definitions.clear() }
        revisionState.update { it + 1 }
    }

    private fun definition(packageName: String): IconPackDefinition? {
        synchronized(cacheLock) { definitions[packageName]?.let { return it } }
        val parsed = definitionLoader.load(packageName) ?: return null
        synchronized(cacheLock) { definitions[packageName] = parsed }
        return parsed
    }

    private fun previewIcons(packageName: String): List<Drawable> {
        val candidates = definition(packageName)?.iconsByComponent?.values.orEmpty().distinct()
        return candidates.asSequence()
            .mapNotNull { drawableLoader.load(packageName, it) }
            .take(PREVIEW_ICON_COUNT)
            .toList()
    }

    companion object {
        private const val PREVIEW_ICON_COUNT = 4

        @Volatile private var instance: IconPackRepository? = null

        fun get(context: Context): IconPackRepository = instance ?: synchronized(this) {
            instance ?: IconPackRepository(context.applicationContext).also { instance = it }
        }
    }
}
