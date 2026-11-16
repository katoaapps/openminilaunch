Open 3.2.3 improves Magic Box input, todo ordering, Home navigation, and app-icon control without changing permissions or the portable backup format.

- **Long-press hot keys work before Magic Box opens:** A hardware or system keyboard can now replace its initial ordinary key with the held-key alternate and route that alternate through Magic Box correctly.
- **Hot-key symbols stay inside active drafts:** After a route or search has started, tapping `@`, `#`, `-`, `/`, `+`, or `?` on Mink Keyboard inserts the character at the cursor instead of abandoning the draft and opening another route.
- **Ordinary typing remains ordinary:** The correction is limited to the first input transition. Regular second characters, larger edits, and automatically committed separators are preserved instead of being mistaken for a long-press replacement.
- **The same routing model across input types:** Hardware-key capture and Android system-keyboard replacement now share one focused reconciliation path, keeping `@`, `#`, `-`, `/`, `+`, and `?` behavior consistent from collapsed Home.
- **New todos go first:** Todos created from Magic Box, MinkSpace, or the full todo screen now appear at the top of the unfinished list.
- **Mink’s Day stays optional on Home:** A new Mink’s Day setting removes its swipe tab while keeping the feature available from its Home button and Settings.
- **Choose one icon per app:** Long-press an app in the drawer to select one icon from an installed icon pack or return that app to the global icon style.

No new permissions, analytics, accounts, or network services are introduced. Portable backup schema version 3 remains unchanged; the new tab and icon preferences are optional fields, so older backups continue to import and older readers can ignore them.
