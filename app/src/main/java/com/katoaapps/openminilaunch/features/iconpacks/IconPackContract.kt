package com.katoaapps.openminilaunch.features.iconpacks

/** External launcher contracts and resource locations understood by OpenMink. */
internal object IconPackContract {
    val discoveryActions = listOf(
        "com.novalauncher.THEME",
        "org.adw.ActivityStarter.THEMES",
    )

    /** Preferred first because packaged resources are safer to read than loose assets. */
    val mappingLocations = listOf(
        IconPackMappingLocation.ResourceXml,
        IconPackMappingLocation.ResourceRaw,
        IconPackMappingLocation.AssetXml,
    )

    const val MAPPING_RESOURCE_NAME = "appfilter"
}

internal sealed interface IconPackMappingLocation {
    data object ResourceXml : IconPackMappingLocation
    data object ResourceRaw : IconPackMappingLocation
    data object AssetXml : IconPackMappingLocation
}
