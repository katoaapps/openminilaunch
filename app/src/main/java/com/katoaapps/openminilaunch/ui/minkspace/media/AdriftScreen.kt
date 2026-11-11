package com.katoaapps.openminilaunch.ui.minkspace.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.media.MediaItem
import com.katoaapps.openminilaunch.features.minkspace.media.MediaPreviewLoader
import com.katoaapps.openminilaunch.ui.launcher.home.rememberHomeDateTimeParts
import com.katoaapps.openminilaunch.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.random.Random

private const val ADRIFT_FRAME_COUNT = 4
private const val ADRIFT_FRAME_FADE_MILLIS = 1_500L
private const val ADRIFT_FRAME_MIN_HOLD_MILLIS = 4_800L
private const val ADRIFT_FRAME_HOLD_VARIANCE_MILLIS = 2_800L
private const val ADRIFT_FRAME_STAGGER_MILLIS = 780L

private data class AdriftFrameContent(
    val item: MediaItem,
    val generation: Int,
    val imageAspectRatio: Float,
    val placement: AdriftFramePlacement,
)

@Composable
internal fun AdriftScreen(
    items: List<MediaItem>,
    use24HourClock: Boolean,
    onManageMedia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep this as a direct snapshot read so Memories changes update the scene without
    // recreating the total-screen host. Video is intentionally excluded from ambient playback.
    val ambientItems = items.filter(MediaItem::isImage).distinctBy { it.uri }
    val libraryKey = ambientItems.joinToString("|") { it.uri.toString() }.hashCode()
    // The library key keeps a deck stable while Adrift is open. A per-session seed prevents the
    // same library from producing the same opening sequence every time the user returns.
    val sessionSeed = rememberSaveable { Random.nextInt() }
    val context = LocalContext.current
    val aspectRatios by produceState<Map<String, Float>?>(
        initialValue = null,
        key1 = libraryKey,
    ) {
        value = withContext(Dispatchers.IO) {
            ambientItems.associate { item ->
                item.uri.toString() to normalizedAdriftAspectRatio(
                    MediaPreviewLoader.imageAspectRatio(context.applicationContext, item)
                        ?: (4f / 3f),
                )
            }
        }
    }
    val time = rememberHomeDateTimeParts(
        datePattern = "",
        showDate = false,
        showClock = true,
        use24HourClock = use24HourClock,
    ).time

    BoxWithConstraints(modifier.background(Color.Black)) {
        val sceneWidth = maxWidth
        val sceneHeight = maxHeight
        val screenAspectRatio = (sceneWidth.value / sceneHeight.value).coerceAtLeast(.1f)
        val landscape = screenAspectRatio >= 1f
        val sceneKey = libraryKey xor sessionSeed xor (screenAspectRatio * 10_000f).roundToInt()
        val resolvedAspectRatios = aspectRatios

        if (ambientItems.isNotEmpty() && resolvedAspectRatios != null) {
            val deck = remember(libraryKey, sessionSeed) {
                AdriftMemoryDeck(
                    sourceItems = ambientItems,
                    slotCount = ADRIFT_FRAME_COUNT,
                    seed = libraryKey xor sessionSeed,
                )
            }
            val frames = remember(deck, sceneKey, resolvedAspectRatios) {
                val occupied = mutableListOf<AdriftFramePlacement>()
                deck.currentItems().mapIndexed { slotIndex, item ->
                    val imageAspectRatio = resolvedAspectRatios[item.uri.toString()] ?: (4f / 3f)
                    val placement = planAdriftFramePlacement(
                        screenAspectRatio = screenAspectRatio,
                        imageAspectRatio = imageAspectRatio,
                        occupied = occupied,
                        previous = null,
                        seed = sceneKey + slotIndex * 137,
                    )
                    occupied += placement
                    AdriftFrameContent(
                        item = item,
                        generation = 0,
                        imageAspectRatio = imageAspectRatio,
                        placement = placement,
                    )
                }.toMutableStateList()
            }

            repeat(frames.size) { slotIndex ->
                AdriftMemoryFrame(
                    frame = frames[slotIndex],
                    slotIndex = slotIndex,
                    sceneKey = sceneKey,
                    sceneWidth = sceneWidth,
                    sceneHeight = sceneHeight,
                    onAdvance = {
                        val current = frames[slotIndex]
                        val nextItem = deck.advance(slotIndex)
                        val nextAspectRatio = resolvedAspectRatios[nextItem.uri.toString()] ?: (4f / 3f)
                        val occupied = frames
                            .filterIndexed { index, _ -> index != slotIndex }
                            .map(AdriftFrameContent::placement)
                        frames[slotIndex] = AdriftFrameContent(
                            item = nextItem,
                            generation = current.generation + 1,
                            imageAspectRatio = nextAspectRatio,
                            placement = planAdriftFramePlacement(
                                screenAspectRatio = screenAspectRatio,
                                imageAspectRatio = nextAspectRatio,
                                occupied = occupied,
                                previous = current.placement,
                                seed = sceneKey + current.generation * 101 + slotIndex * 17,
                            ),
                        )
                    },
                )
            }
        } else if (ambientItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = Dimens.dp145),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.adrift_add_memories),
                    color = Color.White.copy(alpha = .8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Dimens.dp32),
                )
                Button(
                    onClick = onManageMedia,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.padding(top = Dimens.dp14),
                ) {
                    Text(stringResource(R.string.open_memories))
                }
            }
        }

        // Draw the clock last so photos can use the center while always remaining behind it.
        Text(
            text = time,
            color = Color.White,
            fontSize = if (landscape) Dimens.sp56 else Dimens.sp64,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = .85f),
                    blurRadius = 18f,
                ),
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(Dimens.dp20),
        )
    }
}

