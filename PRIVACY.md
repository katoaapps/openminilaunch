# Privacy

MinkLauncher OpenSource does not operate an application server, include analytics or advertising SDKs, create an account, or upload launcher content or personal data. Its normal Internet permission is used only for the optional GitHub release-metadata check described below.

The app stores its settings, to-dos, selected document-folder references, widget configuration, daily well-being preferences, and five most recent plain-text queries locally on the device. Android cloud backup is disabled for the application.

Optional permissions are used as follows:

- Contacts: searches contact names and phone numbers locally for `@` messaging and `#` calling commands.
- Phone: places a call only after the user selects a contact and confirms the call in MinkLauncher OpenSource.
- Messaging: sends a carrier SMS only after the user selects a recipient and submits a message with System Messages and automatic sending enabled. Direct SMS is available only while MinkLauncher OpenSource is the active Android assistant handler. Integrated-app and Android share handoffs pass user-entered text, and a phone number when supported, to the selected provider; that provider handles the final send under its own terms.
- Photos, videos, audio, and older shared-storage access: searches media filenames locally and displays local thumbnails.
- Notification shade: permits the launcher’s swipe-down gesture to expand Android’s notification panel.
- Notification access: reads active Android-standard message and email notifications in memory so they can be grouped and replied to through the originating app when it supplies a compatible reply action. Other notification categories are ignored, and conversation history is not stored by MinkLauncher OpenSource.
- Usage access: reads foreground events for the apps the user chooses to include in Mink’s Day while MinkLauncher OpenSource is visible. Other apps are excluded from the trail and totals. The app stores the user’s daily limit, pause preference, and app choices, but does not create a separate usage-history database.
- Accessibility: the optional **MinkLauncher OpenSource - Double Tap to Lock Screen** service performs only Android's Lock screen global action after the user double-taps empty Home space. It does not subscribe to accessibility events, retrieve window content, perform gestures, or collect data.
- Internet: when GitHub update checks are enabled, reads the latest public release tag from GitHub at most twice a day. GitHub receives ordinary connection metadata. No launcher content is included, and MinkLauncher OpenSource never downloads or installs an APK itself. The check can be disabled in **Settings → About**.

Mink’s Day pausing changes only which selected apps MinkLauncher displays or launches. It does not disable, suspend, modify, or control those apps at the Android system level. The always-on launcher barrier does not require Usage Access; pausing after a daily limit does.

Document search uses only folders selected through Android’s Storage Access Framework. Folder references and filenames are processed locally.

When selected as the Android assistant, MinkLauncher OpenSource opens its keyboard-first Magic Box over the current app. It does not request microphone access, inspect assist context, read the underlying screen, or collect information from the app underneath it.

When the user launches another app, composes a message, creates a note or event, opens a file, or delegates a web query, Android hands the requested content to the app selected by the user. That destination app’s privacy practices then apply.

Privacy questions can be sent to contact@katoaapps.com.
