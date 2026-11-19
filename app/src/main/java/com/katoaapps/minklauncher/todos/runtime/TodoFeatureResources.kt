package com.katoaapps.minklauncher.todos.runtime

import androidx.annotation.StringRes

/**
 * Host-owned copy used by the launcher Todos presentation layer.
 *
 * Keeping resource IDs at the application boundary lets every launcher distribution reuse the
 * same Todos implementation while retaining its own generated R class and localized resources.
 */
internal data class TodoFeatureResources(
    @param:StringRes val authenticationPromptTitle: Int,
    @param:StringRes val authenticationPromptDescription: Int,
    @param:StringRes val authenticationUnavailable: Int,
    @param:StringRes val authenticationFailed: Int,
    @param:StringRes val focusTimerAlreadyRunning: Int,
    @param:StringRes val focusTimerInexact: Int,
    @param:StringRes val notificationPermissionRequired: Int,
    @param:StringRes val focusCardUnavailable: Int,
    @param:StringRes val changeFailed: Int,
    @param:StringRes val keepOneBoard: Int,
    @param:StringRes val chooseBoardsKeepOne: Int,
    @param:StringRes val cardUnavailable: Int,
    @param:StringRes val additionalBoardsUnavailable: Int,
    @param:StringRes val templatesUnavailable: Int,
)
