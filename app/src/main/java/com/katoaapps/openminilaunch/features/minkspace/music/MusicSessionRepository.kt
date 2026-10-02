package com.katoaapps.openminilaunch.features.minkspace.music

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.features.conversations.MinkNotificationListenerService
import com.katoaapps.openminilaunch.features.conversations.NotificationHub

/**
 * Adapts Android's active media-session API into small, observable MinkSpace state.
 *
 * Notification-listener access is the platform-approved way for a launcher to query active
 * sessions. Commands are sent only when the selected session advertises matching capabilities.
 */
internal class MusicSessionRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionManager = appContext.getSystemService(MediaSessionManager::class.java)
    private val notificationListener = ComponentName(
        appContext,
        MinkNotificationListenerService::class.java,
    )

    private var observerCount = 0
    private var activeSessionsListenerRegistered = false
    private var controller: MediaController? = null
    private var registeredController: MediaController? = null

    var state by mutableStateOf(initialState())
        private set

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        selectController(controllers.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishControllerState()
        }

        override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
            publishControllerState()
        }

        override fun onSessionDestroyed() {
            refresh()
        }
    }

    private val positionTicker = object : Runnable {
        override fun run() {
            updatePositionOnly()
            if (state.hasSession && state.isPlaying && observerCount > 0) {
                mainHandler.postDelayed(this, POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    fun start() {
        observerCount += 1
        if (observerCount != 1) return
        refresh()
    }

    fun stop() {
        observerCount = (observerCount - 1).coerceAtLeast(0)
        if (observerCount != 0) return
        removeActiveSessionsListener()
        unregisterController()
        mainHandler.removeCallbacks(positionTicker)
    }

    fun refresh() {
        if (!NotificationHub.hasAccess(appContext)) {
            removeActiveSessionsListener()
            unregisterController()
            controller = null
            state = MusicPlaybackState(MusicPlaybackAvailability.ACCESS_REQUIRED)
            mainHandler.removeCallbacks(positionTicker)
            return
        }

        ensureActiveSessionsListener()
        val controllers = runCatching {
            sessionManager.getActiveSessions(notificationListener)
        }.getOrDefault(emptyList())
        selectController(controllers)
    }

    fun togglePlayback() {
        val activeController = controller ?: return
        val playback = activeController.playbackState ?: return
        if (!state.canTogglePlayback) return
        if (playback.isActivelyPlaying()) {
            activeController.transportControls.pause()
        } else {
            activeController.transportControls.play()
        }
    }

    fun seekBy(offsetMs: Long) {
        if (!state.canSeek) return
        val activeController = controller ?: return
        val currentPosition = activeController.playbackState.currentPosition(state.durationMs)
        val requestedPosition = currentPosition + offsetMs
        val boundedPosition = if (state.durationMs > 0L) {
            requestedPosition.coerceIn(0L, state.durationMs)
        } else {
            requestedPosition.coerceAtLeast(0L)
        }
        activeController.transportControls.seekTo(boundedPosition)
        state = state.copy(positionMs = boundedPosition)
    }

    fun seekToFraction(fraction: Float) {
        if (!state.canSeek || state.durationMs <= 0L) return
        val requestedPosition = (state.durationMs * fraction.coerceIn(0f, 1f)).toLong()
        controller?.transportControls?.seekTo(requestedPosition)
        state = state.copy(positionMs = requestedPosition)
    }

    fun restart() {
        if (!state.canSeek) return
        controller?.transportControls?.seekTo(0L)
        state = state.copy(positionMs = 0L)
    }

    fun skipPrevious() {
        if (state.canSkipPrevious) controller?.transportControls?.skipToPrevious()
    }

    fun skipNext() {
        if (state.canSkipNext) controller?.transportControls?.skipToNext()
    }

    /**
     * The framework MediaController has no universal shuffle command. Honor only an explicit
     * app-provided custom action whose action id or display name identifies itself as shuffle.
     */
    fun requestShuffle() {
        val activeController = controller ?: return
        val shuffleAction = activeController.playbackState.shuffleAction() ?: return
        activeController.transportControls.sendCustomAction(shuffleAction.action, null)
    }

    fun openPlayerApp(): Boolean {
        val activeController = controller
        val packageLaunchIntent = activeController?.let {
            appContext.packageManager.getLaunchIntentForPackage(it.packageName)
        }
        if (start(packageLaunchIntent)) return true
        if (activeController != null && send(activeController.sessionActivity)) return true
        return start(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
    }

    private fun start(intent: Intent?): Boolean {
        intent ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun selectController(controllers: List<MediaController>) {
        val selected = controllers.firstOrNull { it.playbackState.isActivelyPlaying() }
            ?: controllers.firstOrNull { it.hasMeaningfulMedia() }

        if (controller?.sessionToken == selected?.sessionToken) {
            controller = selected
            if (selected != null && registeredController == null) {
                registeredController = selected
                selected.registerCallback(controllerCallback, mainHandler)
            }
            publishControllerState()
            return
        }

        unregisterController()
        controller = selected
        registeredController = selected
        selected?.registerCallback(controllerCallback, mainHandler)
        publishControllerState()
    }

    private fun unregisterController() {
        registeredController?.unregisterCallback(controllerCallback)
        registeredController = null
    }

    private fun ensureActiveSessionsListener() {
        if (activeSessionsListenerRegistered || observerCount <= 0) return
        activeSessionsListenerRegistered = runCatching {
            sessionManager.addOnActiveSessionsChangedListener(
                activeSessionsListener,
                notificationListener,
                mainHandler,
            )
            true
        }.getOrDefault(false)
    }

    private fun removeActiveSessionsListener() {
        if (!activeSessionsListenerRegistered) return
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(activeSessionsListener) }
        activeSessionsListenerRegistered = false
    }

    private fun publishControllerState() {
        val activeController = controller
        if (activeController == null) {
            state = MusicPlaybackState(MusicPlaybackAvailability.NOTHING_PLAYING)
            mainHandler.removeCallbacks(positionTicker)
            return
        }

        state = activeController.toMusicPlaybackState(appContext, previousState = state)
        schedulePositionUpdates()
    }

    private fun updatePositionOnly() {
        val playback = controller?.playbackState ?: return
        state = state.copy(
            positionMs = playback.currentPosition(state.durationMs),
            isPlaying = playback.isActivelyPlaying(),
        )
    }

    private fun schedulePositionUpdates() {
        mainHandler.removeCallbacks(positionTicker)
        if (state.isPlaying && observerCount > 0) {
            mainHandler.postDelayed(positionTicker, POSITION_UPDATE_INTERVAL_MS)
        }
    }

    private fun initialState(): MusicPlaybackState = MusicPlaybackState(
        availability = if (NotificationHub.hasAccess(appContext)) {
            MusicPlaybackAvailability.NOTHING_PLAYING
        } else {
            MusicPlaybackAvailability.ACCESS_REQUIRED
        },
    )

    private fun send(pendingIntent: PendingIntent?): Boolean {
        pendingIntent ?: return false
        return runCatching {
            pendingIntent.send()
            true
        }.getOrDefault(false)
    }

    companion object {
        private const val POSITION_UPDATE_INTERVAL_MS = 500L

        @Volatile private var instance: MusicSessionRepository? = null

        fun get(context: Context): MusicSessionRepository = instance ?: synchronized(this) {
            instance ?: MusicSessionRepository(context).also { instance = it }
        }
    }
}
