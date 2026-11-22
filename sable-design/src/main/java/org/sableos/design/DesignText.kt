package org.sableos.design

/**
 * Every line Sable apps (shared design library) shows, in English, by string resource name.
 * res/values/strings.xml repeats them (DesignTextTest keeps the two equal) and
 * res/values-<lang>/strings.xml translates them.
 */
object DesignText {
    val EN: Map<String, String> =
        mapOf(
            "sable_playback_error" to "playback error",
            "sable_pause" to "Pause",
            "sable_play" to "Play",
            "sable_t_top_b_bottom_space" to "T top · B bottom · Space page · N/P next/previous",
            "sable_more_destinations" to "More destinations: %1\$s",
            "sable_more_actions" to "More actions",
            "sable_more_actions_for" to "More actions for %1\$s",
            "sable_jump_to" to "Jump to %1\$s",
            "sable_return_to" to "Return to %1\$s",
            "sable_undo" to "Undo",
        )

    val t = SableTexts(EN)
}
