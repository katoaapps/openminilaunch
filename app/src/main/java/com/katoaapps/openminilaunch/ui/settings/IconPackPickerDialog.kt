package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.iconpacks.InstalledIconPack
import com.katoaapps.openminilaunch.model.IconSource
import com.katoaapps.openminilaunch.ui.components.DrawableIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun IconPackPickerDialog(
    selectedSource: IconSource,
    selectedPackage: String?,
    packs: List<InstalledIconPack>?,
    selectionEnabled: Boolean,
    showApplyingFeedback: Boolean,
    applyingStyleLabel: String,
    onMinkIcons: () -> Unit,
    onSystemIcons: () -> Unit,
    onIconPack: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.9f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            AnimatedContent(
                targetState = showApplyingFeedback,
                label = "icon style applying feedback",
            ) { applying ->
                if (applying) {
                    IconStyleApplyingScreen(styleLabel = applyingStyleLabel)
                } else {
                    IconStyleChoices(
                        selectedSource = selectedSource,
                        selectedPackage = selectedPackage,
                        packs = packs,
                        selectionEnabled = selectionEnabled,
                        onMinkIcons = onMinkIcons,
                        onSystemIcons = onSystemIcons,
                        onIconPack = onIconPack,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun IconStyleChoices(
    selectedSource: IconSource,
    selectedPackage: String?,
    packs: List<InstalledIconPack>?,
    selectionEnabled: Boolean,
    onMinkIcons: () -> Unit,
    onSystemIcons: () -> Unit,
    onIconPack: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.padding(Dimens.dp14)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.choose_icon_style),
                Modifier.weight(1f),
                fontWeight = FontWeight.Black,
                fontSize = Dimens.sp18,
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, stringResource(R.string.close))
            }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            item {
                BuiltInIconChoice(
                    title = stringResource(R.string.mink_icons),
                    description = stringResource(R.string.mink_icons_description),
                    icon = Icons.Default.AutoAwesome,
                    selected = selectedSource == IconSource.MINK,
                    enabled = selectionEnabled,
                    onClick = onMinkIcons,
                )
            }
            item {
                BuiltInIconChoice(
                    title = stringResource(R.string.system_icons),
                    description = stringResource(R.string.system_icons_description),
                    icon = Icons.Default.Android,
                    selected = selectedSource == IconSource.SYSTEM,
                    enabled = selectionEnabled,
                    onClick = onSystemIcons,
                )
            }
            item { SectionLabel(stringResource(R.string.installed_icon_packs)) }
            item {
                Text(
                    stringResource(R.string.compatible_icon_pack_description),
                    color = Muted,
                    fontSize = Dimens.sp12,
                    modifier = Modifier.padding(horizontal = Dimens.dp6),
                )
            }
            when {
                packs == null -> item {
                    CircularProgressIndicator(Modifier.padding(Dimens.dp24))
                }
                packs.isEmpty() -> item {
                    Text(
                        stringResource(R.string.no_icon_packs_installed),
                        color = Muted,
                        modifier = Modifier.padding(Dimens.dp14),
                    )
                }
                else -> items(packs, key = InstalledIconPack::packageName) { pack ->
                    InstalledIconPackChoice(
                        pack = pack,
                        selected = selectedSource == IconSource.ICON_PACK &&
                            selectedPackage == pack.packageName,
                        enabled = selectionEnabled,
                        onClick = { onIconPack(pack.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BuiltInIconChoice(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconChoiceSurface(selected = selected, enabled = enabled, onClick = onClick) {
        Icon(icon, null, Modifier.size(Dimens.dp38), tint = MaterialTheme.colorScheme.primary)
        IconChoiceText(title, description, Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
    }
}

@Composable
private fun InstalledIconPackChoice(
    pack: InstalledIconPack,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val choiceEnabled = enabled && pack.hasReadableMappings
    IconChoiceSurface(selected, choiceEnabled, onClick) {
        DrawableIcon(
            drawable = pack.packIcon,
            iconKey = "pack:${pack.packageName}",
            size = Dimens.dp38,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.dp6)) {
            IconChoiceText(
                title = pack.label,
                description = if (pack.hasReadableMappings) pack.packageName
                    else stringResource(R.string.icon_pack_could_not_be_read),
            )
            if (pack.previewIcons.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                    pack.previewIcons.forEachIndexed { index, drawable ->
                        DrawableIcon(
                            drawable = drawable,
                            iconKey = "preview:${pack.packageName}:$index",
                            size = Dimens.dp26,
                        )
                    }
                }
            }
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = choiceEnabled,
        )
    }
}

@Composable
private fun IconChoiceSurface(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(durationMillis = 220),
        label = "icon choice color",
    )
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1.025f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "icon choice scale",
    )
    Row(
        Modifier.fillMaxWidth()
            .graphicsLayer {
                scaleX = selectedScale
                scaleY = selectedScale
            }
            .background(
                backgroundColor,
                RoundedCornerShape(Dimens.dp16),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(Dimens.dp12),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun IconChoiceText(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(description, color = Muted, fontSize = Dimens.sp11)
    }
}
