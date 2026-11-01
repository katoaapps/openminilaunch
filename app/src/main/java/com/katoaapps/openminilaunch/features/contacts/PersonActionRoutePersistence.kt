package com.katoaapps.openminilaunch.features.contacts

import android.util.AtomicFile
import com.katoaapps.openminilaunch.model.PersonActionRoutes
import com.katoaapps.openminilaunch.model.PersonAppRoute
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

internal interface PersonActionRouteBlobStorage {
    fun read(): ByteArray?
    fun write(bytes: ByteArray)
    fun clear()
}

/** Atomic encrypted-blob storage under the caller-supplied no-backup path. */
internal class AtomicPersonActionRouteStorage(file: File) : PersonActionRouteBlobStorage {
    private val atomicFile = AtomicFile(file)

    override fun read(): ByteArray? {
        if (!atomicFile.baseFile.exists()) return null
        return atomicFile.openRead().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > MAX_ENCRYPTED_FILE_BYTES) throw IOException("Route store is too large")
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    }

    override fun write(bytes: ByteArray) {
        if (bytes.size > MAX_ENCRYPTED_FILE_BYTES) throw IOException("Route store is too large")
        atomicFile.baseFile.parentFile?.mkdirs()
        val output = atomicFile.startWrite()
        try {
            output.write(bytes)
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (failure: Exception) {
            atomicFile.failWrite(output)
            throw failure
        }
    }

    override fun clear() {
        atomicFile.delete()
    }

    private companion object {
        const val MAX_ENCRYPTED_FILE_BYTES = 2 * 1024 * 1024
    }
}

/**
 * Converts the route map to authenticated ciphertext and treats unreadable state as no overrides.
 * Route preferences are optional, so corruption or key invalidation must never block the launcher.
 */
internal class EncryptedPersonActionRoutePersistence(
    private val storage: PersonActionRouteBlobStorage,
    private val cipher: PersonActionRouteCipher,
) {
    fun load(): Map<String, PersonActionRoutes> {
        val encrypted = try {
            storage.read()
        } catch (_: Exception) {
            return emptyMap()
        } ?: return emptyMap()

        var plaintext: ByteArray? = null
        return try {
            plaintext = cipher.decrypt(encrypted)
            PersonActionRouteCodec.decode(checkNotNull(plaintext))
        } catch (_: Exception) {
            // Never log ciphertext, contact identifiers, packages, or cryptographic failures here.
            runCatching(cipher::resetKey)
            runCatching(storage::clear)
            emptyMap()
        } finally {
            encrypted.fill(0)
            plaintext?.fill(0)
        }
    }

    fun save(routes: Map<String, PersonActionRoutes>): Boolean {
        var plaintext: ByteArray? = null
        var encrypted: ByteArray? = null
        return try {
            plaintext = PersonActionRouteCodec.encode(routes)
            encrypted = encryptWithFreshKeyRecovery(checkNotNull(plaintext))
            storage.write(checkNotNull(encrypted))
            true
        } catch (_: Exception) {
            false
        } finally {
            plaintext?.fill(0)
            encrypted?.fill(0)
        }
    }

    private fun encryptWithFreshKeyRecovery(plaintext: ByteArray): ByteArray = try {
        cipher.encrypt(plaintext)
    } catch (_: Exception) {
        // A permanently invalidated app key cannot repair itself. Rotate this dedicated key and
        // retry once; no other feature or user data shares the alias.
        cipher.resetKey()
        cipher.encrypt(plaintext)
    }
}

