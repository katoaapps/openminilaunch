package com.katoaapps.openminilaunch.ui.launcher

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

@Composable
internal fun rememberHomeDateTimeText(
    datePattern: String,
    showClock: Boolean,
    use24HourClock: Boolean,
): String {
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilNextMinute())
            currentDateTime = LocalDateTime.now()
        }
    }
    return formatHomeDateTime(
        dateTime = currentDateTime,
        datePattern = datePattern,
        showClock = showClock,
        use24HourClock = use24HourClock,
    )
}

internal fun formatHomeDateTime(
    dateTime: LocalDateTime,
    datePattern: String,
    showClock: Boolean,
    use24HourClock: Boolean,
    locale: Locale = Locale.getDefault(),
): String {
    val date = dateTime.format(DateTimeFormatter.ofPattern(datePattern, locale)).uppercase(locale)
    if (!showClock) return date

    val timePattern = if (use24HourClock) "HH:mm" else "h:mm a"
    val time = dateTime.format(DateTimeFormatter.ofPattern(timePattern, locale))
    return "$date · $time"
}

private fun millisUntilNextMinute(nowMillis: Long = System.currentTimeMillis()): Long {
    return 60_000L - (nowMillis % 60_000L)
}
