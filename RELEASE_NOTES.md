Open 2.0.0 turns the Home panel into MinkSpace and gives personal, work, and private apps clearer boundaries.

- **MinkSpace Mini-Apps:** Swipe vertically between To-dos, Calculator, selected Media, and Android’s active Music session. To-dos remain enabled; the other mini-apps can be enabled and reordered in Settings.
- **App Bubbles (Beta):** Long-press a personal-app result or choose automatic apps in Settings to try opening compatible apps in Android’s bubble stack. Compatibility remains controlled by Android and the target app.
- **Work Apps and Privacy Space:** Work-profile apps have a separate badged drawer. Android 15 Private Space stays outside ordinary search, shortcuts, Top 8, and See All, with optional gateways for supported device-maker private areas.
- **Messaging and keyboard refinements:** Nextcloud Talk can surface recent conversations published by the app, first-match Enter actions are faster, and large-display Home layouts are more comfortable.
- **Portable backups:** Backups now preserve MinkSpace order and enablement while remaining readable by OpenMink 1.5.5. Older versions ignore the new MinkSpace block. Calculator history, selected media and URI grants, active media sessions, Work and Privacy Space state, device gateways, and active bubble notifications remain device-local.

App Bubbles requests Android’s ordinary notification permission only when the feature needs to publish a requested bubble. Music reuses the optional Notification access already explained for Conversations so it can query Android’s active media sessions; MinkLauncher does not record audio or keep listening history.
