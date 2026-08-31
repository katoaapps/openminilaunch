package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.AlphabetRail
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Sage

@Composable
internal fun LauncherShortcutGroupList(
    shortcuts: List<LauncherShortcutTarget>,
    apps: List<LauncherAppTarget>,
    selected: Set<String>,
    actions: DeviceActions,
    multiSelect: Boolean,
    selectionLimit: Int,
    onSelectionLimit: () -> Unit,
    onShortcut: (LauncherTarget) -> Unit,
) {
    val groups = remember(shortcuts, apps) {
        shortcuts.groupBy { ShortcutPublisherKey(it.userSerial, it.packageName) }
            .map { (publisher, publishedShortcuts) ->
                val sortedShortcuts = publishedShortcuts.sortedBy { it.label.lowercase() }
                val appTarget = apps.firstOrNull {
                    it.userSerial == publisher.userSerial && it.packageName == publisher.packageName
                }
                LauncherShortcutGroup(
                    key = publisher,
                    appLabel = appTarget?.label ?: actions.appLabel(publisher.packageName),
                    appTarget = appTarget,
                    shortcuts = sortedShortcuts,
                )
            }
            .sortedWith(
                compareBy<LauncherShortcutGroup, String>(String.CASE_INSENSITIVE_ORDER) {
                    it.appLabel
                }.thenBy { it.key.userSerial },
            )
    }
    val listState = rememberLazyListState()
    var requestedLetter by remember { mutableStateOf('A') }
    val groupStartIndices = remember(groups) {
        buildList {
            var itemIndex = 0
            groups.forEach { group ->
                add(itemIndex)
                itemIndex += 1 + (group.shortcuts.size + 2) / 3
            }
        }
    }
    LaunchedEffect(requestedLetter, groups, groupStartIndices) {
        if (groups.isNotEmpty()) {
            val groupIndex = groups.indexOfFirst {
                (it.appLabel.firstOrNull()?.uppercaseChar() ?: 'Z') >= requestedLetter
            }.let { if (it < 0) groups.lastIndex else it }
            listState.scrollToItem(groupStartIndices[groupIndex])
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(end = Dimens.dp34),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
        ) {
            groups.forEach { group ->
                item(key = "header:${group.key.userSerial}:${group.key.packageName}") {
                    ShortcutPublisherHeader(group, actions)
                }
                items(
                    items = group.shortcuts.chunked(3),
                    key = { row -> row.joinToString("|") { it.selectionKey } },
                ) { rowShortcuts ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                    ) {
                        rowShortcuts.forEach { shortcut ->
                            val isSelected = shortcut.selectionKey in selected
                            ShortcutPickerCard(
                                shortcut = shortcut,
                                selected = isSelected,
                                actions = actions,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (multiSelect && !isSelected && selected.size >= selectionLimit) {
                                        onSelectionLimit()
                                    } else {
                                        onShortcut(shortcut)
                                    }
                                },
                            )
                        }
                        repeat(3 - rowShortcuts.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        AlphabetRail(
            onLetterSelected = { requestedLetter = it },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ShortcutPublisherHeader(group: LauncherShortcutGroup, actions: DeviceActions) {
    val iconTarget: LauncherTarget = group.appTarget ?: group.shortcuts.first()
    Row(
        Modifier.fillMaxWidth().padding(top = Dimens.dp10, bottom = Dimens.dp2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LauncherTargetIcon(iconTarget, actions, Dimens.dp34)
        Column(Modifier.weight(1f).padding(start = Dimens.dp10)) {
            Text(group.appLabel, fontWeight = FontWeight.Bold)
            if (iconTarget.isWorkProfile) {
                Text(stringResource(R.string.work_profile), color = Muted, fontSize = Dimens.sp10)
            }
        }
    }
}

@Composable
private fun ShortcutPickerCard(
    shortcut: LauncherShortcutTarget,
    selected: Boolean,
    actions: DeviceActions,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.padding(Dimens.dp2)
            .clip(RoundedCornerShape(Dimens.dp16))
            .background(if (selected) Sage else MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(Dimens.dp8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LauncherTargetIcon(shortcut, actions, Dimens.dp48)
        Text(
            shortcut.label,
            modifier = Modifier.padding(top = Dimens.dp6),
            textAlign = TextAlign.Center,
            fontSize = Dimens.sp11,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class ShortcutPublisherKey(val userSerial: Long, val packageName: String)

private data class LauncherShortcutGroup(
    val key: ShortcutPublisherKey,
    val appLabel: String,
    val appTarget: LauncherAppTarget?,
    val shortcuts: List<LauncherShortcutTarget>,
)
