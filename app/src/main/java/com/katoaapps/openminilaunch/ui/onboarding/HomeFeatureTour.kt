package com.katoaapps.openminilaunch.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.R
import kotlinx.coroutines.delay

internal data class HomeTourStep(
    val target: HomeTourTarget,
    val titleRes: Int,
    val descriptionRes: Int,
)

internal fun shouldShowHomeFeatureTour(
    onboardingComplete: Boolean,
    currentTourSeen: Boolean,
): Boolean = onboardingComplete && !currentTourSeen

internal fun homeFeatureTourSteps(): List<HomeTourStep> {
    val folderStep = HomeTourStep(
        target = HomeTourTarget.APP_GRID,
        titleRes = R.string.home_spotlight_folders_title,
        descriptionRes = R.string.home_spotlight_folders_description,
    )
    return listOf(
        folderStep,
        HomeTourStep(
            HomeTourTarget.MINK_SPACE,
            R.string.home_spotlight_minkspace_title,
            R.string.home_spotlight_minkspace_description,
        ),
        HomeTourStep(
            HomeTourTarget.MAGIC_BOX,
            R.string.home_spotlight_magic_title,
            R.string.home_spotlight_magic_description,
        ),
        HomeTourStep(
            HomeTourTarget.KEYBOARD_ACTION,
            R.string.home_spotlight_keyboard_title,
            R.string.home_spotlight_keyboard_description,
        ),
        HomeTourStep(
            HomeTourTarget.HEADER,
            R.string.home_spotlight_header_title,
            R.string.home_spotlight_header_description,
        ),
        HomeTourStep(
            HomeTourTarget.GESTURES,
            R.string.home_spotlight_gestures_title,
            R.string.home_spotlight_gestures_description,
        ),
    )
}

@Composable
internal fun HomeFeatureTour(
    registry: HomeTourTargetRegistry,
    onComplete: () -> Unit,
) {
    val steps = remember { homeFeatureTourSteps() }
    var currentIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentIndex]
    val targetBounds = registry[step.target]
    val gestureStep = step.target == HomeTourTarget.GESTURES

    // This release introduces folders and the rest of Home's direct-manipulation model to every
    // installation. Back navigates within the lesson instead of silently dismissing it.
    BackHandler { if (currentIndex > 0) currentIndex-- }

    // Layout targets are reported after Home's first placement. If an OEM layout genuinely omits
    // one, move on instead of leaving the user trapped behind an empty overlay.
    LaunchedEffect(currentIndex, targetBounds, gestureStep) {
        if (!gestureStep && targetBounds == null) {
            delay(TARGET_WAIT_MS)
            if (registry[step.target] == null) {
                if (currentIndex < steps.lastIndex) currentIndex++ else onComplete()
            }
        }
    }

    HomeSpotlightOverlay(
        step = step,
        index = currentIndex,
        count = steps.size,
        targetBounds = targetBounds,
        gestureStep = gestureStep,
        onPrevious = { if (currentIndex > 0) currentIndex-- },
        onNext = {
            if (currentIndex < steps.lastIndex) currentIndex++ else onComplete()
        },
    )
}

private const val TARGET_WAIT_MS = 900L
