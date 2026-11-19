# Launcher Todos 2.0

This package is the launcher-owned **Todos** feature. It is not a second copy of
MinkBoard Pro and it must not grow its own board database or board rules.

The dependency direction is:

```text
launcher UI and Magic Box
        -> Todos feature runtime and home projection
        -> MinkBoard launcher facade / public API
        -> MinkBoard model, core, data, focus, and Compose modules
```

## Package ownership

- `runtime/` owns the application-scoped lifetime and coordinates foreground,
  navigation, and focus collaborators supplied by the launcher host.
- `home/` owns launcher-specific Todo behavior: intake creation, completion,
  status visibility, compact-page memory, and card commands used by Home.
- `migration/` owns the one-way conversion of the former launcher Todo JSON into
  the canonical board store.
- `backend/minkboard/` is the explicit adapter boundary from Todos to the
  reusable MinkBoard modules. It receives host configuration instead of
  reaching into an application package.
- `access/` adapts Android device authentication for protected boards.
- `presentation/` hosts the full board route and compact card dialog while
  delegating the actual board UI to `minkboard-compose`. Localized resource IDs
  are injected by the host; this package never imports a distribution's `R`
  class.

## Stability rules

1. Keep board contents in the MinkBoard repository. Launcher preferences may
   store presentation choices, never duplicate cards, statuses, or boards.
2. Keep the existing SharedPreferences keys and notification channel IDs stable
   unless a deliberate migration accompanies the change.
3. Backup/restore remains launcher-owned because the outer launcher archive and
   Home projection are launcher data; the encrypted board envelope itself comes
   from the shared MinkBoard backup workflow.
4. The mini-app may choose a board and visible statuses, add cards, open cards,
   and toggle completion. General board management belongs to the reusable
   MinkBoard surface/API.
5. Distribution-specific limits are selected through a MinkBoard product
   policy. Do not fork the board engine for a launcher distribution.
6. Do not reference OpenMink or PlayMink in package/type names here. Host app
   identity is allowed only in concrete resources, persistent compatibility
   identifiers, and backup metadata supplied at the app boundary.

OpenMink's concrete adapters live outside this generic package:
`features/todos/host` supplies notification/navigation configuration and
`features/backup` combines the board envelope with launcher backup metadata.
