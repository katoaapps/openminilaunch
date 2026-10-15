package com.katoaapps.openminilaunch.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.ui.launcher.MainActivity

private enum class WidgetBindingStage { IDLE, BOUND, CONFIGURING, AWAITING_CONFIRMATION }

/**
 * Shared Android binding/configuration flow. Placement storage and sizing stay with the caller so
 * the full widget page and optional MinkSpace support remain independently replaceable.
 */
@Stable
internal class WidgetBindingController internal constructor(
    private val beginBinding: (AppWidgetProviderInfo, Bundle) -> Unit,
    private val acceptConfiguration: () -> Unit,
    private val discardConfiguration: () -> Unit,
    val isBusy: Boolean,
    val configurationToConfirm: AppWidgetProviderInfo?,
) {
    fun begin(info: AppWidgetProviderInfo, options: Bundle) = beginBinding(info, options)
    fun useConfiguredWidget() = acceptConfiguration()
    fun discardConfiguredWidget() = discardConfiguration()
}

internal enum class WidgetBindingFailure {
    USER_CANCELLED,
    PROVIDER_UNAVAILABLE,
    BIND_LAUNCH_FAILED,
    CONFIGURATION_LAUNCH_FAILED,
}

@Composable
internal fun rememberWidgetBindingController(
    onBound: (Int, AppWidgetProviderInfo) -> Unit,
    onFailed: (WidgetBindingFailure) -> Unit = {},
): WidgetBindingController {
    val activity = LocalContext.current as MainActivity
    val manager = remember(activity) { AppWidgetManager.getInstance(activity) }
    val host = activity.appWidgetHost
    val currentOnBound by rememberUpdatedState(onBound)
    val currentOnFailed by rememberUpdatedState(onFailed)
    var pendingId by rememberSaveable { mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }
    var stage by rememberSaveable { mutableStateOf(WidgetBindingStage.IDLE) }

    fun clearPending(failure: WidgetBindingFailure? = null) {
        if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            runCatching { host.deleteAppWidgetId(pendingId) }
        }
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        stage = WidgetBindingStage.IDLE
        failure?.let(currentOnFailed)
    }

    fun completePending(info: AppWidgetProviderInfo) {
        val completedId = pendingId
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        stage = WidgetBindingStage.IDLE
        currentOnBound(completedId, info)
    }

    val bindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            stage = WidgetBindingStage.BOUND
        } else {
            clearPending(WidgetBindingFailure.USER_CANCELLED)
        }
    }

    LaunchedEffect(stage, pendingId) {
        if (pendingId == AppWidgetManager.INVALID_APPWIDGET_ID) return@LaunchedEffect

        val info = manager.getAppWidgetInfo(pendingId)
        when (stage) {
            WidgetBindingStage.BOUND -> when {
                info == null -> clearPending(WidgetBindingFailure.PROVIDER_UNAVAILABLE)
                info.configure != null -> {
                    stage = WidgetBindingStage.CONFIGURING
                    if (!activity.configureAppWidget(host, pendingId)) {
                        clearPending(WidgetBindingFailure.CONFIGURATION_LAUNCH_FAILED)
                    }
                }
                else -> completePending(info)
            }
            WidgetBindingStage.AWAITING_CONFIRMATION -> if (info == null) {
                clearPending(WidgetBindingFailure.PROVIDER_UNAVAILABLE)
            }
            WidgetBindingStage.IDLE, WidgetBindingStage.CONFIGURING -> Unit
        }
    }

    val configurationResult = activity.widgetConfigurationResult
    LaunchedEffect(configurationResult, pendingId) {
        if (stage != WidgetBindingStage.CONFIGURING ||
            configurationResult?.first != pendingId
        ) return@LaunchedEffect

        val configured = activity.consumeWidgetConfigurationResult(pendingId)
            ?: return@LaunchedEffect
        val info = manager.getAppWidgetInfo(pendingId)
        when (widgetConfigurationDecision(configured, info != null)) {
            WidgetConfigurationDecision.COMPLETE -> completePending(requireNotNull(info))
            WidgetConfigurationDecision.CONFIRM -> stage = WidgetBindingStage.AWAITING_CONFIRMATION
            WidgetConfigurationDecision.FAIL ->
                clearPending(WidgetBindingFailure.PROVIDER_UNAVAILABLE)
        }
    }

    val begin: (AppWidgetProviderInfo, Bundle) -> Unit = remember(
        activity,
        host,
        manager,
        bindLauncher,
        pendingId,
    ) {
        begin@{ info, options ->
            if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) return@begin
            val id = host.allocateAppWidgetId()
            pendingId = id
            if (manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, options)) {
                stage = WidgetBindingStage.BOUND
            } else {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)
                runCatching { bindLauncher.launch(intent) }
                    .onFailure {
                        clearPending(WidgetBindingFailure.BIND_LAUNCH_FAILED)
                    }
            }
        }
    }

    return WidgetBindingController(
        beginBinding = begin,
        acceptConfiguration = {
            val info = manager.getAppWidgetInfo(pendingId)
            if (stage == WidgetBindingStage.AWAITING_CONFIRMATION && info != null) {
                completePending(info)
            } else {
                clearPending(WidgetBindingFailure.PROVIDER_UNAVAILABLE)
            }
        },
        discardConfiguration = {
            clearPending(WidgetBindingFailure.USER_CANCELLED)
        },
        isBusy = pendingId != AppWidgetManager.INVALID_APPWIDGET_ID,
        configurationToConfirm = if (stage == WidgetBindingStage.AWAITING_CONFIRMATION) {
            manager.getAppWidgetInfo(pendingId)
        } else {
            null
        },
    )
}
