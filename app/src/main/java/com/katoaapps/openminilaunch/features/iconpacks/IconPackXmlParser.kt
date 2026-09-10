package com.katoaapps.openminilaunch.features.iconpacks

import org.xmlpull.v1.XmlPullParser

internal object IconPackXmlParser {
    fun parse(parser: XmlPullParser): IconPackDefinition {
        val staticMappings = linkedMapOf<String, IconPackDrawable>()
        val calendarMappings = linkedMapOf<String, IconPackDrawable>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                val component = parser.getAttributeValue(null, "component")
                    ?.let(::normalizedComponent)
                when (parser.name) {
                    "item" -> {
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (component != null && !drawable.isNullOrBlank()) {
                            staticMappings[component] = IconPackDrawable.Static(drawable)
                        }
                    }
                    "calendar" -> {
                        val prefix = parser.getAttributeValue(null, "prefix")
                        if (component != null && !prefix.isNullOrBlank()) {
                            calendarMappings[component] = IconPackDrawable.Calendar(prefix)
                        }
                    }
                }
            }
            parser.next()
        }
        return IconPackDefinition(combineMappings(staticMappings, calendarMappings))
    }

    /** Nova requires calendar entries to win even when a static item appears later in the XML. */
    internal fun combineMappings(
        staticMappings: Map<String, IconPackDrawable>,
        calendarMappings: Map<String, IconPackDrawable>,
    ): Map<String, IconPackDrawable> = staticMappings + calendarMappings

    internal fun normalizedComponent(value: String): String? {
        val flattened = value.trim()
            .removePrefix("ComponentInfo{")
            .removeSuffix("}")
        val separator = flattened.indexOf('/')
        if (separator <= 0 || separator == flattened.lastIndex) return null
        val packageName = flattened.substring(0, separator)
        val rawClassName = flattened.substring(separator + 1)
        val className = if (rawClassName.startsWith('.')) packageName + rawClassName else rawClassName
        return "$packageName/$className"
    }
}
