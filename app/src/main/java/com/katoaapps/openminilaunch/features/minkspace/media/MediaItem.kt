package com.katoaapps.openminilaunch.features.minkspace.media

import android.net.Uri

internal data class MediaItem(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}
