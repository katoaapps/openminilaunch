package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.FavoriteContactActionRoutes
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.favoritePhoneFingerprint
import com.katoaapps.openminilaunch.model.resolvePackage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import javax.crypto.KeyGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteContactRouteTest {
    @Test fun callRoutesKeepInheritAndSystemDefaultDistinct() {
        assertEquals(
            "com.example.phone",
            ContactAppRoute.Inherit.resolvePackage("com.example.phone"),
        )
        assertNull(ContactAppRoute.SystemDefault.resolvePackage("com.example.phone"))
        assertEquals(
            "com.example.other",
            ContactAppRoute.Package("com.example.other").resolvePackage("com.example.phone"),
        )
    }

    @Test fun encryptedRoutesRestoreWithoutPlaintextIdentifiersOnDisk() {
        val contactKey = "android-contact:opaque-contact-key"
        val phoneNumber = "+15551234567"
        val routes = mapOf(
            contactKey to FavoriteContactActionRoutes(
                message = ContactAppRoute.Package("com.example.messages"),
                call = ContactAppRoute.SystemDefault,
                preferredPhoneFingerprint = favoritePhoneFingerprint(
                    FavoriteContactId.Android("opaque-contact-key"),
                    phoneNumber,
                ),
            ),
        )
        val plaintext = FavoriteContactRouteCodec.encode(routes)
        val cipher = testCipher()

        val encrypted = cipher.encrypt(plaintext)

        assertFalse(encrypted.containsBytes("opaque-contact-key".toByteArray()))
        assertFalse(encrypted.containsBytes("com.example.messages".toByteArray()))
        assertFalse(plaintext.containsBytes(phoneNumber.toByteArray()))
        assertFalse(encrypted.containsBytes(phoneNumber.toByteArray()))
        assertEquals(routes, FavoriteContactRouteCodec.decode(cipher.decrypt(encrypted)))
    }

    @Test fun legacyVersionOneRoutesRestoreWithoutAPreferredPhone() {
        val contactKey = "android-contact:legacy"
        val packageName = "com.example.messages"
        val payload = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(0x4D_52_54_50)
                output.writeInt(1)
                output.writeInt(1)
                output.writeTestString(contactKey)
                output.writeByte(2)
                output.writeTestString(packageName)
                output.writeByte(0)
            }
            bytes.toByteArray()
        }

        assertEquals(
            mapOf(
                contactKey to FavoriteContactActionRoutes(
                    message = ContactAppRoute.Package(packageName),
                ),
            ),
            FavoriteContactRouteCodec.decode(payload),
        )
    }

    @Test fun identicalRouteSnapshotsProduceDifferentCiphertext() {
        val plaintext = FavoriteContactRouteCodec.encode(
            mapOf(
                "android-contact:key" to FavoriteContactActionRoutes(ContactAppRoute.SystemDefault),
            ),
        )
        val cipher = testCipher()

        val first = cipher.encrypt(plaintext)
        val second = cipher.encrypt(plaintext)

        assertFalse(first.contentEquals(second))
        assertArrayEquals(plaintext, cipher.decrypt(first))
        assertArrayEquals(plaintext, cipher.decrypt(second))
    }

    @Test fun tamperedCiphertextCannotBeRestored() {
        val cipher = testCipher()
        val encrypted = cipher.encrypt(FavoriteContactRouteCodec.encode(emptyMap()))
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()

        assertThrows(Exception::class.java) { cipher.decrypt(encrypted) }
    }

    @Test fun persistenceClearsTamperedDataAndFallsBackToInheritedRoutes() {
        val storage = MemoryRouteStorage()
        val persistence = EncryptedFavoriteContactRoutePersistence(storage, testCipher())
        val routes = mapOf(
            "android-contact:key" to FavoriteContactActionRoutes(
                message = ContactAppRoute.Package("org.example.messages"),
            ),
        )
        assertTrue(persistence.save(routes))
        storage.bytes!![storage.bytes!!.lastIndex] =
            (storage.bytes!!.last().toInt() xor 1).toByte()

        assertEquals(emptyMap<String, FavoriteContactActionRoutes>(), persistence.load())
        assertNull(storage.bytes)
    }

    @Test fun malformedPayloadIsRejected() {
        assertThrows(IOException::class.java) {
            FavoriteContactRouteCodec.decode(byteArrayOf(0, 1, 2, 3))
        }
    }

    @Test fun invalidatedKeyIsRotatedAndTheNextSaveCanRecover() {
        val storage = MemoryRouteStorage().apply { bytes = byteArrayOf(1, 2, 3) }
        val cipher = ResetTrackingCipher()
        val persistence = EncryptedFavoriteContactRoutePersistence(storage, cipher)

        assertEquals(emptyMap<String, FavoriteContactActionRoutes>(), persistence.load())
        assertEquals(1, cipher.resetCount)
        assertTrue(
            persistence.save(
                mapOf(
                    "android-contact:key" to FavoriteContactActionRoutes(
                        message = ContactAppRoute.SystemDefault,
                    ),
                ),
            ),
        )
    }

    private fun testCipher(): AesGcmContactDataCipher {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        val key = generator.generateKey()
        return AesGcmContactDataCipher(
            keyProvider = { key },
            envelopeMagic = 0x4D_52_54_45,
            authenticatedContext =
                "openmink:favorite-person-action-routes:v1".toByteArray(Charsets.UTF_8),
        )
    }
}

private class MemoryRouteStorage : ContactDataBlobStorage {
    var bytes: ByteArray? = null

    override fun read(): ByteArray? = bytes?.copyOf()

    override fun write(bytes: ByteArray) {
        this.bytes = bytes.copyOf()
    }

    override fun clear() {
        bytes = null
    }
}

private class ResetTrackingCipher : ContactDataCipher {
    var resetCount = 0
    private var usable = false

    override fun encrypt(plaintext: ByteArray): ByteArray {
        if (!usable) throw IllegalStateException("Key is invalid")
        return plaintext.copyOf()
    }

    override fun decrypt(envelope: ByteArray): ByteArray {
        if (!usable) throw IllegalStateException("Key is invalid")
        return envelope.copyOf()
    }

    override fun resetKey() {
        resetCount++
        usable = true
    }
}

private fun ByteArray.containsBytes(candidate: ByteArray): Boolean {
    if (candidate.isEmpty() || candidate.size > size) return false
    return indices.any { start ->
        start + candidate.size <= size && candidate.indices.all { offset ->
            this[start + offset] == candidate[offset]
        }
    }
}

private fun DataOutputStream.writeTestString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeInt(bytes.size)
    write(bytes)
}
