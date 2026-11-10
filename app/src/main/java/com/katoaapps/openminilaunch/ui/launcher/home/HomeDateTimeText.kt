package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class HomeDateTimeParts(
    val date: String,
    val time: String,
)

@Composable
internal fun rememberHomeDateTimeParts(
    datePattern: String,
    showDate: Boolean,
    showClock: Boolean,
    use24HourClock: Boolean,
): HomeDateTimeParts {
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilNextMinute())
            currentDateTime = LocalDateTime.now()
        }
    }
    return formatHomeDateTimeParts(
        dateTime = currentDateTime,
        datePattern = datePattern,
        showDate = showDate,
        showClock = showClock,
        use24HourClock = use24HourClock,
    )
}

internal fun formatHomeDateTimeParts(
    dateTime: LocalDateTime,
    datePattern: String,
    showDate: Boolean,
    showClock: Boolean,
    use24HourClock: Boolean,
    locale: Locale = Locale.getDefault(),
): HomeDateTimeParts {
    val date = if (showDate) {
        dateTime.format(DateTimeFormatter.ofPattern(datePattern, locale)).uppercase(locale)
    } else {
        ""
    }
    val time = if (showClock) {
        val timePattern = if (use24HourClock) "HH:mm" else "h:mm a"
        dateTime.format(DateTimeFormatter.ofPattern(timePattern, locale))
    } else {
        ""
    }
    return HomeDateTimeParts(date = date, time = time)
}

private fun millisUntilNextMinute(nowMillis: Long = System.currentTimeMillis()): Long {
    return 60_000L - (nowMillis % 60_000L)
}
