package com.katoaapps.minkboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

/** Keeps overdue presentation correct when MinkBoard remains open across local midnight. */
@Composable
internal fun rememberCurrentDate(zoneId: ZoneId = ZoneId.systemDefault()): State<LocalDate> =
    produceState(initialValue = LocalDate.now(zoneId), zoneId) {
        while (true) {
            val now = ZonedDateTime.now(zoneId)
            value = now.toLocalDate()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
        }
    }
