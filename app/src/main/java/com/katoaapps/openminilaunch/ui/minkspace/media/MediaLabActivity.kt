package com.katoaapps.openminilaunch.ui.minkspace.media

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.media.MediaActions
import com.katoaapps.openminilaunch.features.minkspace.media.MediaItem
import com.katoaapps.openminilaunch.features.minkspace.media.MediaRepository
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme

class MediaLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = LauncherStore.get(this)
        setContent {
            MinkLauncherTheme(store) {
                MediaLabScreen(onClose = ::finish)
            }
        }
    }
}
@Composable
private fun MediaLabScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { MediaRepository.get(context) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        repository.add(it)
    }
    var pendingRemoval by remember { mutableStateOf<MediaItem?>(null) }
    var focusedItemKey by rememberSaveable {
        mutableStateOf(repository.items.firstOrNull()?.uri?.toString())
    }

    LaunchedEffect(repository.items.toList()) {
        if (repository.items.none { it.uri.toString() == focusedItemKey }) {
            focusedItemKey = repository.items.firstOrNull()?.uri?.toString()
        }
    }

    fun open(item: MediaItem) {
        if (!MediaActions.open(context, item)) {
            Toast.makeText(context, R.string.media_open_failed, Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = stringResource(R.string.media_lab),
            goBack = onClose,
            action = {
                Button(onClick = { picker.launch(arrayOf("image/*", "video/*")) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.add_media), Modifier.padding(start = Dimens.dp6))
                }
            },
        )
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Dimens.dp16),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(Dimens.dp28),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                MediaMiniApp(
                    items = repository.items,
                    onOpenItem = ::open,
                    focusedItemKey = focusedItemKey,
                    onFocusedItemChange = { focusedItemKey = it.uri.toString() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (repository.items.isNotEmpty()) {
            ReorderableMediaLibrary(
                items = repository.items,
                focusedItemKey = focusedItemKey,
                onFocus = { focusedItemKey = it.uri.toString() },
                onOrderChange = repository::setOrder,
                onRemove = { pendingRemoval = it },
            )
        }
    }

    pendingRemoval?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.remove_media_title)) },
            text = { Text(stringResource(R.string.remove_media_description, item.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.remove(item)
                        pendingRemoval = null
                    },
                ) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
