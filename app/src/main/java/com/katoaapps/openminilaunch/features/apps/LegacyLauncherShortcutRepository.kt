package com.katoaapps.openminilaunch.features.apps

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserManager
import androidx.core.graphics.drawable.toBitmap
import com.katoaapps.openminilaunch.model.LegacyLauncherShortcutTarget
import com.katoaapps.openminilaunch.model.legacyLauncherShortcutId
import java.io.File

/** Persists and launches shortcuts delivered through the legacy launcher broadcast API. */
internal class LegacyLauncherShortcutRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val personalSerial = userManager?.getSerialNumberForUser(Process.myUserHandle()) ?: 0L
    private val iconDirectory = File(appContext.filesDir, ICON_DIRECTORY)

    fun resolve(selectionKey: String): LegacyLauncherShortcutTarget? {
        val id = legacyLauncherShortcutId(selectionKey) ?: return null
        val label = prefs.getString(recordKey(id, FIELD_LABEL), null)?.takeIf(String::isNotBlank)
            ?: return null
        val packageName = prefs.getString(recordKey(id, FIELD_PACKAGE), null)
            ?.takeIf(String::isNotBlank)
            ?: return null
        val intentUri = prefs.getString(recordKey(id, FIELD_INTENT), null)
            ?.takeIf(String::isNotBlank)
            ?: return null
        return LegacyLauncherShortcutTarget(
            id = id,
            label = label,
            packageName = packageName,
            intentUri = intentUri,
            userSerial = personalSerial,
            isAvailable = parseIntent(intentUri)?.let(::canResolve) == true,
            selectionKey = selectionKey,
        )
    }

    fun save(target: LegacyLauncherShortcutTarget, icon: Drawable?): Boolean {
        val saved = prefs.edit()
            .putString(recordKey(target.id, FIELD_LABEL), target.label)
            .putString(recordKey(target.id, FIELD_PACKAGE), target.packageName)
            .putString(recordKey(target.id, FIELD_INTENT), target.intentUri)
            .commit()
        if (!saved) return false
        persistIcon(target.id, icon)
        return true
    }

    fun icon(target: LegacyLauncherShortcutTarget): Drawable? {
        val bitmap = BitmapFactory.decodeFile(iconFile(target.id).absolutePath)
        if (bitmap != null) return BitmapDrawable(appContext.resources, bitmap)
        return runCatching { appContext.packageManager.getApplicationIcon(target.packageName) }
            .getOrNull()
    }

    fun launch(target: LegacyLauncherShortcutTarget): Boolean {
        if (!target.isAvailable) return false
        val intent = parseIntent(target.intentUri) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun retainSelections(selectionKeys: Collection<String>) {
        val retained = selectionKeys.mapNotNull(::legacyLauncherShortcutId).toSet()
        storedIds().filterNot(retained::contains).forEach(::delete)
    }

    private fun canResolve(intent: Intent): Boolean =
        appContext.packageManager.resolveActivity(intent, 0) != null

    private fun persistIcon(id: String, drawable: Drawable?) {
        if (drawable == null) return
        runCatching {
            iconDirectory.mkdirs()
            iconFile(id).outputStream().use { output ->
                drawable.toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
                    .compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }

    private fun storedIds(): Set<String> = prefs.all.keys.mapNotNull { key ->
        key.removePrefix(RECORD_PREFIX)
            .substringBefore(':')
            .takeIf { key.startsWith(RECORD_PREFIX) && it.isNotBlank() }
    }.toSet()

    private fun delete(id: String) {
        prefs.edit()
            .remove(recordKey(id, FIELD_LABEL))
            .remove(recordKey(id, FIELD_PACKAGE))
            .remove(recordKey(id, FIELD_INTENT))
            .apply()
        iconFile(id).delete()
    }

    private fun iconFile(id: String): File = File(iconDirectory, "$id.png")

    private fun parseIntent(uri: String): Intent? =
        runCatching { Intent.parseUri(uri, Intent.URI_INTENT_SCHEME) }.getOrNull()

    private fun recordKey(id: String, field: String): String = "$RECORD_PREFIX$id:$field"

    companion object {
        private const val PREFS_NAME = "legacy_launcher_shortcuts"
        private const val RECORD_PREFIX = "record:"
        private const val FIELD_LABEL = "label"
        private const val FIELD_PACKAGE = "package"
        private const val FIELD_INTENT = "intent"
        private const val ICON_DIRECTORY = "legacy_launcher_shortcut_icons"
        private const val ICON_SIZE_PX = 192

        @Volatile private var instance: LegacyLauncherShortcutRepository? = null

        fun get(context: Context): LegacyLauncherShortcutRepository = instance ?: synchronized(this) {
            instance ?: LegacyLauncherShortcutRepository(context).also { instance = it }
        }
    }
}
