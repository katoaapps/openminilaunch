package com.katoaapps.openminilaunch.ui.conversations

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun FavoriteContactAvatar(
    displayName: String,
    photoUri: String?,
    size: Dp = Dimens.dp46,
) {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(initialValue = null, key1 = photoUri) {
        value = photoUri?.let { rawUri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(rawUri))?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    val shape = RoundedCornerShape(size * .32f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = checkNotNull(photo),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = contactInitials(displayName),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

internal fun contactInitials(displayName: String): String = displayName
    .trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .let { parts ->
        when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts.first().take(2)
            else -> "${parts.first().first()}${parts.last().first()}"
        }
    }
    .uppercase()
