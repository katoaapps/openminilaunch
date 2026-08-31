@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch.ui.onboarding

import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.magic.*
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.features.wellbeing.*
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.launcher.ShortcutAssignmentRow
import com.katoaapps.openminilaunch.ui.launcher.displayLabel
import com.katoaapps.openminilaunch.ui.launcher.displaySlotLabel
import com.katoaapps.openminilaunch.ui.theme.*
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.settings.AppPickerDialog
import com.katoaapps.openminilaunch.ui.settings.LauncherAppPickerDialog
import com.katoaapps.openminilaunch.ui.settings.MessagingProviderPickerDialog

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun FeatureUpdateDialog(
    onOpenSettings: () -> Unit,
    onReviewTutorial: () -> Unit,
    onNotNow: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onNotNow,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.Pets, null, tint = Rust) },
        title = { Text(stringResource(R.string.whats_new)) },
        text = {
            Column(
                Modifier.heightIn(max = Dimens.dp560).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp14),
            ) {
                Text(stringResource(R.string.mink_day_can_help_you_step_away), fontSize = Dimens.sp18, fontWeight = FontWeight.Bold)
                UpdatePoint(Icons.Default.Timer, stringResource(R.string.update_notice_daily_limit_title), stringResource(R.string.update_notice_daily_limit_description))
                UpdatePoint(Icons.Default.Block, stringResource(R.string.update_notice_pause_apps_title), stringResource(R.string.update_notice_pause_apps_description))
                UpdatePoint(Icons.Default.Tune, stringResource(R.string.update_notice_home_browser_title), stringResource(R.string.update_notice_home_browser_description))
                TextButton(onClick = onReviewTutorial, contentPadding = PaddingValues(Dimens.dp0)) {
                    Text(stringResource(R.string.review_updated_tutorial))
                }
            }
        },
        confirmButton = { Button(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) } },
        dismissButton = { TextButton(onClick = onNotNow) { Text(stringResource(R.string.not_now)) } },
    )
}

@Composable
internal fun FileSearchScopeDialog(
    onChooseFolder: () -> Unit,
    onSkip: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onSkip,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.FolderOpen, null, tint = Rust) },
        title = { Text(stringResource(R.string.file_scope_title, appName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp14)) {
                Text(stringResource(R.string.file_scope_description))
                Surface(
                    onClick = onChooseFolder,
                    shape = RoundedCornerShape(Dimens.dp16),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Box(Modifier.padding(Dimens.dp14)) {
                        UpdatePoint(
                            Icons.Default.Folder,
                            stringResource(R.string.choose_folder),
                            stringResource(R.string.choose_folder_description, appName),
                        )
                    }
                }
                Text(
                    stringResource(R.string.queries_files_stay_local),
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onSkip) { Text(stringResource(R.string.skip_for_now)) } },
    )
}

@Composable
internal fun UpdatePoint(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(Dimens.dp22), tint = Rust)
        Column(Modifier.padding(start = Dimens.dp10)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = Dimens.sp13)
        }
    }
}

@Composable
internal fun UsageAccessDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
        icon = { Icon(Icons.Default.Pets, null, tint = Rust) },
        title = { Text(stringResource(R.string.usage_disclosure_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                Text(stringResource(R.string.usage_disclosure_body_one, appName))
                Text(stringResource(R.string.usage_disclosure_body_two, appName))
                Text(stringResource(R.string.usage_disclosure_body_three))
            }
        },
        confirmButton = { Button(onClick = onContinue) { Text(stringResource(R.string.continue_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) } },
    )
}

