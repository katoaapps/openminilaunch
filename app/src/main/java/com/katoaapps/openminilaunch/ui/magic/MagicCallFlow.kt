package com.katoaapps.openminilaunch.ui.magic

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.looksLikePhoneRecipient
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.isPermanentlyDenied
import com.katoaapps.openminilaunch.ui.launcher.supportsDirectCalls

@Stable
internal class MagicCallFlow internal constructor() {
    var recipientToConfirm by mutableStateOf<CommunicationRecipient?>(null)
        internal set

    internal var pendingPermissionRecipient: CommunicationRecipient? = null
    internal var callNowAction: () -> Unit = {}
    internal var chooseCallingAppAction: () -> Unit = {}
    internal var dismissAction: () -> Unit = {}

    fun requestConfirmation(recipient: CommunicationRecipient) {
        recipientToConfirm = recipient
    }

    fun callNow() = callNowAction()

    fun chooseCallingApp() = chooseCallingAppAction()

    fun dismiss() = dismissAction()
}

/** Owns direct-call permission handling and the call confirmation lifecycle. */
@Composable
internal fun rememberMagicCallFlow(
    actions: DeviceActions,
    onSessionComplete: () -> Unit,
): MagicCallFlow {
    val context = LocalContext.current
    val currentOnSessionComplete by rememberUpdatedState(onSessionComplete)
    val flow = remember { MagicCallFlow() }

    fun openCallingApp(recipient: CommunicationRecipient) {
        if (!actions.chooseCallingApp(recipient.address)) {
            Toast.makeText(
                context,
                context.getString(R.string.no_compatible_calling_app),
                Toast.LENGTH_LONG,
            ).show()
        }
        currentOnSessionComplete()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val recipient = flow.pendingPermissionRecipient
        flow.pendingPermissionRecipient = null
        if (granted && recipient != null) {
            actions.placeCall(recipient.address)
        } else if (!granted && isPermanentlyDenied(context, Manifest.permission.CALL_PHONE)) {
            actions.openAppSettings()
        }
        currentOnSessionComplete()
    }

    flow.callNowAction = {
        flow.recipientToConfirm?.let { recipient ->
            flow.recipientToConfirm = null
            if (recipient.userEntered && !recipient.address.looksLikePhoneRecipient()) {
                openCallingApp(recipient)
            } else if (
                !supportsDirectCalls(context) ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED
            ) {
                actions.placeCall(recipient.address)
                currentOnSessionComplete()
            } else {
                flow.pendingPermissionRecipient = recipient
                permissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }
    flow.chooseCallingAppAction = {
        flow.recipientToConfirm?.let { recipient ->
            flow.recipientToConfirm = null
            openCallingApp(recipient)
        }
    }
    flow.dismissAction = {
        flow.recipientToConfirm = null
        currentOnSessionComplete()
    }
    return flow
}
