package org.sableos.games.console

/** Which library tab a game sits under. */
enum class GameShelf(
    val key: String,
) {
    Motion("motion"),
    Puzzle("puzzle"),
    Keyboard("keyboard"),
    ;

    companion object {
        fun of(value: String?): GameShelf = entries.firstOrNull { it.key == value } ?: Puzzle
    }
}

/** Whether a higher or a lower score is the better one (points, or seconds and moves). */
enum class BestIs { High, Low }

/**
 * A game's `game.toml`: the same idea as rustmix-wave's `APP.TOM`. Only plain `key = value` lines
 * are read, where a value is a quoted string, a whole number or a list of quoted strings:
 *
 * ```
 * id = "tilt_racer"
 * name = "Tilt Racer"
 * entry = "main.lua"
 * version = "1.0"
 * shelf = "motion"
 * input = ["tilt", "keys"]
 * best = "high"
 * summary = "Steer by tilting the phone and keep clear of the traffic."
 * ```
 */
data class GameManifest(
    val id: String,
    val name: String,
    val entry: String,
    val version: String,
    val shelf: GameShelf,
    val usesTilt: Boolean,
    val bestIs: BestIs,
    val summary: String,
    val controls: String,
) {
    companion object {
        const val FILE = "game.toml"
        private const val MAX_NAME = 40
        private const val MAX_TEXT = 200
        private val ID = Regex("[a-z0-9_]{1,32}")
        private val ENTRY = Regex("[A-Za-z0-9_]{1,32}\\.lua")

        /** The manifest, or the reason it can't be used (shown in Add games). */
        fun parse(text: String): Result<GameManifest> =
            runCatching {
                val values = readToml(text)
                val id = values.string("id")
                require(ID.matches(id)) { "id must be 1-32 of a-z, 0-9 and _" }
                val name = values.string("name").trim()
                require(name.isNotEmpty() && name.length <= MAX_NAME) { "name must be 1-$MAX_NAME characters" }
                val entry = values.string("entry", "main.lua")
                require(ENTRY.matches(entry)) { "entry must be a .lua file in the game's folder" }
                val input = values.list("input")
                GameManifest(
                    id = id,
                    name = name,
                    entry = entry,
                    version = values.string("version", "1.0").take(MAX_NAME),
                    shelf = GameShelf.of(values.string("shelf", "")),
                    usesTilt = "tilt" in input || "imu" in input,
                    bestIs = if (values.string("best", "high") == "low") BestIs.Low else BestIs.High,
                    summary = values.string("summary", "").take(MAX_TEXT),
                    controls = values.string("controls", "").take(MAX_TEXT),
                )
            }

        private fun Map<String, Any>.string(
            key: String,
            fallback: String? = null,
        ): String =
            when (val v = this[key]) {
                is String -> v
                is Long -> v.toString()
                null -> fallback ?: throw IllegalArgumentException("$key is missing")
                else -> throw IllegalArgumentException("$key must be text")
            }

        @Suppress("UNCHECKED_CAST")
        private fun Map<String, Any>.list(key: String): List<String> =
            when (val v = this[key]) {
                is List<*> -> v as List<String>
                is String -> listOf(v)
                else -> emptyList()
            }

        internal fun readToml(text: String): Map<String, Any> {
            val out = linkedMapOf<String, Any>()
            text.lineSequence().forEachIndexed { index, raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) return@forEachIndexed
                val eq = line.indexOf('=')
                require(eq > 0) { "line ${index + 1}: expected key = value" }
                val key = line.substring(0, eq).trim()
                out[key] = value(line.substring(eq + 1).trim(), index + 1)
            }
            return out
        }

        private fun value(
            text: String,
            line: Int,
        ): Any =
            when {
                text.startsWith("[") -> {
                    val body = text.removePrefix("[").substringBefore("]")
                    Regex("\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(body).map { unescape(it.groupValues[1]) }.toList()
                }

                text.startsWith("\"") -> {
                    val m = Regex("^\"((?:[^\"\\\\]|\\\\.)*)\"").find(text)
                    require(m != null) { "line $line: unclosed text" }
                    unescape(m.groupValues[1])
                }

                else -> {
                    text.substringBefore('#').trim().toLongOrNull()
                        ?: throw IllegalArgumentException("line $line: expected text in quotes")
                }
            }

        private fun unescape(s: String) = s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\")
    }
}
