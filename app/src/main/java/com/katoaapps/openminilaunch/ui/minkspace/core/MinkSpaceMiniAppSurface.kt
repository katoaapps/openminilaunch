package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Shared Home container so mini-apps receive the same shape, color, and available space. */
@Composable
internal fun MinkSpaceMiniAppSurface(
    compact: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(if (compact) Dimens.dp18 else Dimens.dp24),
        color = containerColor,
        contentColor = contentColor,
        content = content,
    )
}
