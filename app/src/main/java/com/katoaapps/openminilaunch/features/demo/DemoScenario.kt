package com.katoaapps.openminilaunch.features.demo

import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R

/**
 * Stable, named fixture sets for repeatable screenshots and product demonstrations.
 *
 * Scenarios own fictional content. [DemoHomeProfile] remains a separate visual choice so a
 * capture can change colors without silently changing the story being demonstrated.
 */
internal enum class DemoScenario(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    PRODUCT_TOUR(
        R.string.demo_scenario_product_tour,
        R.string.demo_scenario_product_tour_description,
    ),
    PET_MINK(
        R.string.demo_scenario_pet_mink,
        R.string.demo_scenario_pet_mink_description,
    ),
    WORKDAY(
        R.string.demo_scenario_workday,
        R.string.demo_scenario_workday_description,
    ),
    ;

    companion object {
        fun fromStoredName(value: String?): DemoScenario =
            entries.firstOrNull { it.name == value } ?: PRODUCT_TOUR
    }
}

/** Controls how busy the fictional Conversations surface is for a capture. */
internal enum class DemoConversationState(
    @param:StringRes val labelRes: Int,
) {
    QUIET(R.string.demo_conversation_state_quiet),
    ONE_FAVORITE(R.string.demo_conversation_state_one_favorite),
    ACTIVE_INBOX(R.string.demo_conversation_state_active_inbox),
    ;

    companion object {
        fun fromStoredName(value: String?): DemoConversationState =
            entries.firstOrNull { it.name == value } ?: ACTIVE_INBOX
    }
}

/** A safe, reproducible destination prepared directly from the hidden Demo Studio. */
internal enum class DemoCaptureMoment(
    @param:StringRes val labelRes: Int,
) {
    HOME(R.string.demo_capture_moment_home),
    QUICK_APPS(R.string.demo_capture_moment_quick_apps),
    CONVERSATIONS(R.string.demo_capture_moment_conversations),
    MAGIC_BOX_SEARCH(R.string.demo_capture_moment_magic_box),
}
