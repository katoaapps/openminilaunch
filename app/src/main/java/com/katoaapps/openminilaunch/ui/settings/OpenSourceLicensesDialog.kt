package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens

private val SABLE_LICENSE_ASSETS = listOf(
    "licenses/sable-games/NOTICE.txt",
    "licenses/sable-games/THIRD_PARTY_NOTICES.txt",
    "licenses/sable-games/LICENSE.txt",
)

@Composable
internal fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenseText = remember(context) {
        SABLE_LICENSE_ASSETS.joinToString(separator = "\n\n────────────────────\n\n") { path ->
            context.assets.open(path).bufferedReader().use { it.readText() }.trim()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            modifier = Modifier.minkDialogWidth().fillMaxHeight(.92f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Column(Modifier.padding(Dimens.dp16)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.licenses_and_attribution),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Black,
                        fontSize = Dimens.sp18,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                SelectionContainer(Modifier.weight(1f)) {
                    Text(
                        text = licenseText,
                        modifier = Modifier.fillMaxWidth().verticalScroll(
                            rememberScrollState(),
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .82f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = Dimens.sp12,
                    )
                }
            }
        }
    }
}
