@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun FeatureUpdateDialog(
    onOpenSettings: () -> Unit,
    onReviewTutorial: () -> Unit,
    onNotNow: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        onDismissRequest = onNotNow,
        icon = { Icon(Icons.Default.Pets, null, tint = Rust) },
        title = { Text(stringResource(R.string.whats_new)) },
        text = {
            Column(
                Modifier.heightIn(max = Dimens.dp560).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp14),
            ) {
                Text(stringResource(R.string.your_shortcuts_are_yours), fontSize = Dimens.sp18, fontWeight = FontWeight.Bold)
                UpdatePoint(Icons.Default.Apps, stringResource(R.string.six_generic_app_slots), stringResource(R.string.six_generic_app_slots_description, appName))
                UpdatePoint(Icons.Default.Palette, stringResource(R.string.icons_that_belong_on_home), stringResource(R.string.icons_that_belong_on_home_description))
                UpdatePoint(Icons.Default.Restore, stringResource(R.string.defaults_are_always_there), stringResource(R.string.defaults_are_always_there_description))
                UpdatePoint(Icons.Default.DragIndicator, stringResource(R.string.your_layout_stays_put), stringResource(R.string.your_layout_stays_put_description))
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
        onDismissRequest = onSkip,
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
        onDismissRequest = onDismiss,
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
internal fun OnboardingDialog(store: LauncherStore, actions: DeviceActions, onFinish: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    var page by rememberSaveable { mutableIntStateOf(0) }
    var pickingAi by remember { mutableStateOf(false) }
    var pickingAllAi by remember { mutableStateOf(false) }
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
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.92f).fillMaxHeight(.82f),
            shape = RoundedCornerShape(Dimens.dp30),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().padding(Dimens.dp26)) {
                Icon(icons[page], null, Modifier.size(Dimens.dp46), tint = Rust)
                Text(titles[page], fontSize = Dimens.sp28, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = Dimens.dp16))
                Spacer(Modifier.height(Dimens.dp18))
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
                                "$" to stringResource(R.string.magic_send_note),
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
                            MessageSendModeChooser(store.messageSendMode, store::updateMessageSendMode)
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
                            OnboardingPoint(Icons.Default.Forum, stringResource(R.string.conversation_access), stringResource(R.string.conversation_access_onboarding_description))
                            OnboardingPoint(Icons.Default.Pets, stringResource(R.string.mink_day_usage), stringResource(R.string.mink_day_usage_onboarding_description))
                            Text(stringResource(R.string.document_folder_scope_description, appName), color = Muted, fontSize = Dimens.sp13)
                            Text(stringResource(R.string.permission_sequence_description), color = Muted)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Dimens.dp6)) {
                        repeat(titles.size) { index ->
                            Box(Modifier.size(if (index == page) Dimens.dp22 else Dimens.dp8, Dimens.dp8).clip(CircleShape).background(if (index == page) Rust else Sage))
                        }
                    }
                    if (page > 0) TextButton(onClick = { page-- }) { Text(stringResource(R.string.back)) }
                    Button(
                        onClick = { if (page < titles.lastIndex) page++ else onFinish() },
                    ) {
                        Text(stringResource(if (page < titles.lastIndex) R.string.next else R.string.finish_setup))
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
}

@Composable
internal fun ShortcutSetupDialog(store: LauncherStore, actions: DeviceActions, onFinish: () -> Unit) {
    val context = LocalContext.current
    var pickingShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var installedAppsLoaded by remember { mutableStateOf(false) }
    var appListRefresh by remember { mutableIntStateOf(0) }
    val installedApps by produceState<List<LaunchableApp>>(initialValue = emptyList(), appListRefresh) {
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
            Modifier.fillMaxWidth(.92f).fillMaxHeight(.82f),
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
                        val packageName = store.shortcutPackages[shortcut]
                        ShortcutAssignmentRow(
                            shortcut = shortcut,
                            packageName = packageName,
                            actions = actions,
                            subtitle = when {
                                packageName != null -> actions.appLabel(packageName)
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
        AppPickerDialog(
            title = stringResource(R.string.choose_app_for_shortcut, shortcut.displaySlotLabel()),
            apps = installedApps,
            selected = setOfNotNull(store.shortcutPackages[shortcut]),
            loading = !installedAppsLoaded,
            supportingText = if (showSamsungWeatherGuide) stringResource(R.string.samsung_weather_guide) else null,
            supportingActionLabel = if (showSamsungWeatherGuide) stringResource(R.string.open_apps_settings) else null,
            onSupportingAction = actions::openInstalledAppsSettings,
            onApp = {
                store.assignShortcut(shortcut, it.packageName)
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
