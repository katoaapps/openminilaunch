package org.sableos.games.console

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import org.sableos.design.SableGlobalTheme
import org.sableos.games.SableGames

/** Sable Games Console: the library of Lua games, and the games themselves. */
class ConsoleActivity : ComponentActivity() {
    private val keys = ConsoleKeyHost()
    private lateinit var state: ConsoleState
    private val consumed = mutableSetOf<Int>()

    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) state.picked = uri
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SableGames.initialize(applicationContext)
        @Suppress("DEPRECATION")
        setTaskDescription(ActivityManager.TaskDescription(SableGames.entry("console")?.title))
        state =
            ConsoleState(
                library = ConsoleLibrary(this),
                progress = ConsoleProgress(this),
                tilt = ConsoleTilt(this),
                allowExternalGames = SableGames.allowsExternalConsoleGames(),
                pickFolder = { folderPicker.launch(null) },
            )
        setContent {
            SableGlobalTheme(window = window) {
                CompositionLocalProvider(LocalConsoleKeys provides keys) {
                    ConsoleApp(state)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        state.tilt.suspend()
        state.backgrounded++
    }

    override fun onResume() {
        super.onResume()
        state.tilt.resume()
    }

    // ComponentActivity.dispatchKeyEvent is a public framework override; lint flags it as androidx-restricted.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // A Back key (the Titan 2's own key) goes only to the back dispatcher, which hands it to the
        // screen once (ConsoleApp's BackHandler). Given to the screen here as well, one press went
        // back twice: Details to the library, then out of the app.
        if (!ConsoleKeys.toScreen(event.keyCode)) return super.dispatchKeyEvent(event)
        val taken =
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    (keys.handler?.invoke(event) == true).also { if (it) consumed += event.keyCode }
                }

                KeyEvent.ACTION_UP -> {
                    keys.handler?.invoke(event)
                    consumed.remove(event.keyCode)
                }

                else -> {
                    false
                }
            }
        return taken || super.dispatchKeyEvent(event)
    }
}
