# Messaging provider assets

OpenMink bundles a current icon for every provider in `MessagingProviderCatalog` so the preferred
app picker can identify supported services even when they are not installed. When an app is
installed, Android's `PackageManager` remains the source of truth for its current label and icon.

The bundled files were downloaded from each app's official Google Play listing on August 26,
2026. Each listing exposes its developer-supplied 512 x 512 icon through `og:image`. The Viber
listing returned JPEG data, which was converted to PNG for Android resource consistency; the
other downloads were already PNG files.

These provider names and icons are used only for nominative identification. Their inclusion does
not imply endorsement, sponsorship, or partnership. The marks remain the property of their
respective owners.

| Provider | Runtime package | Bundled artwork | Official listing |
| --- | --- | --- | --- |
| WhatsApp | `com.whatsapp` | `messaging_provider_whatsapp.png` | [Google Play](https://play.google.com/store/apps/details?id=com.whatsapp) |
| WhatsApp Business | `com.whatsapp.w4b` | `messaging_provider_whatsapp_business.png` | [Google Play](https://play.google.com/store/apps/details?id=com.whatsapp.w4b) |
| Telegram | `org.telegram.messenger` | `messaging_provider_telegram.png` | [Google Play](https://play.google.com/store/apps/details?id=org.telegram.messenger) |
| LINE | `jp.naver.line.android` | `messaging_provider_line.png` | [Google Play](https://play.google.com/store/apps/details?id=jp.naver.line.android) |
| Signal | `org.thoughtcrime.securesms` | `messaging_provider_signal.png` | [Google Play](https://play.google.com/store/apps/details?id=org.thoughtcrime.securesms) |
| KakaoTalk | `com.kakao.talk` | `messaging_provider_kakaotalk.png` | [Google Play](https://play.google.com/store/apps/details?id=com.kakao.talk) |
| WeChat | `com.tencent.mm` | `messaging_provider_wechat.png` | [Google Play](https://play.google.com/store/apps/details?id=com.tencent.mm) |
| Viber | `com.viber.voip` | `messaging_provider_viber.png` | [Google Play](https://play.google.com/store/apps/details?id=com.viber.voip) |
| Messenger | `com.facebook.orca` | `messaging_provider_messenger.png` | [Google Play](https://play.google.com/store/apps/details?id=com.facebook.orca) |
| Slack | `com.Slack` | `messaging_provider_slack.png` | [Google Play](https://play.google.com/store/apps/details?id=com.Slack) |
| Microsoft Teams | `com.microsoft.teams` | `messaging_provider_teams.png` | [Google Play](https://play.google.com/store/apps/details?id=com.microsoft.teams) |
| Discord | `com.discord` | `messaging_provider_discord.png` | [Google Play](https://play.google.com/store/apps/details?id=com.discord) |
| Snapchat | `com.snapchat.android` | `messaging_provider_snapchat.png` | [Google Play](https://play.google.com/store/apps/details?id=com.snapchat.android) |
| GroupMe | `com.groupme.android` | `messaging_provider_groupme.png` | [Google Play](https://play.google.com/store/apps/details?id=com.groupme.android) |
| Zalo | `com.zing.zalo` | `messaging_provider_zalo.png` | [Google Play](https://play.google.com/store/apps/details?id=com.zing.zalo) |
| TextNow | `com.enflick.android.TextNow` | `messaging_provider_textnow.png` | [Google Play](https://play.google.com/store/apps/details?id=com.enflick.android.TextNow) |
| TextFree | `com.pinger.textfree` | `messaging_provider_textfree.png` | [Google Play](https://play.google.com/store/apps/details?id=com.pinger.textfree) |

## Rendering rules

- Installed apps use the icon returned by Android, not the bundled copy.
- Unavailable supported apps use the bundled icon with the surrounding row visually disabled.
- Do not recolor, redraw, add effects to, or crop the provider artwork.
- Never allow an unavailable provider to become the saved preferred package.
- Refresh an asset only from the same package's official listing, then verify the package ID,
  512 x 512 dimensions, PNG encoding, and Android resource build.
