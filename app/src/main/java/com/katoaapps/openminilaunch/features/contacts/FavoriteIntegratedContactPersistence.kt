package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContactNameSource
import com.katoaapps.openminilaunch.model.IntegratedContactCallEndpoint
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.ProviderAddressType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

internal class EncryptedFavoriteIntegratedContactPersistence(
    storage: ContactDataBlobStorage,
    cipher: ContactDataCipher,
) {
    private val persistence = EncryptedContactDataPersistence(
        storage = storage,
        cipher = cipher,
        emptyValue = { emptyList() },
        encode = FavoriteIntegratedContactCodec::encode,
        decode = FavoriteIntegratedContactCodec::decode,
    )

    fun load(): List<FavoriteIntegratedContact> = persistence.load()

    fun save(contacts: List<FavoriteIntegratedContact>): Boolean = persistence.save(contacts)
}

internal object FavoriteIntegratedContactCodec {
    fun encode(contacts: List<FavoriteIntegratedContact>): ByteArray {
        require(contacts.size <= MAX_CONTACTS) { "Too many integrated contacts" }
        require(contacts.map { it.id.localId }.distinct().size == contacts.size) {
            "Duplicate integrated contact identity"
        }
        val endpointIdentities = contacts.flatMap(FavoriteIntegratedContact::endpoints)
            .map(IntegratedContactEndpoint::identity)
        require(endpointIdentities.distinct().size == endpointIdentities.size) {
            "Duplicate integrated endpoint identity"
        }
        val callEndpointIdentities = contacts.mapNotNull(FavoriteIntegratedContact::callEndpoint)
            .map(IntegratedContactCallEndpoint::identity)
        require(callEndpointIdentities.distinct().size == callEndpointIdentities.size) {
            "Duplicate integrated call endpoint identity"
        }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(PAYLOAD_MAGIC)
                output.writeInt(PAYLOAD_VERSION)
                output.writeInt(contacts.size)
                contacts.sortedBy { it.id.localId }.forEach { contact ->
                    require(contact.endpoints.isNotEmpty() || contact.callEndpoint != null) {
                        "Integrated contact needs an endpoint"
                    }
                    require(contact.endpoints.size <= MAX_ENDPOINTS_PER_CONTACT) {
                        "Too many integrated contact endpoints"
                    }
                    output.writeBoundedString(contact.id.localId, MAX_LOCAL_ID_BYTES)
                    output.writeBoundedString(contact.displayName, MAX_DISPLAY_NAME_BYTES)
                    output.writeInt(contact.nameSource.ordinal)
                    output.writeInt(contact.endpoints.size)
                    contact.endpoints.forEach { endpoint ->
                        when (endpoint) {
                            is IntegratedContactEndpoint.ConversationShortcut -> {
                                output.writeInt(ENDPOINT_SHORTCUT)
                                output.writeBoundedString(endpoint.packageName, MAX_PACKAGE_BYTES)
                                output.writeBoundedString(endpoint.shortcutId, MAX_SHORTCUT_ID_BYTES)
                                output.writeLong(endpoint.userSerial)
                                output.writeNullableBoundedString(endpoint.label, MAX_LABEL_BYTES)
                            }
                            is IntegratedContactEndpoint.ProviderAddress -> {
                                output.writeInt(ENDPOINT_PROVIDER_ADDRESS)
                                output.writeBoundedString(endpoint.providerId, MAX_PROVIDER_ID_BYTES)
                                output.writeBoundedString(endpoint.packageName, MAX_PACKAGE_BYTES)
                                output.writeInt(endpoint.addressType.ordinal)
                                output.writeBoundedString(endpoint.address, MAX_ADDRESS_BYTES)
                            }
                        }
                    }
                    output.writeBoolean(contact.callEndpoint != null)
                    when (val call = contact.callEndpoint) {
                        is IntegratedContactCallEndpoint.PhoneNumber -> {
                            output.writeInt(CALL_ENDPOINT_PHONE)
                            output.writeBoundedString(call.number, MAX_ADDRESS_BYTES)
                            output.writeNullableBoundedString(call.packageName, MAX_PACKAGE_BYTES)
                        }
                        null -> Unit
                    }
                }
            }
            bytes.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): List<FavoriteIntegratedContact> =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != PAYLOAD_MAGIC) throw IOException("Invalid integrated contact payload")
            val version = input.readInt()
            if (version !in MIN_SUPPORTED_PAYLOAD_VERSION..PAYLOAD_VERSION) {
                throw IOException("Unsupported integrated contact payload")
            }
            val count = input.readInt()
            if (count !in 0..MAX_CONTACTS) throw IOException("Invalid integrated contact count")
            val seenIds = mutableSetOf<String>()
            val seenEndpoints = mutableSetOf<String>()
            val seenCallEndpoints = mutableSetOf<String>()
            buildList(count) {
                repeat(count) {
                    val localId = input.readBoundedString(MAX_LOCAL_ID_BYTES)
                    val displayName = input.readBoundedString(MAX_DISPLAY_NAME_BYTES)
                    val savedNameSource = if (version >= 4) {
                        FavoriteIntegratedContactNameSource.entries.getOrNull(input.readInt())
                            ?: throw IOException("Invalid integrated contact name source")
                    } else {
                        null
                    }
                    if (localId.isBlank() || displayName.isBlank() || !seenIds.add(localId)) {
                        throw IOException("Invalid integrated contact identity")
                    }
                    val endpointCount = input.readInt()
                    if (endpointCount !in 0..MAX_ENDPOINTS_PER_CONTACT ||
                        (version < 3 && endpointCount == 0)
                    ) {
                        throw IOException("Invalid integrated endpoint count")
                    }
                    val endpoints = buildList(endpointCount) {
                        repeat(endpointCount) {
                            val endpoint = if (version == 1) {
                                input.readShortcutEndpoint()
                            } else {
                                when (input.readInt()) {
                                    ENDPOINT_SHORTCUT -> input.readShortcutEndpoint()
                                    ENDPOINT_PROVIDER_ADDRESS -> input.readProviderAddressEndpoint()
                                    else -> throw IOException("Unknown integrated endpoint type")
                                }
                            }
                            if (!endpoint.isValid() || !seenEndpoints.add(endpoint.identity)) {
                                throw IOException("Invalid integrated endpoint identity")
                            }
                            add(endpoint)
                        }
                    }
                    val callEndpoint = if (version >= 3 && input.readBoolean()) {
                        when (input.readInt()) {
                            CALL_ENDPOINT_PHONE -> IntegratedContactCallEndpoint.PhoneNumber(
                                number = input.readBoundedString(MAX_ADDRESS_BYTES),
                                packageName = input.readNullableBoundedString(MAX_PACKAGE_BYTES),
                            )
                            else -> throw IOException("Unknown integrated call endpoint type")
                        }.also { call ->
                            if (!call.isValid() || !seenCallEndpoints.add(call.identity)) {
                                throw IOException("Invalid integrated call endpoint identity")
                            }
                        }
                    } else null
                    if (endpoints.isEmpty() && callEndpoint == null) {
                        throw IOException("Integrated contact has no endpoint")
                    }
                    add(
                        FavoriteIntegratedContact(
                            id = FavoriteContactId.Integrated(localId),
                            displayName = displayName,
                            endpoints = endpoints,
                            callEndpoint = callEndpoint,
                            nameSource = savedNameSource
                                ?: inferredIntegratedContactNameSource(displayName, endpoints),
                        ),
                    )
                }
                if (input.read() != -1) throw IOException("Trailing integrated contact data")
            }
        }

    private fun DataOutputStream.writeBoundedString(value: String, maxBytes: Int) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        try {
            require(encoded.isNotEmpty() && encoded.size <= maxBytes) { "String is out of bounds" }
            writeInt(encoded.size)
            write(encoded)
        } finally {
            encoded.fill(0)
        }
    }

    private fun DataOutputStream.writeNullableBoundedString(value: String?, maxBytes: Int) {
        writeBoolean(value != null)
        if (value != null) writeBoundedString(value, maxBytes)
    }

    private fun DataInputStream.readBoundedString(maxBytes: Int): String {
        val size = readInt()
        if (size !in 1..maxBytes) throw IOException("Invalid string size")
        val encoded = ByteArray(size)
        return try {
            readFully(encoded)
            encoded.toString(Charsets.UTF_8)
        } finally {
            encoded.fill(0)
        }
    }

    private fun DataInputStream.readNullableBoundedString(maxBytes: Int): String? =
        if (readBoolean()) readBoundedString(maxBytes) else null

    private fun DataInputStream.readShortcutEndpoint() =
        IntegratedContactEndpoint.ConversationShortcut(
            packageName = readBoundedString(MAX_PACKAGE_BYTES),
            shortcutId = readBoundedString(MAX_SHORTCUT_ID_BYTES),
            userSerial = readLong(),
            label = readNullableBoundedString(MAX_LABEL_BYTES),
        )

    private fun DataInputStream.readProviderAddressEndpoint(): IntegratedContactEndpoint.ProviderAddress {
        val providerId = readBoundedString(MAX_PROVIDER_ID_BYTES)
        val packageName = readBoundedString(MAX_PACKAGE_BYTES)
        val addressType = ProviderAddressType.entries.getOrNull(readInt())
            ?: throw IOException("Invalid provider address type")
        return IntegratedContactEndpoint.ProviderAddress(
            providerId = providerId,
            packageName = packageName,
            addressType = addressType,
            address = readBoundedString(MAX_ADDRESS_BYTES),
        )
    }

    private const val PAYLOAD_MAGIC = 0x4D_49_43_50 // MICP
    private const val MIN_SUPPORTED_PAYLOAD_VERSION = 1
    private const val PAYLOAD_VERSION = 4
    private const val ENDPOINT_SHORTCUT = 1
    private const val ENDPOINT_PROVIDER_ADDRESS = 2
    private const val CALL_ENDPOINT_PHONE = 1
    private const val MAX_CONTACTS = 256
    private const val MAX_ENDPOINTS_PER_CONTACT = 8
    private const val MAX_LOCAL_ID_BYTES = 128
    private const val MAX_DISPLAY_NAME_BYTES = 512
    private const val MAX_PACKAGE_BYTES = 512
    private const val MAX_PROVIDER_ID_BYTES = 256
    private const val MAX_ADDRESS_BYTES = 1024
    private const val MAX_SHORTCUT_ID_BYTES = 2048
    private const val MAX_LABEL_BYTES = 512
}
