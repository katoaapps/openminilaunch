package org.sableos.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import org.sableos.design.SableActionButton
import org.sableos.design.SableSpacing

/**
 * A game control: the on-screen button and the key that does the same. [tab] marks a Pause or New
 * action that the shell's own top-bar tabs replace; the rest go in the pause menu under a shell.
 */
internal data class GameAction(
    val label: String,
    val key: String,
    val active: Boolean = false,
    val tab: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Where the board and controls go. A window less than [TALL_RATIO] times as tall as it is wide (the
 * Q25's 720 x 720 square, the Titan 2's and P26's 1440 x 1440) gets a thin [Bar] across the top, with
 * the name, the status and small tabs, and the board fills the square under it. A tall phone keeps
 * the bar, the board as wide as the screen and the buttons under it in one column.
 */
internal object GameLayout {
    const val TALL_RATIO = 1.3f
    val Bar = 22.dp
    val Edge = 4.dp

    fun isSquare(
        widthDp: Float,
        heightDp: Float,
    ): Boolean = heightDp < widthDp * TALL_RATIO

    /** The board's side in a square window: everything under the bar. */
    fun squareBoardSide(
        width: Dp,
        height: Dp,
    ): Dp = min(height - Bar - Edge * 2, width - Edge * 2)

    /** The board's side in a tall window: the width, at most 72 % of the height. */
    fun tallBoardSide(
        width: Dp,
        height: Dp,
    ): Dp = min(width - SableSpacing.ScreenHorizontal * 2, height * 0.72f)
}

/** The bar and tab colours: the shell's game palette, or the theme's where there is no shell. */
private data class FrameColors(
    val field: Color,
    val bar: Color,
    val accent: Color,
    val ink: Color,
    val muted: Color,
)

@Composable
private fun frameColors(shell: ShellPlay?): FrameColors {
    val p = shell?.spec?.palette
    val c = MaterialTheme.colorScheme
    return if (p != null) {
        FrameColors(p.field, p.bar, p.accent, p.ink, p.muted)
    } else {
        FrameColors(c.background, c.surface, c.primary, c.onBackground, c.onSurfaceVariant)
    }
}

/** One game's screen, laid out for the window ([GameLayout]). */
@Composable
internal fun GameFrame(
    title: String,
    subtitle: String,
    statusLabel: String,
    status: String,
    hint: String,
    actions: List<GameAction>,
    tallExtras: @Composable ColumnScope.() -> Unit = {},
    board: @Composable (side: Dp) -> Unit,
) {
    val shell = LocalGameShell.current
    val colors = frameColors(shell)
    val tabs =
        if (shell == null) {
            actions
        } else {
            val menuActions = actions.filterNot { it.tab }
            SideEffect { shell.setActions(menuActions) }
            listOfNotNull(
                GameAction("Pause", shell.pauseKey.orEmpty(), onClick = shell.onPause),
                GameAction("New", shell.newKey.orEmpty(), onClick = shell.onNew),
                GameAction("Menu", "", onClick = shell.onMenu),
            )
        }
    Surface(modifier = Modifier.fillMaxSize(), color = colors.field) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val width = maxWidth
            val height = maxHeight
            if (GameLayout.isSquare(width.value, height.value)) {
                Column(Modifier.fillMaxSize()) {
                    TopBar(title, statusLabel, status, tabs, colors)
                    Box(Modifier.fillMaxSize().padding(GameLayout.Edge), contentAlignment = Alignment.Center) {
                        board(GameLayout.squareBoardSide(width, height))
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    TopBar(title, statusLabel, status, if (shell == null) emptyList() else tabs.takeLast(1), colors)
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = SableSpacing.ScreenHorizontal, vertical = SableSpacing.Md),
                        verticalArrangement = Arrangement.spacedBy(SableSpacing.Md),
                    ) {
                        Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            board(GameLayout.tallBoardSide(width, height))
                        }
                        tallExtras()
                        val buttons = (if (shell == null) actions else tabs.dropLast(1) + actions.filterNot { it.tab })
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SableSpacing.Sm),
                        ) {
                            buttons.forEach { action ->
                                SableActionButton(
                                    text = action.label,
                                    modifier = Modifier.weight(1f),
                                    primary = action.active,
                                    onClick = action.onClick,
                                )
                            }
                        }
                        Text(hint, color = colors.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** The thin bar over the board: the game's name in its colour, the status, and the tabs on the right. */
@Composable
private fun TopBar(
    title: String,
    statusLabel: String,
    status: String,
    tabs: List<GameAction>,
    colors: FrameColors,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(GameLayout.Bar)
                .background(colors.bar)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            title.uppercase(),
            color = colors.accent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            maxLines = 1,
        )
        Text(status, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        Text(
            statusLabel,
            color = colors.muted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        tabs.forEach { Tab(it, colors) }
    }
}

/** A small tab in the bar: the label and its key, outlined, filled while [GameAction.active]. */
@Composable
private fun Tab(
    action: GameAction,
    colors: FrameColors,
) {
    Surface(
        onClick = action.onClick,
        modifier = Modifier.height(18.dp),
        shape = RoundedCornerShape(5.dp),
        color = if (action.active) colors.accent else Color.Transparent,
        contentColor = if (action.active) colors.bar else colors.ink,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
    ) {
        Box(Modifier.padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
            Text(
                if (action.key.isEmpty()) action.label else "${action.label} ${action.key}",
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 1,
            )
        }
    }
}
