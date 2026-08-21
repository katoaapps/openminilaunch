# Publishing a GitHub APK

GitHub APKs are published as release assets instead of being committed to the
source tree. This keeps repository history small and keeps packaged binaries
out of the source F-Droid scans.

## One-time signing setup

The upstream release key lives only in `.signing/`, which is ignored by Git.
Back up both `.signing/openminilaunch-release.jks` and
`.signing/release.properties` in a secure password manager or encrypted backup.
Losing this key means existing GitHub installs cannot be updated.

Authenticate GitHub CLI as the `katoaapps` account, then run:

```shell
tools/configure_github_release_secrets.sh
```

The script refuses to continue under another GitHub identity and adds these
encrypted GitHub Actions repository secrets without printing their values:

- `ANDROID_SIGNING_KEY_BASE64`: Base64 contents of the JKS file
- `ANDROID_KEYSTORE_PASSWORD`: `storePassword` from `release.properties`
- `ANDROID_KEY_ALIAS`: `keyAlias` from `release.properties`
- `ANDROID_KEY_PASSWORD`: `keyPassword` from `release.properties`

On macOS, copy the JKS value without printing it to the terminal:

```shell
base64 -i .signing/openminilaunch-release.jks | pbcopy
```

Copy each remaining value from `.signing/release.properties` directly into its
matching GitHub secret.

## Publish

After the release commit is approved and pushed, create and push its version
tag. The `Publish GitHub APK` workflow tests the tagged source, builds and
verifies a signed APK, and creates a GitHub Release containing:

- `MinkLauncher-OpenSource.apk`
- `MinkLauncher-OpenSource.apk.sha256`

The permanent latest-build URL is:

<https://github.com/katoaapps/openminilaunch/releases/latest/download/MinkLauncher-OpenSource.apk>

## Signing-channel warning

The GitHub APK uses the Katoa Apps upstream key. The standard F-Droid APK uses
F-Droid's key. Android cannot update one with the other; changing channels
requires uninstalling the current app first, which clears its local data.
