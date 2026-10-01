package com.katoaapps.openminilaunch.ui.minkspace.calculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.calculator.CalculatorHistoryEntry
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceTitle
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun CalculatorMiniApp(
    history: List<CalculatorHistoryEntry>,
    onHistoryAdded: (expression: String, result: String) -> Unit,
    onHistoryCleared: () -> Unit,
    modifier: Modifier = Modifier,
    mutedContentColor: Color = Muted,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val draft = remember { CalculatorDraftState() }
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.dp14, vertical = Dimens.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MinkSpaceTitle(
                title = if (pagerState.currentPage == 0) {
                    stringResource(R.string.calculator)
                } else {
                    stringResource(R.string.calculator_history)
                },
                icon = Icons.Default.Calculate,
                contentColor = LocalContentColor.current,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${pagerState.currentPage + 1}/2",
                color = mutedContentColor,
                style = MaterialTheme.typography.labelSmall,
            )
            if (pagerState.currentPage == 1 && history.isNotEmpty()) {
                IconButton(onClick = onHistoryCleared) {
                    Icon(Icons.Default.DeleteSweep, stringResource(R.string.clear_history))
                }
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            if (page == 0) {
                CalculatorKeypad(
                    draft = draft,
                    onHistoryAdded = onHistoryAdded,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CalculatorHistory(
                    entries = history,
                    mutedContentColor = mutedContentColor,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
