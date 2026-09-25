package com.katoaapps.openminilaunch.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.profile.ProfileQrCode
import com.katoaapps.openminilaunch.features.profile.ProfileQrDensity
import com.katoaapps.openminilaunch.features.profile.ProfileQrGenerator
import com.katoaapps.openminilaunch.features.profile.ProfileState
import com.katoaapps.openminilaunch.features.profile.resolveSelectedLinks
import com.katoaapps.openminilaunch.ui.profile.EmptyProfileCard
import com.katoaapps.openminilaunch.ui.profile.HomeVCardContent
import com.katoaapps.openminilaunch.ui.profile.ProfileQrDialog
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

private const val FLIP_MIDPOINT_DEGREES = 90f
private const val FLIPPED_DEGREES = 180f

@Composable
internal fun FlippableHomeProfilePanel(
    store: LauncherStore,
    compact: Boolean,
    resetKey: Int,
    openVCardSettings: () -> Unit,
    front: @Composable (showVCard: () -> Unit) -> Unit,
) {
    var angle by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var qrDialog by remember { mutableStateOf<ProfileQrCode?>(null) }
    val state = store.profileRepository.state
    val ready = state as? ProfileState.Ready
    val selectedLinks = ready?.let { it.card.resolveSelectedLinks(it.links) }.orEmpty()
    val qrCode = remember(ready) {
        ready?.let { ProfileQrGenerator.describe(it.card, it.links) }
            ?.takeUnless { it.density == ProfileQrDensity.TOO_DENSE }
    }

    fun settle(flipped: Boolean) {
        settleJob?.cancel()
        settleJob = scope.launch {
            Animatable(angle).animateTo(if (flipped) FLIPPED_DEGREES else 0f, spring()) {
                angle = value
            }
        }
    }

    fun flipTo(flipped: Boolean) {
        val currentlyFlipped = angle >= FLIP_MIDPOINT_DEGREES
        if (currentlyFlipped != flipped) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        settle(flipped)
    }

    LaunchedEffect(resetKey) {
        settleJob?.cancel()
        if (angle != 0f) Animatable(angle).animateTo(0f, spring()) { angle = value }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) settle(false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = angle > 0f) { flipTo(false) }

    val showProfileLabel = stringResource(R.string.show_profile)
    val showHomeLabel = stringResource(R.string.show_home)
    Box(
        Modifier.fillMaxSize()
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = if (angle < FLIP_MIDPOINT_DEGREES) showProfileLabel else showHomeLabel,
                    ) {
                        flipTo(angle < FLIP_MIDPOINT_DEGREES)
                        true
                    },
                )
            },
    ) {
        if (angle <= FLIP_MIDPOINT_DEGREES) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    rotationY = angle
                    cameraDistance = 18f * density.density
                },
            ) { front { flipTo(true) } }
        } else {
            Box(
                Modifier.fillMaxSize().background(Color.Black).graphicsLayer {
                    rotationY = angle - FLIPPED_DEGREES
                    cameraDistance = 18f * density.density
                },
            ) {
                if (ready == null || ready.card.fullName.isBlank()) {
                    EmptyProfileCard(
                        onCreateVCard = openVCardSettings,
                        compact = compact,
                    )
                } else {
                    HomeVCardContent(
                        store = store,
                        card = ready.card,
                        selectedLinks = selectedLinks,
                        qrCode = qrCode,
                        compact = compact,
                        onOpenQr = { qrCode?.let { qrDialog = it } },
                        onFixQr = openVCardSettings,
                    )
                }
                IconButton(
                    onClick = { flipTo(false) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.dp6),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.show_home),
                        tint = Color.White,
                    )
                }
            }
        }
    }

    qrDialog?.let { code ->
        ProfileQrDialog(code, ready?.card?.fullName.orEmpty()) { qrDialog = null }
    }
}
