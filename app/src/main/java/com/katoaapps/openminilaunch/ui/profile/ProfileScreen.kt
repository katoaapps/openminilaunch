package com.katoaapps.openminilaunch.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.features.profile.ProfilePortraitProcessor
import com.katoaapps.openminilaunch.features.profile.ProfileQrCode
import com.katoaapps.openminilaunch.features.profile.ProfileState
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ProfileScreen(store: LauncherStore, goBack: () -> Unit) {
    val repository = store.profileRepository
    val state = repository.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editingLink by remember { mutableStateOf<ProfileLink?>(null) }
    var addingLink by remember { mutableStateOf(false) }
    var qrDialog by remember { mutableStateOf<ProfileQrCode?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val portraitPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val bitmap = ProfilePortraitProcessor.decode(context, uri)
                        try {
                            repository.savePortrait(bitmap)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
                if (result.isFailure) {
                    Toast.makeText(context, R.string.profile_photo_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val pickPortrait = {
        portraitPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = Dimens.dp720).fillMaxWidth().fillMaxHeight().statusBarsPadding(),
        ) {
            PageHeader(stringResource(R.string.profile), goBack)
            when (state) {
                ProfileState.Empty -> VCardPrivacyIntro(
                    onContinue = { repository.saveIdentity("", "", "", "") },
                )
                is ProfileState.Ready -> ProfileEditorContent(
                    store = store,
                    card = state.card,
                    links = state.links,
                    onPickPortrait = pickPortrait,
                    onAddLink = { addingLink = true },
                    onEditLink = { editingLink = it },
                    onShowQr = { qrDialog = it },
                    onReset = { confirmReset = true },
                )
                is ProfileState.Invalid -> UnreadableProfile(onReset = { confirmReset = true })
                is ProfileState.Unreadable -> UnreadableProfile(onReset = { confirmReset = true })
            }
        }
    }

    if (addingLink || editingLink != null) {
        ProfileLinkDialog(
            existing = editingLink,
            onDismiss = { addingLink = false; editingLink = null },
            onSave = { existing, type, label, value ->
                repository.upsertLink(existing?.id, type, label, value)
                addingLink = false
                editingLink = null
            },
            onDelete = editingLink?.let { link ->
                { repository.deleteLink(link.id); editingLink = null }
            },
        )
    }

    qrDialog?.let { code ->
        val name = (repository.state as? ProfileState.Ready)?.card?.fullName.orEmpty()
        ProfileQrDialog(code, name, onDismiss = { qrDialog = null })
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_profile)) },
            text = { Text(stringResource(R.string.reset_profile_description)) },
            confirmButton = {
                TextButton(onClick = { repository.reset(); confirmReset = false }) {
                    Text(stringResource(R.string.reset), color = Rust)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun UnreadableProfile(onReset: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(Dimens.dp24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.RestartAlt, null, Modifier.size(Dimens.dp64), tint = Rust)
        Text(stringResource(R.string.profile_could_not_be_opened), fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.profile_encryption_recovery),
            color = Muted,
            modifier = Modifier.padding(top = Dimens.dp8),
        )
        FilledTonalButton(onClick = onReset, modifier = Modifier.padding(top = Dimens.dp18)) {
            Text(stringResource(R.string.reset_profile))
        }
    }
}
