package com.katoaapps.openminilaunch.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Immutable
internal data class TutorialPage(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
)

/** A reusable, swipeable teaching surface for short feature tours. */
@Composable
internal fun PagedTutorialDialog(
    @StringRes titleRes: Int,
    pages: List<TutorialPage>,
    onDismiss: () -> Unit,
) {
    require(pages.isNotEmpty())
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val lastPage = pagerState.currentPage == pages.lastIndex

    AlertDialog(
        modifier = Modifier.minkDialogWidth(),
        properties = MinkDialogDefaults.properties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp16)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.dp200, max = Dimens.dp280),
                ) { index ->
                    TutorialPageBody(pages[index])
                }
                TutorialPageIndicator(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage,
                )
            }
        },
        confirmButton = {
            if (lastPage) {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.done))
                }
            }
        },
    )
}

@Composable
private fun TutorialPageBody(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.dp8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.dp48),
            tint = Rust,
        )
        Text(
            text = stringResource(page.titleRes),
            modifier = Modifier.padding(top = Dimens.dp14),
            fontSize = Dimens.sp20,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(page.descriptionRes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.dp8),
            color = Muted,
            fontSize = Dimens.sp14,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TutorialPageIndicator(
    pageCount: Int,
    currentPage: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = Dimens.dp4)
                    .size(if (selected) Dimens.dp10 else Dimens.dp7)
                    .background(
                        color = if (selected) Rust else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
