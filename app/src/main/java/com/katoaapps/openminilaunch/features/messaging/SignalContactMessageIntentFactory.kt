package com.katoaapps.openminilaunch.features.messaging

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri

/** Builds the shared Signal/Molly system-contact handoff without fixing an activity class name. */
internal class SignalContactMessageIntentFactory(context: Context) {
    private val packageManager = context.packageManager

    fun directDraftIntent(packageName: String, e164Phone: String, body: String): Intent? {
        val contactEntryPoint = resolveContactEntryPoint(packageName) ?: return null
        return Intent(Intent.ACTION_VIEW)
            .setComponent(contactEntryPoint)
            .setDataAndType(
                "sms:$e164Phone?body=${Uri.encode(body.trim())}".toUri(),
                CONTACT_MIME_TYPE,
            )
    }

    private fun resolveContactEntryPoint(packageName: String): ComponentName? {
        val discoveryIntent = Intent(Intent.ACTION_VIEW)
            .setType(CONTACT_MIME_TYPE)
            .setPackage(packageName)
        return packageManager.queryIntentActivities(
            discoveryIntent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ).firstNotNullOfOrNull { result ->
            result.activityInfo
                ?.takeIf { it.exported && it.enabled && it.packageName == packageName }
                ?.let { ComponentName(it.packageName, it.name) }
        }
    }

    private companion object {
        const val CONTACT_MIME_TYPE =
            "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact"
    }
}
