package com.katoaapps.openminilaunch.ui.launcher

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.readableContentColor

@Composable
internal fun BoxScope.HomeFocusPanel(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    qwertyHome: Boolean,
    availableHeight: Dp,
    focusPanelHeight: Dp,
    todoJumpToken: Int,
    openTodos: () -> Unit,
    onTodoCenterChanged: (Offset) -> Unit,
    onPausedApp: (String) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val context = LocalContext.current
    val panelColor = Color(store.effectiveHomePanelColorArgb)
    val contentColor = readableContentColor(panelColor)
    val mutedColor = contentColor.copy(alpha = .68f)
    val insetColor = if (contentColor == com.katoaapps.openminilaunch.ui.theme.MinkWhite) {
        com.katoaapps.openminilaunch.ui.theme.MinkBlack.copy(alpha = .16f)
    } else {
        com.katoaapps.openminilaunch.ui.theme.MinkWhite.copy(alpha = .24f)
    }
    val panelHeight = if (qwertyHome) availableHeight else minOf(availableHeight, focusPanelHeight)
    val itemsPerPage = visibleTodoItemsForHeight(panelHeight.value)
    val focusModifier = if (qwertyHome) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxWidth().height(focusPanelHeight).align(
            if (store.alignHomePanelBottom) Alignment.BottomCenter else Alignment.TopCenter,
        )
    }

    Surface(
        modifier = focusModifier.widthIn(max = Dimens.dp620),
        shape = RoundedCornerShape(if (qwertyHome) Dimens.dp26 else Dimens.dp34),
        color = panelColor,
        contentColor = contentColor,
        shadowElevation = if (isSystemInDarkTheme()) Dimens.dp2 else Dimens.dp8,
        tonalElevation = Dimens.dp1,
    ) {
        Row(
            Modifier.fillMaxSize().padding(if (qwertyHome) Dimens.dp10 else Dimens.dp14),
            horizontalArrangement = Arrangement.spacedBy(if (qwertyHome) Dimens.dp8 else Dimens.dp12),
        ) {
            TodoPager(
                store = store,
                openTodos = openTodos,
                jumpToken = todoJumpToken,
                itemsPerPage = itemsPerPage,
                compact = qwertyHome,
                embedded = true,
                contentColor = contentColor,
                mutedContentColor = mutedColor,
                insetColor = insetColor,
                modifier = Modifier.weight(2f).fillMaxHeight().onGloballyPositioned { coordinates ->
                    val origin = coordinates.positionInRoot()
                    onTodoCenterChanged(
                        origin + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f),
                    )
                },
            )
            ShortcutGrid(
                store = store,
                actions = actions,
                appAccessState = appAccessState,
                onPausedApp = onPausedApp,
                onUnavailableApp = { label ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.launcher_app_unavailable, label),
                        Toast.LENGTH_LONG,
                    ).show()
                },
                openTodos = openTodos,
                compact = qwertyHome,
                contentColor = contentColor,
                itemContainerColor = contentColor.copy(alpha = .09f),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                openDrawer = onOpenDrawer,
            )
        }
    }
}
