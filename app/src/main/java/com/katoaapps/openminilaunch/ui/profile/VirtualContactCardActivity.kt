package com.katoaapps.openminilaunch.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
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
            data = Uri.Builder()
                .scheme("mink")
                .authority(context.packageName)
                .appendPath("contact-card")
                .build()
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS,
            )
        },
    )
}
