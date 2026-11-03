package com.katoaapps.openminilaunch.features.demo

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.todo.TodoItem
import com.katoaapps.openminilaunch.model.AppGridSlot

internal enum class DemoHomeProfile(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:ColorRes val panelColorRes: Int,
    @param:ColorRes val backgroundColorRes: Int,
) {
    CLASSIC(
        R.string.demo_profile_classic,
        R.string.demo_profile_classic_description,
        R.color.mink_forest,
        R.color.demo_app_background,
    ),
    WORK(
        R.string.demo_profile_work,
        R.string.demo_profile_work_description,
        R.color.home_panel_navy,
        R.color.app_background_paper,
    ),
    NIGHT(
        R.string.demo_profile_night,
        R.string.demo_profile_night_description,
        R.color.home_panel_plum,
        R.color.app_background_midnight,
    ),
    PERSONAL(
        R.string.demo_profile_personal,
        R.string.demo_profile_personal_description,
        R.color.home_panel_mink,
        R.color.app_background_cream,
    ),
    FRESH(
        R.string.demo_profile_fresh,
        R.string.demo_profile_fresh_description,
        R.color.home_panel_charcoal,
        R.color.app_background_sage,
    ),
    ;

    companion object {
        fun fromStoredName(value: String?): DemoHomeProfile =
            entries.firstOrNull { it.name == value } ?: CLASSIC
    }
}

/** Repeatable, non-persistent Home scenes used only while hidden demo mode is enabled. */
internal object DemoHomeData {
    fun todos(profile: DemoHomeProfile = DemoHomeProfile.CLASSIC): List<TodoItem> =
        when (profile) {
            DemoHomeProfile.CLASSIC -> listOf(
                todo(profile, 1, "Send revised deck to Maya"),
                todo(profile, 2, "Book train tickets for Friday"),
                todo(profile, 3, "Call Kara after work", completed = true),
                todo(profile, 4, "Review hosting estimate"),
                todo(profile, 5, "Confirm dinner reservation"),
            )
            DemoHomeProfile.WORK -> listOf(
                todo(profile, 1, "Review the Q3 budget before stand-up"),
                todo(profile, 2, "Send the revised invoice to Noah"),
                todo(profile, 3, "Confirm Thursday's project review"),
                todo(profile, 4, "Add launch notes to the shared doc", completed = true),
                todo(profile, 5, "Book a focus room for 2:30"),
            )
            DemoHomeProfile.NIGHT -> listOf(
                todo(profile, 1, "Set the morning alarm for 6:30"),
                todo(profile, 2, "Read twenty pages before bed"),
                todo(profile, 3, "Charge the keyboard phone"),
                todo(profile, 4, "Put tomorrow's keys by the door", completed = true),
                todo(profile, 5, "Turn off the living room lights"),
            )
            DemoHomeProfile.PERSONAL -> listOf(
                todo(profile, 1, "Pick up film after work"),
                todo(profile, 2, "Water the basil on the balcony"),
                todo(profile, 3, "Message Maya about Friday dinner"),
                todo(profile, 4, "Order stickers for the concert", completed = true),
                todo(profile, 5, "Find the ramen place Kara mentioned"),
            )
            DemoHomeProfile.FRESH -> listOf(
                todo(profile, 1, "Finish the launcher demo GIF"),
                todo(profile, 2, "Resize the calendar widget"),
                todo(profile, 3, "Write the release notes"),
                todo(profile, 4, "Capture the new Home colors", completed = true),
                todo(profile, 5, "Test the download link on mobile"),
            )
        }

    fun appGridOrder(profile: DemoHomeProfile): List<AppGridSlot> = when (profile) {
        DemoHomeProfile.CLASSIC -> AppGridSlot.entries
        DemoHomeProfile.WORK -> listOf(
            AppGridSlot.FILES, AppGridSlot.EVENT, AppGridSlot.NOTE, AppGridSlot.EXTRA,
            AppGridSlot.MESSAGE, AppGridSlot.CALL, AppGridSlot.WEATHER, AppGridSlot.DRAWER,
        )
        DemoHomeProfile.NIGHT -> listOf(
            AppGridSlot.WEATHER, AppGridSlot.EXTRA, AppGridSlot.NOTE, AppGridSlot.EVENT,
            AppGridSlot.CALL, AppGridSlot.MESSAGE, AppGridSlot.FILES, AppGridSlot.DRAWER,
        )
        DemoHomeProfile.PERSONAL -> listOf(
            AppGridSlot.MESSAGE, AppGridSlot.CALL, AppGridSlot.EVENT, AppGridSlot.EXTRA,
            AppGridSlot.NOTE, AppGridSlot.WEATHER, AppGridSlot.FILES, AppGridSlot.DRAWER,
        )
        DemoHomeProfile.FRESH -> listOf(
            AppGridSlot.NOTE, AppGridSlot.FILES, AppGridSlot.EVENT, AppGridSlot.EXTRA,
            AppGridSlot.MESSAGE, AppGridSlot.WEATHER, AppGridSlot.CALL, AppGridSlot.DRAWER,
        )
    }

    private fun todo(
        profile: DemoHomeProfile,
        index: Int,
        text: String,
        completed: Boolean = false,
    ) = TodoItem("demo-${profile.name.lowercase(java.util.Locale.ROOT)}-$index", text, completed)
}
