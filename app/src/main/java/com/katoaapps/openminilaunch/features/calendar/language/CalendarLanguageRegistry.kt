package com.katoaapps.openminilaunch.features.calendar.language

import com.katoaapps.openminilaunch.features.calendar.language.en.AmericanEnglishCalendarLanguage
import com.katoaapps.openminilaunch.features.calendar.language.en.BritishEnglishCalendarLanguage
import com.katoaapps.openminilaunch.features.calendar.language.ar.ArabicCalendarLanguage
import com.katoaapps.openminilaunch.features.calendar.language.es.SpanishCalendarLanguage
import com.katoaapps.openminilaunch.features.calendar.language.zh.SimplifiedChineseCalendarLanguage
import java.util.Locale

internal object CalendarLanguageRegistry {
    const val REFERENCE_LANGUAGE_TAG = "en-US"

    private val modules = listOf(
        AmericanEnglishCalendarLanguage,
        BritishEnglishCalendarLanguage,
        SpanishCalendarLanguage,
        SimplifiedChineseCalendarLanguage,
        ArabicCalendarLanguage,
    )

    val supportedLanguageTags: List<String> = listOf(
        REFERENCE_LANGUAGE_TAG,
        "en-GB",
        "es",
        "zh-Hans",
        "ar",
    )

    fun find(languageTag: String): CalendarLanguageModule? {
        val requested = Locale.forLanguageTag(languageTag)
        val exactMatch = modules.firstOrNull { module ->
            module.languageTags.any { supportedTag ->
                Locale.forLanguageTag(supportedTag).toLanguageTag()
                    .equals(requested.toLanguageTag(), ignoreCase = true)
            }
        }
        if (exactMatch != null) return exactMatch

        val regionalFallback = modules.firstOrNull { module ->
            module.regionalFallbackLanguage?.equals(requested.language, ignoreCase = true) == true
        }
        if (regionalFallback != null) return regionalFallback

        // A region-specific request must not fall back to another region's grammar.
        // In particular, en-GB numeric dates must never be parsed as en-US dates.
        if (requested.country.isNotBlank() || requested.script.isNotBlank()) return null

        return modules.firstOrNull { module ->
            module.languageTags.any { supportedTag ->
                val supported = Locale.forLanguageTag(supportedTag)
                supported.country.isBlank() &&
                    supported.language.equals(requested.language, ignoreCase = true)
            }
        }
    }

    fun reference(): CalendarLanguageModule = AmericanEnglishCalendarLanguage
}
