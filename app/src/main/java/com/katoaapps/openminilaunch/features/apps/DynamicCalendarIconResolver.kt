package com.katoaapps.openminilaunch.features.apps

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import java.time.LocalDate

/** Resolves the day-specific icon arrays published by calendar launcher activities. */
internal class DynamicCalendarIconResolver(
    context: Context,
    private val densityDpi: Int,
) {
    private val appContext = context.applicationContext

    fun iconFor(target: LauncherAppTarget): Drawable? {
        // Public PackageManager APIs cannot read activity metadata across Android profiles.
        // Work-profile targets retain LauncherApps' correctly badged fallback icon instead.
        if (target.isWorkProfile) return null
        return runCatching {
            val packageManager = appContext.packageManager
            val activityInfo = activityInfo(packageManager, target)
            val resources = packageManager.getResourcesForApplication(activityInfo.applicationInfo)
            val metadata = activityInfo.metaData ?: activityInfo.applicationInfo.metaData ?: return null
            val arrayId = dynamicCalendarMetadataKeys(target.packageName)
                .firstNotNullOfOrNull { key -> metadata.getInt(key, 0).takeIf { it != 0 } }
                ?: return null
            val icons = resources.obtainTypedArray(arrayId)
            val iconId = try {
                icons.getResourceId(calendarArrayIndex(LocalDate.now().dayOfMonth), 0)
            } finally {
                icons.recycle()
            }
            if (iconId == 0) return null
            resources.getDrawableForDensity(iconId, densityDpi, null)
        }.getOrNull()
    }

    private fun activityInfo(
        packageManager: PackageManager,
        target: LauncherAppTarget,
    ): ActivityInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getActivityInfo(
            target.componentName,
            PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getActivityInfo(target.componentName, PackageManager.GET_META_DATA)
    }
}

internal fun dynamicCalendarMetadataKeys(packageName: String): List<String> = listOf(
    "$packageName.dynamic_icons",
    "com.teslacoilsw.launcher.calendarIconArray",
)

internal fun calendarArrayIndex(dayOfMonth: Int): Int = (dayOfMonth - 1).coerceIn(0, 30)
