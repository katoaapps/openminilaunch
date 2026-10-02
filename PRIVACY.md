# Privacy

Effective October 1, 2026

MinkLauncher OpenSource is designed to work locally. It does not operate an application server,
include analytics or advertising SDKs, create an account, or upload launcher content or personal
data. Its normal Internet permission is used only for the optional GitHub release-metadata check
described below.

## Information stored on this device

The app stores launcher settings, shortcut and Top 8 choices, to-dos, the five most recent
successful plain-text queries, approved document-folder references, optional cropped Home
wallpaper, widget configuration, Mink’s Day preferences, app-bubble preferences, MinkSpace
mini-app preferences, and onboarding state in app-private storage. Android cloud backup is disabled
for MinkLauncher OpenSource.

MinkSpace stores calculator history locally, up to its visible history limit. Media Lab stores the
content URI, display name, MIME type, order, and Android read grant for media the user explicitly
selects. It does not copy the underlying image, GIF, or video into MinkLauncher storage. Removing a
Media Lab item removes MinkLauncher’s saved reference and releases its grant when Android permits;
it does not delete the original file.

The Music mini-app reads Android’s currently active media-session metadata, artwork, playback state,
position, provider identity, and advertised control capabilities while the feature is being used.
This state is kept in memory rather than recorded as listening history. MinkLauncher does not record
audio and does not request microphone permission.

## Virtual Contact Card

The optional Virtual Contact Card stores contact details, selected links, link order, and an optional
processed photo in AES-GCM encrypted app-private files backed by Android Keystore. It is a contact
card, not an app account or tracking profile, and it is not sent to Mink servers or used for account
management.

vCard QR codes are generated entirely on-device and contain only the identity fields and links the
user selects. The contact photo and fallback Mink icon are not embedded. The system Photo Picker
provides an optional photo without granting MinkLauncher broad photo-library access.

## Portable backup

An explicit portable backup is readable JSON. It includes portable launcher layout and settings,
MinkSpace order and enablement, to-dos, and Virtual Contact Card details and links so a user can move
between distributions. The export screen warns that readable contact details may be present.

Portable backup excludes Android permissions and roles, accessibility and notification access,
widgets, document-folder grants, wallpaper images, recent-query history, calculator history,
selected MinkSpace media and URI grants, active media sessions, Work and Privacy Space state,
device-specific private-area gateways, active bubble notifications, demo data, the Virtual Contact
Card photo, and update caches or reminders. Restoring a card therefore returns to the Mink icon until
another photo is selected.

## Permissions and special access

Optional permissions and special access are used as follows:

- **Contacts:** Searches contact names and labelled phone numbers locally for `@` messaging and `#`
  calling commands.
- **Phone:** Places a call only after the user selects a contact or number and confirms the call in
  MinkLauncher OpenSource. Emergency and failed direct-call routes fall back to Android’s dialer.
- **Messaging:** Sends a carrier SMS only after the user selects a recipient and submits a message
  with System Messages and automatic sending enabled. Direct SMS is available only while
  MinkLauncher OpenSource is the active Android assistant handler. Integrated-app and Android share
  handoffs pass user-entered text, and a recipient when supported, to the selected provider; that
  provider controls the final send under its own terms.
- **Photos, videos, audio, and older shared-storage access:** Searches MediaStore filenames and
  displays locally provided thumbnails when file search is enabled. Media Lab instead uses Android’s
  picker for specific user-selected images, GIFs, and videos.
- **Selected folders:** Searches filenames only inside folders the user approves through Android’s
  Storage Access Framework. MinkLauncher OpenSource does not request broad All files access.
- **Wallpaper:** After the user chooses an image and confirms a device-sized crop, stores that crop
  privately and applies the same file to Android’s Home wallpaper. The source image and crop are not
  uploaded, and the lock-screen wallpaper is not changed.
- **Notification shade:** Permits the launcher’s swipe-down gesture to expand Android’s notification
  panel.
