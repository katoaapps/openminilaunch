package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.PersonActionRoutes
import com.katoaapps.openminilaunch.model.PersonAppRoute
import com.katoaapps.openminilaunch.model.favoritePhoneFingerprint
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

class PersonActionRouteTest {
    @Test fun encryptedRoutesRestoreWithoutPlaintextIdentifiersOnDisk() {
        val personKey = "android-contact:opaque-contact-key"
        val phoneNumber = "+15551234567"
        val routes = mapOf(
            personKey to PersonActionRoutes(
                message = PersonAppRoute.Package("com.example.messages"),
                call = PersonAppRoute.SystemDefault,
                preferredPhoneFingerprint = favoritePhoneFingerprint(personKey, phoneNumber),
            ),
        )
        val plaintext = PersonActionRouteCodec.encode(routes)
        val cipher = testCipher()

        val encrypted = cipher.encrypt(plaintext)

        assertFalse(encrypted.containsBytes("opaque-contact-key".toByteArray()))
        assertFalse(encrypted.containsBytes("com.example.messages".toByteArray()))
        assertFalse(plaintext.containsBytes(phoneNumber.toByteArray()))
        assertFalse(encrypted.containsBytes(phoneNumber.toByteArray()))
        assertEquals(routes, PersonActionRouteCodec.decode(cipher.decrypt(encrypted)))
    }

    @Test fun legacyVersionOneRoutesRestoreWithoutAPreferredPhone() {
        val personKey = "android-contact:legacy"
        val packageName = "com.example.messages"
        val payload = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(0x4D_52_54_50)
                output.writeInt(1)
                output.writeInt(1)
                output.writeTestString(personKey)
                output.writeByte(2)
                output.writeTestString(packageName)
                output.writeByte(0)
            }
            bytes.toByteArray()
        }

        assertEquals(
            mapOf(
                personKey to PersonActionRoutes(
                    message = PersonAppRoute.Package(packageName),
                ),
            ),
            PersonActionRouteCodec.decode(payload),
        )
    }

    @Test fun identicalRouteSnapshotsProduceDifferentCiphertext() {
        val plaintext = PersonActionRouteCodec.encode(
            mapOf("android-contact:key" to PersonActionRoutes(PersonAppRoute.SystemDefault)),
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
        val encrypted = cipher.encrypt(PersonActionRouteCodec.encode(emptyMap()))
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()

        assertThrows(Exception::class.java) { cipher.decrypt(encrypted) }
    }

    @Test fun persistenceClearsTamperedDataAndFallsBackToInheritedRoutes() {
        val storage = MemoryRouteStorage()
        val persistence = EncryptedPersonActionRoutePersistence(storage, testCipher())
        val routes = mapOf(
            "android-contact:key" to PersonActionRoutes(
                message = PersonAppRoute.Package("org.example.messages"),
            ),
        )
        assertTrue(persistence.save(routes))
        storage.bytes!![storage.bytes!!.lastIndex] =
            (storage.bytes!!.last().toInt() xor 1).toByte()

        assertEquals(emptyMap<String, PersonActionRoutes>(), persistence.load())
        assertNull(storage.bytes)
    }

    @Test fun malformedPayloadIsRejected() {
        assertThrows(IOException::class.java) {
            PersonActionRouteCodec.decode(byteArrayOf(0, 1, 2, 3))
        }
    }

    @Test fun invalidatedKeyIsRotatedAndTheNextSaveCanRecover() {
        val storage = MemoryRouteStorage().apply { bytes = byteArrayOf(1, 2, 3) }
        val cipher = ResetTrackingCipher()
        val persistence = EncryptedPersonActionRoutePersistence(storage, cipher)

        assertEquals(emptyMap<String, PersonActionRoutes>(), persistence.load())
        assertEquals(1, cipher.resetCount)
        assertTrue(
            persistence.save(
                mapOf(
                    "android-contact:key" to PersonActionRoutes(
                        message = PersonAppRoute.SystemDefault,
                    ),
                ),
            ),
        )
    }

    private fun testCipher(): AesGcmPersonActionRouteCipher {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        val key = generator.generateKey()
        return AesGcmPersonActionRouteCipher { key }
    }
}

private class MemoryRouteStorage : PersonActionRouteBlobStorage {
    var bytes: ByteArray? = null

    override fun read(): ByteArray? = bytes?.copyOf()

    override fun write(bytes: ByteArray) {
        this.bytes = bytes.copyOf()
    }

    override fun clear() {
        bytes = null
    }
}

private class ResetTrackingCipher : PersonActionRouteCipher {
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
