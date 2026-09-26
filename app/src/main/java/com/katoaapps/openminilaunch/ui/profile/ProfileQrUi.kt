package com.katoaapps.openminilaunch.ui.profile

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.profile.ProfileQrCode
import com.katoaapps.openminilaunch.features.profile.ProfileQrGenerator
import com.katoaapps.openminilaunch.ui.theme.Dimens

@Composable
internal fun ProfileQrImage(
    code: ProfileQrCode,
    size: androidx.compose.ui.unit.Dp,
    description: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val pixels = with(density) { size.roundToPx() }
    val bitmap = remember(code.payload, pixels) { ProfileQrGenerator.render(code, pixels) }
    Image(bitmap.asImageBitmap(), description, modifier.size(size).background(Color.White))
}

@Composable
internal fun ProfileQrDialog(code: ProfileQrCode, fullName: String, onDismiss: () -> Unit) {
    val activity = LocalContext.current as Activity
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = onDismiss)
        DisposableEffect(activity) {
            val window = activity.window
            val oldBrightness = window.attributes.screenBrightness
            val wasKeepingScreenOn = window.attributes.flags and
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply { screenBrightness = 1f }
            onDispose {
                if (!wasKeepingScreenOn) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                window.attributes = window.attributes.apply { screenBrightness = oldBrightness }
            }
        }
        Box(Modifier.fillMaxSize().background(Color.White)) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.dp18),
            ) {
                Icon(Icons.Default.Close, stringResource(R.string.close), tint = Color.Black)
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = Dimens.dp24, vertical = Dimens.dp64),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val qrSize = minOf(maxWidth, maxHeight, 560.dp)
                    ProfileQrImage(code, qrSize, stringResource(R.string.profile_qr_code))
                }
                Text(
                    fullName,
                    Modifier.padding(top = Dimens.dp24),
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.scan_to_add_contact),
                    Modifier.padding(top = Dimens.dp6),
                    color = Color.DarkGray,
                )
            }
        }
    }
}
