@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch.ui.minkspace.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.profile.ProfileCard
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.features.profile.ProfileQrCode
import com.katoaapps.openminilaunch.ui.profile.ProfilePortrait
import com.katoaapps.openminilaunch.ui.profile.ProfileQrImage
import com.katoaapps.openminilaunch.ui.profile.profileLinkTypeLabel
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Pure vCard presentation; the page owns profile state, dialogs, and MinkSpace expansion. */
@Composable
internal fun VCardMiniAppContent(
    store: LauncherStore,
    card: ProfileCard,
    selectedLinks: List<ProfileLink>,
    qrCode: ProfileQrCode?,
    compact: Boolean,
    expanded: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    onExpandedChanged: (Boolean) -> Unit,
    onOpenQr: () -> Unit,
    onFixQr: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val outerPadding = if (compact) Dimens.dp10 else Dimens.dp16
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxSize().padding(outerPadding),
                horizontalArrangement = Arrangement.spacedBy(if (compact) Dimens.dp10 else Dimens.dp20),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VCardIdentity(
                    store = store,
                    card = card,
                    selectedLinks = selectedLinks,
                    compact = compact,
                    expanded = true,
                    contentColor = contentColor,
                    mutedContentColor = mutedContentColor,
                    modifier = Modifier.weight(1f),
                )
                VCardQr(
                    qrCode = qrCode,
                    size = minOf(availableWidth * .36f, availableHeight - outerPadding * 2)
                        .coerceIn(Dimens.dp96, Dimens.dp200),
                    contentColor = contentColor,
                    mutedContentColor = mutedContentColor,
                    onOpenQr = onOpenQr,
                    onFixQr = onFixQr,
                )
            }
        } else {
            VCardIdentity(
                store = store,
                card = card,
                selectedLinks = selectedLinks,
                compact = compact,
                expanded = false,
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                modifier = Modifier.fillMaxSize().padding(outerPadding),
            )
        }

        IconButton(
            onClick = { onExpandedChanged(!expanded) },
            modifier = Modifier.align(Alignment.TopEnd).size(Dimens.dp44),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.collapse_vcard else R.string.expand_vcard,
                ),
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun VCardIdentity(
    store: LauncherStore,
    card: ProfileCard,
    selectedLinks: List<ProfileLink>,
    compact: Boolean,
    expanded: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ProfilePortrait(
            store = store,
            size = when {
                compact -> Dimens.dp48
                expanded -> Dimens.dp72
                else -> Dimens.dp64
            },
        )
        Text(
            text = card.fullName,
            modifier = Modifier.padding(top = if (compact) Dimens.dp5 else Dimens.dp8),
            color = contentColor,
            fontWeight = FontWeight.Black,
            fontSize = if (compact) Dimens.sp17 else Dimens.sp20,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        card.note.takeIf(String::isNotBlank)?.let { bio ->
            Text(
                text = bio,
                modifier = Modifier.padding(top = Dimens.dp3),
                color = contentColor.copy(alpha = .86f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = if (expanded) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        card.organization.takeIf(String::isNotBlank)?.let { company ->
            Text(
                text = company,
                modifier = Modifier.padding(top = Dimens.dp2),
                color = mutedContentColor,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selectedLinks.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = if (compact) Dimens.dp6 else Dimens.dp10),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp5, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp5),
                maxItemsInEachRow = if (compact) 3 else 4,
            ) {
                selectedLinks.take(if (expanded) 6 else 4).forEach { link ->
                    VCardLinkChip(
                        link = link,
                        showValue = card.showValuesOnCard,
                        contentColor = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun VCardLinkChip(
    link: ProfileLink,
    showValue: Boolean,
    contentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(Dimens.dp96),
        color = contentColor.copy(alpha = .1f),
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.dp8, vertical = Dimens.dp5),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Link, null, Modifier.size(Dimens.dp13))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = link.label.ifBlank { profileLinkTypeLabel(link.type) },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showValue) {
                    Text(
                        text = link.value,
                        color = contentColor.copy(alpha = .7f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun VCardQr(
    qrCode: ProfileQrCode?,
    size: Dp,
    contentColor: Color,
    mutedContentColor: Color,
    onOpenQr: () -> Unit,
    onFixQr: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (qrCode != null) {
            ProfileQrImage(
                code = qrCode,
                size = size,
                description = stringResource(R.string.open_profile_qr),
                modifier = Modifier.clickable(onClick = onOpenQr),
            )
            Text(
                text = stringResource(R.string.tap_to_enlarge),
                modifier = Modifier.padding(top = Dimens.dp6),
                color = mutedContentColor,
                style = MaterialTheme.typography.labelSmall,
            )
        } else {
            FilledTonalButton(onClick = onFixQr) {
                Text(stringResource(R.string.fix_profile_qr), color = contentColor)
            }
        }
    }
}
