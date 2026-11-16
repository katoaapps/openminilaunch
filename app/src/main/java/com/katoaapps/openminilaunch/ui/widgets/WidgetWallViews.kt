package com.katoaapps.openminilaunch.ui.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.recyclerview.widget.RecyclerView
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.WidgetEntry
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.model.WidgetProviderIdentity
import kotlin.math.roundToInt

internal class WidgetCardView(context: Context) : LinearLayout(context) {
    private val header = LinearLayout(context)
    private val dragButton = DragHandleButton(context)
    private val label = TextView(context)
    private val sizeLabel = TextView(context)
    private val options = ImageButton(context)
    private val content = FrameLayout(context)
    private var palette = fallbackWidgetWallPalette()
    private var accessibilityActionIds = emptyList<Int>()

    init {
        orientation = VERTICAL
        layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(context.dp(20), 0, context.dp(48), context.dp(20))
        }
        header.orientation = HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        dragButton.setImageDrawable(DragHandleDrawable())
        dragButton.background = null
        dragButton.contentDescription = context.getString(R.string.drag_to_reorder)
        header.addView(dragButton, LinearLayout.LayoutParams(context.dp(44), context.dp(44)))
        label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
        label.maxLines = 1
        label.ellipsize = android.text.TextUtils.TruncateAt.END
        header.addView(label, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        sizeLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
        header.addView(sizeLabel, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        options.setImageDrawable(MoreDrawable())
        options.background = null
        options.contentDescription = context.getString(R.string.widget_options)
        header.addView(options, LinearLayout.LayoutParams(context.dp(44), context.dp(44)))
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun bindHeader(
        entry: WidgetEntry,
        palette: WidgetWallPalette,
        onStartDrag: () -> Unit,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        onMoveUp: () -> Boolean,
        onMoveDown: () -> Boolean,
    ) {
        this.palette = palette
        label.typeface = Typeface.create(palette.typeface, Typeface.BOLD)
        sizeLabel.typeface = palette.typeface
        label.setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            11f * palette.fontSizeMultiplier,
        )
        sizeLabel.setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            10f * palette.fontSizeMultiplier,
        )
        dragButton.setColorFilter(palette.muted)
        options.setColorFilter(palette.muted)
        label.setTextColor(palette.content)
        sizeLabel.setTextColor(palette.muted)
        dragButton.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> onStartDrag()
                MotionEvent.ACTION_UP -> view.performClick()
            }
            true
        }
        accessibilityActionIds.forEach { ViewCompat.removeAccessibilityAction(dragButton, it) }
        accessibilityActionIds = buildList {
            if (canMoveUp) {
                add(
                    ViewCompat.addAccessibilityAction(
                        dragButton,
                        context.getString(R.string.move_up),
                        AccessibilityViewCommand { _, _ -> onMoveUp() },
                    ),
                )
            }
            if (canMoveDown) {
                add(
                    ViewCompat.addAccessibilityAction(
                        dragButton,
                        context.getString(R.string.move_down),
                        AccessibilityViewCommand { _, _ -> onMoveDown() },
                    ),
                )
            }
        }
        if (entry.gridSize != null) sizeLabel.text = context.getString(
            R.string.widget_grid_size,
            entry.gridSize?.columns,
            entry.gridSize?.rows,
        ) else sizeLabel.text = ""
        resetTransform()
    }

    fun setLabel(text: CharSequence, gridSize: WidgetGridSize, palette: WidgetWallPalette) {
        label.text = text
        sizeLabel.text = context.getString(R.string.widget_grid_size, gridSize.columns, gridSize.rows)
        label.setTextColor(palette.content)
        sizeLabel.setTextColor(palette.muted)
    }

    fun setOptions(
        canResize: Boolean,
        palette: WidgetWallPalette,
        onResize: () -> Unit,
        onRemove: () -> Unit,
    ) {
        options.visibility = VISIBLE
        options.setOnClickListener { anchor ->
            PopupMenu(context, anchor).apply {
                if (canResize) menu.add(context.getString(R.string.resize_widget, label.text))
                    .setOnMenuItemClickListener { onResize(); true }
                menu.add(context.getString(R.string.remove)).setOnMenuItemClickListener {
                    onRemove()
                    true
                }
                show()
            }
        }
        options.setColorFilter(palette.muted)
    }

    fun clearOptions() {
        options.visibility = INVISIBLE
        options.setOnClickListener(null)
    }

    fun showWidget(
        view: AppWidgetHostView,
        widthPx: Int,
        heightPx: Int,
        onSizeReady: (width: Int, height: Int) -> Unit,
    ) {
        content.removeAllViews()
        content.background = null
        content.minimumHeight = heightPx
        content.clipChildren = false
        val radius = context.dp(24).toFloat()
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        view.clipToOutline = true
        content.addView(
            view,
            FrameLayout.LayoutParams(widthPx, heightPx, Gravity.CENTER_HORIZONTAL),
        )
        view.post {
            if (view.width > 0 && view.height > 0) {
                onSizeReady(view.width, view.height)
            }
        }
    }

    fun showUnavailable(
        palette: WidgetWallPalette,
        onRetry: () -> Unit,
        onRemove: () -> Unit,
    ) {
        label.text = context.getString(R.string.widget_temporarily_unavailable)
        clearOptions()
        showMessagePanel(
            description = context.getString(R.string.widget_temporarily_unavailable_description),
            primaryLabel = context.getString(R.string.retry),
            onPrimary = onRetry,
            palette = palette,
            onRemove = onRemove,
        )
    }

    fun showRestore(
        provider: WidgetProviderIdentity,
        packageInstalled: Boolean,
        palette: WidgetWallPalette,
        onRestore: () -> Unit,
        onRemove: () -> Unit,
    ) {
        val appName = provider.appLabel.ifBlank { provider.packageName }
        val description = context.getString(
            if (packageInstalled) R.string.widget_restore_ready_description
            else R.string.widget_restore_missing_app_description,
            appName,
        )
        val action = context.getString(
            if (packageInstalled) R.string.set_up_widget else R.string.reinstall,
        )
        showMessagePanel(description, action, onRestore, palette, onRemove)
    }

    private fun showMessagePanel(
        description: String,
        primaryLabel: String,
        onPrimary: () -> Unit,
        palette: WidgetWallPalette,
        onRemove: () -> Unit,
    ) {
        content.removeAllViews()
        content.minimumHeight = 0
        content.background = roundedBackground(palette.surface, context.dp(24).toFloat())
        val column = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(context.dp(20), context.dp(20), context.dp(20), context.dp(20))
        }
        val descriptionView = TextView(context).apply {
            text = description
            gravity = Gravity.CENTER
            setTextColor(palette.muted)
            typeface = palette.typeface
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                12f * palette.fontSizeMultiplier,
            )
        }
        column.addView(descriptionView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val actions = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            orientation = HORIZONTAL
        }
        actions.addView(actionButton(primaryLabel, palette.primary, onPrimary))
        actions.addView(actionButton(context.getString(R.string.remove), palette.error, onRemove))
        column.addView(actions, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = context.dp(8)
        })
        content.addView(column, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun actionButton(text: String, color: Int, onClick: () -> Unit): Button = Button(context).apply {
        this.text = text
        typeface = palette.typeface
        setTextColor(color)
        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        setOnClickListener { onClick() }
    }

    private fun resetTransform() {
        scaleX = 1f
        scaleY = 1f
        elevation = 0f
    }
}

