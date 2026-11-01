package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.AndroidContactPhone
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun FavoritePhonePickerDialog(
    contact: FavoriteAndroidContact,
    phones: List<AndroidContactPhone>,
    action: FavoriteContactAction,
    onPhone: (AndroidContactPhone) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            modifier = Modifier.minkDialogWidth().fillMaxHeight(.82f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp16)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.choose_phone_number),
                            fontWeight = FontWeight.Black,
                            fontSize = Dimens.sp20,
                        )
                        Text(contact.displayName, color = Muted, fontSize = Dimens.sp13)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(top = Dimens.dp12),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                ) {
                    items(phones, key = AndroidContactPhone::normalizedNumber) { phone ->
                        Surface(
                            onClick = { onPhone(phone) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.dp16),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(Dimens.dp14),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (action == FavoriteContactAction.MESSAGE) {
                                        Icons.AutoMirrored.Filled.Message
                                    } else {
                                        Icons.Default.Phone
                                    },
                                    contentDescription = null,
                                )
                                Column(Modifier.padding(start = Dimens.dp12)) {
                                    Text(phone.rawNumber, fontWeight = FontWeight.Bold)
                                    Text(phone.label, color = Muted, fontSize = Dimens.sp12)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
