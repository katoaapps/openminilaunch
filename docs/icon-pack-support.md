# Icon pack support

MinkLauncher OpenSource discovers installed icon packs through the established
Nova and ADW theme intent actions. It does not download packs, contact their
developers, or send the selected pack off the device.

## First-pass compatibility

- `com.novalauncher.THEME` and `org.adw.ActivityStarter.THEMES` discovery
- `res/xml/appfilter.xml`, `res/raw/appfilter.xml`, and `assets/appfilter.xml`
- Static `<item>` component-to-drawable mappings
- Nova-compatible `<calendar>` mappings for day-of-month icons
- Package-level fallback for Android app-published shortcuts
- Work-profile badging after a themed icon is loaded

When a pack does not map an app, Mink uses that app's original system icon.
Legacy shortcuts installed by browsers or older apps retain their own icon
because an icon pack cannot reliably identify the shortcut's destination.

Manual alternate-icon selection through `drawable.xml`, icon masks, icon
background generation, wallpapers, and dock themes are outside this first pass.

## Implementation boundaries

The `features/iconpacks` package keeps each external concern separate:

- `IconPackContract` lists supported discovery actions and mapping locations.
- `IconPackDiscovery` finds installed packs.
- `IconPackDefinitionLoader` reads the first valid mapping file.
- `IconPackXmlParser` converts XML into launcher-independent mappings.
- `IconPackDrawableLoader` resolves static and day-specific drawable resources.
- `IconPackRepository` owns caching, invalidation, target lookup, and work badges.

New third-party formats should be added at the contract and loader/parser boundary
instead of adding format-specific branches to Compose UI code.
