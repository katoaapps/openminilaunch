package com.katoaapps.openminilaunch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
internal fun PageHeader(
    title: String,
    goBack: () -> Unit,
    showBackButton: Boolean = true,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.dp10, vertical = Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackButton) {
            IconButton(onClick = goBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
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

@Composable
internal fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.dp16))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onCheckedChange(!checked) }
            .padding(Dimens.dp14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = Dimens.dp12)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = Dimens.sp12)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

internal data class SettingsDropdownOption<T>(
    val value: T,
    val label: String,
)

/** Settings row whose current value and alternatives remain visible without opening another page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SettingsDropdownRow(
    title: String,
    selectedValue: T,
    options: List<SettingsDropdownOption<T>>,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    valuePreview: (@Composable () -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        Row(
            Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.dp16))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(Dimens.dp14),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = Dimens.dp12)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        selectedLabel,
                        color = Muted,
                        fontSize = Dimens.sp12,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    valuePreview?.invoke()
                }
            }
            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onValueSelected(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}
