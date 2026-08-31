package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun folderCountLabel(count: Int): String =
    pluralStringResource(R.plurals.folder_count, count, count)

@Composable
internal fun SettingsPage(
    title: String,
    goBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageHeader(title, goBack)
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(LocalSettingsScrollState.current)
                .padding(horizontal = Dimens.dp22),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp14),
        ) {
            content()
            Spacer(Modifier.height(Dimens.dp24))
        }
    }
}

@Composable
internal fun SettingsCategoryRow(
    title: String,
    subtitle: String,
    status: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp18))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.dp14, vertical = Dimens.dp13),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(Dimens.dp40), contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    null,
                    Modifier.size(Dimens.dp21),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp12)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                color = Muted,
                fontSize = Dimens.sp12,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(status, color = Muted, fontSize = Dimens.sp10, maxLines = 1)
            Icon(Icons.Default.ChevronRight, null, tint = Muted, modifier = Modifier.size(Dimens.dp20))
        }
    }
}
