package com.katoaapps.openminilaunch

import android.Manifest
import android.provider.AlarmClock
import android.content.ContentUris
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import android.graphics.drawable.Drawable
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.LruCache

internal enum class DirectSmsResult {
    QUEUED,
    NO_DEFAULT_SUBSCRIPTION,
    NOT_AUTHORIZED,
    UNSUPPORTED,
    FAILED,
}

class DeviceActions(private val context: Context) {
    @Volatile private var appsCache: List<LaunchableApp>? = null
    private val labelCache = mutableMapOf<String, String>()
    private val legacyLockAdminComponent = ComponentName(
        context.packageName,
        "${context.packageName}.LockDeviceAdminReceiver",
    )

    fun isLockServiceEnabled(): Boolean {
        val component = ComponentName(context, LockScreenAccessibilityService::class.java)
        return context.getSystemService(AccessibilityManager::class.java)
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { service ->
                val info = service.resolveInfo.serviceInfo
                ComponentName(info.packageName, info.name) == component
            } == true
    }

    fun supportsLockScreenAction(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun lockDevice(): Boolean = isLockServiceEnabled() && LockScreenAccessibilityService.lockScreen()

    fun lockAccessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun openLockAccessibilitySettings() = start(lockAccessibilitySettingsIntent())

    fun isAssistantRoleAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            context.getSystemService(RoleManager::class.java)?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true

    fun isAssistantRoleHeld(): Boolean {
        val configuredAssistant = Settings.Secure.getString(
            context.contentResolver,
            "assistant",
        )?.let(ComponentName::unflattenFromString)
        if (configuredAssistant?.packageName == context.packageName) return true

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
    }

    fun assistantRoleSelectionIntent(): Intent =
        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).takeIf(::canResolve)
            ?: Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)

    /** Removes the retired force-lock admin so future locks do not require a PIN. */
    fun removeLegacyLockAdmin() {
        val manager = context.getSystemService(DevicePolicyManager::class.java) ?: return
        if (manager.isAdminActive(legacyLockAdminComponent)) manager.removeActiveAdmin(legacyLockAdminComponent)
    }

    fun installedApps(): List<LaunchableApp> {
        appsCache?.let { return it }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { LaunchableApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
            .also { apps ->
                synchronized(labelCache) { apps.forEach { labelCache[it.packageName] = it.label } }
                appsCache = apps
            }
    }

    fun invalidateInstalledApps() {
        appsCache = null
    }

    fun openInstalledAppsSettings() = start(Intent(Settings.ACTION_APPLICATION_SETTINGS))

    fun appLabel(packageName: String): String {
        synchronized(labelCache) { labelCache[packageName]?.let { return it } }
        return runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault("Not installed").also { synchronized(labelCache) { labelCache[packageName] = it } }
    }

    fun appIcon(packageName: String): Drawable? {
        iconStateCache.get(packageName)?.let { return it.newDrawable(context.resources) }
        return runCatching { context.packageManager.getApplicationIcon(packageName) }
            .getOrNull()
            ?.also { drawable -> drawable.constantState?.let { iconStateCache.put(packageName, it) } }
    }

    fun launchPackage(packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName)?.let(::start) ?: false

    fun openClock(): Boolean {
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true) && launchPackage(SAMSUNG_CLOCK_PACKAGE)) {
            return true
        }
        val showAlarms = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        if (canResolve(showAlarms) && start(showAlarms)) return true
        CLOCK_PACKAGES.forEach { packageName ->
            if (launchPackage(packageName)) return true
        }
        val discoveredClock = installedApps().firstOrNull { app ->
            app.packageName.contains("clock", ignoreCase = true) ||
                app.label.equals("Clock", ignoreCase = true)
        }
        return discoveredClock?.let { launchPackage(it.packageName) } == true
    }

    fun launchShortcut(shortcut: Shortcut, assignedPackage: String?, openTodos: () -> Unit, openDrawer: () -> Unit) {
        if (!assignedPackage.isNullOrBlank() && shortcut !in listOf(Shortcut.TODO, Shortcut.DRAWER)) {
            launchPackage(assignedPackage)
            return
        }
        when (shortcut) {
            Shortcut.NOTE -> createNote("")
            Shortcut.EVENT -> start(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI))
            Shortcut.WEATHER -> start(Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.com/")))
            Shortcut.TODO -> openTodos()
            Shortcut.CALL -> start(Intent(Intent.ACTION_DIAL))
            Shortcut.MESSAGE -> launchDefaultMessagesApp()
            Shortcut.FILES -> openFilesApp()
            Shortcut.DRAWER -> openDrawer()
        }
    }

    fun searchContacts(query: String): List<ContactResult> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<ContactResult>()
        val seenNumbers = mutableSetOf<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ?"
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            arrayOf("$query%"),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC",
        )?.use { cursor ->
            val contactIdIndex = cursor.getColumnIndexOrThrow(projection[0])
            val nameIndex = cursor.getColumnIndexOrThrow(projection[1])
            val phoneIndex = cursor.getColumnIndexOrThrow(projection[2])
            val typeIndex = cursor.getColumnIndexOrThrow(projection[3])
            val labelIndex = cursor.getColumnIndexOrThrow(projection[4])
            while (cursor.moveToNext() && results.size < 8) {
                val phone = cursor.getString(phoneIndex)
                val normalizedPhone = PhoneNumberUtils.normalizeNumber(phone).ifBlank { phone }
                val uniqueNumber = "${cursor.getLong(contactIdIndex)}:$normalizedPhone"
                if (seenNumbers.add(uniqueNumber)) {
                    val phoneLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources,
                        cursor.getInt(typeIndex),
                        cursor.getString(labelIndex),
                    ).toString().ifBlank { "Phone" }
                    val contactUri = ContentUris.withAppendedId(
                        ContactsContract.Contacts.CONTENT_URI,
                        cursor.getLong(contactIdIndex),
                    ).toString()
                    results += ContactResult(contactUri, cursor.getString(nameIndex), phone, phoneLabel)
                }
            }
        }
        return results
    }

    private fun launchDefaultMessagesApp() {
        val packageName = Telephony.Sms.getDefaultSmsPackage(context)
        if (!packageName.isNullOrBlank()) launchPackage(packageName)
        else start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
    }

    fun chooseMessagingApp(contact: ContactResult, body: String): Boolean {
        val contactAware = Intent(ContactsContract.Intents.ACTION_VOICE_SEND_MESSAGE_TO_CONTACTS)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, body.trim())
            .putExtra(ContactsContract.Intents.EXTRA_RECIPIENT_CONTACT_URI, arrayOf(contact.contactUri))
            .putExtra(ContactsContract.Intents.EXTRA_RECIPIENT_CONTACT_NAME, arrayOf(contact.name))
        if (hasHandler(contactAware)) return start(contactAware, chooser = true)

        val smsOrRcs = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:${Uri.encode(contact.phone)}"),
        ).putExtra("sms_body", body.trim())
        return hasHandler(smsOrRcs) && start(smsOrRcs, chooser = true)
    }

    internal fun sendSmsDirect(phone: String, body: String): DirectSmsResult {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            return DirectSmsResult.UNSUPPORTED
        }
        if (!isAssistantRoleHeld() || context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
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

    fun placeCall(phone: String): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ||
            runCatching { PhoneNumberUtils.isEmergencyNumber(phone) }.getOrDefault(false)
        ) {
            return dial(phone)
        }
        val direct = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(phone)}"))
        return if (canResolve(direct) && start(direct)) true else dial(phone)
    }

    fun dial(phone: String): Boolean = start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))

    fun chooseCallingApp(phone: String): Boolean {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
        return hasHandler(intent) && start(intent, chooser = true)
    }

    fun textShareApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { LaunchableApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun curatedAiApps(): List<LaunchableApp> {
        val compatible = textShareApps().associateBy { it.packageName }
        return CURATED_AI_PACKAGES.mapNotNull(compatible::get).sortedBy { it.label.lowercase() }
    }

    fun webSearchApps(): List<LaunchableApp> {
        val searchUrl = Uri.parse("https://www.google.com/search?q=minklauncher")
        return context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_VIEW, searchUrl),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { LaunchableApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun shareQueryWithApp(query: String, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, query.trim())
            .setPackage(packageName)
        return canResolve(intent) && start(intent)
    }

    fun shareText(text: String) = start(
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), chooser = true
    )

    fun createNote(text: String, preferredPackage: String? = null): Boolean {
        val clean = text.trim()
        fun modern(packageName: String? = null) = Intent(Intent.ACTION_CREATE_NOTE).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, clean)
            .apply { if (!packageName.isNullOrBlank()) setPackage(packageName) }
        fun legacy(packageName: String? = null) = Intent("com.google.android.gms.actions.CREATE_NOTE").setType("text/plain")
            .putExtra("com.google.android.gms.actions.extra.TEXT", clean)
            .apply { if (!packageName.isNullOrBlank()) setPackage(packageName) }
        fun shared(packageName: String) = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, clean)
            .setPackage(packageName)

        if (!preferredPackage.isNullOrBlank()) {
            modern(preferredPackage).takeIf(::canResolve)?.let { return start(it) }
            legacy(preferredPackage).takeIf(::canResolve)?.let { return start(it) }
            shared(preferredPackage).takeIf(::canResolve)?.let { return start(it) }
        }
        modern().takeIf(::canResolve)?.let { return start(it) }
        legacy().takeIf(::canResolve)?.let { return start(it) }

        // Samsung Notes does not currently advertise either standardized create-note action.
        // Its exported text share target is the stable public handoff available to launchers.
        val samsungPackage = "com.samsung.android.app.notes"
        if (clean.isEmpty() && launchPackage(samsungPackage)) return true
        shared(samsungPackage).takeIf(::canResolve)?.let { return start(it) }
        return start(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, clean), chooser = true)
    }

    fun exportTodosToNotes(text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty()) return false
        val createNote = Intent(Intent.ACTION_CREATE_NOTE).setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "MinkLauncher To-do List")
            .putExtra(Intent.EXTRA_TEXT, clean)
        if (hasHandler(createNote)) return start(createNote, chooser = true)
        return start(
            Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TITLE, "MinkLauncher To-do List")
                .putExtra(Intent.EXTRA_TEXT, clean),
            chooser = true,
        )
    }

    fun createEvent(description: String): Boolean {
        val draft = parseCalendarPhrase(description)
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, draft.title)
            .putExtra(CalendarContract.Events.DESCRIPTION, draft.description)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, draft.allDay)
        draft.startMillis?.let { intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
        draft.endMillis?.let { intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
        return start(intent)
    }

    fun emailSupport() = start(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:contact@katoaapps.com")))

    fun openPrivacyPolicy() = start(Intent(Intent.ACTION_VIEW, Uri.parse("https://minklauncher.com/privacy")))

    fun openTermsOfUse() = start(Intent(Intent.ACTION_VIEW, Uri.parse("https://minklauncher.com/terms")))

    fun webSearch(query: String, preferredPackage: String? = null): Boolean {
        val clean = query.trim()
        if (clean.isEmpty()) return false
        val destination = normalizedWebUrl(clean)
            ?: "https://www.google.com/search?q=${Uri.encode(clean)}"
        val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(destination))
        if (!preferredPackage.isNullOrBlank()) {
            val explicitUrl = Intent(searchIntent).setPackage(preferredPackage)
            if (canResolve(explicitUrl)) return start(explicitUrl)
        }
        return start(searchIntent)
    }

    private fun openFilesApp(): Boolean {
        val files = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_FILES)
        if (canResolve(files)) return start(files)
        return start(
            Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
        )
    }

    fun openFile(result: FileSearchResult) = start(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(result.uri, result.mimeType.ifBlank { "*/*" })
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        chooser = true,
    )

    fun openAppSettings() = start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))

    fun openNotificationAccessSettings() = start(NotificationHub.accessSettingsIntent())

    fun expandNotificationShade() {
        runCatching {
            val statusBar = context.getSystemService("statusbar")
            statusBar.javaClass.getMethod("expandNotificationsPanel").invoke(statusBar)
        }
    }

    private fun canResolve(intent: Intent): Boolean = intent.resolveActivity(context.packageManager) != null

    private fun hasHandler(intent: Intent): Boolean =
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

    private fun start(intent: Intent, chooser: Boolean = false): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(if (chooser) Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else intent)
            true
        }.getOrDefault(false)
    }

    private companion object {
        const val SAMSUNG_CLOCK_PACKAGE = "com.sec.android.app.clockpackage"
        val CLOCK_PACKAGES = listOf(
            SAMSUNG_CLOCK_PACKAGE,
            "com.google.android.deskclock",
            "com.android.deskclock",
        )

        // Package icons are reused by search, shortcuts, setup, and Settings.
        // ConstantState gives each caller a fresh Drawable while keeping decoded icon data cached.
        val iconStateCache = LruCache<String, Drawable.ConstantState>(96)

        val CURATED_AI_PACKAGES = setOf(
            "com.openai.chatgpt",
            "com.anthropic.claude",
            "ai.perplexity.app.android",
            "com.microsoft.copilot",
            "com.deepseek.chat",
            "com.facebook.stella",
            "com.google.android.apps.bard",
        )
    }
}
