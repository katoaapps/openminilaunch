package com.katoaapps.openminilaunch.ui.minkspace.todo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSettingsTarget
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceScope

/** Todo's complete MinkSpace adapter. The feature only receives platform primitives through [MinkSpaceScope]. */
internal class TodoMinkSpaceApp(
    private val store: LauncherStore,
    private val openTodos: () -> Unit,
    private val jumpToken: Int,
    private val itemsPerPage: Int,
    private val textMetrics: HomeTodoTextMetrics,
) : MinkSpaceApp {
    override val manifest: MinkSpaceMiniAppManifest = Manifest

    @Composable
    override fun Content(scope: MinkSpaceScope, modifier: Modifier) {
        scope.Surface(modifier = modifier) {
            TodoPager(
                store = store,
                openTodos = openTodos,
                jumpToken = jumpToken,
                itemsPerPage = itemsPerPage,
                textMetrics = textMetrics,
                compact = scope.compact,
                contentColor = scope.contentColor,
                mutedContentColor = scope.mutedContentColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    companion object {
        val Manifest = MinkSpaceMiniAppManifest(
            miniApp = MinkSpaceMiniApp.TODO,
            labelRes = R.string.todo_page_title,
            icon = Icons.Default.Checklist,
            settingsTarget = MinkSpaceMiniAppSettingsTarget.TODOS,
        )
    }
}
