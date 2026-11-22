package org.sableos.design

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sableos.design.DesignText.t

/*
 * Compose building blocks for DESIGN-KF-D part 2. The decisions live in the
 * pure policies (SableResponsive.kt, SableAlphabetIndex.kt,
 * SableMiniPlayerPolicy.kt); these composables only render them.
 *
 * NOTE: written against Compose 1.8-era APIs (compose-bom 2026.02.01) but not
 * compiled in the Q25 tree (no Google Maven there).
 */

/** Window metrics + font scale of the current configuration. */
@Composable
fun rememberSableLayoutMetrics(): SableLayoutMetrics {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    return remember(configuration.screenWidthDp, configuration.screenHeightDp, fontScale) {
        SableLayoutMetrics(configuration.screenWidthDp, configuration.screenHeightDp, fontScale)
    }
}

/**
 * Visible keyboard focus (VISIBLE_KEYBOARD_FOCUS=YES): a 2 dp accent outline
 * while focused. Hover never moves focus (POINTER_HOVER_STEALS_FOCUS=NO).
 */
fun Modifier.sableFocusRing(): Modifier =
    composed {
        var focused by remember { mutableStateOf(false) }
        val color = MaterialTheme.colorScheme.primary
        this
            .onFocusChanged { focused = it.hasFocus }
            .then(
                if (focused) {
                    Modifier.border(BorderStroke(2.dp, color), MaterialTheme.shapes.small)
                } else {
                    Modifier
                },
            )
    }

/**
 * Top-level destinations without clipping: at most three slots on compact or
 * square windows (two plus "more" when four or more exist), labels on one line,
 * and secondary destinations in a keyboard-reachable "more" menu.
 */
@Composable
fun SableAdaptiveTopNav(
    destinations: List<SableDestination>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val metrics = rememberSableLayoutMetrics()
    val style = MaterialTheme.typography.titleMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableDp = with(density) { constraints.maxWidth.toDp().value }
        val layout =
            remember(destinations, selectedId, metrics, availableDp, style) {
                SableNavigationPolicy.layout(
                    destinations = destinations,
                    selectedId = selectedId,
                    metrics = metrics,
                    availableWidthDp = availableDp,
                ) { label ->
                    with(density) { measurer.measure(label, style, maxLines = 1).size.width.toDp().value }
                }
            }
        var moreOpen by remember { mutableStateOf(false) }
        val overflowSelected = layout.overflow.any { it.id == selectedId }

        Row(modifier = Modifier.fillMaxWidth()) {
            layout.visible.forEach { destination ->
                SableNavTab(
                    label = destination.label,
                    selected = destination.id == selectedId,
                    accent = accent,
                    onClick = { onSelect(destination.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (layout.hasOverflow) {
                Box(modifier = Modifier.weight(1f)) {
                    SableNavTab(
                        label = SableNavigationPolicy.MORE_LABEL,
                        selected = overflowSelected,
                        accent = accent,
                        description =
                            t("sable_more_destinations", layout.overflow.joinToString(", ") { it.label }),
                        onClick = { moreOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        layout.overflow.forEach { destination ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = destination.label,
                                        color = if (destination.id == selectedId) accent else Color.Unspecified,
                                    )
                                },
                                onClick = {
                                    moreOpen = false
                                    onSelect(destination.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SableNavTab(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    description: String? = null,
) {
    val content =
        @Composable {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = SableSize.TouchTarget)
                        .padding(horizontal = SableSpacing.Sm, vertical = SableSpacing.Xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier =
                        Modifier
                            .padding(top = SableSpacing.Xs)
                            .fillMaxWidth(if (selected) 0.6f else 0.2f)
                            .height(if (selected) 3.dp else 1.dp)
                            .background(if (selected) accent else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    val tabModifier =
        modifier
            .sableFocusRing()
            .semantics {
                role = Role.Tab
                this.selected = selected
                this.contentDescription = description ?: label
            }
    Surface(
        onClick = onClick,
        modifier = tabModifier,
        color = Color.Transparent,
    ) {
        content()
    }
}

/**
 * Mini-player that always communicates state: primary text, a state label, a
 * labelled play/pause button and progress only when meaningful. Renders
 * nothing when [model] is null (no solid accent bar without meaning).
 */
@Composable
fun SableMiniPlayer(
    model: SableMiniPlayerModel?,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    menuActions: List<SableRowAction> = emptyList(),
    onMenuAction: (SableRowAction) -> Unit = {},
) {
    if (model == null) return
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        onClick = onOpen,
        modifier =
            modifier
                .fillMaxWidth()
                .sableFocusRing()
                .semantics { contentDescription = model.contentDescription },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = SableSpacing.Md, vertical = SableSpacing.Sm),
                horizontalArrangement = Arrangement.spacedBy(SableSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.primary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(model.stateLabel, model.secondary).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SableSquareAction(
                    text = if (model.toggle == SableMiniPlayerToggle.Pause) "❚❚" else "▶",
                    description = model.toggleLabel,
                    active = true,
                    onClick = onToggle,
                )
                if (menuActions.isNotEmpty()) {
                    Box {
                        SableSquareAction(text = "⋯", description = t("sable_more_actions"), onClick = { menuOpen = true })
                        SableActionMenu(menuOpen, menuActions, { menuOpen = false }, onMenuAction)
                    }
                }
            }
            val progress = model.progress
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
            }
        }
    }
}

/**
 * Dense list row: 1–2 line title, 1 line subtitle, at most one primary
 * trailing action on compact layouts and everything else in a "⋯" menu that is
 * also reachable with the Menu key. Minimum 48 dp, visible focus.
 */
@Composable
fun SableDenseRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: List<SableRowAction> = emptyList(),
    onAction: (SableRowAction) -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
) {
    val metrics = rememberSableLayoutMetrics()
    val split = remember(actions, metrics) { SableDenseRowPolicy.split(actions, metrics) }
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = SableSize.TouchTarget)
                .onPreviewKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    if (native.keyCode == AndroidKeyEvent.KEYCODE_MENU && split.overflow.isNotEmpty()) {
                        if (event.type == KeyEventType.KeyUp) menuOpen = true
                        true
                    } else {
                        false
                    }
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SableSpacing.Sm),
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = SableSize.TouchTarget)
                    .sableFocusRing()
                    .clickable(onClick = onClick)
                    .padding(vertical = SableSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SableSpacing.Md),
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = SableResponsive.PRIMARY_TEXT_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = SableResponsive.SECONDARY_TEXT_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        split.trailing.forEach { action ->
            SableSquareAction(
                text = actionGlyph(action),
                description = action.label,
                active = action.primary,
                onClick = { onAction(action) },
            )
        }
        if (split.overflow.isNotEmpty()) {
            Box {
                SableSquareAction(text = "⋯", description = t("sable_more_actions_for", title), onClick = { menuOpen = true })
                SableActionMenu(menuOpen, split.overflow, { menuOpen = false }, onAction)
            }
        }
    }
}

/** Short glyph for well-known row actions; the label is always the accessible name. */
private fun actionGlyph(action: SableRowAction): String =
    when (action.id) {
        "play" -> "▶"
        "pause" -> "❚❚"
        else -> action.label
    }

@Composable
private fun SableActionMenu(
    expanded: Boolean,
    actions: List<SableRowAction>,
    onDismiss: () -> Unit,
    onAction: (SableRowAction) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        actions.forEach { action ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = action.label,
                        color = if (action.destructive) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                },
                onClick = {
                    onDismiss()
                    onAction(action)
                },
            )
        }
    }
}

/** 48 dp square action with an accessible label. */
@Composable
fun SableSquareAction(
    text: String,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .widthIn(min = SableSize.TouchTarget)
                .heightIn(min = SableSize.TouchTarget)
                .sableFocusRing()
                .semantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier.heightIn(min = SableSize.TouchTarget).padding(horizontal = SableSpacing.Sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, maxLines = 1)
        }
    }
}

