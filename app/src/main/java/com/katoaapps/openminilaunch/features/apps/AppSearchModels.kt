package com.katoaapps.openminilaunch.features.apps

import com.katoaapps.openminilaunch.model.LauncherTarget

internal enum class AppSearchTier {
    EXACT,
    PREFIX,
    WORD_PREFIX,
    TOKEN_PREFIX,
    INITIALISM,
    SUBSEQUENCE,
    TYPO,
}

internal data class AppSearchMatch(
    val target: LauncherTarget,
    val score: Int,
    val tier: AppSearchTier,
    val matchedText: String,
)
