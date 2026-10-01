package com.katoaapps.openminilaunch.ui.minkspace.music

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.minkspace.core.rememberMinkSpacePresentationState
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme

class MusicLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = LauncherStore.get(this)
        setContent {
            MinkLauncherTheme(store) {
                MusicLabScreen(onClose = ::finish)
            }
        }
    }
}

@Composable
private fun MusicLabScreen(onClose: () -> Unit) {
    val presentationState = rememberMinkSpacePresentationState()
    Column(Modifier.fillMaxSize()) {
        PageHeader(stringResource(R.string.music_player_preview), onClose)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.dp16),
            contentAlignment = Alignment.Center,
        ) {
            MusicMiniAppPage(
                compact = false,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                presentationState = presentationState,
                modifier = Modifier
                    .widthIn(max = Dimens.dp420)
                    .heightIn(min = Dimens.dp420, max = Dimens.dp560),
            )
        }
    }
}
