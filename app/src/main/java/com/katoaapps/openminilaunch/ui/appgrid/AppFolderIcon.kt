package com.katoaapps.openminilaunch.ui.appgrid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.model.AppFolderIndicatorStyle
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.dominantAppColor
import com.katoaapps.openminilaunch.ui.theme.readableContentColor

/** Renders a folder using the App Grid's selected indicator style. */
@Composable
internal fun AppFolderIcon(
    targetKeys: List<String>,
    actions: DeviceActions,
    size: Dp,
    backgroundColor: Color,
    indicatorStyle: AppFolderIndicatorStyle,
    modifier: Modifier = Modifier,
) {
    val appsRevision by actions.launcherAppsRevision.collectAsState()
    val shortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val targets = remember(targetKeys, appsRevision, shortcutsRevision) {
        targetKeys.map(actions::resolveLauncherSelection)
            .filter(LauncherTarget::isAvailable)
            .take(4)
    }

    if (targets.isEmpty()) {
        EmptyFolderIcon(
            size = size,
            backgroundColor = backgroundColor,
            modifier = modifier,
        )
        return
    }

    when (indicatorStyle) {
        AppFolderIndicatorStyle.GRID -> GridFolderIndicator(
            targets = targets,
            actions = actions,
            size = size,
            backgroundColor = backgroundColor,
            modifier = modifier,
        )
        AppFolderIndicatorStyle.STACKED -> StackedFolderIndicator(
            targets = targets.take(3),
            actions = actions,
            size = size,
            backgroundColor = backgroundColor,
            appsRevision = appsRevision,
            shortcutsRevision = shortcutsRevision,
            modifier = modifier,
        )
    }
}

@Composable
private fun GridFolderIndicator(
    targets: List<LauncherTarget>,
    actions: DeviceActions,
    size: Dp,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * .22f))
            .background(backgroundColor)
            .padding(size * .10f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(size * .04f),
        ) {
            targets.chunked(2).forEach { rowTargets ->
                Row(
                    Modifier.weight(1f).fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(size * .04f),
                ) {
                    rowTargets.forEach { target ->
                        Box(
                            Modifier.weight(1f).fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LauncherTargetIcon(
                                target = target,
                                actions = actions,
                                size = size * .30f,
                                contentDescription = target.label,
                            )
                        }
                    }
                    if (rowTargets.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StackedFolderIndicator(
    targets: List<LauncherTarget>,
    actions: DeviceActions,
    size: Dp,
    backgroundColor: Color,
    appsRevision: Long,
    shortcutsRevision: Long,
    modifier: Modifier = Modifier,
) {
    val layerColors = remember(targets, appsRevision, shortcutsRevision, backgroundColor) {
        targets.map { target ->
            dominantAppColor(actions.launcherTargetIcon(target)) ?: backgroundColor
        }
    }
    val rearColors = layerColors.drop(1)
    val stackSize = size * .82f
    val layerOffset = when (rearColors.size) {
        0 -> stackSize * 0f
        1 -> stackSize * .12f
        else -> stackSize * .11f
    }
    val cardSize = stackSize - (layerOffset * rearColors.size)
    val cardShape = RoundedCornerShape(cardSize * .22f)

    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * .22f))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(stackSize),
        ) {
            rearColors.asReversed().forEachIndexed { reversedIndex, color ->
                val originalIndex = rearColors.lastIndex - reversedIndex
                val distance = originalIndex + 1
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset(
                            x = layerOffset * distance,
                            y = -(layerOffset * distance),
                        )
                        .size(cardSize)
                        .clip(cardShape)
                        .background(color),
                )
            }

            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .size(cardSize)
                    .clip(cardShape)
                    .background(layerColors.first()),
                contentAlignment = Alignment.Center,
            ) {
                val leadingTarget = targets.first()
                LauncherTargetIcon(
                    target = leadingTarget,
                    actions = actions,
                    size = cardSize,
                    contentDescription = leadingTarget.label,
                )
            }
        }
    }
}

@Composable
private fun EmptyFolderIcon(
    size: Dp,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * .22f))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(size * .62f),
            tint = readableContentColor(backgroundColor),
        )
    }
}
