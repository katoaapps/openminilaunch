package com.katoaapps.openminilaunch.features.games

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.katoaapps.openminilaunch.model.LauncherActionTarget
import org.sableos.games.SableGameEntry
import org.sableos.games.SableGames

/**
 * Launcher-facing adapter for the embedded Sable Games runtime.
 *
 * Games are launcher-owned destinations, not synthetic installed packages. Keeping that distinction
 * prevents package removal, work-profile, app-info, and bubble behavior from treating them as APKs.
 */
internal class SableGameLauncherCatalog(private val context: Context) {
    fun targets(): List<LauncherActionTarget> = SableGames.catalog.map(::target)

    fun resolve(selectionKey: String): LauncherActionTarget? =
        entry(selectionKey)?.let(::target)

    fun entry(selectionKey: String): SableGameEntry? =
        gameId(selectionKey)?.let(SableGames::entry)

    fun launch(target: LauncherActionTarget): Boolean {
        val entry = entry(target.selectionKey) ?: return false
        return runCatching {
            context.startActivity(
                SableGames.intent(context, entry.id)
                    // Request a distinct document task so launches from the excluded Home task can
                    // be represented independently in Android Recents.
                    .setData(
                        Uri.Builder()
                            .scheme("mink")
                            .authority(context.packageName)
                            .appendPath("game")
                            .appendPath(entry.id)
                            .build(),
                    )
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                            Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS,
                    ),
            )
            true
        }.getOrDefault(false)
    }

    private fun target(entry: SableGameEntry) = LauncherActionTarget(
        label = entry.title,
        packageName = context.packageName,
        selectionKey = selectionKey(entry.id),
    )

    private fun gameId(selectionKey: String): String? = selectionKey
        .takeIf { it.startsWith(SELECTION_PREFIX) }
        ?.removePrefix(SELECTION_PREFIX)
        ?.takeIf(String::isNotBlank)

    companion object {
        const val SELECTION_PREFIX = "internal:sable-game:"

        fun selectionKey(gameId: String): String = "$SELECTION_PREFIX$gameId"
    }
}
