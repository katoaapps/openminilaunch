package com.katoaapps.minklauncher.todos.presentation

import com.katoaapps.minklauncher.todos.home.TodoCardUnavailableException
import com.katoaapps.minklauncher.todos.runtime.TodoFeatureResources

import android.content.Context
/** Maps launcher command failures to host-owned, localized presentation text. */
internal fun Context.todoBoardErrorMessage(
    failure: Throwable,
    resources: TodoFeatureResources,
): String = when (failure) {
    is TodoCardUnavailableException -> getString(resources.cardUnavailable)
    else -> failure.message ?: getString(resources.changeFailed)
}
