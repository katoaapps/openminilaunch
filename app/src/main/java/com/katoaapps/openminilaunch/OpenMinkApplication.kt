package com.katoaapps.openminilaunch

import android.app.Application
import android.content.Context
import com.katoaapps.minkboard.core.MinkBoardHostAccess
import com.katoaapps.minkboard.core.OpenMinkFullPolicy
import com.katoaapps.minkboard.focus.android.AndroidMinkBoardFocusRuntime
import com.katoaapps.minkboard.focus.android.MinkBoardFocusRuntimeOwner
import com.katoaapps.minklauncher.todos.runtime.TodoFeatureRuntime
import com.katoaapps.minklauncher.todos.runtime.TodoFeatureResources
import com.katoaapps.openminilaunch.features.backup.TodoBoardBackupBridge
import com.katoaapps.openminilaunch.features.todos.host.createOpenMinkTodoFocusRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.sableos.games.SableGames
import org.sableos.games.SableGamesConfiguration

class OpenMinkApplication : Application(), MinkBoardFocusRuntimeOwner {
    internal val todoFeatureRuntime by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        TodoFeatureRuntime(
            context = this,
            defaultBoardName = getString(R.string.todo_page_title),
            hostAccess = MinkBoardHostAccess.full(OpenMinkFullPolicy),
            resources = TodoFeatureResources(
                authenticationPromptTitle = R.string.minkboard_authentication_prompt_title,
                authenticationPromptDescription =
                    R.string.minkboard_authentication_prompt_description,
                authenticationUnavailable = R.string.minkboard_authentication_unavailable,
                authenticationFailed = R.string.minkboard_authentication_failed,
                focusTimerAlreadyRunning = R.string.minkboard_focus_timer_already_running,
                focusTimerInexact = R.string.minkboard_focus_timer_inexact,
                notificationPermissionRequired =
                    R.string.minkboard_notification_permission_required,
                focusCardUnavailable = R.string.minkboard_focus_card_unavailable,
                changeFailed = R.string.minkboard_change_failed,
                keepOneBoard = R.string.minkboard_keep_one_board,
                chooseBoardsKeepOne = R.string.minkboard_choose_boards_keep_one,
                cardUnavailable = R.string.minkboard_card_unavailable,
                additionalBoardsUnavailable =
                    R.string.minkboard_additional_boards_unavailable,
                templatesUnavailable = R.string.minkboard_templates_unavailable,
            ),
            focusRuntimeFactory = ::createOpenMinkTodoFocusRuntime,
        )
    }

    internal val todoBoardBackupBridge by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        TodoBoardBackupBridge(
            launcherFacade = todoFeatureRuntime.launcherFacade,
            homePreferences = todoFeatureRuntime.homePreferences,
        )
    }

    override val minkBoardFocusRuntime: AndroidMinkBoardFocusRuntime
        get() = todoFeatureRuntime.focusRuntime

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SableGames.initialize(
            context = this,
            configuration = SableGamesConfiguration(allowExternalConsoleGames = false),
        )
        applicationScope.launch { minkBoardFocusRuntime.coordinator.reconcile() }
    }
}

internal val Context.todoFeatureRuntime: TodoFeatureRuntime
    get() = (applicationContext as OpenMinkApplication).todoFeatureRuntime

internal val Context.todoBoardBackupBridge: TodoBoardBackupBridge
    get() = (applicationContext as OpenMinkApplication).todoBoardBackupBridge
