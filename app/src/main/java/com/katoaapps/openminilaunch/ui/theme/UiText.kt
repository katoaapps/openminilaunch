package com.katoaapps.openminilaunch.ui.theme

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.WidgetGridSize

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
internal fun socialGoalLabel(minutes: Int): String = when {
    minutes == 0 -> stringResource(R.string.hours_short, 0)
    minutes < 60 -> stringResource(R.string.minutes_short, minutes)
    minutes % 60 == 0 -> stringResource(R.string.hours_short, minutes / 60)
    else -> stringResource(R.string.minutes_short, minutes)
}

@Composable
internal fun WidgetGridSize.displayLabel(): String =
    stringResource(R.string.widget_grid_size, columns, rows)
