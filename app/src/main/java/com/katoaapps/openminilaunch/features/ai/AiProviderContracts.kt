package com.katoaapps.openminilaunch.features.ai

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

internal enum class AiHandoffMode {
    TEXT_SHARE,
    COPY_AND_LAUNCH,
}

internal enum class AiHandoffResult {
    SHARED,
    COPIED_AND_OPENED,
    FAILED;

    val succeeded: Boolean
        get() = this != FAILED
}

internal data class AiProvider(
    val id: String,
    @param:StringRes val labelRes: Int,
    val packageName: String,
    val alternatePackageNames: List<String> = emptyList(),
    val handoffMode: AiHandoffMode,
    val installUrl: String,
    @param:DrawableRes val bundledIconRes: Int,
)

internal val AiProvider.packageNames: List<String>
    get() = listOf(packageName) + alternatePackageNames

internal data class AiProviderOption(
    val id: String,
    val label: String,
    val installedPackageName: String?,
    val handoffMode: AiHandoffMode,
    val installed: Boolean,
    val canHandoff: Boolean,
    val installUrl: String,
    @param:DrawableRes val bundledIconRes: Int,
)
