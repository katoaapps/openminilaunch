package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Shared in-surface identity for every MinkSpace mini-app. */
@Composable
internal fun MinkSpaceTitle(
    title: String,
    icon: ImageVector,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.dp18),
            tint = contentColor,
        )
        Text(
            text = title,
            color = contentColor,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
