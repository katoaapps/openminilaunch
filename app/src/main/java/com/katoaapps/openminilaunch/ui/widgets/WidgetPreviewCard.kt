package com.katoaapps.openminilaunch.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.drawable.toBitmap
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.displayLabel
import kotlin.math.roundToInt

/** One provider preview in the shared Android widget picker. */
@Composable
internal fun WidgetPreviewCard(
    info: AppWidgetProviderInfo,
    actions: DeviceActions,
    modifier: Modifier = Modifier,
    badges: WidgetProviderBadges? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.densityDpi
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val sizeRange = remember(info.provider, info.minWidth, info.minHeight) {
        widgetSizeRange(info, context.resources.displayMetrics.density)
    }
    val bitmap = remember(info.provider, info.previewImage, density, launcherAppsRevision) {
        val drawable = runCatching { info.loadPreviewImage(context, density) }.getOrNull()
            ?: runCatching { info.loadIcon(context, density) }.getOrNull()
            ?: actions.appIcon(info.provider.packageName)
        drawable?.let {
            runCatching {
                val sourceWidth = it.intrinsicWidth.coerceAtLeast(1)
                val sourceHeight = it.intrinsicHeight.coerceAtLeast(1)
                val scale = minOf(360f / sourceWidth, 220f / sourceHeight, 1f)
                it.toBitmap(
                    width = (sourceWidth * scale).roundToInt().coerceAtLeast(1),
                    height = (sourceHeight * scale).roundToInt().coerceAtLeast(1),
                ).asImageBitmap()
            }.getOrNull()
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.dp16),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(Dimens.dp10)) {
            Box(
                Modifier.fillMaxWidth().height(Dimens.dp116)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(Dimens.dp12),
                    )
                    .padding(Dimens.dp8),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Icon(Icons.Default.Widgets, null, Modifier.size(Dimens.dp42), tint = Muted)
                }
            }
            Text(
                info.loadLabel(context.packageManager),
                modifier = Modifier.padding(top = Dimens.dp8),
                fontSize = Dimens.sp12,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                if (sizeRange.isResizable) {
                    stringResource(R.string.widget_resizable, sizeRange.preferred.displayLabel())
                } else {
                    sizeRange.preferred.displayLabel()
                },
                color = Muted,
                fontSize = Dimens.sp10,
                modifier = Modifier.padding(top = Dimens.dp3),
            )
            badges?.let { providerBadges ->
                Text(
                    stringResource(providerBadges.fitLabelRes),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = Dimens.sp10,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Dimens.dp5),
                )
                if (providerBadges.needsSetup) {
                    Text(
                        stringResource(R.string.widget_needs_setup),
                        color = Muted,
                        fontSize = Dimens.sp10,
                        modifier = Modifier.padding(top = Dimens.dp2),
                    )
                }
            }
        }
    }
}
