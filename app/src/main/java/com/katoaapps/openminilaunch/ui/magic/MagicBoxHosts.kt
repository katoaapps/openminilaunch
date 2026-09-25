package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.platform.DeviceActions

/** Home owns a persistent session and can present the collapsed keyboard-first entry bar. */
@Composable
internal fun HomeMagicBox(
    store: LauncherStore,
    actions: DeviceActions,
    sessionState: MagicBoxSessionState,
    keyboardInputEnabled: Boolean,
    autoOpenSoftwareKeyboardOnHome: Boolean,
    homeRequestToken: Int,
    onTodoAdded: (String) -> Unit,
    appAccessState: MinkAppAccessState,
    modifier: Modifier = Modifier,
    collapsedModifier: Modifier = Modifier,
) {
    MagicBoxContent(
        store = store,
        actions = actions,
        sessionState = sessionState,
        modifier = modifier,
        collapsedModifier = collapsedModifier,
        keyboardInputEnabled = keyboardInputEnabled,
        autoOpenSoftwareKeyboardOnHome = autoOpenSoftwareKeyboardOnHome,
        homeRequestToken = homeRequestToken,
        onTodoAdded = onTodoAdded,
        appAccessState = appAccessState,
    )
}

/** Android's assistant entry point uses the same feature in a temporary expanded session. */
@Composable
internal fun AssistantMagicBox(
    store: LauncherStore,
    actions: DeviceActions,
    onSessionComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MagicBoxContent(
        store = store,
        actions = actions,
        modifier = modifier,
        keyboardInputEnabled = true,
        initiallyExpanded = true,
        showSoftwareKeyboardOnStart = true,
        onSessionComplete = onSessionComplete,
    )
}
