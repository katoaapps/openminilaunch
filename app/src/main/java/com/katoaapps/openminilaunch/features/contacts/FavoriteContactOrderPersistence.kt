package com.katoaapps.openminilaunch.features.contacts

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/** Keeps sensitive contact lookup identities inside the authenticated encrypted envelope. */
internal class EncryptedFavoriteContactOrderPersistence(
    storage: ContactDataBlobStorage,
    cipher: ContactDataCipher,
) {
    private val persistence = EncryptedContactDataPersistence(
        storage = storage,
        cipher = cipher,
        emptyValue = { emptyList() },
        encode = FavoriteContactOrderCodec::encode,
        decode = FavoriteContactOrderCodec::decode,
    )

    fun load(): List<String> = persistence.load()

    fun save(order: List<String>): Boolean = persistence.save(order)
}

internal object FavoriteContactOrderCodec {
    fun encode(order: List<String>): ByteArray {
        val clean = order.filter(String::isNotBlank).distinct()
        require(clean.size == order.size && clean.size <= MAX_ENTRIES) {
            "Invalid favorite contact order"
        }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(PAYLOAD_MAGIC)
                output.writeInt(PAYLOAD_VERSION)
                output.writeInt(clean.size)
                clean.forEach { output.writeBoundedString(it) }
            }
            bytes.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): List<String> =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != PAYLOAD_MAGIC || input.readInt() != PAYLOAD_VERSION) {
                throw IOException("Unsupported favorite contact order payload")
            }
            val count = input.readInt()
            if (count !in 0..MAX_ENTRIES) throw IOException("Invalid favorite contact order count")
            val seen = mutableSetOf<String>()
            buildList(count) {
                repeat(count) {
                    val key = input.readBoundedString()
                    if (!seen.add(key)) throw IOException("Duplicate favorite contact order key")
                    add(key)
                }
                if (input.read() != -1) throw IOException("Trailing favorite contact order data")
            }
        }

    private fun DataOutputStream.writeBoundedString(value: String) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        try {
            require(encoded.size in 1..MAX_KEY_BYTES) { "Favorite contact key is out of bounds" }
            writeInt(encoded.size)
            write(encoded)
        } finally {
            encoded.fill(0)
        }
    }

    private fun DataInputStream.readBoundedString(): String {
        val size = readInt()
        if (size !in 1..MAX_KEY_BYTES) throw IOException("Invalid favorite contact key size")
        val encoded = ByteArray(size)
        return try {
            readFully(encoded)
            encoded.toString(Charsets.UTF_8).takeIf(String::isNotBlank)
                ?: throw IOException("Blank favorite contact key")
        } finally {
            encoded.fill(0)
        }
    }

    private const val PAYLOAD_MAGIC = 0x4D_46_43_4F // MFCO
    private const val PAYLOAD_VERSION = 1
    private const val MAX_ENTRIES = 2_048
    private const val MAX_KEY_BYTES = 4_096
}
