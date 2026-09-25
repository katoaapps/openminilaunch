package com.katoaapps.openminilaunch.ui.profile

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.profile.ProfileCard
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.ui.components.DrawableIcon
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ProfilePortrait(
    store: LauncherStore,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val revision = store.profileRepository.portraitRevision
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = revision) {
        value = withContext(Dispatchers.IO) { store.profileRepository.loadPortrait() }
    }
    DisposableEffect(bitmap) {
        onDispose { bitmap?.takeUnless(Bitmap::isRecycled)?.recycle() }
    }
    Box(
        modifier.size(size).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                checkNotNull(bitmap).asImageBitmap(),
                stringResource(R.string.profile_photo),
                Modifier.fillMaxSize(),
            )
        } else {
            DrawableIcon(
                drawable = rememberMinkIcon(),
                iconKey = "mink-profile-fallback",
                size = size * .78f,
                contentDescription = stringResource(R.string.mink_profile_icon),
            )
        }
    }
}

@Composable
internal fun ProfileEditorPreview(
    store: LauncherStore,
    card: ProfileCard,
    selectedLinks: List<ProfileLink>,
) {
    Row(
        Modifier.fillMaxWidth().padding(Dimens.dp18),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp18),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        ) {
            ProfilePortrait(
                store = store,
                size = Dimens.dp84,
            )
            Text(
                card.fullName,
                fontWeight = FontWeight.Black,
                fontSize = Dimens.sp24,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            card.jobTitle.takeIf(String::isNotBlank)?.let {
                Text(it, fontSize = Dimens.sp13, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            card.organization.takeIf(String::isNotBlank)?.let {
                Text(it, color = Muted, fontSize = Dimens.sp12, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selectedLinks.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp4)) {
                    selectedLinks.take(6).forEach { link ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Icon(
                                Icons.Default.Link,
                                link.label,
                                Modifier.padding(Dimens.dp6).size(Dimens.dp16),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyProfileCard(
    onCreateVCard: () -> Unit,
    compact: Boolean,
) {
    Column(
        Modifier.fillMaxSize().padding(Dimens.dp20),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(Dimens.dp24),
            color = Color.White.copy(alpha = .14f),
        ) {
            DrawableIcon(
                drawable = rememberMinkIcon(),
                iconKey = "mink-profile-empty",
                size = if (compact) Dimens.dp58 else Dimens.dp84,
                contentDescription = stringResource(R.string.mink_profile_icon),
            )
        }
        Text(
            stringResource(R.string.create_your_profile),
            Modifier.padding(top = Dimens.dp12),
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            stringResource(R.string.profile_empty_description),
            Modifier.padding(top = Dimens.dp4),
            color = Color.White.copy(alpha = .7f),
            fontSize = Dimens.sp12,
        )
        Button(onClick = onCreateVCard, modifier = Modifier.padding(top = Dimens.dp12)) {
            Icon(Icons.Default.Add, null)
            Text(stringResource(R.string.create_profile), Modifier.padding(start = Dimens.dp6))
        }
    }
}

@Composable
private fun rememberMinkIcon(): android.graphics.drawable.Drawable? {
    val context = LocalContext.current
    return androidx.compose.runtime.remember(context) {
        runCatching { context.packageManager.getApplicationIcon(context.packageName) }.getOrNull()
    }
}