@Composable
internal fun OnboardingScreen(store: LauncherStore, actions: DeviceActions, onFinish: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    var page by rememberSaveable { mutableIntStateOf(0) }
    var pickingAi by remember { mutableStateOf(false) }
    var pickingAllAi by remember { mutableStateOf(false) }
    var pickingMessaging by remember { mutableStateOf(false) }
    var messagingOptions by remember { mutableStateOf<List<MessagingProviderOption>>(emptyList()) }
    var messagingOptionsLoaded by remember { mutableStateOf(false) }
    var aiAppsLoaded by remember { mutableStateOf(false) }
    val curatedAiApps by produceState<List<LaunchableApp>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { actions.curatedAiApps() }
        aiAppsLoaded = true
    }
    var allAiAppsLoaded by remember { mutableStateOf(false) }
    var assistantRoleHeld by remember { mutableStateOf(actions.isAssistantRoleHeld()) }
    val assistantRoleSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        assistantRoleHeld = actions.isAssistantRoleHeld()
    }
    val allAiApps by produceState<List<LaunchableApp>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { actions.textShareApps() }
        allAiAppsLoaded = true
    }
    LaunchedEffect(pickingMessaging) {
        if (pickingMessaging && !messagingOptionsLoaded) {
            messagingOptions = withContext(Dispatchers.IO) { actions.messagingProviderOptions() }
            messagingOptionsLoaded = true
        }
    }
    val titles = listOf(
        stringResource(R.string.onboarding_page_home),
        stringResource(R.string.onboarding_page_magic),
        stringResource(R.string.onboarding_page_todos),
        stringResource(R.string.onboarding_page_search),
        stringResource(R.string.onboarding_page_assistant),
        stringResource(R.string.onboarding_page_mink_day),
        stringResource(R.string.onboarding_page_spaces),
        stringResource(R.string.onboarding_page_permissions),
    )
    val icons = listOf(
        Icons.Default.Keyboard,
        Icons.Default.AutoAwesome,
        Icons.Default.Checklist,
        Icons.Default.ManageSearch,
        Icons.Default.Assistant,
        Icons.Default.Pets,
        Icons.Default.Widgets,
        Icons.Default.Security,
    )
    BackHandler {
        if (page > 0) page--
    }
    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.dp24),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = Dimens.dp18),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.padding(Dimens.dp10).size(Dimens.dp24),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(Modifier.padding(start = Dimens.dp12).weight(1f)) {
                    Text(appName, fontWeight = FontWeight.Bold, fontSize = Dimens.sp15)
                    Text(
                        stringResource(R.string.setup),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = Dimens.sp12,
                    )
                }
                Text(
                    "${page + 1} / ${titles.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = Dimens.sp13,
                )
            }
            LinearProgressIndicator(
                progress = { (page + 1).toFloat() / titles.size },
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp18),
                color = Rust,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                drawStopIndicator = {},
            )
            Row(
                Modifier.fillMaxWidth().padding(top = Dimens.dp28, bottom = Dimens.dp20),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icons[page], null, Modifier.size(Dimens.dp40), tint = Rust)
                Text(
                    titles[page],
                    fontSize = Dimens.sp28,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = Dimens.dp14),
                )
            }
            key(page) {
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    when (page) {
                        0 -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
                            Text(stringResource(R.string.onboarding_intro_description, appName), fontSize = Dimens.sp18)
                            OnboardingPoint(Icons.Default.FilterAlt, stringResource(R.string.less_visual_noise), stringResource(R.string.less_visual_noise_description))
                            OnboardingPoint(Icons.Default.Palette, stringResource(R.string.choose_panel_color), stringResource(R.string.choose_panel_color_description))
                            OnboardingPoint(Icons.Default.Keyboard, stringResource(R.string.just_start_typing), stringResource(R.string.just_start_typing_description))
                            OnboardingPoint(Icons.Default.Search, stringResource(R.string.everything_reachable), stringResource(R.string.everything_reachable_description))
                        }
                        1 -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                            Text(stringResource(R.string.magic_box_ready_description), color = Muted, fontSize = Dimens.sp12)
                            listOf(
                                "@" to stringResource(R.string.magic_text_contact),
                                "#" to stringResource(R.string.magic_call_contact),
                                "-" to stringResource(R.string.magic_create_todo),
                                MAGIC_NOTE_PREFIX.toString() to stringResource(R.string.magic_send_note),
                                "+" to stringResource(R.string.magic_create_event),
                                "?" to stringResource(R.string.magic_find_app),
                            ).forEach { (key, description) -> MagicKeyRow(key, description) }
                            Text(stringResource(R.string.message_behavior_onboarding), color = Muted, fontSize = Dimens.sp12)
                        }
                        2 -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
                            Text(stringResource(R.string.todo_onboarding_intro), fontSize = Dimens.sp18)
                            OnboardingPoint(Icons.Default.Swipe, stringResource(R.string.swipe_widget), stringResource(R.string.swipe_widget_description))
                            OnboardingPoint(Icons.Default.TouchApp, stringResource(R.string.tap_widget), stringResource(R.string.tap_widget_description))
                            OnboardingPoint(Icons.Default.IosShare, stringResource(R.string.take_list_with_you), stringResource(R.string.take_list_with_you_description))
                            OnboardingPoint(Icons.Default.CheckCircle, stringResource(R.string.keep_context), stringResource(R.string.keep_context_description))
                        }
                        3 -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
                            Text(stringResource(R.string.search_onboarding_intro), fontSize = Dimens.sp18)
                            OnboardingPoint(Icons.Default.Language, stringResource(R.string.links_stay_links), stringResource(R.string.links_stay_links_description))
                            OnboardingPoint(Icons.Default.History, stringResource(R.string.recent_activity), stringResource(R.string.recent_activity_description))
                            OnboardingPoint(Icons.Default.AutoAwesome, stringResource(R.string.ai_optional), stringResource(R.string.ai_optional_description, appName))
                            OutlinedButton(onClick = { pickingAi = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AutoAwesome, null)
                                Text(
                                    store.preferredAiPackage?.let { stringResource(R.string.ai_app_selected, actions.appLabel(it)) }
                                        ?: stringResource(R.string.choose_an_ai_app),
                                    Modifier.padding(start = Dimens.dp8),
                                )
                            }
                            Text(stringResource(R.string.ai_optional_first_use, appName), color = Muted, fontSize = Dimens.sp12)
                            OnboardingPoint(Icons.Default.PhotoLibrary, stringResource(R.string.media_filenames), stringResource(R.string.media_filenames_description))
                            OnboardingPoint(Icons.Default.FolderOpen, stringResource(R.string.choose_scope), stringResource(R.string.choose_scope_description))
                            OnboardingPoint(Icons.Default.Security, stringResource(R.string.never_sent_to_us), stringResource(R.string.never_sent_to_us_description, appName))
                        }
                        4 -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
                            Text(stringResource(R.string.assistant_onboarding_intro, appName), fontSize = Dimens.sp18)
                            OnboardingPoint(Icons.Default.Keyboard, stringResource(R.string.keyboard_first), stringResource(R.string.keyboard_first_description))
                            OnboardingPoint(Icons.Default.TouchApp, stringResource(R.string.deliberate_choice), stringResource(R.string.deliberate_choice_description))
                            OnboardingPoint(Icons.Default.PrivacyTip, stringResource(R.string.no_screen_inspection), stringResource(R.string.no_screen_inspection_description))
                            OnboardingPoint(Icons.Default.Sms, stringResource(R.string.direct_sms), stringResource(R.string.direct_sms_onboarding_description, appName))
                            Button(
                                onClick = { assistantRoleSettings.launch(actions.assistantRoleSelectionIntent()) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Assistant, null)
                                Text(
                                    stringResource(if (assistantRoleHeld) R.string.manage_mink_assistant else R.string.choose_mink_assistant),
                                    Modifier.padding(start = Dimens.dp8),
                                )
                            }
                            Text(
                                stringResource(if (assistantRoleHeld) R.string.assistant_active_description else R.string.assistant_inactive_description),
                                color = Muted,
                                fontSize = Dimens.sp13,
                            )
                            Text(stringResource(R.string.choose_message_behavior), fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = { pickingMessaging = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null)
                                Column(Modifier.padding(start = Dimens.dp8).weight(1f)) {
                                    Text(stringResource(R.string.integrated_messaging_app_onboarding))
                                    Text(
                                        store.preferredMessagingPackage?.let(actions::appLabel)
                                            ?: stringResource(R.string.system_messages),
                                        color = Muted,
                                        fontSize = Dimens.sp12,
                                    )
                                }
                            }
                            Text(
                                stringResource(R.string.integrated_messaging_app_onboarding_description),
                                color = Muted,
                                fontSize = Dimens.sp12,
                            )
                            SettingsSwitchRow(
                                title = stringResource(R.string.send_messages_automatically),
                                subtitle = stringResource(R.string.send_messages_automatically_onboarding_description),
                                checked = store.sendMessagesAutomatically,
                                onCheckedChange = store::updateSendMessagesAutomatically,
                            )
                        }
                        5 -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
                            Text(stringResource(R.string.mink_day_onboarding_intro), fontSize = Dimens.sp18)
                            OnboardingPoint(Icons.Default.Pets, stringResource(R.string.six_gentle_states), stringResource(R.string.six_gentle_states_description))
                            OnboardingPoint(Icons.Default.Tune, stringResource(R.string.tracked_apps_and_goal), stringResource(R.string.tracked_apps_and_goal_description))
                            OnboardingPoint(Icons.Default.Security, stringResource(R.string.calculated_on_device), stringResource(R.string.calculated_on_device_description, appName))
                            OnboardingPoint(Icons.Default.VisibilityOff, stringResource(R.string.completely_optional), stringResource(R.string.completely_optional_description))
                        }
                        6 -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
                            Text(stringResource(R.string.spaces_onboarding_intro), fontSize = Dimens.sp18)
                            OnboardingPoint(Icons.Default.Forum, stringResource(R.string.conversations), stringResource(R.string.conversations_onboarding_description))
                            OnboardingPoint(Icons.Default.Widgets, stringResource(R.string.widget_page), stringResource(R.string.widget_page_onboarding_description))
                            OnboardingPoint(Icons.Default.Pets, stringResource(R.string.mink_day), stringResource(R.string.mink_day_onboarding_short_description))
                            OnboardingPoint(Icons.Default.Apps, stringResource(R.string.six_shortcuts_any_apps), stringResource(R.string.six_shortcuts_any_apps_description))
                            OnboardingPoint(Icons.Default.DragIndicator, stringResource(R.string.arrange_grid), stringResource(R.string.arrange_grid_description))
                            OnboardingPoint(Icons.Default.PrivacyTip, stringResource(R.string.active_and_local), stringResource(R.string.active_and_local_description, appName))
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
                            Text(stringResource(R.string.permissions_onboarding_intro, appName), fontSize = Dimens.sp18)
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Dimens.dp16),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Contacts, null, tint = Rust)
                                Column(Modifier.padding(start = Dimens.dp12)) {
                                    Text(stringResource(R.string.contacts), fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.contacts_onboarding_description), color = Muted, fontSize = Dimens.sp13)
                                }
                            }
                            OnboardingPoint(Icons.Default.Phone, stringResource(R.string.calls), stringResource(R.string.calls_onboarding_description))
                            OnboardingPoint(Icons.Default.Sms, stringResource(R.string.direct_sms), stringResource(R.string.direct_sms_onboarding_permission_description))
                            OnboardingPoint(Icons.Default.Lock, stringResource(R.string.double_tap_lock), stringResource(R.string.double_tap_lock_onboarding_description))
                            OnboardingPoint(Icons.Default.PhotoLibrary, stringResource(R.string.media), stringResource(R.string.media_onboarding_description))
                            OnboardingPoint(Icons.Default.SystemUpdateAlt, stringResource(R.string.github_update_checks), stringResource(R.string.github_updates_onboarding_description, appName))
                            OnboardingPoint(Icons.Default.Forum, stringResource(R.string.conversation_access), stringResource(R.string.conversation_access_onboarding_description))
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                OnboardingPoint(
                                    Icons.Default.AdminPanelSettings,
                                    stringResource(R.string.restricted_settings_onboarding_title),
                                    stringResource(R.string.restricted_settings_onboarding_description),
                                )
                            }
                            OnboardingPoint(Icons.Default.Pets, stringResource(R.string.mink_day_usage), stringResource(R.string.mink_day_usage_onboarding_description))
                            Text(stringResource(R.string.document_folder_scope_description, appName), color = Muted, fontSize = Dimens.sp13)
                            Text(stringResource(R.string.permission_sequence_description), color = Muted)
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(vertical = Dimens.dp16),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page > 0) {
                    OutlinedButton(onClick = { page-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        Text(stringResource(R.string.back), Modifier.padding(start = Dimens.dp6))
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Button(
                    onClick = { if (page < titles.lastIndex) page++ else onFinish() },
                    modifier = if (page > 0) Modifier.weight(1f) else Modifier,
                ) {
                    Text(stringResource(if (page < titles.lastIndex) R.string.next else R.string.finish_setup))
                    if (page < titles.lastIndex) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.padding(start = Dimens.dp6))
                    }
                }
            }
        }
    }
    if (pickingAi) {
        AppPickerDialog(
            title = stringResource(R.string.choose_ai_app_title),
            apps = curatedAiApps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !aiAppsLoaded,
            emptyMessage = stringResource(R.string.no_curated_ai_apps_short),
            extraActionLabel = stringResource(R.string.other_compatible_app),
            onExtraAction = { pickingAi = false; pickingAllAi = true },
            onApp = { store.setPreferredAiApp(it.packageName); pickingAi = false },
            onReset = { store.resetPreferredAiApp(); pickingAi = false },
            resetLabel = stringResource(R.string.choose_on_first_use),
            onDismiss = { pickingAi = false },
        )
    }
    if (pickingAllAi) {
        AppPickerDialog(
            title = stringResource(R.string.other_compatible_apps),
            apps = allAiApps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !allAiAppsLoaded,
            onApp = { store.setPreferredAiApp(it.packageName); pickingAllAi = false },
            onDismiss = { pickingAllAi = false },
        )
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
            onDismiss = { pickingMessaging = false },
        )
    }
}