/** Small deterministic binary format kept entirely inside the authenticated encrypted envelope. */
internal object PersonActionRouteCodec {
    fun encode(routes: Map<String, PersonActionRoutes>): ByteArray {
        val persisted = routes
            .filterKeys(String::isNotBlank)
            .filterValues {
                it.message != PersonAppRoute.Inherit ||
                    it.call != PersonAppRoute.Inherit ||
                    it.preferredPhoneFingerprint != null
            }
            .toSortedMap()
        require(persisted.size <= MAX_ROUTE_ENTRIES) { "Too many person action routes" }

        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(PAYLOAD_MAGIC)
                output.writeInt(PAYLOAD_VERSION)
                output.writeInt(persisted.size)
                persisted.forEach { (personKey, routes) ->
                    output.writeBoundedString(personKey, MAX_PERSON_KEY_BYTES)
                    output.writeRoute(routes.message)
                    output.writeRoute(routes.call)
                    output.writeNullableBoundedString(
                        routes.preferredPhoneFingerprint,
                        MAX_PHONE_FINGERPRINT_BYTES,
                    )
                }
            }
            bytes.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): Map<String, PersonActionRoutes> =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != PAYLOAD_MAGIC) throw IOException("Invalid route payload")
            val version = input.readInt()
            if (version !in LEGACY_PAYLOAD_VERSION..PAYLOAD_VERSION) {
                throw IOException("Unsupported route payload")
            }
            val count = input.readInt()
            if (count !in 0..MAX_ROUTE_ENTRIES) throw IOException("Invalid route count")
            buildMap(count) {
                repeat(count) {
                    val personKey = input.readBoundedString(MAX_PERSON_KEY_BYTES)
                    if (personKey.isBlank() || containsKey(personKey)) {
                        throw IOException("Invalid person route identity")
                    }
                    val routes = PersonActionRoutes(
                        message = input.readRoute(),
                        call = input.readRoute(),
                        preferredPhoneFingerprint = if (version >= 2) {
                            input.readNullableBoundedString(MAX_PHONE_FINGERPRINT_BYTES)
                        } else {
                            null
                        },
                    )
                    if (routes.message != PersonAppRoute.Inherit ||
                        routes.call != PersonAppRoute.Inherit ||
                        routes.preferredPhoneFingerprint != null
                    ) {
                        put(personKey, routes)
                    }
                }
                if (input.read() != -1) throw IOException("Trailing route payload data")
            }
        }

    private fun DataOutputStream.writeRoute(route: PersonAppRoute) {
        when (route) {
            PersonAppRoute.Inherit -> writeByte(ROUTE_INHERIT)
            PersonAppRoute.SystemDefault -> writeByte(ROUTE_SYSTEM_DEFAULT)
            is PersonAppRoute.Package -> {
                require(route.packageName.isNotBlank()) { "Package route must not be blank" }
                writeByte(ROUTE_PACKAGE)
                writeBoundedString(route.packageName, MAX_PACKAGE_BYTES)
            }
        }
    }

    private fun DataInputStream.readRoute(): PersonAppRoute = when (readUnsignedByte()) {
        ROUTE_INHERIT -> PersonAppRoute.Inherit
        ROUTE_SYSTEM_DEFAULT -> PersonAppRoute.SystemDefault
        ROUTE_PACKAGE -> readBoundedString(MAX_PACKAGE_BYTES)
            .takeIf(String::isNotBlank)
            ?.let(PersonAppRoute::Package)
            ?: throw IOException("Blank route package")
        else -> throw IOException("Unknown person action route")
    }

    private fun DataOutputStream.writeBoundedString(value: String, maximumBytes: Int) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        try {
            require(encoded.size in 1..maximumBytes) { "Route string is outside allowed size" }
            writeInt(encoded.size)
            write(encoded)
        } finally {
            encoded.fill(0)
        }
    }

    private fun DataOutputStream.writeNullableBoundedString(value: String?, maximumBytes: Int) {
        writeBoolean(value != null)
        value?.let { writeBoundedString(it, maximumBytes) }
    }

    private fun DataInputStream.readBoundedString(maximumBytes: Int): String {
        val size = readInt()
        if (size !in 1..maximumBytes) throw IOException("Invalid route string size")
        val encoded = ByteArray(size)
        return try {
            readFully(encoded)
            encoded.toString(Charsets.UTF_8)
        } finally {
            encoded.fill(0)
        }
    }

    private fun DataInputStream.readNullableBoundedString(maximumBytes: Int): String? =
        if (readBoolean()) readBoundedString(maximumBytes) else null

    private const val PAYLOAD_MAGIC = 0x4D_52_54_50 // MRTP
    private const val LEGACY_PAYLOAD_VERSION = 1
    private const val PAYLOAD_VERSION = 2
    private const val MAX_ROUTE_ENTRIES = 2_048
    private const val MAX_PERSON_KEY_BYTES = 4_096
    private const val MAX_PACKAGE_BYTES = 512
    private const val MAX_PHONE_FINGERPRINT_BYTES = 128
    private const val ROUTE_INHERIT = 0
    private const val ROUTE_SYSTEM_DEFAULT = 1
    private const val ROUTE_PACKAGE = 2
}
