package com.katoaapps.openminilaunch

import android.app.Application
import android.content.Context
import com.katoaapps.openminilaunch.features.minkboard.OpenMinkMinkBoardRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OpenMinkApplication : Application() {
    internal val minkBoardRuntime by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OpenMinkMinkBoardRuntime(this)
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { minkBoardRuntime.focusTimerCoordinator.reconcile() }
    }
}

internal val Context.openMinkMinkBoardRuntime: OpenMinkMinkBoardRuntime
    get() = (applicationContext as OpenMinkApplication).minkBoardRuntime
