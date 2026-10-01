package com.katoaapps.openminilaunch.features.minkspace.music

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.core.graphics.drawable.toBitmap
import java.util.Locale

/** Converts framework media-session objects into the small model consumed by Compose. */
@Suppress("DEPRECATION")
internal fun MediaController.toMusicPlaybackState(
    context: Context,
    previousState: MusicPlaybackState? = null,
): MusicPlaybackState {
    val sessionMetadata = metadata
    val playback = playbackState
    val description = sessionMetadata?.description
    val duration = sessionMetadata
        ?.getLong(MediaMetadata.METADATA_KEY_DURATION)
        ?.coerceAtLeast(0L)
        ?: 0L
    val actions = playback?.actions ?: 0L
    val sourcePackage = packageName
    val appInfo = runCatching {
        context.packageManager.getApplicationInfo(sourcePackage, 0)
    }.getOrNull()
    val appName = appInfo?.let {
        context.packageManager.getApplicationLabel(it).toString()
    }.orEmpty().ifBlank { sourcePackage }
    val appIcon = previousState
        ?.takeIf { it.packageName == sourcePackage }
        ?.appIcon
        ?: appInfo?.let {
            runCatching {
                context.packageManager.getApplicationIcon(it)
                    .toBitmap(APP_ICON_SIZE_PX, APP_ICON_SIZE_PX)
            }.getOrNull()
        }
    val artwork = sessionMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: sessionMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: sessionMetadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        ?: description?.iconBitmap

    return MusicPlaybackState(
        availability = MusicPlaybackAvailability.READY,
        packageName = sourcePackage,
        appName = appName,
        title = firstText(
            description?.title,
            sessionMetadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            sessionMetadata?.getText(MediaMetadata.METADATA_KEY_TITLE),
        ),
        subtitle = firstText(
            description?.subtitle,
            description?.description,
            sessionMetadata?.getText(MediaMetadata.METADATA_KEY_ARTIST),
            sessionMetadata?.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            sessionMetadata?.getText(MediaMetadata.METADATA_KEY_ALBUM),
        ),
        artwork = artwork,
        appIcon = appIcon,
        durationMs = duration,
        positionMs = playback.currentPosition(duration),
        isPlaying = playback.isActivelyPlaying(),
        canTogglePlayback = if (playback.isActivelyPlaying()) {
            actions.supportsAny(PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)
        } else {
            actions.supportsAny(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PLAY_PAUSE)
        },
        canSeek = actions.supports(PlaybackState.ACTION_SEEK_TO),
        canSkipPrevious = actions.supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
        canSkipNext = actions.supports(PlaybackState.ACTION_SKIP_TO_NEXT),
        canShuffle = playback.shuffleAction() != null,
    )
}

internal fun MediaController.hasMeaningfulMedia(): Boolean =
    metadata?.description?.title?.isNotBlank() == true ||
        playbackState?.state !in setOf(null, PlaybackState.STATE_NONE)

internal fun PlaybackState?.isActivelyPlaying(): Boolean = when (this?.state) {
    PlaybackState.STATE_PLAYING,
    PlaybackState.STATE_BUFFERING,
    PlaybackState.STATE_CONNECTING -> true
    else -> false
}

internal fun PlaybackState?.currentPosition(durationMs: Long): Long {
    this ?: return 0L
    val elapsed = if (isActivelyPlaying() && lastPositionUpdateTime > 0L) {
        (SystemClock.elapsedRealtime() - lastPositionUpdateTime).coerceAtLeast(0L)
    } else {
        0L
    }
    val estimated = position + (elapsed * playbackSpeed).toLong()
    return if (durationMs > 0L) {
        estimated.coerceIn(0L, durationMs)
    } else {
        estimated.coerceAtLeast(0L)
    }
}

internal fun PlaybackState?.shuffleAction(): PlaybackState.CustomAction? =
    this?.customActions?.firstOrNull { action ->
        action.action.contains("shuffle", ignoreCase = true) ||
            action.name.toString().lowercase(Locale.ROOT).contains("shuffle")
    }

private fun Long.supports(action: Long): Boolean = (this and action) != 0L

private fun Long.supportsAny(actions: Long): Boolean = (this and actions) != 0L

private fun firstText(vararg values: CharSequence?): String =
    values.firstNotNullOfOrNull { value -> value?.toString()?.takeIf(String::isNotBlank) }.orEmpty()

private const val APP_ICON_SIZE_PX = 192
