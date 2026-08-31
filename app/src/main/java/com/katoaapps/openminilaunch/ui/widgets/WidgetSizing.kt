package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.WidgetGridSize
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.displayLabel
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun WidgetSizeDialog(
    info: AppWidgetProviderInfo,
    current: WidgetGridSize,
    @StringRes confirmLabelRes: Int,
    onConfirm: (WidgetGridSize) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val range = remember(info.provider, info.minWidth, info.minHeight) {
        widgetSizeRange(info, context.resources.displayMetrics.density)
    }
    var selected by remember(info.provider, current) {
        mutableStateOf(
            WidgetGridSize(
                current.columns.coerceIn(range.minColumns, range.maxColumns),
                current.rows.coerceIn(range.minRows, range.maxRows),
            ),
        )
    }
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            Modifier.minkDialogWidth(),
            shape = RoundedCornerShape(Dimens.dp26),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp22), verticalArrangement = Arrangement.spacedBy(Dimens.dp14)) {
                Text(stringResource(R.string.widget_size), fontSize = Dimens.sp22, fontWeight = FontWeight.Black)
                Text(info.loadLabel(context.packageManager), color = Muted, fontSize = Dimens.sp13)
                Text(
                    selected.displayLabel(),
                    fontSize = Dimens.sp32,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                SizeSelector(
                    label = stringResource(R.string.width),
                    range = range.minColumns..range.maxColumns,
                    selected = selected.columns,
                    onSelect = { selected = selected.copy(columns = it) },
                )
                SizeSelector(
                    label = stringResource(R.string.height),
                    range = range.minRows..range.maxRows,
                    selected = selected.rows,
                    onSelect = { selected = selected.copy(rows = it) },
                )
                Text(
                    stringResource(R.string.widget_grid_description, stringResource(R.string.app_name)),
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(
                        onClick = { onConfirm(selected) },
                        modifier = Modifier.padding(start = Dimens.dp8),
                    ) { Text(stringResource(confirmLabelRes)) }
                }
            }
        }
    }
}

@Composable
private fun SizeSelector(
    label: String,
    range: IntRange,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Text(
        label,
        color = Muted,
        fontSize = Dimens.sp10,
        fontWeight = FontWeight.Bold,
        letterSpacing = Dimens.sp1,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        range.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text("$value") },
            )
        }
    }
}

internal fun widgetSizeRange(info: AppWidgetProviderInfo, density: Float): WidgetSizeRange {
    fun cellsForPixels(pixels: Int, fallback: Int): Int =
        if (pixels > 0) estimateWidgetCells(pixels / density) else fallback

    val defaultColumns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
        info.targetCellWidth
    } else {
        cellsForPixels(info.minWidth, 2)
    }.coerceIn(1, 4)
    val defaultRows = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
        info.targetCellHeight
    } else {
        cellsForPixels(info.minHeight, 2)
    }.coerceIn(1, 5)
    val horizontallyResizable = info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0
    val verticallyResizable = info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0
    val minColumns = (if (horizontallyResizable) cellsForPixels(info.minResizeWidth, defaultColumns) else defaultColumns)
        .coerceIn(1, 4)
    val minRows = (if (verticallyResizable) cellsForPixels(info.minResizeHeight, defaultRows) else defaultRows)
        .coerceIn(1, 5)
    val declaredMaxColumns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) cellsForPixels(info.maxResizeWidth, 4) else 4
    val declaredMaxRows = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) cellsForPixels(info.maxResizeHeight, 5) else 5
    val maxColumns = (if (horizontallyResizable) declaredMaxColumns else defaultColumns).coerceIn(minColumns, 4)
    val maxRows = (if (verticallyResizable) declaredMaxRows else defaultRows).coerceIn(minRows, 5)
    return WidgetSizeRange(
        minColumns = minColumns,
        maxColumns = maxColumns,
        minRows = minRows,
        maxRows = maxRows,
        preferred = WidgetGridSize(
            defaultColumns.coerceIn(minColumns, maxColumns),
            defaultRows.coerceIn(minRows, maxRows),
        ),
    )
}

internal fun estimateWidgetCells(sizeDp: Float): Int = ceil((sizeDp + 30f) / 70f).toInt().coerceAtLeast(1)

@Suppress("DEPRECATION")
internal fun reportWidgetSize(view: AppWidgetHostView, size: IntSize, density: Float) {
    val widthDp = size.width / density
    val heightDp = size.height / density
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
    } else {
        view.updateAppWidgetSize(
            null,
            widthDp.roundToInt(),
            heightDp.roundToInt(),
            widthDp.roundToInt(),
            heightDp.roundToInt(),
        )
    }
}
