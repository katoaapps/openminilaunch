package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Parcelable
import android.util.SparseArray
import android.view.ViewGroup

internal const val MINK_WIDGET_HOST_ID = 0x4D494E4B

internal class InteractiveAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    /**
     * A Classic pager can dispose the Widget page while the launcher Activity remains alive. Keep
     * the provider-owned view at the Activity-scoped host so returning to Widgets reuses the same
     * hierarchy, including any inner list position the provider keeps in that hierarchy.
     *
     * This is intentionally session-only. RemoteViews hierarchy state is provider implementation
     * detail and is not safe to persist to disk across provider updates or process recreation.
     */
    private val sessionViews = mutableMapOf<Int, AppWidgetHostView>()

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo,
    ): AppWidgetHostView = InteractiveAppWidgetHostView(context).also { view ->
        sessionViews[appWidgetId] = view
    }

    fun obtainView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo,
    ): AppWidgetHostView = sessionViews[appWidgetId]?.also { view ->
        // A cached host view can still be attached to a page being disposed. Detaching here makes
        // ownership explicit before the new Widget Wall adopts it.
        (view.parent as? ViewGroup)?.removeView(view)
    } ?: createView(context, appWidgetId, appWidget)

    override fun deleteAppWidgetId(appWidgetId: Int) {
        forgetView(appWidgetId)
        super.deleteAppWidgetId(appWidgetId)
    }

    override fun onAppWidgetRemoved(appWidgetId: Int) {
        forgetView(appWidgetId)
        super.onAppWidgetRemoved(appWidgetId)
    }

    private fun forgetView(appWidgetId: Int) {
        sessionViews.remove(appWidgetId)?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
    }
}

private class InteractiveAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    private var unfocusedHierarchyState: SparseArray<Parcelable>? = null

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!hasWindowFocus && isAttachedToWindow) {
            unfocusedHierarchyState = SparseArray<Parcelable>().also(::saveHierarchyState)
        }
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            val savedState = unfocusedHierarchyState ?: return
            post {
                if (isAttachedToWindow) restoreHierarchyState(savedState)
                if (unfocusedHierarchyState === savedState) unfocusedHierarchyState = null
            }
        }
    }
}
