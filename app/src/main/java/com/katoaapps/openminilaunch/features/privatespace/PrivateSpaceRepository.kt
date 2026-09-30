package com.katoaapps.openminilaunch.features.privatespace

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Standard Private Space and certified OEM gateways, isolated from the normal app library. */
internal class PrivateSpaceRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val roleManager = appContext.getSystemService(RoleManager::class.java)
    private val oemGateways = OemPrivateContainerGatewayResolver(appContext)
    private val platform = AndroidPrivateSpacePlatform(appContext)
    private val revisionState = MutableStateFlow(0L)
    val revision: StateFlow<Long> = revisionState.asStateFlow()
    private val changeMonitor = PrivateSpaceChangeMonitor(appContext, ::refresh)

    fun snapshot(): PrivateSpaceSnapshot {
        if (roleManager?.isRoleHeld(RoleManager.ROLE_HOME) != true) {
            return PrivateSpaceSnapshot(PrivateSpaceStatus.HOME_ROLE_REQUIRED)
        }

        oemGateways.resolve()?.let { gateway ->
            return PrivateSpaceSnapshot(
                status = PrivateSpaceStatus.OEM_GATEWAY,
                gateway = gateway,
            )
        }

        return platform.snapshot()
    }

    /**
     * Only advertise a container Mink can actually reach. Some OEMs expose fragments of the
     * Android contract while suppressing setup, so a settings IntentSender alone is not proof.
     */
    fun isEntryPointAvailable(): Boolean {
        if (roleManager?.isRoleHeld(RoleManager.ROLE_HOME) != true) return false
        if (oemGateways.resolve() != null) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false
        return platform.isConfigured()
    }

    fun openSettings(context: Context): Boolean = platform.openSettings(context)

    fun setLocked(locked: Boolean): Boolean = platform.setLocked(locked).also { refresh() }

    fun launch(app: PrivateSpaceApp): Boolean = platform.launch(app)

    fun launchGateway(gateway: PrivateSpaceGateway): Boolean = runCatching {
        val intent = appContext.packageManager.getLaunchIntentForPackage(gateway.packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
        true
    }.getOrDefault(false)

    fun refresh() {
        revisionState.update { it + 1 }
    }

    companion object {
        @Volatile private var instance: PrivateSpaceRepository? = null

        fun get(context: Context): PrivateSpaceRepository = instance ?: synchronized(this) {
            instance ?: PrivateSpaceRepository(context).also { instance = it }
        }
    }
}
