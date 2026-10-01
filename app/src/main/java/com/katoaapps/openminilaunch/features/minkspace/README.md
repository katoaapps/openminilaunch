# Adding a MinkSpace mini-app

MinkSpace mini-apps are built-in launcher features, not dynamically loaded plugins. This keeps
their data private, their behavior reviewable, and the Home pager predictable on small devices.

## Package shape

Use the same two-package structure as Calculator, Media, and Todo:

```text
features/minkspace/<name>/
  <Name>Engine.kt                 # pure behavior, when needed
  <Name>Repository.kt             # persistence or platform data, when needed
  <Name>Item.kt                   # feature models, when needed

ui/minkspace/<name>/
  <Name>MiniAppPage.kt            # Home entry point and feature-state acquisition
  <Name>MiniApp.kt                # Mini-app content UI
  <Name>LabActivity.kt            # optional setup/management screen
  ...                             # feature-only UI components
```

Shared paging and presentation code belongs in `ui/minkspace/core`. Do not put feature state or
feature-specific controls there.

## Registration checklist

1. Add a stable identity to `MinkSpaceMiniApp`. Never rename a shipped `stableId`; it is persisted.
2. Mark `readyForHome` only after the Home UI is usable. Use `alwaysEnabled` only for a required
   mini-app such as Todo.
3. Add label, icon, and optional manager metadata to `MinkSpaceMiniAppCatalog`.
4. Expose one `<Name>MiniAppPage` composable from the mini-app's UI package. It owns repository
   lookup and uses `MinkSpaceMiniAppSurface` for the common Home container.
5. Add one branch in `MinkSpaceHost` that calls that page. The host should not acquire the
   mini-app's repository or contain its UI.
6. If setup or management is needed, add a focused Activity in the same UI package, register it in
   the manifest, and connect its preview destination from Settings.
7. Add localized strings and focused tests under the matching `features/minkspace/<name>` test
   package.

## Interaction rules

- Vertical paging belongs to MinkSpace. A mini-app may use horizontal paging for its own items.
- Todo remains enabled, but it does not have to be first.
- Mini-app drafts should survive their own horizontal page changes.
- Do not intercept launcher gestures outside the mini-app bounds.
- Use the shared content, muted-content, and container colors supplied by the Home page.
- Keep runtime permissions and destructive confirmations inside the owning mini-app feature.