@SuppressLint("ViewConstructor")
internal class WidgetWallScrollRail(
    context: Context,
    private val recyclerView: WidgetWallRecyclerView,
) : View(context) {
    var palette = fallbackWidgetWallPalette()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dragging = false

    init {
        contentDescription = context.getString(R.string.scroll_widgets)
        visibility = INVISIBLE
    }

    fun refresh() {
        val (range, extent, _) = recyclerView.verticalMetrics()
        val scrollable = range > extent && extent > 0
        visibility = if (scrollable) VISIBLE else INVISIBLE
        isFocusable = scrollable
        importantForAccessibility = if (scrollable) {
            IMPORTANT_FOR_ACCESSIBILITY_YES
        } else {
            IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        if (scrollable) invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val (range, extent, offset) = recyclerView.verticalMetrics()
        if (range <= extent || extent <= 0) return
        val trackTop = height * .16f
        val trackBottom = height * .84f
        val trackHeight = trackBottom - trackTop
        paint.color = withAlpha(palette.muted, .35f)
        paint.strokeWidth = context.dp(3).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(width / 2f, trackTop, width / 2f, trackBottom, paint)
        val thumbHeight = (trackHeight * extent / range).coerceAtLeast(context.dp(32).toFloat())
        val travel = trackHeight - thumbHeight
        val fraction = offset.toFloat() / (range - extent).coerceAtLeast(1)
        val thumbTop = trackTop + travel * fraction
        paint.color = palette.primary
        paint.strokeWidth = context.dp(6).toFloat()
        canvas.drawLine(
            width / 2f,
            thumbTop,
            width / 2f,
            thumbTop + thumbHeight,
            paint,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val (range, extent, offset) = recyclerView.verticalMetrics()
        if (range <= extent) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> dragging = true
            MotionEvent.ACTION_MOVE -> if (dragging) {
                val trackTop = height * .16f
                val trackHeight = height * .68f
                val fraction = ((event.y - trackTop) / trackHeight).coerceIn(0f, 1f)
                val target = ((range - extent) * fraction).roundToInt()
                recyclerView.scrollBy(0, target - offset)
            }
            MotionEvent.ACTION_UP -> {
                dragging = false
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        val (range, extent, offset) = recyclerView.verticalMetrics()
        val maxOffset = (range - extent).coerceAtLeast(0)
        info.className = android.widget.SeekBar::class.java.name
        info.isScrollable = maxOffset > 0
        info.rangeInfo = widgetScrollRangeInfo(maxOffset, offset)
        if (offset < maxOffset) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        }
        if (offset > 0) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        val (_, extent, _) = recyclerView.verticalMetrics()
        val amount = (extent * .8f).roundToInt().coerceAtLeast(context.dp(48))
        return when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> {
                recyclerView.smoothScrollBy(0, amount)
                true
            }
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> {
                recyclerView.smoothScrollBy(0, -amount)
                true
            }
            else -> super.performAccessibilityAction(action, arguments)
        }
    }
}

private class DragHandleDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    override fun draw(canvas: Canvas) {
        paint.color = colorFilterColor
        val centerY = bounds.exactCenterY()
        for (offset in intArrayOf(-6, 0, 6)) {
            canvas.drawLine(bounds.left + 10f, centerY + offset, bounds.right - 10f, centerY + offset, paint)
        }
    }

    private var colorFilterColor: Int = Color.GRAY
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter }
    override fun setTint(tintColor: Int) { colorFilterColor = tintColor; invalidateSelf() }
    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

