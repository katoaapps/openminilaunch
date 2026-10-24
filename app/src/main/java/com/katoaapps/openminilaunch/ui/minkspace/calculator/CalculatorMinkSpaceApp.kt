package com.katoaapps.openminilaunch.ui.minkspace.calculator

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.features.minkspace.calculator.CalculatorHistoryRepository
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceScope

/** Calculator implementation registered with the MinkSpace platform. */
internal class CalculatorMinkSpaceApp : MinkSpaceApp {
    override val manifest: MinkSpaceMiniAppManifest = Manifest

    @Composable
    override fun Content(scope: MinkSpaceScope, modifier: Modifier) {
        val context = LocalContext.current
        val historyRepository = remember(context) { CalculatorHistoryRepository.get(context) }
        scope.Surface(modifier = modifier) {
            CalculatorMiniApp(
                history = historyRepository.entries,
                onHistoryAdded = historyRepository::add,
                onHistoryCleared = historyRepository::clear,
                modifier = Modifier.fillMaxSize(),
                mutedContentColor = scope.mutedContentColor,
            )
        }
    }

    companion object {
        val Manifest = MinkSpaceMiniAppManifest(
            miniApp = MinkSpaceMiniApp.CALCULATOR,
            labelRes = R.string.calculator,
            icon = Icons.Default.Calculate,
        )
    }
}
