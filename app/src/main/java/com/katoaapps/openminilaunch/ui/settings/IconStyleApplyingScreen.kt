package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

/** Brief visual confirmation while the new icon style propagates to launcher surfaces. */
@Composable
internal fun IconStyleApplyingScreen(styleLabel: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.dp32),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.applying_icon_style),
            fontSize = Dimens.sp22,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = styleLabel,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = Dimens.dp6),
        )
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp24),
        )
        Text(
            text = stringResource(R.string.applying_icon_style_description),
            color = Muted,
            fontSize = Dimens.sp12,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.dp14),
        )
    }
}