private class DragHandleButton(context: Context) : ImageButton(context) {
    override fun performClick(): Boolean = super.performClick()
}

private class MoreDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var tint = Color.GRAY
    override fun draw(canvas: Canvas) {
        paint.color = tint
        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()
        for (offset in intArrayOf(-8, 0, 8)) canvas.drawCircle(x, y + offset, 2.4f, paint)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter }
    override fun setTint(tintColor: Int) { tint = tintColor; invalidateSelf() }
    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

internal fun fallbackWidgetWallPalette() = WidgetWallPalette(
    content = Color.WHITE,
    muted = Color.GRAY,
    surface = Color.DKGRAY,
    primary = Color.WHITE,
    error = Color.RED,
    typeface = Typeface.DEFAULT,
    fontSizeMultiplier = 1f,
)

private fun roundedBackground(color: Int, radiusPx: Float): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(color)
    cornerRadius = radiusPx
}

private fun withAlpha(color: Int, fraction: Float): Int = Color.argb(
    (Color.alpha(color) * fraction).roundToInt(),
    Color.red(color),
    Color.green(color),
    Color.blue(color),
)

private fun widgetScrollRangeInfo(
    maxOffset: Int,
    offset: Int,
): AccessibilityNodeInfo.RangeInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    AccessibilityNodeInfo.RangeInfo(
        AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,
        0f,
        maxOffset.toFloat(),
        offset.coerceIn(0, maxOffset).toFloat(),
    )
} else {
    legacyWidgetScrollRangeInfo(maxOffset, offset)
}

@Suppress("DEPRECATION")
private fun legacyWidgetScrollRangeInfo(
    maxOffset: Int,
    offset: Int,
): AccessibilityNodeInfo.RangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(
    AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,
    0f,
    maxOffset.toFloat(),
    offset.coerceIn(0, maxOffset).toFloat(),
)

internal fun String.stableLongId(): Long {
    var result = 1125899906842597L
    forEach { result = 31L * result + it.code }
    return result
}
