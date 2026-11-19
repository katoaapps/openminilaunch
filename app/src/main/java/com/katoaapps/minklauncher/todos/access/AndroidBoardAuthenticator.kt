package com.katoaapps.minklauncher.todos.access

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Launcher-owned Android credential adapter; the shared board modules remain UI-host agnostic. */
internal class AndroidBoardAuthenticator(
    private val activity: FragmentActivity,
    private val promptTitle: String,
    private val promptDescription: String,
) : BoardAuthenticator {
    private var activeCallback: ((BoardAuthenticationResult) -> Unit)? = null

    private val authenticators: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                finish(BoardAuthenticationResult.Succeeded)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                finish(
                    when (errorCode) {
                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        -> BoardAuthenticationResult.Canceled

                        else -> BoardAuthenticationResult.Failed(errString.toString())
                    },
                )
            }
        },
    )

    override fun availability(): BoardAuthenticationAvailability = when (
        BiometricManager.from(activity).canAuthenticate(authenticators)
    ) {
        BiometricManager.BIOMETRIC_SUCCESS -> BoardAuthenticationAvailability.Available
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        -> BoardAuthenticationAvailability.NoDeviceSecurity

        else -> BoardAuthenticationAvailability.TemporarilyUnavailable
    }

    override fun authenticate(onResult: (BoardAuthenticationResult) -> Unit) {
        if (availability() != BoardAuthenticationAvailability.Available) {
            onResult(BoardAuthenticationResult.Failed(null))
            return
        }
        activeCallback = onResult
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(promptTitle)
                .setDescription(promptDescription)
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }

    override fun cancel() {
        prompt.cancelAuthentication()
    }

    private fun finish(result: BoardAuthenticationResult) {
        val callback = activeCallback ?: return
        activeCallback = null
        callback(result)
    }
}
