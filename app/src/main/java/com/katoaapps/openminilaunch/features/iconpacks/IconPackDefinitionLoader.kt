package com.katoaapps.openminilaunch.features.iconpacks

import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/** Reads appfilter mappings from the resource locations used by Nova-compatible packs. */
internal class IconPackDefinitionLoader(
    private val packageManager: PackageManager,
) {
    fun load(packageName: String): IconPackDefinition? {
        val resources = runCatching {
            packageManager.getResourcesForApplication(packageName)
        }.getOrNull() ?: return null

        return IconPackContract.mappingLocations.firstNotNullOfOrNull { location ->
            // A malformed higher-priority file should not hide a valid fallback file.
            runCatching { load(resources, packageName, location) }.getOrNull()
        }
    }

    private fun load(
        resources: Resources,
        packageName: String,
        location: IconPackMappingLocation,
    ): IconPackDefinition? = when (location) {
        IconPackMappingLocation.ResourceXml -> {
            val resourceId = resources.getIdentifier(
                IconPackContract.MAPPING_RESOURCE_NAME,
                "xml",
                packageName,
            )
            resourceId.takeIf { it != 0 }?.let { parseXmlResource(resources, it) }
        }
        IconPackMappingLocation.ResourceRaw -> {
            val resourceId = resources.getIdentifier(
                IconPackContract.MAPPING_RESOURCE_NAME,
                "raw",
                packageName,
            )
            resourceId.takeIf { it != 0 }?.let { resources.openRawResource(it).use(::parseStream) }
        }
        IconPackMappingLocation.AssetXml -> {
            resources.assets.open("${IconPackContract.MAPPING_RESOURCE_NAME}.xml").use(::parseStream)
        }
    }

    private fun parseXmlResource(resources: Resources, resourceId: Int): IconPackDefinition {
        resources.getXml(resourceId).let { parser ->
            return try {
                IconPackXmlParser.parse(parser)
            } finally {
                parser.close()
            }
        }
    }

    private fun parseStream(input: InputStream): IconPackDefinition {
        val parser: XmlPullParser = Xml.newPullParser().apply {
            setInput(input, Charsets.UTF_8.name())
        }
        return IconPackXmlParser.parse(parser)
    }
}
