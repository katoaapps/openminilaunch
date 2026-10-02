# F-Droid updates

MinkLauncher OpenSource is already included in the official F-Droid repository. The production
build recipe lives in `fdroid/fdroiddata`; the YAML in this directory is a historical local copy and
must be refreshed from production before it is used for review or troubleshooting.

Normal tagged releases use F-Droid's configured version check and auto-update flow:

1. Commit and publish the source release on GitHub with a monotonically increasing Android
   `versionCode` and a matching `v<versionName>` tag.
2. Confirm the tag resolves to the intended immutable commit and contains no generated APKs or
   private signing material.
3. Allow F-Droid's auto-update bot to propose the new build. Open a manual fdroiddata update only
   when the bot cannot derive the release correctly or the recipe itself must change.
4. When reviewing or proposing a recipe update, start from the current production metadata and run:

   ```shell
   fdroid readmeta
   fdroid rewritemeta com.katoaapps.openminilaunch
   fdroid checkupdates --allow-dirty com.katoaapps.openminilaunch
   fdroid lint com.katoaapps.openminilaunch
   fdroid build com.katoaapps.openminilaunch
   ```

5. Use the full immutable source commit hash in a manual build entry. Do not use a branch or tag as
   the build commit.

F-Droid builds and signs its APK independently from tagged source. Do not upload the GitHub release
APK to fdroiddata, reuse private signing material, or assume the GitHub and F-Droid APKs are
signature-compatible.
