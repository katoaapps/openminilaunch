# Contributing OpenMink translations

OpenMink has two technical language surfaces that share one user-facing **Launcher language** setting:

1. Android UI resources in `app/src/main/res/values-<locale>/`.
2. Deterministic calendar phrase modules in `features/calendar/language/<locale>/`.

Changing the Launcher language changes both surfaces. A translated UI does not automatically make that language available for `+` calendar commands, however. When a matching calendar grammar is unavailable or cannot confidently understand a phrase, OpenMink must ask the user to review it instead of guessing.

OpenMink does not upload phrases for translation or parsing. UI resources and calendar modules ship inside the APK and run on the device.

## Target language roadmap

US English is the reference locale. Machine-generated UI drafts now exist for every target below, but a locale does not become release-ready until its UI resources have received fluent human review. Calendar grammar support is reviewed separately.

| Language | BCP 47 tag | UI status | Calendar status |
| --- | --- | --- | --- |
| English (United States) | `en-US` | Reference locale | Implemented |
| English (United Kingdom) | `en-GB` | Machine draft; review required | Implemented |
| Mandarin Chinese | `zh-Hans` | Machine draft; review required | Implemented |
| Cantonese | `yue-Hant-HK` | Machine draft; review required | Planned |
| Japanese | `ja` | Machine draft; review required | Planned |
| Korean | `ko` | Machine draft; review required | Planned |
| Hindi | `hi` | Machine draft; review required | Planned |
| Spanish | `es` | Machine draft; review required | Implemented |
| French | `fr` | Machine draft; review required | Planned |
| Bengali | `bn` | Machine draft; review required | Planned |
| Portuguese (Brazil) | `pt-BR` | Machine draft; review required | Planned |
| Russian | `ru` | Machine draft; review required | Planned |
| Urdu | `ur` | Machine draft; review required | Planned |
| Indonesian | `id` | Machine draft; review required | Planned |
| Arabic | `ar` | Machine draft; review required | Implemented |

The app language picker is generated only from packaged locale resources. Do not add a roadmap-only locale to `localeFilters` or `SupportedAppLanguages` until its resource catalog is complete and validated.

## Protected English names and syntax

The following brand names always remain exactly in plain English in every locale:

- Mink
- Mink Launcher
- Mink's Day

Also preserve the established spellings of `MinkLauncher`, `OpenMink`, `MinkLauncher OpenSource`, `Magic Box`, and `Katoa Apps` wherever they appear. Do not translate product or provider names.

Keep `@`, `#`, `-`, `/`, `+`, and `?` unchanged. Translate the explanation beside each command symbol, not the symbol itself. Preserve URLs, email addresses, package names, file extensions, and Android permission identifiers.

## UI resource rules

- Translate from the unqualified English resources in `res/values/`.
- Preserve positional placeholders such as `%1$s`, `%2$d`, and `%%` exactly.
- Preserve explicit `\n` line breaks, XML markup, and escaping.
- Use Android plural resources instead of building singular/plural sentences in Kotlin.
- Review plurals using the target language's rules rather than translating English forms literally.
- Do not translate by concatenating fragments. Ask for a complete contextual resource when needed.
- Prefer natural, concise mobile-interface language over word-for-word translation.
- Add translator notes when a source string is ambiguous.
- Check privacy and permission explanations especially carefully.
- Check long text and right-to-left layout; do not shorten away meaning merely to fit.
- Machine translation is useful for a draft, but fluent human review is required before release.

Incomplete or unreviewed locale folders must not ship in a production release. Android's generated locale configuration advertises every packaged locale to users.

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

- Run `python3 tools/validate_localizations.py`.
- Confirm all placeholders and plural forms match English.
- Inspect the locale with a long-text screen and, where applicable, RTL layout.
- Exercise Home, Settings, onboarding, To-dos, Widgets, Conversations, All Apps, Mink's Day, and Magic Box.
- Verify that Mink, Mink Launcher, and Mink's Day remain in plain English.
- Verify physical-keyboard command symbols are unchanged.
- Verify calendar previews show the exact date/time handed to the provider.
- Verify unsupported temporal wording asks for review instead of scheduling a partial match.
- Include the translator/reviewer names or handles in the pull request when they want attribution.
