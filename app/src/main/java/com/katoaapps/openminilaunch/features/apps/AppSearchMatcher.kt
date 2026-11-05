package com.katoaapps.openminilaunch.features.apps

import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import java.text.Collator
import java.util.Locale
import kotlin.math.min

internal class AppSearchMatcher(
    private val packageLabel: (String) -> String,
    locale: Locale = Locale.getDefault(),
) {
    private val labelCollator = Collator.getInstance(locale).apply {
        strength = Collator.PRIMARY
    }

    fun match(target: LauncherTarget, query: String): AppSearchMatch? =
        match(
            target = target,
            query = normalizeAppSearchText(query),
            publisherLabel = if (target is LauncherAppTarget) "" else packageLabel(target.packageName),
        )

    fun rank(targets: List<LauncherTarget>, query: String): List<AppSearchMatch> {
        val normalizedQuery = normalizeAppSearchText(query)
        if (normalizedQuery.value.isBlank()) return emptyList()
        val packageLabels = mutableMapOf<String, String>()
        return targets.mapNotNull { target ->
            val publisher = if (target is LauncherAppTarget) {
                ""
            } else {
                packageLabels.getOrPut(target.packageName) {
                    packageLabel(target.packageName)
                }
            }
            match(target, normalizedQuery, publisher)
        }.sortedWith(::compareMatches)
    }

    private fun match(
        target: LauncherTarget,
        query: NormalizedAppSearchText,
        publisherLabel: String,
    ): AppSearchMatch? {
        if (query.value.isBlank()) return null
        val candidates = buildList {
            add(SearchCandidate(target.label, sourcePenalty = 0))
            if (target !is LauncherAppTarget && publisherLabel.isNotBlank()) {
                add(SearchCandidate(publisherLabel, sourcePenalty = PUBLISHER_PENALTY))
                add(
                    SearchCandidate(
                        "${target.label} $publisherLabel",
                        sourcePenalty = COMBINED_LABEL_PENALTY,
                    ),
                )
            }
        }.distinctBy { normalizeAppSearchText(it.text).value }
        val best = candidates.mapNotNull { candidate ->
            evaluateCandidate(candidate, query)
        }.minWithOrNull(
            compareBy<CandidateMatch>(CandidateMatch::tier)
                .thenByDescending(CandidateMatch::score),
        ) ?: return null
        return AppSearchMatch(
            target = target,
            score = best.score,
            tier = best.tier,
            matchedText = best.matchedText,
        )
    }

    private fun evaluateCandidate(
        candidate: SearchCandidate,
        query: NormalizedAppSearchText,
    ): CandidateMatch? {
        val text = normalizeAppSearchText(candidate.text)
        if (text.value.isBlank()) return null
        val tierAndQuality = when {
            text.value == query.value -> AppSearchTier.EXACT to QUALITY_MAX
            text.value.startsWith(query.value) -> AppSearchTier.PREFIX to
                (QUALITY_MAX - (text.value.length - query.value.length).coerceAtMost(QUALITY_MAX))
            query.tokens.size == 1 -> wordPrefixQuality(text, query)
            else -> null
        } ?: tokenPrefixQuality(text, query)
            ?: initialismQuality(text, query)
            ?: subsequenceQuality(text, query)
            ?: typoQuality(text, query)
            ?: return null
        val (tier, quality) = tierAndQuality
        return CandidateMatch(
            tier = tier,
            score = tierBaseScore(tier) + quality - candidate.sourcePenalty,
            matchedText = candidate.text,
        )
    }

    private fun compareMatches(left: AppSearchMatch, right: AppSearchMatch): Int {
        val tierComparison = left.tier.compareTo(right.tier)
        if (tierComparison != 0) return tierComparison
        val scoreComparison = right.score.compareTo(left.score)
        if (scoreComparison != 0) return scoreComparison
        val labelComparison = labelCollator.compare(left.target.label, right.target.label)
        if (labelComparison != 0) return labelComparison
        val profileComparison = left.target.isWorkProfile.compareTo(right.target.isWorkProfile)
        if (profileComparison != 0) return profileComparison
        return left.target.selectionKey.compareTo(right.target.selectionKey)
    }
}

private data class SearchCandidate(
    val text: String,
    val sourcePenalty: Int,
)

private data class CandidateMatch(
    val tier: AppSearchTier,
    val score: Int,
    val matchedText: String,
)

private fun wordPrefixQuality(
    text: NormalizedAppSearchText,
    query: NormalizedAppSearchText,
): Pair<AppSearchTier, Int>? {
    val queryToken = query.tokens.singleOrNull() ?: return null
    val index = text.tokens.indexOfFirst { it.startsWith(queryToken) }
    if (index < 0) return null
    val excess = text.tokens[index].length - queryToken.length
    return AppSearchTier.WORD_PREFIX to
        (QUALITY_MAX - index * 8 - excess.coerceAtMost(QUALITY_MAX / 2))
}

