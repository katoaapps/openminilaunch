@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.minkspace.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.media.MediaItem
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceTitle
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun MediaMiniApp(
    items: List<MediaItem>,
    onOpenItem: (MediaItem) -> Unit,
    expanded: Boolean = false,
    expandedPresentation: Boolean = expanded,
    onExpand: (() -> Unit)? = null,
    onManageMedia: (() -> Unit)? = null,
    focusedItemKey: String? = null,
    onFocusedItemChange: ((MediaItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentColor: Color = LocalContentColor.current,
    mutedContentColor: Color = Muted,
) {
    if (items.isEmpty()) {
        val emptyModifier = if (onManageMedia != null) {
            modifier.clickable(onClick = onManageMedia)
        } else {
            modifier
        }
        Box(emptyModifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = mutedContentColor)
                Text(
                    text = stringResource(R.string.media_empty),
                    color = mutedContentColor,
                    modifier = Modifier.padding(top = Dimens.dp8),
                )
            }
        }
        return
    }

    val itemKeys = items.map { it.uri.toString() }
    val initialPage = itemKeys.indexOf(focusedItemKey)
        .takeIf { it >= 0 }
        ?: 0
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { items.size },
    )
    LaunchedEffect(items.size) {
        if (pagerState.currentPage > items.lastIndex) {
            pagerState.scrollToPage(items.lastIndex)
        }
    }
    LaunchedEffect(focusedItemKey, itemKeys) {
        val focusedIndex = itemKeys.indexOf(focusedItemKey)
        if (focusedIndex >= 0 && focusedIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(focusedIndex)
        }
    }
    val currentOnFocusedItemChange = rememberUpdatedState(onFocusedItemChange)
    val currentItems = rememberUpdatedState(items)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentItems.value.getOrNull(page)?.let { item ->
                currentOnFocusedItemChange.value?.invoke(item)
            }
        }
    }
    Box(modifier) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            items.getOrNull(page)?.let { item ->
                MediaPreview(
                    item = item,
                    onClick = {
                        if (!expanded && onExpand != null) {
                            onExpand()
                        } else {
                            onOpenItem(item)
                        }
                    },
                    fitCenter = expandedPresentation,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(Dimens.dp12),
        ) {
            MinkSpaceTitle(
                title = stringResource(R.string.media_lab),
                icon = Icons.Default.PhotoLibrary,
                contentColor = contentColor,
            )
            Text(
                text = stringResource(R.string.page_of_pages, pagerState.currentPage + 1, items.size),
                color = mutedContentColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
