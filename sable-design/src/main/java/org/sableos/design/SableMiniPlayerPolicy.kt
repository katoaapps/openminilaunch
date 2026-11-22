package org.sableos.design

import org.sableos.design.DesignText.t

/**
 * Media mini-player content rules (DESIGN-KF-D part 2).
 *
 * ```text
 * minimum: title or source · play/pause · state/progress where meaningful
 * SOLID_ACCENT_BAR_WITHOUT_MEANING=FORBIDDEN
 * PLAYBACK_STATE_AMBIGUOUS=FORBIDDEN
 * ```
 */
enum class SablePlaybackState {
    Playing,
    Paused,
    Buffering,
    Stopped,
    Ended,
    Error,

    /** Nothing loaded. */
    Idle,
}

enum class SableMiniPlayerToggle {
    Play,
    Pause,
}

data class SableMiniPlayerInput(
    val title: String?,
    val source: String?,
    val state: SablePlaybackState,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    /** Live streams (radio) have no meaningful progress. */
    val live: Boolean = false,
)

data class SableMiniPlayerModel(
    /** Title, or the source when there is no title. Never blank. */
    val primary: String,
    /** Source/artist when the title is shown; null otherwise. */
    val secondary: String?,
    /** Always present: "playing", "paused", "buffering", "live", ... */
    val stateLabel: String,
    val toggle: SableMiniPlayerToggle,
    /** Accessible label of the toggle button ("Pause", "Play"). */
    val toggleLabel: String,
    /** 0..1 when duration is known and the item is not live; null hides the indicator. */
    val progress: Float?,
    val contentDescription: String,
)

object SableMiniPlayerPolicy {
    /** Returns null when there is nothing meaningful to show: no bar at all instead of an empty accent bar. */
    fun present(input: SableMiniPlayerInput): SableMiniPlayerModel? {
        val title = input.title?.trim().orEmpty()
        val source = input.source?.trim().orEmpty()
        if (input.state == SablePlaybackState.Idle) return null
        if (title.isEmpty() && source.isEmpty()) return null

        val primary = title.ifEmpty { source }
        val secondary = source.takeIf { title.isNotEmpty() && it.isNotEmpty() && it != title }
        val stateLabel =
            when (input.state) {
                SablePlaybackState.Playing -> if (input.live) "live" else "playing"
                SablePlaybackState.Paused -> "paused"
                SablePlaybackState.Buffering -> "buffering"
                SablePlaybackState.Stopped -> "stopped"
                SablePlaybackState.Ended -> "finished"
                SablePlaybackState.Error -> t("sable_playback_error")
                SablePlaybackState.Idle -> "idle"
            }
        val toggle =
            when (input.state) {
                SablePlaybackState.Playing, SablePlaybackState.Buffering -> SableMiniPlayerToggle.Pause
                else -> SableMiniPlayerToggle.Play
            }
        val toggleLabel = if (toggle == SableMiniPlayerToggle.Pause) t("sable_pause") else t("sable_play")
        val progress =
            if (!input.live && input.durationMs > 0L && input.state != SablePlaybackState.Stopped) {
                (input.positionMs.toFloat() / input.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
        val description =
            listOfNotNull(primary, secondary, stateLabel).joinToString(", ")
        return SableMiniPlayerModel(
            primary = primary,
            secondary = secondary,
            stateLabel = stateLabel,
            toggle = toggle,
            toggleLabel = toggleLabel,
            progress = progress,
            contentDescription = description,
        )
    }
}
