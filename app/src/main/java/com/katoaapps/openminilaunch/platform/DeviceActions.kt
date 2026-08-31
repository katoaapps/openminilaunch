package com.katoaapps.openminilaunch.platform

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.calendar.parseCalendarPhrase
import com.katoaapps.openminilaunch.features.apps.LauncherAppRepository
import com.katoaapps.openminilaunch.features.conversations.NotificationHub
import com.katoaapps.openminilaunch.features.magic.normalizedWebUrl
import com.katoaapps.openminilaunch.features.messaging.MessagingDeviceActions
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderOption
import com.katoaapps.openminilaunch.features.messaging.PreferredMessageDraftResult
import com.katoaapps.openminilaunch.features.updates.GITHUB_LATEST_APK_URL
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.ui.apps.AllAppsActivity

import android.Manifest
import android.provider.AlarmClock
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
import android.app.SearchManager
import android.provider.Settings
import android.provider.CalendarContract
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.util.LruCache
import kotlinx.coroutines.flow.StateFlow

class DeviceActions(private val context: Context) {
    private val launcherAppRepository = LauncherAppRepository.get(context)
    val launcherAppsRevision: StateFlow<Long> = launcherAppRepository.revision
    private val labelCache = mutableMapOf<String, String>()
    private val contactSearch = ContactSearch(context)
    private val shareTargetDiscovery = ShareTargetDiscovery(context)
    private val directSmsSender = DirectSmsSender(context, ::isAssistantRoleHeld)
    private val legacyLockAdminComponent = ComponentName(
        context.packageName,
        "${context.packageName}.LockDeviceAdminReceiver",
    )
    private val messagingActions by lazy {
        MessagingDeviceActions(
            context = context,
            appLabel = ::appLabel,
            startActivity = ::start,
        )
    }

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

    fun installedApps(): List<LauncherAppTarget> = launcherAppRepository.targets()

    fun invalidateInstalledApps() {
        launcherAppRepository.invalidate()
    }

    fun resolveLauncherTarget(selectionKey: String): LauncherAppTarget =
        launcherAppRepository.resolve(selectionKey)

    fun normalizedLauncherSelectionKey(selectionKey: String): String? =
        launcherAppRepository.normalizedSelectionKey(selectionKey)

    fun launcherAppLabel(selectionKey: String): String = resolveLauncherTarget(selectionKey).label

    fun launcherAppIcon(selectionKey: String): Drawable? =
        launcherAppRepository.icon(resolveLauncherTarget(selectionKey))

    fun launcherAppIcon(target: LauncherAppTarget): Drawable? = launcherAppRepository.icon(target)

    fun launchLauncherTarget(target: LauncherAppTarget): Boolean = launcherAppRepository.launch(target)

    fun launchLauncherSelection(selectionKey: String): Boolean =
        launcherAppRepository.launch(resolveLauncherTarget(selectionKey))

    fun openInstalledAppsSettings() = start(Intent(Settings.ACTION_APPLICATION_SETTINGS))

    fun openAllApps() = start(Intent(context, AllAppsActivity::class.java))

    fun appLabel(packageName: String): String {
        synchronized(labelCache) { labelCache[packageName]?.let { return it } }
        return runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(context.getString(R.string.not_installed)).also { synchronized(labelCache) { labelCache[packageName] = it } }
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
        return discoveredClock?.let(::launchLauncherTarget) == true
    }

    fun launchShortcut(shortcut: Shortcut, assignedTarget: String?, openTodos: () -> Unit, openDrawer: () -> Unit) {
        if (!assignedTarget.isNullOrBlank() && shortcut !in listOf(Shortcut.TODO, Shortcut.DRAWER)) {
            launchLauncherSelection(assignedTarget)
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

    fun shortcutTargetPackage(shortcut: Shortcut, assignedTarget: String?): String? {
        if (!assignedTarget.isNullOrBlank() && shortcut !in listOf(Shortcut.TODO, Shortcut.DRAWER)) {
            return resolveLauncherTarget(assignedTarget).takeUnless(LauncherAppTarget::isWorkProfile)?.packageName
        }
        return if (shortcut == Shortcut.MESSAGE) Telephony.Sms.getDefaultSmsPackage(context) else null
    }

    fun searchContacts(query: String, useDemoData: Boolean = false): List<ContactResult> {
        return contactSearch.search(query, useDemoData)
    }

    private fun launchDefaultMessagesApp() {
        val packageName = Telephony.Sms.getDefaultSmsPackage(context)
        if (!packageName.isNullOrBlank()) launchPackage(packageName)
        else start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
    }

    internal fun messagingProviderOptions(): List<MessagingProviderOption> =
        messagingActions.providerOptions()

    fun defaultMessagingAppLabel(): String = messagingActions.defaultMessagingAppLabel()

    internal fun openPreferredMessageDraft(
        contact: ContactResult,
        body: String,
        preferredPackage: String?,
    ): PreferredMessageDraftResult = messagingActions.openPreferredMessageDraft(
        contact = contact,
        body = body,
        preferredPackage = preferredPackage,
    )

    fun chooseMessagingApp(body: String): Boolean = messagingActions.chooseMessagingApp(body)

    internal fun sendSmsDirect(phone: String, body: String): DirectSmsResult {
        return directSmsSender.send(phone, body)
    }

    @Suppress("DEPRECATION")
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
        return shareTargetDiscovery.textShareApps()
    }

    fun curatedAiApps(): List<LaunchableApp> {
        return shareTargetDiscovery.curatedAiApps()
    }

    fun webSearchApps(): List<LaunchableApp> {
        return shareTargetDiscovery.webSearchApps()
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

    fun createNote(text: String): Boolean {
        val clean = text.trim()
        if (clean.isNotEmpty()) return shareText(clean)

        val modern = Intent(Intent.ACTION_CREATE_NOTE).setType("text/plain")
        val legacy = Intent("com.google.android.gms.actions.CREATE_NOTE").setType("text/plain")

        modern.takeIf(::canResolve)?.let { return start(it) }
        legacy.takeIf(::canResolve)?.let { return start(it) }

        val samsungPackage = "com.samsung.android.app.notes"
        return launchPackage(samsungPackage)
    }

    fun exportTodosToNotes(text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty()) return false
        val title = context.getString(R.string.todo_export_title, context.getString(R.string.app_name))
        return start(
            Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TITLE, title)
                .putExtra(Intent.EXTRA_TEXT, clean),
            chooser = true,
        )
    }

    fun createEvent(description: String): Boolean {
        val draft = parseCalendarPhrase(description, context.getString(R.string.new_event))
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

    fun openLatestGitHubReleaseDownload(): Boolean =
        start(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_LATEST_APK_URL)))

    fun webSearch(query: String, preferredPackage: String? = null): Boolean {
        val clean = query.trim()
        if (clean.isEmpty()) return false
        val searchIntent = normalizedWebUrl(clean)?.let { destination ->
            Intent(Intent.ACTION_VIEW, Uri.parse(destination))
        } ?: Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, clean)
        if (!preferredPackage.isNullOrBlank()) {
            val explicitSearch = Intent(searchIntent).setPackage(preferredPackage)
            if (canResolve(explicitSearch)) return start(explicitSearch)
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

    }
}
