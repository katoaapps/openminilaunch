package com.katoaapps.openminilaunch.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.profile.ProfileCard
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.features.profile.ProfileQrCode
import com.katoaapps.openminilaunch.features.profile.ProfileQrDensity
import com.katoaapps.openminilaunch.features.profile.ProfileQrGenerator
import com.katoaapps.openminilaunch.features.profile.resolveSelectedLinks
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import kotlinx.coroutines.delay

@Composable
internal fun ProfileEditorContent(
    store: LauncherStore,
    card: ProfileCard,
    links: List<ProfileLink>,
    onPickPortrait: () -> Unit,
    onAddLink: () -> Unit,
    onEditLink: (ProfileLink) -> Unit,
    onShowQr: (ProfileQrCode) -> Unit,
    onReset: () -> Unit,
) {
    val repository = store.profileRepository
    val context = LocalContext.current
    var fullName by rememberSaveable(card.fullName) { mutableStateOf(card.fullName) }
    var organization by rememberSaveable(card.organization) { mutableStateOf(card.organization) }
    var jobTitle by rememberSaveable(card.jobTitle) { mutableStateOf(card.jobTitle) }
    var note by rememberSaveable(card.note) { mutableStateOf(card.note) }
    var savedFeedback by remember { mutableStateOf(false) }
    val draftCard = card.copy(
        fullName = fullName,
        organization = organization,
        jobTitle = jobTitle,
        note = note,
    )
    val identityChanged = fullName != card.fullName || organization != card.organization ||
        jobTitle != card.jobTitle || note != card.note
    val selected = draftCard.resolveSelectedLinks(links)
    val available = links.filterNot { it.id in draftCard.selectedLinkIds }
    val qrResult = remember(draftCard, links) { ProfileQrGenerator.describe(draftCard, links) }

    LaunchedEffect(savedFeedback) {
        if (savedFeedback) {
            delay(SAVE_FEEDBACK_MILLIS)
            savedFeedback = false
        }
    }

    fun saveIdentity() {
        repository.saveIdentity(fullName, organization, jobTitle, note)
        savedFeedback = true
        Toast.makeText(context, R.string.vcard_saved, Toast.LENGTH_SHORT).show()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.dp22, vertical = Dimens.dp8),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
    ) {
        ProfileIdentitySection(
            store = store,
            draft = draftCard,
            selectedLinks = selected,
            onPickPortrait = onPickPortrait,
            onFullNameChange = { fullName = it; savedFeedback = false },
            onOrganizationChange = { organization = it; savedFeedback = false },
            onJobTitleChange = { jobTitle = it; savedFeedback = false },
            onNoteChange = { note = it; savedFeedback = false },
        )

        SectionLabel(stringResource(R.string.selected_profile_links))
        if (selected.isEmpty()) {
            Text(stringResource(R.string.no_profile_links_selected), color = Muted)
        } else {
            SelectedProfileLinks(store, selected, onEditLink)
        }
        if (available.isNotEmpty()) {
            SectionLabel(stringResource(R.string.available_profile_links))
            available.forEach { link ->
                ProfileLinkRow(
                    link = link,
                    selected = false,
                    onToggle = { repository.setLinkSelected(link.id, true) },
                    onEdit = { onEditLink(link) },
                )
            }
        }
        OutlinedButton(onClick = onAddLink, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AddLink, null)
            Text(stringResource(R.string.add_profile_link), Modifier.padding(start = Dimens.dp8))
        }

        qrResult?.takeIf { it.density == ProfileQrDensity.DENSE }?.let {
            Text(stringResource(R.string.profile_qr_dense_warning), color = Rust, fontSize = Dimens.sp12)
        }
        val qrTooDense = qrResult?.density == ProfileQrDensity.TOO_DENSE
        if (qrTooDense) {
            Text(stringResource(R.string.profile_qr_too_dense), color = MaterialTheme.colorScheme.error)
        } else if (qrResult == null) {
            Text(stringResource(R.string.profile_qr_not_ready), color = MaterialTheme.colorScheme.error)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp10),
        ) {
            Button(
                onClick = ::saveIdentity,
                enabled = identityChanged || !savedFeedback,
                modifier = Modifier.weight(1f),
            ) {
                if (savedFeedback) Icon(Icons.Default.Check, null)
                Text(
                    stringResource(if (savedFeedback) R.string.vcard_saved else R.string.save_profile),
                    Modifier.padding(start = if (savedFeedback) Dimens.dp6 else Dimens.dp0),
                )
            }
            Button(
                onClick = {
                    if (identityChanged) saveIdentity()
                    qrResult?.let(onShowQr)
                },
                enabled = qrResult != null && !qrTooDense,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.QrCode2, null)
                Text(stringResource(R.string.qr_code), Modifier.padding(start = Dimens.dp6))
            }
        }
        TextButton(
            onClick = onReset,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = Dimens.dp32),
        ) {
            Text(stringResource(R.string.reset_profile), color = Rust)
        }
    }
}

private const val SAVE_FEEDBACK_MILLIS = 1_500L
