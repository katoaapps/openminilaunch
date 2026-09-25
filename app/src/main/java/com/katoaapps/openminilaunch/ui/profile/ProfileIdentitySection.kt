package com.katoaapps.openminilaunch.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.profile.ProfileCard
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun ProfileIdentitySection(
    store: LauncherStore,
    draft: ProfileCard,
    selectedLinks: List<ProfileLink>,
    onPickPortrait: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onOrganizationChange: (String) -> Unit,
    onJobTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    var detailsExpanded by rememberSaveable {
        mutableStateOf(
            draft.organization.isNotBlank() || draft.jobTitle.isNotBlank() || draft.note.isNotBlank(),
        )
    }
    var confirmRemovePhoto by rememberSaveable { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(Dimens.dp24),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ProfileEditorPreview(store, draft, selectedLinks)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        OutlinedButton(onClick = onPickPortrait, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Image, null)
            Text(
                stringResource(if (draft.hasPortrait) R.string.change_photo else R.string.choose_photo),
                Modifier.padding(start = Dimens.dp6),
            )
        }
        if (draft.hasPortrait) {
            IconButton(onClick = { confirmRemovePhoto = true }) {
                Icon(Icons.Default.DeleteOutline, stringResource(R.string.remove_profile_photo))
            }
        }
    }

    SectionLabel(stringResource(R.string.profile_details))
    OutlinedTextField(
        draft.fullName,
        onFullNameChange,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.full_name)) },
        singleLine = true,
    )
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { detailsExpanded = !detailsExpanded },
        shape = RoundedCornerShape(Dimens.dp16),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.dp16, vertical = Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.more_details), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.more_details_summary), color = Muted, fontSize = Dimens.sp12)
            }
            Icon(
                if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
        }
    }
    AnimatedVisibility(detailsExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
            OutlinedTextField(
                draft.organization,
                onOrganizationChange,
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.organization)) },
                singleLine = true,
            )
            OutlinedTextField(
                draft.jobTitle,
                onJobTitleChange,
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.job_title)) },
                singleLine = true,
            )
            OutlinedTextField(
                draft.note,
                onNoteChange,
                Modifier.fillMaxWidth().heightIn(min = Dimens.dp96),
                label = { Text(stringResource(R.string.profile_note)) },
                minLines = 3,
            )
        }
    }

    if (confirmRemovePhoto) {
        AlertDialog(
            onDismissRequest = { confirmRemovePhoto = false },
            title = { Text(stringResource(R.string.remove_profile_photo)) },
            text = { Text(stringResource(R.string.remove_profile_photo_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    store.profileRepository.removePortrait()
                    confirmRemovePhoto = false
                }) {
                    Text(stringResource(R.string.remove), color = Rust)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemovePhoto = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
