# Building a MinkSpace mini-app

MinkSpace is a small platform inside the launcher. It owns the vertical pager, the shared Home
surface, compact-device sizing, theme colors, expansion animation, Back handling, and saved
presentation state. A mini-app owns its feature data, UI, and the actions that request presentation
changes.

Mini-apps are built into the launcher rather than loaded from arbitrary APKs. This keeps private
data local, behavior reviewable, and Home predictable on compact devices.

## The contract

Every released mini-app has one adapter class implementing `MinkSpaceApp`:

```kotlin
internal class ExampleMinkSpaceApp(
    private val repository: ExampleRepository,
) : MinkSpaceApp {
    override val manifest = Manifest

    @Composable
    override fun Content(scope: MinkSpaceScope, modifier: Modifier) {
        scope.Surface(modifier) {
            ExampleMiniApp(
                compact = scope.compact,
                contentColor = scope.contentColor,
                mutedContentColor = scope.mutedContentColor,
                expanded = scope.expansion.expanded,
                onExpand = scope.expansion::expand,
                onCollapse = scope.expansion::collapse,
            )
        }
    }

    companion object {
        val Manifest = MinkSpaceMiniAppManifest(
            miniApp = MinkSpaceMiniApp.EXAMPLE,
            labelRes = R.string.example,
            icon = Icons.Default.Extension,
            supportsExpansion = true,
            showExpansionButton = true,
        )
    }
}
```

`MinkSpaceScope` is the complete platform-facing API available to a mini-app:

- `compact` describes the current layout class.
- `contentColor`, `mutedContentColor`, and `containerColor` are resolved by Home.
- `Surface` draws the standard MinkSpace container.
- `expansion` exposes `enabled`, `expanded`, `expand()`, and `collapse()`.

The scope deliberately does not expose `LauncherStore`, `DeviceActions`, Activities, or feature
repositories. Pass those through the adapter constructor instead. This prevents MinkSpace from
becoming a service locator and keeps feature dependencies visible.

`supportsExpansion` grants permission to request the full Home panel. `showExpansionButton` asks
MinkSpace to draw its standard collapsed-state action using "Expand view" or the manifest's optional
label override. A mini-app may hide that standard action and call the controller from feature UI.
MinkSpace always draws the expanded-state close button; mini-apps cannot suppress it.

## Registration

Adding a mini-app requires three deliberate changes:

1. Add its permanent `stableId` to `MinkSpaceMiniApp`. This enum is the persistence identity; never
   rename an ID after release. Keep `readyForHome = false` until the runtime UI is complete, and add
   it to `defaultEnabled` only when new installs should show it automatically.
2. Implement one `<Name>MinkSpaceApp` adapter beside the feature UI. Keep its manifest with the
   implementation so metadata and capabilities cannot drift away from the actual app.
3. Add one `MinkSpaceMiniAppRegistration` to `MinkSpaceMiniAppCatalog`. The factory receives
   `MinkSpaceHostDependencies` and constructor-injects only the dependencies that app needs.

Do not add a branch to `MinkSpaceHost`. The host renders every mini-app through the registry and
the shared interface. Catalog validation fails early if an identity is duplicated, omitted, or
marked ready without a runtime implementation.

If setup or management is needed, add a focused Activity in the feature's UI package, declare a
`settingsTarget` in the manifest, and route that destination from Settings.

Add localized strings and focused tests in the matching feature package. Mini-apps backed by
Android system state should keep platform callbacks and capability checks in their repository;
their `MinkSpaceApp` adapter owns that repository's UI lifecycle.

## Package shape

```text
features/minkspace/<name>/
  <Name>Engine.kt                 # pure behavior, when needed
  <Name>Repository.kt             # persistence or platform data, when needed
  <Name>Item.kt                   # feature models, when needed

ui/minkspace/<name>/
  <Name>MinkSpaceApp.kt           # manifest, dependency adapter, and platform entry point
  <Name>MiniApp.kt                # feature content UI
  <Name>LabActivity.kt            # optional setup or management screen
  ...                             # feature-only UI components
```

Shared platform code belongs in `ui/minkspace/core`. Feature state and feature-specific controls
do not.

## Interaction rules

- Vertical paging belongs to MinkSpace. A mini-app may use horizontal paging internally.
- Mini-app drafts should survive their own horizontal page changes.
- Do not intercept launcher gestures outside the mini-app bounds.
- Keep permissions, destructive confirmations, and platform failures inside the owning feature.
- Multi-tap controls must resolve the complete tap sequence before firing a single-tap action.
- System-backed commands must honor the provider's advertised capabilities.
- MinkSpace owns expansion state, the standard expand action, the permanent expanded-state close
  button, layout animation, pager locking, Back handling, saved state, and cleanup when the visible
  mini-app changes.
- A mini-app may hide the standard expand action and provide its own trigger. Use
  `minkSpaceExpansionBackgroundTap` only when empty-space taps fit the feature.
- Nested actionable content must win its gesture before an empty-space expansion gesture.
- Android-view hosts need an explicit interaction state. Widget is the reference: collapsed widget
  taps open the provider, its affordance requests expansion, and real widget controls are enabled
  only while expanded.

## Current built-ins

OpenMink ships Todo, Calculator, Media, Music, Widget, and vCard. Recorder retains a reserved stable
ID for migration safety but remains hidden until its permission, privacy, UI, and release plan are
approved.
