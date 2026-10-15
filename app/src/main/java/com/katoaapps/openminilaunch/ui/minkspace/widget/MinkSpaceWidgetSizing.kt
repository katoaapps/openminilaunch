package com.katoaapps.openminilaunch.ui.minkspace.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.widgets.WidgetProviderBadges
import kotlin.math.roundToInt

internal data class MinkSpaceWidgetSizeDp(
    val width: Float,
    val height: Float,
)

internal data class MinkSpaceWidgetViewport(
    val collapsed: MinkSpaceWidgetSizeDp,
    val expanded: MinkSpaceWidgetSizeDp,
)

internal enum class MinkSpaceWidgetFit(@param:StringRes val labelRes: Int) {
    COLLAPSED(R.string.widget_fits_mink_space),
    EXPANDED(R.string.widget_fits_expanded),
    CRAMPED(R.string.widget_may_be_cramped),
    UNKNOWN(R.string.widget_fit_unknown),
}

@Composable
internal fun rememberMinkSpaceWidgetViewport(
    twoPanelModeForLargeDisplays: Boolean,
): MinkSpaceWidgetViewport {
    val configuration = LocalConfiguration.current
    return remember(configuration, twoPanelModeForLargeDisplays) {
        minkSpaceWidgetViewport(
            screenWidthDp = configuration.screenWidthDp.toFloat(),
            screenHeightDp = configuration.screenHeightDp.toFloat(),
            smallestWidthDp = configuration.smallestScreenWidthDp,
            orientation = configuration.orientation,
            twoPanelModeForLargeDisplays = twoPanelModeForLargeDisplays,
        )
    }
}

internal fun minkSpaceWidgetViewport(
    screenWidthDp: Float,
    screenHeightDp: Float,
    smallestWidthDp: Int,
    orientation: Int,
    twoPanelModeForLargeDisplays: Boolean,
): MinkSpaceWidgetViewport {
    val twoPanel = twoPanelModeForLargeDisplays &&
        smallestWidthDp >= 600 && orientation == Configuration.ORIENTATION_LANDSCAPE
    val homePaneWidth = if (twoPanel) {
        ((screenWidthDp - 12f) / 2f).coerceAtLeast(1f)
    } else {
        screenWidthDp
    }
    val compact = screenHeightDp <= homePaneWidth * 1.55f
    val homeHorizontalPadding = if (compact) 14f else 22f
    val panelWidth = (homePaneWidth - homeHorizontalPadding * 2f).coerceIn(1f, 620f)
    val panelHeight = when {
        twoPanel -> (screenHeightDp * .7f).coerceIn(350f, 420f)
        compact -> (screenHeightDp - 150f).coerceAtLeast(180f)
        else -> (screenWidthDp * .78f).coerceIn(310f, 350f)
    }.coerceAtMost(screenHeightDp)
    val rowPadding = if (compact) 10f else 14f
    val rowSpacing = if (compact) 8f else 12f
    val rowContentWidth = (panelWidth - rowPadding * 2f - rowSpacing).coerceAtLeast(1f)
    val collapsedWidth = (rowContentWidth * 2f / 3f).coerceAtLeast(96f)
    val collapsedHeight = (panelHeight - rowPadding * 2f).coerceAtLeast(96f)

    return MinkSpaceWidgetViewport(
        collapsed = MinkSpaceWidgetSizeDp(collapsedWidth, collapsedHeight),
        expanded = MinkSpaceWidgetSizeDp(panelWidth, panelHeight),
    )
}

internal fun minkSpaceWidgetFit(
    info: AppWidgetProviderInfo,
    density: Float,
    viewport: MinkSpaceWidgetViewport,
): MinkSpaceWidgetFit {
    if (density <= 0f) return MinkSpaceWidgetFit.UNKNOWN
    val horizontallyResizable =
        info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0
    val verticallyResizable =
        info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0
    val minWidthPx = if (horizontallyResizable && info.minResizeWidth > 0) {
        info.minResizeWidth
    } else {
        info.minWidth
    }
    val minHeightPx = if (verticallyResizable && info.minResizeHeight > 0) {
        info.minResizeHeight
    } else {
        info.minHeight
    }
    if (minWidthPx <= 0 || minHeightPx <= 0) return MinkSpaceWidgetFit.UNKNOWN

    val minWidthDp = minWidthPx / density
    val minHeightDp = minHeightPx / density
    return minkSpaceWidgetFit(minWidthDp, minHeightDp, viewport)
}

internal fun minkSpaceWidgetFit(
    minWidthDp: Float,
    minHeightDp: Float,
    viewport: MinkSpaceWidgetViewport,
): MinkSpaceWidgetFit {
    if (minWidthDp <= 0f || minHeightDp <= 0f) return MinkSpaceWidgetFit.UNKNOWN
    return when {
        minWidthDp <= viewport.collapsed.width && minHeightDp <= viewport.collapsed.height ->
            MinkSpaceWidgetFit.COLLAPSED
        minWidthDp <= viewport.expanded.width && minHeightDp <= viewport.expanded.height ->
            MinkSpaceWidgetFit.EXPANDED
        else -> MinkSpaceWidgetFit.CRAMPED
    }
}

internal fun minkSpaceWidgetBadges(
    info: AppWidgetProviderInfo,
    density: Float,
    viewport: MinkSpaceWidgetViewport,
): WidgetProviderBadges = WidgetProviderBadges(
    fitLabelRes = minkSpaceWidgetFit(info, density, viewport).labelRes,
    needsSetup = info.configure != null,
)

internal fun minkSpaceWidgetBindingOptions(viewport: MinkSpaceWidgetViewport): Bundle {
    val collapsed = viewport.collapsed
    val expanded = viewport.expanded
    return Bundle().apply {
        putInt(
            AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, collapsed.width.roundToInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, collapsed.height.roundToInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, expanded.width.roundToInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, expanded.height.roundToInt())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                arrayListOf(
                    SizeF(collapsed.width, collapsed.height),
                    SizeF(expanded.width, expanded.height),
                ).distinctBy { size -> size.width to size.height }.let(::ArrayList),
            )
        }
    }
}
