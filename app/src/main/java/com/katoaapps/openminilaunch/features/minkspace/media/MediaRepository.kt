package com.katoaapps.openminilaunch.features.minkspace.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Persists only user-selected document URIs and their display metadata. */
internal class MediaRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val prefs = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mutableItems = mutableStateListOf<MediaItem>()
    val items: List<MediaItem> get() = mutableItems

    init {
        mutableItems.addAll(load())
    }

    fun add(uris: List<Uri>) {
        uris.forEach { uri ->
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val item = describe(uri)
            val existingIndex = mutableItems.indexOfFirst { it.uri == uri }
            if (existingIndex >= 0) mutableItems[existingIndex] = item else mutableItems.add(item)
        }
        save()
    }

    fun remove(item: MediaItem) {
        if (!mutableItems.remove(item)) return
        runCatching {
            resolver.releasePersistableUriPermission(item.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        save()
    }

    fun move(item: MediaItem, direction: Int) {
        val currentIndex = mutableItems.indexOf(item)
        if (currentIndex < 0) return
        val targetIndex = (currentIndex + direction).coerceIn(mutableItems.indices)
        if (currentIndex == targetIndex) return
        mutableItems.add(targetIndex, mutableItems.removeAt(currentIndex))
        save()
    }

    private fun describe(uri: Uri): MediaItem {
        var displayName: String? = null
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) displayName = cursor.getString(0)
            }
        }
        return MediaItem(
            uri = uri,
            displayName = displayName?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment.orEmpty(),
            mimeType = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" },
        )
    }

    private fun load(): List<MediaItem> = runCatching {
        val array = JSONArray(prefs.getString(KEY_ITEMS, "[]"))
        buildList {
            repeat(array.length()) { index ->
                val value = array.getJSONObject(index)
                add(
                    MediaItem(
                        uri = Uri.parse(value.getString("uri")),
                        displayName = value.getString("displayName"),
                        mimeType = value.getString("mimeType"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun save() {
        val array = JSONArray().apply {
            mutableItems.forEach { item ->
                put(
                    JSONObject()
                        .put("uri", item.uri.toString())
                        .put("displayName", item.displayName)
                        .put("mimeType", item.mimeType),
                )
            }
        }
        prefs.edit { putString(KEY_ITEMS, array.toString()) }
    }

    companion object {
        private const val PREFERENCES = "mink_space_media"
        private const val KEY_ITEMS = "items"

        @Volatile
        private var instance: MediaRepository? = null

        fun get(context: Context): MediaRepository = instance ?: synchronized(this) {
            instance ?: MediaRepository(context.applicationContext).also { instance = it }
        }
    }
}
