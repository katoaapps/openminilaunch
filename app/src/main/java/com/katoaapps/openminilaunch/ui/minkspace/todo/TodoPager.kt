@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.minkspace.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceTitle
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.LightPaper
import com.katoaapps.openminilaunch.ui.theme.MinkBlack
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Sage
import kotlin.math.ceil

@Composable
internal fun TodoPager(
    store: LauncherStore,
    openTodos: () -> Unit,
    jumpToken: Int,
    itemsPerPage: Int = 3,
    textMetrics: HomeTodoTextMetrics = HomeTodoTextMetrics(15f, 20f, 2),
    compact: Boolean = false,
    contentColor: Color = LightPaper,
    mutedContentColor: Color = Sage,
    modifier: Modifier = Modifier,
) {
    val safeItemsPerPage = itemsPerPage.coerceIn(1, 5)
    val pages = maxOf(1, ceil(store.todos.size / safeItemsPerPage.toFloat()).toInt())
    val pagerState = rememberPagerState(pageCount = { pages })
    LaunchedEffect(pages) {
        if (pagerState.currentPage >= pages) pagerState.scrollToPage(pages - 1)
    }
    LaunchedEffect(jumpToken, pages) {
        if (jumpToken > 0) {
            val newestUnfinishedPage = store.todos.indexOfLast { !it.completed }
                .coerceAtLeast(0) / safeItemsPerPage
            pagerState.animateScrollToPage(newestUnfinishedPage.coerceAtMost(pages - 1))
        }
    }
    Column(
        modifier.fillMaxWidth()
            .clickable(onClick = openTodos)
            .padding(if (compact) Dimens.dp8 else Dimens.dp16),
        verticalArrangement = Arrangement.spacedBy(if (compact) Dimens.dp3 else Dimens.dp8),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MinkSpaceTitle(
                title = stringResource(R.string.todo_heading),
                icon = Icons.Default.Checklist,
                contentColor = contentColor,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.page_of_pages, pagerState.currentPage + 1, pages),
                color = mutedContentColor,
                fontSize = Dimens.sp12,
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            val pageItems = store.todos
                .drop(page * safeItemsPerPage)
                .take(safeItemsPerPage)
            if (pageItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Text(stringResource(R.string.tap_to_add_first_todo), color = mutedContentColor)
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (compact) Dimens.dp5 else Dimens.dp8),
                ) {
                    pageItems.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().weight(1f),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Checkbox(
                                checked = item.completed,
                                onCheckedChange = { store.toggleTodo(item.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = contentColor,
                                    uncheckedColor = mutedContentColor,
                                    checkmarkColor = if (contentColor == MinkWhite) MinkBlack else MinkWhite,
                                ),
                                modifier = Modifier.size(if (compact) Dimens.dp24 else Dimens.dp26),
                            )
                            Text(
                                item.text,
                                color = if (item.completed) mutedContentColor else contentColor,
                                fontSize = textMetrics.fontSizeSp.sp,
                                lineHeight = textMetrics.lineHeightSp.sp,
                                maxLines = textMetrics.maxLines,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                                modifier = Modifier.padding(start = Dimens.dp7, top = Dimens.dp2).weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
