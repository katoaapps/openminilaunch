package com.katoaapps.openminilaunch.features.profile

import java.util.Locale

internal object ProfileVCard {
    fun create(card: ProfileCard, links: List<ProfileLink>): String {
        require(card.fullName.isNotBlank()) { "A full name is required" }
        val selected = card.selectedLinkIds.mapNotNull { id -> links.firstOrNull { it.id == id } }
        return buildList {
            add("BEGIN:VCARD")
            add("VERSION:3.0")
            add("N:${escape(card.fullName)};;;;")
            add("FN:${escape(card.fullName)}")
            card.organization.takeIf(String::isNotBlank)?.let { add("ORG:${escape(it)}") }
            card.jobTitle.takeIf(String::isNotBlank)?.let { add("TITLE:${escape(it)}") }
            card.note.takeIf(String::isNotBlank)?.let { add("NOTE:${escape(it)}") }
            selected.forEach { link -> add(linkLine(link)) }
            add("END:VCARD")
        }.flatMap(::foldLine).joinToString("\r\n", postfix = "\r\n")
    }

    fun normalizedValue(link: ProfileLink): String = when (link.type) {
        ProfileLinkType.PHONE -> normalizePhone(link.value)
        ProfileLinkType.EMAIL -> link.value.trim()
        ProfileLinkType.WEBSITE, ProfileLinkType.CUSTOM_URL -> ensureWebScheme(link.value)
        ProfileLinkType.INSTAGRAM -> providerUrl(link.value, "instagram.com/", "https://instagram.com/")
        ProfileLinkType.LINKEDIN -> providerUrl(link.value, "linkedin.com/", "https://www.linkedin.com/in/")
        ProfileLinkType.X -> providerUrl(link.value, "x.com/", "https://x.com/")
        ProfileLinkType.FACEBOOK -> providerUrl(link.value, "facebook.com/", "https://facebook.com/")
        ProfileLinkType.YOUTUBE -> providerUrl(link.value, "youtube.com/", "https://youtube.com/@")
        ProfileLinkType.TIKTOK -> providerUrl(link.value, "tiktok.com/", "https://tiktok.com/@")
        ProfileLinkType.GITHUB -> providerUrl(link.value, "github.com/", "https://github.com/")
        ProfileLinkType.WHATSAPP -> if (link.value.contains("wa.me/", ignoreCase = true)) {
            ensureWebScheme(link.value)
        } else {
            "https://wa.me/${normalizePhone(link.value).removePrefix("+")}"
        }
        ProfileLinkType.SIGNAL -> link.value.trim()
        ProfileLinkType.VENMO -> providerUrl(link.value, "venmo.com/", "https://venmo.com/")
        ProfileLinkType.PAYPAL -> providerUrl(link.value, "paypal.me/", "https://paypal.me/")
        ProfileLinkType.CASH_APP -> providerUrl(
            link.value.removePrefix("$"),
            "cash.app/",
            "https://cash.app/\$",
        )
    }

    fun isValid(link: ProfileLink): Boolean {
        val normalized = normalizedValue(link)
        return when (link.type) {
            ProfileLinkType.PHONE, ProfileLinkType.WHATSAPP -> normalized.count(Char::isDigit) >= 3
            ProfileLinkType.EMAIL -> EMAIL_REGEX.matches(normalized)
            ProfileLinkType.SIGNAL -> normalized.lowercase(Locale.ROOT).let {
                (it.startsWith("https://") || it.startsWith("http://")) && "signal.me/" in it
            }
            else -> normalized.lowercase(Locale.ROOT).let {
                it.startsWith("https://") || it.startsWith("http://")
            }
        }
    }

    private fun linkLine(link: ProfileLink): String {
        val value = normalizedValue(link)
        return when (link.type) {
            // Ungrouped, standard vCard 3.0 properties are intentionally used here. Several
            // Android QR scanners recognize grouped Apple-style item1.TEL/item1.URL fields as a
            // vCard but silently discard their values during contact import.
            ProfileLinkType.PHONE -> "TEL;TYPE=CELL:${escape(value)}"
            ProfileLinkType.EMAIL -> "EMAIL;TYPE=INTERNET:${escape(value)}"
            else -> "URL:${escape(value)}"
        }
    }

    private fun normalizePhone(value: String): String {
        val trimmed = value.trim()
        val digits = trimmed.filter(Char::isDigit)
        return if (trimmed.startsWith('+')) "+$digits" else digits
    }

    private fun providerUrl(value: String, hostFragment: String, prefix: String): String {
        val trimmed = value.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower.startsWith("http://") || lower.startsWith("https://")) return trimmed
        if (hostFragment in lower) return ensureWebScheme(trimmed)
        return prefix + trimmed.removePrefix("@").trimStart('/')
    }

    private fun ensureWebScheme(value: String): String {
        val trimmed = value.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        return if (lower.startsWith("http://") || lower.startsWith("https://")) trimmed else "https://$trimmed"
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")
        .replace(";", "\\;")
        .replace(",", "\\,")

    private fun foldLine(line: String): List<String> {
        if (line.toByteArray(Charsets.UTF_8).size <= MAX_LINE_BYTES) return listOf(line)
        val output = mutableListOf<String>()
        val segment = StringBuilder()
        var segmentBytes = 0
        var index = 0
        var first = true
        while (index < line.length) {
            val codePoint = line.codePointAt(index)
            val value = String(Character.toChars(codePoint))
            val valueBytes = value.toByteArray(Charsets.UTF_8).size
            val limit = if (first) MAX_LINE_BYTES else MAX_LINE_BYTES - 1
            if (segment.isNotEmpty() && segmentBytes + valueBytes > limit) {
                output += (if (first) "" else " ") + segment.toString()
                segment.clear()
                segmentBytes = 0
                first = false
            }
            segment.append(value)
            segmentBytes += valueBytes
            index += Character.charCount(codePoint)
        }
        if (segment.isNotEmpty()) output += (if (first) "" else " ") + segment.toString()
        return output
    }

    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private const val MAX_LINE_BYTES = 75
}
