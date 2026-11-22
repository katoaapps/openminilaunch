package org.sableos.design

import android.annotation.SuppressLint
import android.content.Context
import java.util.Locale

/**
 * Translatable text for a Sable app (the same contract as the platform apps' SableTexts). [english] holds every
 * user-visible line by key, in Android format syntax (`%1$s`), and is what pure tests read. On a device [lookup]
 * is bound to the app's string resources ([bindTo]), so a `values-<lang>/strings.xml` translation is used when
 * the phone's language has one and English otherwise. `values/strings.xml` repeats [english]; each app's tests
 * keep the two equal.
 */
class SableTexts(
    val english: Map<String, String>,
) {
    @Volatile
    var lookup: (String) -> String? = { null }

    /** The line [key] in the current language, with [args] filled in. An unknown key is a programming error. */
    operator fun invoke(
        key: String,
        vararg args: Any?,
    ): String {
        val template = lookup(key) ?: english.getValue(key)
        return if (args.isEmpty()) template else String.format(Locale.getDefault(), template, *args)
    }

    /**
     * Reads lines from [context]'s string resources, by the same keys, so the phone's language picks the
     * translation. Call it in `onCreate` (and in receivers and services before they show text): a language
     * change recreates the activity and binds again.
     */
    @SuppressLint("DiscouragedApi")
    fun bindTo(context: Context) {
        val res = context.resources
        val pkg = context.packageName
        lookup = { key ->
            val id = res.getIdentifier(key, "string", pkg)
            if (id == 0) null else res.getString(id)
        }
    }
}
