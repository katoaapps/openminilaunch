# MinkLauncher OpenSource

A focused, keyboard-first Android home-screen launcher and digital assistant built with Kotlin and Jetpack Compose.

Current version: **Open 1.1** (`1.1.0`). Feature releases show existing users a one-time in-app update notice covering new behavior, privacy impact, and any optional permissions; the onboarding tutorial is updated alongside each release.

## Download

- [Download the latest GitHub build](https://github.com/katoaapps/openminilaunch/releases/latest/download/MinkLauncher-OpenSource.apk)
- [Download from F-Droid](https://f-droid.org/packages/com.katoaapps.openminilaunch/)
- Google Play: coming soon

GitHub and F-Droid builds currently use different signing keys. Android cannot
update one distribution channel with the other; switching requires uninstalling
the existing app first and clears MinkLauncher OpenSource's local data.

## Included

- Tappable date header that opens the system clock, plus settings access
- First-run onboarding for the launcher, Magic Box, to-dos, search, and permissions, followed by a dedicated shortcut-app setup step
- Replayable onboarding from Settings, including permission setup
- A responsive raised Home panel with a two-thirds to-do area and a one-third icon-only shortcut grid
- Three-at-a-time, horizontally snapping to-do preview with unfinished tasks first
- Eight home shortcuts: six generic app slots with built-in Note, Calendar, Weather, Call, Messenger, and Files defaults, plus To-do and Top 8
- Long-press shortcut edit mode with persistent drag reordering and a reset control in Settings
- An active-only **Conversations** space opened from Home:
  - Android-standard message and email notifications are included; other notification categories are ignored
  - Messages and email updates are grouped into conversation timelines and ordered newest first
  - One-to-one conversations can merge across apps when Android exposes a shared contact identity; ambiguous identities remain separate
  - Each mini conversation shows its source apps and can open the latest provider's full conversation
  - Inline reply appears only when a provider supplies a compatible Android `RemoteInput` action
  - Successful reply handoff is acknowledged with `Sent`; MinkLauncher OpenSource does not claim delivery or read status
  - Conversation contents and replies remain transient in memory, are not retained as history, and are never sent to Katoa Apps
- A horizontally adjacent widget page for up to four Android app widgets, including an app-grouped visual picker, system binding/configuration, reordering, and removal
- A dedicated **Mink’s Day** page to the left of Home:
  - Six illustrated Mink states reflect the time of day and, when optional Usage Access is enabled, foreground time and opens only for the social apps the user chooses to include
  - A compact Mink icon in the Home header opens the page and shows an attention dot only when something is actionable
  - The daily social goal can be set to 30, 60, 90, or 120 minutes
  - Android’s Social category is the visible default; selecting custom apps replaces it, and Restore defaults returns to Android’s category
  - Selected apps stay pinned in a fixed section at the top of the picker for quick review and removal
  - Top-app activity and short, non-judgmental observations are calculated only while the app is open
  - MinkLauncher OpenSource stores the goal and category choices, but does not create a separate usage-history database
  - Activity is refreshed only while MinkLauncher OpenSource is visible; other apps can end a session but never enter the trail or totals
  - The app has no Internet permission and cannot upload usage data itself
  - Without Usage Access, the page remains available as a time-of-day companion
- Weather opens a user-selected app, with Weather.com as the browser fallback
- Six generic app slots that accept any installed app and replace the default Home icon; Android monochrome icons follow the Home panel theme when available
- One-tap reset restores each slot's original built-in action and icon
- A compact drawer containing up to eight selected apps, with a **See all** handoff to a full installed-app browser
- The all-apps browser uses a three-app, one-at-a-time carousel and a draggable A–Z arc with M at its center; the focused app supplies the screen's gradient color
- Real installed-app icons and an alphabetical jump rail in both app pickers
- A searchable Magic Box:
  - Physical-keyboard instant typing — press any printable key from the home screen to reveal the already-focused Magic Box with the first character preserved
  - Plain text — search locally accessible file names, then open valid web addresses directly or search other text with Android's system browser or a user-selected search app
  - Plain text can also be handed to a user-selected app that accepts shared text, for review and submission there
  - `@name message` — choose a contact, then send carrier SMS now or hand the recipient and text to an Android-compatible messaging app
  - `#name` — choose a contact, then place the confirmed carrier call or open Android’s compatible calling-app chooser
  - `-task` — save an internal to-do
  - `/text` — enter a multiline note; prefer Android's dedicated create-note action, use the chosen Notes app when compatible, and use Samsung Notes' text handoff on Samsung devices
  - `+text` — create a calendar draft with local English-language parsing for titles, `for …` descriptions, today/tomorrow, weekdays, `in N days/weeks/months`, `first weekday after the Nth`, and common 12/24-hour times
  - `?app` — search and launch any installed app
- **Mink Assistant** integration: invoke the same keyboard-first Magic Box over the current app using the phone's system assistant gesture
- Direct SMS is available only while MinkLauncher OpenSource is the active assistant handler. Android may grant Send SMS access automatically as part of that role; MinkLauncher OpenSource uses it only after the user approves a specific recipient and message. If it is not role-granted, it is requested on first use or from Settings.
- Message behavior applies only to one-time `@` messages from the Magic Box, not replies in Conversations. It defaults to **Always ask**, which offers **Send SMS now**, **Choose messaging app**, and **Cancel**. Settings can instead remember **Always send as SMS** or **Always choose messaging app**; remembered modes treat pressing the Magic Box action as the user's approval and skip MinkLauncher OpenSource's extra confirmation.
- **Send SMS now** uses the carrier SMS stack for that message and may incur carrier charges, but it does not disable RCS or change any setting in the user's messaging app. Conversation replies continue through the reply action supplied by their source app. The provider chooser first uses Android’s contact-aware messaging contract, then falls back to the SMS/RCS composer contract; the selected provider controls the final send.
- Mink Assistant deliberately ignores assist context and requests no microphone, call-log, screen-reading, or screen-context access; selecting it replaces the current default digital assistant until the user changes it back
- The five most recent successful query handoffs and `?` app launches are stored locally, with controls to reuse, delete, or clear them; other hot-key actions are never added
- Direct calling uses Android's Call permission; emergency numbers and failed direct-call attempts fall back to the system dialer. **Choose calling app** sends the number through Android’s dial intent, so only apps that publicly support telephone dialing appear.
- AI setup shows a curated list of known assistants plus an explicit fallback list of apps accepting shared text
- Android exposes the selected system assistant role, but not a universal "AI app" capability; MinkLauncher OpenSource therefore validates every selected app against the text-sharing contract
- AI handoff does not call AI APIs, submit prompts silently, or render responses inside MinkLauncher OpenSource
- Full to-do management: add, check, edit, delete, reorder, send to a notes app, or save as PDF through Android’s document picker
- Delete confirmation to protect against accidental taps and back-swipe gestures
- Animated Magic Box to-do delivery into the newest widget page
- System, light, and dark appearance modes
- Swipe down anywhere on the home screen to expand notifications
- Optionally enable the minimal **Double-tap screen lock** accessibility service, then double-tap empty home-screen space to lock like the power button. The service cannot read screen content, subscribe to accessibility events, perform gestures, or collect data.
- Local persistence via SharedPreferences; no account or network is required
- Privacy-first file search through Android's MediaStore and user-selected document folders; filenames never leave the device
- Document search uses only folders the user explicitly selects through Android's system folder picker, with an in-search setup reminder until one is selected
- File results are grouped as Photos, Videos, Documents, and Audio, with locally generated thumbnails where Android provides them
- App results from `?` launch immediately when tapped
- Contact results distinguish Mobile, Work, Home, and custom phone-number labels

## Run

Open the folder in Android Studio or build from the terminal:

```shell
./gradlew assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

After installing, press the device Home button and select **MinkLauncher OpenSource** as the home app. Contact permission is requested for `@` and `#` search. Notification access is optional special access granted on Android's dedicated settings screen after MinkLauncher OpenSource explains its use. Usage Access is separately optional and is used only for local Mink’s Day calculations; app activity and insights are never sent to Katoa Apps. Direct SMS is optional and requires MinkLauncher OpenSource to be the active assistant handler; Android may grant Send SMS access with that role. Advanced permission controls are also linked from Settings.

On first launch, MinkLauncher OpenSource explicitly opens Android's default Home-app prompt. If it is dismissed, it can be reopened from **Settings → Default home app**.

After onboarding and its initial permission prompts, a separate setup dialog shows all six generic shortcut slots. The user can assign any installed app or tap **Keep built-in defaults for remaining**; assigning an app makes that slot a direct app launcher and replaces its Home icon, while resetting restores its original default. Shortcut assignments never change how Magic Box commands hand content to Android.

## Test contacts

The repository includes a development-only contact fixture with three plausible fictional names for every first-name initial A–Z. Twelve contacts have an additional Home or Work number for multi-number testing. All 78 entries use numbers from the reserved `202-555-01xx` fictional range and are not packaged in the app.

Regenerate and import the fixture into a running emulator with:

```shell
ruby tools/generate_test_contacts.rb
tools/import_test_contacts.sh emulator-5554
```

Android's Contacts importer opens so the developer can confirm the destination account. This is never run by the app, Gradle, or Android Studio. The script verifies Android's emulator flag and refuses physical devices even if their serial is supplied. It also exits without reopening the importer when the fixture is already staged on the emulator.

## Source layout

- `MainActivity.kt` — activity lifecycle, theme, and top-level navigation
- `HomeScreen.kt` — launcher home, Magic Box, and local-search presentation
- `ShortcutPresentation.kt` — default shortcut icons and generic shortcut assignment rows
- `OnboardingScreen.kt` — first-run setup, update notice, and replayable tutorial
- `SettingsScreen.kt` — launcher preferences and installed-app pickers
- `TodosScreen.kt` — to-do management
- `DeviceActions.kt` — Android intents and device integrations
- `LauncherStore.kt` — locally persisted launcher state
- `FileSearchRepository.kt` — MediaStore and selected-folder search
- `NotificationHub.kt` — transient conversation parsing, cross-provider contact grouping, listener service, and provider-owned replies
- `NotificationHubScreen.kt` — conversation list, mini timelines, full-conversation handoff, and inline reply UI
- `WidgetPage.kt` — Android widget hosting, binding, configuration, lifecycle, and page presentation
- `UsageInsights.kt` — local usage-event analysis, state selection, and privacy-bounded usage models
- `MinkDayScreen.kt` — Mink’s Day UI, six-state sprite renderer, social-app selection, and compact Home status

## Privacy

MinkLauncher OpenSource has no accounts, ads, analytics, trackers, or application server. Search, to-dos, settings, conversations, widget configuration, and usage insights stay local. See [PRIVACY.md](PRIVACY.md) for permission details and the boundaries of Android intent handoffs.

## Terms of use

The official application and project resources are governed by the [MinkLauncher Terms of Use](TERMS.md). Those terms do not reduce the source-code rights granted by Apache License 2.0.

## License

Copyright 2026 Katoa Apps. Source code and bundled artwork are released under the [Apache License 2.0](LICENSE). The license does not grant unrestricted use of the MinkLauncher name or branding.
