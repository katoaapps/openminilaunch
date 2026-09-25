package com.katoaapps.openminilaunch.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun VCardPrivacyIntro(onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(Dimens.dp24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.vcard_private_title),
            modifier = Modifier.padding(top = Dimens.dp16),
            fontWeight = FontWeight.Bold,
            fontSize = Dimens.sp22,
        )
        Text(
            stringResource(R.string.vcard_private_description),
            modifier = Modifier.padding(top = Dimens.dp10),
            color = Muted,
        )
        Text(
            stringResource(R.string.vcard_no_account_description),
            modifier = Modifier.padding(top = Dimens.dp8),
            color = Muted,
        )
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp24),
        ) {
            Text(stringResource(R.string.continue_label))
        }
    }
}
