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
import com.katoaapps.openminilaunch.model.ContactResult
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.isPermanentlyDenied
import com.katoaapps.openminilaunch.ui.launcher.supportsDirectCalls

@Stable
internal class MagicCallFlow internal constructor() {
    var contactToConfirm by mutableStateOf<ContactResult?>(null)
        internal set

    internal var pendingPermissionContact: ContactResult? = null
    internal var callNowAction: () -> Unit = {}
    internal var chooseCallingAppAction: () -> Unit = {}
    internal var dismissAction: () -> Unit = {}

    fun requestConfirmation(contact: ContactResult) {
        contactToConfirm = contact
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val contact = flow.pendingPermissionContact
        flow.pendingPermissionContact = null
        if (granted && contact != null) {
            actions.placeCall(contact.phone)
        } else if (!granted && isPermanentlyDenied(context, Manifest.permission.CALL_PHONE)) {
            actions.openAppSettings()
        }
        currentOnSessionComplete()
    }

    flow.callNowAction = {
        flow.contactToConfirm?.let { contact ->
            flow.contactToConfirm = null
            if (
                !supportsDirectCalls(context) ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED
            ) {
                actions.placeCall(contact.phone)
                currentOnSessionComplete()
            } else {
                flow.pendingPermissionContact = contact
                permissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }
    flow.chooseCallingAppAction = {
        flow.contactToConfirm?.let { contact ->
            flow.contactToConfirm = null
            if (!actions.chooseCallingApp(contact.phone)) {
                Toast.makeText(
                    context,
                    context.getString(R.string.no_compatible_calling_app),
                    Toast.LENGTH_LONG,
                ).show()
            }
            currentOnSessionComplete()
        }
    }
    flow.dismissAction = {
        flow.contactToConfirm = null
        currentOnSessionComplete()
    }
    return flow
}
