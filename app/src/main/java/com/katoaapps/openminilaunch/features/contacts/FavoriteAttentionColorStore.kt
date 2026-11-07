package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.model.FavoriteContactId
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

/** Encrypted, device-local colors used by the Conversations software attention light. */
internal class FavoriteAttentionColorStore private constructor(
    private val persistence: EncryptedFavoriteAttentionColorPersistence,
) {
    private var colors by mutableStateOf(persistence.load())

    constructor(context: Context) : this(createPersistence(context.applicationContext))

    fun color(contactId: FavoriteContactId): Int? = colors[contactId.storageKey]

    @Synchronized
    fun update(contactId: FavoriteContactId, argb: Int?) {
        val key = contactId.storageKey
        if (key.isBlank()) return
        val updated = colors.toMutableMap().apply {
            if (argb == null) remove(key) else put(key, argb or OPAQUE_ALPHA)
        }
        if (updated == colors) return
        if (persistence.save(updated)) colors = updated
    }

    private companion object {
        const val OPAQUE_ALPHA = 0xFF000000.toInt()
        const val ENCRYPTED_FILE_NAME = "favorite_attention_colors_v1.enc"
        const val KEY_ALIAS = "mink.favorite-attention-colors.aes-gcm.v1"

        fun createPersistence(context: Context) = EncryptedFavoriteAttentionColorPersistence(
            storage = AtomicContactDataBlobStorage(
                File(context.noBackupFilesDir, ENCRYPTED_FILE_NAME),
            ),
            cipher = AesGcmContactDataCipher(
                keyProvider = AndroidKeystoreContactDataKeyProvider(KEY_ALIAS),
                envelopeMagic = 0x4D_46_41_43,
                authenticatedContext =
                    "openmink:favorite-attention-colors:v1".toByteArray(Charsets.UTF_8),
            ),
        )
    }
}

internal class EncryptedFavoriteAttentionColorPersistence(
    storage: ContactDataBlobStorage,
    cipher: ContactDataCipher,
) {
    private val persistence = EncryptedContactDataPersistence(
        storage = storage,
        cipher = cipher,
        emptyValue = { emptyMap() },
        encode = FavoriteAttentionColorCodec::encode,
        decode = FavoriteAttentionColorCodec::decode,
    )

    fun load(): Map<String, Int> = persistence.load()

    fun save(colors: Map<String, Int>): Boolean = persistence.save(colors)
}

/** Small deterministic payload kept entirely inside the authenticated encrypted envelope. */
internal object FavoriteAttentionColorCodec {
    fun encode(colors: Map<String, Int>): ByteArray {
        val persisted = colors.filterKeys(String::isNotBlank).toSortedMap()
        require(persisted.size <= MAX_ENTRIES) { "Too many favorite attention colors" }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(PAYLOAD_MAGIC)
                output.writeInt(PAYLOAD_VERSION)
                output.writeInt(persisted.size)
                persisted.forEach { (key, color) ->
                    val encodedKey = key.toByteArray(Charsets.UTF_8)
                    try {
                        require(encodedKey.size in 1..MAX_KEY_BYTES) {
                            "Favorite identity is outside allowed size"
                        }
                        output.writeInt(encodedKey.size)
                        output.write(encodedKey)
                        output.writeInt(color or OPAQUE_ALPHA)
                    } finally {
                        encodedKey.fill(0)
                    }
                }
            }
            bytes.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): Map<String, Int> =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != PAYLOAD_MAGIC) throw IOException("Invalid color payload")
            if (input.readInt() != PAYLOAD_VERSION) throw IOException("Unsupported color payload")
            val count = input.readInt()
            if (count !in 0..MAX_ENTRIES) throw IOException("Invalid color count")
            buildMap(count) {
                repeat(count) {
                    val size = input.readInt()
                    if (size !in 1..MAX_KEY_BYTES) throw IOException("Invalid favorite identity")
                    val encodedKey = ByteArray(size)
                    val key = try {
                        input.readFully(encodedKey)
                        encodedKey.toString(Charsets.UTF_8)
                    } finally {
                        encodedKey.fill(0)
                    }
                    if (key.isBlank() || containsKey(key)) {
                        throw IOException("Duplicate favorite identity")
                    }
                    put(key, input.readInt() or OPAQUE_ALPHA)
                }
                if (input.read() != -1) throw IOException("Trailing color payload data")
            }
        }

    private const val OPAQUE_ALPHA = 0xFF000000.toInt()
    private const val PAYLOAD_MAGIC = 0x4D_46_43_50
    private const val PAYLOAD_VERSION = 1
    private const val MAX_ENTRIES = 2_048
    private const val MAX_KEY_BYTES = 4_096
}
