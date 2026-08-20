package com.katoaapps.openminilaunch

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import java.util.ArrayDeque

class FileSearchRepository(private val context: Context) {
    private var indexedTrees: Set<String> = emptySet()
    private var documentIndex: List<FileSearchResult> = emptyList()
    private var indexedAt: Long = 0L
    private val thumbnailCache = object : LruCache<String, Bitmap>(THUMBNAIL_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun folderLabel(uri: Uri): String = runCatching {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        context.contentResolver.query(
            documentUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull() ?: context.getString(R.string.selected_folder)

    fun search(query: String, folders: List<SearchFolder>, includeMedia: Boolean): List<FileSearchResult> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        val results = mutableListOf<FileSearchResult>()
        if (includeMedia) results += searchMedia(clean)
        ensureDocumentIndex(folders)
        results += documentIndex.asSequence()
            .filter { it.name.contains(clean, ignoreCase = true) }
            .take(MAX_RESULTS_PER_TYPE)
        return results.distinctBy { it.uri }.sortedWith(
            compareBy<FileSearchResult> { !it.name.startsWith(clean, ignoreCase = true) }
                .thenByDescending { it.modifiedAt }
        )
    }

    fun loadThumbnail(result: FileSearchResult): Bitmap? {
        val cacheKey = "${result.uri}:${result.modifiedAt}"
        thumbnailCache.get(cacheKey)?.let { return it }
        val bitmap = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(result.uri, Size(320, 320), null)
            } else {
                decodeSampledThumbnail(result.uri, 320)
            }
        }.getOrNull()
        bitmap?.let { thumbnailCache.put(cacheKey, it) }
        return bitmap
    }

    fun invalidateFolders() {
        indexedTrees = emptySet()
        documentIndex = emptyList()
        indexedAt = 0L
    }

    private fun searchMedia(query: String): List<FileSearchResult> = listOf(
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
        MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO,
    ).flatMap { mediaType -> searchMediaType(query, mediaType) }

    private fun searchMediaType(query: String, mediaType: Int): List<FileSearchResult> = runCatching {
        val uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf(
            mediaType.toString(),
            "%$query%",
        )
        buildList {
            context.contentResolver.query(uri, projection, selection, args, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(projection[0])
                val name = cursor.getColumnIndexOrThrow(projection[1])
                val mime = cursor.getColumnIndexOrThrow(projection[2])
                val modified = cursor.getColumnIndexOrThrow(projection[3])
                while (cursor.moveToNext() && size < MAX_RESULTS_PER_TYPE) {
                    add(
                        FileSearchResult(
                            name = cursor.getString(name),
                            uri = ContentUris.withAppendedId(uri, cursor.getLong(id)),
                            mimeType = cursor.getString(mime) ?: "*/*",
                            modifiedAt = cursor.getLong(modified) * 1000L,
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun decodeSampledThumbnail(uri: Uri, targetSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > targetSize * 2 || bounds.outHeight / sampleSize > targetSize * 2) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun ensureDocumentIndex(folders: List<SearchFolder>) {
        val requestedTrees = folders.map { it.uri }.toSet()
        if (requestedTrees == indexedTrees && System.currentTimeMillis() - indexedAt < INDEX_REFRESH_INTERVAL_MS) return
        documentIndex = folders.flatMap { indexTree(Uri.parse(it.uri)) }
        indexedTrees = requestedTrees
        indexedAt = System.currentTimeMillis()
    }

    private fun indexTree(treeUri: Uri): List<FileSearchResult> = runCatching {
        val resolver = context.contentResolver
        val queue = ArrayDeque<String>()
        queue += DocumentsContract.getTreeDocumentId(treeUri)
        buildList {
            while (queue.isNotEmpty() && size < MAX_INDEXED_DOCUMENTS) {
                val parentId = queue.removeFirst()
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
                resolver.query(childrenUri, DOCUMENT_PROJECTION, null, null, null)?.use { cursor ->
                    val id = cursor.getColumnIndexOrThrow(DOCUMENT_PROJECTION[0])
                    val name = cursor.getColumnIndexOrThrow(DOCUMENT_PROJECTION[1])
                    val mime = cursor.getColumnIndexOrThrow(DOCUMENT_PROJECTION[2])
                    val modified = cursor.getColumnIndexOrThrow(DOCUMENT_PROJECTION[3])
                    while (cursor.moveToNext() && size < MAX_INDEXED_DOCUMENTS) {
                        val documentId = cursor.getString(id)
                        val mimeType = cursor.getString(mime) ?: "*/*"
                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            queue += documentId
                        } else {
                            add(
                                FileSearchResult(
                                    name = cursor.getString(name),
                                    uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                                    mimeType = mimeType,
                                    modifiedAt = cursor.getLong(modified),
                                )
                            )
                        }
                    }
                }
            }
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val MAX_INDEXED_DOCUMENTS = 20_000
        const val MAX_RESULTS_PER_TYPE = 6
        const val INDEX_REFRESH_INTERVAL_MS = 60_000L
        const val THUMBNAIL_CACHE_BYTES = 16 * 1024 * 1024
        val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}
