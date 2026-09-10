package com.katoaapps.openminilaunch.ui.magic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.zIndex
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.magic.printableHardwareText
import com.katoaapps.openminilaunch.model.ContactResult
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun MagicEditorSurface(
    noteMode: Boolean,
    text: TextFieldValue,
    selectedContact: ContactResult?,
    prefix: Char?,
    actionVisuals: MagicActionVisuals,
    actionContentColor: Color,
    focusRequester: FocusRequester,
    interactionSource: MutableInteractionSource,
    onRefocus: () -> Unit,
    onTextChange: (TextFieldValue) -> Unit,
    onPlaced: () -> Unit,
    onSubmit: () -> Unit,
    onHardwareKeyUp: (Int) -> Unit,
    onClearMessage: () -> Unit,
    onDeleteNote: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onRefocus)
            .graphicsLayer { alpha = if (visible) 1f else 0f },
        shape = RoundedCornerShape(Dimens.dp24),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Dimens.dp12,
    ) {
        Box(if (noteMode) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
            Row(
                modifier = if (noteMode) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth().padding(start = Dimens.dp12, end = Dimens.dp6)
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!noteMode) {
                    selectedContact?.let { contact ->
                        CommandChip(
                            stringResource(R.string.two_part_label, contact.name, contact.phoneLabel),
                            actionVisuals.color,
                            actionContentColor,
                            onClear = onClearMessage,
                        )
                    }
                }
                MagicInputField(
                    value = text,
                    onValueChange = onTextChange,
                    prefix = prefix,
                    focusRequester = focusRequester,
                    onPlaced = onPlaced,
                    onSubmit = onSubmit,
                    onHardwareKeyUp = onHardwareKeyUp,
                    modifier = Modifier.weight(1f).then(
                        if (noteMode) {
                            Modifier.fillMaxHeight().padding(end = Dimens.dp48, bottom = Dimens.dp54)
                        } else {
                            Modifier
                        },
                    ),
                    maxLines = if (noteMode) Int.MAX_VALUE else 5,
                )
                if (!noteMode) {
                    FilledIconButton(
                        onClick = onSubmit,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = actionVisuals.color,
                            contentColor = actionContentColor,
                        ),
                    ) { Icon(actionVisuals.icon, stringResource(R.string.run_command)) }
                }
            }
            if (noteMode) {
                IconButton(
                    onClick = onDeleteNote,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.dp6),
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        stringResource(R.string.delete_note_draft),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                FilledIconButton(
                    onClick = onSubmit,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.dp10),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = actionVisuals.color,
                        contentColor = actionContentColor,
                    ),
                ) { Icon(actionVisuals.icon, stringResource(R.string.run_command)) }
            }
        }
    }
}

@Composable
internal fun CollapsedMagicBar(
    modifier: Modifier,
    minimumHeight: androidx.compose.ui.unit.Dp,
    armedFocusRequester: FocusRequester,
    onArmedPlaced: () -> Unit,
    onPrintableKeyDown: (String, Int) -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier
            .focusRequester(armedFocusRequester)
            .onGloballyPositioned { onArmedPlaced() }
            .onPreviewKeyEvent { event ->
                val typedText = printableHardwareText(event.nativeKeyEvent.unicodeChar)
                if (typedText == null) {
                    false
                } else {
                    if (event.type == KeyEventType.KeyDown) {
                        onPrintableKeyDown(typedText, event.nativeKeyEvent.keyCode)
                    }
                    true
                }
            }
            .focusable()
            .clip(RoundedCornerShape(Dimens.dp22))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onOpen)
            .heightIn(min = minimumHeight)
            .padding(start = Dimens.dp18, end = Dimens.dp6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.magic_box_collapsed_hint),
            Modifier.weight(1f),
            color = Muted,
            fontSize = Dimens.sp14,
        )
        FilledIconButton(
            onClick = onOpen,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Default.Keyboard, stringResource(R.string.open_magic_box))
        }
    }
}

@Composable
internal fun SmsSentConfirmation(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.zIndex(30f),
        enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = .88f),
        exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = .94f),
    ) {
        Surface(
            shape = RoundedCornerShape(Dimens.dp28),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = Dimens.dp16,
        ) {
            Row(
                Modifier.padding(horizontal = Dimens.dp28, vertical = Dimens.dp20),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
            ) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(Dimens.dp30))
                Column {
                    Text(
                        stringResource(R.string.message_sent),
                        fontSize = Dimens.sp19,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.sent_as_sms),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f),
                        fontSize = Dimens.sp13,
                    )
                }
            }
        }
    }
}