/**
 * The single, functional alphabet rail: only letters that have items, each one
 * jumps. It is a touch convenience; it never takes keyboard focus (keyboard
 * users type letters, see [sableLetterJump]).
 */
@Composable
fun SableAlphabetRail(
    index: SableAlphabetIndex,
    activeKey: String?,
    onJump: (itemIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (index.isEmpty) return
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .width(SableAlphabetRailWidth)
                .focusProperties { canFocus = false }
                .pointerInput(index) {
                    fun jump(y: Float) {
                        if (size.height <= 0) return
                        index.sectionAt(y / size.height)?.let { onJump(it.firstIndex) }
                    }
                    detectDragGestures(
                        onDragStart = { offset -> jump(offset.y) },
                        onDrag = { change, _ ->
                            jump(change.position.y)
                            change.consume()
                        },
                    )
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        index.sections.forEach { section ->
            val active = section.key == activeKey
            Text(
                text = section.key,
                modifier =
                    Modifier
                        .focusProperties { canFocus = false }
                        .clickable { onJump(section.firstIndex) }
                        .semantics { contentDescription = t("sable_jump_to", section.key) }
                        .padding(horizontal = SableSpacing.Sm, vertical = 1.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

val SableAlphabetRailWidth = 32.dp

/**
 * Keyboard A–Z jump for an indexed list (KEYBOARD_A_TO_Z_JUMP=PRIMARY): a
 * printable letter or digit with no Ctrl/Alt/Meta, typed while focus is inside
 * the list (not in a text field), jumps to its section or the next one.
 */
fun Modifier.sableLetterJump(
    index: SableAlphabetIndex,
    enabled: Boolean = true,
    onJump: (itemIndex: Int) -> Unit,
): Modifier =
    onPreviewKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val native = event.nativeKeyEvent
        if (native.isCtrlPressed || native.isAltPressed || native.isMetaPressed || native.repeatCount > 0) {
            return@onPreviewKeyEvent false
        }
        val unicode = native.unicodeChar
        if (unicode <= 0) return@onPreviewKeyEvent false
        val char = unicode.toChar()
        if (!char.isLetterOrDigit()) return@onPreviewKeyEvent false
        val target = index.indexForChar(char) ?: return@onPreviewKeyEvent false
        onJump(target)
        true
    }

/** "Back to latest" action shown while the newest content is scrolled out of view. */
@Composable
fun SableReturnToCurrent(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "latest",
) {
    if (!visible) return
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .heightIn(min = SableSize.TouchTarget)
                .sableFocusRing()
                .semantics { contentDescription = t("sable_return_to", label) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Box(
            modifier = Modifier.heightIn(min = SableSize.TouchTarget).padding(horizontal = SableSpacing.Lg),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "↓ $label", style = MaterialTheme.typography.labelLarge)
        }
    }
}
