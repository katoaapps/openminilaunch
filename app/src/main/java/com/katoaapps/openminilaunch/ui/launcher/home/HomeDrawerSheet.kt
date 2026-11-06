@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katoaapps.openminilaunch.ui.launcher.home

import com.katoaapps.minkpad.core.R as MinkPadR

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkpad.MinkPadLauncherEntry
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.quickdrawer.QuickDrawerDestination
import com.katoaapps.openminilaunch.ui.quickdrawer.quickDrawerDestinations
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.readableContentColor

@Composable
internal fun HomeDrawerSheet(
    store: LauncherStore,
    actions: DeviceActions,
    onDismiss: () -> Unit,
    onManageQuickApps: () -> Unit,
) {
    val context = LocalContext.current
    val drawerColor = store.quickDrawerColorArgb?.let(::Color) ?: MaterialTheme.colorScheme.background
    val drawerContentColor = readableContentColor(drawerColor)
    val destinations = quickDrawerDestinations(
        showMinkPad = store.showMinkPadInQuickDrawer,
        showPrivateSpace = store.showPrivateSpaceInQuickDrawer,
        privateSpaceAvailable = store.demoSearchDataEnabled ||
            actions.privateContainerEntryPointAvailable(store.privateContainerGatewaySelectionKey),
        showWorkSpace = store.showWorkProfileInQuickDrawer,
        workSpaceAvailable = store.demoSearchDataEnabled || actions.workProfileAvailable(),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = drawerColor,
        contentColor = drawerContentColor,
    ) {
        Text(
            stringResource(R.string.your_drawer),
            Modifier.padding(horizontal = Dimens.dp24),
            fontWeight = FontWeight.Black,
            letterSpacing = Dimens.sp1,
        )
        ManageQuickAppsRow(
            selectedCount = store.drawerTargets.size,
            contentColor = drawerContentColor,
            onClick = {
                onDismiss()
                onManageQuickApps()
            },
        )
        QuickDrawerNavigationGrid(
            destinations = destinations,
            contentColor = drawerContentColor,
            onDestination = { destination ->
                onDismiss()
                when (destination) {
                    QuickDrawerDestination.MINKPAD_LITE -> MinkPadLauncherEntry.open(context)
                    QuickDrawerDestination.ALL_APPS -> actions.openAllApps()
                    QuickDrawerDestination.PRIVATE_SPACE -> actions.openPrivateApps()
                    QuickDrawerDestination.WORK_SPACE -> actions.openWorkApps()
                }
            },
        )
        Spacer(Modifier.height(Dimens.dp28))
    }
}

@Composable
private fun ManageQuickAppsRow(
    selectedCount: Int,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.dp18, vertical = Dimens.dp12),
        shape = RoundedCornerShape(Dimens.dp18),
        color = contentColor.copy(alpha = .09f),
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.dp16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
        ) {
            Icon(Icons.Default.GridView, null)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.quick_apps),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.quick_drawer_apps_description),
                    color = contentColor.copy(alpha = .68f),
                    fontSize = Dimens.sp12,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                stringResource(R.string.count_of_max, selectedCount, MAX_DRAWER_APPS),
                color = contentColor.copy(alpha = .68f),
                fontWeight = FontWeight.Bold,
            )
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
    }
}

@Composable
private fun QuickDrawerNavigationGrid(
    destinations: List<QuickDrawerDestination>,
    contentColor: Color,
    onDestination: (QuickDrawerDestination) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        destinations.chunked(2).forEach { rowDestinations ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
            ) {
                rowDestinations.forEach { destination ->
                    QuickDrawerNavigationButton(
                        destination = destination,
                        contentColor = contentColor,
                        onClick = { onDestination(destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowDestinations.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickDrawerNavigationButton(
    destination: QuickDrawerDestination,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(Dimens.dp84),
        shape = RoundedCornerShape(Dimens.dp18),
        color = contentColor.copy(alpha = .09f),
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.dp12),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (destination) {
                QuickDrawerDestination.MINKPAD_LITE -> MinkPadDrawerIcon()
                QuickDrawerDestination.ALL_APPS -> Icon(Icons.Default.Apps, null)
                QuickDrawerDestination.PRIVATE_SPACE -> Icon(Icons.Default.Shield, null)
                QuickDrawerDestination.WORK_SPACE -> Icon(Icons.Default.BusinessCenter, null)
            }
            Spacer(Modifier.height(Dimens.dp6))
            Text(
                text = when (destination) {
                    QuickDrawerDestination.MINKPAD_LITE -> stringResource(R.string.minkpad_lite)
                    QuickDrawerDestination.ALL_APPS -> stringResource(R.string.all_apps)
                    QuickDrawerDestination.PRIVATE_SPACE -> stringResource(R.string.private_space)
                    QuickDrawerDestination.WORK_SPACE -> stringResource(R.string.work_space)
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MinkPadDrawerIcon() {
    Box(Modifier.size(Dimens.dp28)) {
        Icon(
            painterResource(R.drawable.ic_minkpad_notification),
            stringResource(MinkPadR.string.minkpad_name),
            modifier = Modifier.size(Dimens.dp24).align(Alignment.TopStart),
        )
        Box(
            Modifier
                .size(Dimens.dp14)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = 1.55f, scaleY = 1.55f),
            )
        }
    }
}
