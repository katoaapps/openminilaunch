@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class MagicActionVisuals(
    val color: Color,
    val icon: ImageVector,
)

@Composable
private fun MagicInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    prefix: Char?,
    focusRequester: FocusRequester,
    onPlaced: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 5,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(R.string.magic_box_hotkey_hint), color = Muted) },
        modifier = modifier.focusRequester(focusRequester)
            .onGloballyPositioned { onPlaced() }
            .onPreviewKeyEvent { event ->
                if (prefix == '-' && event.key == Key.Enter) {
                    if (event.type == KeyEventType.KeyUp) onSubmit()
                    true
                } else false
            },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MinkTransparent,
            unfocusedContainerColor = MinkTransparent,
            focusedIndicatorColor = MinkTransparent,
            unfocusedIndicatorColor = MinkTransparent,
        ),
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            showKeyboardOnFocus = false,
            imeAction = when {
                prefix == MAGIC_NOTE_PREFIX -> ImeAction.Default
                prefix in listOf('@', '#', '-', '+', '?') -> ImeAction.Send
                else -> ImeAction.Search
            },
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSubmit() },
            onSend = { onSubmit() },
        ),
    )
}

@Composable
internal fun MagicBox(
    store: LauncherStore,
    actions: DeviceActions,
    modifier: Modifier = Modifier,
    collapsedModifier: Modifier = Modifier,
    keyboardInputEnabled: Boolean = true,
    initiallyExpanded: Boolean = false,
    showSoftwareKeyboardOnStart: Boolean = false,
    onTodoAdded: (String) -> Unit = {},
    onExpandedChange: (Boolean) -> Unit = {},
    onSessionComplete: () -> Unit = {},
) {
    val magicBoxMinimumHeight = Dimens.dp64
    val context = LocalContext.current
    val fileSearchRepository = remember { FileSearchRepository(context.applicationContext) }
    var text by remember { mutableStateOf(TextFieldValue()) }
    var selectedContact by remember { mutableStateOf<ContactResult?>(null) }
    var lockedPrefix by remember { mutableStateOf<Char?>(null) }
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var hasContacts by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasMediaAccess by remember { mutableStateOf(hasMediaReadAccess(context)) }
    var showFileScopeChoice by remember { mutableStateOf(false) }
    var showAiPicker by remember { mutableStateOf(false) }
    var showAllAiApps by remember { mutableStateOf(false) }
    var pendingAiQuery by remember { mutableStateOf<String?>(null) }
    var callToConfirm by remember { mutableStateOf<ContactResult?>(null) }
    var pendingPermissionCall by remember { mutableStateOf<ContactResult?>(null) }
    var smsToConfirm by remember { mutableStateOf<PendingSms?>(null) }
    var pendingPermissionSms by remember { mutableStateOf<PendingSms?>(null) }
    var pendingAssistantSms by remember { mutableStateOf<PendingSms?>(null) }
    var showSmsSentConfirmation by remember { mutableStateOf(false) }
    var smsSentConfirmationToken by remember { mutableIntStateOf(0) }
    var showSmsAssistantDisclosure by remember { mutableStateOf(false) }
    var showNoteDeleteConfirmation by remember { mutableStateOf(false) }
    var fileResults by remember { mutableStateOf<List<FileSearchResult>>(emptyList()) }
    var fileSearchLoading by remember { mutableStateOf(false) }
    val fileSearchRequests = remember { FileSearchRequestTracker() }
    var aiAppsLoaded by remember { mutableStateOf(false) }
    val curatedAiApps by produceState<List<LaunchableApp>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { actions.curatedAiApps() }
        aiAppsLoaded = true
    }
    var allAiAppsLoaded by remember { mutableStateOf(false) }
    val allAiApps by produceState<List<LaunchableApp>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { actions.textShareApps() }
        allAiAppsLoaded = true
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
        if (!granted && isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)) actions.openAppSettings()
    }
    val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val contact = pendingPermissionCall
        pendingPermissionCall = null
        if (granted && contact != null) {
            actions.placeCall(contact.phone)
        } else if (!granted && isPermanentlyDenied(context, Manifest.permission.CALL_PHONE)) {
            actions.openAppSettings()
        }
        onSessionComplete()
    }
    fun completeSmsAttempt(draft: PendingSms) {
        fun openComposerFallback(message: String) {
            val opened = actions.chooseMessagingApp(draft.contact, draft.body)
            Toast.makeText(
                context,
                if (opened) message else context.getString(R.string.sms_no_compatible_fallback),
                Toast.LENGTH_LONG,
            ).show()
        }
        when (actions.sendSmsDirect(draft.contact.phone, draft.body)) {
            DirectSmsResult.QUEUED -> {
                showSmsSentConfirmation = true
                smsSentConfirmationToken++
            }
            DirectSmsResult.NO_DEFAULT_SUBSCRIPTION -> {
                openComposerFallback(context.getString(R.string.choose_sim_in_messaging_app))
                onSessionComplete()
            }
            DirectSmsResult.NOT_AUTHORIZED, DirectSmsResult.UNSUPPORTED, DirectSmsResult.FAILED -> {
                openComposerFallback(context.getString(R.string.direct_sms_unavailable))
                onSessionComplete()
            }
        }
    }
    LaunchedEffect(smsSentConfirmationToken) {
        if (smsSentConfirmationToken > 0) {
            delay(1_100)
            showSmsSentConfirmation = false
            delay(350)
            onSessionComplete()
        }
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val draft = pendingPermissionSms
        pendingPermissionSms = null
        if (granted && draft != null) {
            completeSmsAttempt(draft)
        } else {
            if (!granted && isPermanentlyDenied(context, Manifest.permission.SEND_SMS)) actions.openAppSettings()
            onSessionComplete()
        }
    }
    val assistantSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val draft = pendingAssistantSms
        pendingAssistantSms = null
        if (draft != null && actions.isAssistantRoleHeld()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                completeSmsAttempt(draft)
            } else {
                pendingPermissionSms = draft
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        } else if (draft != null) {
            smsToConfirm = draft
            Toast.makeText(context, context.getString(R.string.choose_assistant_for_sms), Toast.LENGTH_LONG).show()
        }
    }
    fun sendDirectOrRequestAccess(draft: PendingSms) {
        when {
            !supportsDirectSms(context) -> completeSmsAttempt(draft)
            !actions.isAssistantRoleHeld() -> {
                pendingAssistantSms = draft
                showSmsAssistantDisclosure = true
            }
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED -> {
                completeSmsAttempt(draft)
            }
            else -> {
                pendingPermissionSms = draft
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }
    }

    fun chooseMessagingApp(draft: PendingSms) {
        val opened = actions.chooseMessagingApp(draft.contact, draft.body)
        if (!opened) Toast.makeText(context, context.getString(R.string.no_compatible_messaging_app), Toast.LENGTH_LONG).show()
        onSessionComplete()
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasMediaAccess = hasMediaReadAccess(context)
        if (!hasMediaAccess && mediaPermissionPermanentlyDenied(context)) actions.openAppSettings()
        showFileScopeChoice = true
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            store.addSearchFolder(it.toString(), fileSearchRepository.folderLabel(it))
            fileSearchRepository.invalidateFolders()
        }
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var focusRequestSerial by remember { mutableIntStateOf(0) }
    var focusRequestShowsKeyboard by remember { mutableStateOf(true) }
    var textFieldPlaced by remember { mutableStateOf(false) }
    val magicResultsScroll = rememberScrollState()
    val parsedInput = parseMagicBoxInput(text.text, lockedPrefix)
    val prefix = parsedInput.prefix
    val searchTerm = parsedInput.searchTerm
    val contactResults = remember(prefix, searchTerm, hasContacts) {
        if (prefix in listOf('@', '#') && hasContacts && selectedContact == null) actions.searchContacts(searchTerm).take(5) else emptyList()
    }
    val appResults = remember(prefix, searchTerm) {
        if (prefix == '?' && searchTerm.isNotBlank()) {
            actions.installedApps().filter { it.label.startsWith(searchTerm, true) }.take(5)
        } else emptyList()
    }
    val plainQuery = parsedInput.plainQuery
    val noteMode = expanded && prefix == MAGIC_NOTE_PREFIX
    val hasNoteDraft = noteMode && text.text.drop(1).isNotBlank()
    val indexedFolderUris = store.searchFolders.map { it.uri }
    LaunchedEffect(plainQuery, indexedFolderUris, hasMediaAccess) {
        val request = fileSearchRequests.begin(plainQuery)
        if (plainQuery.length < 2) {
            fileResults = emptyList()
            fileSearchLoading = false
            magicResultsScroll.scrollTo(0)
        } else {
            fileSearchLoading = true
            try {
                delay(180)
                val folders = store.searchFolders.toList()
                val results = withContext(Dispatchers.IO) {
                    fileSearchRepository.search(request.query, folders, hasMediaAccess)
                }
                if (fileSearchRequests.isCurrent(request)) {
                    fileResults = results
                    magicResultsScroll.scrollTo(0)
                }
            } finally {
                if (fileSearchRequests.isCurrent(request)) fileSearchLoading = false
            }
        }
    }
    val actionVisuals = when (prefix) {
        '@' -> MagicActionVisuals(MagicTextColor, Icons.AutoMirrored.Filled.Send)
        '#' -> MagicActionVisuals(MagicCallColor, Icons.Default.Phone)
        '-' -> MagicActionVisuals(MagicTodoColor, Icons.Default.Checklist)
        MAGIC_NOTE_PREFIX -> MagicActionVisuals(MagicNoteColor, Icons.AutoMirrored.Filled.NoteAdd)
        '+' -> MagicActionVisuals(MagicEventColor, Icons.Default.Event)
        '?' -> MagicActionVisuals(MagicAppColor, Icons.Default.Apps)
        else -> MagicActionVisuals(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Search,
        )
    }
    val actionContentColor = when (prefix) {
        '-' -> LightInk
        null -> MaterialTheme.colorScheme.onPrimary
        else -> MinkWhite
    }
    fun refocus(showSoftwareKeyboard: Boolean = true) {
        focusRequestShowsKeyboard = showSoftwareKeyboard
        focusRequestSerial += 1
    }

    fun clearCommand() {
        fileSearchRequests.invalidate()
        text = TextFieldValue()
        selectedContact = null
        lockedPrefix = null
        fileResults = emptyList()
        fileSearchLoading = false
    }

    fun dismiss() {
        clearCommand()
        keyboard?.hide()
        expanded = false
        onExpandedChange(false)
        onSessionComplete()
    }

    fun requestDismiss() {
        if (hasNoteDraft) {
            keyboard?.hide()
            showNoteDeleteConfirmation = true
        } else {
            dismiss()
        }
    }

    fun collapseForDialog() {
        clearCommand()
        keyboard?.hide()
        expanded = false
        onExpandedChange(false)
    }

    fun submitAi() {
        val query = plainQuery
        if (query.isBlank()) return
        val preferredPackage = store.preferredAiPackage
        if (!preferredPackage.isNullOrBlank() && actions.shareQueryWithApp(query, preferredPackage)) {
            store.addSearchQuery(query)
            dismiss()
        } else {
            if (!preferredPackage.isNullOrBlank()) store.resetPreferredAiApp()
            pendingAiQuery = query
            showAiPicker = true
            keyboard?.hide()
        }
    }

    fun submit() {
        val payload = if (lockedPrefix != null) text.text.trim() else text.text.drop(1).trim()
        var keepDraftAfterExternalHandoff = false
        val handled = when (prefix) {
            '-' -> payload.isNotBlank().also { if (it) { store.addTodo(payload); onTodoAdded(payload) } }
            MAGIC_NOTE_PREFIX -> payload.isNotBlank() && actions.createNote(payload).also { opened ->
                keepDraftAfterExternalHandoff = opened
            }
            '+' -> payload.isNotBlank() && actions.createEvent(payload)
            '@' -> (selectedContact != null && payload.isNotBlank()).also {
                if (it) {
                    val draft = PendingSms(selectedContact!!, payload)
                    collapseForDialog()
                    when (store.messageSendMode) {
                        MessageSendMode.ALWAYS_ASK -> smsToConfirm = draft
                        MessageSendMode.DIRECT_SMS -> sendDirectOrRequestAccess(draft)
                        MessageSendMode.MESSAGING_APP -> chooseMessagingApp(draft)
                    }
                }
            }
            '#' -> false
            '?' -> false
            else -> text.text.isNotBlank() && actions.webSearch(text.text, store.preferredWebPackage).also {
                if (it) store.addSearchQuery(text.text)
            }
        }
        if (handled && prefix != '@') {
            if (keepDraftAfterExternalHandoff) {
                keyboard?.hide()
            } else {
                dismiss()
            }
        }
    }

    LaunchedEffect(keyboardInputEnabled) {
        if (keyboardInputEnabled) {
            while (!textFieldPlaced) withFrameNanos { }
            withFrameNanos { }
            focusRequester.requestFocus()
            withFrameNanos { }
            if (initiallyExpanded && showSoftwareKeyboardOnStart) keyboard?.show() else keyboard?.hide()
            if (initiallyExpanded) onExpandedChange(true)
        } else {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(focusRequestSerial) {
        if (focusRequestSerial > 0) {
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

    LaunchedEffect(noteMode) {
        if (noteMode) {
            // Entering note mode replaces the compact field with the full-screen editor.
            // Wait for that editor to be placed before restoring focus so hardware-keyboard
            // input immediately following the slash command is not dropped.
            textFieldPlaced = false
            while (!textFieldPlaced) withFrameNanos { }
            withFrameNanos { }
            runCatching { focusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = expanded) { requestDismiss() }

    Box(modifier) {
        if (expanded) {
            Box(Modifier.matchParentSize().background(MinkBlack.copy(alpha = .48f)).clickable(onClick = { requestDismiss() }))
        }

        Column(
            modifier = Modifier
                .matchParentSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = Dimens.dp18, vertical = Dimens.dp18),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            if (!noteMode) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (expanded) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(magicResultsScroll),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
                        if (text.text.isBlank() && lockedPrefix == null && store.searchHistory.isNotEmpty()) {
                            SearchHistoryList(
                                queries = store.searchHistory,
                                onSelect = { query ->
                                    text = TextFieldValue(query, selection = TextRange(query.length))
                                    refocus()
                                },
                                onDelete = store::removeSearchQuery,
                                onClearAll = store::clearSearchHistory,
                            )
                        }
                        if (plainQuery.isNotBlank() && fileSearchLoading) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        if (fileResults.isNotEmpty()) {
                            FileResultsGrid(fileResults, fileSearchRepository) { file ->
                                store.addSearchQuery(plainQuery)
                                actions.openFile(file)
                                dismiss()
                            }
                        }
                        if (plainQuery.length >= 2 && !hasMediaAccess) {
                            FilledTonalButton(onClick = { mediaPermissionLauncher.launch(mediaReadPermissions()) }) {
                                Icon(Icons.Default.PhotoLibrary, null, Modifier.size(Dimens.dp18))
                                Text(stringResource(R.string.search_media_filenames), Modifier.padding(start = Dimens.dp8))
                            }
                        }
                        if (plainQuery.isNotBlank() && store.searchFolders.isEmpty()) {
                            Surface(
                                onClick = { showFileScopeChoice = true },
                                shape = RoundedCornerShape(Dimens.dp14),
                                color = MagicTodoColor.copy(alpha = .18f),
                            ) {
                                Row(Modifier.fillMaxWidth().padding(Dimens.dp12), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FolderOff, null, tint = MagicTodoColor)
                                    Column(Modifier.weight(1f).padding(start = Dimens.dp10)) {
                                        Text(stringResource(R.string.document_search_not_set_up), fontWeight = FontWeight.SemiBold)
                                        Text(stringResource(R.string.choose_document_search_folder), color = Muted, fontSize = Dimens.sp12)
                                    }
                                    Icon(Icons.Default.ChevronRight, null)
                                }
                            }
                        }
                        if (prefix !in MAGIC_COMMAND_PREFIXES && text.text.isNotBlank()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                                SearchDestinationButton(
                                    label = stringResource(R.string.web),
                                    detail = stringResource(R.string.search_browser),
                                    icon = Icons.Default.Public,
                                    modifier = Modifier.weight(1f),
                                    onClick = { submit() },
                                )
                                SearchDestinationButton(
                                    label = stringResource(R.string.ai),
                                    detail = store.preferredAiPackage?.let(actions::appLabel) ?: stringResource(R.string.choose_an_app),
                                    icon = Icons.Default.AutoAwesome,
                                    modifier = Modifier.weight(1f),
                                    onClick = { submitAi() },
                                )
                            }
                        }
                        contactResults.forEach { contact ->
                            SuggestionRow(
                                stringResource(R.string.two_part_label, contact.name, contact.phoneLabel),
                                Icons.Default.Person,
                            ) {
                                if (prefix == '#') {
                                    callToConfirm = contact
                                    collapseForDialog()
                                } else {
                                    lockedPrefix = prefix
                                    selectedContact = contact
                                    text = TextFieldValue()
                                    refocus()
                                }
                            }
                        }
                        appResults.forEach { app ->
                            SuggestionRow(
                                text = app.label,
                                leadingContent = { AppIcon(app.packageName, actions, Dimens.dp26) },
                            ) {
                                if (actions.launchPackage(app.packageName)) {
                                    store.addSearchQuery("?${app.label}")
                                    dismiss()
                                }
                            }
                        }
                        if (prefix in listOf('@', '#') && !hasContacts) {
                            FilledTonalButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                                Text(stringResource(R.string.allow_contacts_to_search_people))
                            }
                        }
                    }
                    }
                }
            }

            if (!noteMode && expanded && text.text.isBlank() && lockedPrefix == null) {
                MagicBoxLegend(prefix, enabled = true) { key ->
                    clearCommand()
                    text = TextFieldValue(key.toString(), selection = TextRange(1))
                    refocus()
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().then(
                    if (noteMode) Modifier.weight(1f) else Modifier.heightIn(min = magicBoxMinimumHeight),
                )
                    .graphicsLayer { alpha = if (expanded) 1f else 0f },
                shape = RoundedCornerShape(Dimens.dp24),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = Dimens.dp12,
            ) {
                if (noteMode) {
                    Box(Modifier.fillMaxSize()) {
                        MagicInputField(
                            value = text,
                            onValueChange = { text = it },
                            prefix = prefix,
                            focusRequester = focusRequester,
                            onPlaced = { textFieldPlaced = true },
                            onSubmit = { submit() },
                            modifier = Modifier.fillMaxSize().padding(end = Dimens.dp48, bottom = Dimens.dp54),
                            maxLines = Int.MAX_VALUE,
                        )
                        IconButton(
                            onClick = {
                                keyboard?.hide()
                                showNoteDeleteConfirmation = true
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.dp6),
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                stringResource(R.string.delete_note_draft),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        FilledIconButton(
                            onClick = { submit() },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.dp10),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = actionVisuals.color,
                                contentColor = actionContentColor,
                            ),
                        ) { Icon(actionVisuals.icon, stringResource(R.string.run_command)) }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(start = Dimens.dp12, end = Dimens.dp6),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        selectedContact?.let { contact ->
                            CommandChip(
                                stringResource(R.string.two_part_label, contact.name, contact.phoneLabel),
                                actionVisuals.color,
                                actionContentColor,
                            ) {
                                clearCommand()
                                refocus()
                            }
                        }
                        MagicInputField(
                            value = text,
                            onValueChange = { value ->
                                text = value
                                if (!expanded && value.text.isNotEmpty()) {
                                    expanded = true
                                    onExpandedChange(true)
                                }
                                if (lockedPrefix == null && value.text.firstOrNull() != prefix) {
                                    selectedContact = null
                                }
                            },
                            prefix = prefix,
                            focusRequester = focusRequester,
                            onPlaced = { textFieldPlaced = true },
                            onSubmit = { submit() },
                            modifier = Modifier.weight(1f),
                        )
                        FilledIconButton(
                            onClick = { submit() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = actionVisuals.color,
                                contentColor = actionContentColor,
                            ),
                        ) { Icon(actionVisuals.icon, stringResource(R.string.run_command)) }
                    }
                }
            }
        }

        if (!expanded && !showSmsSentConfirmation) {
            Row(
                collapsedModifier.align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(Dimens.dp22))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable {
                        clearCommand()
                        expanded = true
                        onExpandedChange(true)
                        refocus(showSoftwareKeyboard = true)
                    }
                    .heightIn(min = magicBoxMinimumHeight)
                    .padding(start = Dimens.dp18, end = Dimens.dp6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.magic_box_collapsed_hint), Modifier.weight(1f), color = Muted, fontSize = Dimens.sp14)
                FilledIconButton(
                    onClick = {
                        clearCommand()
                        expanded = true
                        onExpandedChange(true)
                        refocus(showSoftwareKeyboard = true)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.Keyboard, stringResource(R.string.open_magic_box))
                }
            }
        }

        AnimatedVisibility(
            visible = showSmsSentConfirmation,
            modifier = Modifier.align(Alignment.Center).zIndex(30f),
            enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = .88f),
            exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = .94f),
        ) {
            Surface(
                shape = RoundedCornerShape(Dimens.dp28),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = Dimens.dp16,
            ) {
                Row(
                    Modifier.padding(horizontal = Dimens.dp28, vertical = Dimens.dp20),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
                ) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(Dimens.dp30))
                    Column {
                        Text(stringResource(R.string.message_sent), fontSize = Dimens.sp19, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.sent_as_sms), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f), fontSize = Dimens.sp13)
                    }
                }
            }
        }
    }
    if (showNoteDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showNoteDeleteConfirmation = false
                refocus()
            },
            icon = { Icon(Icons.Default.DeleteOutline, null) },
            title = { Text(stringResource(R.string.delete_note_draft_title)) },
            text = { Text(stringResource(R.string.delete_note_draft_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNoteDeleteConfirmation = false
                        dismiss()
                    },
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNoteDeleteConfirmation = false
                        refocus()
                    },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (showFileScopeChoice) {
        FileSearchScopeDialog(
            onChooseFolder = {
                showFileScopeChoice = false
                folderPicker.launch(null)
            },
            onSkip = { showFileScopeChoice = false },
        )
    }
    if (showSmsAssistantDisclosure) {
        AssistantDisclosureDialog(
            active = false,
            onContinue = {
                showSmsAssistantDisclosure = false
                assistantSettingsLauncher.launch(actions.assistantRoleSelectionIntent())
            },
            onDismiss = {
                showSmsAssistantDisclosure = false
                pendingAssistantSms?.let { draft ->
                    pendingAssistantSms = null
                    smsToConfirm = draft
                } ?: onSessionComplete()
            },
        )
    }
    if (showAiPicker) {
        AppPickerDialog(
            title = stringResource(R.string.choose_ai_app),
            apps = curatedAiApps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !aiAppsLoaded,
            emptyMessage = stringResource(R.string.no_curated_ai_apps),
            extraActionLabel = stringResource(R.string.other_compatible_app),
            onExtraAction = { showAiPicker = false; showAllAiApps = true },
            onApp = { app ->
                val query = pendingAiQuery
                if (query != null && actions.shareQueryWithApp(query, app.packageName)) {
                    store.setPreferredAiApp(app.packageName)
                    store.addSearchQuery(query)
                    showAiPicker = false
                    pendingAiQuery = null
                    dismiss()
                }
            },
            onDismiss = {
                showAiPicker = false
                pendingAiQuery = null
                refocus()
            },
        )
    }
    if (showAllAiApps) {
        AppPickerDialog(
            title = stringResource(R.string.other_compatible_apps),
            apps = allAiApps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !allAiAppsLoaded,
            emptyMessage = stringResource(R.string.no_other_text_apps),
            onApp = { app ->
                val query = pendingAiQuery
                if (query != null && actions.shareQueryWithApp(query, app.packageName)) {
                    store.setPreferredAiApp(app.packageName)
                    store.addSearchQuery(query)
                    showAllAiApps = false
                    pendingAiQuery = null
                    dismiss()
                }
            },
            onDismiss = {
                showAllAiApps = false
                pendingAiQuery = null
                refocus()
            },
        )
    }
    callToConfirm?.let { contact ->
        AlertDialog(
            onDismissRequest = { callToConfirm = null; onSessionComplete() },
            icon = { Icon(Icons.Default.Phone, null, tint = MagicCallColor) },
            title = { Text(stringResource(R.string.call_contact, contact.name)) },
            text = { Text(stringResource(R.string.phone_type_and_number, contact.phoneLabel, contact.phone)) },
            confirmButton = {
                Button(
                    onClick = {
                        callToConfirm = null
                        if (!supportsDirectCalls(context) || ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            actions.placeCall(contact.phone)
                            onSessionComplete()
                        } else {
                            pendingPermissionCall = contact
                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MagicCallColor, contentColor = MinkWhite),
                ) { Text(stringResource(R.string.call_now)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { callToConfirm = null; onSessionComplete() }) { Text(stringResource(R.string.cancel)) }
                    TextButton(
                        onClick = {
                            callToConfirm = null
                            if (!actions.chooseCallingApp(contact.phone)) {
                                Toast.makeText(context, context.getString(R.string.no_compatible_calling_app), Toast.LENGTH_LONG).show()
                            }
                            onSessionComplete()
                        },
                    ) { Text(stringResource(R.string.choose_calling_app)) }
                }
            },
        )
    }
    smsToConfirm?.let { draft ->
        val assistantActive = actions.isAssistantRoleHeld()
        AlertDialog(
            onDismissRequest = { smsToConfirm = null; onSessionComplete() },
            icon = { Icon(Icons.AutoMirrored.Filled.Send, null, tint = MagicTextColor) },
            title = { Text(stringResource(R.string.send_message_to_contact, draft.contact.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                    Text(stringResource(R.string.phone_type_and_number, draft.contact.phoneLabel, draft.contact.phone))
                    Text(
                        draft.body,
                        Modifier.heightIn(max = Dimens.dp180).verticalScroll(rememberScrollState()),
                    )
                    Text(stringResource(R.string.sms_carrier_notice), color = Muted, fontSize = Dimens.sp12)
                    if (!assistantActive) {
                        Text(stringResource(R.string.assistant_sms_restriction), color = Muted, fontSize = Dimens.sp12)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        smsToConfirm = null
                        sendDirectOrRequestAccess(draft)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MagicTextColor, contentColor = MinkWhite),
                ) { Text(stringResource(if (assistantActive) R.string.send_sms_now else R.string.choose_assistant)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { smsToConfirm = null; onSessionComplete() }) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { smsToConfirm = null; chooseMessagingApp(draft) }) { Text(stringResource(R.string.choose_messaging_app)) }
                }
            },
        )
    }
}

private data class PendingSms(val contact: ContactResult, val body: String)
