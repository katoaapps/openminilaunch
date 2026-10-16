package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import com.katoaapps.openminilaunch.ui.minkpad.help.MinkPadHelpPageContent
import com.katoaapps.openminilaunch.ui.minkpad.help.minkPadHelpPages
import kotlinx.coroutines.launch

@Composable
internal fun MinkPadHelpDialog(platform: MinkPadHostPlatform, onSettings: () -> Unit, close: () -> Unit) {
    val pages = minkPadHelpPages(platform)
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val fontScale = LocalDensity.current.fontScale
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().pointerInput(close) { detectTapGestures { close() } }
                .safeDrawingPadding().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val dialogWidth = (maxWidth * .98f).coerceAtMost(960.dp)
            val dialogHeight = (maxHeight * .94f).coerceAtMost(760.dp)
            val compactHeader = maxHeight < 480.dp || fontScale > 1.5f
            // Outside taps dismiss; the inner teaching surface consumes its own background taps.
            Surface(
                modifier = Modifier.width(dialogWidth).height(dialogHeight)
                    .pointerInput(Unit) { detectTapGestures {} },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Trackpad help", style = MaterialTheme.typography.titleLarge)
                            if (compactHeader) Text(platform.label, style = MaterialTheme.typography.labelMedium)
                        }
                        if (compactHeader) IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Choose operating system or open settings")
                        }
                        IconButton(onClick = close) { Icon(Icons.Default.Close, contentDescription = "Close help") }
                    }
                    if (!compactHeader) TextButton(onClick = onSettings, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text("Operating System · ${platform.label} · Change")
                    }
                    HorizontalDivider()
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) { index ->
                        MinkPadHelpPageContent(pages[index])
                    }
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("${pager.currentPage + 1} of ${pages.size}",
                            style = MaterialTheme.typography.labelMedium)
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                            enabled = pager.currentPage > 0,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text("Previous") }
                        FilledTonalButton(
                            onClick = {
                                if (pager.currentPage == pages.lastIndex) close()
                                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(if (pager.currentPage == pages.lastIndex) "Done" else "Next") }
                    }
                }
            }
        }
    }
}
