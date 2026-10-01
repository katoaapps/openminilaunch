package com.katoaapps.openminilaunch.features.minkspace.music

import android.graphics.Bitmap

/** User-visible state for the active system media session. */
internal data class MusicPlaybackState(
    val availability: MusicPlaybackAvailability,
    val packageName: String? = null,
    val appName: String = "",
    val title: String = "",
    val subtitle: String = "",
    val artwork: Bitmap? = null,
    val appIcon: Bitmap? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val canTogglePlayback: Boolean = false,
    val canSeek: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val canShuffle: Boolean = false,
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val hasSession: Boolean get() = availability == MusicPlaybackAvailability.READY
}

internal enum class MusicPlaybackAvailability {
    ACCESS_REQUIRED,
    NOTHING_PLAYING,
    READY,
}
