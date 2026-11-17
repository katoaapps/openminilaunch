package com.katoaapps.openminilaunch.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.ui.PortableArchiveImportDisplayResult
import com.katoaapps.minkboard.ui.PortableArchiveImportResultsDialog
import com.katoaapps.minkboard.ui.PortableArchiveWorkflowDialog
import com.katoaapps.minkboard.ui.PortableArchiveWorkflowRequest
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkboard.OpenMinkMinkBoardRuntime
import com.katoaapps.openminilaunch.features.minkboard.access.AndroidBoardAuthenticator
import com.katoaapps.openminilaunch.features.minkboard.access.BoardAuthenticationAvailability
import com.katoaapps.openminilaunch.features.minkboard.access.BoardAuthenticationResult
import com.katoaapps.openminilaunch.features.minkboard.archive.OpenMinkArchiveDocumentIo
import com.katoaapps.openminilaunch.features.minkboard.archive.OpenMinkPortableArchiveService
import com.katoaapps.openminilaunch.features.minkboard.archive.OpenMinkSelectedArchive
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Portable board transfer UI. Launcher settings backup remains a separate workflow. */
@Composable
internal fun MinkBoardArchiveSettingsSection(
    runtime: OpenMinkMinkBoardRuntime,
    boards: List<BoardSnapshot>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val archiveService = remember(runtime) {
        OpenMinkPortableArchiveService(runtime.repository)
    }
    val activity = context as? FragmentActivity
    val authenticator = remember(activity) {
        activity?.let {
            AndroidBoardAuthenticator(
                it,
                context.getString(R.string.minkboard_authentication_prompt_title),
                context.getString(R.string.minkboard_archive_export_auth_description),
            )
        }
    }

    var showExportSelection by remember { mutableStateOf(false) }
    var selectedExportBoardIds by remember { mutableStateOf<Set<BoardId>>(emptySet()) }
    var workflowRequest by remember {
        mutableStateOf<PortableArchiveWorkflowRequest?>(null)
    }
    var workflowError by remember { mutableStateOf<String?>(null) }
    var workflowBusy by remember { mutableStateOf(false) }
    var selectedArchives by remember { mutableStateOf<List<OpenMinkSelectedArchive>>(emptyList()) }
    var selectedArchiveSupport by remember { mutableStateOf<List<Boolean>>(emptyList()) }
    var selectedArchiveIndex by remember { mutableStateOf(0) }
    var pendingImportResults by remember {
        mutableStateOf<List<PortableArchiveImportDisplayResult>>(emptyList())
    }
    var visibleImportResults by remember {
        mutableStateOf<List<PortableArchiveImportDisplayResult>>(emptyList())
    }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    fun failureMessage(failure: Throwable): String =
        failure.message?.takeIf(String::isNotBlank)
            ?: context.getString(R.string.minkboard_archive_failed)

    fun clearSelectedArchives() {
        selectedArchives.forEach { it.bytes.fill(0) }
        selectedArchives = emptyList()
        selectedArchiveSupport = emptyList()
        selectedArchiveIndex = 0
    }

    fun finishImport() {
        val results = pendingImportResults
        pendingImportResults = emptyList()
        workflowRequest = null
        workflowError = null
        workflowBusy = false
        clearSelectedArchives()
        if (results.size == 1 && results.single().failureMessage == null) {
            statusMessage = context.getString(R.string.minkboard_archive_imported)
            statusIsError = false
        } else if (results.isNotEmpty()) {
            visibleImportResults = results
        }
    }

    fun prepareCurrentImport() {
        var index = selectedArchiveIndex
        var results = pendingImportResults
        while (index < selectedArchives.size) {
            val archive = selectedArchives[index]
            if (selectedArchiveSupport.getOrNull(index) == true) break
            archive.bytes.fill(0)
            results += PortableArchiveImportDisplayResult(
                displayName = archive.displayName
                    ?: context.getString(R.string.minkboard_archive_unnamed, index + 1),
                failureMessage = context.getString(R.string.minkboard_archive_wrong_format),
            )
            index += 1
        }
        selectedArchiveIndex = index
        pendingImportResults = results
        val archive = selectedArchives.getOrNull(index)
        if (archive == null) {
            finishImport()
        } else {
            workflowError = null
            workflowRequest = PortableArchiveWorkflowRequest.Import(
                displayName = archive.displayName
                    ?: context.getString(R.string.minkboard_archive_unnamed, index + 1),
                requiresPassphrase = true,
                requiresBoardName = false,
                batchPosition = index + 1,
                batchSize = selectedArchives.size,
            )
        }
    }

    fun dismissWorkflow() {
        workflowRequest = null
        workflowError = null
        workflowBusy = false
        selectedExportBoardIds = emptySet()
        pendingImportResults = emptyList()
        clearSelectedArchives()
    }

    val createArchiveDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(OpenMinkArchiveDocumentIo.MIME_TYPE),
    ) { uri: Uri? ->
        val bytes = pendingExportBytes.also { pendingExportBytes = null }
        if (uri == null || bytes == null) {
            bytes?.fill(0)
            return@rememberLauncherForActivityResult
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = try {
                withContext(Dispatchers.IO) {
                    archiveResult {
                        OpenMinkArchiveDocumentIo.writeArchive(context.contentResolver, uri, bytes)
                    }
                }
            } finally {
                bytes.fill(0)
            }
            statusMessage = result.fold(
                onSuccess = { context.getString(R.string.minkboard_archive_exported) },
                onFailure = ::failureMessage,
            )
            statusIsError = result.isFailure
        }
    }

    val openArchiveDocuments = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            statusMessage = null
            val result = withContext(Dispatchers.IO) {
                archiveResult {
                    OpenMinkArchiveDocumentIo.readArchives(context.contentResolver, uris)
                }
            }
            result.fold(
                onSuccess = { archives ->
                    val support = withContext(Dispatchers.Default) {
                        archives.map { archive -> archiveService.isSupportedArchive(archive.bytes) }
                    }
                    clearSelectedArchives()
                    selectedArchives = archives
                    selectedArchiveSupport = support
                    selectedArchiveIndex = 0
                    pendingImportResults = emptyList()
                    prepareCurrentImport()
                },
                onFailure = { failure ->
                    statusMessage = failureMessage(failure)
                    statusIsError = true
                },
            )
        }
    }

    fun beginExport() {
        workflowError = null
        workflowRequest = PortableArchiveWorkflowRequest.Export
    }

    fun authorizeAndBeginExport() {
        val selectedBoards = boards.filter { it.board.id in selectedExportBoardIds }
        if (selectedBoards.none { it.board.isLocked }) {
            beginExport()
            return
        }
        val boardAuthenticator = authenticator
        if (
            boardAuthenticator == null ||
            boardAuthenticator.availability() != BoardAuthenticationAvailability.Available
        ) {
            statusMessage = context.getString(R.string.minkboard_authentication_unavailable)
            statusIsError = true
            return
        }
        boardAuthenticator.authenticate { result ->
            when (result) {
                BoardAuthenticationResult.Succeeded -> beginExport()
                BoardAuthenticationResult.Canceled -> Unit
                is BoardAuthenticationResult.Failed -> {
                    statusMessage = context.getString(R.string.minkboard_authentication_failed)
                    statusIsError = true
                }
            }
        }
    }

    val latestSelectedArchives by rememberUpdatedState(selectedArchives)
    val latestPendingExportBytes by rememberUpdatedState(pendingExportBytes)
    DisposableEffect(authenticator) {
        onDispose {
            authenticator?.cancel()
            latestSelectedArchives.forEach { it.bytes.fill(0) }
            latestPendingExportBytes?.fill(0)
        }
    }

    SectionLabel(stringResource(R.string.minkboard_archives))
    Text(
        text = stringResource(R.string.minkboard_archives_description),
        color = Muted,
    )
    SettingsRow(
        title = stringResource(R.string.minkboard_import_archive),
        subtitle = stringResource(R.string.minkboard_import_archive_description),
        icon = Icons.Outlined.FileOpen,
        onClick = {
            statusMessage = null
            runCatching {
                openArchiveDocuments.launch(
                    arrayOf(
                        OpenMinkArchiveDocumentIo.MIME_TYPE,
                        "application/json",
                        "application/octet-stream",
                    ),
                )
            }.onFailure { failure ->
                statusMessage = failureMessage(failure)
                statusIsError = true
            }
        },
    )
    SettingsRow(
        title = stringResource(R.string.minkboard_export_archive),
        subtitle = stringResource(R.string.minkboard_export_archive_description),
        icon = Icons.Outlined.SaveAlt,
        enabled = boards.isNotEmpty(),
        onClick = {
            statusMessage = null
            selectedExportBoardIds = boards.mapTo(linkedSetOf()) { it.board.id }
            showExportSelection = true
        },
    )
    statusMessage?.let { message ->
        Text(
            text = message,
            color = if (statusIsError) MaterialTheme.colorScheme.error else Muted,
        )
    }

    MinkBoardArchiveExportDialog(
        visible = showExportSelection,
        boards = boards,
        selectedBoardIds = selectedExportBoardIds,
        onSelectionChange = { selectedExportBoardIds = it },
        onDismiss = { showExportSelection = false },
        onExport = {
            showExportSelection = false
            authorizeAndBeginExport()
        },
    )

    PortableArchiveWorkflowDialog(
        request = workflowRequest,
        errorMessage = workflowError,
        isWorking = workflowBusy,
        onDismiss = ::dismissWorkflow,
        onExport = { passphrase ->
            workflowError = null
            workflowBusy = true
            val exportBoardIds = selectedExportBoardIds
            val exportedBoards = boards.filter { it.board.id in exportBoardIds }
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                val result = try {
                    withContext(Dispatchers.Default) {
                        archiveResult {
                            archiveService.exportBoards(exportBoardIds, passphrase)
                        }
                    }
                } finally {
                    // The workflow hands ownership of this mutable secret to the host. Clear it
                    // even if the settings scope is cancelled before the codec gets scheduled.
                    passphrase.fill('\u0000')
                }
                workflowBusy = false
                result.fold(
                    onSuccess = { bytes ->
                        val suggestedFileName = suggestedOpenMinkArchiveName(exportedBoards)
                        workflowRequest = null
                        selectedExportBoardIds = emptySet()
                        pendingExportBytes?.fill(0)
                        pendingExportBytes = bytes
                        runCatching {
                            createArchiveDocument.launch(suggestedFileName)
                        }.onFailure { failure ->
                            pendingExportBytes?.fill(0)
                            pendingExportBytes = null
                            statusMessage = failureMessage(failure)
                            statusIsError = true
                        }
                    },
                    onFailure = { failure -> workflowError = failureMessage(failure) },
                )
            }
        },
        onImport = { passphrase, _ ->
            val index = selectedArchiveIndex
            val archive = selectedArchives.getOrNull(index)
            if (archive == null) {
                passphrase.fill('\u0000')
                workflowError = context.getString(R.string.minkboard_archive_selection_lost)
            } else {
                workflowError = null
                workflowBusy = true
                val ownedBytes = archive.bytes.copyOf()
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    val result = try {
                        withContext(Dispatchers.Default) {
                            archiveResult { archiveService.importBoards(ownedBytes, passphrase) }
                        }
                    } finally {
                        // See export above. The service also clears these after codec use; the host
                        // finally block covers cancellation before the service begins executing.
                        ownedBytes.fill(0)
                        passphrase.fill('\u0000')
                    }
                    workflowBusy = false
                    result.fold(
                        onSuccess = {
                            archive.bytes.fill(0)
                            pendingImportResults += PortableArchiveImportDisplayResult(
                                displayName = archive.displayName
                                    ?: context.getString(
                                        R.string.minkboard_archive_unnamed,
                                        index + 1,
                                    ),
                                failureMessage = null,
                            )
                            selectedArchiveIndex = index + 1
                            prepareCurrentImport()
                        },
                        onFailure = { failure -> workflowError = failureMessage(failure) },
                    )
                }
            }
        },
    )

    PortableArchiveImportResultsDialog(
        results = visibleImportResults,
        onDismiss = { visibleImportResults = emptyList() },
    )
}

private inline fun <T> archiveResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Exception) {
    Result.failure(failure)
}
