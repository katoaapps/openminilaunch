package com.katoaapps.openminilaunch.ui.minkspace.calculator

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.features.minkspace.calculator.CalculatorHistoryRepository
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSurface

/** Home-facing entry point. Feature state and common MinkSpace framing stay out of the host. */
@Composable
internal fun CalculatorMiniAppPage(
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val historyRepository = remember(context) { CalculatorHistoryRepository.get(context) }
    MinkSpaceMiniAppSurface(
        compact = compact,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        CalculatorMiniApp(
            history = historyRepository.entries,
            onHistoryAdded = historyRepository::add,
            onHistoryCleared = historyRepository::clear,
            modifier = Modifier.fillMaxSize(),
            mutedContentColor = mutedContentColor,
        )
    }
}
