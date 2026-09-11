package com.katoaapps.openminilaunch.ui.settings

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.BuildConfig
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.backup.LauncherBackup
import com.katoaapps.openminilaunch.features.backup.LauncherBackupFile
import com.katoaapps.openminilaunch.features.backup.createPortableBackup
import com.katoaapps.openminilaunch.features.backup.restorePortableBackup
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
    val exportSuccess = stringResource(R.string.backup_exported)
    val exportFailure = stringResource(R.string.backup_export_failed)
    val importFailure = stringResource(R.string.backup_import_failed)
    val importSuccess = stringResource(R.string.backup_imported)

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val backup = store.createPortableBackup(BuildConfig.VERSION_NAME)
            scope.launch {
                val succeeded = withContext(Dispatchers.IO) {
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
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                createBackup.launch(context.getString(R.string.backup_filename, date))
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
    }

    pendingImport?.let { backup ->
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
                Text(
                    stringResource(
                        R.string.confirm_backup_import_description,
                        sourceVersion,
                        exportedAt,
                        backup.todos.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.restorePortableBackup(backup)
                        pendingImport = null
                        Toast.makeText(context, importSuccess, Toast.LENGTH_LONG).show()
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
