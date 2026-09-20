package com.katoaapps.openminilaunch.features.calendar.engine

import com.katoaapps.openminilaunch.features.calendar.language.CalendarLanguageRegistry
import com.katoaapps.openminilaunch.features.calendar.model.*

internal object CalendarPhraseEngine {
    fun parse(request: CalendarParseRequest): CalendarParseResult {
        val text = request.text.trim().replace(Regex("\\s+"), " ")
        val emptyDraft = CalendarDraft(title = request.defaultTitle)
        if (text.isBlank()) return CalendarParseResult.Invalid(emptyDraft, CalendarParseIssue.EMPTY_INPUT)

        val requestedModule = CalendarLanguageRegistry.find(request.languageTag)
        val parsed = (requestedModule ?: CalendarLanguageRegistry.reference()).parse(text)
        fun draftRemoving(spans: List<SourceSpan>) = CalendarDraft(
            title = CalendarTitleExtractor.extract(text, spans, request.defaultTitle),
        )
        val partialDraft = draftRemoving(parsed.recognizedSpans)

        if (requestedModule == null) {
            return CalendarParseResult.NeedsReview(
                CalendarDraft(title = text),
                CalendarParseIssue.UNSUPPORTED_LANGUAGE,
            )
        }

        parsed.issue?.let { issue ->
            val acceptedSpans = when (issue) {
                CalendarParseIssue.INVALID_DATE -> parsed.times.map { it.span } + parsed.durations.map { it.span }
                CalendarParseIssue.INVALID_TIME -> parsed.dates.map { it.span } + parsed.durations.map { it.span }
                else -> emptyList()
            }
            return CalendarParseResult.NeedsReview(draftRemoving(acceptedSpans), issue)
        }
        if (parsed.dates.size > 1 || parsed.times.size > 1 || parsed.durations.size > 1) {
            val nonConflictingSpans = buildList {
                if (parsed.dates.size == 1) add(parsed.dates.single().span)
                if (parsed.times.size == 1) add(parsed.times.single().span)
                if (parsed.durations.size == 1) add(parsed.durations.single().span)
            }
            return CalendarParseResult.NeedsReview(
                draftRemoving(nonConflictingSpans),
                CalendarParseIssue.CONFLICTING_TEMPORAL_PHRASES,
            )
        }
        val unrecognizedCues = parsed.temporalCues.filter { cue ->
            parsed.recognizedSpans.none { recognized -> recognized.contains(cue) }
        }
        if (unrecognizedCues.isNotEmpty()) {
            val independentlyRecognizedSpans = parsed.recognizedSpans.filter { recognized ->
                unrecognizedCues.none { cue -> cue.overlaps(recognized) }
            }
            return CalendarParseResult.NeedsReview(
                draftRemoving(independentlyRecognizedSpans),
                CalendarParseIssue.UNSUPPORTED_TEMPORAL_PHRASE,
            )
        }
        val date = parsed.dates.singleOrNull()?.value
        val time = parsed.times.singleOrNull()?.value
        val duration = parsed.durations.singleOrNull()?.value
        if (date == null && time == null) {
            return if (duration == null && parsed.temporalCues.isEmpty()) {
                CalendarParseResult.NoTemporalPhrase(partialDraft)
            } else {
                CalendarParseResult.NeedsReview(
                    partialDraft,
                    CalendarParseIssue.UNSUPPORTED_TEMPORAL_PHRASE,
                )
            }
        }

        val resolution = CalendarTemporalResolver.resolve(
            dateExpression = date,
            timeExpression = time,
            durationExpression = duration,
            now = request.now,
            zoneId = request.zoneId,
            defaultDuration = request.defaultDuration,
        )
        resolution.issue?.let { issue ->
            val reviewDraft = if (issue == CalendarParseIssue.INVALID_DATE) {
                draftRemoving(parsed.times.map { it.span } + parsed.durations.map { it.span })
            } else {
                partialDraft
            }
            return CalendarParseResult.NeedsReview(reviewDraft, issue)
        }
        return CalendarParseResult.Success(
            partialDraft.copy(
                startMillis = resolution.start?.toInstant()?.toEpochMilli(),
                endMillis = resolution.end?.toInstant()?.toEpochMilli(),
                allDay = resolution.allDay,
                warnings = resolution.warnings,
            ),
        )
    }
}
