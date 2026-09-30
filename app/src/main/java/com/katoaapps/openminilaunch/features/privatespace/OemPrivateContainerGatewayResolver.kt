package com.katoaapps.openminilaunch.features.privatespace

import android.content.Context
import android.os.Build

/**
 * Resolves OEM-owned private containers that intentionally do not expose their contained apps
 * through Android's standard Private Space launcher contract.
 */
internal class OemPrivateContainerGatewayResolver(context: Context) {
    private val packageManager = context.packageManager

    fun resolve(): PrivateSpaceGateway? = when {
        Build.MANUFACTURER.equals(SAMSUNG_MANUFACTURER, ignoreCase = true) -> {
            resolvePackage(
                packageName = SAMSUNG_SECURE_FOLDER_PACKAGE,
                kind = PrivateSpaceGatewayKind.SAMSUNG_SECURE_FOLDER,
            )
        }
        Build.MANUFACTURER.equals(MOTOROLA_MANUFACTURER, ignoreCase = true) -> {
            resolvePackage(
                packageName = MOTOROLA_MOTO_SECURE_PACKAGE,
                kind = PrivateSpaceGatewayKind.MOTOROLA_MOTO_SECURE,
            )
        }
        else -> null
    }

    private fun resolvePackage(
        packageName: String,
        kind: PrivateSpaceGatewayKind,
    ): PrivateSpaceGateway? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        val resolvedPackage = launchIntent.component?.packageName ?: packageName
        val applicationInfo = runCatching {
            packageManager.getApplicationInfo(resolvedPackage, 0)
        }.getOrNull() ?: return null
        return PrivateSpaceGateway(
            label = packageManager.getApplicationLabel(applicationInfo).toString(),
            packageName = applicationInfo.packageName,
            icon = runCatching { packageManager.getApplicationIcon(applicationInfo) }.getOrNull(),
            kind = kind,
        )
    }

    private companion object {
        const val SAMSUNG_MANUFACTURER = "samsung"
        const val SAMSUNG_SECURE_FOLDER_PACKAGE = "com.samsung.knox.securefolder"
        const val MOTOROLA_MANUFACTURER = "motorola"
        const val MOTOROLA_MOTO_SECURE_PACKAGE = "com.motorola.securityhub"
    }
}
