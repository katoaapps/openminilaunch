package com.katoaapps.openminilaunch.ui.magic

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.calendar.model.CalendarDraft
import com.katoaapps.openminilaunch.features.calendar.model.CalendarParseIssue
import com.katoaapps.openminilaunch.features.calendar.model.CalendarParseResult
import com.katoaapps.openminilaunch.features.calendar.model.CalendarParseWarning
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CalendarParsePreview(
    result: CalendarParseResult,
    languageTag: String,
    use24HourClock: Boolean,
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val needsReview = result is CalendarParseResult.NeedsReview || result is CalendarParseResult.Invalid
    val container = if (needsReview) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (needsReview) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        modifier = modifier.fillMaxWidth().then(
            if (needsReview) Modifier.clickable(onClick = onReview) else Modifier,
        ),
        shape = RoundedCornerShape(Dimens.dp16),
        color = container,
        contentColor = content,
    ) {
        Row(
            Modifier.padding(horizontal = Dimens.dp14, vertical = Dimens.dp11),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp10),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                if (needsReview) Icons.Default.WarningAmber else Icons.Default.Event,
                null,
                Modifier.size(Dimens.dp20),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.dp2)) {
                Text(result.draft.title, fontWeight = FontWeight.SemiBold)
                when (result) {
                    is CalendarParseResult.Success -> {
                        Text(calendarDraftTimeLabel(result.draft, use24HourClock), fontSize = Dimens.sp12)
                        result.draft.warnings.forEach { warning ->
                            Text(calendarWarningLabel(warning), fontSize = Dimens.sp11)
                        }
                    }
                    is CalendarParseResult.NoTemporalPhrase -> Text(
                        stringResource(R.string.calendar_preview_no_time),
                        fontSize = Dimens.sp12,
                    )
                    is CalendarParseResult.NeedsReview -> {
                        Text(stringResource(R.string.calendar_preview_review), fontSize = Dimens.sp12)
                        Text(calendarIssueLabel(result.issue), fontSize = Dimens.sp11)
                    }
                    is CalendarParseResult.Invalid -> Text(
                        calendarIssueLabel(result.issue),
                        fontSize = Dimens.sp12,
                    )
                }
                val appLocale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
                val parserLocale = Locale.forLanguageTag(languageTag)
                if (
                    parserLocale.language.isNotBlank() &&
                    !parserLocale.toLanguageTag().equals(appLocale.toLanguageTag(), ignoreCase = true)
                ) {
                    Text(
                        parserLocale.getDisplayName(parserLocale),
                        color = content.copy(alpha = .72f),
                        fontSize = Dimens.sp10,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CalendarReviewDialog(
    result: CalendarParseResult,
    onContinueWithoutTime: (CalendarDraft) -> Unit,
    onEdit: () -> Unit,
) {
    val issue = when (result) {
        is CalendarParseResult.NeedsReview -> result.issue
        is CalendarParseResult.Invalid -> result.issue
        else -> return
    }
    AlertDialog(
        onDismissRequest = onEdit,
        title = { Text(stringResource(R.string.calendar_review_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                Text(result.draft.title, fontWeight = FontWeight.SemiBold)
                Text(calendarIssueLabel(issue), color = Muted)
            }
        },
        confirmButton = {
            TextButton(onClick = { onContinueWithoutTime(result.draft) }) {
                Text(stringResource(R.string.calendar_review_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onEdit) { Text(stringResource(R.string.calendar_review_edit)) }
        },
    )
}

@Composable
private fun calendarDraftTimeLabel(draft: CalendarDraft, use24HourClock: Boolean): String {
    val startMillis = draft.startMillis ?: return stringResource(R.string.calendar_preview_no_time)
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val datePattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd")
    val timePattern = DateFormat.getBestDateTimePattern(locale, if (use24HourClock) "Hm" else "hm")
    val date = SimpleDateFormat(datePattern, locale).format(Date(startMillis))
    if (draft.allDay) return stringResource(R.string.calendar_preview_date_all_day, date)
    val start = SimpleDateFormat(timePattern, locale).format(Date(startMillis))
    val end = draft.endMillis?.let { SimpleDateFormat(timePattern, locale).format(Date(it)) }
    return if (end == null) {
        stringResource(R.string.calendar_preview_date_time, date, start)
    } else {
        stringResource(R.string.calendar_preview_date_time_range, date, start, end)
    }
}

@Composable
private fun calendarIssueLabel(issue: CalendarParseIssue): String = stringResource(
    when (issue) {
        CalendarParseIssue.EMPTY_INPUT -> R.string.calendar_issue_empty
        CalendarParseIssue.UNSUPPORTED_LANGUAGE -> R.string.calendar_issue_unsupported_language
        CalendarParseIssue.UNSUPPORTED_TEMPORAL_PHRASE -> R.string.calendar_issue_unsupported_phrase
        CalendarParseIssue.AMBIGUOUS_DATE -> R.string.calendar_issue_ambiguous_date
        CalendarParseIssue.INVALID_DATE -> R.string.calendar_issue_invalid_date
        CalendarParseIssue.INVALID_TIME -> R.string.calendar_issue_invalid_time
        CalendarParseIssue.CONFLICTING_TEMPORAL_PHRASES -> R.string.calendar_issue_conflict
        CalendarParseIssue.DST_GAP -> R.string.calendar_issue_dst_gap
    },
)

@Composable
private fun calendarWarningLabel(warning: CalendarParseWarning): String = stringResource(
    when (warning) {
        CalendarParseWarning.PAST_EVENT -> R.string.calendar_warning_past
        CalendarParseWarning.DST_OVERLAP -> R.string.calendar_warning_dst_overlap
    },
)
