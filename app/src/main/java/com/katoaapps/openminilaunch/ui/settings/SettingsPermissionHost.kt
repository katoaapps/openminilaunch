package com.katoaapps.openminilaunch.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.katoaapps.openminilaunch.features.conversations.NotificationHub
import com.katoaapps.openminilaunch.features.wellbeing.UsageInsightsRepository
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.hasMediaReadAccess
import com.katoaapps.openminilaunch.ui.launcher.isPermanentlyDenied
import com.katoaapps.openminilaunch.ui.launcher.mediaPermissionPermanentlyDenied
import com.katoaapps.openminilaunch.ui.launcher.mediaReadPermissions
import com.katoaapps.openminilaunch.ui.launcher.supportsDirectCalls
import com.katoaapps.openminilaunch.ui.launcher.supportsDirectSms
import com.katoaapps.openminilaunch.ui.onboarding.UsageAccessDisclosureDialog

internal data class SettingsPermissionHostState(
    val state: SettingsPermissionState,
    val actions: SettingsPermissionActions,
    val usageInsights: UsageInsightsRepository,
)

/** Owns permission status refresh, Android activity-result launchers, and disclosures. */
@Composable
internal fun rememberSettingsPermissionHost(
    deviceActions: DeviceActions,
    onMediaPermissionResult: () -> Unit,
): SettingsPermissionHostState {
    val context = LocalContext.current
    val currentOnMediaPermissionResult by rememberUpdatedState(onMediaPermissionResult)
    val usageInsights = remember { UsageInsightsRepository(context.applicationContext) }
    var contactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var mediaGranted by remember { mutableStateOf(hasMediaReadAccess(context)) }
    var callsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var smsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var lockServiceEnabled by remember { mutableStateOf(deviceActions.isLockServiceEnabled()) }
    var assistantRoleHeld by remember { mutableStateOf(deviceActions.isAssistantRoleHeld()) }
    var notificationAccessGranted by remember { mutableStateOf(NotificationHub.hasAccess(context)) }
    var usageAccessGranted by remember { mutableStateOf(usageInsights.hasAccess()) }
    var showLockDisclosure by remember { mutableStateOf(false) }
    var showAssistantDisclosure by remember { mutableStateOf(false) }
    var showNotificationDisclosure by remember { mutableStateOf(false) }
    var showUsageDisclosure by remember { mutableStateOf(false) }

    fun refreshStatuses() {
        contactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        mediaGranted = hasMediaReadAccess(context)
        callsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED
        smsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        lockServiceEnabled = deviceActions.isLockServiceEnabled()
        assistantRoleHeld = deviceActions.isAssistantRoleHeld()
        notificationAccessGranted = NotificationHub.hasAccess(context)
        usageAccessGranted = usageInsights.hasAccess()
    }

    DisposableEffect(context, deviceActions) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshStatuses()
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    val contactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        contactsGranted = granted
        if (!granted && isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)) {
            deviceActions.openAppSettings()
        }
    }
    val callPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        callsGranted = granted
        if (!granted && isPermanentlyDenied(context, Manifest.permission.CALL_PHONE)) {
            deviceActions.openAppSettings()
        }
    }
    val smsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        smsGranted = granted
        if (!granted && isPermanentlyDenied(context, Manifest.permission.SEND_SMS)) {
            deviceActions.openAppSettings()
        }
    }
    val mediaPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        mediaGranted = hasMediaReadAccess(context)
        if (!mediaGranted && mediaPermissionPermanentlyDenied(context)) {
            deviceActions.openAppSettings()
        }
        currentOnMediaPermissionResult()
    }
    val lockServiceSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        lockServiceEnabled = deviceActions.isLockServiceEnabled()
    }
    val assistantRoleSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        assistantRoleHeld = deviceActions.isAssistantRoleHeld()
    }
    val notificationAccessSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        notificationAccessGranted = NotificationHub.hasAccess(context)
        if (notificationAccessGranted) {
            showNotificationDisclosure = false
            NotificationHub.requestReconnect(context)
        }
    }
    val usageAccessSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        usageAccessGranted = usageInsights.hasAccess()
    }

    if (showLockDisclosure) {
        LockAccessibilityDisclosureDialog(
            onContinue = {
                showLockDisclosure = false
                lockServiceSettings.launch(deviceActions.lockAccessibilitySettingsIntent())
            },
            onDismiss = { showLockDisclosure = false },
        )
    }
    if (showAssistantDisclosure) {
        AssistantDisclosureDialog(
            active = assistantRoleHeld,
            onContinue = {
                showAssistantDisclosure = false
                assistantRoleSettings.launch(deviceActions.assistantRoleSelectionIntent())
            },
            onDismiss = { showAssistantDisclosure = false },
        )
    }
    if (showNotificationDisclosure) {
        NotificationAccessDisclosureDialog(
            onOpenAppInfo = deviceActions::openAppSettings,
            onOpenNotificationAccess = {
                notificationAccessSettings.launch(NotificationHub.accessSettingsIntent())
            },
            onDismiss = { showNotificationDisclosure = false },
        )
    }
    if (showUsageDisclosure) {
        UsageAccessDisclosureDialog(
            onContinue = {
                showUsageDisclosure = false
                usageAccessSettings.launch(usageInsights.accessSettingsIntent())
            },
            onDismiss = { showUsageDisclosure = false },
        )
    }

    return SettingsPermissionHostState(
        state = SettingsPermissionState(
            usageAccessGranted = usageAccessGranted,
            notificationAccessGranted = notificationAccessGranted,
            contactsGranted = contactsGranted,
            directCallsSupported = remember(context) { supportsDirectCalls(context) },
            callsGranted = callsGranted,
            directSmsSupported = supportsDirectSms(context),
            assistantRoleHeld = assistantRoleHeld,
            smsGranted = smsGranted,
            mediaGranted = mediaGranted,
            lockSupported = deviceActions.supportsLockScreenAction(),
            lockServiceEnabled = lockServiceEnabled,
        ),
        actions = SettingsPermissionActions(
            requestUsageAccess = { showUsageDisclosure = true },
            manageUsageAccess = { usageAccessSettings.launch(usageInsights.accessSettingsIntent()) },
            requestNotificationAccess = { showNotificationDisclosure = true },
            manageNotificationAccess = {
                notificationAccessSettings.launch(NotificationHub.accessSettingsIntent())
            },
            requestContacts = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) },
            requestCalls = { callPermission.launch(Manifest.permission.CALL_PHONE) },
            requestSms = { smsPermission.launch(Manifest.permission.SEND_SMS) },
            requestMedia = { mediaPermission.launch(mediaReadPermissions()) },
            manageAppPermissions = deviceActions::openAppSettings,
            requestLockService = { showLockDisclosure = true },
            manageLockService = deviceActions::openLockAccessibilitySettings,
            showAssistantSetup = { showAssistantDisclosure = true },
        ),
        usageInsights = usageInsights,
    )
}
