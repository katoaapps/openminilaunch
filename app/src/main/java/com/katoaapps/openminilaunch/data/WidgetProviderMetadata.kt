package com.katoaapps.openminilaunch.data

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import com.katoaapps.openminilaunch.model.WidgetProviderIdentity

internal fun AppWidgetProviderInfo.toWidgetProviderIdentity(
    packageManager: PackageManager,
): WidgetProviderIdentity {
    val packageName = provider.packageName
    val appLabel = runCatching {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(packageName, 0),
        ).toString()
    }.getOrDefault(packageName)
    return WidgetProviderIdentity(
        packageName = packageName,
        className = provider.className,
        appLabel = appLabel,
        widgetLabel = loadLabel(packageManager).toString(),
    )
}
