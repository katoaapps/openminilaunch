package com.katoaapps.openminilaunch.ui.minkpad

import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadGestureAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform

@get:StringRes
internal val MinkPadHostPlatform.labelResource: Int
    get() = when (this) {
        MinkPadHostPlatform.GENERIC -> R.string.minkpad_platform_choose
        MinkPadHostPlatform.ANDROID -> R.string.minkpad_platform_android
        MinkPadHostPlatform.MAC -> R.string.minkpad_platform_mac
        MinkPadHostPlatform.WINDOWS -> R.string.minkpad_platform_windows
        MinkPadHostPlatform.APPLE_MOBILE -> R.string.minkpad_platform_apple_mobile
    }

@StringRes
internal fun MinkPadHostPlatform.shortcutLabelResource(action: MinkPadGestureAction): Int = when (action) {
    MinkPadGestureAction.BACK -> R.string.minkpad_shortcut_back
    MinkPadGestureAction.OVERVIEW -> when (this) {
        MinkPadHostPlatform.MAC -> R.string.minkpad_shortcut_mission_control
        MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_task_view
        else -> R.string.minkpad_shortcut_recent_apps
    }
    MinkPadGestureAction.DISMISS_OVERVIEW -> when (this) {
        MinkPadHostPlatform.MAC -> R.string.minkpad_shortcut_app_expose
        MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_close_view
        else -> R.string.minkpad_shortcut_dismiss_view
    }
    MinkPadGestureAction.PREVIOUS_APP -> if (this == MinkPadHostPlatform.MAC) {
        R.string.minkpad_shortcut_previous_space
    } else {
        R.string.minkpad_shortcut_previous_app
    }
    MinkPadGestureAction.NEXT_APP -> if (this == MinkPadHostPlatform.MAC) {
        R.string.minkpad_shortcut_next_space
    } else {
        R.string.minkpad_shortcut_next_app
    }
    MinkPadGestureAction.APP_LIST -> if (this == MinkPadHostPlatform.WINDOWS) {
        R.string.minkpad_shortcut_start
    } else {
        R.string.minkpad_shortcut_app_list
    }
    MinkPadGestureAction.ZOOM_IN -> R.string.minkpad_shortcut_zoom_in
    MinkPadGestureAction.ZOOM_OUT -> R.string.minkpad_shortcut_zoom_out
}

@StringRes
internal fun MinkPadHostPlatform.shortcutDescriptionResource(action: MinkPadGestureAction): Int? = when (action) {
    MinkPadGestureAction.BACK -> when (this) {
        MinkPadHostPlatform.ANDROID -> R.string.minkpad_shortcut_back_android
        MinkPadHostPlatform.MAC -> R.string.minkpad_shortcut_back_mac
        MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_back_windows
        else -> null
    }
    MinkPadGestureAction.OVERVIEW -> when (this) {
        MinkPadHostPlatform.ANDROID -> R.string.minkpad_shortcut_overview_android
        MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_overview_windows
        MinkPadHostPlatform.MAC -> R.string.minkpad_shortcut_overview_mac
        else -> null
    }
    MinkPadGestureAction.DISMISS_OVERVIEW -> when (this) {
        MinkPadHostPlatform.ANDROID -> R.string.minkpad_shortcut_dismiss_android
        MinkPadHostPlatform.MAC -> R.string.minkpad_shortcut_dismiss_mac
        MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_dismiss_windows
        else -> null
    }
    MinkPadGestureAction.PREVIOUS_APP -> when (this) {
        MinkPadHostPlatform.MAC -> R.string.minkpad_shortcut_previous_space_description
        MinkPadHostPlatform.APPLE_MOBILE -> R.string.minkpad_shortcut_previous_app_command
        MinkPadHostPlatform.ANDROID, MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_previous_app_alt
        else -> null
    }
    MinkPadGestureAction.NEXT_APP -> when (this) {
        MinkPadHostPlatform.MAC -> R.string.minkpad_shortcut_next_space_description
        MinkPadHostPlatform.APPLE_MOBILE -> R.string.minkpad_shortcut_next_app_command
        MinkPadHostPlatform.ANDROID, MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_next_app_alt
        else -> null
    }
    MinkPadGestureAction.APP_LIST -> when (this) {
        MinkPadHostPlatform.ANDROID -> R.string.minkpad_shortcut_app_list_android
        MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_app_list_windows
        else -> null
    }
    MinkPadGestureAction.ZOOM_IN -> when (this) {
        MinkPadHostPlatform.MAC -> R.string.minkpad_shortcut_zoom_in_mac
        MinkPadHostPlatform.ANDROID, MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_zoom_in_control
        else -> null
    }
    MinkPadGestureAction.ZOOM_OUT -> when (this) {
        MinkPadHostPlatform.MAC -> R.string.minkpad_shortcut_zoom_out_mac
        MinkPadHostPlatform.ANDROID, MinkPadHostPlatform.WINDOWS -> R.string.minkpad_shortcut_zoom_out_control
        else -> null
    }
}
