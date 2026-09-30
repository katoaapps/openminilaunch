# Privacy Space and OEM secure-container spike

Date: 2026-09-30

## Question

Can Mink Launcher present one safe `Privacy Space` experience across Android Private Space and the private-app systems supplied by Samsung, Motorola, Xiaomi/Redmi, OPPO, vivo, OnePlus, realme, Huawei, Honor, and LG?

## Executive conclusion

Not as one app-enumeration feature.

The OEM features with similar user-facing names are implemented as several materially different systems:

1. **Standard Android Private Space profile** — Android 15 adds the private profile type `android.os.usertype.profile.PRIVATE`. A default launcher with `ACCESS_HIDDEN_PROFILES` can identify the profile, list its launcher activities while unlocked, and request lock-state changes.
2. **Separate system or secondary user** — Xiaomi Second Space, Huawei PrivateSpace, and several ColorOS-family “System Cloner” implementations behave like another phone/user. The owner-side launcher is not meant to enumerate or directly start the other space's apps.
3. **OEM-owned workspace** — Samsung Secure Folder is a Knox container, while Motorola routes Secure Folder through Moto Secure. A third-party launcher may open these public gateways, but it should not claim that either system is Android Private Space or that its contained apps are enumerable.
4. **OEM-launcher hiding** — vivo App Hiding, realme Hide Apps, older OnePlus Hidden Space, Xiaomi Hidden Apps, and older LG Hide Apps are primarily launcher/privacy-password features. They do not provide a public launcher API. A replacement launcher must verify that it does not accidentally reveal apps the user hid through the OEM launcher.
5. **File vault** — features named Private Safe or Content Lock often protect files, not apps. They are out of scope.

Mink should therefore support **standard Android Private Space fully**, support an OEM container only as a **vendor-owned gateway after real-device certification**, and otherwise hide the feature. Private OEM activities and undocumented intents should not be shipped.

## Standard Android contract

Android's supported launcher contract is the only full integration target:

- Require Android 15 or newer, the default `ROLE_HOME`, and `ACCESS_HIDDEN_PROFILES`.
- Identify `android.os.usertype.profile.PRIVATE` through `LauncherApps.getLauncherUserInfo()`.
- Keep private apps in a separate container.
- Use `UserManager.isQuietModeEnabled()` and `requestQuietModeEnabled()` for lock state.
- Remove private apps from search and every normal app surface while locked.
- Listen for `ACTION_PROFILE_AVAILABLE` and `ACTION_PROFILE_UNAVAILABLE`.
- Android 16 adds `LauncherApps.getPrivateSpaceSettingsIntent()`, but the returned settings sender is not sufficient evidence that an OEM actually exposes a working feature.

Sources:

