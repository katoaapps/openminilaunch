package com.katoaapps.openminilaunch.features.contacts

/** One installed app that can handle an Android dial request. */
internal data class CallingAppOption(
    val packageName: String,
    val label: String,
)
