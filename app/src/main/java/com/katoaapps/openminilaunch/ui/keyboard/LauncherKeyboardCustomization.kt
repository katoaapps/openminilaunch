package com.katoaapps.openminilaunch.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.model.LauncherKeyboardPreferences
import com.katoaapps.openminilaunch.model.resolveLauncherKeyboardHeightDp
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlin.math.min

@Composable
internal fun <T> KeyboardChoiceGrid(
    options: List<T>,
    columns: Int,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        options.chunked(columns).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
            ) {
                rowOptions.forEach { option ->
                    val isSelected = option == selected
                    Surface(
                        onClick = { onSelect(option) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(Dimens.dp14),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        border = if (isSelected) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label(option),
                                textAlign = TextAlign.Center,
                                fontSize = Dimens.sp11,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                            )
                        }
                    }
                }
                repeat(columns - rowOptions.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun KeyboardDevicePreview(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    preferences: LauncherKeyboardPreferences,
    themePrimaryArgb: Int,
    themeSecondaryArgb: Int,
    maxPreviewHeight: Dp = 420.dp,
) {
    val configuration = LocalConfiguration.current
    val deviceWidth = configuration.screenWidthDp.coerceAtLeast(1).dp
    val deviceHeight = configuration.screenHeightDp.coerceAtLeast(1).dp
    val previewPreferences = preferences.copy(
        heightDp = resolveLauncherKeyboardHeightDp(
            preferences = preferences,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
        ),
    )

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val scale = min(
            1f,
            min(maxWidth.value / deviceWidth.value, maxPreviewHeight.value / deviceHeight.value),
        )
        val previewWidth = deviceWidth * scale
        val previewHeight = deviceHeight * scale
        val frameShape = RoundedCornerShape(24.dp)

        Box(
            modifier = Modifier
                .size(previewWidth, previewHeight)
                .clip(frameShape)
                .background(MaterialTheme.colorScheme.background)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, frameShape),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                    .requiredSize(deviceWidth, deviceHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
                KeyboardPreviewScreen(
                    modifier = Modifier.fillMaxSize(),
                    panelColor = Color(themePrimaryArgb),
                )
                LauncherKeyboard(
                    value = value,
                    onValueChange = onValueChange,
                    preferences = previewPreferences,
                    themePrimaryArgb = themePrimaryArgb,
                    themeSecondaryArgb = themeSecondaryArgb,
                    onSubmit = {},
                    multiline = false,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun KeyboardPreviewScreen(
    modifier: Modifier,
    panelColor: Color,
) {
    Box(modifier.background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(.84f)
                .padding(top = 56.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(68.dp),
                color = panelColor.copy(alpha = .18f),
                shape = RoundedCornerShape(22.dp),
            ) {}
            Surface(
                modifier = Modifier.fillMaxWidth(.72f).height(18.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f),
                shape = RoundedCornerShape(9.dp),
            ) {}
            Surface(
                modifier = Modifier.fillMaxWidth(.5f).height(18.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f),
                shape = RoundedCornerShape(9.dp),
            ) {}
        }
    }
}
