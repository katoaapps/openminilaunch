package com.katoaapps.openminilaunch.ui.launcher

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

internal fun isPermanentlyDenied(context: Context, permission: String): Boolean =
    context is Activity &&
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
        !ActivityCompat.shouldShowRequestPermissionRationale(context, permission)

internal fun mediaReadPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

internal fun hasMediaReadAccess(context: Context): Boolean = mediaReadPermissions().any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

internal fun mediaPermissionPermanentlyDenied(context: Context): Boolean =
    !hasMediaReadAccess(context) && mediaReadPermissions().all { isPermanentlyDenied(context, it) }

internal fun supportsDirectCalls(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

internal fun supportsDirectSms(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)
