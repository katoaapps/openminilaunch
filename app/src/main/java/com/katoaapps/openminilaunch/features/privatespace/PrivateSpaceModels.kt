package com.katoaapps.openminilaunch.features.privatespace

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle

internal data class PrivateSpaceApp(
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val user: UserHandle,
    val userSerial: Long,
    val icon: Drawable?,
) {
    val key: String = "$userSerial:${componentName.flattenToString()}"
}

internal data class PrivateSpaceSnapshot(
    val status: PrivateSpaceStatus,
    val apps: List<PrivateSpaceApp> = emptyList(),
    val gateway: PrivateSpaceGateway? = null,
)

internal data class PrivateSpaceGateway(
    val label: String,
    val packageName: String,
    val icon: Drawable?,
    val kind: PrivateSpaceGatewayKind,
)

internal enum class PrivateSpaceGatewayKind {
    SAMSUNG_SECURE_FOLDER,
    MOTOROLA_MOTO_SECURE,
}

internal enum class PrivateSpaceStatus {
    UNSUPPORTED,
    HOME_ROLE_REQUIRED,
    NOT_CONFIGURED,
    LOCKED,
    UNLOCKED,
    OEM_GATEWAY,
}
