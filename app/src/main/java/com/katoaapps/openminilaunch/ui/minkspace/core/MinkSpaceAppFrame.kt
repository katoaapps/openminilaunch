package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.ui.theme.Dimens

/** Hosts one mini-app and draws platform controls above content owned by that app. */
@Composable
internal fun MinkSpaceAppFrame(
    app: MinkSpaceApp,
    scope: MinkSpaceScope,
    modifier: Modifier = Modifier,
) {
    val expansion = scope.expansion
    Box(modifier) {
        app.Content(
            scope = scope,
            modifier = Modifier.fillMaxSize(),
        )

        if (app.manifest.showExpansionButton && !expansion.expanded) {
            MinkSpaceExpandButton(
                expansion = expansion,
                label = stringResource(
                    app.manifest.expansionButtonLabelRes ?: R.string.expand_view,
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Dimens.dp8),
            )
        }

        if (expansion.expanded) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.dp8),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = Dimens.dp2,
            ) {
                IconButton(
                    onClick = expansion::collapse,
                    modifier = Modifier.size(Dimens.dp48),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }
        }
    }
}
