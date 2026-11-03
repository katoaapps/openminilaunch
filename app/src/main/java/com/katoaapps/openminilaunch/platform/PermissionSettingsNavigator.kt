package com.katoaapps.openminilaunch.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens the narrowest available Android settings surface for runtime permissions. */
internal class PermissionSettingsNavigator(private val context: Context) {
    fun openAppPermissions(): Boolean {
        val permissions = Intent(ACTION_MANAGE_APP_PERMISSIONS)
            .putExtra(EXTRA_PACKAGE_NAME, context.packageName)
        permissions.resolveActivity(context.packageManager)
            ?.packageName
            ?.let(permissions::setPackage)
        if (start(permissions)) return true
        return openAppInfo()
    }

    fun openAppInfo(): Boolean = start(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ),
    )

    private fun start(intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    private companion object {
        const val ACTION_MANAGE_APP_PERMISSIONS = "android.intent.action.MANAGE_APP_PERMISSIONS"
        const val EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME"
    }
}
