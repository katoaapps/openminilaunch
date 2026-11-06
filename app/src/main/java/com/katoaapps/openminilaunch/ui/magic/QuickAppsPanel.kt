package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun QuickAppsPanel(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    onSelect: (LauncherTarget) -> Unit,
) {
    val targets = store.drawerTargets
        .map(actions::resolveLauncherSelection)
        .filterNot(appAccessState::isPaused)

    Surface(
        shape = RoundedCornerShape(Dimens.dp18),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        shadowElevation = Dimens.dp8,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.dp10),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp7),
        ) {
            Text(
                stringResource(R.string.quick_apps),
                color = Muted,
                fontSize = Dimens.sp10,
                fontWeight = FontWeight.Bold,
                letterSpacing = Dimens.sp1_2,
            )
            when {
                !appAccessState.isResolved -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                targets.isEmpty() -> Text(
                    text = if (store.drawerTargets.isEmpty()) {
                        stringResource(R.string.quick_apps_empty)
                    } else {
                        stringResource(R.string.drawer_apps_paused)
                    },
                    modifier = Modifier.fillMaxWidth().padding(Dimens.dp8),
                    color = Muted,
                    textAlign = TextAlign.Center,
                )
                else -> targets.chunked(4).forEach { rowTargets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dp7),
                    ) {
                        rowTargets.forEach { target ->
                            QuickAppButton(
                                target = target,
                                actions = actions,
                                onClick = { onSelect(target) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(4 - rowTargets.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAppButton(
    target: LauncherTarget,
    actions: DeviceActions,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(Dimens.dp72),
        shape = RoundedCornerShape(Dimens.dp12),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.dp4, vertical = Dimens.dp7),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.dp3),
        ) {
            LauncherTargetIcon(target, actions, Dimens.dp28)
            Text(
                text = target.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
