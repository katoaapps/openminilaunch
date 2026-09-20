package com.katoaapps.openminilaunch.features.calendar.model

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

internal data class CalendarParseRequest(
    val text: String,
    val defaultTitle: String,
    val languageTag: String,
    val now: ZonedDateTime = ZonedDateTime.now(),
    val zoneId: ZoneId = now.zone,
    val defaultDuration: Duration = Duration.ofHours(1),
)
