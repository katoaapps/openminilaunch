package com.katoaapps.openminilaunch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun PageHeader(title: String, goBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.dp10, vertical = Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = goBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
        }
        Text(
            title,
            Modifier.weight(1f),
            fontSize = Dimens.sp28,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        action?.invoke()
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = Dimens.dp10),
        color = Rust,
        fontSize = Dimens.sp12,
        fontWeight = FontWeight.Black,
        letterSpacing = Dimens.sp1_4,
    )
}

@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.dp16))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(Dimens.dp14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = Dimens.sp12, maxLines = 1)
        }
        Icon(icon, null, tint = Muted)
    }
}
