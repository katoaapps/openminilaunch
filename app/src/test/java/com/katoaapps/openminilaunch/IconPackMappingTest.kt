package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.iconpacks.IconPackDefinition
import com.katoaapps.openminilaunch.features.iconpacks.IconPackContract
import com.katoaapps.openminilaunch.features.iconpacks.IconPackDrawable
import com.katoaapps.openminilaunch.features.iconpacks.IconPackMappingLocation
import com.katoaapps.openminilaunch.features.iconpacks.IconPackXmlParser
import com.katoaapps.openminilaunch.features.iconpacks.drawableName
import com.katoaapps.openminilaunch.features.apps.calendarArrayIndex
import com.katoaapps.openminilaunch.features.apps.dynamicCalendarMetadataKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconPackMappingTest {
    @Test fun supportedMappingLocationsHaveStableFallbackOrder() {
        assertEquals(
            listOf(
                IconPackMappingLocation.ResourceXml,
                IconPackMappingLocation.ResourceRaw,
                IconPackMappingLocation.AssetXml,
            ),
            IconPackContract.mappingLocations,
        )
    }

    @Test fun novaComponentNamesAreNormalized() {
        assertEquals(
            "com.example.notes/com.example.notes.MainActivity",
            IconPackXmlParser.normalizedComponent(
                "ComponentInfo{com.example.notes/.MainActivity}",
            ),
        )
        assertEquals(
            "com.example.notes/com.example.notes.MainActivity",
            IconPackXmlParser.normalizedComponent(
                "ComponentInfo{com.example.notes/com.example.notes.MainActivity}",
            ),
        )
    }

    @Test fun malformedComponentNamesAreIgnored() {
        assertNull(IconPackXmlParser.normalizedComponent("com.example.notes"))
        assertNull(IconPackXmlParser.normalizedComponent("ComponentInfo{/MainActivity}"))
    }

    @Test fun componentMappingsAlsoProvidePackageFallbacks() {
        val icon = IconPackDrawable.Static("notes")
        val definition = IconPackDefinition(
            mapOf("com.example.notes/com.example.notes.MainActivity" to icon),
        )

        assertEquals(icon, definition.iconsByPackage["com.example.notes"])
    }

    @Test fun dynamicCalendarMappingAlwaysOverridesStaticMapping() {
        val component = "com.example.calendar/com.example.calendar.MainActivity"
        val static = IconPackDrawable.Static("calendar_31")
        val dynamic = IconPackDrawable.Calendar("calendar_")

        val combined = IconPackXmlParser.combineMappings(
            staticMappings = mapOf(component to static),
            calendarMappings = mapOf(component to dynamic),
        )

        assertEquals(dynamic, combined[component])
    }

    @Test fun packageFallbackPrefersDynamicCalendarMapping() {
        val dynamic = IconPackDrawable.Calendar("calendar_")
        val definition = IconPackDefinition(
            linkedMapOf(
                "com.example.calendar/com.example.calendar.Static" to IconPackDrawable.Static("calendar_31"),
                "com.example.calendar/com.example.calendar.Dynamic" to dynamic,
            ),
        )

        assertEquals(dynamic, definition.iconsByPackage["com.example.calendar"])
    }

    @Test fun calendarAppMetadataSupportsAndroidAndNovaContracts() {
        assertEquals(
            listOf(
                "com.example.calendar.dynamic_icons",
                "com.teslacoilsw.launcher.calendarIconArray",
            ),
            dynamicCalendarMetadataKeys("com.example.calendar"),
        )
        assertEquals(0, calendarArrayIndex(1))
        assertEquals(30, calendarArrayIndex(31))
    }

    @Test fun drawableNamesResolveStaticAndCalendarMappings() {
        assertEquals("notes", IconPackDrawable.Static("notes").drawableName(dayOfMonth = 9))
        assertEquals("calendar_9", IconPackDrawable.Calendar("calendar_").drawableName(dayOfMonth = 9))
        assertEquals("calendar_1", IconPackDrawable.Calendar("calendar_").drawableName(dayOfMonth = 0))
        assertEquals("calendar_31", IconPackDrawable.Calendar("calendar_").drawableName(dayOfMonth = 99))
    }
}
