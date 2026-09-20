package com.katoaapps.openminilaunch.features.calendar

import com.katoaapps.openminilaunch.features.calendar.engine.CalendarPhraseEngine
import com.katoaapps.openminilaunch.features.calendar.language.CalendarLanguageRegistry
import com.katoaapps.openminilaunch.features.calendar.model.CalendarParseRequest
import java.time.ZonedDateTime

internal typealias CalendarDraft = com.katoaapps.openminilaunch.features.calendar.model.CalendarDraft

/** Compatibility facade for callers that only need the best safe draft. */
internal fun parseCalendarPhrase(
    input: String,
    defaultTitle: String,
    now: ZonedDateTime = ZonedDateTime.now(),
): CalendarDraft = CalendarPhraseEngine.parse(
    CalendarParseRequest(
        text = input,
        defaultTitle = defaultTitle,
        languageTag = CalendarLanguageRegistry.REFERENCE_LANGUAGE_TAG,
        now = now,
    ),
).draft
