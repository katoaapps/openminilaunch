package com.katoaapps.openminilaunch.features.iconpacks

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/** Finds applications that advertise a supported launcher icon-pack contract. */
internal class IconPackDiscovery(
    private val packageManager: PackageManager,
) {
    fun installedThemeActivities(): List<ResolveInfo> = IconPackContract.discoveryActions
        .flatMap(::queryThemeActivities)
        .distinctBy { it.activityInfo.packageName }

    @Suppress("DEPRECATION")
    private fun queryThemeActivities(action: String): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                Intent(action),
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            packageManager.queryIntentActivities(Intent(action), PackageManager.MATCH_ALL)
        }
}