private fun tokenPrefixQuality(
    text: NormalizedAppSearchText,
    query: NormalizedAppSearchText,
): Pair<AppSearchTier, Int>? {
    if (query.tokens.size < 2 || query.tokens.size > text.tokens.size) return null
    var textIndex = 0
    var skippedTokens = 0
    var excessCharacters = 0
    query.tokens.forEach { queryToken ->
        val matchIndex = (textIndex until text.tokens.size).firstOrNull { index ->
            text.tokens[index].startsWith(queryToken)
        } ?: return null
        skippedTokens += matchIndex - textIndex
        excessCharacters += text.tokens[matchIndex].length - queryToken.length
        textIndex = matchIndex + 1
    }
    return AppSearchTier.TOKEN_PREFIX to (
        QUALITY_MAX - skippedTokens * 10 - excessCharacters.coerceAtMost(QUALITY_MAX / 2)
        )
}

private fun initialismQuality(
    text: NormalizedAppSearchText,
    query: NormalizedAppSearchText,
): Pair<AppSearchTier, Int>? {
    if (query.compact.length < 2 || !text.initials.startsWith(query.compact)) return null
    return AppSearchTier.INITIALISM to
        (QUALITY_MAX - (text.initials.length - query.compact.length).coerceAtMost(QUALITY_MAX))
}

private fun subsequenceQuality(
    text: NormalizedAppSearchText,
    query: NormalizedAppSearchText,
): Pair<AppSearchTier, Int>? {
    if (
        query.compact.length < MIN_SUBSEQUENCE_QUERY_LENGTH ||
        text.compact.length - query.compact.length < MIN_SUBSEQUENCE_ABBREVIATION
    ) return null
    val typoDistance = maximumTypoDistance(query.compact.length)
    if (
        query.tokens.size == 1 &&
        typoDistance > 0 &&
        text.tokens.any { boundedEditDistance(query.compact, it, typoDistance) <= typoDistance }
    ) return null
    var candidateIndex = 0
    var firstMatch = -1
    var lastMatch = -1
    query.compact.forEach { character ->
        val matchIndex = text.compact.indexOf(character, startIndex = candidateIndex)
        if (matchIndex < 0) return null
        if (firstMatch < 0) firstMatch = matchIndex
        lastMatch = matchIndex
        candidateIndex = matchIndex + 1
    }
    val span = lastMatch - firstMatch + 1
    val skipped = span - query.compact.length
    return AppSearchTier.SUBSEQUENCE to (
        QUALITY_MAX - firstMatch * 5 - skipped * 4 -
            (text.compact.length - query.compact.length).coerceAtMost(QUALITY_MAX / 2)
        )
}

private fun typoQuality(
    text: NormalizedAppSearchText,
    query: NormalizedAppSearchText,
): Pair<AppSearchTier, Int>? {
    val maximumDistance = maximumTypoDistance(query.compact.length)
    if (maximumDistance == 0) return null
    val candidates = if (query.tokens.size == 1) text.tokens else listOf(text.compact)
    val best = candidates.mapIndexedNotNull { index, candidate ->
        val distance = boundedEditDistance(query.compact, candidate, maximumDistance)
        if (distance <= maximumDistance) distance to index else null
    }.minWithOrNull(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second }) ?: return null
    return AppSearchTier.TYPO to (QUALITY_MAX - best.first * 25 - best.second * 5)
}

private fun maximumTypoDistance(queryLength: Int): Int = when (queryLength) {
    in 0..2 -> 0
    in 3..4 -> 1
    else -> 2
}

private fun boundedEditDistance(left: String, right: String, limit: Int): Int {
    if (kotlin.math.abs(left.length - right.length) > limit) return limit + 1
    var previous = IntArray(right.length + 1) { it }
    for (leftIndex in left.indices) {
        val current = IntArray(right.length + 1)
        current[0] = leftIndex + 1
        var rowMinimum = current[0]
        for (rightIndex in right.indices) {
            current[rightIndex + 1] = min(
                min(current[rightIndex] + 1, previous[rightIndex + 1] + 1),
                previous[rightIndex] + if (left[leftIndex] == right[rightIndex]) 0 else 1,
            )
            rowMinimum = min(rowMinimum, current[rightIndex + 1])
        }
        if (rowMinimum > limit) return limit + 1
        previous = current
    }
    return previous[right.length]
}

private fun tierBaseScore(tier: AppSearchTier): Int =
    (AppSearchTier.entries.size - tier.ordinal) * 1_000

private const val QUALITY_MAX = 100
private const val PUBLISHER_PENALTY = 8
private const val COMBINED_LABEL_PENALTY = 16
private const val MIN_SUBSEQUENCE_ABBREVIATION = 2
private const val MIN_SUBSEQUENCE_QUERY_LENGTH = 4
