package com.katoaapps.openminilaunch.features.backup

import android.content.Context
import android.net.Uri
import java.io.Reader

internal object LauncherBackupFile {
    private const val MAX_BACKUP_CHARACTERS = 2_000_000

    fun write(context: Context, uri: Uri, backup: LauncherBackup): Result<Unit> = runCatching {
        val json = LauncherBackupCodec.encode(backup)
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(json)
        } ?: error("Unable to open backup destination")
    }

    fun read(context: Context, uri: Uri): Result<LauncherBackup> = runCatching {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readLimited()
        }
            ?: error("Unable to open backup file")
        LauncherBackupCodec.decode(json)
    }

    private fun Reader.readLimited(): String {
        val output = StringBuilder()
        val buffer = CharArray(8_192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(output.length + count <= MAX_BACKUP_CHARACTERS) { "Backup file is too large" }
            output.append(buffer, 0, count)
        }
        return output.toString()
    }
}
