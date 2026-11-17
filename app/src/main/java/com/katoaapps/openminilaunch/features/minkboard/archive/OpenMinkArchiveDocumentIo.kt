package com.katoaapps.openminilaunch.features.minkboard.archive

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal data class OpenMinkSelectedArchive(
    val displayName: String?,
    val bytes: ByteArray,
)

/** Bounded SAF document IO for portable board archives. */
internal object OpenMinkArchiveDocumentIo {
    const val MIME_TYPE = "application/vnd.katoaapps.minkboard.archive+json"
    const val MAX_ARCHIVE_BYTES = 32 * 1024 * 1024
    const val MAX_BATCH_ARCHIVES = 32
    const val MAX_BATCH_BYTES = 128L * 1024L * 1024L

    fun readArchives(
        contentResolver: ContentResolver,
        uris: List<Uri>,
    ): List<OpenMinkSelectedArchive> {
        require(uris.size <= MAX_BATCH_ARCHIVES) {
            "Choose no more than $MAX_BATCH_ARCHIVES archives at once"
        }
        val archives = mutableListOf<OpenMinkSelectedArchive>()
        var totalBytes = 0L
        try {
            uris.forEach { uri ->
                val archive = readArchive(contentResolver, uri)
                try {
                    totalBytes += archive.bytes.size
                    require(totalBytes <= MAX_BATCH_BYTES) {
                        "The selected archive batch is too large"
                    }
                    archives += archive
                } catch (failure: Exception) {
                    archive.bytes.fill(0)
                    throw failure
                }
            }
            return archives
        } catch (failure: Exception) {
            archives.forEach { it.bytes.fill(0) }
            throw failure
        }
    }

    fun writeArchive(
        contentResolver: ContentResolver,
        uri: Uri,
        bytes: ByteArray,
    ) {
        require(bytes.isNotEmpty()) { "Archive cannot be empty" }
        require(bytes.size <= MAX_ARCHIVE_BYTES) { "Archive is too large" }
        val stream = contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalArgumentException("The archive destination could not be opened")
        stream.use { output ->
            output.write(bytes)
            output.flush()
        }
    }

    fun readBounded(
        input: InputStream,
        maxBytes: Int = MAX_ARCHIVE_BYTES,
    ): ByteArray {
        require(maxBytes >= 0)
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= maxBytes) { "Archive is too large" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun readArchive(
        contentResolver: ContentResolver,
        uri: Uri,
    ): OpenMinkSelectedArchive {
        val displayName = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        val bytes = contentResolver.openInputStream(uri)?.use(::readBounded)
            ?: throw IllegalArgumentException("The selected archive could not be opened")
        return OpenMinkSelectedArchive(displayName, bytes)
    }
}
