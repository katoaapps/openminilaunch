package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.Size
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.katoaapps.openminilaunch.model.WidgetEntry
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.model.WidgetRestoreRecipe
import kotlin.math.roundToInt

internal class WidgetWallAdapter(private val context: Context) : RecyclerView.Adapter<WidgetViewHolder>() {
    private var entries = mutableListOf<WidgetEntry>()
    private var availableProviders: List<AppWidgetProviderInfo> = emptyList()
    private var installedPackages: Set<String> = emptySet()
    private var palette = fallbackWidgetWallPalette()
    private var density = context.resources.displayMetrics.density
    private var callbacks: WidgetWallCallbacks? = null
    private lateinit var host: AppWidgetHost
    private lateinit var manager: AppWidgetManager
    private val hostedViews = mutableMapOf<Int, AppWidgetHostView>()
    private val reportedWidgetSizes = mutableMapOf<Int, Size>()
    var dragStarter: ((RecyclerView.ViewHolder) -> Unit)? = null
    var wallWidth: Int = 0
        set(value) {
            if (field == value) return
            field = value
            if (value > 0) notifyItemRangeChanged(0, itemCount)
        }
    private var orderChanged = false

    init {
        setHasStableIds(true)
    }

    fun bindHost(host: AppWidgetHost, manager: AppWidgetManager) {
        this.host = host
        this.manager = manager
    }

