package com.katoaapps.openminilaunch.features.localization

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import java.util.Locale

internal data class SupportedAppLanguage(val languageTag: String) {
    fun nativeDisplayName(): String {
        val locale = Locale.forLanguageTag(languageTag)
        return locale.getDisplayName(locale).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }
}

internal object SupportedAppLanguages {
    val all = listOf(SupportedAppLanguage("en-US"))

    fun isSupportedByDevice(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun selectedLanguageTag(context: Context): String? {
        if (!isSupportedByDevice()) return null
        return localeManager(context).applicationLocales.get(0)?.toLanguageTag()
    }

    fun useSystemLanguage(context: Context) {
        if (!isSupportedByDevice()) return
        localeManager(context).applicationLocales = LocaleList.getEmptyLocaleList()
    }

    fun select(context: Context, languageTag: String) {
        require(all.any { it.languageTag == languageTag })
        if (!isSupportedByDevice()) return
        localeManager(context).applicationLocales = LocaleList.forLanguageTags(languageTag)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun localeManager(context: Context): LocaleManager =
        context.getSystemService(LocaleManager::class.java)
}
