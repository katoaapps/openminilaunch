package com.katoaapps.openminilaunch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Shared centered prompt for empty states and simple navigation gateways. */
@Composable
internal fun CenteredActionPrompt(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    contentColor: Color = LocalContentColor.current,
    mutedContentColor: Color = contentColor.copy(alpha = .68f),
    iconColor: Color = mutedContentColor,
    compact: Boolean = false,
    clickEntirePrompt: Boolean = false,
) {
    val promptModifier = if (clickEntirePrompt) {
        modifier.clickable(
            role = Role.Button,
            onClickLabel = actionLabel,
            onClick = onAction,
        )
    } else {
        modifier
    }
    val actionModifier = Modifier
        .padding(top = if (compact) Dimens.dp10 else Dimens.dp12)
        .heightIn(min = Dimens.dp48)
        .then(
            if (clickEntirePrompt) {
                Modifier
            } else {
                Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = actionLabel,
                    onClick = onAction,
                )
            },
        )

    Column(
        modifier = promptModifier.padding(
            horizontal = if (compact) Dimens.dp12 else Dimens.dp28,
            vertical = if (compact) Dimens.dp12 else Dimens.dp24,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(if (compact) Dimens.dp28 else Dimens.dp48),
            tint = iconColor,
        )
        Text(
            text = title,
            modifier = Modifier.padding(top = if (compact) Dimens.dp8 else Dimens.dp14),
            color = contentColor,
            fontWeight = FontWeight.Black,
            style = if (compact) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.titleLarge
            },
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            modifier = Modifier.padding(top = if (compact) Dimens.dp4 else Dimens.dp10),
            color = mutedContentColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Surface(
            modifier = actionModifier,
            shape = RoundedCornerShape(Dimens.dp96),
            color = contentColor.copy(alpha = .12f),
            contentColor = contentColor,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Dimens.dp14, vertical = Dimens.dp8),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actionIcon?.let { actionImage ->
                    Icon(
                        imageVector = actionImage,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.dp16),
                    )
                }
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
