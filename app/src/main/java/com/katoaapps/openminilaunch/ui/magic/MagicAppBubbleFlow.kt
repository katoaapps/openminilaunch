package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.bubbles.AppBubbleLaunchResult
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.isPermanentlyDenied

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** State and Android permission flow for the experimental Magic Box app-bubble route. */
internal class MagicAppBubbleFlow internal constructor(
    val actionTarget: LauncherAppTarget?,
    private val automaticTargetKeys: Set<String>,
    private val setActionTarget: (LauncherAppTarget?) -> Unit,
    private val setAutomatic: (LauncherAppTarget, Boolean) -> Unit,
    private val openBubbleAction: (LauncherAppTarget) -> Unit,
    private val openNormallyAction: (LauncherAppTarget) -> Unit,
    private val dismissAction: () -> Unit,
) {
    fun opensAutomatically(target: LauncherAppTarget): Boolean =
        target.selectionKey in automaticTargetKeys

    fun showActions(target: LauncherAppTarget) = setActionTarget(target)

    fun isAutomatic(target: LauncherAppTarget): Boolean = opensAutomatically(target)

    fun updateAutomatic(target: LauncherAppTarget, enabled: Boolean) =
        setAutomatic(target, enabled)

    fun openBubble(target: LauncherAppTarget) {
        setActionTarget(null)
        openBubbleAction(target)
    }

    fun openNormally(target: LauncherAppTarget) = openNormallyAction(target)

    fun dismissActions() = dismissAction()
}

@Composable
internal fun rememberMagicAppBubbleFlow(
    store: LauncherStore,
    actions: DeviceActions,
    onAppOpened: (LauncherAppTarget) -> Unit,
    onRefocus: () -> Unit,
): MagicAppBubbleFlow {
    val context = LocalContext.current
    val currentOnAppOpened by rememberUpdatedState(onAppOpened)
    val currentOnRefocus by rememberUpdatedState(onRefocus)
    var actionTarget by remember { mutableStateOf<LauncherAppTarget?>(null) }
    var pendingPermissionTarget by remember { mutableStateOf<LauncherAppTarget?>(null) }
    var grantedPermissionTarget by remember { mutableStateOf<LauncherAppTarget?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val target = pendingPermissionTarget
        pendingPermissionTarget = null
        if (granted) {
            grantedPermissionTarget = target
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            isPermanentlyDenied(context, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            actions.openAppNotificationSettings()
        } else {
            currentOnRefocus()
        }
    }

    fun toast(message: Int) {
        Toast.makeText(context, context.getString(message), Toast.LENGTH_LONG).show()
    }

    fun openBubble(target: LauncherAppTarget) {
        when (actions.launchAppBubble(target)) {
            AppBubbleLaunchResult.OPENED -> currentOnAppOpened(target)
            AppBubbleLaunchResult.NOTIFICATION_PERMISSION_REQUIRED -> {
                pendingPermissionTarget = target
                val canRequestRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                if (canRequestRuntimePermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    pendingPermissionTarget = null
                    actions.openAppNotificationSettings()
                    toast(R.string.app_bubble_notifications_required)
                }
            }
            AppBubbleLaunchResult.BUBBLES_DISABLED -> {
                actions.openAppBubbleSettings()
                toast(R.string.app_bubbles_disabled_toast)
            }
            AppBubbleLaunchResult.UNSUPPORTED -> {
                toast(R.string.app_bubbles_requires_android_11)
                currentOnRefocus()
            }
            AppBubbleLaunchResult.WORK_PROFILE_UNSUPPORTED -> {
                toast(R.string.app_bubble_work_profile_unsupported)
                currentOnRefocus()
            }
            AppBubbleLaunchResult.APP_UNAVAILABLE -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.launcher_app_unavailable, target.label),
                    Toast.LENGTH_LONG,
                ).show()
                currentOnRefocus()
            }
            AppBubbleLaunchResult.FAILED -> {
                toast(R.string.app_bubble_launch_failed)
                currentOnRefocus()
            }
        }
    }

    LaunchedEffect(grantedPermissionTarget) {
        grantedPermissionTarget?.let { target ->
            grantedPermissionTarget = null
            openBubble(target)
        }
    }

    return MagicAppBubbleFlow(
        actionTarget = actionTarget,
        automaticTargetKeys = store.automaticAppBubbleTargets,
        setActionTarget = { actionTarget = it },
        setAutomatic = { target, enabled ->
            store.setAppBubbleAutomatic(target.selectionKey, enabled)
        },
        openBubbleAction = ::openBubble,
        openNormallyAction = { target ->
            actionTarget = null
            if (actions.launchLauncherTarget(target)) {
                currentOnAppOpened(target)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.launcher_app_unavailable, target.label),
                    Toast.LENGTH_LONG,
                ).show()
                currentOnRefocus()
            }
        },
        dismissAction = {
            actionTarget = null
            currentOnRefocus()
        },
    )
}

@Composable
internal fun MagicAppBubbleDialogHost(flow: MagicAppBubbleFlow, actions: DeviceActions) {
    flow.actionTarget?.let { target ->
        AppBubbleActionDialog(
            target = target,
            actions = actions,
            automatic = flow.isAutomatic(target),
            onAutomaticChange = { flow.updateAutomatic(target, it) },
            onOpenNormally = { flow.openNormally(target) },
            onOpenBubble = { flow.openBubble(target) },
            onDismiss = flow::dismissActions,
        )
    }
}
