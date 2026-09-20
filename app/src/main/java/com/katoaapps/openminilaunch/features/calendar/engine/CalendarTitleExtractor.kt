package com.katoaapps.openminilaunch.features.calendar.engine

import com.katoaapps.openminilaunch.features.calendar.model.SourceSpan

internal object CalendarTitleExtractor {
    fun extract(text: String, recognizedSpans: List<SourceSpan>, defaultTitle: String): String {
        var title = text
        recognizedSpans
            .distinct()
            .sortedByDescending(SourceSpan::startInclusive)
            .forEach { span -> title = title.removeRange(span.startInclusive, span.endExclusive) }
        return title
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '-', '.', ':')
            .ifBlank { defaultTitle }
    }
}
