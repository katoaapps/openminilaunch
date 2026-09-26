package com.katoaapps.openminilaunch.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.profile.ProfileCard
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.features.profile.ProfileQrCode
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** The high-contrast Home face is intentionally independent from the editor preview. */
@Composable
internal fun HomeVCardContent(
    store: LauncherStore,
    card: ProfileCard,
    selectedLinks: List<ProfileLink>,
    qrCode: ProfileQrCode?,
    compact: Boolean,
    onOpenQr: () -> Unit,
    onFixQr: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val qrSize = responsiveQrSize(maxWidth, maxHeight, compact)
        Row(
            Modifier.fillMaxSize().padding(
                start = if (compact) Dimens.dp12 else Dimens.dp20,
                top = if (compact) Dimens.dp12 else Dimens.dp18,
                end = if (compact) Dimens.dp48 else Dimens.dp58,
                bottom = if (compact) Dimens.dp12 else Dimens.dp18,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (compact) Dimens.dp12 else Dimens.dp22),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) Dimens.dp4 else Dimens.dp7),
            ) {
                ProfilePortrait(store, if (compact) Dimens.dp54 else Dimens.dp72)
                Text(
                    card.fullName,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = if (compact) Dimens.sp18 else Dimens.sp24,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                card.jobTitle.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        color = Color.White.copy(alpha = .88f),
                        fontSize = Dimens.sp13,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                card.organization.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        color = Color.White.copy(alpha = .66f),
                        fontSize = Dimens.sp12,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                selectedLinks.take(if (compact) 3 else 5).forEach { link ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.dp14),
                            tint = Color.White.copy(alpha = .72f),
                        )
                        if (card.showValuesOnCard) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    link.label.ifBlank { profileLinkTypeLabel(link.type) },
                                    color = Color.White.copy(alpha = .68f),
                                    fontSize = Dimens.sp11,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    link.value,
                                    color = Color.White.copy(alpha = .92f),
                                    fontSize = Dimens.sp12,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else {
                            Text(
                                link.label.ifBlank { profileLinkTypeLabel(link.type) },
                                color = Color.White.copy(alpha = .82f),
                                fontSize = Dimens.sp12,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (qrCode != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp6),
                ) {
                    ProfileQrImage(
                        code = qrCode,
                        size = qrSize,
                        description = stringResource(R.string.open_profile_qr),
                        modifier = Modifier.clickable(onClick = onOpenQr),
                    )
                    Text(
                        stringResource(R.string.tap_to_enlarge),
                        color = Color.White.copy(alpha = .72f),
                        fontSize = Dimens.sp11,
                    )
                }
            } else {
                FilledTonalButton(onClick = onFixQr) {
                    Text(stringResource(R.string.fix_profile_qr))
                }
            }
        }
    }
}

@Composable
private fun responsiveQrSize(width: Dp, height: Dp, compact: Boolean): Dp {
    val widthBound = width * if (compact) .34f else .39f
    val heightBound = height - if (compact) Dimens.dp48 else Dimens.dp64
    return minOf(widthBound, heightBound).coerceIn(
        Dimens.dp96,
        if (compact) Dimens.dp180 else Dimens.dp280,
    )
}
