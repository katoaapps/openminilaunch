package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.ui.minkspace.calculator.CalculatorMinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.media.MediaMinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.music.MusicMinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.profile.VCardMinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.todo.TodoMinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.widget.WidgetMinkSpaceApp

/** The one registration point for built-in MinkSpace mini-apps. */
internal object MinkSpaceMiniAppCatalog {
    private val registrations = listOf(
        MinkSpaceMiniAppRegistration(TodoMinkSpaceApp.Manifest) { dependencies ->
            TodoMinkSpaceApp(
                store = dependencies.store,
                openTodos = dependencies.openTodos,
                jumpToken = dependencies.todoJumpToken,
                itemsPerPage = dependencies.todoItemsPerPage,
                textMetrics = dependencies.todoTextMetrics,
            )
        },
        MinkSpaceMiniAppRegistration(CalculatorMinkSpaceApp.Manifest) {
            CalculatorMinkSpaceApp()
        },
        MinkSpaceMiniAppRegistration(MediaMinkSpaceApp.Manifest) { dependencies ->
            MediaMinkSpaceApp(onManageMedia = dependencies.openMediaLab)
        },
        MinkSpaceMiniAppRegistration(
            manifest = MinkSpaceMiniAppManifest(
                miniApp = MinkSpaceMiniApp.RECORDER,
                labelRes = R.string.recorder,
                icon = Icons.Default.Mic,
            ),
            create = null,
        ),
        MinkSpaceMiniAppRegistration(MusicMinkSpaceApp.Manifest) {
            MusicMinkSpaceApp()
        },
        MinkSpaceMiniAppRegistration(WidgetMinkSpaceApp.Manifest) { dependencies ->
            WidgetMinkSpaceApp(
                store = dependencies.store,
                actions = dependencies.actions,
            )
        },
        MinkSpaceMiniAppRegistration(VCardMinkSpaceApp.Manifest) { dependencies ->
            VCardMinkSpaceApp(
                store = dependencies.store,
                openVCardSettings = dependencies.openVCardSettings,
            )
        },
    )

    private val byMiniApp = registrations.associateBy { it.manifest.miniApp }

    init {
        check(
            registrations.size == MinkSpaceMiniApp.entries.size &&
                byMiniApp.keys == MinkSpaceMiniApp.entries.toSet(),
        ) {
            "Every MinkSpaceMiniApp must have exactly one catalog registration"
        }
        check(registrations.none { it.manifest.miniApp.readyForHome && it.create == null }) {
            "Every ready MinkSpace mini-app must provide a runtime implementation"
        }
    }

    fun get(miniApp: MinkSpaceMiniApp): MinkSpaceMiniAppManifest =
        checkNotNull(byMiniApp[miniApp]) {
            "Missing MinkSpace manifest for ${miniApp.stableId}"
        }.manifest

    fun createRegistry(dependencies: MinkSpaceHostDependencies): MinkSpaceMiniAppRegistry =
        MinkSpaceMiniAppRegistry(
            registrations.mapNotNull { registration ->
                registration.create?.invoke(dependencies)?.also { app ->
                    check(app.manifest == registration.manifest) {
                        "Factory manifest does not match the catalog entry for " +
                            registration.manifest.miniApp.stableId
                    }
                }
            },
        )
}
