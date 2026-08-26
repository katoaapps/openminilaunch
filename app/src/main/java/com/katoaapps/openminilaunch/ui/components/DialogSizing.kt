package com.katoaapps.openminilaunch.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Keeps dialogs roomy on phones without allowing them to become unwieldy on larger displays. */
internal object MinkDialogDefaults {
    const val WidthFraction = 0.90f

    val properties: DialogProperties
        get() = DialogProperties(usePlatformDefaultWidth = false)
}

@Composable
internal fun Modifier.minkDialogWidth(): Modifier =
    fillMaxWidth(MinkDialogDefaults.WidthFraction).widthIn(max = Dimens.dp560)
