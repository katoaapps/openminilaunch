package com.katoaapps.openminilaunch.ui.appgrid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.readableContentColor

/** Minimal shared folder mark. Final folder visuals intentionally remain a later design decision. */
@Composable
internal fun AppFolderIcon(
    targetKeys: List<String>,
    actions: DeviceActions,
    size: Dp,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val targets = targetKeys.map(actions::resolveLauncherSelection)
        .filter(LauncherTarget::isAvailable)
        .take(4)
    val foregroundColor = readableContentColor(backgroundColor)
    val shape = RoundedCornerShape(size * .22f)
    Box(
        modifier.size(size)
            .clip(shape)
            .background(backgroundColor)
            .padding(size * .10f),
        contentAlignment = Alignment.Center,
    ) {
        if (targets.isEmpty()) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(size * .7f),
                tint = foregroundColor,
            )
            return@Box
        }
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp2),
        ) {
            targets.chunked(2).forEach { rowTargets ->
                Row(
                    Modifier.weight(1f).fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.dp2),
                ) {
                    rowTargets.forEach { target ->
                        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
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
