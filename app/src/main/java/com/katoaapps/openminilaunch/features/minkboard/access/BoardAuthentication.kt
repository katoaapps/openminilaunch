package com.katoaapps.openminilaunch.features.minkboard.access

internal enum class BoardAuthenticationAvailability {
    Available,
    NoDeviceSecurity,
    TemporarilyUnavailable,
}

internal sealed interface BoardAuthenticationResult {
    data object Succeeded : BoardAuthenticationResult
    data object Canceled : BoardAuthenticationResult
    data class Failed(val message: String?) : BoardAuthenticationResult
}

internal interface BoardAuthenticator {
    fun availability(): BoardAuthenticationAvailability
    fun authenticate(onResult: (BoardAuthenticationResult) -> Unit)
    fun cancel()
}
