package com.katoaapps.openminilaunch.features.updates

import java.net.HttpURLConnection
import java.net.URL

internal const val GITHUB_LATEST_APK_URL =
    "https://github.com/katoaapps/openminilaunch/releases/latest/download/MinkLauncher-OpenSource.apk"

private const val GITHUB_LATEST_RELEASE_API =
    "https://api.github.com/repos/katoaapps/openminilaunch/releases/latest"

/** Reads only the latest public release tag. APK downloads remain browser-owned. */
internal class GitHubReleaseChecker {
    fun latestReleaseTag(): String? {
        val connection = runCatching {
            URL(GITHUB_LATEST_RELEASE_API).openConnection() as HttpURLConnection
        }.getOrNull() ?: return null

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "MinkLauncher-OpenSource")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                connection.inputStream.bufferedReader().use { reader ->
                    releaseTagFromJson(reader.readText())
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}

internal fun releaseTagFromJson(payload: String): String? =
    TAG_NAME_PATTERN.find(payload)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotEmpty)

internal fun isNewerRelease(currentVersion: String, releaseTag: String): Boolean {
    val current = semanticVersionParts(currentVersion) ?: return false
    val release = semanticVersionParts(releaseTag) ?: return false
    val partCount = maxOf(current.size, release.size)
    for (index in 0 until partCount) {
        val comparison = release.getOrElse(index) { 0 }.compareTo(current.getOrElse(index) { 0 })
        if (comparison != 0) return comparison > 0
    }
    return false
}

private fun semanticVersionParts(raw: String): List<Int>? {
    val normalized = raw.trim()
        .removePrefix("v")
        .substringBefore('-')
        .substringBefore('+')
    if (normalized.isEmpty()) return null
    return normalized.split('.').map { it.toIntOrNull() ?: return null }
}

private val TAG_NAME_PATTERN = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"")
