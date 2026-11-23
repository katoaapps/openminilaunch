package com.katoaapps.openminilaunch.features.apps

import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.MAX_APP_FOLDER_TARGETS
import java.text.Collator
import java.util.Locale
import kotlin.math.ceil

/** The six purpose-based folders shared by fresh Mink and Classic Home layouts. */
internal enum class HomeFolderPurpose(@param:StringRes val titleRes: Int) {
    CONNECT(R.string.home_folder_connect),
    TASKS(R.string.home_folder_tasks),
    MEDIA(R.string.home_folder_media),
    COMPOSE(R.string.home_folder_compose),
    BROWSE(R.string.home_folder_browse),
    TOOLS(R.string.home_folder_tools),
}

internal data class FreshHomeFolderCandidate(
    val selectionKey: String,
    val packageName: String,
    val label: String,
    val applicationCategory: Int,
    val preferredPurpose: HomeFolderPurpose? = null,
    val capabilityPurposes: Set<HomeFolderPurpose> = emptySet(),
)

internal data class FreshHomeFolderSeed(
    val purpose: HomeFolderPurpose,
    val title: String,
    val selectionKeys: List<String>,
)

/**
 * Builds a one-time, purpose-based Home layout from launchable personal-profile apps.
 *
 * Android's declared app category supplies the broad grouping. Default handlers override that
 * category and sort first because those are the apps the owner has already chosen for a job.
 */
internal fun freshHomeFolderSeeds(
    candidates: List<FreshHomeFolderCandidate>,
    locale: Locale,
    title: (HomeFolderPurpose) -> String,
): List<FreshHomeFolderSeed> {
    val eligible = candidates
        .asSequence()
        .filter { it.selectionKey.isNotBlank() && it.packageName.isNotBlank() && it.label.isNotBlank() }
        .distinctBy(FreshHomeFolderCandidate::packageName)
        .toList()
    val initialFolderSize = if (eligible.isEmpty()) {
        0
    } else {
        ceil(eligible.size / HomeFolderPurpose.entries.size.toDouble())
            .toInt()
            .coerceIn(1, minOf(16, MAX_APP_FOLDER_TARGETS))
    }
    val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }

    return HomeFolderPurpose.entries.map { purpose ->
        val selected = eligible
            .asSequence()
            .filter { candidate -> purposeForFreshHomeCandidate(candidate) == purpose }
            .sortedWith(
                compareByDescending<FreshHomeFolderCandidate> { it.preferredPurpose == purpose }
                    .thenComparator { left, right -> collator.compare(left.label, right.label) },
            )
            .take(initialFolderSize)
            .map(FreshHomeFolderCandidate::selectionKey)
            .toList()
        FreshHomeFolderSeed(
            purpose = purpose,
            title = title(purpose),
            selectionKeys = selected,
        )
    }
}

/**
 * Chooses one initial folder for an app. Strong user defaults always win, declared Android
 * categories remain authoritative, and exported capabilities only refine broad productivity or
 * fallback categories. This keeps the generated folders useful without duplicating apps.
 */
internal fun purposeForFreshHomeCandidate(
    candidate: FreshHomeFolderCandidate,
): HomeFolderPurpose? {
    candidate.preferredPurpose?.let { return it }
    val categoryPurpose = purposeForApplicationCategory(candidate.applicationCategory)
    if (categoryPurpose == null) return null
    if (
        candidate.applicationCategory == ApplicationInfo.CATEGORY_PRODUCTIVITY ||
        categoryPurpose == HomeFolderPurpose.TOOLS
    ) {
        capabilityPurpose(candidate.capabilityPurposes)?.let { return it }
    }
    return categoryPurpose
}

private fun capabilityPurpose(
    purposes: Set<HomeFolderPurpose>,
): HomeFolderPurpose? = CAPABILITY_PURPOSE_PRIORITY.firstOrNull(purposes::contains)

private val CAPABILITY_PURPOSE_PRIORITY = listOf(
    HomeFolderPurpose.CONNECT,
    HomeFolderPurpose.COMPOSE,
    HomeFolderPurpose.BROWSE,
    HomeFolderPurpose.MEDIA,
)

internal fun purposeForApplicationCategory(category: Int): HomeFolderPurpose? = when (category) {
    ApplicationInfo.CATEGORY_SOCIAL -> HomeFolderPurpose.CONNECT
    ApplicationInfo.CATEGORY_PRODUCTIVITY -> HomeFolderPurpose.TASKS
    ApplicationInfo.CATEGORY_AUDIO,
    ApplicationInfo.CATEGORY_VIDEO,
    ApplicationInfo.CATEGORY_NEWS,
    -> HomeFolderPurpose.MEDIA
    ApplicationInfo.CATEGORY_IMAGE -> HomeFolderPurpose.COMPOSE
    ApplicationInfo.CATEGORY_MAPS -> HomeFolderPurpose.BROWSE
    // The final Home slot stays unassigned. Games remain discoverable through search, See All,
    // and manual folder selection instead of creating a dedicated Fun folder on first launch.
    ApplicationInfo.CATEGORY_GAME -> null
    else -> HomeFolderPurpose.TOOLS
}
