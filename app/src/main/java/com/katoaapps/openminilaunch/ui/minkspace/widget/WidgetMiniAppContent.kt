package com.katoaapps.openminilaunch.ui.minkspace.widget

import android.appwidget.AppWidgetHostView
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceExpansionController
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceTitle
import com.katoaapps.openminilaunch.ui.minkspace.core.minkSpaceExpansionBackgroundTap
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.widgets.reportWidgetSizes

private class WidgetSizeReportCache {
    var hostView: AppWidgetHostView? = null
    var sizes: List<IntSize> = emptyList()
}

/** Renders one bound widget and switches gesture ownership in explicit interaction mode. */
@Composable
internal fun WidgetMiniAppContent(
    appWidgetId: Int,
    infoLabel: String,
    compact: Boolean,
    expansion: MinkSpaceExpansionController,
    contentColor: Color,
    hostViewFactory: () -> AppWidgetHostView,
    onOpenProvider: () -> Unit,
    onRemove: () -> Unit,
) {
    val expanded = expansion.expanded
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val sizeStateKey = listOf(
        appWidgetId,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.orientation,
    )
    var menuExpanded by remember { mutableStateOf(false) }
    var measuredSize by remember(sizeStateKey) { mutableStateOf(IntSize.Zero) }
    var collapsedSize by remember(sizeStateKey) { mutableStateOf(IntSize.Zero) }
    var expandedSize by remember(sizeStateKey) { mutableStateOf(IntSize.Zero) }
    val sizeReportCache = remember(sizeStateKey) { WidgetSizeReportCache() }

    Column(Modifier.fillMaxSize().minkSpaceExpansionBackgroundTap(expansion)) {
        Row(
            modifier = Modifier
                .heightIn(min = if (compact) Dimens.dp38 else Dimens.dp44)
                .padding(
                    start = Dimens.dp12,
                    end = if (expanded) Dimens.dp64 else Dimens.dp4,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MinkSpaceTitle(
                title = stringResource(R.string.quick_widget),
                icon = Icons.Default.Widgets,
                contentColor = contentColor,
            )
            Text(
                text = infoLabel,
                modifier = Modifier.padding(start = Dimens.dp8),
                color = contentColor.copy(alpha = .62f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(Dimens.dp48),
                ) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.widget_options))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove)) },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) Dimens.dp4 else Dimens.dp6,
                    end = if (compact) Dimens.dp4 else Dimens.dp6,
                    bottom = if (compact) Dimens.dp4 else Dimens.dp6,
                )
                .clip(RoundedCornerShape(if (compact) Dimens.dp14 else Dimens.dp18)),
        ) {
            AndroidView(
                factory = { hostViewFactory() },
                update = { view ->
                    view.descendantFocusability = if (expanded) {
                        ViewGroup.FOCUS_AFTER_DESCENDANTS
                    } else {
                        ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    }
                    view.importantForAccessibility = if (expanded) {
                        View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                    } else {
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    }
                    val sizes = listOf(collapsedSize, expandedSize, measuredSize)
                        .filter { it != IntSize.Zero }
                        .distinct()
                    if (sizes.isNotEmpty() &&
                        (sizeReportCache.hostView !== view || sizeReportCache.sizes != sizes)
                    ) {
                        reportWidgetSizes(view, sizes, density)
                        sizeReportCache.hostView = view
                        sizeReportCache.sizes = sizes
                    }
                },
                modifier = Modifier
                    .matchParentSize()
                    .then(if (expanded) Modifier else Modifier.clearAndSetSemantics { })
                    .onSizeChanged { size ->
                        measuredSize = size
                        if (expanded) expandedSize = size else collapsedSize = size
                    },
            )

            if (!expanded) {
                val openProviderDescription = stringResource(R.string.open_app, infoLabel)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            onClickLabel = openProviderDescription,
                            role = Role.Button,
                            onClick = onOpenProvider,
                        ),
                )
            }
        }
    }
}
