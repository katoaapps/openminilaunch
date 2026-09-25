package com.katoaapps.openminilaunch.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme

/**
 * Keeps vCard editing in Recents so users can leave to copy details and return to their draft.
 */
class VirtualContactCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val store = LauncherStore.get(this)
        setContent {
            MinkLauncherTheme(store) {
                Surface(Modifier.fillMaxSize()) {
                    ProfileScreen(store = store, goBack = ::finish)
                }
            }
        }
    }
}

internal fun openVirtualContactCard(context: Context) {
    context.startActivity(
        Intent(context, VirtualContactCardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        },
    )
}