    fun render(
        entries: List<WidgetEntry>,
        availableProviders: List<AppWidgetProviderInfo>,
        installedPackages: Set<String>,
        palette: WidgetWallPalette,
        density: Float,
        callbacks: WidgetWallCallbacks,
        wallWidth: Int,
    ) {
        val presentationChanged = this.availableProviders != availableProviders ||
            this.installedPackages != installedPackages || this.palette != palette ||
            this.density != density
        this.availableProviders = availableProviders
        this.installedPackages = installedPackages
        this.palette = palette
        this.density = density
        this.callbacks = callbacks
        this.wallWidth = wallWidth
        val entriesChanged = this.entries != entries
        if (entriesChanged) {
            val previousEntries = this.entries.toList()
            val diff = DiffUtil.calculateDiff(
                object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int = previousEntries.size
                    override fun getNewListSize(): Int = entries.size
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        previousEntries[oldItemPosition].entryId == entries[newItemPosition].entryId

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        previousEntries[oldItemPosition] == entries[newItemPosition]
                },
            )
            this.entries.clear()
            this.entries.addAll(entries)
            diff.dispatchUpdatesTo(this)
        }
        if (presentationChanged) {
            notifyItemRangeChanged(0, itemCount)
        }
        val liveIds = entries.filterIsInstance<WidgetEntry.Bound>().mapTo(mutableSetOf()) { it.appWidgetId }
        hostedViews.keys.filterNot(liveIds::contains).forEach { id ->
            hostedViews.remove(id)?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
            reportedWidgetSizes.remove(id)
        }
    }

    override fun getItemId(position: Int): Long = entries[position].entryId.stableLongId()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetViewHolder =
        WidgetViewHolder(WidgetCardView(context))

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
        val entry = entries[position]
        holder.card.bindHeader(
            entry = entry,
            palette = palette,
            onStartDrag = {
                holder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                dragStarter?.invoke(holder)
            },
            canMoveUp = position > 0,
            canMoveDown = position < entries.lastIndex,
            onMoveUp = { moveForAccessibility(entry.entryId, -1) },
            onMoveDown = { moveForAccessibility(entry.entryId, 1) },
        )
        when (entry) {
            is WidgetEntry.Bound -> bindBound(holder.card, entry)
            is WidgetEntry.RestorePlaceholder -> bindRestore(holder.card, entry)
        }
    }

    override fun getItemCount(): Int = entries.size

    fun move(from: Int, to: Int): Boolean {
        if (from !in entries.indices || to !in entries.indices) return false
        val entry = entries.removeAt(from)
        entries.add(to, entry)
        notifyItemMoved(from, to)
        orderChanged = true
        return true
    }

    fun finishMove() {
        if (!orderChanged) return
        orderChanged = false
        callbacks?.onOrderChange?.invoke(entries.map(WidgetEntry::entryId))
        // ItemTouchHelper moves holders without rebinding every row. Refresh the headers so
        // TalkBack's move-up/move-down actions reflect each widget's new position.
        notifyItemRangeChanged(0, itemCount)
    }

    private fun moveForAccessibility(entryId: String, offset: Int): Boolean {
        val from = entries.indexOfFirst { it.entryId == entryId }
        val to = from + offset
        if (!move(from, to)) return false
        finishMove()
        return true
    }

    private fun bindBound(card: WidgetCardView, entry: WidgetEntry.Bound) {
        val info = manager.getAppWidgetInfo(entry.appWidgetId)
        if (info == null) {
            if (entry.provider != null && entry.gridSize != null) {
                bindRestore(
                    card,
                    WidgetEntry.RestorePlaceholder(
                        WidgetRestoreRecipe(entry.entryId, entry.provider, entry.gridSize),
                    ),
                    removeEntry = entry,
                )
            } else {
                card.showUnavailable(
                    palette,
                    onRetry = { callbacks?.onRetryUnavailableWidget?.invoke() },
                    onRemove = { callbacks?.onRemove?.invoke(entry) },
                )
            }
            return
        }
        val gridSize = entry.gridSize ?: widgetSizeRange(info, density).preferred
        card.setLabel(info.loadLabel(context.packageManager), gridSize, palette)
        card.setOptions(
            canResize = widgetSizeRange(info, density).isResizable,
            palette = palette,
            onResize = { callbacks?.onResize?.invoke(entry, info, gridSize) },
            onRemove = { callbacks?.onRemove?.invoke(entry) },
        )
        val widgetView = hostedViews.getOrPut(entry.appWidgetId) {
            host.createView(context, entry.appWidgetId, info).apply {
                setAppWidget(entry.appWidgetId, info)
                ViewCompat.setNestedScrollingEnabled(this, true)
            }
        }
        (widgetView.parent as? ViewGroup)?.removeView(widgetView)
        val contentWidthPx = (wallWidth - context.dp(20) - context.dp(48)).coerceAtLeast(context.dp(96))
        val dimensions = widgetPanelDimensions(contentWidthPx / density, gridSize)
        val widthPx = (dimensions.widthDp * density).roundToInt()
        val heightPx = (dimensions.heightDp * density).roundToInt()
        card.showWidget(widgetView, widthPx, heightPx) { measuredWidth, measuredHeight ->
            // The posted measurement can arrive after removal/replacement. Never report options
            // for a host view that is no longer the live view for this app-widget ID.
            if (hostedViews[entry.appWidgetId] !== widgetView) return@showWidget
            val measuredSize = Size(measuredWidth, measuredHeight)
            if (reportedWidgetSizes[entry.appWidgetId] != measuredSize) {
                reportWidgetSize(
                    widgetView,
                    androidx.compose.ui.unit.IntSize(measuredWidth, measuredHeight),
                    density,
                )
                reportedWidgetSizes[entry.appWidgetId] = measuredSize
            }
        }
    }

    private fun bindRestore(
        card: WidgetCardView,
        placeholder: WidgetEntry.RestorePlaceholder,
        removeEntry: WidgetEntry = placeholder,
    ) {
        val provider = placeholder.recipe.provider
        val exactProvider = availableProviders.singleOrNull {
            it.provider.packageName == provider.packageName &&
                it.provider.className == provider.className
        }
        val packageInstalled = provider.packageName in installedPackages
        val title = provider.widgetLabel.ifBlank { provider.appLabel.ifBlank { provider.packageName } }
        card.setLabel(title, placeholder.gridSize, palette)
        card.clearOptions()
        card.showRestore(
            provider = provider,
            packageInstalled = packageInstalled,
            palette = palette,
            onRestore = {
                when {
                    exactProvider != null -> callbacks?.onBindReplacement?.invoke(
                        placeholder.entryId,
                        exactProvider,
                        placeholder.gridSize,
                    )
                    packageInstalled -> callbacks?.onChooseReplacement?.invoke(placeholder)
                    else -> callbacks?.onReinstall?.invoke(provider)
                }
            },
            onRemove = { callbacks?.onRemove?.invoke(removeEntry) },
        )
    }
}

internal class WidgetViewHolder(val card: WidgetCardView) : RecyclerView.ViewHolder(card)

internal class WidgetTouchHelperCallback(
    private val adapter: WidgetWallAdapter,
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
    override fun isLongPressDragEnabled(): Boolean = false
    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean = adapter.move(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.scaleX = 1f
        viewHolder.itemView.scaleY = 1f
        viewHolder.itemView.elevation = 0f
        adapter.finishMove()
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        viewHolder?.itemView?.apply {
            scaleX = 1.025f
            scaleY = 1.025f
            elevation = context.dp(12).toFloat()
        }
    }
}
