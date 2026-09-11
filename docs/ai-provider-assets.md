# AI provider assets

OpenMink bundles a current icon for every provider in `AiProviderCatalog` so the AI picker can
identify reviewed providers even when their apps are not installed. When an app is installed,
Android's `PackageManager` remains the source of truth for its current label and icon.

The bundled files were downloaded on September 10, 2026. Except for Microsoft Copilot, each icon
comes from the developer-supplied 512 x 512 artwork exposed by the app's official Google Play
listing. The Copilot Play listing returned HTTP 404 to the retrieval client, so its 512 x 512 icon
comes from Microsoft's official App Store listing instead.

These provider names and icons are used only for nominative identification. Their inclusion does
not imply endorsement, sponsorship, or partnership. The marks remain the property of their
respective owners.

| Provider | Runtime package | Bundled artwork | Artwork source | Install information |
| --- | --- | --- | --- | --- |
| ChatGPT | `com.openai.chatgpt` | `ai_provider_chatgpt.png` | [Google Play](https://play.google.com/store/apps/details?id=com.openai.chatgpt) | [ChatGPT](https://chatgpt.com/download/) |
| Claude | `com.anthropic.claude` | `ai_provider_claude.png` | [Google Play](https://play.google.com/store/apps/details?id=com.anthropic.claude) | [Anthropic](https://claude.com/download) |
| Perplexity | `ai.perplexity.app.android` | `ai_provider_perplexity.png` | [Google Play](https://play.google.com/store/apps/details?id=ai.perplexity.app.android) | [Perplexity](https://www.perplexity.ai/) |
| Microsoft Copilot | `com.microsoft.copilot` | `ai_provider_copilot.png` | [App Store](https://apps.apple.com/us/app/microsoft-copilot/id6738511300) | [Microsoft](https://www.microsoft.com/en-us/microsoft-copilot/for-individuals/get-copilot) |
| DeepSeek | `com.deepseek.chat` | `ai_provider_deepseek.png` | [Google Play](https://play.google.com/store/apps/details?id=com.deepseek.chat) | [DeepSeek](https://download.deepseek.com/) |
| Meta AI | `com.facebook.stella` | `ai_provider_meta_ai.png` | [Google Play](https://play.google.com/store/apps/details?id=com.facebook.stella) | [Meta AI](https://www.meta.ai/) |
| Google Gemini | `com.google.android.apps.bard` | `ai_provider_gemini.png` | [Google Play](https://play.google.com/store/apps/details?id=com.google.android.apps.bard) | [Gemini](https://gemini.google.com/app/download) |
| Lumo | `me.proton.android.lumo`, `me.proton.lumo` | `ai_provider_lumo.png` | [Google Play](https://play.google.com/store/apps/details?id=me.proton.android.lumo) | [Proton](https://proton.me/lumo/download) |

## Rendering rules

- Installed apps use the icon returned by Android, not the bundled copy.
- Unavailable reviewed providers use the bundled icon and open the provider-owned install page.
- Do not recolor, redraw, add effects to, or crop the provider artwork.
- Never allow an unavailable provider to become the saved AI package.
- Refresh artwork only from the same developer's official store listing, then verify its identity,
  dimensions, PNG encoding, and Android resource build.
