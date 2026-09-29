package com.katoaapps.openminilaunch.features.bubbles

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.LauncherAppTarget

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/** Starts the requested launcher activity from inside Android's bubble task. */
class AppBubbleHostActivity : Activity() {
    private var targetStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetStarted = savedInstanceState?.getBoolean(STATE_TARGET_STARTED) == true
    }

    override fun onResume() {
        super.onResume()
        if (targetStarted) {
            finish()
            return
        }
        targetStarted = true
        val component = intent.getStringExtra(EXTRA_TARGET_COMPONENT)
            ?.let(ComponentName::unflattenFromString)
        if (component == null || !startTarget(component)) {
            Toast.makeText(this, R.string.app_bubble_launch_failed, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_TARGET_STARTED, targetStarted)
    }

    private fun startTarget(component: ComponentName): Boolean = runCatching {
        startActivity(Intent.makeMainActivity(component))
        true
    }.onFailure { error ->
        Log.w(TAG, "Unable to start $component inside the app bubble", error)
    }.getOrDefault(false)

    companion object {
        private const val EXTRA_TARGET_COMPONENT = "app_bubble_target_component"
        private const val STATE_TARGET_STARTED = "app_bubble_target_started"
        private const val TAG = "MinkAppBubbles"

        fun intentFor(context: Context, target: LauncherAppTarget): Intent =
            Intent(context, AppBubbleHostActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.Builder()
                    .scheme("mink-app-bubble")
                    .authority(target.packageName)
                    .path(target.selectionKey.hashCode().toUInt().toString(16))
                    .build()
                putExtra(EXTRA_TARGET_COMPONENT, target.componentName.flattenToString())
            }
    }
}
