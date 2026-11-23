package com.katoaapps.openminilaunch.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.magic.hasUsableHardwareKeyboard
import com.katoaapps.openminilaunch.model.HomeLayoutStyle
import kotlinx.coroutines.delay

internal data class HomeTourStep(
    val target: HomeTourTarget,
    val titleRes: Int,
    val descriptionRes: Int,
    val classicGestureCues: Boolean = false,
)

internal fun shouldShowHomeFeatureTour(
    onboardingComplete: Boolean,
    currentTourSeen: Boolean,
): Boolean = onboardingComplete && !currentTourSeen

internal fun homeFeatureTourSteps(layoutStyle: HomeLayoutStyle): List<HomeTourStep> =
    if (layoutStyle == HomeLayoutStyle.CLASSIC) classicHomeFeatureTourSteps()
    else minkHomeFeatureTourSteps()

private fun minkHomeFeatureTourSteps(): List<HomeTourStep> = listOf(
    HomeTourStep(
        HomeTourTarget.APP_GRID,
        R.string.home_spotlight_folders_title,
        R.string.home_spotlight_folders_description,
    ),
    HomeTourStep(
        HomeTourTarget.MINK_SPACE,
        R.string.home_spotlight_minkspace_title,
        R.string.home_spotlight_minkspace_description,
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
    HomeTourStep(
        HomeTourTarget.KEYBOARD_ACTION,
        R.string.home_spotlight_keyboard_title,
        R.string.home_spotlight_keyboard_description,
    ),
    HomeTourStep(
        HomeTourTarget.MAGIC_BOX,
        R.string.home_spotlight_magic_title,
        R.string.home_spotlight_magic_description,
    ),
)

private fun classicHomeFeatureTourSteps(): List<HomeTourStep> = listOf(
    HomeTourStep(
        HomeTourTarget.CLASSIC_PAGES,
        R.string.home_spotlight_classic_pages_title,
        R.string.home_spotlight_classic_pages_description,
    ),
    HomeTourStep(
        HomeTourTarget.CLASSIC_FOLDER_TITLE,
        R.string.home_spotlight_classic_folder_title_title,
        R.string.home_spotlight_classic_folder_title_description,
    ),
    HomeTourStep(
        HomeTourTarget.CLASSIC_FOLDER_HOME,
        R.string.home_spotlight_classic_folder_home_title,
        R.string.home_spotlight_classic_folder_home_description,
    ),
    HomeTourStep(
        HomeTourTarget.CLASSIC_FOLDER_EDIT,
        R.string.home_spotlight_classic_folder_edit_title,
        R.string.home_spotlight_classic_folder_edit_description,
    ),
    HomeTourStep(
        HomeTourTarget.CLASSIC_RAIL,
        R.string.home_spotlight_classic_rail_title,
        R.string.home_spotlight_classic_rail_description,
    ),
    HomeTourStep(
        HomeTourTarget.HEADER,
        R.string.home_spotlight_header_title,
        R.string.home_spotlight_header_description,
    ),
    HomeTourStep(
        HomeTourTarget.GESTURES,
        R.string.home_spotlight_classic_gestures_title,
        R.string.home_spotlight_classic_gestures_description,
        classicGestureCues = true,
    ),
    HomeTourStep(
        HomeTourTarget.KEYBOARD_ACTION,
        R.string.home_spotlight_keyboard_title,
        R.string.home_spotlight_keyboard_description,
    ),
    HomeTourStep(
        HomeTourTarget.MAGIC_BOX,
        R.string.home_spotlight_magic_title,
        R.string.home_spotlight_magic_description,
    ),
)

@Composable
internal fun HomeFeatureTour(
    registry: HomeTourTargetRegistry,
    layoutStyle: HomeLayoutStyle,
    onComplete: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val steps = remember(layoutStyle) { homeFeatureTourSteps(layoutStyle) }
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
        clearClassicSlabRail = layoutStyle == HomeLayoutStyle.CLASSIC &&
            !hasUsableHardwareKeyboard(
                configuration.keyboard,
                configuration.hardKeyboardHidden,
            ),
        onPrevious = { if (currentIndex > 0) currentIndex-- },
        onNext = {
            if (currentIndex < steps.lastIndex) currentIndex++ else onComplete()
        },
    )
}

private const val TARGET_WAIT_MS = 900L
