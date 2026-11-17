package com.katoaapps.minkboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.katoaapps.minkboard.core.cardsDueOn
import com.katoaapps.minkboard.core.dueCardCount
import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoardCalendarScreen(
    board: BoardSnapshot,
    onBack: () -> Unit,
    onOpenCard: (BoardCard) -> Unit,
) {
    val locale = Locale.getDefault()
    val today by rememberCurrentDate()
    var selectedDateValue by rememberSaveable(board.board.id.value) {
        mutableStateOf(today.toString())
    }
    var displayedMonthValue by rememberSaveable(board.board.id.value) {
        mutableStateOf(YearMonth.from(today).toString())
    }
    val selectedDate = runCatching { LocalDate.parse(selectedDateValue) }.getOrNull() ?: today
    val displayedMonth = runCatching { YearMonth.parse(displayedMonthValue) }.getOrNull()
        ?: YearMonth.from(today)
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val visibleDates = calendarDates(displayedMonth, firstDayOfWeek)
    val selectedCards = board.cardsDueOn(selectedDate)

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_board),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val fontScale = LocalDensity.current.fontScale
        val dayHeight = 40.dp + (16.dp * (fontScale - 1f).coerceIn(0f, 1f))
        val weekdayFontSize = (12f * minOf(fontScale, 1.4f) / fontScale).sp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "board-name") {
                Text(
                    text = board.board.name,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item(key = "month-navigation") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = {
                        displayedMonthValue = displayedMonth.minusMonths(1).toString()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.NavigateBefore,
                            contentDescription = stringResource(R.string.previous_month),
                        )
                    }
                    Text(
                        text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = {
                        displayedMonthValue = displayedMonth.plusMonths(1).toString()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.NavigateNext,
                            contentDescription = stringResource(R.string.next_month),
                        )
                    }
                }
            }

            item(key = "weekdays") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    orderedWeekdays(firstDayOfWeek).forEach { day ->
                        Text(
                            text = day.getDisplayName(TextStyle.NARROW, locale),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = weekdayFontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(key = "calendar-grid") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    visibleDates.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                CalendarDay(
                                    date = date,
                                    inDisplayedMonth = YearMonth.from(date) == displayedMonth,
                                    isSelected = date == selectedDate,
                                    isToday = date == today,
                                    dueCount = board.dueCardCount(date),
                                    height = dayHeight,
                                    onClick = {
                                        selectedDateValue = date.toString()
                                        displayedMonthValue = YearMonth.from(date).toString()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "selected-date") {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (selectedCards.isEmpty()) {
                item(key = "no-cards-due") {
                    Text(
                        text = stringResource(R.string.no_cards_due),
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(selectedCards, key = { card -> "due-card-${card.id.value}" }) { card ->
                    val status = board.statuses.firstOrNull { it.id == card.statusId }
                    Card(
                        onClick = { onOpenCard(card) },
                        modifier = Modifier.fillMaxWidth(),
                        border = status?.let { BorderStroke(1.dp, androidx.compose.ui.graphics.Color(it.colorArgb)) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            status?.let {
                                Text(
                                    text = it.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item(key = "calendar-bottom-spacing") { Box(Modifier.height(6.dp)) }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    inDisplayedMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    dueCount: Int,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    val dayFontSize = (14f * minOf(fontScale, 1.4f) / fontScale).sp
    val markerFontSize = (11f * minOf(fontScale, 1.3f) / fontScale).sp
    val dueDescription = if (dueCount > 0) {
        pluralStringResource(R.plurals.cards_due_count, dueCount, dueCount)
    } else {
        null
    }
    val dateDescription = buildString {
        append(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")))
        dueDescription?.let { append(", ").append(it) }
    }
    Surface(
        modifier = modifier
            .height(height)
            .padding(2.dp)
            .semantics { contentDescription = dateDescription }
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = when {
                    !inDisplayedMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = dayFontSize,
            )
            if (dueCount > 0) {
                Text(
                    text = if (dueCount > 9) "9+" else dueCount.toString(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = markerFontSize,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun calendarDates(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val firstOfMonth = month.atDay(1)
    val offset = (7 + firstOfMonth.dayOfWeek.value - firstDayOfWeek.value) % 7
    val firstVisibleDate = firstOfMonth.minusDays(offset.toLong())
    return List(42) { index -> firstVisibleDate.plusDays(index.toLong()) }
}

private fun orderedWeekdays(firstDay: DayOfWeek): List<DayOfWeek> =
    List(7) { offset -> DayOfWeek.of(((firstDay.value - 1 + offset) % 7) + 1) }
