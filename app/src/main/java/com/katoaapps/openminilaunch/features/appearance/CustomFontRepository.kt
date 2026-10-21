package com.katoaapps.openminilaunch.features.appearance

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class CustomFontRepository(context: Context) {
    private val appContext = context.applicationContext
    private val fontDirectory = File(appContext.filesDir, "appearance")
    val fontFile = File(fontDirectory, "custom-launcher-font")
    val hasFont: Boolean get() = fontFile.isFile

    fun import(uri: Uri): Result<String?> = runCatching {
        require(fontDirectory.exists() || fontDirectory.mkdirs()) {
            "The font storage directory could not be created"
        }
        val importedName = displayName(uri)
        val temporaryFile = File(fontDirectory, "custom-launcher-font.pending")
        try {
            appContext.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "The selected font could not be opened" }
                temporaryFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        require(totalBytes <= MAX_CUSTOM_FONT_BYTES) { "The selected font is too large" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            require(temporaryFile.length() > 0L) { "The selected font is empty" }
            Typeface.createFromFile(temporaryFile)
            runCatching {
                Files.move(
                    temporaryFile.toPath(),
                    fontFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }.getOrElse {
                Files.move(
                    temporaryFile.toPath(),
                    fontFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } finally {
            temporaryFile.delete()
        }
        importedName
    }

    private fun displayName(uri: Uri): String? {
        val queriedName = runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return queriedName?.takeIf(String::isNotBlank)?.take(MAX_CUSTOM_FONT_NAME_LENGTH)
    }

    private companion object {
        const val MAX_CUSTOM_FONT_BYTES = 32L * 1024L * 1024L
        const val MAX_CUSTOM_FONT_NAME_LENGTH = 120
    }
}
