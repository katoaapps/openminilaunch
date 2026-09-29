package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
        onTodoAdded = { text, animationFinished ->
            onTodoAdded(text)
            animationFinished()
        },
        appAccessState = appAccessState,
    )
}

/** Android's assistant entry point uses the same feature in a temporary expanded session. */
@Composable
internal fun AssistantMagicBox(
    store: LauncherStore,
    actions: DeviceActions,
    onSessionComplete: () -> Unit,
    onBubbleLaunched: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var todoFeedback by remember { mutableStateOf<AssistantTodoFeedback?>(null) }
    val todoFlightProgress = remember { Animatable(0f) }

    LaunchedEffect(todoFeedback) {
        val feedback = todoFeedback ?: return@LaunchedEffect
        todoFlightProgress.snapTo(0f)
        todoFlightProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1_300, easing = FastOutSlowInEasing),
        )
        todoFeedback = null
        feedback.onAnimationFinished()
    }

    Box(modifier) {
        MagicBoxContent(
            store = store,
            actions = actions,
            modifier = Modifier.fillMaxSize(),
            keyboardInputEnabled = true,
            initiallyExpanded = true,
            showSoftwareKeyboardOnStart = true,
            onTodoAdded = { text, animationFinished ->
                todoFeedback = AssistantTodoFeedback(text, animationFinished)
            },
            onSessionComplete = onSessionComplete,
            onBubbleLaunched = onBubbleLaunched,
        )
        todoFeedback?.let { feedback ->
            AssistantTodoFlightChip(
                text = feedback.text,
                progress = todoFlightProgress,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private data class AssistantTodoFeedback(
    val text: String,
    val onAnimationFinished: () -> Unit,
)