- **Notification access — Conversations:** Reads active Android-standard message and email
  notifications in memory so they can be grouped and replied to through the originating app when it
  supplies a compatible reply action. Conversation history is not stored by MinkLauncher.
- **Notification access — Music:** Authorizes MinkLauncher to query Android’s active media sessions.
  The Music mini-app reads only the active session information Android exposes and sends only
  provider-advertised media commands. It does not inspect unrelated notification contents for music
  control.
- **App bubble notifications:** The optional beta uses Android’s ordinary notification permission to
  publish a local bubble only after the user requests one or selects an app for automatic Magic Box
  bubbling. The notification names the selected app but contains no Magic Box text, message, or
  launcher content. It makes no network request and does not require draw-over-other-apps or
  accessibility access.
- **Usage access:** Reads foreground events only for apps the user includes in Mink’s Day while
  MinkLauncher OpenSource is visible. Other apps are excluded from its trail and totals. The app
  stores the daily limit, pause preference, and app choices but does not create a separate
  usage-history database.
- **Accessibility:** The optional **MinkLauncher OpenSource - Double Tap to Lock Screen** service
  performs only Android’s Lock screen global action after the user double-taps empty Home space. It
  does not subscribe to accessibility events, retrieve window content, perform gestures, or collect
  data.
- **Android assistant role:** Opens the keyboard-first Magic Box over the current app. MinkLauncher
  does not request microphone access, inspect assist context, read the underlying screen, or collect
  information from the app beneath it.
- **Android 15 hidden profiles:** While MinkLauncher OpenSource is the active Home app, Android’s
  normal hidden-profile permission lets it present Private Space in a separate launcher container.
  MinkLauncher reads the labels, icons, launch targets, and lock state Android exposes. Android owns
  authentication and stops exposing contained apps while the space is locked.
- **Internet:** When GitHub update checks are enabled, reads the latest public release tag from GitHub
  at most twice a day. GitHub receives ordinary connection metadata. No launcher content is included,
  and MinkLauncher OpenSource never downloads or installs an APK itself. The check can be disabled in
  **Settings → About**.

## Apps, shortcuts, work profiles, and private areas

As a launcher, MinkLauncher reads installed launchable app labels, icons, package identities, and
developer-published shortcuts that Android makes available. Recent messaging conversations are
derived from compatible shortcuts published locally by installed providers. This discovery data is
used to show and launch choices and is not sent to Katoa Apps.

Accessible Work Apps are shown in a separate drawer with Android’s work badge. MinkLauncher can ask
Android to pause or resume the profile, but Android and the managing organization control the profile,
its policy, and its contents. MinkLauncher does not read work-app account or document contents.

Android Private Space apps are kept outside regular search, shortcuts, Top 8, and See All. On devices
using a manufacturer-specific private area, the user may save a device-owned app or shortcut as a
gateway. MinkLauncher stores and opens only that target; the manufacturer controls authentication and
the apps inside it.

Mink’s Day pausing changes only which selected personal apps MinkLauncher displays or launches. It
does not disable, suspend, modify, or control those apps at the Android system level. The always-on
launcher barrier does not require Usage Access; pausing after a daily limit does.

## Handoffs to other apps

When the user launches another app, opens selected media, composes a message, creates a note or event,
opens a file, delegates a Web or AI query, or shares a vCard QR, the chosen content leaves
MinkLauncher through Android or the user’s camera/recipient. The receiving app, service, person, or
device then controls that copy under its own privacy terms. MinkLauncher does not receive the
provider’s response.

## Deletion

Users can delete to-dos, query history, calculator history, Media Lab selections, Virtual Contact
Card fields and links, app-bubble choices, approved folders, widgets, and other supported items from
their relevant screens. Permissions and special access can be revoked in Android Settings. Clearing
MinkLauncher storage or uninstalling it removes its local app data, subject to files or exports the
user deliberately saved elsewhere.

Privacy questions can be sent to contact@katoaapps.com.
