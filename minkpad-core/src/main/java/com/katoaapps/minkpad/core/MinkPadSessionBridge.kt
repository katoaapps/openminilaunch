package com.katoaapps.minkpad.core

import android.content.Context

/** App-owned lifecycle operations used by the controller without depending on an app service class. */
interface MinkPadSessionBridge {
    fun startForegroundSession(context: Context): Boolean
    fun stopSession(context: Context)
    fun publishState(context: Context)
}

object UnconfiguredMinkPadSessionBridge : MinkPadSessionBridge {
    override fun startForegroundSession(context: Context) = false
    override fun stopSession(context: Context) = Unit
    override fun publishState(context: Context) = Unit
}
