package com.katoaapps.openminilaunch.ui.minkspace.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.calculator.CalculatorHistoryRepository
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme

class CalculatorLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = LauncherStore.get(this)
        setContent {
            MinkLauncherTheme(store) {
                CalculatorLabScreen(onClose = ::finish)
            }
        }
    }
}

@Composable
private fun CalculatorLabScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val historyRepository = remember(context) { CalculatorHistoryRepository.get(context) }
    Column(Modifier.fillMaxSize()) {
        PageHeader(stringResource(R.string.mink_space_lab), onClose)
        Box(
            modifier = Modifier.fillMaxSize().padding(Dimens.dp16),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = Dimens.dp420)
                    .heightIn(min = Dimens.dp420, max = Dimens.dp560),
                shape = RoundedCornerShape(Dimens.dp28),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                CalculatorMiniApp(
                    history = historyRepository.entries,
                    onHistoryAdded = historyRepository::add,
                    onHistoryCleared = historyRepository::clear,
                )
            }
        }
    }
}
