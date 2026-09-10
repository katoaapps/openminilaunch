package com.katoaapps.openminilaunch.features.iconpacks

import android.graphics.drawable.Drawable

internal data class InstalledIconPack(
    val packageName: String,
    val label: String,
    val packIcon: Drawable?,
    val previewIcons: List<Drawable>,
    val hasReadableMappings: Boolean,
)

internal sealed interface IconPackDrawable {
    data class Static(val drawableName: String) : IconPackDrawable
    data class Calendar(val drawablePrefix: String) : IconPackDrawable
}

internal data class IconPackDefinition(
    val iconsByComponent: Map<String, IconPackDrawable>,
) {
    val iconsByPackage: Map<String, IconPackDrawable> = iconsByComponent.entries
        .groupBy { it.key.substringBefore('/') }
        .mapValues { (_, mappings) ->
            mappings.firstOrNull { it.value is IconPackDrawable.Calendar }?.value
                ?: mappings.first().value
        }

    fun iconForComponent(componentName: String, packageName: String): IconPackDrawable? =
        iconsByComponent[componentName] ?: iconsByPackage[packageName]

    fun iconForPackage(packageName: String): IconPackDrawable? = iconsByPackage[packageName]
}
