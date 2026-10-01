package com.katoaapps.openminilaunch.ui.minkspace.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.calculator.CalculatorHistoryEntry
import com.katoaapps.openminilaunch.ui.theme.Dimens

@Composable
internal fun CalculatorHistory(
    entries: List<CalculatorHistoryEntry>,
    mutedContentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.calculator_history_empty), color = mutedContentColor)
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Dimens.dp12),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        items(entries) { entry ->
            Surface(
                shape = RoundedCornerShape(Dimens.dp12),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.fillMaxWidth().padding(Dimens.dp12)) {
                    Text(entry.expression, color = mutedContentColor)
                    Text(entry.result, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
