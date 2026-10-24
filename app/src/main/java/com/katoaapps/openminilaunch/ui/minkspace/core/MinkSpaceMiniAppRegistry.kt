package com.katoaapps.openminilaunch.ui.minkspace.core

import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics

/** Host-owned dependencies used only while constructing feature-specific mini-apps. */
internal data class MinkSpaceHostDependencies(
    val store: LauncherStore,
    val actions: DeviceActions,
    val openTodos: () -> Unit,
    val todoJumpToken: Int,
    val todoItemsPerPage: Int,
    val todoTextMetrics: HomeTodoTextMetrics,
    val openMediaLab: () -> Unit,
    val openVCardSettings: () -> Unit,
)

/** One catalog entry: static metadata plus an optional runtime implementation factory. */
internal data class MinkSpaceMiniAppRegistration(
    val manifest: MinkSpaceMiniAppManifest,
    val create: ((MinkSpaceHostDependencies) -> MinkSpaceApp)?,
)

internal class MinkSpaceMiniAppRegistry(
    apps: List<MinkSpaceApp>,
) {
    private val byId = apps.associateBy { it.manifest.miniApp }

    init {
        check(byId.size == apps.size) { "MinkSpace registry contains duplicate mini-app IDs" }
    }

    fun get(miniApp: MinkSpaceMiniApp): MinkSpaceApp =
        checkNotNull(byId[miniApp]) { "No runtime MinkSpace app registered for ${miniApp.stableId}" }
}
