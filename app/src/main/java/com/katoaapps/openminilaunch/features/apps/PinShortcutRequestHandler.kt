package com.katoaapps.openminilaunch.features.apps

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcelable
import android.os.Process
import android.os.UserManager
import com.katoaapps.openminilaunch.model.LegacyLauncherShortcutTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import java.util.UUID

const val LEGACY_INSTALL_SHORTCUT_ACTION = "com.android.launcher.action.INSTALL_SHORTCUT"

/** Reads and accepts modern pin requests and legacy INSTALL_SHORTCUT broadcasts. */
internal class PinShortcutRequestHandler(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val legacyRepository = LegacyLauncherShortcutRepository.get(appContext)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val personalSerial = userManager?.getSerialNumberForUser(Process.myUserHandle()) ?: 0L
    private val densityDpi = appContext.resources.displayMetrics.densityDpi

    fun read(intent: Intent): PendingPinShortcut? =
        if (intent.action == LEGACY_INSTALL_SHORTCUT_ACTION) readLegacy(intent) else readModern(intent)

    fun accept(pending: PendingPinShortcut): Boolean = when {
        pending.modernRequest != null -> pending.modernRequest.isValid &&
            runCatching { pending.modernRequest.accept() }.getOrDefault(false)
        pending.target is LegacyLauncherShortcutTarget ->
            legacyRepository.save(pending.target, pending.icon)
        else -> false
    }

    private fun readModern(intent: Intent): PendingPinShortcut? {
        val service = launcherApps ?: return null
        val request = runCatching { service.getPinItemRequest(intent) }.getOrNull() ?: return null
        if (request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return null
        val info = request.shortcutInfo ?: return null
        val userSerial = userManager?.getSerialNumberForUser(info.userHandle)?.takeIf { it >= 0 }
            ?: return null
        val appLabel = publisherLabel(service, info)
        return PendingPinShortcut(
            modernRequest = request,
            target = LauncherShortcutTarget(
                label = info.shortLabel?.toString()
                    ?: info.longLabel?.toString()
                    ?: appLabel,
                packageName = info.`package`,
                shortcutId = info.id,
                userSerial = userSerial,
                isWorkProfile = userSerial != personalSerial,
            ),
            publisherLabel = appLabel,
            icon = runCatching {
                service.getShortcutBadgedIconDrawable(info, densityDpi)
            }.getOrNull(),
        )
    }

    private fun readLegacy(intent: Intent): PendingPinShortcut? {
        val launchIntent = intent.parcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
            ?: return null
        val resolved = appContext.packageManager.resolveActivity(launchIntent, 0)?.activityInfo
            ?: return null
        val packageName = launchIntent.component?.packageName
            ?: launchIntent.`package`
            ?: resolved.packageName
        val label = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: resolved.loadLabel(appContext.packageManager).toString()
        val target = LegacyLauncherShortcutTarget(
            id = UUID.randomUUID().toString(),
            label = label,
            packageName = packageName,
            intentUri = launchIntent.toUri(Intent.URI_INTENT_SCHEME),
            userSerial = personalSerial,
        )
        return PendingPinShortcut(
            target = target,
            publisherLabel = runCatching {
                resolved.applicationInfo.loadLabel(appContext.packageManager).toString()
            }.getOrDefault(packageName),
            icon = legacyIcon(intent),
        )
    }

    private fun legacyIcon(intent: Intent): Drawable? {
        intent.parcelableExtra<Bitmap>(Intent.EXTRA_SHORTCUT_ICON)?.let { bitmap ->
            return BitmapDrawable(appContext.resources, bitmap)
        }
        val resource = intent.parcelableExtra<Intent.ShortcutIconResource>(
            Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
        ) ?: return null
        return runCatching {
            val resources = appContext.packageManager.getResourcesForApplication(resource.packageName)
            val resourceId = resources.getIdentifier(resource.resourceName, null, null)
            resources.getDrawable(resourceId, null)
        }.getOrNull()
    }

    private fun publisherLabel(service: LauncherApps, info: ShortcutInfo): String = runCatching {
        service.getActivityList(info.`package`, info.userHandle)
            .firstOrNull()
            ?.label
            ?.toString()
    }.getOrNull()?.takeIf(String::isNotBlank) ?: info.`package`
}

internal data class PendingPinShortcut(
    internal val modernRequest: LauncherApps.PinItemRequest? = null,
    val target: LauncherTarget,
    val publisherLabel: String,
    val icon: Drawable?,
)

private inline fun <reified T : Parcelable> Intent.parcelableExtra(name: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name)
    }
