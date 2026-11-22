package org.sableos.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Screen padding: the design's 24/20 dp, or 12/8 dp in a short window so content gets the room. */
@Composable
fun Modifier.sableScreenPadding(): Modifier =
    if (isSableShortWindow()) {
        padding(horizontal = SableSpacing.ScreenHorizontalShort, vertical = SableSpacing.ScreenVerticalShort)
    } else {
        padding(horizontal = SableSpacing.ScreenHorizontal, vertical = SableSpacing.ScreenVertical)
    }

/** Inner padding of panels and tiles: 16 dp, or 12 dp in a short window. */
@Composable
fun sablePanelPadding(): Dp = if (isSableShortWindow()) SableSpacing.Md else SableSpacing.Lg

/** Gap between a screen's blocks: 16 dp, or 8 dp in a short window. */
@Composable
fun sableScreenGap(): Dp = if (isSableShortWindow()) SableSpacing.Sm else SableSpacing.Lg

@Composable
fun SableScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .sableScreenPadding(),
            verticalArrangement = Arrangement.spacedBy(sableScreenGap()),
            content = content,
        )
    }
}

@Composable
fun SableScrollableScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
                    .sableVerticalScroll()
                    .sableScreenPadding(),
            verticalArrangement = Arrangement.spacedBy(sableScreenGap()),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SableRefreshableSurface(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        content()
    }
}

/**
 * True draws every [SablePanel] below it as a flat BlackBerry 10 list section (black, a divider
 * above, no card) instead of a card. Sable Hub provides it (Sable 2.0 Hub polish); other apps
 * keep cards.
 */
val LocalSableFlatPanels = staticCompositionLocalOf { false }

@Composable
fun SablePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalSableFlatPanels.current) {
        // BlackBerry 10 list section (Sable 2.0): flat on the screen's black, a thin line above.
        Column(modifier = modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(vertical = SableSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(SableSpacing.Md),
                content = content,
            )
        }
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(sablePanelPadding()),
            verticalArrangement = Arrangement.spacedBy(SableSpacing.Md),
            content = content,
        )
    }
}

@Composable
fun SableActionButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    onClick: () -> Unit,
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = SableSize.TouchTarget),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = SableSize.TouchTarget),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(text)
        }
    }
}

@Composable
fun SablePageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    if (isSableShortWindow()) {
        SableShortHeader(title = title, subtitle = subtitle, modifier = modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SableSpacing.Sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier =
                Modifier
                    .padding(top = SableSpacing.Sm)
                    .width(SableSpacing.Xxxl)
                    .height(SableSize.AccentBarWidth)
                    .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
fun SableHeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
) {
    if (isSableShortWindow()) {
        SableShortHeader(title = title, eyebrow = eyebrow, modifier = modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SableSpacing.Sm),
    ) {
        if (eyebrow != null) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Light,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier =
                Modifier
                    .padding(top = SableSpacing.Sm)
                    .width(SableSpacing.Xxxl)
                    .height(SableSize.AccentBarWidth)
                    .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * The page and hero header in a short window: one line, eyebrow then title, and the subtitle (when
 * there is one) as one muted line, so the screen's content starts right under it.
 */
@Composable
private fun SableShortHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(SableSpacing.Sm)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SableValuePanel(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(sablePanelPadding()),
            verticalArrangement = Arrangement.spacedBy(if (isSableShortWindow()) SableSpacing.Xs else SableSpacing.Sm),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    modifier = Modifier.wrapContentWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SableSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun SableListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val background =
        if (selected) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color.Transparent
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(background)
                .sableFocusRing()
                .clickable(onClick = onClick)
                // Minimum, not fixed: large font scales grow the row instead of clipping it.
                .heightIn(min = SableSize.ListRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(SableSize.AccentBarWidth)
                    .height(SableSpacing.Xxl)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = SableSpacing.Lg),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = SableResponsive.PRIMARY_TEXT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun SableTile(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val container =
        if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val content =
        if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = SableSize.TouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        border =
            BorderStroke(
                1.dp,
                if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            ),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(sablePanelPadding()),
            verticalArrangement = Arrangement.spacedBy(SableSpacing.Sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content.copy(alpha = 0.82f),
                )
            }
        }
    }
}

@Composable
fun SableDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