@Composable
private fun AdriftMemoryFrame(
    frame: AdriftFrameContent,
    slotIndex: Int,
    sceneKey: Int,
    sceneWidth: Dp,
    sceneHeight: Dp,
    onAdvance: () -> Unit,
) {
    var started by remember(slotIndex, sceneKey) { mutableStateOf(false) }
    val latestAdvance by rememberUpdatedState(onAdvance)

    LaunchedEffect(slotIndex, sceneKey) {
        delay(slotIndex * ADRIFT_FRAME_STAGGER_MILLIS)
        started = true
        var generation = 0
        while (true) {
            val hold = randomizedHoldMillis(sceneKey, slotIndex, generation)
            delay(hold + ADRIFT_FRAME_FADE_MILLIS)
            latestAdvance()
            generation++
        }
    }

    AnimatedVisibility(
        visible = started,
        enter = fadeIn(tween(ADRIFT_FRAME_FADE_MILLIS.toInt())),
        modifier = Modifier.fillMaxSize(),
    ) {
        AnimatedContent(
            targetState = frame,
            transitionSpec = {
                fadeIn(tween(ADRIFT_FRAME_FADE_MILLIS.toInt())) togetherWith
                    fadeOut(tween(ADRIFT_FRAME_FADE_MILLIS.toInt()))
            },
            label = "Adrift memory $slotIndex",
            modifier = Modifier.fillMaxSize(),
        ) { targetFrame ->
            val placement = targetFrame.placement
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .offset(
                            x = sceneWidth * placement.xFraction,
                            y = sceneHeight * placement.yFraction,
                        )
                        .width(sceneWidth * placement.widthFraction)
                        .aspectRatio(targetFrame.imageAspectRatio)
                        .rotate(placement.rotationDegrees)
                        .shadow(Dimens.dp8, RoundedCornerShape(Dimens.dp8))
                        .background(Color.White, RoundedCornerShape(Dimens.dp8))
                        .border(Dimens.dp4, Color.White, RoundedCornerShape(Dimens.dp8))
                        .padding(Dimens.dp4),
                ) {
                    MediaPreview(
                        item = targetFrame.item,
                        fitCenter = true,
                        onClick = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private fun randomizedHoldMillis(sceneKey: Int, slotIndex: Int, generation: Int): Long {
    val random = Random(sceneKey xor (slotIndex * 0x45D9F3B) xor (generation * 0x119DE1F3))
    return ADRIFT_FRAME_MIN_HOLD_MILLIS +
        random.nextLong(ADRIFT_FRAME_HOLD_VARIANCE_MILLIS + 1L)
}
