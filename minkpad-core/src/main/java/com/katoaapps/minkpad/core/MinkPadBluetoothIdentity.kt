package com.katoaapps.minkpad.core

private const val MINKPAD_COMPATIBILITY_NAME = "Pro Controller - MinkPad"
private const val LEGACY_MINKPAD_BLUETOOTH_NAME = "Pro Controller"
private const val MAX_BLUETOOTH_NAME_BYTES = 248
private val LEGACY_MINKPAD_NAME = Regex("^MinkPad [0-9A-F]{8}$", RegexOption.IGNORE_CASE)

fun minkPadBluetoothName(deviceName: String): String {
    val prefix = "$MINKPAD_COMPATIBILITY_NAME ("
    val suffix = ")"
    val availableDeviceNameBytes = MAX_BLUETOOTH_NAME_BYTES -
        prefix.toByteArray(Charsets.UTF_8).size - suffix.toByteArray(Charsets.UTF_8).size
    val normalizedDeviceName = deviceName.trim().ifEmpty { "Android device" }
    return prefix + normalizedDeviceName.takeUtf8Bytes(availableDeviceNameBytes) + suffix
}

fun isMinkPadBluetoothName(value: String): Boolean =
    value == MINKPAD_COMPATIBILITY_NAME ||
        value == LEGACY_MINKPAD_BLUETOOTH_NAME ||
        LEGACY_MINKPAD_NAME.matches(value) ||
        (value.startsWith("$MINKPAD_COMPATIBILITY_NAME (") && value.endsWith(')'))

fun resolveOriginalBluetoothName(
    current: String?,
    remembered: String?,
    configured: String?,
    model: String?,
): String? = sequenceOf(current, remembered, configured, model)
    .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
    .firstOrNull { !isMinkPadBluetoothName(it) }

/** Returns the name that should be present after cleanup, including no-op cleanup. */
fun resolvedRestoredBluetoothName(
    current: String,
    remembered: String?,
    configured: String?,
    model: String?,
): String? = if (!isMinkPadBluetoothName(current)) current else {
    resolveOriginalBluetoothName(null, remembered, configured, model)
}

private fun String.takeUtf8Bytes(maxBytes: Int): String {
    if (maxBytes <= 0) return ""
    var byteCount = 0
    var offset = 0
    while (offset < length) {
        val codePoint = codePointAt(offset)
        val codePointText = String(Character.toChars(codePoint))
        val codePointBytes = codePointText.toByteArray(Charsets.UTF_8).size
        if (byteCount + codePointBytes > maxBytes) break
        byteCount += codePointBytes
        offset += Character.charCount(codePoint)
    }
    return substring(0, offset)
}
