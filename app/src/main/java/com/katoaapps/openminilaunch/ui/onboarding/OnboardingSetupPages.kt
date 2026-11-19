package com.katoaapps.openminilaunch.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.HomeLayoutStyle
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MagicCallColor
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun OnboardingHomeSetupPage(
    appName: String,
    active: Boolean,
    onChooseHome: () -> Unit,
    onManageHome: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
        Text(stringResource(R.string.onboarding_home_setup_description, appName), fontSize = Dimens.sp18)
        SetupStatusCard(
            icon = { Icon(Icons.Default.Home, null, tint = Rust) },
            title = stringResource(R.string.default_home_app),
            description = if (active) {
                stringResource(R.string.onboarding_home_setup_active, appName)
            } else {
                stringResource(R.string.onboarding_home_setup_inactive)
            },
            active = active,
        )
        if (active) {
            FilledTonalButton(onClick = onManageHome, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, null)
                Text(stringResource(R.string.manage), Modifier.padding(start = Dimens.dp8))
            }
        } else {
            Button(onClick = onChooseHome, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Home, null)
                Text(stringResource(R.string.default_home_app), Modifier.padding(start = Dimens.dp8))
            }
        }
    }
}

@Composable
internal fun OnboardingHomeLayoutSetupPage(
    selected: HomeLayoutStyle,
    onSelect: (HomeLayoutStyle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
        Text(
            stringResource(R.string.onboarding_choose_home_description),
            fontSize = Dimens.sp18,
        )
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
        ) {
            HomeLayoutOnboardingChoice(
                title = stringResource(R.string.home_layout_mink),
                description = stringResource(R.string.home_layout_mink_description),
                selected = selected == HomeLayoutStyle.MINK,
                classic = false,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onSelect(HomeLayoutStyle.MINK) },
            )
            HomeLayoutOnboardingChoice(
                title = stringResource(R.string.home_layout_classic),
                description = stringResource(R.string.home_layout_classic_description),
                selected = selected == HomeLayoutStyle.CLASSIC,
                classic = true,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onSelect(HomeLayoutStyle.CLASSIC) },
            )
        }
    }
}

@Composable
private fun HomeLayoutOnboardingChoice(
    title: String,
    description: String,
    selected: Boolean,
    classic: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.dp18),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (selected) {
            BorderStroke(Dimens.dp2, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(
            Modifier.padding(horizontal = Dimens.dp12, vertical = Dimens.dp16),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = Dimens.sp17,
                textAlign = TextAlign.Center,
            )
            HomeLayoutMiniature(classic = classic, selected = selected)
            Text(
                description,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .78f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = Dimens.sp13,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeLayoutMiniature(classic: Boolean, selected: Boolean) {
    val accent = if (selected) MaterialTheme.colorScheme.primary else Rust
    Surface(
        modifier = Modifier.width(Dimens.dp72).height(Dimens.dp88),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.dp14),
    ) {
        if (classic) {
            Column(
                Modifier.padding(Dimens.dp8),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp6),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.ViewCarousel, null, tint = accent)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp4)) {
                    repeat(3) { HomeLayoutPreviewBlock(Dimens.dp12, Dimens.dp12, accent) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp3)) {
                    repeat(5) { index ->
                        Box(
                            Modifier.size(if (index == 1) Dimens.dp6 else Dimens.dp4)
                                .background(
                                    if (index == 1) accent else Muted,
                                    RoundedCornerShape(Dimens.dp6),
                                ),
                        )
                    }
                }
            }
        } else {
            Row(
                Modifier.padding(Dimens.dp8),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp5),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeLayoutPreviewBlock(Dimens.dp34, Dimens.dp58, accent)
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp5)) {
                    HomeLayoutPreviewBlock(Dimens.dp17, Dimens.dp17, accent)
                    HomeLayoutPreviewBlock(Dimens.dp17, Dimens.dp17, accent)
                    HomeLayoutPreviewBlock(Dimens.dp17, Dimens.dp17, accent)
                }
            }
        }
    }
}

@Composable
private fun HomeLayoutPreviewBlock(
    width: Dp,
    height: Dp,
    color: Color,
) {
    Box(
        Modifier.width(width).height(height)
            .background(color.copy(alpha = .72f), RoundedCornerShape(Dimens.dp5)),
    )
}

@Composable
internal fun OnboardingContactsSetupPage(
    granted: Boolean,
    onAllow: () -> Unit,
    onManage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp18)) {
        Text(stringResource(R.string.onboarding_contacts_setup_description), fontSize = Dimens.sp18)
        SetupStatusCard(
            icon = { Icon(Icons.Default.Contacts, null, tint = Rust) },
            title = stringResource(R.string.contacts),
            description = stringResource(R.string.onboarding_contacts_permission_description),
            active = granted,
        )
        if (granted) {
            OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.manage))
            }
        } else {
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.allow_contact_access))
            }
        }
    }
}

@Composable
internal fun OnboardingAssistantSetupPage(
    active: Boolean,
    onChooseAssistant: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
        Text(stringResource(R.string.onboarding_assistant_setup_description), fontSize = Dimens.sp18)
        if (active) {
            OutlinedButton(onClick = onChooseAssistant, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, null)
                Text(stringResource(R.string.manage_mink_assistant))
            }
        } else {
            Button(onClick = onChooseAssistant, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Assistant, null)
                Text(stringResource(R.string.choose_mink_assistant), Modifier.padding(start = Dimens.dp8))
            }
        }
        Text(
            stringResource(
                if (active) {
                    R.string.onboarding_assistant_active_description
                } else {
                    R.string.onboarding_assistant_inactive_description
                },
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = Dimens.sp13,
        )
        Text(
            stringResource(R.string.deliberate_choice_description),
            color = Muted,
            fontSize = Dimens.sp12,
        )
    }
}

@Composable
private fun SetupStatusCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    active: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(Dimens.dp16))
            .padding(Dimens.dp16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Column(Modifier.padding(start = Dimens.dp12).weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = Dimens.sp13)
        }
        if (active) {
            Icon(
                Icons.Default.CheckCircle,
                stringResource(R.string.granted),
                tint = MagicCallColor,
                modifier = Modifier.size(Dimens.dp24),
            )
        }
    }
}
