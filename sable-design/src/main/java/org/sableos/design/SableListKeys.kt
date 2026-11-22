package org.sableos.design

import android.view.KeyEvent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.sableos.design.DesignText.t

/** BlackBerry single-key list movement: T top, B bottom, Space page down, Shift+Space page up, N next, P previous. */
enum class SableListMove { Top, Bottom, PageDown, PageUp, Next, Previous }

/**
 * The single-key list contract shared by Sable lists (Messages, Hub, and Mail through its own keymap). Plain keys
 * only: an Alt, Ctrl or Meta chord types a symbol or is an app shortcut. A list passes the moves whose keys it
 * already gives another meaning in [command]'s `owned`, so they keep it.
 */
object SableListKeys {
    val HINT: String get() = t("sable_t_top_b_bottom_space")

    fun command(
        keyCode: Int,
        shift: Boolean,
        chord: Boolean,
        owned: Set<SableListMove> = emptySet(),
    ): SableListMove? {
        if (chord) return null
        val move =
            when (keyCode) {
                KeyEvent.KEYCODE_SPACE -> if (shift) SableListMove.PageUp else SableListMove.PageDown
                KeyEvent.KEYCODE_T -> SableListMove.Top.takeUnless { shift }
                KeyEvent.KEYCODE_B -> SableListMove.Bottom.takeUnless { shift }
                KeyEvent.KEYCODE_N -> SableListMove.Next.takeUnless { shift }
                KeyEvent.KEYCODE_P -> SableListMove.Previous.takeUnless { shift }
                else -> null
            }
        return move?.takeUnless { it in owned }
    }

    /**
     * The item [move] lands on, or null for an empty list.
     *
     * @param focused the focused item, or -1 when no item has focus (moves start from the first visible one).
     */
    fun target(
        move: SableListMove,
        focused: Int,
        firstVisible: Int,
        lastVisible: Int,
        itemCount: Int,
    ): Int? {
        if (itemCount <= 0) return null
        val anchor = if (focused >= 0) focused else firstVisible.coerceAtLeast(0)
        val page = (lastVisible - firstVisible).coerceAtLeast(1)
        val target =
            when (move) {
                SableListMove.Top -> 0
                SableListMove.Bottom -> itemCount - 1
                SableListMove.Next -> if (focused >= 0) focused + 1 else anchor
                SableListMove.Previous -> if (focused >= 0) focused - 1 else anchor
                SableListMove.PageDown -> anchor + page
                SableListMove.PageUp -> anchor - page
            }
        return target.coerceIn(0, itemCount - 1)
    }
}

/** Keyboard focus of a LazyColumn's rows, so [sableListKeys] can scroll to a row and focus it. */
class SableListFocus(
    val state: LazyListState,
) {
    private val requesters = HashMap<Any, FocusRequester>()

    /** Index of the row that has focus, or -1. */
    var focused: Int = -1
        private set

    fun row(
        index: Int,
        key: Any,
    ): Modifier =
        Modifier
            .focusRequester(requesters.getOrPut(key) { FocusRequester() })
            .onFocusChanged {
                if (it.hasFocus) {
                    focused = index
                } else if (focused == index) {
                    focused = -1
                }
            }

    suspend fun moveTo(
        index: Int,
        key: Any,
    ) {
        state.scrollToItem(index)
        withFrameNanos { }
        runCatching { requesters[key]?.requestFocus() }
    }
}

/**
 * Put on the LazyColumn: T, B, Space, Shift+Space, N and P move focus between its rows ([keys] are the row keys in
 * order, the same keys given to [SableListFocus.row]). Both the down and the up of a handled key are consumed, so
 * Space never also opens the row.
 */
fun Modifier.sableListKeys(
    focus: SableListFocus,
    keys: List<Any>,
    scope: CoroutineScope,
    owned: Set<SableListMove> = emptySet(),
): Modifier =
    onPreviewKeyEvent { event ->
        val native = event.nativeKeyEvent
        val chord = native.isAltPressed || native.isCtrlPressed || native.isMetaPressed
        val move = SableListKeys.command(native.keyCode, native.isShiftPressed, chord, owned)
        when {
            move == null -> {
                false
            }

            event.type != KeyEventType.KeyDown -> {
                true
            }

            else -> {
                val visible = focus.state.layoutInfo.visibleItemsInfo
                val target =
                    SableListKeys.target(
                        move,
                        focus.focused,
                        visible.firstOrNull()?.index ?: 0,
                        visible.lastOrNull()?.index ?: 0,
                        keys.size,
                    )
                if (target != null) scope.launch { focus.moveTo(target, keys[target]) }
                true
            }
        }
    }

/** Where T, B, Space and Shift+Space take a plain scrolling screen. Pure, so it is JVM-tested. */
object SableScrollKeys {
    /** The scroll offset [move] goes to, or null when it is not a scroll move or nothing would change. */
    fun destination(
        move: SableListMove,
        value: Int,
        max: Int,
        viewport: Int,
    ): Int? {
        val page = viewport * PAGE_NUMERATOR / PAGE_DENOMINATOR
        val to =
            when (move) {
                SableListMove.Top -> 0
                SableListMove.Bottom -> max
                SableListMove.PageDown -> value + page
                SableListMove.PageUp -> value - page
                SableListMove.Next, SableListMove.Previous -> return null
            }
        return to.coerceIn(0, max.coerceAtLeast(0)).takeIf { it != value }
    }

    // A page keeps a tenth of the screen in view, so the eye keeps its place.
    private const val PAGE_NUMERATOR = 9
    private const val PAGE_DENOMINATOR = 10
}

/**
 * A vertically scrolling screen with the BlackBerry keys: T top, B bottom, Space page down and Shift+Space page up.
 * It acts only on a key nothing inside took (a text field keeps its letters, a button its Space), so screens that
 * have their own list keys keep them. Use in place of `verticalScroll(rememberScrollState())`.
 */
@Composable
fun Modifier.sableVerticalScroll(): Modifier {
    val state = rememberScrollState()
    return sableScrollKeys(state).verticalScroll(state)
}

@Composable
private fun Modifier.sableScrollKeys(state: ScrollState): Modifier {
    val scope = rememberCoroutineScope()
    var viewport by remember { mutableIntStateOf(0) }
    return onSizeChanged { viewport = it.height }.onKeyEvent { event ->
        val native = event.nativeKeyEvent
        val chord = native.isAltPressed || native.isCtrlPressed || native.isMetaPressed
        val move =
            SableListKeys.command(
                native.keyCode,
                native.isShiftPressed,
                chord,
                owned = setOf(SableListMove.Next, SableListMove.Previous),
            )
        val to = move?.let { SableScrollKeys.destination(it, state.value, state.maxValue, viewport) }
        if (to == null) {
            false
        } else {
            if (event.type == KeyEventType.KeyDown) scope.launch { state.animateScrollTo(to) }
            true
        }
    }
}
