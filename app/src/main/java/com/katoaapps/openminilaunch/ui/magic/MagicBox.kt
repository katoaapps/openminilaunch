@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.magic

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.apps.AppSearchMatcher
import com.katoaapps.openminilaunch.features.apps.launcherDiscoveryLabel
import com.katoaapps.openminilaunch.features.apps.launcherLibraryTargets
import com.katoaapps.openminilaunch.features.ai.*
import com.katoaapps.openminilaunch.features.calendar.*
import com.katoaapps.openminilaunch.features.calendar.engine.CalendarPhraseEngine
import com.katoaapps.openminilaunch.features.localization.SupportedAppLanguages
import com.katoaapps.openminilaunch.features.calendar.model.CalendarParseRequest
import com.katoaapps.openminilaunch.features.calendar.model.CalendarParseResult
import com.katoaapps.openminilaunch.features.conversations.*
import com.katoaapps.openminilaunch.features.files.*
import com.katoaapps.openminilaunch.features.magic.*
import com.katoaapps.openminilaunch.features.messaging.*
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*
import com.katoaapps.openminilaunch.platform.hasMediaReadAccess
import com.katoaapps.openminilaunch.platform.isPermanentlyDenied
import com.katoaapps.openminilaunch.platform.mediaPermissionPermanentlyDenied
import com.katoaapps.openminilaunch.platform.mediaReadPermissions
import com.katoaapps.openminilaunch.platform.supportsDirectCalls
import com.katoaapps.openminilaunch.platform.supportsDirectSms
import com.katoaapps.openminilaunch.ui.wellbeing.rememberMinkAppAccessState

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MagicBoxContent(
    store: LauncherStore,
    actions: DeviceActions,
    sessionState: MagicBoxSessionState? = null,
    modifier: Modifier = Modifier,
    collapsedModifier: Modifier = Modifier,
    keyboardInputEnabled: Boolean = true,
    initiallyExpanded: Boolean = false,
    showSoftwareKeyboardOnStart: Boolean = false,
    autoOpenSoftwareKeyboardOnHome: Boolean = false,
    captureActivityCommandHotkeys: Boolean = false,
    homeRequestToken: Int = 0,
    onTodoAdded: (String, onAnimationFinished: () -> Unit) -> Unit = { _, finished ->
        finished()
    },
    onOpenTodos: (() -> Unit)? = null,
    onOpenKeyboardSettings: () -> Unit = {},
    onOpenTotalScreen: (() -> Unit)? = null,
    onSessionComplete: () -> Unit = {},
    onBubblePublished: () -> Unit = {},
    appBubbleHost: MagicAppBubbleHost = MagicAppBubbleHost.HOME,
    appAccessState: MinkAppAccessState? = null,
) {
    val magicBoxMinimumHeight = Dimens.dp64
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val useDirectHardwareInput = hasUsableHardwareKeyboard(
        configuration.keyboard,
        configuration.hardKeyboardHidden,
    )
    val useMinkKeyboard = store.launcherKeyboardPreferences.useMinkKeyboard &&
        keyboardInputEnabled &&
        !useDirectHardwareInput
    val shouldAutoOpenOnHome = shouldAutoOpenSoftwareKeyboard(
        autoOpenSoftwareKeyboardOnHome,
        configuration.keyboard,
        configuration.hardKeyboardHidden,
    )
    val fileSearchRepository = remember { FileSearchRepository(context.applicationContext) }
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val launcherShortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val localAppAccessState = if (appAccessState == null) rememberMinkAppAccessState(store) else null
    val effectiveAppAccessState = appAccessState ?: checkNotNull(localAppAccessState).value
    val editorSession = sessionState ?: rememberMagicBoxSessionState(initiallyExpanded)
    var text by editorSession.textState
    val initialHardwareKeyCorrection = remember { InitialHardwareKeyCorrection() }
    var selectedRecipient by editorSession.selectedRecipientState
    var route by editorSession.routeState
    var expanded by editorSession.expandedState
    var showKeyboardWhileCollapsed by remember {
        mutableStateOf(shouldAutoOpenOnHome && !useMinkKeyboard && !initiallyExpanded)
    }
    var hasContacts by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasMediaAccess by remember { mutableStateOf(hasMediaReadAccess(context)) }
    var showFileScopeChoice by remember { mutableStateOf(false) }
    var showAiPicker by remember { mutableStateOf(false) }
    var showAllAiApps by remember { mutableStateOf(false) }
    var pendingAiQuery by remember { mutableStateOf<String?>(null) }
    var pendingMessagingChoice by remember { mutableStateOf<MessageDraft?>(null) }
    var showNoteDeleteConfirmation by remember { mutableStateOf(false) }
    var showMessageDiscardConfirmation by remember { mutableStateOf(false) }
    var showCommandDiscardConfirmation by remember { mutableStateOf(false) }
    var todoCompletionPending by remember { mutableStateOf(false) }
    var pendingCalendarReview by remember { mutableStateOf<CalendarParseResult?>(null) }
    val callFlow = rememberMagicCallFlow(
        actions = actions,
        callAutomatically = store.callAutomatically,
        preferredCallingPackage = store.preferredCallingPackage,
        onSessionComplete = onSessionComplete,
    )
    val smsFlow = rememberMagicSmsFlow(actions, onSessionComplete)
    var aiProvidersLoaded by remember { mutableStateOf(false) }
    val aiProviders by produceState<List<AiProviderOption>>(
        initialValue = emptyList(),
        key1 = showAiPicker,
        key2 = launcherAppsRevision,
    ) {
        if (showAiPicker) {
            aiProvidersLoaded = false
            value = withContext(Dispatchers.IO) { actions.aiProviderOptions() }
            aiProvidersLoaded = true
        }
    }
    var allAiAppsLoaded by remember { mutableStateOf(false) }
    val allAiApps by produceState<List<LaunchableApp>>(
        initialValue = emptyList(),
        key1 = showAllAiApps,
        key2 = launcherAppsRevision,
    ) {
        if (showAllAiApps) {
            allAiAppsLoaded = false
            value = withContext(Dispatchers.IO) { actions.compatibleAiApps() }
            allAiAppsLoaded = true
        }
    }
    val messagingProviders by produceState(
        initialValue = MessagingOptionsLoadState(),
        key1 = pendingMessagingChoice,
        key2 = launcherAppsRevision,
    ) {
        if (pendingMessagingChoice != null) {
            value = MessagingOptionsLoadState(
                options = withContext(Dispatchers.IO) { actions.messagingProviderOptions() },
                loaded = true,
            )
        }
    }
    DisposableEffect(context) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                hasMediaAccess = hasMediaReadAccess(context)
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasContacts = granted
        if (!granted && isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)) {
            actions.openAppPermissionSettings()
        }
    }
    val focusRequester = remember { FocusRequester() }
    val armedFocusRequester = remember { FocusRequester() }
    val inputSurfaceInteractionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var focusRequestSerial by remember { mutableIntStateOf(0) }
    var focusRequestShowsKeyboard by remember { mutableStateOf(true) }
    var textFieldPlaced by remember { mutableStateOf(false) }
    var armedTargetPlaced by remember { mutableStateOf(false) }

    fun refocus(showSoftwareKeyboard: Boolean = !useMinkKeyboard) {
        focusRequestShowsKeyboard = showSoftwareKeyboard
        focusRequestSerial += 1
    }

    fun chooseMessagingApp(draft: MessageDraft) {
        val opened = actions.chooseMessagingApp(draft.body)
        if (!opened) {
            Toast.makeText(
                context,
                context.getString(R.string.no_compatible_messaging_app),
                Toast.LENGTH_LONG,
            ).show()
            refocus()
        }
    }

    fun openMessagingProvider(draft: MessageDraft, providerPackage: String?) {
        val result = actions.openPreferredMessageDraft(
            draft.recipient,
            draft.body,
            providerPackage,
        )
        when (result) {
            PreferredMessageDraftResult.OPENED -> Unit
            PreferredMessageDraftResult.OPENED_WITH_RECIPIENT_PICKER -> Toast.makeText(
                context,
                context.getString(
                    R.string.choose_recipient_in_preferred_app,
                    draft.recipient.displayName,
                ),
                Toast.LENGTH_LONG,
            ).show()
            PreferredMessageDraftResult.FALLBACK_OPENED -> Toast.makeText(
                context,
                context.getString(R.string.preferred_messaging_fallback),
                Toast.LENGTH_LONG,
            ).show()
            PreferredMessageDraftResult.FAILED -> Toast.makeText(
                context,
                context.getString(R.string.no_compatible_messaging_app),
                Toast.LENGTH_LONG,
            ).show()
        }
        if (result == PreferredMessageDraftResult.FAILED) refocus()
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasMediaAccess = hasMediaReadAccess(context)
        if (!hasMediaAccess && mediaPermissionPermanentlyDenied(context)) {
            actions.openAppPermissionSettings()
        }
        showFileScopeChoice = true
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            store.addSearchFolder(it.toString(), fileSearchRepository.folderLabel(it))
            fileSearchRepository.invalidateFolders()
        }
    }
    val magicResultsScroll = rememberScrollState()
    val parsedInput = parseMagicBoxInput(text.text, route)
    val searchTerm = parsedInput.searchTerm
    val calendarLanguageTag = SupportedAppLanguages.activeLanguageTag(context)
    val calendarPayload = text.text.trim().takeIf { route == MagicBoxRoute.EVENT }.orEmpty()
    val calendarParseResult = remember(calendarPayload, calendarLanguageTag) {
        calendarPayload.takeIf(String::isNotBlank)?.let { payload ->
            CalendarPhraseEngine.parse(
                CalendarParseRequest(
                    text = payload,
                    defaultTitle = context.getString(R.string.new_event),
                    languageTag = calendarLanguageTag,
                ),
            )
        }
    }
    val canSearchContacts = hasContacts || store.demoModeEnabled
    val contactResults = remember(route, searchTerm, canSearchContacts, store.demoModeEnabled) {
        if (
            (route == MagicBoxRoute.MESSAGE || route == MagicBoxRoute.CALL) &&
            canSearchContacts &&
            selectedRecipient == null
        ) {
            actions.searchContacts(searchTerm, useDemoData = store.demoModeEnabled).take(5)
        } else emptyList()
    }
    val recentConversations = rememberRecentConversationResults(
        route = route,
        query = searchTerm,
        hasSelectedRecipient = selectedRecipient != null,
        launcherShortcutsRevision = launcherShortcutsRevision,
        sendAutomatically = store.sendMessagesAutomatically,
        preferredMessagingPackage = store.preferredMessagingPackage,
        demoModeEnabled = store.demoModeEnabled,
        actions = actions,
    )
    val visibleRecentConversations = remember(recentConversations, effectiveAppAccessState) {
        if (effectiveAppAccessState.isResolved) {
            recentConversations.filterNot(effectiveAppAccessState::isPaused)
        } else {
            recentConversations
        }
    }
    val alwaysVisibleTargetKeys = store.pinnedLauncherSelectionKeys
    val appSearchMatcher = remember(actions) { AppSearchMatcher(actions::appLabel) }
    val appResults = remember(
        route,
        searchTerm,
        store.demoModeEnabled,
        launcherAppsRevision,
        launcherShortcutsRevision,
        alwaysVisibleTargetKeys,
        store.includeAppShortcutsInDiscovery,
        appSearchMatcher,
    ) {
        if (store.demoModeEnabled) {
            emptyList()
        } else if (route == MagicBoxRoute.APP && searchTerm.isNotBlank()) {
            val targets = launcherLibraryTargets(
                apps = actions.installedApps(),
                alwaysVisibleTargetKeys = alwaysVisibleTargetKeys,
                appShortcuts = actions.installedShortcuts(),
                includeAppShortcuts = store.includeAppShortcutsInDiscovery,
                resolveTarget = actions::resolveLauncherSelection,
            )
            appSearchMatcher.rank(targets, searchTerm).take(5).map { it.target }
        } else emptyList()
    }
    val visibleAppResults = remember(appResults, effectiveAppAccessState) {
        if (effectiveAppAccessState.isResolved) {
            appResults.filterNot(effectiveAppAccessState::isPaused)
        } else {
            appResults
        }
    }
    val plainQuery = parsedInput.plainQuery
    val aiQuery = if (route == MagicBoxRoute.ASSISTANT) text.text.trim() else plainQuery
    val noteMode = expanded && route == MagicBoxRoute.NOTE
    val hasTextDraft = expanded && hasMagicBoxDraftText(text.text)
    val hasNoteDraft = noteMode && hasTextDraft
    val hasMessageDraft = expanded &&
        route == MagicBoxRoute.MESSAGE &&
        selectedRecipient != null &&
        text.text.isNotBlank()
    val fileSearchState = rememberMagicFileSearchState(
        query = plainQuery,
        store = store,
        repository = fileSearchRepository,
        hasMediaAccess = hasMediaAccess,
        resultsScroll = magicResultsScroll,
    )
    val actionVisuals = when (route) {
        MagicBoxRoute.MESSAGE -> MagicActionVisuals(MagicTextColor, Icons.AutoMirrored.Filled.Send)
        MagicBoxRoute.CALL -> MagicActionVisuals(MagicCallColor, Icons.Default.Phone)
        MagicBoxRoute.TODO -> MagicActionVisuals(MagicTodoColor, Icons.Default.Checklist)
        MagicBoxRoute.NOTE -> MagicActionVisuals(MagicNoteColor, Icons.AutoMirrored.Filled.NoteAdd)
        MagicBoxRoute.EVENT -> if (
            calendarParseResult is CalendarParseResult.NeedsReview ||
            calendarParseResult is CalendarParseResult.Invalid
        ) {
            MagicActionVisuals(MaterialTheme.colorScheme.error, Icons.Default.WarningAmber)
        } else {
            MagicActionVisuals(MagicEventColor, Icons.Default.Event)
        }
        MagicBoxRoute.APP -> MagicActionVisuals(MagicAppColor, Icons.Default.Apps)
        MagicBoxRoute.ASSISTANT -> MagicActionVisuals(MinkBlack, Icons.Default.AutoAwesome)
        MagicBoxRoute.SEARCH -> MagicActionVisuals(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Search,
        )
    }
    val actionContentColor = when (route) {
        MagicBoxRoute.TODO -> LightInk
        MagicBoxRoute.SEARCH -> MaterialTheme.colorScheme.onPrimary
        else -> MinkWhite
    }
    fun clearCommand() {
        fileSearchState.clear()
        initialHardwareKeyCorrection.clear()
        text = TextFieldValue()
        selectedRecipient = null
        route = MagicBoxRoute.SEARCH
        pendingCalendarReview = null
    }

    fun activateRoute(
        nextRoute: MagicBoxRoute,
        showSoftwareKeyboard: Boolean = !useMinkKeyboard,
    ) {
        clearCommand()
        route = nextRoute
        expanded = true
        refocus(showSoftwareKeyboard = showSoftwareKeyboard)
    }

    fun updateEditorValue(value: TextFieldValue, correctHardwareInput: Boolean) {
        val correctedValue = if (correctHardwareInput) {
            initialHardwareKeyCorrection.correct(value)
        } else {
            value
        }
        val trigger = consumeMagicBoxTrigger(
            previousText = text.text,
            updatedText = correctedValue.text,
            routeSelectionEnabled = selectedRecipient == null,
        )
        if (trigger != null) {
            initialHardwareKeyCorrection.clear()
            route = trigger.route
            selectedRecipient = null
            val selectionStart = (correctedValue.selection.start - 1)
                .coerceIn(0, trigger.content.length)
            val selectionEnd = (correctedValue.selection.end - 1)
                .coerceIn(0, trigger.content.length)
            text = TextFieldValue(
                text = trigger.content,
                selection = TextRange(selectionStart, selectionEnd),
            )
        } else {
            text = correctedValue
        }
        if (!expanded && correctedValue.text.isNotEmpty()) expanded = true
    }

    fun beginCollapsedHardwareInput(typedText: String, keyCode: Int) {
        if (expanded) return
        clearCommand()
        val hotkeyRoute = typedText.singleOrNull()?.let(MagicBoxRoute::fromTrigger)
        if (hotkeyRoute != null) {
            route = hotkeyRoute
        } else {
            initialHardwareKeyCorrection.begin(typedText, keyCode)
            text = TextFieldValue(typedText, selection = TextRange(typedText.length))
        }
        expanded = true
        refocus(showSoftwareKeyboard = false)
    }

    fun requestClearMessageDraft() {
        if (text.text.isBlank()) {
            clearCommand()
            refocus()
        } else {
            keyboard?.hide()
            showMessageDiscardConfirmation = true
        }
    }

    fun dismiss() {
        clearCommand()
        keyboard?.hide()
        expanded = false
        onSessionComplete()
    }

    fun openTodos() {
        val callback = onOpenTodos ?: return
        dismiss()
        callback()
    }

    fun beginTodoCompletion(todo: String) {
        clearCommand()
        keyboard?.hide()
        expanded = false
        todoCompletionPending = true
        onTodoAdded(todo) {
            todoCompletionPending = false
            onSessionComplete()
        }
    }

    fun completeAppHandoff(target: LauncherAppTarget) {
        store.addSearchQuery("?${target.label}")
        clearCommand()
        keyboard?.hide()
        expanded = false
    }

    val appBubbleFlow = rememberMagicAppBubbleFlow(
        store = store,
        actions = actions,
        host = appBubbleHost,
        onBubblePublished = { target ->
            completeAppHandoff(target)
            onBubblePublished()
        },
        onAppOpenedNormally = { target ->
            completeAppHandoff(target)
            onSessionComplete()
        },
        onRefocus = { refocus() },
    )

    fun requestDismiss() {
        when {
            hasNoteDraft -> {
                keyboard?.hide()
                showNoteDeleteConfirmation = true
            }
            hasTextDraft -> {
                keyboard?.hide()
                showCommandDiscardConfirmation = true
            }
            else -> dismiss()
        }
    }

    fun collapseForDialog() {
        clearCommand()
        keyboard?.hide()
        expanded = false
    }

    fun completeAiHandoff(
        query: String,
        packageName: String,
        result: AiHandoffResult,
    ) {
        if (result == AiHandoffResult.COPIED_AND_OPENED) {
            Toast.makeText(
                context,
                context.getString(
                    R.string.ai_query_copied_paste_in_app,
                    actions.appLabel(packageName),
                ),
                Toast.LENGTH_LONG,
            ).show()
        }
        store.addSearchQuery(query)
        dismiss()
    }

    fun submitAi() {
        val query = aiQuery
        if (query.isBlank()) return
        val preferredPackage = store.preferredAiPackage
        if (!preferredPackage.isNullOrBlank()) {
            val result = actions.handoffQueryToAiApp(query, preferredPackage)
            if (result.succeeded) {
                completeAiHandoff(query, preferredPackage, result)
                return
            }
            store.resetPreferredAiApp()
        }
        pendingAiQuery = query
        showAiPicker = true
        keyboard?.hide()
    }

    fun selectAiPackage(packageName: String) {
        val query = pendingAiQuery ?: return
        val result = actions.handoffQueryToAiApp(query, packageName)
        if (!result.succeeded) return

        store.setPreferredAiApp(packageName)
        showAiPicker = false
        showAllAiApps = false
        pendingAiQuery = null
        completeAiHandoff(query, packageName, result)
    }

    fun selectAiApp(app: LaunchableApp) {
        selectAiPackage(app.packageName)
    }

    fun dismissAiPickers() {
        showAiPicker = false
        showAllAiApps = false
        pendingAiQuery = null
        refocus()
    }

    fun selectContact(contact: ContactResult) {
        if (store.demoModeEnabled && route == MagicBoxRoute.CALL) {
            Toast.makeText(context, R.string.demo_preview_action, Toast.LENGTH_SHORT).show()
        } else if (route == MagicBoxRoute.CALL) {
            callFlow.requestConfirmation(contact.toCommunicationRecipient())
            collapseForDialog()
        } else {
            selectedRecipient = SelectedMessageRecipient.AddressRecipient(
                contact.toCommunicationRecipient(),
            )
            text = TextFieldValue()
            refocus()
        }
    }

    fun selectRecentConversation(conversation: LauncherShortcutTarget) {
        handleRecentConversationTap(
            context = context,
            shortcut = conversation,
            canSearchContacts = canSearchContacts,
            actions = actions,
            onShortcutDraft = { shortcut ->
                route = MagicBoxRoute.MESSAGE
                selectedRecipient = SelectedMessageRecipient.ConversationRecipient(shortcut)
                text = TextFieldValue()
                refocus()
            },
            onDraftRecipient = { contact, packageName ->
                route = MagicBoxRoute.MESSAGE
                selectedRecipient = SelectedMessageRecipient.AddressRecipient(
                    recipient = contact.toCommunicationRecipient(),
                    providerPackage = packageName,
                )
                text = TextFieldValue()
                refocus()
            },
            onDirectOpen = ::dismiss,
        )
    }

    fun selectApp(app: LauncherTarget) {
        if (store.demoModeEnabled) {
            Toast.makeText(context, R.string.demo_preview_action, Toast.LENGTH_SHORT).show()
        } else if (app is LauncherAppTarget && appBubbleFlow.opensAutomatically(app)) {
            appBubbleFlow.launchInBubble(app)
        } else if (actions.launchLauncherTarget(app)) {
            store.addSearchQuery("?${app.label}")
            dismiss()
        } else {
            val displayLabel = launcherDiscoveryLabel(app, actions::appLabel)
            Toast.makeText(
                context,
                context.getString(R.string.launcher_app_unavailable, displayLabel),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun showAppActions(target: LauncherAppTarget) {
        if (store.demoModeEnabled) {
            Toast.makeText(context, R.string.demo_preview_action, Toast.LENGTH_SHORT).show()
            return
        }
        keyboard?.hide()
        appBubbleFlow.showActions(target)
    }

    fun selectFirstCommandResult(): Boolean {
        when (route) {
            MagicBoxRoute.MESSAGE,
            MagicBoxRoute.CALL,
            -> {
                contactResults.firstOrNull()?.let {
                    selectContact(it)
                    return true
                }
                if (route == MagicBoxRoute.MESSAGE) {
                    visibleRecentConversations.firstOrNull()?.let {
                        selectRecentConversation(it)
                        return true
                    }
                }
            }
            MagicBoxRoute.APP -> visibleAppResults.firstOrNull()?.let {
                selectApp(it)
                return true
            }
            else -> Unit
        }
        return false
    }

    fun submit() {
        if (onOpenTodos != null && isOpenTodosCommand(text.text, route)) {
            openTodos()
            return
        }
        if (store.demoModeEnabled && route != MagicBoxRoute.TODO) {
            Toast.makeText(context, R.string.demo_preview_action, Toast.LENGTH_SHORT).show()
            return
        }
        if (selectFirstCommandResult()) return
        if (route == MagicBoxRoute.ASSISTANT) {
            submitAi()
            return
        }
        if (route == MagicBoxRoute.EVENT) {
            when (val result = calendarParseResult) {
                is CalendarParseResult.Success,
                is CalendarParseResult.NoTemporalPhrase -> {
                    if (actions.createEvent(checkNotNull(result).draft)) dismiss()
                }
                is CalendarParseResult.NeedsReview,
                is CalendarParseResult.Invalid -> {
                    pendingCalendarReview = result
                    keyboard?.hide()
                }
                null -> Unit
            }
            return
        }
        dispatchMagicCommand(
            route = route,
            rawText = text.text,
            selectedRecipient = selectedRecipient,
            store = store,
            actions = actions,
            onTodoAdded = ::beginTodoCompletion,
            onExternalDraftOpened = { keyboard?.hide() },
            onMessage = { draft, route ->
                val resolvedRoute = resolvedMessageSendRoute(
                    recipient = draft.recipient,
                    selectedConversationPackage = draft.conversationPackage,
                    defaultSmsPackage = actions.defaultMessagingPackage(),
                    sendAutomatically = store.sendMessagesAutomatically,
                    preferredPackage = store.preferredMessagingPackage,
                    fallbackRoute = route,
                )
                when (resolvedRoute) {
                    MessagingSendRoute.DIRECT_SMS -> {
                        collapseForDialog()
                        smsFlow.requestDirect(draft)
                    }
                    // A composer can be dismissed without sending. Keep the contact and body
                    // in Magic Mode just as note handoffs keep their unsaved text.
                    MessagingSendRoute.PREFERRED_DRAFT -> {
                        keyboard?.hide()
                        openMessagingProvider(
                            draft,
                            if (draft.providerRouteOverridden) {
                                draft.conversationPackage
                            } else {
                                draft.conversationPackage ?: store.preferredMessagingPackage
                            },
                        )
                    }
                    MessagingSendRoute.PROVIDER_PICKER -> {
                        keyboard?.hide()
                        pendingMessagingChoice = draft
                    }
                }
            },
            onConversationShortcutMessage = { draft ->
                keyboard?.hide()
                handleRecentConversationDraftSend(context, draft, actions) { refocus() }
            },
            onDismiss = ::dismiss,
        )
    }

    // Keep collapsed Home armed without focusing a text editor. This lets a physical
    // keyboard open Magic Mode with its first printable key without giving keyboard apps
    // an input connection (and therefore a suggestion toolbar) before typing begins.
    // The Home-keyboard preference still focuses the real field for touch-first devices.
    LaunchedEffect(
        keyboardInputEnabled,
        useMinkKeyboard,
        expanded,
        smsFlow.sentConfirmationVisible,
        showKeyboardWhileCollapsed,
        todoCompletionPending,
    ) {
        if (!keyboardInputEnabled) {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
        } else if (!expanded && !smsFlow.sentConfirmationVisible && !todoCompletionPending) {
            if (showKeyboardWhileCollapsed) {
                while (!textFieldPlaced) withFrameNanos { }
                withFrameNanos { }
                focusRequester.requestFocus()
                withFrameNanos { }
                if (!useMinkKeyboard) keyboard?.show() else keyboard?.hide()
            } else {
                while (!armedTargetPlaced) withFrameNanos { }
                withFrameNanos { }
                armedFocusRequester.requestFocus()
                keyboard?.hide()
            }
        } else {
            armedTargetPlaced = false
            if (expanded) showKeyboardWhileCollapsed = false
            if (expanded && focusRequestSerial == 0) {
                if (useMinkKeyboard) {
                    // Mink's keyboard writes directly to this editor, so the field still needs
                    // focus for an immediate cursor and selection even though Android's IME is
                    // intentionally blocked by MagicInputField.
                    while (!textFieldPlaced) withFrameNanos { }
                    withFrameNanos { }
                    focusRequester.requestFocus()
                    keyboard?.hide()
                } else {
                    while (!textFieldPlaced) withFrameNanos { }
                    withFrameNanos { }
                    focusRequester.requestFocus()
                    withFrameNanos { }
                    if (showSoftwareKeyboardOnStart || !useDirectHardwareInput) {
                        keyboard?.show()
                    } else {
                        keyboard?.hide()
                    }
                }
            }
        }
    }

    LaunchedEffect(homeRequestToken, shouldAutoOpenOnHome, keyboardInputEnabled, useMinkKeyboard) {
        if (homeRequestToken > 0 && shouldAutoOpenOnHome && keyboardInputEnabled &&
            !expanded && text.text.isBlank() &&
            route == MagicBoxRoute.SEARCH && selectedRecipient == null
        ) {
            // Android can redeliver the launcher's Home intent while a fold changes displays.
            // Re-arm an idle field, but never erase an expanded draft as a side effect.
            clearCommand()
            if (useMinkKeyboard) {
                keyboard?.hide()
            } else {
                showKeyboardWhileCollapsed = true
            }
        }
    }

    LaunchedEffect(shouldAutoOpenOnHome, useMinkKeyboard) {
        showKeyboardWhileCollapsed = shouldAutoOpenOnHome && !useMinkKeyboard && !expanded
    }

    LaunchedEffect(focusRequestSerial) {
        if (focusRequestSerial > 0) {
            if (useMinkKeyboard) {
                while (!textFieldPlaced) withFrameNanos { }
                withFrameNanos { }
                focusRequester.requestFocus()
                keyboard?.hide()
            } else {
                // TextField focus can request bring-into-view. Wait until its newly docked parent
                // has completed placement before requesting focus or showing the IME.
                while (!textFieldPlaced) withFrameNanos { }
                withFrameNanos { }
                focusRequester.requestFocus()
                if (focusRequestShowsKeyboard) {
                    withFrameNanos { }
                    keyboard?.show()
                }
            }
        }
    }

    MagicHardwareHotkeyEffect(
        enabled = captureActivityCommandHotkeys &&
            keyboardInputEnabled &&
            !expanded &&
            !smsFlow.sentConfirmationVisible &&
            !todoCompletionPending,
        onHotkey = ::beginCollapsedHardwareInput,
    )

    val resolvedLauncherKeyboardHeight = resolveLauncherKeyboardHeightDp(
        preferences = store.launcherKeyboardPreferences,
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
    )
    val launcherKeyboardPreferences = store.launcherKeyboardPreferences.copy(
        heightDp = resolvedLauncherKeyboardHeight.coerceAtMost(
            (configuration.screenHeightDp - 168).coerceAtLeast(168).toFloat(),
        ),
    )
    val onLauncherKeyboardValueChange: (TextFieldValue) -> Unit = { value ->
        updateEditorValue(value, correctHardwareInput = false)
    }
    val showDockedLauncherKeyboard = useMinkKeyboard &&
        shouldAutoOpenOnHome &&
        !expanded &&
        !smsFlow.sentConfirmationVisible &&
        !todoCompletionPending
    val launcherKeyboardNavigationBottom = if (useMinkKeyboard) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        Dimens.dp0
    }
    val launcherKeyboardTotalHeight =
        launcherKeyboardPreferences.heightDp.dp + launcherKeyboardNavigationBottom

    BackHandler(enabled = expanded) { requestDismiss() }

    Box(modifier) {
        if (expanded) {
            Box(Modifier.matchParentSize().background(MinkBlack.copy(alpha = .48f)).clickable(onClick = { requestDismiss() }))
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .statusBarsPadding()
                .then(
                    if (!useMinkKeyboard) {
                        Modifier.imePadding()
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = Dimens.dp720)
                    .fillMaxSize()
                    .padding(
                        start = Dimens.dp18,
                        top = Dimens.dp18,
                        end = Dimens.dp18,
                        bottom = if (expanded && useMinkKeyboard) {
                            Dimens.dp0
                        } else {
                            Dimens.dp18
                        },
                    ),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
            ) {
            if (!noteMode) {
                if (expanded) {
                    MagicResultsPanel(
                        store = store,
                        actions = actions,
                        rawText = text.text,
                        route = route,
                        launcherAppsRevision = launcherAppsRevision,
                        launcherShortcutsRevision = launcherShortcutsRevision,
                        plainQuery = plainQuery,
                        fileSearchLoading = fileSearchState.loading,
                        fileResults = fileSearchState.results,
                        fileSearchRepository = fileSearchRepository,
                        hasMediaAccess = hasMediaAccess,
                        canSearchContacts = canSearchContacts,
                        contactResults = contactResults,
                        recentConversations = visibleRecentConversations,
                        forcedRecipient = searchTerm.takeIf {
                            (route == MagicBoxRoute.MESSAGE || route == MagicBoxRoute.CALL) &&
                                it.isNotBlank() &&
                                selectedRecipient == null
                        },
                        appResults = visibleAppResults,
                        includeAppShortcuts = store.includeAppShortcutsInDiscovery,
                        showClearMessage = hasMessageDraft,
                        scrollState = magicResultsScroll,
                        onSelectHistory = { query ->
                            updateEditorValue(
                                TextFieldValue(query, selection = TextRange(query.length)),
                                correctHardwareInput = false,
                            )
                            refocus()
                        },
                        onOpenFile = { file ->
                            if (store.demoModeEnabled) {
                                Toast.makeText(
                                    context,
                                    R.string.demo_preview_action,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                store.addSearchQuery(plainQuery)
                                actions.openFile(file)
                                dismiss()
                            }
                        },
                        onRequestMedia = { mediaPermissionLauncher.launch(mediaReadPermissions()) },
                        onRequestFolder = { showFileScopeChoice = true },
                        onSubmitWeb = ::submit,
                        onSubmitAi = {
                            if (store.demoModeEnabled) {
                                Toast.makeText(
                                    context,
                                    R.string.demo_preview_action,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                submitAi()
                            }
                        },
                        onOpenTodos = onOpenTodos?.let { { openTodos() } },
                        onSelectContact = ::selectContact,
                        onSelectRecentConversation = ::selectRecentConversation,
                        onSelectForcedRecipient = { identifier ->
                            if (store.demoModeEnabled && route == MagicBoxRoute.CALL) {
                                Toast.makeText(
                                    context,
                                    R.string.demo_preview_action,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@MagicResultsPanel
                            }
                            val recipient = userEnteredRecipient(identifier)
                            if (route == MagicBoxRoute.CALL) {
                                callFlow.requestConfirmation(recipient)
                                collapseForDialog()
                            } else {
                                route = MagicBoxRoute.MESSAGE
                                selectedRecipient = SelectedMessageRecipient.AddressRecipient(
                                    recipient = recipient,
                                    providerPackage = automaticMessagingPackage(
                                        sendAutomatically = store.sendMessagesAutomatically,
                                        preferredPackage = store.preferredMessagingPackage,
                                        defaultMessagingPackage = actions.defaultMessagingPackage(),
                                    ),
                                )
                                text = TextFieldValue()
                                refocus()
                            }
                        },
                        onSelectApp = ::selectApp,
                        onLongPressApp = ::showAppActions,
                        onIncludeAppShortcutsChange = store::updateIncludeAppShortcutsInDiscovery,
                        onRequestContacts = {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        onClearMessage = ::requestClearMessageDraft,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                } else {
                    Spacer(Modifier.fillMaxWidth().weight(1f))
                }
            }

            if (
                !noteMode && expanded && text.text.isBlank() &&
                route == MagicBoxRoute.SEARCH
            ) {
                if (appBubbleHost == MagicAppBubbleHost.ASSISTANT) {
                    QuickAppsPanel(
                        store = store,
                        actions = actions,
                        appAccessState = effectiveAppAccessState,
                        onSelect = ::selectApp,
                    )
                } else {
                    MagicBoxLegend(route.trigger, enabled = true) { key ->
                        MagicBoxRoute.fromTrigger(key)?.let(::activateRoute)
                    }
                }
            }

            calendarParseResult?.let { result ->
                CalendarParsePreview(
                    result = result,
                    languageTag = calendarLanguageTag,
                    use24HourClock = store.use24HourClock,
                    onReview = {
                        pendingCalendarReview = result
                        keyboard?.hide()
                    },
                )
            }

            MagicEditorSurface(
                noteMode = noteMode,
                text = text,
                selectedRecipientLabel = selectedRecipient?.label,
                selectedRecipientDetail = selectedRecipient?.detail(actions::appLabel),
                selectedRecipientLeadingIcon = selectedRecipient?.providerPackage?.let { packageName ->
                    {
                        DrawableIcon(
                            drawable = actions.appIcon(packageName),
                            iconKey = "selected-conversation:$packageName:$launcherAppsRevision",
                            size = Dimens.dp18,
                        )
                    }
                },
                route = route,
                acceptPlatformTextInput = !useMinkKeyboard,
                actionVisuals = actionVisuals,
                actionContentColor = actionContentColor,
                focusRequester = focusRequester,
                interactionSource = inputSurfaceInteractionSource,
                onRefocus = { refocus(showSoftwareKeyboard = !useMinkKeyboard) },
                onTextChange = { value ->
                    updateEditorValue(value, correctHardwareInput = true)
                },
                onPlaced = { textFieldPlaced = true },
                onSubmit = ::submit,
                onHardwareKeyUp = initialHardwareKeyCorrection::onHardwareKeyUp,
                onClearMessage = ::requestClearMessageDraft,
                onDeleteNote = {
                    keyboard?.hide()
                    showNoteDeleteConfirmation = true
                },
                visible = expanded,
                modifier = Modifier.fillMaxWidth().then(
                    if (noteMode) Modifier.weight(1f) else Modifier.heightIn(min = magicBoxMinimumHeight),
                ),
            )

                if (expanded && useMinkKeyboard) {
                    Spacer(
                        Modifier.height(
                            launcherKeyboardTotalHeight + Dimens.dp8,
                        ),
                    )
                }
            }

        }

        if (!expanded && !smsFlow.sentConfirmationVisible && !todoCompletionPending) {
            CollapsedMagicBar(
                modifier = collapsedModifier
                    .then(
                        if (showDockedLauncherKeyboard) {
                            Modifier.padding(
                                // Home's collapsed modifier already owns the navigation-bar inset.
                                // Reserve only the visible key area here so the inset is not counted twice.
                                bottom = launcherKeyboardPreferences.heightDp.dp + Dimens.dp8,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .align(Alignment.BottomCenter),
                minimumHeight = magicBoxMinimumHeight,
                armedFocusRequester = armedFocusRequester,
                onArmedPlaced = { armedTargetPlaced = true },
                onPrintableKeyDown = { typedText, keyCode ->
                    if (!expanded) {
                        beginCollapsedHardwareInput(typedText, keyCode)
                    } else {
                        val updatedText = text.text + typedText
                        text = TextFieldValue(updatedText, selection = TextRange(updatedText.length))
                    }
                },
                onOpen = {
                    clearCommand()
                    expanded = true
                    refocus(showSoftwareKeyboard = !useMinkKeyboard)
                },
                onOpenTotalScreen = onOpenTotalScreen,
            )

        }

        MagicLauncherKeyboardHost(
            visible = useMinkKeyboard && (expanded || showDockedLauncherKeyboard),
            value = text,
            onValueChange = { value ->
                val changed = value.text != text.text
                onLauncherKeyboardValueChange(value)
                if (!expanded && changed) {
                    expanded = true
                    refocus(showSoftwareKeyboard = false)
                }
            },
            preferences = launcherKeyboardPreferences,
            themePrimaryArgb = store.effectiveHomePanelColorArgb,
            themeSecondaryArgb = store.effectiveThemeSecondaryColorArgb,
            onHotkey = { trigger ->
                MagicBoxRoute.fromTrigger(trigger)?.let { nextRoute ->
                    activateRoute(nextRoute, showSoftwareKeyboard = false)
                }
            },
            onSubmit = ::submit,
            multiline = expanded && noteMode,
            onOpenSettings = onOpenKeyboardSettings,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        SmsSentConfirmation(
            visible = smsFlow.sentConfirmationVisible,
            modifier = Modifier.align(Alignment.Center),
        )
    }
    MagicDraftDialogHost(
        showNoteDelete = showNoteDeleteConfirmation,
        showMessageDiscard = showMessageDiscardConfirmation,
        showCommandDiscard = showCommandDiscardConfirmation,
        showFileScopeChoice = showFileScopeChoice,
        showSmsAssistantDisclosure = smsFlow.assistantDisclosureVisible,
        onDeleteNote = {
            showNoteDeleteConfirmation = false
            dismiss()
        },
        onKeepNote = {
            showNoteDeleteConfirmation = false
            refocus()
        },
        onDiscardMessage = {
            showMessageDiscardConfirmation = false
            clearCommand()
            refocus()
        },
        onKeepMessage = {
            showMessageDiscardConfirmation = false
            refocus()
        },
        onDiscardCommand = {
            showCommandDiscardConfirmation = false
            dismiss()
        },
        onKeepCommand = {
            showCommandDiscardConfirmation = false
            refocus()
        },
        onChooseFolder = {
            showFileScopeChoice = false
            folderPicker.launch(null)
        },
        onSkipFolder = { showFileScopeChoice = false },
        onAssistantContinue = smsFlow::continueAssistantSelection,
        onAssistantDismiss = smsFlow::dismissAssistantDisclosure,
    )

    MagicPickerDialogHost(
        messageDraft = pendingMessagingChoice,
        messagingOptions = messagingProviders.options,
        messagingOptionsLoaded = messagingProviders.loaded,
        preferredMessagingPackage = store.preferredMessagingPackage,
        showAiPicker = showAiPicker,
        showAllAiApps = showAllAiApps,
        aiProviders = aiProviders,
        aiProvidersLoaded = aiProvidersLoaded,
        allAiApps = allAiApps,
        allAiAppsLoaded = allAiAppsLoaded,
        preferredAiPackage = store.preferredAiPackage,
        onMessagingProvider = { draft, option ->
            pendingMessagingChoice = null
            openMessagingProvider(
                draft,
                providerPackage = if (option.systemDefault) null else option.preferencePackageName,
            )
        },
        onInstallMessagingProvider = { option ->
            option.preferencePackageName?.let(actions::openMessagingProviderInstallPage)
        },
        onSeeAllMessagingApps = { draft ->
            pendingMessagingChoice = null
            chooseMessagingApp(draft)
        },
        onDismissMessaging = {
            pendingMessagingChoice = null
            refocus()
        },
        onSeeAllAiApps = {
            showAiPicker = false
            showAllAiApps = true
        },
        onAiProvider = { option ->
            option.installedPackageName?.let(::selectAiPackage)
        },
        onInstallAiProvider = { actions.openAiProviderInstallPage(it) },
        onDismissCuratedAi = ::dismissAiPickers,
        onAllAiApp = ::selectAiApp,
        onDismissAllAi = ::dismissAiPickers,
    )

    MagicAppBubbleDialogHost(appBubbleFlow, actions)

    MagicCommunicationDialogHost(
        callRecipient = callFlow.recipientToConfirm,
        callAutomatically = callFlow.callAutomatically,
        smsDraft = smsFlow.draftToConfirm,
        assistantActive = actions.isAssistantRoleHeld(),
        onCallNow = { callFlow.callNow() },
        onChooseCallingApp = { callFlow.chooseCallingApp() },
        onDismissCall = callFlow::dismiss,
        onSendSms = { smsFlow.confirmDraft() },
        onChooseMessagingApp = { smsFlow.chooseMessagingApp { pendingMessagingChoice = it } },
        onDismissSms = { smsFlow.dismissDraft(onSessionComplete) },
    )
    pendingCalendarReview?.let { review ->
        CalendarReviewDialog(
            result = review,
            onContinueWithoutTime = { draft ->
                pendingCalendarReview = null
                if (actions.createEvent(draft.copy(startMillis = null, endMillis = null, allDay = false))) {
                    dismiss()
                } else {
                    refocus()
                }
            },
            onEdit = {
                pendingCalendarReview = null
                refocus()
            },
        )
    }
}
