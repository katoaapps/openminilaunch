package com.katoaapps.openminilaunch.ui.appgrid

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.AppFolderColorMode
import com.katoaapps.openminilaunch.model.AppFolderConfig
import com.katoaapps.openminilaunch.model.AppFolderAnimationStyle
import com.katoaapps.openminilaunch.model.MAX_APP_FOLDER_NAME_LENGTH
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.UnavailableLauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.settings.AppearanceColorDialog
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.readableContentColor
import com.katoaapps.openminilaunch.ui.wellbeing.MinkPausedBadge

/** Functional folder surface used until the final Home folder design is chosen. */
@Composable
internal fun AppFolderContentsDialog(
    folder: AppFolderConfig,
    animationStyle: AppFolderAnimationStyle,
    actions: DeviceActions,
    isPaused: (LauncherTarget) -> Boolean,
    onTarget: (LauncherTarget) -> Unit,
    onAppearanceChange: (customName: String?, colorMode: AppFolderColorMode, colorArgb: Int?) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val targets = folder.orderedTargetKeys.map(actions::resolveLauncherSelection)
    var draftName by remember(folder.id) { mutableStateOf(folder.customName.orEmpty()) }
    var draftColorMode by remember(folder.id) { mutableStateOf(folder.colorMode) }
    var draftColorArgb by remember(folder.id) { mutableStateOf(folder.colorArgb) }
    var showColorPicker by remember(folder.id) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val folderColor = folder.copy(
        colorMode = draftColorMode,
        colorArgb = draftColorArgb,
    ).backgroundColor(
        defaultColor = MaterialTheme.colorScheme.background,
        themeColor = MaterialTheme.colorScheme.primaryContainer,
    )
    val folderContentColor = readableContentColor(folderColor)

    fun commitAppearance() {
        onAppearanceChange(draftName, draftColorMode, draftColorArgb)
    }

    fun commitAndDismiss() {
        commitAppearance()
        onDismiss()
    }

    Dialog(onDismissRequest = ::commitAndDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.72f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = folderColor,
            contentColor = folderContentColor,
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(Dimens.dp14)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = draftName,
                            onValueChange = { draftName = it.take(MAX_APP_FOLDER_NAME_LENGTH) },
                            modifier = Modifier.weight(1f)
                                .padding(horizontal = Dimens.dp4, vertical = Dimens.dp8),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = folderContentColor,
                                fontWeight = FontWeight.Black,
                            ),
                            cursorBrush = SolidColor(folderContentColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                commitAppearance()
                            }),
                            decorationBox = { field ->
                                Box {
                                    if (draftName.isBlank()) {
                                        Text(
                                            stringResource(R.string.apps),
                                            color = folderContentColor.copy(alpha = .62f),
                                            fontWeight = FontWeight.Black,
                                            fontSize = Dimens.sp18,
                                        )
                                    }
                                    field()
                                }
                            },
                        )
                        IconButton(onClick = { showColorPicker = true }) {
                            Icon(Icons.Default.Palette, stringResource(R.string.folder_color))
                        }
                        TextButton(onClick = {
                            commitAppearance()
                            onEdit()
                        }) { Text(stringResource(R.string.edit)) }
                        IconButton(onClick = ::commitAndDismiss) {
                            Icon(Icons.Default.Close, stringResource(R.string.close))
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
                        itemsIndexed(targets, key = { _, target -> target.selectionKey }) { index, target ->
                            AnimatedFolderEntry(style = animationStyle, index = index) {
                                Column(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            commitAppearance()
                                            onTarget(target)
                                        }
                                        .padding(horizontal = Dimens.dp4, vertical = Dimens.dp8),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(Modifier.size(Dimens.dp48), contentAlignment = Alignment.Center) {
                                        if (target.isAvailable) {
                                            LauncherTargetIcon(target, actions, Dimens.dp48)
                                        } else {
                                            UnavailableLauncherTargetIcon(
                                                size = Dimens.dp48,
                                                contentDescription = stringResource(R.string.app_unavailable),
                                            )
                                        }
                                        if (target.isAvailable && isPaused(target)) {
                                            MinkPausedBadge(Modifier.align(Alignment.Center))
                                        }
                                    }
                                    Text(
                                        if (target.isAvailable) {
                                            target.label
                                        } else {
                                            stringResource(R.string.app_unavailable)
                                        },
                                        modifier = Modifier.padding(top = Dimens.dp6),
                                        textAlign = TextAlign.Center,
                                        fontSize = Dimens.sp11,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        AppearanceColorDialog(
            initialArgb = draftColorArgb ?: MaterialTheme.colorScheme.primaryContainer.toArgb(),
            titleRes = R.string.folder_color,
            descriptionRes = R.string.custom_panel_color_description,
            colorHotphrases = emptyMap(),
            onConfirm = { _, argb ->
                draftColorMode = AppFolderColorMode.CUSTOM
                draftColorArgb = argb
                onAppearanceChange(draftName, draftColorMode, argb)
                showColorPicker = false
            },
            onUseThemeColor = {
                draftColorMode = AppFolderColorMode.THEME
                draftColorArgb = null
                onAppearanceChange(draftName, draftColorMode, null)
                showColorPicker = false
            },
            onUseDefaultAppearance = {
                draftColorMode = AppFolderColorMode.DEFAULT
                draftColorArgb = null
                onAppearanceChange(draftName, draftColorMode, null)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }
}
