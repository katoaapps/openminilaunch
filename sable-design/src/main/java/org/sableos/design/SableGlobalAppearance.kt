package org.sableos.design

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

object SableGlobalAppearanceContract {
    const val AUTHORITY = "org.sableos.appearance"
    const val PATH_APPEARANCE = "appearance"
    const val COLUMN_MODE = "mode"
    const val COLUMN_ACCENT = "accent"

    /**
     * Sable app corner style (compact, rounded). Written only by Settings and App display
     * compatibility; a provider without the column reads as Compact.
     */
    const val COLUMN_CORNER_STYLE = "corner_style"

    val CONTENT_URI: Uri =
        Uri.parse("content://$AUTHORITY/$PATH_APPEARANCE")
}

fun readGlobalSableAppearance(context: Context): SableAppearance =
    runCatching {
        context.contentResolver
            .query(
                SableGlobalAppearanceContract.CONTENT_URI,
                arrayOf(
                    SableGlobalAppearanceContract.COLUMN_MODE,
                    SableGlobalAppearanceContract.COLUMN_ACCENT,
                    SableGlobalAppearanceContract.COLUMN_CORNER_STYLE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use SableAppearance()
                }

                val mode =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            SableGlobalAppearanceContract.COLUMN_MODE,
                        ),
                    )
                val accent =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            SableGlobalAppearanceContract.COLUMN_ACCENT,
                        ),
                    )

                val cornerIndex =
                    cursor.getColumnIndex(
                        SableGlobalAppearanceContract.COLUMN_CORNER_STYLE,
                    )
                val cornerStyle =
                    if (cornerIndex >= 0) cursor.getString(cornerIndex) else null

                SableAppearance(
                    mode = AppearanceMode.fromStableValue(mode),
                    accent = AccentPreset.fromStableValue(accent),
                    cornerStyle = SableCornerStyle.fromStableValue(cornerStyle),
                )
            }
            ?: SableAppearance()
    }.getOrDefault(SableAppearance())

fun writeGlobalSableAppearance(
    context: Context,
    appearance: SableAppearance,
): Boolean =
    runCatching {
        val values =
            ContentValues().apply {
                put(
                    SableGlobalAppearanceContract.COLUMN_MODE,
                    appearance.mode.stableValue,
                )
                put(
                    SableGlobalAppearanceContract.COLUMN_ACCENT,
                    appearance.accent.stableValue,
                )
            }

        context.contentResolver.update(
            SableGlobalAppearanceContract.CONTENT_URI,
            values,
            null,
            null,
        ) == 1
    }.getOrDefault(false)

/**
 * Stores the Sable app corner style. Only Settings and App display compatibility may write
 * it; any other caller gets false. [writeGlobalSableAppearance] never writes this column, so
 * an appearance reset from Sable Start leaves the user's corner style alone.
 */
fun writeGlobalSableCornerStyle(
    context: Context,
    style: SableCornerStyle,
): Boolean =
    runCatching {
        val values =
            ContentValues().apply {
                put(
                    SableGlobalAppearanceContract.COLUMN_CORNER_STYLE,
                    style.stableValue,
                )
            }

        context.contentResolver.update(
            SableGlobalAppearanceContract.CONTENT_URI,
            values,
            null,
            null,
        ) == 1
    }.getOrDefault(false)

@Composable
fun rememberGlobalSableAppearance(context: Context = LocalContext.current): SableAppearance {
    var appearance by remember(context) {
        mutableStateOf(readGlobalSableAppearance(context))
    }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer =
            object : ContentObserver(
                Handler(Looper.getMainLooper()),
            ) {
                override fun onChange(selfChange: Boolean) {
                    appearance = readGlobalSableAppearance(context)
                }
            }

        runCatching {
            resolver.registerContentObserver(
                SableGlobalAppearanceContract.CONTENT_URI,
                false,
                observer,
            )
        }

        onDispose {
            runCatching {
                resolver.unregisterContentObserver(observer)
            }
        }
    }

    return appearance
}

@Composable
fun SableGlobalTheme(
    window: Window? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val appearance = rememberGlobalSableAppearance(context)

    SideEffect {
        window?.let {
            applySableSystemBars(
                window = it,
                darkTheme =
                    resolveSableDarkAppearance(
                        context = context,
                        appearance = appearance,
                    ),
            )
        }
    }

    SableTheme(appearance = appearance) {
        if (window == null) {
            content()
        } else {
            SableSafeArea(content)
        }
    }
}

/**
 * Every Sable app screen sits between the status bar and the navigation or gesture bar, and above
 * the keyboard and any display cutout. Android 15+ draws apps edge to edge, so an app that did not
 * pad itself drew under the status bar (Calendar on the Q25, owner report 2026-11-12). The strips
 * behind the bars take the app's background; a screen inside that pads itself again finds the
 * insets already used, so nothing is padded twice. When an app hides the bars (a full-screen
 * reader or video), the insets are zero and the screen fills the display.
 */
@Composable
private fun SableSafeArea(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
        ) {
            content()
        }
    }
}
