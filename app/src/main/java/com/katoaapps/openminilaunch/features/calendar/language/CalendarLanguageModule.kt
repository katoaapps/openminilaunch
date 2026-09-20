package com.katoaapps.openminilaunch.features.calendar.language

import com.katoaapps.openminilaunch.features.calendar.model.CalendarLanguageParse

internal interface CalendarLanguageModule {
    val languageTags: Set<String>
    val regionalFallbackLanguage: String? get() = null

    fun parse(text: String): CalendarLanguageParse
}
