package com.katoaapps.openminilaunch.features.iconpacks

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import java.time.LocalDate

/** Turns a parsed icon-pack mapping into today's Android drawable. */
internal class IconPackDrawableLoader(
    context: Context,
    private val currentDayOfMonth: () -> Int = { LocalDate.now().dayOfMonth },
) {
    private val appContext = context.applicationContext

    fun load(packageName: String, icon: IconPackDrawable): Drawable? = runCatching {
        val packContext = appContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
        val resources = packContext.resources
        val drawableName = icon.drawableName(currentDayOfMonth())
        val resourceId = resources.getIdentifier(drawableName, "drawable", packageName)
            .takeIf { it != 0 }
            ?: resources.getIdentifier(drawableName, "mipmap", packageName).takeIf { it != 0 }
        resourceId?.let { ContextCompat.getDrawable(packContext, it) }
    }.getOrNull()
}

internal fun IconPackDrawable.drawableName(dayOfMonth: Int): String = when (this) {
    is IconPackDrawable.Static -> drawableName
    is IconPackDrawable.Calendar -> drawablePrefix + dayOfMonth.coerceIn(1, 31)
}
