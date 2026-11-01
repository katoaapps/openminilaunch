package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.FavoriteContactActionRoutes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Converts the route map to authenticated ciphertext and treats unreadable state as no overrides.
 * Route preferences are optional, so corruption or key invalidation must never block the launcher.
 */
internal class EncryptedFavoriteContactRoutePersistence(
    private val storage: ContactDataBlobStorage,
    private val cipher: ContactDataCipher,
) {
    fun load(): Map<String, FavoriteContactActionRoutes> {
        val encrypted = try {
            storage.read()
        } catch (_: Exception) {
            return emptyMap()
        } ?: return emptyMap()

        var plaintext: ByteArray? = null
        return try {
            plaintext = cipher.decrypt(encrypted)
            FavoriteContactRouteCodec.decode(checkNotNull(plaintext))
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

    fun save(routes: Map<String, FavoriteContactActionRoutes>): Boolean {
        var plaintext: ByteArray? = null
        var encrypted: ByteArray? = null
        return try {
            plaintext = FavoriteContactRouteCodec.encode(routes)
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
internal object FavoriteContactRouteCodec {
    fun encode(routes: Map<String, FavoriteContactActionRoutes>): ByteArray {
        val persisted = routes
            .filterKeys(String::isNotBlank)
            .filterValues {
                it.message != ContactAppRoute.Inherit ||
                    it.call != ContactAppRoute.Inherit ||
                    it.preferredPhoneFingerprint != null
            }
            .toSortedMap()
        require(persisted.size <= MAX_ROUTE_ENTRIES) { "Too many contact action routes" }

        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(PAYLOAD_MAGIC)
                output.writeInt(PAYLOAD_VERSION)
                output.writeInt(persisted.size)
                persisted.forEach { (contactKey, routes) ->
                    output.writeBoundedString(contactKey, MAX_CONTACT_KEY_BYTES)
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

    fun decode(bytes: ByteArray): Map<String, FavoriteContactActionRoutes> =
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
                    val contactKey = input.readBoundedString(MAX_CONTACT_KEY_BYTES)
                    if (contactKey.isBlank() || containsKey(contactKey)) {
                        throw IOException("Invalid person route identity")
                    }
                    val routes = FavoriteContactActionRoutes(
                        message = input.readRoute(),
                        call = input.readRoute(),
                        preferredPhoneFingerprint = if (version >= 2) {
                            input.readNullableBoundedString(MAX_PHONE_FINGERPRINT_BYTES)
                        } else {
                            null
                        },
                    )
                    if (routes.message != ContactAppRoute.Inherit ||
                        routes.call != ContactAppRoute.Inherit ||
                        routes.preferredPhoneFingerprint != null
                    ) {
                        put(contactKey, routes)
                    }
                }
                if (input.read() != -1) throw IOException("Trailing route payload data")
            }
        }

    private fun DataOutputStream.writeRoute(route: ContactAppRoute) {
        when (route) {
            ContactAppRoute.Inherit -> writeByte(ROUTE_INHERIT)
            ContactAppRoute.SystemDefault -> writeByte(ROUTE_SYSTEM_DEFAULT)
            is ContactAppRoute.Package -> {
                require(route.packageName.isNotBlank()) { "Package route must not be blank" }
                writeByte(ROUTE_PACKAGE)
                writeBoundedString(route.packageName, MAX_PACKAGE_BYTES)
            }
        }
    }

    private fun DataInputStream.readRoute(): ContactAppRoute = when (readUnsignedByte()) {
        ROUTE_INHERIT -> ContactAppRoute.Inherit
        ROUTE_SYSTEM_DEFAULT -> ContactAppRoute.SystemDefault
        ROUTE_PACKAGE -> readBoundedString(MAX_PACKAGE_BYTES)
            .takeIf(String::isNotBlank)
            ?.let(ContactAppRoute::Package)
            ?: throw IOException("Blank route package")
        else -> throw IOException("Unknown contact action route")
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
    private const val MAX_CONTACT_KEY_BYTES = 4_096
    private const val MAX_PACKAGE_BYTES = 512
    private const val MAX_PHONE_FINGERPRINT_BYTES = 128
    private const val ROUTE_INHERIT = 0
    private const val ROUTE_SYSTEM_DEFAULT = 1
    private const val ROUTE_PACKAGE = 2
}
