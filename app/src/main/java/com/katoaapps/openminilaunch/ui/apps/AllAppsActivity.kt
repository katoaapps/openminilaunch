@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.apps

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.apps.*
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class AllAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val store = LauncherStore(this)
        val actions = DeviceActions(this)
        setContent {
            AllAppsTheme(store) {
                AllAppsScreen(
                    store = store,
                    actions = actions,
                    onClose = ::finish,
                )
            }
        }
    }
}

@Composable
private fun AllAppsTheme(
    store: LauncherStore,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (store.themePreference) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val fallback = if (darkTheme) {
        darkColorScheme(
            primary = DarkPrimary,
            onPrimary = DarkOnPrimary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceContainerLow = DarkSurfaceContainerLow,
            onSurface = DarkOnSurface,
            secondary = Rust,
        )
    } else {
        lightColorScheme(
            primary = LightInk,
            onPrimary = LightPaper,
            background = LightPaper,
            surface = LightPaper,
            surfaceContainerLow = MinkWhite,
            onSurface = LightInk,
            secondary = Rust,
        )
    }
    val baseColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        fallback
    }
    val colors = baseColors.withAppBackground(store.effectiveAppBackgroundColorArgb)
    val view = LocalView.current
    val transparent = MinkTransparent
    SideEffect {
        val activityWindow = (context as Activity).window
        activityWindow.statusBarColor = transparent.toArgb()
        activityWindow.navigationBarColor = transparent.toArgb()
        WindowInsetsControllerCompat(activityWindow, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}

@Composable
internal fun AllAppsScreen(
    store: LauncherStore,
    actions: DeviceActions,
    onClose: () -> Unit,
) {
    val appsState by produceState<List<LaunchableApp>?>(initialValue = null, actions) {
        value = withContext(Dispatchers.IO) { actions.installedApps() }
    }
    val apps = appsState.orEmpty()
    val pagerState = rememberPagerState(pageCount = { apps.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(apps) {
        if (apps.isNotEmpty()) pagerState.scrollToPage(initialAllAppsIndex(apps))
    }

    val focusedApp = apps.getOrNull(pagerState.currentPage)
    val fallbackAccent = Color(store.effectiveHomePanelColorArgb)
    val targetAccent = remember(focusedApp?.packageName, fallbackAccent) {
        focusedApp?.let { dominantAppColor(actions.appIcon(it.packageName)) } ?: fallbackAccent
    }
    val accent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 420),
        label = "all-apps-accent",
    )
    val centerGlow = lerpColor(accent, Color.White, .34f)
    val edgeColor = lerpColor(accent, Color.Black, .68f)
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < Dimens.dp560
        val availableWidth = maxWidth
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(centerGlow, accent, edgeColor),
                    radius = with(density) { availableWidth.toPx() } * .88f,
                ),
            ),
        ) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            ) {
            Row(
                Modifier.fillMaxWidth().height(if (compact) Dimens.dp48 else Dimens.dp64)
                    .padding(horizontal = Dimens.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.all_apps).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = Dimens.sp1_5,
                    )
                    Text(
                        stringResource(R.string.all_apps_hint),
                        color = Color.White.copy(alpha = .72f),
                        fontSize = Dimens.sp11,
                    )
                }
                if (apps.isNotEmpty()) {
                    Text(
                        stringResource(R.string.app_position, pagerState.currentPage + 1, apps.size),
                        color = Color.White.copy(alpha = .72f),
                        fontSize = Dimens.sp12,
                    )
                }
            }

            when {
                appsState == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                apps.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.Apps, null, Modifier.size(Dimens.dp64), tint = Color.White)
                    Text(
                        stringResource(R.string.no_apps_available),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = Dimens.dp14),
                    )
                }
                else -> {
                    val pageWidth = Dimens.dp116
                    val iconSize = if (compact) Dimens.dp72 else Dimens.dp140
                    HorizontalPager(
                        state = pagerState,
                        pageSize = PageSize.Fixed(pageWidth),
                        contentPadding = PaddingValues(horizontal = (availableWidth - pageWidth) / 2),
                        pageSpacing = if (compact) Dimens.dp4 else Dimens.dp8,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { page ->
                        val pageOffset = abs(
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction,
                        ).coerceIn(0f, 1f)
                        val scale = lerpFloat(.60f, 1f, 1f - pageOffset)
                        val app = apps[page]
                        AppCarouselItem(
                            app = app,
                            actions = actions,
                            iconSize = iconSize,
                            scale = scale,
                            focused = page == pagerState.currentPage,
                            onClick = {
                                if (page == pagerState.currentPage) {
                                    actions.launchPackage(app.packageName)
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(page) }
                                }
                            },
                        )
                    }
                    LetterArc(
                        availableLetters = remember(apps) {
                            apps.mapNotNull { letterForApp(it) }.toSet()
                        },
                        selectedLetter = letterForApp(focusedApp),
                        selectedContentColor = edgeColor,
                        compact = compact,
                        onLetter = { letter ->
                            appIndexForLetter(apps, letter)?.let { index ->
                                scope.launch { pagerState.scrollToPage(index) }
                            }
                        },
                    )
                }
            }
        }
    }
}
}
