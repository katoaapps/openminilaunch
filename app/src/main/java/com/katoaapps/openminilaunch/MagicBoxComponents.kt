package com.katoaapps.openminilaunch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class FileResultGroup(val labelRes: Int) {
    PHOTOS(R.string.file_group_photos),
    VIDEOS(R.string.file_group_videos),
    DOCUMENTS(R.string.file_group_documents),
    AUDIO(R.string.file_group_audio),
}

internal fun fileResultGroup(result: FileSearchResult): FileResultGroup = when {
    result.mimeType.startsWith("image/") -> FileResultGroup.PHOTOS
    result.mimeType.startsWith("video/") -> FileResultGroup.VIDEOS
    result.mimeType.startsWith("audio/") -> FileResultGroup.AUDIO
    else -> FileResultGroup.DOCUMENTS
}

internal fun fileResultIcon(result: FileSearchResult): ImageVector = when {
    result.mimeType.startsWith("image/") -> Icons.Default.Image
    result.mimeType.startsWith("video/") -> Icons.Default.Movie
    result.mimeType.startsWith("audio/") -> Icons.Default.AudioFile
    result.mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
internal fun FileResultsGrid(
    results: List<FileSearchResult>,
    repository: FileSearchRepository,
    onOpen: (FileSearchResult) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
            .clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = .96f))
            .padding(horizontal = 9.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        FileResultGroup.entries.forEach { group ->
            val groupedResults = results.filter { fileResultGroup(it) == group }
            if (groupedResults.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "header_${group.name}") {
                    Text(
                        stringResource(group.labelRes),
                        color = Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(groupedResults, key = { it.uri.toString() }) { file ->
                    FileResultTile(file, repository) { onOpen(file) }
                }
            }
        }
    }
}

@Composable
internal fun FileResultTile(file: FileSearchResult, repository: FileSearchRepository, onClick: () -> Unit) {
    val thumbnail by produceState<android.graphics.Bitmap?>(null, file.uri, file.modifiedAt) {
        value = withContext(Dispatchers.IO) { repository.loadThumbnail(file) }
    }
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(fileResultIcon(file), null, Modifier.size(34.dp), tint = Rust)
            }
            if (fileResultGroup(file) == FileResultGroup.VIDEOS) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = .62f)) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.padding(5.dp).size(20.dp), tint = Color.White)
                }
            }
        }
        Text(
            file.name,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
internal fun SearchHistoryList(
    queries: List<String>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.recent_searches),
                    Modifier.weight(1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Muted,
                )
                TextButton(onClick = onClearAll) { Text(stringResource(R.string.clear_all)) }
            }
            queries.forEach { query ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(query) }
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.History, null, Modifier.size(20.dp), tint = Muted)
                    Text(query, Modifier.weight(1f).padding(horizontal = 12.dp), maxLines = 1)
                    IconButton(onClick = { onDelete(query) }) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.delete_search_history_entry, query),
                            tint = Muted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private data class MagicCommand(val key: Char, val labelRes: Int, val color: Color)

@Composable
internal fun MagicBoxLegend(activePrefix: Char?, enabled: Boolean = true, onSelect: (Char) -> Unit) {
    val commands = listOf(
        MagicCommand('@', R.string.command_text, Color(0xFF2563EB)),
        MagicCommand('#', R.string.command_call, Color(0xFF198754)),
        MagicCommand('-', R.string.command_todo, Color(0xFFD6A300)),
        MagicCommand('$', R.string.command_note, Color(0xFF7C3AED)),
        MagicCommand('+', R.string.command_event, Color(0xFF8B5A2B)),
        MagicCommand('?', R.string.command_app, Color(0xFFC62828)),
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.hot_keys),
                    Modifier.weight(1f),
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(stringResource(R.string.just_type_to_search), color = Muted, fontSize = 11.sp)
            }
            commands.chunked(3).forEach { rowCommands ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    rowCommands.forEach { command ->
                        val active = activePrefix == command.key
                        val activeContentColor = if (command.key == '-') LightInk else Color.White
                        Surface(
                            onClick = { if (enabled) onSelect(command.key) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) command.color else MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = (if (active) activeContentColor else MaterialTheme.colorScheme.onSurface)
                                .copy(alpha = if (enabled) 1f else .45f),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    command.key.toString(),
                                    fontWeight = FontWeight.Black,
                                    color = if (active) activeContentColor else command.color,
                                )
                                Text(
                                    stringResource(command.labelRes),
                                    Modifier.padding(start = 6.dp),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommandChip(label: String, color: Color, contentColor: Color, onClear: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(label, maxLines = 1) },
        trailingIcon = { Icon(Icons.Default.Close, stringResource(R.string.clear_command), Modifier.size(16.dp)) },
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = color,
            selectedLabelColor = contentColor,
            selectedTrailingIconColor = contentColor,
        ),
        modifier = Modifier.widthIn(max = 145.dp),
    )
}

@Composable
internal fun SuggestionRow(
    text: String,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) leadingContent() else icon?.let { Icon(it, null, Modifier.size(20.dp)) }
        Text(text, Modifier.padding(start = 10.dp), maxLines = 1, fontSize = 14.sp)
    }
}

@Composable
internal fun SearchDestinationButton(
    label: String,
    detail: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(21.dp))
            Column(Modifier.padding(start = 9.dp)) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(detail, color = Muted, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
