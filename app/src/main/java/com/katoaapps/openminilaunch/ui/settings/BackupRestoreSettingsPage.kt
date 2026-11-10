package com.katoaapps.openminilaunch.ui.settings

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.BuildConfig
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.backup.LauncherBackup
import com.katoaapps.openminilaunch.features.backup.LauncherBackupFile
import com.katoaapps.openminilaunch.features.backup.createPortableBackup
import com.katoaapps.openminilaunch.features.backup.restorePortableBackup
import com.katoaapps.openminilaunch.features.profile.ProfileState
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BackupRestoreSettingsPage(
    store: LauncherStore,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<LauncherBackup?>(null) }
    var confirmSensitiveExport by remember { mutableStateOf(false) }
    val includesMinkFavoriteContacts by produceState(
        initialValue = false,
        key1 = store.favoriteIntegratedContactStore,
    ) {
        value = withContext(Dispatchers.IO) {
            store.favoriteIntegratedContactStore.contacts().isNotEmpty()
        }
    }
    val includesFavoriteAttentionColors by produceState(
        initialValue = false,
        key1 = store.favoriteAttentionColorStore,
    ) {
        value = withContext(Dispatchers.IO) {
            store.favoriteAttentionColorStore.hasColorsForBackup()
        }
    }
    val exportSuccess = stringResource(R.string.backup_exported)
    val exportFailure = stringResource(R.string.backup_export_failed)
    val importFailure = stringResource(R.string.backup_import_failed)
    val importSuccess = stringResource(R.string.backup_imported)

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val succeeded = withContext(Dispatchers.IO) {
                    val backup = store.createPortableBackup(BuildConfig.VERSION_NAME)
                    LauncherBackupFile.write(context, uri, backup).isSuccess
                }
                val message = if (succeeded) exportSuccess else exportFailure
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) { LauncherBackupFile.read(context, uri) }
                result
                    .onSuccess { pendingImport = it }
                    .onFailure { Toast.makeText(context, importFailure, Toast.LENGTH_LONG).show() }
            }
        }
    }
    val beginExport = {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        createBackup.launch(context.getString(R.string.backup_filename, date))
    }

    SettingsPage(stringResource(R.string.backup_and_restore), goBack) {
        Text(
            stringResource(R.string.backup_restore_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        SettingsRow(
            title = stringResource(R.string.export_backup),
            subtitle = stringResource(R.string.export_backup_description),
            icon = Icons.Default.FileUpload,
            onClick = {
                if (
                    store.profileRepository.state is ProfileState.Ready ||
                    includesMinkFavoriteContacts ||
                    includesFavoriteAttentionColors
                ) {
                    confirmSensitiveExport = true
                } else {
                    beginExport()
                }
            },
        )
        SettingsRow(
            title = stringResource(R.string.import_backup),
            subtitle = stringResource(R.string.import_backup_description),
            icon = Icons.Default.FileDownload,
            onClick = { openBackup.launch(arrayOf("application/json", "text/plain")) },
        )
        Text(
            stringResource(R.string.backup_not_included_description),
            color = Muted,
            fontSize = Dimens.sp11,
            modifier = Modifier.padding(top = Dimens.dp4),
        )
        Text(
            stringResource(R.string.backup_minkspace_description),
            color = Muted,
            fontSize = Dimens.sp11,
            modifier = Modifier.padding(top = Dimens.dp4),
        )
        Text(
            stringResource(R.string.profile_backup_privacy_warning),
            color = Muted,
            fontSize = Dimens.sp11,
            modifier = Modifier.padding(top = Dimens.dp4),
        )
    }

    if (confirmSensitiveExport) {
        AlertDialog(
            onDismissRequest = { confirmSensitiveExport = false },
            title = { Text(stringResource(R.string.sensitive_backup_export_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                    Text(stringResource(R.string.profile_backup_privacy_warning))
                    if (includesFavoriteAttentionColors) {
                        Text(stringResource(R.string.favorite_attention_colors_backup_privacy_warning))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSensitiveExport = false
                        beginExport()
                    },
                ) { Text(stringResource(R.string.continue_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSensitiveExport = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingImport?.let { backup ->
        var restoreWidgetPlaceholders by remember(backup) { mutableStateOf(true) }
        val sourceVersion = backup.sourceAppVersion.ifBlank {
            context.getString(R.string.unknown_version)
        }
        val exportedAt = backup.exportedAtMillis.takeIf { it > 0L }?.let { timestamp ->
            DateFormat.getMediumDateFormat(context).format(Date(timestamp))
        } ?: stringResource(R.string.unknown_date)
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.confirm_backup_import)) },
            text = {
                Column {
                    Text(
                        stringResource(
                            R.string.confirm_backup_import_description,
                            sourceVersion,
                            exportedAt,
                            backup.todos.size,
                        ),
                    )
                    if (backup.integratedContacts != null) {
                        Text(
                            stringResource(R.string.favorite_contacts_backup_import_description),
                            color = Muted,
                            fontSize = Dimens.sp11,
                            modifier = Modifier.padding(top = Dimens.dp8),
                        )
                    }
                    if (
                        backup.favoriteAttentionColors?.isNotEmpty() == true ||
                        backup.androidFavoriteAttention != null
                    ) {
                        Text(
                            stringResource(R.string.favorite_attention_colors_backup_import_description),
                            color = Muted,
                            fontSize = Dimens.sp11,
                            modifier = Modifier.padding(top = Dimens.dp8),
                        )
                    }
                    if (backup.widgetRecipes.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = Dimens.dp12),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = restoreWidgetPlaceholders,
                                onCheckedChange = { restoreWidgetPlaceholders = it },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.restore_widget_placeholders))
                                Text(
                                    stringResource(
                                        R.string.restore_widget_placeholders_description,
                                        backup.widgetRecipes.size,
                                    ),
                                    color = Muted,
                                    fontSize = Dimens.sp11,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val widgetResult = store.restorePortableBackup(
                            backup,
                            restoreWidgetPlaceholders = restoreWidgetPlaceholders,
                        )
                        pendingImport = null
                        val message = if (widgetResult.skippedAtLimit > 0) {
                            context.getString(
                                R.string.backup_imported_widget_limit,
                                widgetResult.skippedAtLimit,
                            )
                        } else {
                            importSuccess
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    },
                ) { Text(stringResource(R.string.import_backup)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
