package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

/** Shared settings control for opening a color picker. Folder editing keeps its compact icon. */
@Composable
internal fun SettingsColorPickerButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Surface(
            modifier = Modifier.size(Dimens.dp18),
            shape = CircleShape,
            color = color,
        ) {}
        Column(Modifier.padding(horizontal = Dimens.dp6).weight(1f)) {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supportingText?.let { description ->
                Text(
                    description,
                    color = Muted,
                    fontSize = Dimens.sp11,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.Default.Edit,
            stringResource(R.string.edit),
            Modifier.size(Dimens.dp16),
        )
    }
}
