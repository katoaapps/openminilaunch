package com.katoaapps.openminilaunch.features.ai

import androidx.annotation.DrawableRes
import com.katoaapps.openminilaunch.R

/** Known AI providers whose Android handoff behavior Mink has explicitly reviewed. */
internal object AiProviderCatalog {
    const val LUMO_PLAY_PACKAGE = "me.proton.android.lumo"
    const val LUMO_NO_GMS_PACKAGE = "me.proton.lumo"

    val providers = listOf(
        textShareProvider(
            id = "chatgpt",
            labelRes = R.string.provider_chatgpt,
            packageName = "com.openai.chatgpt",
            installUrl = "https://chatgpt.com/download/",
            bundledIconRes = R.drawable.ai_provider_chatgpt,
        ),
        textShareProvider(
            id = "claude",
            labelRes = R.string.provider_claude,
            packageName = "com.anthropic.claude",
            installUrl = "https://claude.com/download",
            bundledIconRes = R.drawable.ai_provider_claude,
        ),
        textShareProvider(
            id = "perplexity",
            labelRes = R.string.provider_perplexity,
            packageName = "ai.perplexity.app.android",
            installUrl = "https://www.perplexity.ai/",
            bundledIconRes = R.drawable.ai_provider_perplexity,
        ),
        textShareProvider(
            id = "copilot",
            labelRes = R.string.provider_copilot,
            packageName = "com.microsoft.copilot",
            installUrl = "https://www.microsoft.com/en-us/microsoft-copilot/for-individuals/get-copilot",
            bundledIconRes = R.drawable.ai_provider_copilot,
        ),
        textShareProvider(
            id = "deepseek",
            labelRes = R.string.provider_deepseek,
            packageName = "com.deepseek.chat",
            installUrl = "https://download.deepseek.com/",
            bundledIconRes = R.drawable.ai_provider_deepseek,
        ),
        textShareProvider(
            id = "meta_ai",
            labelRes = R.string.provider_meta_ai,
            packageName = "com.facebook.stella",
            installUrl = "https://www.meta.ai/",
            bundledIconRes = R.drawable.ai_provider_meta_ai,
        ),
        textShareProvider(
            id = "gemini",
            labelRes = R.string.provider_gemini,
            packageName = "com.google.android.apps.bard",
            installUrl = "https://gemini.google.com/app/download",
            bundledIconRes = R.drawable.ai_provider_gemini,
        ),
        AiProvider(
            id = "lumo",
            labelRes = R.string.provider_lumo,
            packageName = LUMO_PLAY_PACKAGE,
            alternatePackageNames = listOf(LUMO_NO_GMS_PACKAGE),
            handoffMode = AiHandoffMode.COPY_AND_LAUNCH,
            installUrl = "https://proton.me/lumo/download",
            bundledIconRes = R.drawable.ai_provider_lumo,
        ),
    )

    val curatedPackages: Set<String> = providers.flatMap(AiProvider::packageNames).toSet()

    val launchOnlyPackages: Set<String> = providers
        .filter { it.handoffMode == AiHandoffMode.COPY_AND_LAUNCH }
        .flatMap(AiProvider::packageNames)
        .toSet()

    fun providerForPackage(packageName: String?): AiProvider? =
        providers.firstOrNull { packageName in it.packageNames }

    fun handoffMode(packageName: String): AiHandoffMode =
        providerForPackage(packageName)?.handoffMode ?: AiHandoffMode.TEXT_SHARE

    private fun textShareProvider(
        id: String,
        labelRes: Int,
        packageName: String,
        installUrl: String,
        @DrawableRes bundledIconRes: Int,
    ) = AiProvider(
        id = id,
        labelRes = labelRes,
        packageName = packageName,
        handoffMode = AiHandoffMode.TEXT_SHARE,
        installUrl = installUrl,
        bundledIconRes = bundledIconRes,
    )
}
