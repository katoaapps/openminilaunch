package com.katoaapps.openminilaunch.ui.minkspace.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.ui.launcher.home.ConversationAttentionButton
import com.katoaapps.openminilaunch.ui.launcher.home.rememberPendingFavoriteAttention
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkFeatureColors
import kotlinx.coroutines.delay

private const val TOTAL_SCREEN_CHROME_TIMEOUT_MILLIS = 4_000L

/** Full-window shell owned by MinkSpace. Mini-apps provide content but never own its exits. */
@Composable
internal fun MinkSpaceTotalScreenHost(
    store: LauncherStore,
    app: MinkSpaceTotalScreenApp,
    onDismiss: () -> Unit,
    onOpenConversations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val view = LocalView.current
    val window = activity?.window
    var chromeVisible by remember { mutableStateOf(true) }
    val conversations = ConversationNotificationStore.currentConversations()
    val favoriteAttention = rememberPendingFavoriteAttention(
        store = store,
        conversations = conversations,
        defaultColorArgb = MinkFeatureColors.ConversationAttention.toArgb(),
    )
    val backgroundInteractionSource = remember { MutableInteractionSource() }

    BackHandler(onBack = onDismiss)

    DisposableEffect(view, window, activity) {
        val previouslyKeptAwake = view.keepScreenOn
        val previousOrientation = activity?.requestedOrientation
        val previousCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window?.attributes?.layoutInDisplayCutoutMode
        } else {
            null
        }
        val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }
        view.keepScreenOn = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        onDispose {
            view.keepScreenOn = previouslyKeptAwake
            if (previousOrientation != null) activity.requestedOrientation = previousOrientation
            insetsController?.apply {
                if (store.hideStatusBar) hide(WindowInsetsCompat.Type.statusBars())
                else show(WindowInsetsCompat.Type.statusBars())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && previousCutoutMode != null) {
                window?.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = previousCutoutMode
                }
            }
        }
    }

    // MiniLaunchApp also owns the normal Home bars. Reassert total-screen behavior after
    // configuration changes so a rotation cannot briefly restore the status bar.
    SideEffect {
        window?.let { currentWindow ->
            WindowInsetsControllerCompat(currentWindow, view).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.statusBars())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentWindow.attributes = currentWindow.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
    }
    LaunchedEffect(chromeVisible) {
        if (chromeVisible) {
            delay(TOTAL_SCREEN_CHROME_TIMEOUT_MILLIS)
            chromeVisible = false
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = backgroundInteractionSource,
                indication = null,
                onClick = {},
            )
            .navigationBarsPadding(),
    ) {
        app.TotalScreenContent(
            scope = MinkSpaceTotalScreenScope(
                use24HourClock = store.use24HourClock,
            ),
            modifier = Modifier.fillMaxSize(),
        )

        // This transparent strip is the shell-owned way to recover hidden chrome.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .height(Dimens.dp64)
                .clickable { chromeVisible = true },
        )

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = .42f))
                    .statusBarsPadding()
                    .height(Dimens.dp64),
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = Dimens.dp6)
                        .size(Dimens.dp52),
                ) {
                    Icon(
                        Icons.Default.Close,
                        stringResource(R.string.close),
                        tint = Color.White,
                    )
                }
                Text(
                    text = stringResource(
                        app.manifest.totalScreenLabelRes ?: app.manifest.labelRes,
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        if (favoriteAttention.isNotEmpty()) {
            ConversationAttentionButton(
                conversationCount = favoriteAttention.size,
                attentionColors = favoriteAttention.map { Color(it.colorArgb) },
                actionSize = Dimens.dp56,
                iconSize = Dimens.dp25,
                restingColor = Color.White,
                // Conversations temporarily replaces Home. The presentation state remains
                // active so Back returns to Adrift; explicit compose actions dismiss it.
                onClick = onOpenConversations,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = Dimens.dp4, end = Dimens.dp8),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
