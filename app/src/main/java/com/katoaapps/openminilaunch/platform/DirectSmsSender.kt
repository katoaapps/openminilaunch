package com.katoaapps.openminilaunch.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager

internal enum class DirectSmsResult {
    QUEUED,
    NO_DEFAULT_SUBSCRIPTION,
    NOT_AUTHORIZED,
    UNSUPPORTED,
    FAILED,
}

internal class DirectSmsSender(
    private val context: Context,
    private val assistantRoleHeld: () -> Boolean,
) {
    fun send(phone: String, body: String): DirectSmsResult {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            return DirectSmsResult.UNSUPPORTED
        }
        if (!assistantRoleHeld() || context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return DirectSmsResult.NOT_AUTHORIZED
        }
        val cleanBody = body.trim()
        if (phone.isBlank() || cleanBody.isBlank()) return DirectSmsResult.FAILED
        val subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId()
        if (subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return DirectSmsResult.NO_DEFAULT_SUBSCRIPTION
        }
        return runCatching {
            @Suppress("DEPRECATION")
            val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
            } else {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }
            val parts = manager.divideMessage(cleanBody)
            if (parts.size == 1) {
                manager.sendTextMessage(phone, null, cleanBody, null, null)
            } else {
                manager.sendMultipartTextMessage(phone, null, parts, null, null)
            }
            DirectSmsResult.QUEUED
        }.getOrDefault(DirectSmsResult.FAILED)
    }
}
