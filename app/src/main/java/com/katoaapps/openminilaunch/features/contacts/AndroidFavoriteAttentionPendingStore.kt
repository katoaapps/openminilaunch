package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

/** Encrypted device-local holding area for transfers waiting on Android Contacts sync. */
internal class AndroidFavoriteAttentionPendingStore(
    storage: ContactDataBlobStorage,
    cipher: ContactDataCipher,
) {
    private val persistence = EncryptedContactDataPersistence(
        storage = storage,
        cipher = cipher,
        emptyValue = { emptyList() },
        encode = AndroidFavoriteAttentionPendingCodec::encode,
        decode = AndroidFavoriteAttentionPendingCodec::decode,
    )

    constructor(context: Context) : this(
        storage = AtomicContactDataBlobStorage(
            File(context.applicationContext.noBackupFilesDir, ENCRYPTED_FILE_NAME),
        ),
        cipher = AesGcmContactDataCipher(
            keyProvider = AndroidKeystoreContactDataKeyProvider(KEY_ALIAS),
            envelopeMagic = 0x4D_46_41_50,
            authenticatedContext =
                "openmink:pending-android-favorite-attention:v1".toByteArray(Charsets.UTF_8),
        ),
    )

    fun load(): List<AndroidFavoriteAttentionTransfer> = persistence.load()

    fun add(transfer: AndroidFavoriteAttentionTransfer): Boolean {
        if (!AndroidFavoriteAttentionTransfers.valid(transfer)) return false
        val updated = (load().filterNot { it.scopeKey == transfer.scopeKey } + transfer)
            .takeLast(MAX_PENDING_TRANSFERS)
        return persistence.save(updated)
    }

    fun replace(transfers: List<AndroidFavoriteAttentionTransfer>): Boolean =
        persistence.save(
            transfers.filter(AndroidFavoriteAttentionTransfers::valid)
                .takeLast(MAX_PENDING_TRANSFERS),
        )

    private companion object {
        const val ENCRYPTED_FILE_NAME = "pending_android_favorite_attention_v1.enc"
        const val KEY_ALIAS = "mink.pending-android-favorite-attention.aes-gcm.v1"
    }
}

internal object AndroidFavoriteAttentionPendingCodec {
    fun encode(transfers: List<AndroidFavoriteAttentionTransfer>): ByteArray {
        require(transfers.size <= MAX_PENDING_TRANSFERS) { "Too many pending color transfers" }
        require(transfers.all(AndroidFavoriteAttentionTransfers::valid)) {
            "Invalid pending color transfer"
        }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(PAYLOAD_MAGIC)
                output.writeInt(PAYLOAD_VERSION)
                output.writeInt(transfers.size)
                transfers.forEach { transfer ->
                    output.writeUTF(transfer.scopeKey)
                    output.writeInt(transfer.entries.size)
                    transfer.entries.forEach { entry ->
                        output.writeInt(entry.colorArgb)
                        output.writeInt(entry.tokens.size)
                        entry.tokens.forEach(output::writeUTF)
                    }
                }
            }
            bytes.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): List<AndroidFavoriteAttentionTransfer> =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != PAYLOAD_MAGIC) throw IOException("Invalid pending color payload")
            if (input.readInt() != PAYLOAD_VERSION) throw IOException("Unsupported pending color payload")
            val transferCount = input.readInt()
            if (transferCount !in 0..MAX_PENDING_TRANSFERS) throw IOException("Invalid transfer count")
            buildList(transferCount) {
                repeat(transferCount) {
                    val scopeKey = input.readUTF()
                    val entryCount = input.readInt()
                    if (entryCount !in 1..AndroidFavoriteAttentionTransfers.MAX_TRANSFER_ENTRIES) {
                        throw IOException("Invalid transfer entry count")
                    }
                    val entries = buildList(entryCount) {
                        repeat(entryCount) {
                            val color = input.readInt()
                            val tokenCount = input.readInt()
                            if (tokenCount !in 1..AndroidFavoriteAttentionTransfers.MAX_TOKENS_PER_ENTRY) {
                                throw IOException("Invalid transfer token count")
                            }
                            add(
                                AndroidFavoriteAttentionTransferEntry(
                                    tokens = List(tokenCount) { input.readUTF() },
                                    colorArgb = color,
                                ),
                            )
                        }
                    }
                    val transfer = AndroidFavoriteAttentionTransfer(scopeKey, entries)
                    if (!AndroidFavoriteAttentionTransfers.valid(transfer)) {
                        throw IOException("Invalid pending transfer")
                    }
                    add(transfer)
                }
                if (input.read() != -1) throw IOException("Trailing pending color data")
            }
        }

    private const val PAYLOAD_MAGIC = 0x4D_46_50_41
    private const val PAYLOAD_VERSION = 1
}

private const val MAX_PENDING_TRANSFERS = 4
