package com.katoaapps.openminilaunch.ui.minkpad.help

import com.katoaapps.minkpad.core.R as MinkPadR

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.minkpad.core.input.MinkPadGestureAction
import com.katoaapps.minkpad.core.input.MinkPadHostPlatform
import com.katoaapps.openminilaunch.ui.minkpad.shortcutDescriptionResource

internal data class MinkPadHelpPage(
    val title: String,
    val description: String,
    val examples: List<MinkPadGestureExample> = emptyList(),
)

/** Visual examples have no input-report behavior. Their captions explain the same host mappings. */
@Composable
internal fun minkPadHelpPages(platform: MinkPadHostPlatform): List<MinkPadHelpPage> {
    @Composable
    fun text(@StringRes resource: Int) = stringResource(resource)
    @Composable
    fun action(value: MinkPadGestureAction) = platform.shortcutDescriptionResource(value)
        ?.let { stringResource(it) }
        ?: stringResource(MinkPadR.string.minkpad_help_not_mapped)
    fun example(label: String, fingers: Int, motion: MinkPadGestureMotion, caption: String) =
        MinkPadGestureExample(label, fingers, motion, caption)

    return listOf(
        MinkPadHelpPage(text(MinkPadR.string.minkpad_help_point_title),
            text(MinkPadR.string.minkpad_help_point_description),
            listOf(
                example(text(MinkPadR.string.minkpad_gesture_move), 1, MinkPadGestureMotion.MOVE, text(MinkPadR.string.minkpad_gesture_move_caption)),
                example(text(MinkPadR.string.minkpad_gesture_tap), 1, MinkPadGestureMotion.TAP, text(MinkPadR.string.minkpad_gesture_tap_caption)),
                example(text(MinkPadR.string.minkpad_gesture_double_tap), 1, MinkPadGestureMotion.DOUBLE_TAP, text(MinkPadR.string.minkpad_gesture_double_tap_caption)),
            )),
        MinkPadHelpPage(text(MinkPadR.string.minkpad_help_scroll_title),
            text(MinkPadR.string.minkpad_help_scroll_description),
            listOf(
                example(text(MinkPadR.string.minkpad_gesture_right_click), 2, MinkPadGestureMotion.TAP, text(MinkPadR.string.minkpad_gesture_right_click_caption)),
                example(text(MinkPadR.string.minkpad_gesture_scroll_vertical), 2, MinkPadGestureMotion.VERTICAL, text(MinkPadR.string.minkpad_gesture_scroll_vertical_caption)),
                example(text(MinkPadR.string.minkpad_gesture_scroll_horizontal), 2, MinkPadGestureMotion.HORIZONTAL, text(MinkPadR.string.minkpad_gesture_scroll_horizontal_caption)),
            )),
        MinkPadHelpPage(text(MinkPadR.string.minkpad_help_zoom_title),
            text(MinkPadR.string.minkpad_help_zoom_description),
            listOf(
                example(text(MinkPadR.string.minkpad_gesture_zoom_in), 2, MinkPadGestureMotion.SPREAD, action(MinkPadGestureAction.ZOOM_IN)),
                example(text(MinkPadR.string.minkpad_gesture_zoom_out), 2, MinkPadGestureMotion.PINCH, action(MinkPadGestureAction.ZOOM_OUT)),
            )),
        MinkPadHelpPage(text(MinkPadR.string.minkpad_help_drag_title),
            text(MinkPadR.string.minkpad_help_drag_description),
            listOf(
                example(text(MinkPadR.string.minkpad_gesture_hold), 1, MinkPadGestureMotion.HOLD, text(MinkPadR.string.minkpad_gesture_hold_caption)),
                example(text(MinkPadR.string.minkpad_gesture_drag), 1, MinkPadGestureMotion.DRAG, text(MinkPadR.string.minkpad_gesture_drag_caption)),
            )),
        MinkPadHelpPage(text(MinkPadR.string.minkpad_help_overview_title),
            text(MinkPadR.string.minkpad_help_overview_description),
            listOf(
                example(text(MinkPadR.string.minkpad_shortcut_back), 3, MinkPadGestureMotion.TAP, action(MinkPadGestureAction.BACK)),
                example(text(MinkPadR.string.minkpad_gesture_swipe_up), 3, MinkPadGestureMotion.UP, action(MinkPadGestureAction.OVERVIEW)),
                example(text(MinkPadR.string.minkpad_gesture_swipe_down), 3, MinkPadGestureMotion.DOWN, action(MinkPadGestureAction.DISMISS_OVERVIEW)),
            )),
        MinkPadHelpPage(text(MinkPadR.string.minkpad_help_switch_title),
            text(MinkPadR.string.minkpad_help_switch_description),
            listOf(
                example(text(MinkPadR.string.minkpad_gesture_swipe_left), 3, MinkPadGestureMotion.LEFT, action(MinkPadGestureAction.NEXT_APP)),
                example(text(MinkPadR.string.minkpad_gesture_swipe_right), 3, MinkPadGestureMotion.RIGHT, action(MinkPadGestureAction.PREVIOUS_APP)),
                example(text(MinkPadR.string.minkpad_gesture_app_list), 4, MinkPadGestureMotion.TAP, action(MinkPadGestureAction.APP_LIST)),
            )),
        MinkPadHelpPage(text(MinkPadR.string.minkpad_help_host_title), text(MinkPadR.string.minkpad_help_host_description)),
        MinkPadHelpPage(text(MinkPadR.string.minkpad_help_bluetooth_title), text(MinkPadR.string.minkpad_help_bluetooth_description)),
    )
}
