Open 3.1.1 improves visual consistency, language handling, and access to Android settings without changing the OpenSource product identity.

- **Mink and system palettes:** Appearance settings now let you choose the intentional Mink color palette or Android's dynamic system colors on Android 12 and newer. The choice is included in backups, while older backups continue to import with Mink as the safe default.
- **Geist typography:** Mink's default interface typeface is now Geist, with a matching monospace family and bundled open-source license information.
- **Consistent app surfaces:** Home, Settings, the app drawer, dialogs, and secondary activities now share the same color and typography foundation.
- **Reliable language switching:** The launcher entry point is separated from the Home activity so changing the app language no longer crashes the launcher. Indonesian resources now use Android's supported `in` qualifier and load correctly.
- **System Settings shortcut:** Settings > More now includes a direct link to Android's system settings.
- **OpenSource identity preserved:** User-facing OpenSource naming remains unchanged while the internal theme architecture is normalized for future development.

No new permissions, network services, or data collection are introduced in this release. Backup schema version 3 remains unchanged and compatible with existing backups.
