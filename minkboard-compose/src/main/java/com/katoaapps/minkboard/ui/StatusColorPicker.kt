package com.katoaapps.minkboard.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal data class StatusColorChoice(
    val argb: Int,
    @param:StringRes val labelRes: Int,
)

internal val STATUS_COLORS = listOf(
    StatusColorChoice(0xFF602C00.toInt(), R.string.color_brown),
    StatusColorChoice(0xFFAD4A25.toInt(), R.string.color_rust),
    StatusColorChoice(0xFFD6A300.toInt(), R.string.color_gold),
    StatusColorChoice(0xFF173529.toInt(), R.string.color_forest),
    StatusColorChoice(0xFF2563EB.toInt(), R.string.color_blue),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatusColorPicker(
    selectedColor: Int,
    customColorDraft: StatusCustomColorDraft?,
    onColorSelected: (Int) -> Unit,
    onCustomColorDraftChange: (StatusCustomColorDraft?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCustomColor = STATUS_COLORS.none { it.argb == selectedColor }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        STATUS_COLORS.forEach { color ->
            FilterChip(
                selected = color.argb == selectedColor,
                onClick = { onColorSelected(color.argb) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(Color(color.argb), CircleShape),
                        )
                        Text(stringResource(color.labelRes))
                    }
                },
            )
        }
        FilterChip(
            selected = isCustomColor,
            onClick = {
                onCustomColorDraftChange(statusCustomColorDraft(selectedColor))
            },
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(Color(selectedColor), CircleShape),
                    )
                    Text(stringResource(R.string.color_custom))
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }
            },
        )
    }
    if (customColorDraft != null) {
        StatusCustomColorDialog(
            draft = customColorDraft,
            onDraftChange = onCustomColorDraftChange,
            onDismiss = { onCustomColorDraftChange(null) },
            onConfirm = onColorSelected,
        )
    }
}
