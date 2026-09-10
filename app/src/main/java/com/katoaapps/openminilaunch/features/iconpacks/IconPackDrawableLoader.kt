package com.katoaapps.openminilaunch.features.iconpacks

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.time.LocalDate

/** Turns a parsed icon-pack mapping into today's Android drawable. */
internal class IconPackDrawableLoader(
    private val packageManager: PackageManager,
    private val currentDayOfMonth: () -> Int = { LocalDate.now().dayOfMonth },
) {
    fun load(packageName: String, icon: IconPackDrawable): Drawable? = runCatching {
        val resources = packageManager.getResourcesForApplication(packageName)
        val drawableName = icon.drawableName(currentDayOfMonth())
        val resourceId = resources.getIdentifier(drawableName, "drawable", packageName)
        resourceId.takeIf { it != 0 }?.let { resources.getDrawable(it, null) }
    }.getOrNull()
}

internal fun IconPackDrawable.drawableName(dayOfMonth: Int): String = when (this) {
    is IconPackDrawable.Static -> drawableName
    is IconPackDrawable.Calendar -> drawablePrefix + dayOfMonth.coerceIn(1, 31)
}
