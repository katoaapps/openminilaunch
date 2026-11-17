package com.katoaapps.minkboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

sealed interface PortableArchiveWorkflowRequest {
    data object Export : PortableArchiveWorkflowRequest

    data class Import(
        val displayName: String?,
        val requiresPassphrase: Boolean,
        val requiresBoardName: Boolean,
        val source: PortableArchiveImportSource = PortableArchiveImportSource.MinkBoard,
        val batchPosition: Int = 1,
        val batchSize: Int = 1,
    ) : PortableArchiveWorkflowRequest
}

enum class PortableArchiveImportSource {
    MinkBoard,
    Spencer,
}

/**
 * Reusable passphrase/import-policy UI. The host owns and must clear callback CharArrays.
 * Passphrases use remember rather than rememberSaveable so they never enter saved instance state.
 */
@Composable
fun PortableArchiveWorkflowDialog(
    request: PortableArchiveWorkflowRequest?,
    errorMessage: String?,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onExport: (CharArray) -> Unit,
    onImport: (CharArray, String?) -> Unit,
) {
    if (request == null) return
    var passphrase by remember(request) { mutableStateOf("") }
    var confirmation by remember(request) { mutableStateOf("") }
    var boardName by remember(request) { mutableStateOf("") }

    fun dismissAndClear() {
        passphrase = ""
        confirmation = ""
        boardName = ""
        onDismiss()
    }

    val isExport = request is PortableArchiveWorkflowRequest.Export
    val importRequest = request as? PortableArchiveWorkflowRequest.Import
    val requiresPassphrase = isExport || importRequest?.requiresPassphrase == true
    val validLength = !requiresPassphrase || passphrase.length >= MIN_PASSPHRASE_LENGTH
    val hasRequiredBoardName = importRequest?.requiresBoardName != true || boardName.isNotBlank()
    val canSubmit = validLength && hasRequiredBoardName && (!isExport || passphrase == confirmation)
    MinkBoardAlertDialog(
        onDismissRequest = { if (!isWorking) dismissAndClear() },
        title = {
            Text(
                stringResource(
                    when {
                        isExport -> R.string.archive_export_title
                        importRequest?.source == PortableArchiveImportSource.Spencer ->
                            R.string.archive_spencer_import_title
                        else -> R.string.archive_import_title
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (request is PortableArchiveWorkflowRequest.Import) {
                    if (request.batchSize > 1) {
                        Text(
                            stringResource(
                                R.string.archive_import_progress,
                                request.batchPosition,
                                request.batchSize,
                            ),
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        )
                    }
                    request.displayName?.let {
                        Text(
                            it,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                if (isWorking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(
                            if (isExport) {
                                R.string.archive_export_working
                            } else {
                                R.string.archive_import_working
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (request is PortableArchiveWorkflowRequest.Import) {
                    Text(stringResource(R.string.archive_import_additive_help))
                    if (request.requiresBoardName) {
                        Text(stringResource(R.string.archive_spencer_mapping_help))
                        OutlinedTextField(
                            value = boardName,
                            onValueChange = { boardName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.archive_board_name)) },
                            supportingText = {
                                Text(stringResource(R.string.archive_board_name_help))
                            },
                            singleLine = true,
                        )
                    }
                }
                if (!isWorking && requiresPassphrase) {
                    ArchivePasswordField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text(stringResource(R.string.archive_passphrase)) },
                        supportingText = {
                            Text(
                                stringResource(
                                    if (importRequest?.batchSize?.let { it > 1 } == true) {
                                        R.string.archive_batch_passphrase_help
                                    } else {
                                        R.string.archive_passphrase_help
                                    },
                                    MIN_PASSPHRASE_LENGTH,
                                ),
                            )
                        },
                    )
                }
                if (!isWorking && isExport) {
                    ArchivePasswordField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text(stringResource(R.string.archive_confirm_passphrase)) },
                        isError = confirmation.isNotEmpty() && confirmation != passphrase,
                    )
                }
                if (!isWorking) {
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            if (!isWorking) {
                TextButton(
                    enabled = canSubmit,
                    onClick = {
                        if (isExport) {
                            val ownedPassphrase = passphrase.toCharArray()
                            passphrase = ""
                            confirmation = ""
                            onExport(ownedPassphrase)
                        } else {
                            val ownedPassphrase = passphrase.toCharArray()
                            passphrase = ""
                            onImport(
                                ownedPassphrase,
                                boardName.trim().takeIf(String::isNotEmpty),
                            )
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            if (isExport) R.string.archive_export_action else R.string.archive_import_action,
                        ),
                    )
                }
            }
        },
        dismissButton = {
            if (!isWorking) {
                TextButton(onClick = ::dismissAndClear) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}

data class PortableArchiveImportDisplayResult(
    val displayName: String,
    val failureMessage: String?,
)

@Composable
fun PortableArchiveImportResultsDialog(
    results: List<PortableArchiveImportDisplayResult>,
    onDismiss: () -> Unit,
) {
    if (results.isEmpty()) return
    val successCount = results.count { it.failureMessage == null }
    MinkBoardAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.archive_import_results_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(
                        R.string.archive_import_results_summary,
                        successCount,
                        results.size,
                    ),
                )
                results.forEach { result ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (result.failureMessage == null) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = if (result.failureMessage == null) {
                                androidx.compose.material3.MaterialTheme.colorScheme.primary
                            } else {
                                androidx.compose.material3.MaterialTheme.colorScheme.error
                            },
                        )
                        Column {
                            Text(result.displayName)
                            result.failureMessage?.let { message ->
                                Text(
                                    message,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
    )
}

@Composable
private fun ArchivePasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit),
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    val visibilityLabel = stringResource(
        if (visible) R.string.archive_hide_passphrase else R.string.archive_show_passphrase,
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = label,
        supportingText = supportingText,
        isError = isError,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = visibilityLabel,
                )
            }
        },
    )
}

private const val MIN_PASSPHRASE_LENGTH = 8
