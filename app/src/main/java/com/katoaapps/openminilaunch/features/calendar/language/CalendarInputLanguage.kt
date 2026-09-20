package com.katoaapps.openminilaunch.features.calendar.language

import android.content.Context
import android.content.res.Resources
import java.util.Locale

internal sealed interface CalendarInputLanguage {
    data object AppLanguage : CalendarInputLanguage
    data object SystemLanguage : CalendarInputLanguage
    data class Explicit(val languageTag: String) : CalendarInputLanguage

    val storageValue: String
        get() = when (this) {
            AppLanguage -> APP_LANGUAGE_VALUE
            SystemLanguage -> SYSTEM_LANGUAGE_VALUE
            is Explicit -> languageTag
        }

    companion object {
        const val APP_LANGUAGE_VALUE = "app"
        const val SYSTEM_LANGUAGE_VALUE = "system"

        fun fromStorage(value: String?): CalendarInputLanguage = when (value) {
            null, "", APP_LANGUAGE_VALUE -> AppLanguage
            SYSTEM_LANGUAGE_VALUE -> SystemLanguage
            else -> value
                .takeIf(::isUsableLanguageTag)
                ?.let(::Explicit)
                ?: AppLanguage
        }

        private fun isUsableLanguageTag(tag: String): Boolean {
            if (tag.length > 64) return false
            val locale = Locale.forLanguageTag(tag)
            return locale.language.isNotBlank() && locale.language != "und"
        }
    }
}

internal fun CalendarInputLanguage.effectiveLanguageTag(context: Context): String = when (this) {
    CalendarInputLanguage.AppLanguage -> context.resources.configuration.locales[0]
        ?.toLanguageTag()
        .orEmpty()
        .ifBlank { CalendarLanguageRegistry.REFERENCE_LANGUAGE_TAG }
    CalendarInputLanguage.SystemLanguage -> Resources.getSystem().configuration.locales[0]
        ?.toLanguageTag()
        .orEmpty()
        .ifBlank { CalendarLanguageRegistry.REFERENCE_LANGUAGE_TAG }
    is CalendarInputLanguage.Explicit -> languageTag
}
