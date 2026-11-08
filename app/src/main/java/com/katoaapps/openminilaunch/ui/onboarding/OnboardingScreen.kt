package com.katoaapps.openminilaunch.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption
import com.katoaapps.openminilaunch.features.magic.hasUsableHardwareKeyboard
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.platform.isPermanentlyDenied
import com.katoaapps.openminilaunch.ui.settings.MessagingProviderPickerDialog
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Rust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class OnboardingPage(
    val step: OnboardingStep,
    val title: String,
    val icon: ImageVector,
)

private enum class OnboardingStep {
    HOME,
    KEYBOARD,
    CONTACTS,
    MESSAGING,
    ASSISTANT,
}

@Composable
internal fun OnboardingScreen(
    store: LauncherStore,
    actions: DeviceActions,
    requestHomeRole: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val showKeyboardSetup by rememberSaveable {
        mutableStateOf(
            !hasUsableHardwareKeyboard(
                configuration.keyboard,
                configuration.hardKeyboardHidden,
            ),
        )
    }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var homeRoleHeld by remember { mutableStateOf(actions.isHomeRoleHeld()) }
    var contactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var assistantRoleHeld by remember { mutableStateOf(actions.isAssistantRoleHeld()) }
    var pickingMessaging by remember { mutableStateOf(false) }
    var messagingOptions by remember { mutableStateOf<List<MessagingProviderOption>>(emptyList()) }
    var messagingOptionsLoaded by remember { mutableStateOf(false) }
    var messagingOptionsRefresh by remember { mutableIntStateOf(0) }

    val contactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        contactsGranted = granted
        if (!granted && isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)) {
            actions.openAppPermissionSettings()
        }
    }
    val assistantRoleSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        assistantRoleHeld = actions.isAssistantRoleHeld()
    }

    DisposableEffect(lifecycleOwner, actions) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeRoleHeld = actions.isHomeRoleHeld()
                contactsGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS,
                ) == PackageManager.PERMISSION_GRANTED
                assistantRoleHeld = actions.isAssistantRoleHeld()
                if (pickingMessaging) {
                    messagingOptionsLoaded = false
                    messagingOptionsRefresh++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pickingMessaging, messagingOptionsRefresh) {
        if (pickingMessaging) {
            messagingOptions = withContext(Dispatchers.IO) { actions.messagingProviderOptions() }
            messagingOptionsLoaded = true
        }
    }

    val pages = buildList {
        add(OnboardingPage(OnboardingStep.HOME, stringResource(R.string.default_home_app), Icons.Default.Home))
        if (showKeyboardSetup) {
            add(
                OnboardingPage(
                    OnboardingStep.KEYBOARD,
                    stringResource(R.string.onboarding_keyboard_title),
                    Icons.Default.Keyboard,
                ),
            )
        }
        add(
            OnboardingPage(
                OnboardingStep.CONTACTS,
                stringResource(R.string.onboarding_contacts_title),
                Icons.Default.Contacts,
            ),
        )
        add(
            OnboardingPage(
                OnboardingStep.MESSAGING,
                stringResource(R.string.onboarding_messages_title),
                Icons.AutoMirrored.Filled.Chat,
            ),
        )
        add(
            OnboardingPage(
                OnboardingStep.ASSISTANT,
                stringResource(R.string.onboarding_assistant_title),
                Icons.Default.Assistant,
            ),
        )
    }

    BackHandler(enabled = page > 0) { page-- }
    OnboardingFrame(
        pages = pages,
        page = page,
        nextEnabled = page > 0 || homeRoleHeld,
        onBack = { page-- },
        onNext = { if (page < pages.lastIndex) page++ else onFinish() },
    ) {
        key(page) {
            when (pages[page].step) {
                OnboardingStep.HOME -> OnboardingHomeSetupPage(
                    appName = stringResource(R.string.app_name),
                    active = homeRoleHeld,
                    onChooseHome = requestHomeRole,
                    onManageHome = { actions.openHomeSettings() },
                )
                OnboardingStep.KEYBOARD -> OnboardingKeyboardSetupPage(
                    preferences = store.launcherKeyboardPreferences,
                    themePrimaryArgb = store.effectiveHomePanelColorArgb,
                    themeSecondaryArgb = store.visibleAppBackgroundColorArgb
                        ?: store.effectiveAppBackgroundColorArgb
                        ?: 0xFFFFE9CB.toInt(),
                    onStyle = { style ->
                        store.updateLauncherKeyboardPreferences(
                            store.launcherKeyboardPreferences.copy(style = style),
                        )
                    },
                )
                OnboardingStep.CONTACTS -> OnboardingContactsSetupPage(
                    granted = contactsGranted,
                    onAllow = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) },
                    onManage = actions::openAppPermissionSettings,
                )
                OnboardingStep.MESSAGING -> OnboardingMessagingSetupPage(
                    sendAutomatically = store.sendMessagesAutomatically,
                    onSendAutomaticallyChanged = store::updateSendMessagesAutomatically,
                    selectedProvider = store.preferredMessagingPackage
                        ?.let(actions::appLabel)
                        ?: stringResource(R.string.system_messages),
                    onChooseProvider = { pickingMessaging = true },
                )
                OnboardingStep.ASSISTANT -> OnboardingAssistantSetupPage(
                    active = assistantRoleHeld,
                    onChooseAssistant = {
                        assistantRoleSettings.launch(actions.assistantRoleSelectionIntent())
                    },
                )
            }
        }
    }

    if (pickingMessaging) {
        MessagingProviderPickerDialog(
            title = stringResource(R.string.choose_preferred_messaging_app),
            options = messagingOptions,
            loading = !messagingOptionsLoaded,
            selectedProviderId = MessagingProviderCatalog.providerForPackage(
                store.preferredMessagingPackage,
            )?.id ?: MessagingProviderCatalog.SYSTEM_DEFAULT_PROVIDER_ID,
            showUnavailable = true,
            onProvider = { option ->
                if (option.systemDefault) {
                    store.resetPreferredMessagingApp()
                } else {
                    option.preferencePackageName?.let(store::setPreferredMessagingApp)
                }
                pickingMessaging = false
            },
            onInstall = { option ->
                option.preferencePackageName?.let(actions::openMessagingProviderInstallPage)
            },
            onDismiss = { pickingMessaging = false },
        )
    }
}

