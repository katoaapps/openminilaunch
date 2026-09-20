# Contributing OpenMink translations

OpenMink has two independent language surfaces:

1. Android UI resources in `app/src/main/res/values-<locale>/`.
2. Deterministic calendar phrase modules in `features/calendar/language/<locale>/`.

A translated UI does not automatically make that language available for `+` calendar commands. Calendar support is listed separately in Settings.

## Target language roadmap

OpenMink's long-term target set is listed below. A target does not become a shipped language until its UI resources and calendar grammar have completed review.

| Language | Planned BCP 47 tag | Notes |
| --- | --- | --- |
| English (United States) | `en-US` | Reference locale and current calendar grammar |
| English (United Kingdom) | `en-GB` | First wave: day-month calendar grammar implemented; complete UI resources pending |
| Mandarin Chinese | `zh-Hans` | First wave: Simplified Chinese calendar grammar implemented; UI translation pending |
| Cantonese | `yue-Hant-HK` | Traditional Chinese script and Hong Kong Cantonese grammar |
| Japanese | `ja` | |
| Korean | `ko` | |
| Hindi | `hi` | |
| Spanish | `es` | First wave: calendar grammar implemented; region-neutral UI translation pending |
| French | `fr` | |
| Bengali | `bn` | Bengali-script input plus documented Latin-digit handling |
| Portuguese (Brazil) | `pt-BR` | Confirmed regional baseline |
| Russian | `ru` | |
| Urdu | `ur` | Right-to-left layout |
| Indonesian | `id` | |
| Arabic | `ar` | First wave: calendar grammar implemented; UI translation and RTL review pending |

The app language picker is intentionally generated only from completed locale resources. Do not add roadmap-only tags to `localeFilters` or `SupportedAppLanguages`.

## Protected names and syntax

Do not translate product and provider names or command syntax:

- MinkLauncher OpenSource
- MinkLauncher and Mink
- Mink's Day
- Katoa Apps
- `@`, `#`, `-`, `/`, `+`, and `?`
- app/provider names, URLs, email addresses, package names, and Android permission names

Translate the explanation beside each command symbol.

## UI resource rules

- Translate from the unqualified English resources in `res/values/`.
- Preserve positional placeholders exactly: `%1$s`, `%2$d`, and similar.
- Preserve XML markup and escaping.
- Use Android plural resources instead of building singular/plural sentences in Kotlin.
- Do not translate by concatenating fragments. Ask for a complete contextual resource when needed.
- Add translator notes when a source string is ambiguous.
- Check long text and right-to-left layout; do not shorten away meaning merely to fit.
- Machine translation is useful for a draft, but a fluent human review is required before release.

Incomplete locale folders must not be merged into a release branch. Android's generated locale configuration advertises every packaged locale to users.

## Calendar language rules

Calendar modules are grammar implementations, not translated copies of English regular expressions. Each module must define its own:

- weekday and month forms;
- relative-date wording;
- date ordering and region behavior;
- localized meridiem/noon/midnight forms;
- duration wording;
- temporal-cue detection;
- negative and ambiguity cases.

Language modules emit typed date/time facts and source spans. Shared engine code resolves those facts against the injected clock and time zone. Modules must not launch Android intents or silently choose a date when the language is ambiguous.

Every calendar language requires a fixed corpus covering:

- supported happy paths;
- abbreviations, accents, punctuation, and capitalization;
- impossible and ambiguous dates;
- multiple/conflicting date or time phrases;
- time-only rollover;
- month/year rollover and leap years;
- daylight-saving gaps and overlaps;
- ordinary sentences containing calendar vocabulary;
- unsupported temporal wording that must return `NeedsReview`.

## Before requesting review

- Confirm all placeholders and plural forms match English.
- Inspect the locale with a long-text screen and, where applicable, RTL layout.
- Exercise Home, Settings, onboarding, To-dos, Widgets, Conversations, All Apps, Mink's Day, and Magic Box.
- Verify physical-keyboard command symbols are unchanged.
- Verify calendar previews show the exact date/time handed to the provider.
- Verify unsupported temporal wording asks for review instead of scheduling a partial match.
- Include the translator/reviewer names or handles in the pull request when they want attribution.

OpenMink does not upload phrases for translation or parsing. UI resources and calendar modules ship inside the APK and run on the device.