@Composable
internal fun ShortcutSetupDialog(store: LauncherStore, actions: DeviceActions, onFinish: () -> Unit) {
    val context = LocalContext.current
    var pickingShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var installedAppsLoaded by remember { mutableStateOf(false) }
    var appListRefresh by remember { mutableIntStateOf(0) }
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val installedApps by produceState<List<LauncherAppTarget>>(
        initialValue = emptyList(),
        appListRefresh,
        launcherAppsRevision,
    ) {
        value = withContext(Dispatchers.IO) { actions.installedApps() }
        installedAppsLoaded = true
    }
    DisposableEffect(context) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                actions.invalidateInstalledApps()
                appListRefresh++
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.82f),
            shape = RoundedCornerShape(Dimens.dp30),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().padding(Dimens.dp26)) {
                Icon(Icons.Default.Apps, null, Modifier.size(Dimens.dp46), tint = Rust)
                Text(
                    stringResource(R.string.choose_your_shortcut_apps),
                    fontSize = Dimens.sp28,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = Dimens.dp16),
                )
                Text(
                    stringResource(R.string.shortcut_setup_description),
                    color = Muted,
                    modifier = Modifier.padding(top = Dimens.dp10, bottom = Dimens.dp18),
                )
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
                ) {
                    configurableShortcuts.forEach { shortcut ->
                        val targetKey = store.shortcutTargets[shortcut]
                        ShortcutAssignmentRow(
                            shortcut = shortcut,
                            targetKey = targetKey,
                            actions = actions,
                            subtitle = when {
                                targetKey != null -> actions.launcherAppLabel(targetKey)
                                shortcut in store.confirmedShortcutChoices -> stringResource(
                                    R.string.shortcut_default,
                                    shortcut.displayLabel(),
                                )
                                else -> stringResource(
                                    R.string.choose_app_or_keep_default,
                                    shortcut.displayLabel(),
                                )
                            },
                        ) { pickingShortcut = shortcut }
                    }
                    if (!store.hasConfirmedAllShortcutChoices()) {
                        OutlinedButton(
                            onClick = store::confirmSystemDefaultsForUnselectedShortcuts,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Restore, null)
                            Text(stringResource(R.string.keep_built_in_defaults_remaining), Modifier.padding(start = Dimens.dp8))
                        }
                    }
                }
                Button(
                    onClick = onFinish,
                    enabled = store.hasConfirmedAllShortcutChoices(),
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp18),
                ) {
                    Text(stringResource(R.string.finish_launcher_setup))
                }
            }
        }
    }

    pickingShortcut?.let { shortcut ->
        val showSamsungWeatherGuide = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        LauncherAppPickerDialog(
            title = stringResource(R.string.choose_app_for_shortcut, shortcut.displaySlotLabel()),
            apps = installedApps,
            selected = setOfNotNull(store.shortcutTargets[shortcut]),
            actions = actions,
            loading = !installedAppsLoaded,
            supportingText = if (showSamsungWeatherGuide) stringResource(R.string.samsung_weather_guide) else null,
            supportingActionLabel = if (showSamsungWeatherGuide) stringResource(R.string.open_apps_settings) else null,
            onSupportingAction = actions::openInstalledAppsSettings,
            onApp = {
                store.assignShortcut(shortcut, it.selectionKey)
                pickingShortcut = null
            },
            onReset = {
                store.resetShortcut(shortcut)
                pickingShortcut = null
            },
            resetLabel = stringResource(R.string.restore_shortcut_default, shortcut.displayLabel()),
            onDismiss = { pickingShortcut = null },
        )
    }
}

@Composable
internal fun OnboardingPoint(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Rust, modifier = Modifier.size(Dimens.dp24))
        Column(Modifier.padding(start = Dimens.dp12)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = Dimens.sp14)
        }
    }
}

@Composable
internal fun MagicKeyRow(key: String, description: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp14)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Dimens.dp11),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Dimens.dp36).clip(CircleShape).background(LightInk), contentAlignment = Alignment.Center) {
            Text(key, color = LightPaper, fontSize = Dimens.sp20, fontWeight = FontWeight.Black)
        }
        Text(description, Modifier.padding(start = Dimens.dp12), fontWeight = FontWeight.SemiBold)
    }
}
