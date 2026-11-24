package com.katoaapps.openminilaunch.ui.launcher

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.katoaapps.openminilaunch.ui.magic.SelectedMessageRecipient
import com.katoaapps.openminilaunch.ui.magic.recipientFromSnapshot
import com.katoaapps.openminilaunch.ui.magic.snapshot

/** One explicit handoff from a Recents-visible Mink workspace to the excluded Home task. */
internal sealed interface MinkHomeRequest {
    val token: Long

    data class Message(
        override val token: Long,
        val recipient: SelectedMessageRecipient,
    ) : MinkHomeRequest

    data class FeatureTour(override val token: Long) : MinkHomeRequest

    data class MagicBoxTour(override val token: Long) : MinkHomeRequest

    data class QuickDrawerTour(override val token: Long) : MinkHomeRequest

    data class DemoHome(override val token: Long) : MinkHomeRequest

    data class DemoSearch(
        override val token: Long,
        val query: String,
    ) : MinkHomeRequest
}

internal object MinkHomeRequestIntent {
    private const val ACTION_OPEN_HOME =
        "com.katoaapps.openminilaunch.action.OPEN_HOME"
    private const val EXTRA_TYPE =
        "com.katoaapps.openminilaunch.extra.HOME_REQUEST_TYPE"
    private const val EXTRA_RECIPIENT =
        "com.katoaapps.openminilaunch.extra.HOME_RECIPIENT"
    private const val EXTRA_QUERY =
        "com.katoaapps.openminilaunch.extra.HOME_QUERY"

    private const val TYPE_MESSAGE = "message"
    private const val TYPE_FEATURE_TOUR = "feature_tour"
    private const val TYPE_MAGIC_BOX_TOUR = "magic_box_tour"
    private const val TYPE_QUICK_DRAWER_TOUR = "quick_drawer_tour"
    private const val TYPE_DEMO_HOME = "demo_home"
    private const val TYPE_DEMO_SEARCH = "demo_search"

    fun create(context: Context, request: MinkHomeRequest): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_HOME)
            .putExtra(
                EXTRA_TYPE,
                when (request) {
                    is MinkHomeRequest.Message -> TYPE_MESSAGE
                    is MinkHomeRequest.FeatureTour -> TYPE_FEATURE_TOUR
                    is MinkHomeRequest.MagicBoxTour -> TYPE_MAGIC_BOX_TOUR
                    is MinkHomeRequest.QuickDrawerTour -> TYPE_QUICK_DRAWER_TOUR
                    is MinkHomeRequest.DemoHome -> TYPE_DEMO_HOME
                    is MinkHomeRequest.DemoSearch -> TYPE_DEMO_SEARCH
                },
            )
            .apply {
                when (request) {
                    is MinkHomeRequest.Message ->
                        putExtra(EXTRA_RECIPIENT, request.recipient.snapshot())
                    is MinkHomeRequest.DemoSearch -> putExtra(EXTRA_QUERY, request.query)
                    is MinkHomeRequest.FeatureTour,
                    is MinkHomeRequest.MagicBoxTour,
                    is MinkHomeRequest.QuickDrawerTour,
                    is MinkHomeRequest.DemoHome,
                    -> Unit
                }
            }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

    fun parse(intent: Intent?): MinkHomeRequest? {
        if (intent?.action != ACTION_OPEN_HOME) return null
        val token = SystemClock.elapsedRealtimeNanos()
        return when (intent.getStringExtra(EXTRA_TYPE)) {
            TYPE_MESSAGE -> intent.getStringExtra(EXTRA_RECIPIENT)
                ?.let(::recipientFromSnapshot)
                ?.let { recipient -> MinkHomeRequest.Message(token, recipient) }
            TYPE_FEATURE_TOUR -> MinkHomeRequest.FeatureTour(token)
            TYPE_MAGIC_BOX_TOUR -> MinkHomeRequest.MagicBoxTour(token)
            TYPE_QUICK_DRAWER_TOUR -> MinkHomeRequest.QuickDrawerTour(token)
            TYPE_DEMO_HOME -> MinkHomeRequest.DemoHome(token)
            TYPE_DEMO_SEARCH -> MinkHomeRequest.DemoSearch(
                token = token,
                query = intent.getStringExtra(EXTRA_QUERY).orEmpty(),
            )
            else -> null
        }
    }
}
