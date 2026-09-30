package com.katoaapps.openminilaunch.features.workprofile

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.katoaapps.openminilaunch.features.privatespace.AndroidProfileClassifier
import com.katoaapps.openminilaunch.features.privatespace.AndroidProfileKind

internal data class WorkProfileSnapshot(
    val available: Boolean,
    val paused: Boolean,
)

/** Reads and changes managed-profile availability without mixing it with Private Space. */
internal class WorkProfileRepository private constructor(context: Context) {
    private val launcherApps = context.applicationContext.getSystemService(LauncherApps::class.java)
    private val userManager = context.applicationContext.getSystemService(UserManager::class.java)
    private val profileClassifier = AndroidProfileClassifier(launcherApps)

    fun snapshot(): WorkProfileSnapshot {
        val profiles = workProfiles()
        return WorkProfileSnapshot(
            available = profiles.isNotEmpty(),
            paused = profiles.isNotEmpty() && profiles.all(::isPaused),
        )
    }

    fun setPaused(paused: Boolean): Boolean {
        val profiles = workProfiles()
        if (profiles.isEmpty()) return false
        return profiles.all { profile ->
            if (isPaused(profile) == paused) {
                true
            } else {
                runCatching {
                    // A false result while resuming can mean Android opened its credential UI.
                    userManager?.requestQuietModeEnabled(paused, profile)
                    true
                }.getOrDefault(false)
            }
        }
    }

    private fun workProfiles(): List<UserHandle> = runCatching {
        launcherApps?.profiles.orEmpty().filter { profile ->
            profile != Process.myUserHandle() &&
                profileClassifier.classify(profile) == AndroidProfileKind.OTHER_VISIBLE
        }
    }.getOrDefault(emptyList())

    private fun isPaused(profile: UserHandle): Boolean = runCatching {
        userManager?.isQuietModeEnabled(profile) == true
    }.getOrDefault(false)

    companion object {
        @Volatile private var instance: WorkProfileRepository? = null

        fun get(context: Context): WorkProfileRepository = instance ?: synchronized(this) {
            instance ?: WorkProfileRepository(context).also { instance = it }
        }
    }
}
