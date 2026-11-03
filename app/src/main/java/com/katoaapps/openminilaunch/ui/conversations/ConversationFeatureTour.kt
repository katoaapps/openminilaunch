package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Forum
import androidx.compose.runtime.Composable
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.PagedTutorialDialog
import com.katoaapps.openminilaunch.ui.components.TutorialPage

private val conversationFeatureTourPages = listOf(
    TutorialPage(
        icon = Icons.Default.Forum,
        titleRes = R.string.messages,
        descriptionRes = R.string.conversation_tour_messages_description,
    ),
    TutorialPage(
        icon = Icons.Default.Contacts,
        titleRes = R.string.favorites,
        descriptionRes = R.string.conversation_tour_favorites_description,
    ),
)

@Composable
internal fun ConversationFeatureTour(onDismiss: () -> Unit) {
    PagedTutorialDialog(
        titleRes = R.string.conversations,
        pages = conversationFeatureTourPages,
        onDismiss = onDismiss,
    )
}
