package org.sableos.games.console

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.ui.graphics.ImageBitmap
import java.io.File

/** A game in the library: one that comes with Sable (in the app's assets) or one the owner added. */
data class ConsoleGame(
    val manifest: GameManifest,
    val builtIn: Boolean,
    /** The asset folder for a built-in game, the folder in app storage for an added one. */
    val path: String,
)

/** A game folder found in the folder the owner chose, ready to add, or why it can't be. */
data class FoundGame(
    val folderName: String,
    val manifest: GameManifest?,
    val problem: String?,
    val manifestUri: Uri?,
    val scriptUri: Uri?,
    /** The game's pictures for `sable.image`, by file name. */
    val pictureUris: Map<String, Uri> = emptyMap(),
)

/** Best scores, recently played, settings and each game's own saved values (`sable.save`). */
class ConsoleProgress(
    context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("console", Context.MODE_PRIVATE)
    private val app = context.applicationContext

    fun best(id: String): Long? = if (prefs.contains("best_$id")) prefs.getLong("best_$id", 0) else null

    /** Keeps [score] when it beats the saved best; true when it did. */
    fun offer(
        game: GameManifest,
        score: Long,
    ): Boolean {
        val old = best(game.id)
        val better = old == null || if (game.bestIs == BestIs.High) score > old else score < old
        if (better) prefs.edit().putLong("best_${game.id}", score).apply()
        return better
    }

    fun lastPlayed(id: String): Long = prefs.getLong("played_$id", 0)

    fun markPlayed(id: String) {
        prefs.edit().putLong("played_$id", System.currentTimeMillis()).apply()
    }

    fun reset(id: String) {
        prefs
            .edit()
            .remove("best_$id")
            .remove("played_$id")
            .apply()
        app
            .getSharedPreferences(storeName(id), Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    var sensitivity: TiltSensitivity
        get() = TiltSensitivity.of(prefs.getString("sensitivity", null))
        set(value) = prefs.edit().putString("sensitivity", value.key).apply()

    var keysOnly: Boolean
        get() = prefs.getBoolean("keys_only", false)
        set(value) = prefs.edit().putBoolean("keys_only", value).apply()

    fun store(id: String): GameStore {
        val p = app.getSharedPreferences(storeName(id), Context.MODE_PRIVATE)
        return object : GameStore {
            override fun get(key: String): String? = p.getString(key, null)

            override fun put(
                key: String,
                value: String?,
            ) {
                if (value == null || p.all.size < MAX_KEYS || p.contains(key)) {
                    p.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
                }
            }
        }
    }

    private fun storeName(id: String) = "console_game_$id"

    private companion object {
        const val MAX_KEYS = 64
    }
}

/**
 * Finds the games: the ones in `assets/console/<id>/` and the ones added to `files/console/<id>/`.
 * Adding reads a folder the owner picked with the system's folder picker, so the app needs no storage
 * permission and sees only that folder; each game's `game.toml`, its one `.lua` file and its PNG
 * pictures ([GamePictures]) are copied in.
 */
class ConsoleLibrary(
    context: Context,
) {
    private val app = context.applicationContext
    private val installed = File(app.filesDir, "console")

    fun games(): List<ConsoleGame> {
        val builtIn =
            app.assets
                .list(ASSETS)
                .orEmpty()
                .sorted()
                .mapNotNull { dir ->
                    val text =
                        runCatching {
                            app.assets
                                .open(
                                    "$ASSETS/$dir/${GameManifest.FILE}",
                                ).use { it.reader().readText() }
                        }
                    text
                        .getOrNull()
                        ?.let {
                            GameManifest
                                .parse(
                                    it,
                                ).getOrNull()
                        }?.let { ConsoleGame(it, true, "$ASSETS/$dir") }
                }
        val ids = builtIn.map { it.manifest.id }.toSet()
        val added =
            installed
                .listFiles()
                .orEmpty()
                .filter { it.isDirectory }
                .sortedBy { it.name }
                .mapNotNull { dir ->
                    val file = File(dir, GameManifest.FILE)
                    val manifest = if (file.isFile) GameManifest.parse(file.readText()).getOrNull() else null
                    manifest?.takeIf { it.id !in ids }?.let { ConsoleGame(it, false, dir.path) }
                }
        return builtIn + added
    }

    fun script(game: ConsoleGame): String =
        if (game.builtIn) {
            app.assets.open("${game.path}/${game.manifest.entry}").use { it.reader().readText() }
        } else {
            File(game.path, game.manifest.entry).readText()
        }

    /** The game's pictures by file name; one that breaks the [GamePictures] limits is left out. */
    fun pictures(game: ConsoleGame): Map<String, ImageBitmap> {
        val names =
            if (game.builtIn) {
                app.assets.list(game.path).orEmpty().toList()
            } else {
                File(game.path).list().orEmpty().toList()
            }
        return names
            .filter { GamePictures.isPicture(it) }
            .sorted()
            .take(GamePictures.MAX)
            .mapNotNull { name ->
                val bytes =
                    runCatching {
                        if (game.builtIn) {
                            app.assets.open("${game.path}/$name").use { it.readBytes() }
                        } else {
                            File(game.path, name).readBytes()
                        }
                    }.getOrNull()
                bytes?.let { GamePictures.decode(name, it) }?.let { name to it }
            }.toMap()
    }

    fun remove(game: ConsoleGame) {
        if (!game.builtIn) File(game.path).deleteRecursively()
    }

    /** The games in [tree]: the picked folder itself, or the folders inside it. */
    fun scan(tree: Uri): List<FoundGame> {
        val root = DocumentsContract.getTreeDocumentId(tree)
        val top = children(tree, root)
        if (top.any { it.name == GameManifest.FILE }) return listOf(inspect(tree, "", top))
        return top
            .filter { it.isDir }
            .take(MAX_FOLDERS)
            .map { dir -> inspect(tree, dir.name, children(tree, dir.id)) }
            .filter { it.manifest != null || it.manifestUri != null }
    }

    /** Copies a found game into the library; null on success, else why not. */
    fun add(found: FoundGame): String? {
        val manifest = found.manifest ?: return found.problem
        val script = found.scriptUri ?: return found.problem
        val manifestText = read(found.manifestUri ?: return found.problem) ?: return "game.toml can't be read"
        val scriptText = read(script) ?: return "${manifest.entry} can't be read"
        val dir = File(installed, manifest.id)
        dir.deleteRecursively()
        dir.mkdirs()
        File(dir, GameManifest.FILE).writeText(manifestText)
        File(dir, manifest.entry).writeText(scriptText)
        for ((name, uri) in found.pictureUris) {
            val bytes = readBytes(uri, GamePictures.MAX_BYTES)
            val problem = GamePictures.problem(name, bytes)
            if (bytes == null || problem != null) {
                dir.deleteRecursively()
                return problem ?: "$name can't be read"
            }
            File(dir, name).writeBytes(bytes)
        }
        return null
    }

    private fun inspect(
        tree: Uri,
        folder: String,
        files: List<Doc>,
    ): FoundGame {
        val toml = files.firstOrNull { it.name == GameManifest.FILE }
        val tomlUri = toml?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it.id) }
        val text = tomlUri?.let { read(it) }
        val parsed = text?.let { GameManifest.parse(it) }
        val manifest = parsed?.getOrNull()
        val script = manifest?.let { m -> files.firstOrNull { it.name == m.entry } }
        val scriptUri = script?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it.id) }
        val builtInIds = games().filter { it.builtIn }.map { it.manifest.id }.toSet()
        val pictures =
            files
                .filter { !it.isDir && GamePictures.isPicture(it.name) }
                .associate { it.name to DocumentsContract.buildDocumentUriUsingTree(tree, it.id) }
        val problem =
            when {
                toml == null -> "no game.toml"
                text == null -> "game.toml is too big or can't be read"
                manifest == null -> parsed?.exceptionOrNull()?.message ?: "game.toml can't be read"
                manifest.id in builtInIds -> "it has the same id as a game that comes with Sable"
                script == null -> "${manifest.entry} is missing"
                pictures.size > GamePictures.MAX -> "it has more than ${GamePictures.MAX} pictures"
                else -> picturesProblem(pictures) ?: compileProblem(scriptUri, manifest, pictures.keys)
            }
        return FoundGame(manifest?.name ?: folder, manifest, problem, tomlUri, scriptUri, pictures)
    }

    private fun picturesProblem(pictures: Map<String, Uri>): String? =
        pictures.firstNotNullOfOrNull { (name, uri) -> GamePictures.problem(name, readBytes(uri, GamePictures.MAX_BYTES)) }

    private fun compileProblem(
        uri: Uri?,
        manifest: GameManifest,
        pictures: Set<String>,
    ): String? {
        val source = uri?.let { read(it) } ?: return "${manifest.entry} is too big or can't be read"
        return try {
            LuaGame(source, manifest.entry, MemoryGameStore(), pictures)
            null
        } catch (e: GameScriptError) {
            e.message
        }
    }

    private fun read(uri: Uri): String? = readBytes(uri, MAX_FILE_BYTES)?.toString(Charsets.UTF_8)

    /** The file's bytes, or null when it is longer than [limit] or can't be read. */
    private fun readBytes(
        uri: Uri,
        limit: Int,
    ): ByteArray? =
        runCatching {
            app.contentResolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(BUFFER_BYTES)
                var n = input.read(buffer)
                while (n >= 0 && out.size() <= limit) {
                    out.write(buffer, 0, n)
                    n = input.read(buffer)
                }
                if (out.size() > limit) null else out.toByteArray()
            }
        }.getOrNull()

    private data class Doc(
        val id: String,
        val name: String,
        val isDir: Boolean,
    )

    private fun children(
        tree: Uri,
        parent: String,
    ): List<Doc> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        val columns =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
        val out = mutableListOf<Doc>()
        runCatching {
            app.contentResolver.query(uri, columns, null, null, null)?.use { c ->
                while (c.moveToNext() && out.size < MAX_CHILDREN) {
                    out +=
                        Doc(
                            c.getString(0),
                            c.getString(1).orEmpty(),
                            c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                        )
                }
            }
        }
        return out
    }

    companion object {
        const val ASSETS = "console"
        const val MAX_FOLDERS = 50
        const val MAX_CHILDREN = 200
        const val MAX_FILE_BYTES = 256 * 1024
        const val BUFFER_BYTES = 8 * 1024
    }
}
