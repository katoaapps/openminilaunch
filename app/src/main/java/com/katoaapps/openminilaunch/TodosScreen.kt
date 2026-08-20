@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun TodosScreen(store: LauncherStore, actions: DeviceActions, goBack: () -> Unit) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val todoListTitle = stringResource(R.string.todo_export_title, appName)
    var newText by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<TodoItem?>(null) }
    var deleting by remember { mutableStateOf<TodoItem?>(null) }
    var showExportOptions by remember { mutableStateOf(false) }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            val saved = TodoPdfExporter.write(context, uri, store.todos.toList())
            Toast.makeText(
                context,
                if (saved) context.getString(R.string.todo_pdf_saved) else context.getString(R.string.todo_export_failed),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val listState = rememberLazyListState()
    var draggedTodoId by remember { mutableStateOf<String?>(null) }
    val visibleTodos = remember { mutableStateListOf<TodoItem>().apply { addAll(store.todos) } }
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
    ) { from, to ->
        visibleTodos.add(to.index, visibleTodos.removeAt(from.index))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    LaunchedEffect(store.todos.toList(), draggedTodoId) {
        if (draggedTodoId == null && visibleTodos != store.todos) {
            visibleTodos.clear()
            visibleTodos.addAll(store.todos)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageHeader(stringResource(R.string.todo_page_title), goBack) {
            IconButton(
                onClick = { showExportOptions = true },
                enabled = store.todos.isNotEmpty(),
            ) {
                Icon(Icons.Default.IosShare, stringResource(R.string.export_todo_list))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.dp22, vertical = Dimens.dp8), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                placeholder = { Text(stringResource(R.string.add_todo)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(Dimens.dp16),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { store.addTodo(newText); newText = "" }),
            )
            IconButton(onClick = { store.addTodo(newText); newText = "" }, enabled = newText.isNotBlank()) {
                Icon(Icons.Default.AddCircle, stringResource(R.string.add), tint = Rust, modifier = Modifier.size(Dimens.dp32))
            }
        }
        if (store.todos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.nothing_here_yet), color = Muted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = Dimens.dp14, vertical = Dimens.dp8),
            ) {
                itemsIndexed(visibleTodos, key = { _, item -> item.id }) { _, item ->
                    ReorderableItem(reorderableState, key = item.id) { isDragging ->
                        val elevation by animateFloatAsState(
                            targetValue = if (isDragging) 12f else 0f,
                            label = "todo drag elevation",
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.025f else 1f,
                            label = "todo drag scale",
                        )
                        val jiggle = if (isDragging) {
                            val jiggleTransition = rememberInfiniteTransition(label = "todo drag jiggle")
                            jiggleTransition.animateFloat(
                                initialValue = -0.7f,
                                targetValue = 0.7f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(95),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                                label = "todo drag rotation",
                            ).value
                        } else {
                            0f
                        }
                        val interactionSource = remember { MutableInteractionSource() }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.dp3)
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = jiggle
                                    shadowElevation = elevation
                                }
                                .clip(RoundedCornerShape(Dimens.dp16))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(Dimens.dp8),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = item.completed, onCheckedChange = { store.toggleTodo(item.id) })
                            Text(
                                item.text,
                                Modifier.weight(1f).clickable { editing = item },
                                textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                                color = if (item.completed) Muted else MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = stringResource(R.string.hold_drag_reorder),
                                tint = Muted,
                                modifier = Modifier
                                    .size(Dimens.dp44)
                                    .longPressDraggableHandle(
                                        interactionSource = interactionSource,
                                        onDragStarted = {
                                            draggedTodoId = item.id
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragStopped = {
                                            store.setTodoOrder(visibleTodos.map(TodoItem::id))
                                            draggedTodoId = null
                                        },
                                    )
                                    .padding(Dimens.dp10),
                            )
                            IconButton(onClick = { deleting = item }) {
                                Icon(Icons.Default.DeleteOutline, stringResource(R.string.delete), tint = Rust)
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { item ->
        var editText by remember(item.id) { mutableStateOf(item.text) }
        AlertDialog(
            modifier = Modifier.fillMaxWidth(.94f),
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.edit_todo)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.dp160, max = Dimens.dp280),
                    minLines = 5,
                    maxLines = 10,
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                )
            },
            confirmButton = { TextButton(onClick = { if (editText.isNotBlank()) store.renameTodo(item.id, editText); editing = null }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            icon = { Icon(Icons.Default.DeleteOutline, null, tint = Rust) },
            title = { Text(stringResource(R.string.delete_todo_title)) },
            text = { Text(stringResource(R.string.delete_todo_description, item.text)) },
            confirmButton = {
                TextButton(onClick = { store.deleteTodo(item.id); deleting = null }) {
                    Text(stringResource(R.string.delete), color = Rust)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showExportOptions) {
        AlertDialog(
            onDismissRequest = { showExportOptions = false },
            icon = { Icon(Icons.Default.IosShare, null) },
            title = { Text(stringResource(R.string.export_todo_list)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                    Text(stringResource(R.string.export_todo_list_description))
                    FilledTonalButton(
                        onClick = {
                            showExportOptions = false
                            if (!actions.exportTodosToNotes(formatTodoExport(todoListTitle, store.todos))) {
                                Toast.makeText(context, R.string.todo_export_failed, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, null)
                        Spacer(Modifier.width(Dimens.dp8))
                        Text(stringResource(R.string.send_to_notes_app))
                    }
                    FilledTonalButton(
                        onClick = {
                            showExportOptions = false
                            val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            pdfLauncher.launch(context.getString(R.string.todo_export_filename, date))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null)
                        Spacer(Modifier.width(Dimens.dp8))
                        Text(stringResource(R.string.save_as_pdf))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportOptions = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
internal fun PageHeader(title: String, goBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.dp10, vertical = Dimens.dp8), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        Text(
            title,
            Modifier.weight(1f),
            fontSize = Dimens.sp28,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        action?.invoke()
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, fontSize = Dimens.sp12, fontWeight = FontWeight.Black, letterSpacing = Dimens.sp1_4, color = Rust, modifier = Modifier.padding(top = Dimens.dp10))
}

@Composable
internal fun SettingsRow(title: String, subtitle: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16)).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(enabled = enabled, onClick = onClick).padding(Dimens.dp14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = Dimens.sp12, maxLines = 1)
        }
        Icon(icon, null, tint = Muted)
    }
}
