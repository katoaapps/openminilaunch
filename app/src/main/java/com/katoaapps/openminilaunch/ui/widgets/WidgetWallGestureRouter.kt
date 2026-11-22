package com.katoaapps.openminilaunch.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.widget.AbsSeekBar
import android.widget.AdapterViewAnimator
import android.widget.CompoundButton
import android.widget.HorizontalScrollView
import androidx.recyclerview.widget.RecyclerView
import com.katoaapps.openminilaunch.BuildConfig
import kotlin.math.abs

internal enum class WidgetGestureAxis { UNDECIDED, HORIZONTAL, VERTICAL }

internal fun classifyWidgetGesture(
    dx: Float,
    dy: Float,
    touchSlop: Int,
): WidgetGestureAxis = when {
    maxOf(abs(dx), abs(dy)) <= touchSlop -> WidgetGestureAxis.UNDECIDED
    abs(dx) > abs(dy) * 1.08f -> WidgetGestureAxis.HORIZONTAL
    abs(dy) > abs(dx) * 1.08f -> WidgetGestureAxis.VERTICAL
    // Do not permanently lock a nearly diagonal start to the vertical wall. Keep observing the
    // same DOWN-relative movement until the user's intended axis becomes clear.
    else -> WidgetGestureAxis.UNDECIDED
}

/**
 * Owns the touch boundary between Android widgets, the vertical widget wall, and Compose pages.
 * A single owner sees the entire stream, then delegates it based on the user's dominant axis.
 */
