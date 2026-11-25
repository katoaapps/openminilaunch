package com.katoaapps.openminilaunch.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.StringRes
import com.katoaapps.minkboard.ui.R as MinkBoardR
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.Screen
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination

/** A request to show a durable, non-Home Mink surface in the Recents-visible workspace task. */
internal data class MinkWorkspaceRequest(
    val token: Long,
    val screen: Screen,
    val settingsDestination: SettingsDestination = SettingsDestination.OVERVIEW,
)

/** Localized title shown for the workspace document in Android Recents. */
@StringRes
internal fun Screen.workspaceRecentsLabelRes(): Int = when (this) {
    Screen.HOME -> R.string.app_name
    Screen.SETTINGS -> R.string.settings
    Screen.TODOS -> MinkBoardR.string.minkboard_title
    Screen.CONVERSATIONS -> R.string.conversations
    Screen.MINK_DAY -> R.string.mink_day
}

/**
 * Hosts Mink's full-screen work separately from the default launcher task.
 *
 * Android brings [MainActivity] forward whenever the user presses Home. Keeping this activity in a
 * different task preserves an unfinished board edit or settings page in Recents instead of letting
 * that Home request replace it.
 */
class MinkWorkspaceActivity : MainActivity() {
    override val isWorkspaceHost: Boolean = true

    companion object {
        private const val ACTION_OPEN_WORKSPACE =
            "com.katoaapps.openminilaunch.action.OPEN_WORKSPACE"
        internal const val ACTION_OPEN_TODOS =
            "com.katoaapps.openminilaunch.action.OPEN_TODOS"
        private const val EXTRA_SCREEN =
            "com.katoaapps.openminilaunch.extra.WORKSPACE_SCREEN"
        private const val EXTRA_SETTINGS_DESTINATION =
            "com.katoaapps.openminilaunch.extra.WORKSPACE_SETTINGS_DESTINATION"

        internal fun intent(
            context: Context,
            screen: Screen,
            settingsDestination: SettingsDestination = SettingsDestination.OVERVIEW,
        ): Intent =
            Intent(context, MinkWorkspaceActivity::class.java)
                .setAction(ACTION_OPEN_WORKSPACE)
                .setData(
                    Uri.Builder()
                        .scheme("mink")
                        .authority(context.packageName)
                        .appendPath("workspace")
                        .appendPath(screen.name.lowercase())
                        .build(),
                )
                .putExtra(EXTRA_SCREEN, screen.name)
                .putExtra(EXTRA_SETTINGS_DESTINATION, settingsDestination.name)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS,
                )

        internal fun todosIntent(context: Context): Intent =
            intent(context, Screen.TODOS).setAction(ACTION_OPEN_TODOS)

        internal fun request(intent: Intent?): MinkWorkspaceRequest? {
            if (intent?.action == ACTION_OPEN_TODOS) {
                return MinkWorkspaceRequest(
                    token = SystemClock.elapsedRealtimeNanos(),
                    screen = Screen.TODOS,
                )
            }
            if (intent?.action != ACTION_OPEN_WORKSPACE) return null
            val screen = intent.getStringExtra(EXTRA_SCREEN)
                ?.let { value -> enumValues<Screen>().firstOrNull { it.name == value } }
                ?.takeUnless { it == Screen.HOME }
                ?: return null
            val settingsDestination = intent.getStringExtra(EXTRA_SETTINGS_DESTINATION)
                ?.let { value ->
                    enumValues<SettingsDestination>().firstOrNull { it.name == value }
                }
                ?: SettingsDestination.OVERVIEW
            return MinkWorkspaceRequest(
                token = SystemClock.elapsedRealtimeNanos(),
                screen = screen,
                settingsDestination = settingsDestination,
            )
        }
    }
}
