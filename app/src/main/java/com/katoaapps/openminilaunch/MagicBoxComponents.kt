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
        modifier = Modifier.fillMaxWidth().heightIn(max = Dimens.dp240)
            .clip(RoundedCornerShape(Dimens.dp18)).background(MaterialTheme.colorScheme.surface.copy(alpha = .96f))
            .padding(horizontal = Dimens.dp9),
        contentPadding = PaddingValues(vertical = Dimens.dp8),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp9),
    ) {
        FileResultGroup.entries.forEach { group ->
            val groupedResults = results.filter { fileResultGroup(it) == group }
            if (groupedResults.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "header_${group.name}") {
                    Text(
                        stringResource(group.labelRes),
                        color = Muted,
                        fontSize = Dimens.sp10,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = Dimens.sp1_2,
                        modifier = Modifier.padding(top = Dimens.dp4),
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
        Modifier.clip(RoundedCornerShape(Dimens.dp12)).clickable(onClick = onClick).padding(Dimens.dp3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(Dimens.dp10))
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
                Icon(fileResultIcon(file), null, Modifier.size(Dimens.dp34), tint = Rust)
            }
            if (fileResultGroup(file) == FileResultGroup.VIDEOS) {
                Surface(shape = CircleShape, color = MinkBlack.copy(alpha = .62f)) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.padding(Dimens.dp5).size(Dimens.dp20), tint = MinkWhite)
                }
            }
        }
        Text(
            file.name,
            fontSize = Dimens.sp10,
            lineHeight = Dimens.sp12,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp4),
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
        shape = RoundedCornerShape(Dimens.dp18),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Dimens.dp6,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.dp16, end = Dimens.dp8, top = Dimens.dp8, bottom = Dimens.dp4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.recent_searches),
                    Modifier.weight(1f),
                    fontSize = Dimens.sp12,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = Dimens.sp1,
                    color = Muted,
                )
                TextButton(onClick = onClearAll) { Text(stringResource(R.string.clear_all)) }
            }
            queries.forEach { query ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(query) }
                        .padding(start = Dimens.dp16, end = Dimens.dp4, top = Dimens.dp4, bottom = Dimens.dp4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.History, null, Modifier.size(Dimens.dp20), tint = Muted)
                    Text(query, Modifier.weight(1f).padding(horizontal = Dimens.dp12), maxLines = 1)
                    IconButton(onClick = { onDelete(query) }) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.delete_search_history_entry, query),
                            tint = Muted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Dimens.dp4))
        }
    }
}

private data class MagicCommand(val key: Char, val labelRes: Int, val color: Color)

@Composable
internal fun MagicBoxLegend(activePrefix: Char?, enabled: Boolean = true, onSelect: (Char) -> Unit) {
    val commands = listOf(
        MagicCommand('@', R.string.command_text, MagicTextColor),
        MagicCommand('#', R.string.command_call, MagicCallColor),
        MagicCommand('-', R.string.command_todo, MagicTodoColor),
        MagicCommand('$', R.string.command_note, MagicNoteColor),
        MagicCommand('+', R.string.command_event, MagicEventColor),
        MagicCommand('?', R.string.command_app, MagicAppColor),
    )
    Surface(
        shape = RoundedCornerShape(Dimens.dp18),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        shadowElevation = Dimens.dp8,
    ) {
        Column(Modifier.fillMaxWidth().padding(Dimens.dp10), verticalArrangement = Arrangement.spacedBy(Dimens.dp7)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.hot_keys),
                    Modifier.weight(1f),
                    color = Muted,
                    fontSize = Dimens.sp10,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = Dimens.sp1_2,
                )
                Text(stringResource(R.string.just_type_to_search), color = Muted, fontSize = Dimens.sp11)
            }
            commands.chunked(3).forEach { rowCommands ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp7)) {
                    rowCommands.forEach { command ->
                        val active = activePrefix == command.key
                        val activeContentColor = if (command.key == '-') LightInk else MinkWhite
                        Surface(
                            onClick = { if (enabled) onSelect(command.key) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Dimens.dp12),
                            color = if (active) command.color else MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = (if (active) activeContentColor else MaterialTheme.colorScheme.onSurface)
                                .copy(alpha = if (enabled) 1f else .45f),
                        ) {
                            Row(
                                Modifier.padding(horizontal = Dimens.dp9, vertical = Dimens.dp7),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    command.key.toString(),
                                    fontWeight = FontWeight.Black,
                                    color = if (active) activeContentColor else command.color,
                                )
                                Text(
                                    stringResource(command.labelRes),
                                    Modifier.padding(start = Dimens.dp6),
                                    fontSize = Dimens.sp12,
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
        trailingIcon = { Icon(Icons.Default.Close, stringResource(R.string.clear_command), Modifier.size(Dimens.dp16)) },
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = color,
            selectedLabelColor = contentColor,
            selectedTrailingIconColor = contentColor,
        ),
        modifier = Modifier.widthIn(max = Dimens.dp145),
    )
}

@Composable
internal fun SuggestionRow(
    text: String,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp14))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick).padding(Dimens.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) leadingContent() else icon?.let {
            Icon(it, null, Modifier.size(Dimens.dp20), tint = contentColor)
        }
        Text(text, Modifier.padding(start = Dimens.dp10), color = contentColor, maxLines = 1, fontSize = Dimens.sp14)
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
        shape = RoundedCornerShape(Dimens.dp14),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.padding(Dimens.dp12), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(Dimens.dp21))
            Column(Modifier.padding(start = Dimens.dp9)) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = Dimens.sp14)
                Text(detail, color = Muted, fontSize = Dimens.sp10, maxLines = 1)
            }
        }
    }
}
