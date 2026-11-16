package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.WidgetEntry
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.model.WidgetProviderIdentity

internal data class WidgetWallPalette(
    val content: Int,
    val muted: Int,
    val surface: Int,
    val primary: Int,
    val error: Int,
    val typeface: Typeface,
    val fontSizeMultiplier: Float,
)

internal data class WidgetWallCallbacks(
    val onRetryUnavailableWidget: () -> Unit,
    val onOrderChange: (List<String>) -> Unit,
    val onRemove: (WidgetEntry) -> Unit,
    val onResize: (WidgetEntry.Bound, AppWidgetProviderInfo, WidgetGridSize) -> Unit,
    val onBindReplacement: (String, AppWidgetProviderInfo, WidgetGridSize) -> Unit,
    val onChooseReplacement: (WidgetEntry.RestorePlaceholder) -> Unit,
    val onReinstall: (WidgetProviderIdentity) -> Unit,
)

@Composable
internal fun NativeWidgetWall(
    entries: List<WidgetEntry>,
    host: AppWidgetHost,
    manager: AppWidgetManager,
    availableProviders: List<AppWidgetProviderInfo>,
    installedPackages: Set<String>,
    horizontalDragHandler: WidgetHorizontalDragHandler?,
    callbacks: WidgetWallCallbacks,
    contentColor: ComposeColor,
    typeface: Typeface,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val fontSizeMultiplier = LocalDensity.current.fontScale /
        context.resources.configuration.fontScale.coerceAtLeast(.1f)
    val palette = WidgetWallPalette(
        content = contentColor.toArgb(),
        muted = contentColor.copy(alpha = .68f).toArgb(),
        surface = MaterialTheme.colorScheme.surfaceContainerLow.toArgb(),
        primary = MaterialTheme.colorScheme.primary.toArgb(),
        error = MaterialTheme.colorScheme.error.toArgb(),
        typeface = typeface,
        fontSizeMultiplier = fontSizeMultiplier,
    )
    AndroidView(
        factory = {
            WidgetWallView(context).apply {
                bindHost(host, manager)
            }
        },
        update = { wall ->
            wall.render(
                entries = entries,
                availableProviders = availableProviders,
                installedPackages = installedPackages,
                palette = palette,
                density = density,
                horizontalDragHandler = horizontalDragHandler,
                callbacks = callbacks,
            )
        },
        modifier = modifier,
    )
}

/** Native scrolling boundary for RemoteViews widgets and their own nested scrollable children. */
private class WidgetWallView(context: Context) : FrameLayout(context) {
    private val recyclerView = WidgetWallRecyclerView(context)
    private val rail = WidgetWallScrollRail(context, recyclerView)
    private val adapter = WidgetWallAdapter(context)
    private val itemTouchHelper = ItemTouchHelper(WidgetTouchHelperCallback(adapter))
    private lateinit var host: AppWidgetHost
    private lateinit var manager: AppWidgetManager

    init {
        id = R.id.widget_wall
        clipChildren = false
        recyclerView.id = R.id.widget_wall_list
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null
        recyclerView.setPadding(0, 0, 0, context.dp(12))
        recyclerView.clipToPadding = false
        recyclerView.overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(
            rail,
            LayoutParams(context.dp(38), LayoutParams.MATCH_PARENT, Gravity.END),
        )
        itemTouchHelper.attachToRecyclerView(recyclerView)
        adapter.dragStarter = itemTouchHelper::startDrag
        recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    rail.refresh()
                }
            },
        )
        recyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> rail.refresh() }
    }

    fun bindHost(host: AppWidgetHost, manager: AppWidgetManager) {
        this.host = host
        this.manager = manager
        adapter.bindHost(host, manager)
    }

    fun render(
        entries: List<WidgetEntry>,
        availableProviders: List<AppWidgetProviderInfo>,
        installedPackages: Set<String>,
        palette: WidgetWallPalette,
        density: Float,
        horizontalDragHandler: WidgetHorizontalDragHandler?,
        callbacks: WidgetWallCallbacks,
    ) {
        check(::host.isInitialized && ::manager.isInitialized)
        recyclerView.horizontalDragHandler = horizontalDragHandler
        rail.palette = palette
        adapter.render(
            entries,
            availableProviders,
            installedPackages,
            palette,
            density,
            callbacks,
            recyclerView.width,
        )
        recyclerView.post(rail::refresh)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) adapter.wallWidth = w
    }
}
