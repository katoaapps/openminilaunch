package com.katoaapps.openminilaunch.features.messaging

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.ContactResult

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Owns installed-provider discovery and Android intent handoffs for Magic Box messages.
 *
 * DeviceActions delegates here so package-specific behavior stays separate from unrelated
 * launcher actions. The callbacks preserve DeviceActions' shared label cache and activity start
 * behavior.
 */
internal class MessagingDeviceActions(
    private val context: Context,
    private val appLabel: (String) -> String,
    private val startActivity: (Intent, Boolean) -> Boolean,
) {
    /** Resolves the curated catalog and keeps missing providers as disabled picker rows. */
    fun providerOptions(): List<MessagingProviderOption> {
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
        val systemOption = MessagingProviderOption(
            id = MessagingProviderCatalog.SYSTEM_DEFAULT_PROVIDER_ID,
            label = defaultPackage?.let(appLabel) ?: context.getString(R.string.system_messages),
            preferencePackageName = null,
            installedPackageName = defaultPackage,
            supportTier = MessagingSupportTier.CONTACT_AND_DRAFT,
            bundledIconRes = null,
            installed = true,
            selectable = true,
            systemDefault = true,
        )
        val providerOptions = MessagingProviderCatalog.providers.map { provider ->
            // The first installed package wins when a provider publishes more than one official
            // build. Keep this order stable because it also controls preference migration.
            val installedPackage = provider.resolveInstalledPackage(::isPackageInstalled)
            val selectable = installedPackage != null && preferredMessageIntent(
                provider = provider,
                phone = "+15551234567",
                body = "MinkLauncher",
                packageName = installedPackage,
            )?.let(::canResolve) == true
            MessagingProviderOption(
                id = provider.id,
                label = installedPackage?.let(appLabel) ?: context.getString(provider.labelRes),
                preferencePackageName = installedPackage ?: provider.packageName,
                installedPackageName = installedPackage,
                supportTier = provider.supportTier,
                bundledIconRes = provider.bundledIconRes,
                installed = installedPackage != null,
                selectable = selectable,
            )
        }
        return listOf(systemOption) + providerOptions.sortedWith(
            compareBy<MessagingProviderOption> {
                it.supportTier != MessagingSupportTier.CONTACT_AND_DRAFT
            }.thenBy {
                it.supportTier == MessagingSupportTier.RECIPIENT_IN_APP
            }.thenBy {
                it.label.lowercase()
            },
        )
    }

    fun defaultMessagingAppLabel(): String = Telephony.Sms.getDefaultSmsPackage(context)
        ?.let(appLabel)
        ?: context.getString(R.string.system_messages)

    /**
     * Opens a provider-owned draft and falls back to the system SMS composer if the saved
     * integration is missing or no longer accepts its documented intent.
     */
    fun openPreferredMessageDraft(
        contact: ContactResult,
        body: String,
        preferredPackage: String?,
    ): PreferredMessageDraftResult {
        val provider = MessagingProviderCatalog.providerForPackage(preferredPackage)
        if (provider != null) {
            val targetPackage = preferredPackage ?: provider.packageName
            val intent = preferredMessageIntent(provider, contact.phone, body, targetPackage)
            if (intent != null && canResolve(intent) && startActivity(intent, false)) {
                return if (provider.supportTier == MessagingSupportTier.RECIPIENT_IN_APP) {
                    PreferredMessageDraftResult.OPENED_WITH_RECIPIENT_PICKER
                } else {
                    PreferredMessageDraftResult.OPENED
                }
            }
        }
        return defaultMessageDraftResult(
            integratedPackage = preferredPackage,
            opened = openDefaultMessageDraft(contact, body),
        )
    }

    /** Android's share sheet carries the body, but messaging apps choose their own recipient. */
    fun chooseMessagingApp(body: String): Boolean {
        val genericShare = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, body.trim())
        return hasHandler(genericShare) && startActivity(genericShare, true)
    }

    /** Builds only contracts verified for the provider tier recorded in the catalog. */
    private fun preferredMessageIntent(
        provider: MessagingDraftProvider,
        phone: String,
        body: String,
        packageName: String = provider.packageName,
    ): Intent? {
        val cleanBody = body.trim()
        val internationalPhone = internationalPhoneNumber(phone)
        val uri = when (provider.kind) {
            MessagingDraftKind.WHATSAPP -> {
                val digits = internationalPhone?.filter(Char::isDigit) ?: return null
                Uri.Builder()
                    .scheme("https")
                    .authority("wa.me")
                    .appendPath(digits)
                    .appendQueryParameter("text", cleanBody)
                    .build()
            }
            MessagingDraftKind.TELEGRAM -> {
                val number = internationalPhone ?: return null
                Uri.Builder()
                    .scheme("tg")
                    .authority("resolve")
                    .appendQueryParameter("phone", number)
                    .appendQueryParameter("text", cleanBody)
                    .build()
            }
            MessagingDraftKind.LINE -> Uri.Builder()
                .scheme("https")
                .authority("line.me")
                .appendPath("R")
                .appendPath("share")
                .appendQueryParameter("text", cleanBody)
                .build()
            MessagingDraftKind.SMS_URI -> Uri.parse("smsto:${Uri.encode(phone)}")
            MessagingDraftKind.GENERIC_SHARE -> return Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, cleanBody)
                .setPackage(packageName)
        }
        val action = if (provider.kind == MessagingDraftKind.SMS_URI) {
            Intent.ACTION_SENDTO
        } else {
            Intent.ACTION_VIEW
        }
        return Intent(action, uri)
            .setPackage(packageName)
            .apply {
                if (provider.kind == MessagingDraftKind.SMS_URI) {
                    putExtra("sms_body", cleanBody)
                }
            }
    }

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

    private fun isPackageInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
    }.isSuccess

    private fun openDefaultMessageDraft(contact: ContactResult, body: String): Boolean {
        val draft = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:${Uri.encode(contact.phone)}"),
        ).putExtra("sms_body", body.trim())
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
        if (!defaultPackage.isNullOrBlank()) {
            val explicit = Intent(draft).setPackage(defaultPackage)
            if (canResolve(explicit) && startActivity(explicit, false)) return true
        }
        return canResolve(draft) && startActivity(draft, false)
    }

    private fun canResolve(intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null

    private fun hasHandler(intent: Intent): Boolean =
        context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ).isNotEmpty()
}
