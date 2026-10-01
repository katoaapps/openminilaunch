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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.media.MediaItem
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceTitle
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun MediaMiniApp(
    items: List<MediaItem>,
    onOpenItem: (MediaItem) -> Unit,
    onManageMedia: (() -> Unit)? = null,
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

    val pagerState = rememberPagerState(pageCount = { items.size })
    LaunchedEffect(items.size) {
        if (pagerState.currentPage > items.lastIndex) {
            pagerState.scrollToPage(items.lastIndex)
        }
    }
    Box(modifier) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            items.getOrNull(page)?.let { item ->
                MediaPreview(
                    item = item,
                    onClick = { onOpenItem(item) },
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
                title = stringResource(R.string.media),
                icon = Icons.Default.PhotoLibrary,
                contentColor = contentColor,
            )
            Text(
                text = items.getOrNull(pagerState.currentPage)?.displayName.orEmpty(),
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.page_of_pages, pagerState.currentPage + 1, items.size),
                color = mutedContentColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