@SuppressLint("ClickableViewAccessibility")
internal class WidgetWallRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RecyclerView(context, attrs) {
    var horizontalDragHandler: WidgetHorizontalDragHandler? = null
    private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private var axis = WidgetGestureAxis.UNDECIDED
    private var horizontalChildOwnsGesture = false
    private var verticalChildOwnsGesture = false
    private var outerVerticalWasRebased = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var totalX = 0f
    private var hitChain: List<View> = emptyList()
    private var velocityTracker: VelocityTracker? = null
    private var loggedAmbiguousMovement = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && horizontalDragHandler != null) {
            // The native wall arbitrates this stream; it explicitly forwards page movement.
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return handled
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // Widget children are not allowed to change ownership at the Compose boundary. The wall
        // holds that boundary for the complete DOWN-to-UP stream and releases it explicitly in
        // dispatchTouchEvent. Returning false from onInterceptTouchEvent still leaves a matching
        // nested control in charge inside the wall.
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetGesture()
                downX = event.x
                downY = event.y
                lastX = event.x
                lastY = event.y
                hitChain = findHitChain(event.rawX, event.rawY)
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                gestureLog(
                    "DOWN x=${event.x.toInt()} y=${event.y.toInt()} " +
                        "hits=${hitChain.joinToString(" > ") { it.javaClass.simpleName }}",
                )
                super.onInterceptTouchEvent(event)
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                resolveAxis(event)
                return when (axis) {
                    WidgetGestureAxis.HORIZONTAL -> {
                        if (horizontalChildOwnsGesture) return false
                        val delta = event.x - lastX
                        totalX += delta
                        lastX = event.x
                        horizontalDragHandler?.onDragBy(delta)
                        true
                    }
                    WidgetGestureAxis.VERTICAL -> {
                        val deltaY = event.y - lastY
                        lastY = event.y
                        val direction = if (deltaY < 0f) 1 else -1
                        val childCanContinue = !outerVerticalWasRebased &&
                            hitChain.any { it.ownsVerticalGesture(direction) }
                        when {
                            childCanContinue -> {
                                verticalChildOwnsGesture = true
                                false
                            }
                            verticalChildOwnsGesture -> {
                                // Restart RecyclerView's movement origin at the widget's edge. If
                                // it inherited the original DOWN, the accumulated distance would
                                // make the outer wall jump as soon as the child ran out of content.
                                verticalChildOwnsGesture = false
                                outerVerticalWasRebased = true
                                val rebasedDown = MotionEvent.obtain(event).apply {
                                    action = MotionEvent.ACTION_DOWN
                                }
                                super.onInterceptTouchEvent(rebasedDown)
                                rebasedDown.recycle()
                                false
                            }
                            else -> super.onInterceptTouchEvent(event)
                        }
                    }
                    WidgetGestureAxis.UNDECIDED -> false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                finishHorizontalGesture(event.actionMasked == MotionEvent.ACTION_CANCEL)
                val intercepted = super.onInterceptTouchEvent(event)
                resetGesture()
                return intercepted
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            velocityTracker?.addMovement(event)
            // When the touched descendant does not consume DOWN (for example a plain header label
            // or empty RemoteViews region), RecyclerView itself becomes the touch target. Its later
            // MOVE events bypass onInterceptTouchEvent, so axis resolution must also happen here.
            resolveAxis(event)
        }
        if (axis != WidgetGestureAxis.HORIZONTAL || horizontalChildOwnsGesture) {
            return super.onTouchEvent(event)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val delta = event.x - lastX
                totalX += delta
                lastX = event.x
                horizontalDragHandler?.onDragBy(delta)
            }
            MotionEvent.ACTION_UP -> {
                finishHorizontalGesture(cancelled = false)
                resetGesture()
            }
            MotionEvent.ACTION_CANCEL -> {
                finishHorizontalGesture(cancelled = true)
                resetGesture()
            }
        }
        return true
    }

    private fun resolveAxis(event: MotionEvent) {
        if (axis != WidgetGestureAxis.UNDECIDED) return
        val dx = event.x - downX
        val dy = event.y - downY
        axis = classifyWidgetGesture(dx, dy, touchSlop)
        when (axis) {
            WidgetGestureAxis.HORIZONTAL -> {
                val direction = if (dx < 0f) 1 else -1
                val owningChild = hitChain.firstOrNull {
                    it.ownsHorizontalGesture(direction)
                }
                if (horizontalDragHandler == null || owningChild != null) {
                    horizontalChildOwnsGesture = true
                    gestureLog(
                        "LOCK HORIZONTAL dx=${dx.toInt()} dy=${dy.toInt()} " +
                            "owner=${owningChild?.javaClass?.simpleName ?: "none"}",
                    )
                } else {
                    gestureLog(
                        "LOCK HORIZONTAL dx=${dx.toInt()} dy=${dy.toInt()} owner=page",
                    )
                    horizontalDragHandler?.onDragStart()
                }
            }
            WidgetGestureAxis.VERTICAL -> {
                gestureLog("LOCK VERTICAL dx=${dx.toInt()} dy=${dy.toInt()}")
            }
            WidgetGestureAxis.UNDECIDED -> {
                if (!loggedAmbiguousMovement && maxOf(abs(dx), abs(dy)) > touchSlop) {
                    loggedAmbiguousMovement = true
                    gestureLog("WAIT DIAGONAL dx=${dx.toInt()} dy=${dy.toInt()}")
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        if (axis == WidgetGestureAxis.HORIZONTAL && !horizontalChildOwnsGesture) {
            horizontalDragHandler?.onDragCancel()
        }
        parent?.requestDisallowInterceptTouchEvent(false)
        resetGesture()
        super.onDetachedFromWindow()
    }

    private fun finishHorizontalGesture(cancelled: Boolean) {
        if (axis != WidgetGestureAxis.HORIZONTAL || horizontalChildOwnsGesture) return
        if (cancelled) {
            gestureLog("END HORIZONTAL cancelled totalX=${totalX.toInt()}")
            horizontalDragHandler?.onDragCancel()
        } else {
            velocityTracker?.computeCurrentVelocity(1000)
            val velocityX = velocityTracker?.xVelocity ?: 0f
            gestureLog(
                "END HORIZONTAL totalX=${totalX.toInt()} velocityX=${velocityX.toInt()}",
            )
            horizontalDragHandler?.onDragEnd(totalX, velocityX)
        }
    }

    private fun resetGesture() {
        velocityTracker?.recycle()
        velocityTracker = null
        axis = WidgetGestureAxis.UNDECIDED
        horizontalChildOwnsGesture = false
        verticalChildOwnsGesture = false
        outerVerticalWasRebased = false
        totalX = 0f
        hitChain = emptyList()
        loggedAmbiguousMovement = false
    }

    private fun findHitChain(rawX: Float, rawY: Float): List<View> {
        fun descend(view: View): List<View>? {
            val rect = Rect()
            if (!view.isShown || !view.getGlobalVisibleRect(rect) ||
                !rect.contains(rawX.toInt(), rawY.toInt())
            ) {
                return null
            }
            if (view is ViewGroup) {
                for (index in view.childCount - 1 downTo 0) {
                    val childChain = descend(view.getChildAt(index))
                    if (childChain != null) return childChain + view
                }
            }
            return listOf(view)
        }
        // Exclude this RecyclerView: only a widget descendant can retain the inner gesture.
        return descend(this).orEmpty().filterNot { it === this }
    }

    internal fun verticalMetrics(): Triple<Int, Int, Int> = Triple(
        computeVerticalScrollRange(),
        computeVerticalScrollExtent(),
        computeVerticalScrollOffset(),
    )
}

private fun gestureLog(message: String) {
    if (BuildConfig.DEBUG) Log.d("WidgetGesture", message)
}

private fun View.ownsHorizontalGesture(direction: Int): Boolean = when (this) {
    // These controls use a horizontal drag as their primary interaction, even when the thumb is
    // already at an edge.
    is AbsSeekBar, is CompoundButton -> true

    // Only retain a scrollable child's gesture while it can continue in that direction. This lets
    // the launcher page take over at the child's edge, like a conventional launcher widget page.
    is HorizontalScrollView -> canScrollHorizontally(direction)
    is RecyclerView -> layoutManager?.canScrollHorizontally() == true &&
        canScrollHorizontally(direction)

    // Do not trust View.canScrollHorizontally() generically. Ellipsized TextViews and broad
    // RemoteViews descendants can report horizontal range even though they are not user-pannable,
    // which used to create dead areas across widget headers and visually empty widget content.
    else -> false
}

private fun View.ownsVerticalGesture(direction: Int): Boolean =
    // StackView and AdapterViewFlipper consume discrete swipes but do not expose scroll range
    // through canScrollVertically(), so they must retain the stream explicitly.
    this is AdapterViewAnimator || canScrollVertically(direction)
