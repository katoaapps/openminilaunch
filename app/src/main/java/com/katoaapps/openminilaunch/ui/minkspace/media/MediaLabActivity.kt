package com.katoaapps.openminilaunch.ui.minkspace.media

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.media.MediaActions
import com.katoaapps.openminilaunch.features.minkspace.media.MediaItem
import com.katoaapps.openminilaunch.features.minkspace.media.MediaRepository
import com.katoaapps.openminilaunch.ui.components.PageHeader
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme
import com.katoaapps.openminilaunch.ui.theme.Muted

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
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (repository.items.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Dimens.dp240)
                    .padding(horizontal = Dimens.dp16, vertical = Dimens.dp8),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp6),
            ) {
                itemsIndexed(repository.items, key = { _, item -> item.uri.toString() }) { index, item ->
                    MediaLibraryRow(
                        item = item,
                        first = index == 0,
                        last = index == repository.items.lastIndex,
                        onMove = { repository.move(item, it) },
                        onRemove = { pendingRemoval = item },
                    )
                }
            }
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

@Composable
private fun MediaLibraryRow(
    item: MediaItem,
    first: Boolean,
    last: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(Dimens.dp14),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.isVideo) stringResource(R.string.video) else stringResource(R.string.image_or_gif),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = { onMove(-1) }, enabled = !first) {
                Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up))
            }
            IconButton(onClick = { onMove(1) }, enabled = !last) {
                Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, stringResource(R.string.remove))
            }
        }
    }
}
