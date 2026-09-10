package com.katoaapps.openminilaunch.features.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.net.toUri
import com.katoaapps.openminilaunch.model.looksLikePhoneRecipient
import java.util.Locale

/** Builds provider-owned message routes in preferred-to-fallback order. */
internal class MessagingIntentFactory(private val context: Context) {
    private val signalContactIntents = SignalContactMessageIntentFactory(context)

    fun routes(
        provider: MessagingDraftProvider,
        recipient: String,
        body: String,
        packageName: String = provider.packageName,
        forcedRecipient: Boolean = false,
    ): List<ProviderMessageIntent> {
        val cleanBody = body.trim()
        val internationalPhone = recipient
            .takeUnless { forcedRecipient && !it.looksLikePhoneRecipient() }
            ?.let(::internationalPhoneNumber)
        if (provider.kind == MessagingDraftKind.SIGNAL_CONTACT) {
            val directDraft = internationalPhone?.let { e164Phone ->
                signalContactIntents.directDraftIntent(packageName, e164Phone, cleanBody)
            }
            return listOfNotNull(
                directDraft?.let { ProviderMessageIntent(it, carriesRecipient = true) },
                ProviderMessageIntent(
                    genericShareIntent(packageName, cleanBody),
                    carriesRecipient = false,
                ),
            )
        }

        val uri = when (provider.kind) {
            MessagingDraftKind.WHATSAPP -> internationalPhone?.filter(Char::isDigit)?.let { digits ->
                Uri.Builder()
                    .scheme("https")
                    .authority("wa.me")
                    .appendPath(digits)
                    .appendQueryParameter("text", cleanBody)
                    .build()
            }
            MessagingDraftKind.TELEGRAM -> telegramRecipientUri(
                recipient,
                internationalPhone,
                cleanBody,
            )
            MessagingDraftKind.LINE -> Uri.Builder()
                .scheme("https")
                .authority("line.me")
                .appendPath("R")
                .appendPath("share")
                .appendQueryParameter("text", cleanBody)
                .build()
            MessagingDraftKind.SMS_URI -> "smsto:${Uri.encode(recipient)}".toUri()
            MessagingDraftKind.GENERIC_SHARE -> null
            MessagingDraftKind.SIGNAL_CONTACT -> error("Handled above")
        }
        val routes = when {
            provider.kind == MessagingDraftKind.GENERIC_SHARE -> listOf(
                ProviderMessageIntent(
                    genericShareIntent(packageName, cleanBody),
                    carriesRecipient = false,
                ),
            )
            uri == null -> emptyList()
            else -> {
                val action = if (provider.kind == MessagingDraftKind.SMS_URI) {
                    Intent.ACTION_SENDTO
                } else {
                    Intent.ACTION_VIEW
                }
                val intent = Intent(action, uri)
                    .setPackage(packageName)
                    .apply {
                        if (provider.kind == MessagingDraftKind.SMS_URI) {
                            putExtra("sms_body", cleanBody)
                        }
                    }
                listOf(
                    ProviderMessageIntent(
                        intent,
                        carriesRecipient = provider.supportTier != MessagingSupportTier.RECIPIENT_IN_APP,
                    ),
                )
            }
        }
        return routes.withForcedRecipientFallback(forcedRecipient, packageName, cleanBody)
    }

    private fun telegramRecipientUri(
        recipient: String,
        internationalPhone: String?,
        body: String,
    ): Uri? {
        val builder = Uri.Builder().scheme("tg").authority("resolve")
        if (internationalPhone != null) {
            builder.appendQueryParameter("phone", internationalPhone)
        } else {
            val username = recipient.trim().removePrefix("@").takeIf(String::isNotBlank)
                ?: return null
            builder.appendQueryParameter("domain", username)
        }
        return builder.appendQueryParameter("text", body).build()
    }

    private fun List<ProviderMessageIntent>.withForcedRecipientFallback(
        forcedRecipient: Boolean,
        packageName: String,
        body: String,
    ): List<ProviderMessageIntent> {
        if (!forcedRecipient || any { it.intent.action == Intent.ACTION_SEND }) return this
        return this + ProviderMessageIntent(
            intent = genericShareIntent(packageName, body),
            carriesRecipient = false,
        )
    }

    private fun genericShareIntent(packageName: String, body: String): Intent =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, body)
            .setPackage(packageName)

    private fun internationalPhoneNumber(phone: String): String? {
        val normalized = PhoneNumberUtils.normalizeNumber(phone)
        if (normalized.startsWith("+") && normalized.drop(1).all(Char::isDigit)) {
            return normalized
        }
        val telephony = context.getSystemService(TelephonyManager::class.java)
        val countryIso = telephony?.networkCountryIso
            ?.takeIf(String::isNotBlank)
            ?: telephony?.simCountryIso?.takeIf(String::isNotBlank)
            ?: Locale.getDefault().country.takeIf(String::isNotBlank)
            ?: return null
        return PhoneNumberUtils.formatNumberToE164(phone, countryIso.uppercase(Locale.US))
    }
}

internal data class ProviderMessageIntent(
    val intent: Intent,
    val carriesRecipient: Boolean,
)