- [AOSP Private Space architecture](https://source.android.com/docs/security/features/private-space)
- [Android 15 launcher requirements](https://developer.android.com/about/versions/15/behavior-changes-all#private-space-launcher-apps)
- [LauncherApps API](https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps.html)

## OEM findings

### Samsung

**Feature:** Secure Folder / Knox workspace.

**Finding:** Secure Folder can present a profile that resembles Android Private Space to launcher APIs, but Samsung keeps its apps inside the Secure Folder experience. On tested hardware, lock-state calls affect Secure Folder authentication while `LauncherApps.getActivityList()` remains empty.

**Safe capability:** Open the installed, exported Samsung Secure Folder launcher entry and let Samsung perform authentication and app selection.

**Do not:** Describe Secure Folder as Android Private Space, show an empty standard app list, enumerate with private APIs, or hard-code an internal activity class.

**Current confidence:** Gateway prototype works conceptually, but it should remain experimental until tested across One UI versions and states: unset, configured, hidden, locked, unlocked, disabled, and uninstalled.

Source: [Samsung Secure Folder usage](https://docs.samsungknox.com/secure-folder/Content/header-use.htm)

### Motorola

**Feature:** Moto Secure / Secure Folder.

**Finding:** Motorola documents Moto Secure as the public entrance to Secure Folder. Secure Folder can use a separate credential, contain protected apps, and be hidden using Stealth mode. The official Moto Secure package is `com.motorola.securityhub`.

**Safe capability:** On Motorola hardware, open the installed, exported Moto Secure launcher entry and let Motorola perform authentication and Secure Folder navigation.

**Do not:** Guess a Secure Folder activity, enumerate its protected apps, or reveal a hidden folder entrance. If Moto Secure is missing or has no exported launcher entry, Mink should not show the certified gateway.

**Current confidence:** The package-level gateway is safe and documented, but remains beta until tested on Motorola hardware with Secure Folder unset, configured, locked, unlocked, disguised, hidden, and disabled.

Sources:

- [Motorola Secure Folder guidance](https://en-us.support.motorola.com/app/answers/detail/a_id/175941/)
- [Moto Secure on Google Play](https://play.google.com/store/apps/details?id=com.motorola.securityhub)

### Xiaomi and Redmi

**Features:** Second Space, Hidden Apps, and Dual Apps.

- Second Space is a separate environment with its own apps, accounts, files, settings, and credential. Xiaomi documents switching through the lock-screen credential, Settings, or an optional Home shortcut.
- Hidden Apps is an OEM-launcher surface entered through a two-finger gesture.
- Dual Apps is app cloning, not a privacy container.

**Safe capability:** Do not enumerate Second Space from Main Space. A user-selected or system-pinned `Switch space` shortcut could be retained as an opaque gateway if it remains launchable under Mink.

**Risk:** Hidden Apps may only be hidden from Xiaomi's launcher. Hardware testing must prove that Mink's ordinary drawer, Magic Box, shortcut chooser, and launcher-shortcut results do not reveal those packages.

**Do not:** Ship known internal `com.miui.*` activity names. They are undocumented, version-dependent, and have previously changed security/export behavior.

Sources:

- [Xiaomi Second Space behavior and switching](https://www.mi.com/es/support/faq/details/KA-1104308/)
- [Xiaomi Hidden Apps](https://www.mi.com/uk/support/faq/details/KA-539968/)
- [Xiaomi Second Space security model](https://trust.mi.com/pdf/MIUI12_Security_White_Paper.pdf)

### OPPO

**Features:** Private System/System Cloner, Hide Apps, and Private Safe.

- OPPO describes Private System as an independent copy of apps and data entered using another fingerprint or password.
- Private Safe is a file vault and is not an app container.
- Availability and naming vary by ColorOS generation and region.

**Safe capability:** Treat System Cloner as a separate system. Only open a documented, exported gateway or a user-selected shortcut after hardware verification.

**Risk:** OPPO's current ColorOS 15 public feature page does not document a third-party launcher contract. ColorOS-family implementations and labels change substantially between releases.

Sources:

- [OPPO Private System description](https://www.oppo.com/en/newsroom/press/oppo-launches-coloros-11-globally-with-rich-customization-in-the-android-11-rollout-s-first-wave/)
- [ColorOS 11 user guide](https://ipics.oppo.com/oppo_nl/answer/ColorOS_11.1_User_Guide_V1.1.pdf)
- [ColorOS 15](https://www.oppo.com/en/coloros15/)

### OnePlus

**Features:** Hidden Space/Hide Apps, Private Safe, and System Cloner on some ColorOS-derived versions or markets.

- Older Hidden Space is part of the OnePlus launcher and intentionally removes apps from launcher search.
- Private Safe protects files.
- System Cloner, where present, should be treated as another system rather than an enumerable app profile.

**Safe capability:** No generic integration is currently justified. A documented/exported gateway or user-selected shortcut could be certified per OxygenOS generation.

**Risk:** The same device family may expose a different feature depending on OxygenOS version and market firmware.

Sources:

- [OnePlus Hidden Space manual](https://service.oneplus.com/content/dam/support/user-manuals/common/OnePlus_6T_User_Manual_EN.pdf)
- [OnePlus Private Safe](https://www.oneplus.com/global/oxygenos12)

### realme

**Features:** Hide Apps, System Cloner, Private Safe, and App Cloner.

- Hide Apps is accessed using a privacy access code in the dialer.
- System Cloner is isolated from the main system and entered using a separate password or fingerprint.
- realme explicitly says App Cloner does not work with third-party launchers; it is not a privacy-container integration target.

**Safe capability:** Treat System Cloner as another system. Do not bypass the dialer or credential flow. A vendor-owned exported gateway may be certified if one exists on real hardware.

**Risk:** Feature availability differs across realme UI full, R, Go, S, T, and U editions.

Sources:

- [realme System Cloner](https://www.realme.com/in/support/kw/doc/2058696)
- [realme Hide Apps](https://www.realme.com/in/support/kw/doc/2043722)
- [realme privacy feature distinctions](https://www.realme.com/in/support/kw/doc/2073877)
- [realme App Cloner third-party launcher limitation](https://www.realme.com/br/support/kw/doc/2232256)

### vivo

**Feature:** App Hiding protected by a privacy password.

**Finding:** vivo documents viewing hidden apps through Settings or a two-finger upward gesture on its Home screen. Some versions completely close hidden apps and remove them from Home, Recents, and other app lists.

**Safe capability:** None without hardware proof. The OEM gesture cannot be recreated reliably by Mink and there is no documented public gateway API.

**Critical test:** Confirm that the package is also absent from Mink's launcher queries. If it remains visible, Mink must not claim compatibility with vivo App Hiding.

Sources:

- [vivo privacy and app-encryption guide](https://asia-exstatic-vivofs.vivo.com/PSee2l50xoirPK7y/1740713719699/ccedd0784d8e3510d11a8cb348ed56e9.pdf)
- [vivo hidden-app behavior](https://asia-exstatic-vivofs.vivo.com/PSee2l50xoirPK7y/1709715366998/f9532f1c8e2f58c1c7f9b8a7cfeba7bc.pdf)

### Huawei

**Feature:** PrivateSpace.

**Finding:** Huawei describes PrivateSpace as an independent user space based on Android's native multi-user mechanism. Users enter it using a separate lock-screen credential/fingerprint or through Huawei Settings. It is not the Android 15 private-profile contract.

**Safe capability:** Do not enumerate its apps from MainSpace. A Settings or lock-screen handoff remains Huawei-owned. There is no documented public launcher API for a third-party gateway.

**Risk:** The PrivateSpace entrance itself can be intentionally hidden. Mink must not expose its existence when the user chose to hide it.

Sources:

- [Huawei PrivateSpace setup and hidden entrance](https://consumer.huawei.com/en/support/content/en-us15834600/)
- [Huawei PrivateSpace support detection](https://consumer.huawei.com/en/support/content/en-us15786103/)
- [Huawei security white paper](https://consumer.huawei.com/content/dam/huawei-cbg-site/en/mkt/legal/privacy-policy/EMUI%208.0%20Security%20Technology%20White%20Paper.pdf)

### Honor

**Features:** Parallel Space on current MagicOS and PrivateSpace on older models.

**Finding:** Parallel Space is independent but can operate simultaneously with MainSpace. Honor documents that enabling it automatically creates a Home shortcut. That makes Honor the strongest candidate for an opaque shortcut-based gateway, but not for app enumeration.

**Safe capability:** Capture or let the user select Honor's generated Parallel Space shortcut. Launch it without interpreting its target or contained apps.

**Risk:** Older Honor devices use Huawei-style PrivateSpace instead. Detection must be capability-based, not based only on manufacturer.

Sources:

- [Honor Parallel Space](https://www.honor.com/mea/support/content/en-us15857429/)
- [Honor MagicOS 9 Parallel Space](https://www.honor.com/cn/support/content/zh-cn15878851/)

### LG

**Features found:** OEM-launcher Hide/Show Apps and Content Lock for files.

**Finding:** The official material located describes hiding icons in LG's own Apps screen and protecting Gallery/QuickMemo content. No current Android 15 private-app profile or documented third-party launcher gateway was found.

**Safe capability:** None for this feature. Standard Android Private Space can still be supported if a future/relevant LG build actually exposes the standard profile contract.

**Priority:** Lowest. Do not add a legacy LG-specific adapter.

Sources:

- [LG launcher Hide/Show Apps](https://www.lg.com/us/mobile-phones/VS415PP/Userguide/044.html)
- [LG Content Lock](https://www.lg.com/us/accessibility/mobile/how)

## What Mink can safely build

### 1. Standard backend

`AndroidPrivateSpaceBackend`

- Full app list.
- Standard lock/unlock.
- Standard broadcasts and hidden-state handling.
- Strict isolation from Magic Box, See All, shortcuts, app history, bubbles, and pinned shortcuts.

### 2. Opaque OEM gateway backend

`OemPrivateContainerGateway`

- Displays only a vendor-owned name/icon and `Open` button.
- Opens a launchable exported package/activity or a user-approved launcher shortcut.
- Never enumerates contained apps.
- Never changes container state itself.
- Never guesses an internal component.
- Supported only after testing the exact OEM/OS family.

### 3. User-selected gateway

This is safer and more scalable than a large package-name table:

- Let the user choose an already-visible launcher activity or launcher shortcut as their device's private-area gateway.
- Clearly state that Mink only stores and opens the selected shortcut.
- Validate the target before every launch and clear it if the package/shortcut disappears.
- Do not offer arbitrary intent entry or accept a copied component name.
- Do not treat selection as proof that hidden apps are protected from Mink's normal lists.

Honor's automatically-created Parallel Space shortcut and Xiaomi's optional `Switch space` shortcut are the first hardware candidates for this path.

### 4. Unsupported state

- Do not show the `Privacy apps` button merely because Android exposes a settings sender or a profile-like flag.
- Do not fall back to generic Security Settings.
- Do not show a setup path Mink cannot verify.
- Keep unsupported OEM containers out of user-facing compatibility claims.

## Hardware certification matrix

Each OEM/OS family needs these tests on a real device:

1. Record manufacturer, model, region/firmware, Android version, OEM OS version, build fingerprint, and whether Mink is the default Home app.
2. Capture launcher-visible profiles and each `LauncherUserInfo.userType` before setup, while locked, and while unlocked.
3. Capture launchable activities per visible profile before setup, while locked, and while unlocked.
4. Hide one harmless test app through the OEM feature.
5. Verify that app is absent from:
   - Home shortcut selection.
   - Top drawer apps.
   - See All.
   - `?` Magic Box results.
   - Messaging/AI/app-provider pickers where relevant.
   - Pinned and developer shortcut results.
   - Bubble configuration.
6. Verify that unlocking never injects private apps into normal Mink surfaces.
7. Verify gateway behavior after reboot, OEM-feature hiding, credential change, feature disablement, and package/system update.
8. Verify no private-container existence is revealed when the OEM offers a “hide entrance” option.

No report should include the user's private app names. The diagnostic output should use package hashes or a known test package.

## Recommended release decision

1. Keep the current OEM work experimental on `v156`.
2. Keep `Privacy Space` marked as beta until its OEM gateways pass hardware certification.
3. Certify the standard Android profile on Pixel/AOSP first.
4. Certify Samsung only as a Secure Folder gateway.
5. Certify Motorola only as a Moto Secure gateway.
6. Prototype the user-selected shortcut gateway for Honor and Xiaomi.
7. Test app-hiding leakage before doing any work on vivo, realme, OnePlus, or LG.
8. Treat Huawei, Xiaomi Second Space, OPPO Private System, and realme System Cloner as separate-system handoffs, never app drawers.

## Go/no-go criteria for an OEM gateway

An OEM gateway may ship only when all answers are yes:

- Is the gateway public/exported or supplied through a standard launcher shortcut?
- Does it survive reboot and an OEM system update?
- Does the OEM own authentication and app selection?
- Does Mink avoid learning or storing the contained app list?
- Does Mink respect an OEM option that hides the container's entrance?
- Are OEM-hidden apps absent from every ordinary Mink discovery surface?
- Has the behavior been verified on real hardware for that OS family?

If any answer is no or unknown, the OEM adapter remains disabled.