@Composable
private fun OnboardingFrame(
    pages: List<OnboardingPage>,
    page: Int,
    nextEnabled: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = Dimens.dp720)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.dp24),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = Dimens.dp18),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(Dimens.dp44),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .clip(CircleShape)
                                .graphicsLayer(scaleX = 1.55f, scaleY = 1.55f),
                        )
                    }
                    Column(Modifier.padding(start = Dimens.dp12).weight(1f)) {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = Dimens.sp15)
                        Text(
                            stringResource(R.string.setup),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = Dimens.sp12,
                        )
                    }
                    Text(
                        "${page + 1} / ${pages.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = Dimens.sp13,
                    )
                }
                LinearProgressIndicator(
                    progress = { (page + 1).toFloat() / pages.size },
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp18),
                    color = Rust,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    drawStopIndicator = {},
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = Dimens.dp28, bottom = Dimens.dp20),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(pages[page].icon, null, Modifier.size(Dimens.dp40), tint = Rust)
                    Text(
                        pages[page].title,
                        fontSize = Dimens.sp28,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = Dimens.dp14),
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    content()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = Dimens.dp16),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (page > 0) {
                        OutlinedButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            Text(stringResource(R.string.back), Modifier.padding(start = Dimens.dp6))
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Button(
                        onClick = onNext,
                        enabled = nextEnabled,
                        modifier = if (page > 0) Modifier.weight(1f) else Modifier,
                    ) {
                        Text(stringResource(if (page < pages.lastIndex) R.string.next else R.string.finish_setup))
                        if (page < pages.lastIndex) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                null,
                                Modifier.padding(start = Dimens.dp6),
                            )
                        }
                    }
                }
            }
        }
    }
}
