package com.katoaapps.openminilaunch.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.iconpacks.IconPackIconChoice
import com.katoaapps.openminilaunch.features.iconpacks.IconPackRepository
import com.katoaapps.openminilaunch.features.iconpacks.InstalledIconPack
import com.katoaapps.openminilaunch.model.LauncherIconOverride
import com.katoaapps.openminilaunch.ui.components.DrawableIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AppIconOverrideDialog(
    appLabel: String,
    current: LauncherIconOverride?,
    onSelect: (LauncherIconOverride?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { IconPackRepository.get(context) }
    val packs by produceState<List<InstalledIconPack>?>(null, repository) {
        value = withContext(Dispatchers.IO) { repository.installedPacks() }
    }
    val readablePacks = packs?.filter(InstalledIconPack::hasReadableMappings)
    var selectedPack by remember { mutableStateOf<InstalledIconPack?>(null) }
    val icons by produceState<List<IconPackIconChoice>?>(null, selectedPack) {
        value = selectedPack?.let { pack ->
            withContext(Dispatchers.IO) { repository.iconsInPack(pack.packageName) }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.9f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().padding(Dimens.dp14)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedPack != null) {
                        IconButton(onClick = { selectedPack = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.choose_app_icon),
                            fontWeight = FontWeight.Black,
                            fontSize = Dimens.sp18,
                        )
                        Text(
                            selectedPack?.label ?: appLabel,
                            color = Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = Dimens.sp12,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }

                if (selectedPack == null) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) {
                        item {
                            IconPackRow(
                                title = stringResource(R.string.use_default_app_icon),
                                subtitle = stringResource(R.string.use_default_app_icon_description),
                                icon = null,
                                selected = current == null,
                                onClick = { onSelect(null) },
                            )
                        }
                        when (val installed = readablePacks) {
                            null -> item {
                                Box(
                                    Modifier.fillMaxWidth().padding(Dimens.dp24),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator() }
                            }
                            else -> if (installed.isEmpty()) {
                                item {
                                    Text(
                                        stringResource(R.string.no_icon_packs_installed),
                                        color = Muted,
                                        modifier = Modifier.padding(Dimens.dp14),
                                    )
                                }
                            } else {
                                items(installed, key = InstalledIconPack::packageName) { pack ->
                                    IconPackRow(
                                        title = pack.label,
                                        subtitle = stringResource(R.string.choose_one_icon_from_pack),
                                        icon = pack,
                                        selected = current?.iconPackPackage == pack.packageName,
                                        onClick = { selectedPack = pack },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when (val choices = icons) {
                        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        else -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(Dimens.dp64),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(choices, key = IconPackIconChoice::drawableName) { choice ->
                                val packPackage = selectedPack!!.packageName
                                val drawable = remember(packPackage, choice.drawableName) {
                                    repository.iconForOverride(packPackage, choice.drawableName)
                                }
                                val selected = current?.iconPackPackage == selectedPack?.packageName &&
                                    current?.drawableName == choice.drawableName
                                Surface(
                                    modifier = Modifier.size(Dimens.dp64).clickable {
                                        onSelect(
                                            LauncherIconOverride(
                                                iconPackPackage = packPackage,
                                                drawableName = choice.drawableName,
                                            ),
                                        )
                                    },
                                    shape = RoundedCornerShape(Dimens.dp14),
                                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        DrawableIcon(
                                            drawable = drawable,
                                            iconKey = "icon-choice:$packPackage:${choice.drawableName}",
                                            size = Dimens.dp42,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPackRow(
    title: String,
    subtitle: String,
    icon: InstalledIconPack?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Dimens.dp12),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon == null) {
            Icon(Icons.Default.Android, null, Modifier.size(Dimens.dp38))
        } else {
            DrawableIcon(
                drawable = icon.packIcon,
                iconKey = "override-pack:${icon.packageName}",
                size = Dimens.dp38,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = Dimens.sp12)
        }
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        }
    }
}
