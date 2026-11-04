package com.katoaapps.openminilaunch.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.katoaapps.openminilaunch.model.AppFolderAnimationStyle
import com.katoaapps.openminilaunch.model.AppFolderIndicatorStyle
import com.katoaapps.openminilaunch.model.appFolderAnimationStyle
import com.katoaapps.openminilaunch.model.appFolderIndicatorStyle

/** Display preferences that apply to the App Grid as a whole. */
internal class AppGridPreferenceStore(private val prefs: SharedPreferences) {
    var folderIndicatorStyle by mutableStateOf(
        appFolderIndicatorStyle(prefs.getString(FOLDER_INDICATOR_STYLE_KEY, null)),
    )
        private set
    var folderAnimationStyle by mutableStateOf(
        appFolderAnimationStyle(prefs.getString(FOLDER_ANIMATION_STYLE_KEY, null)),
    )
        private set

    fun updateFolderIndicatorStyle(style: AppFolderIndicatorStyle) {
        if (folderIndicatorStyle == style) return
        folderIndicatorStyle = style
        prefs.edit { putString(FOLDER_INDICATOR_STYLE_KEY, style.name) }
    }

    fun updateFolderAnimationStyle(style: AppFolderAnimationStyle) {
        if (folderAnimationStyle == style) return
        folderAnimationStyle = style
        prefs.edit { putString(FOLDER_ANIMATION_STYLE_KEY, style.name) }
    }

    private companion object {
        const val FOLDER_INDICATOR_STYLE_KEY = "app_grid_folder_indicator_style"
        const val FOLDER_ANIMATION_STYLE_KEY = "app_grid_folder_animation_style"
    }
}
