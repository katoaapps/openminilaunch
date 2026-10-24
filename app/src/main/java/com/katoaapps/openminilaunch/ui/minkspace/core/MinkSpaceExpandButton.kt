package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Standard collapsed-state expansion action drawn by the MinkSpace host. */
@Composable
internal fun MinkSpaceExpandButton(
    expansion: MinkSpaceExpansionController,
    label: String,
    modifier: Modifier = Modifier,
) {
    if (!expansion.enabled || expansion.expanded) return

    Surface(
        shape = RoundedCornerShape(Dimens.dp96),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = Dimens.dp2,
        modifier = modifier.clickable(
            onClickLabel = label,
            role = Role.Button,
            onClick = expansion::expand,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.dp12, vertical = Dimens.dp7),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.OpenInFull, null, Modifier.size(Dimens.dp16))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
