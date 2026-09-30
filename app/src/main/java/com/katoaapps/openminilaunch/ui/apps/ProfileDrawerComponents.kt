package com.katoaapps.openminilaunch.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun ProfileDrawerHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClose: () -> Unit,
) {
    Row(
        Modifier.widthIn(max = Dimens.dp720).fillMaxWidth()
            .height(Dimens.dp64)
            .padding(horizontal = Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Black)
            Text(subtitle, color = Muted, fontSize = Dimens.sp11)
        }
        Icon(icon, null)
    }
}

@Composable
internal fun ProfileDrawerControlRow(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(Dimens.dp16),
            )
            .clickable { onEnabledChange(!enabled) }
            .padding(Dimens.dp14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(Dimens.dp32))
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = Dimens.sp11)
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
internal fun ProfileDrawerAppRow(
    icon: @Composable () -> Unit,
    label: String,
    supportingText: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(Dimens.dp16),
            )
            .clickable(onClick = onClick)
            .padding(Dimens.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(supportingText, color = Muted, fontSize = Dimens.sp10)
        }
    }
}

@Composable
internal fun ProfileDrawerMessage(message: String) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(Dimens.dp16),
            )
            .padding(Dimens.dp18),
        color = Muted,
    )
}
