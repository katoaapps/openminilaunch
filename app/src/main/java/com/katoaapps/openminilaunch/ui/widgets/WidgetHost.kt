package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

internal const val MINK_WIDGET_HOST_ID = 0x4D494E4B

internal class InteractiveAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo,
    ): AppWidgetHostView = InteractiveAppWidgetHostView(context)
}

private class InteractiveAppWidgetHostView(context: Context) : AppWidgetHostView(context)
