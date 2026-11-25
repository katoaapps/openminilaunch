Open 4.0.0 is a major Home update with an optional Classic layout, a rebuilt To-Dos experience powered by MinkBoard, a rewritten Widget Wall, refreshed onboarding, and 31 built-in Mink Games.

## Home and onboarding

- **Choose Mink or Classic Home:** Mink keeps the focused action-first layout. Classic adds a BlackBerry-inspired, page-based Home with configurable folders, anchored mini-app and conversation destinations, gestures, and its own guided tour.
- **A useful Home from the first launch:** Fresh installs receive purpose-based Connect, Tasks, Media, Compose, Browse, and Tools folders plus a seeded Quick Apps page. The final configurable Home slot remains empty and ready to create. Existing Home layouts are not overwritten.
- **Larger folders:** Regular Home folders now hold up to 16 apps, shortcuts, or built-in destinations. Quick Apps remains intentionally limited to eight.
- **Clearer onboarding:** First-run setup now teaches the chosen Home layout, Mink or system keyboard behavior, contacts and Mink Assistant benefits, Home gestures, folders, and Magic Box hot keys with layout-specific spotlights.
- **Stable Home navigation:** Header, Magic Box, Conversations, keyboard, mini-app, and Classic page state are reconciled when leaving a surface so input does not remain trapped in the wrong destination.

## To-Dos and MinkBoard

- **To-Dos 2.0:** Existing saved to-dos migrate once into a local two-status MinkBoard. New to-dos appear first, open through the canonical card workflow, and stay synchronized with board and status changes.
- **Full board access remains available:** The To-Dos mini-app provides focused card and status controls, while the embedded MinkBoard surface handles board creation, richer editing, reordering, focus timers, import, and export.
- **Portable board backups:** Backup schema 5 adds passphrase-encrypted MinkBoard data and non-destructive board restore. Older supported backups still import, but backups created by 4.0.0 require a schema-5-capable OpenMink release so boards are never silently discarded.

## Widgets, conversations, and input

- **Rewritten Widget Wall gestures:** Horizontal page navigation and nested widget interaction no longer depend on the old gesture trap, making vertically scrolling widgets and Home swipes substantially easier to use together.
- **Improved conversation replies:** Reply fields resize with touch keyboards, release focus when leaving Conversations, and return to the conversation root so unread Favorite indicators remain meaningful.
- **More reliable Magic Box hot keys:** Long-press alternates work before Magic Box opens, while hot-key symbols typed inside an active draft remain ordinary draft text instead of replacing the current route.
- **One icon per app:** Long-press an app in the drawer to select one icon from an installed icon pack or return it to the global icon style.
- **Optional Mink's Day page:** Its Home tab can be hidden without disabling the feature.

## Mink Games

- **31 games, already included:** Fifteen launcher entries cover ten focused games, a ten-game Card Games collection, Snakes & Ladders, Ludo, Scaffold, and eight motion-controlled Tilt Games. Nothing else needs to be installed.
- **Games behave like destinations, not fake installed packages:** They can be searched in Magic Box, placed in Home folders, and launched from See All with dedicated arcade artwork.
- **Independent Android tasks:** Each top-level game receives its own recent-app entry so leaving Home does not discard the game session. The launcher remains the primary Home and Android Studio launch destination.
- **Available without taking over Home:** Mink Games stay searchable in Magic Box and available in See All or any folder the user chooses to create.
- **Embedded console is closed to imports:** OpenMink includes the eight reviewed Tilt Games but does not expose Sable Games' external game-import workflow.

Mink Games adds no permissions or network service. Protected boards use Android's device-credential and biometric prompt APIs only when the user enables protection or opens a protected board. MinkBoard focus timers add exact-alarm scheduling with an inexact fallback, reboot restoration, wake lock, vibration, and a special-use foreground service so a user-started timer can remain visible, finish while the app is closed, and alert reliably. Android notification permission is requested only when that timer workflow needs it. This release adds no analytics, accounts, advertising, or application network service.
