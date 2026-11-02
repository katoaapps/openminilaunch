package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens

internal enum class ConversationSection {
    MESSAGES,
    FAVORITES,
}

@Composable
internal fun ConversationSectionTabs(
    selected: ConversationSection,
    onSelected: (ConversationSection) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18, vertical = Dimens.dp6),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.dp16),
    ) {
        Row(Modifier.padding(Dimens.dp4), verticalAlignment = Alignment.CenterVertically) {
            ConversationSection.entries.forEach { section ->
                val isSelected = section == selected
                Surface(
                    onClick = { onSelected(section) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.dp12),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Text(
                        text = stringResource(
                            when (section) {
                                ConversationSection.MESSAGES -> R.string.messages
                                ConversationSection.FAVORITES -> R.string.favorites
                            },
                        ),
                        modifier = Modifier.padding(horizontal = Dimens.dp8, vertical = Dimens.dp10),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
