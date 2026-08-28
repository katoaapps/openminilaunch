package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.demo.DemoHomeProfile
import com.katoaapps.openminilaunch.ui.components.MinkDialogDefaults
import com.katoaapps.openminilaunch.ui.components.minkDialogWidth
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog

@Composable
internal fun DemoHomeProfilePicker(
    selected: DemoHomeProfile,
    onSelect: (DemoHomeProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = MinkDialogDefaults.properties) {
        Surface(
            modifier = Modifier.minkDialogWidth().fillMaxHeight(.86f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp14)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.demo_home_profile),
                            fontWeight = FontWeight.Black,
                            fontSize = Dimens.sp20,
                        )
                        Text(
                            stringResource(R.string.demo_home_profile_picker_description),
                            color = Muted,
                            fontSize = Dimens.sp12,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp10),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
                ) {
                    items(DemoHomeProfile.entries, key = DemoHomeProfile::name) { profile ->
                        DemoHomeProfileRow(
                            profile = profile,
                            selected = profile == selected,
                            onClick = {
                                onSelect(profile)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoHomeProfileRow(
    profile: DemoHomeProfile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16)).clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.dp16),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.padding(end = Dimens.dp12),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp4),
            ) {
                Box(
                    Modifier.size(Dimens.dp24).clip(CircleShape)
                        .background(colorResource(profile.backgroundColorRes)),
                )
                Box(
                    Modifier.size(Dimens.dp24).clip(CircleShape)
                        .background(colorResource(profile.panelColorRes)),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(profile.labelRes), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(profile.descriptionRes),
                    color = Muted,
                    fontSize = Dimens.sp12,
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
